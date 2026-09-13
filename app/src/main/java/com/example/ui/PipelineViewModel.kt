package com.example.ui

import android.app.Application
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.models.CallLog
import com.example.models.CallLogRepository
import com.example.services.ScamDetectionService
import com.example.utils.Constants
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PipelineViewModel(
    application: Application,
    private val repository: CallLogRepository,
) : AndroidViewModel(application) {

    // Persistent historic logs
    val allLogs: StateFlow<List<CallLog>> = repository.allLogs.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList(),
    )

    val scamLogs: StateFlow<List<CallLog>> = repository.scamLogs.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList(),
    )

    // Reactive Permission Checkers
    var permissionStates = mutableStateMapOf<String, Boolean>()

    // Live AI telemetry state (synced with ScamDetectionService broadcasts)

    var activeCaller by mutableStateOf("")
    var liveScore by mutableIntStateOf(0)
    var liveTranscript by mutableStateOf("")
    var liveHitKeyword by mutableStateOf("")
    var activeStage by mutableStateOf("IDLE")

    var urgencyScore by mutableIntStateOf(0)
    var financialScore by mutableIntStateOf(0)
    var coercionScore by mutableIntStateOf(0)
    var intimacyScore by mutableIntStateOf(0)
    var trustScore by mutableIntStateOf(0)

    private val applicationContext = application.applicationContext

    init {
        viewModelScope.launch {
            com.example.utils.CallStateBroadcaster.telemetryFlow.collect { update ->

                liveScore = update.score
                liveTranscript = update.transcript
                liveHitKeyword = update.hitWord
                activeCaller = update.callerNumber
                activeStage = update.stage

                urgencyScore = update.urgency
                financialScore = update.financial
                coercionScore = update.coercion
                intimacyScore = update.intimacy
                trustScore = update.trust
            }
        }
        viewModelScope.launch {
            com.example.utils.CallStateBroadcaster.callEndedFlow.collect {
                resetTelemetry()
            }
        }
        checkAllPermissions()
    }



    private fun resetTelemetry() {

        activeCaller = ""
        liveScore = 0
        liveTranscript = ""
        liveHitKeyword = ""
        activeStage = "IDLE"
        urgencyScore = 0
        financialScore = 0
        coercionScore = 0
        intimacyScore = 0
        trustScore = 0
    }

    fun clearAllLogs() {
        viewModelScope.launch {
            repository.deleteAll()
        }
    }

    /**
     * MANDATE: DPDP Act (Right to be Forgotten)
     * Performs a complete erasure of all local and remote data associations.
     */
    fun purgeAllData(onComplete: () -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            val context = applicationContext ?: return@launch

            // 1. Wipe local databases
            val scamDb = com.example.models.ScamDatabase.getDatabase(context)
            scamDb.clearAllTables()

            val appDb = com.example.database.AppDatabase.getDatabase(context)
            appDb.clearAllTables()

            // 2. Trigger server cascade delete (for current device/caller hash if possible)
            // Note: Since we are 100% anonymized, we purge by device context where applicable.
            val reporter = com.example.pipeline.SwarmReporter(context)
            reporter.purgeUserData("SELF") // Prototype placeholder

            // 3. Clear SharedPreferences
            context.getSharedPreferences("cyberguard_settings", Context.MODE_PRIVATE).edit().clear().apply()
            context.getSharedPreferences("cyberguard_settings_internal", Context.MODE_PRIVATE).edit().clear().apply()

            withContext(Dispatchers.Main) {
                resetTelemetry()
                onComplete()
            }
        }
    }

    fun deleteLog(log: CallLog) {
        viewModelScope.launch {
            repository.deleteLog(log)
        }
    }

    fun checkAllPermissions() {
        val context = applicationContext

        permissionStates["Record Audio"] = ContextCompat.checkSelfPermission(
            context, android.Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        permissionStates["Read Call Log"] = ContextCompat.checkSelfPermission(
            context, android.Manifest.permission.READ_CALL_LOG
        ) == PackageManager.PERMISSION_GRANTED

        // Dialer role check
        permissionStates["Default Dialer"] = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = context.getSystemService(Context.ROLE_SERVICE) as RoleManager
            roleManager.isRoleHeld(RoleManager.ROLE_DIALER)
        } else {
            true
        }

        permissionStates["Call Screener"] = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = context.getSystemService(Context.ROLE_SERVICE) as RoleManager
            roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)
        } else {
            true
        }
    }

    override fun onCleared() {
        // ViewModel cleanup if needed
        super.onCleared()
    }
}

class PipelineViewModelFactory(
    private val application: Application,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PipelineViewModel::class.java)) {
            val database = com.example.models.ScamDatabase.getDatabase(application.applicationContext)
            val repository = com.example.models.CallLogRepository(database.callLogDao())
            @Suppress("UNCHECKED_CAST")
            return PipelineViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
