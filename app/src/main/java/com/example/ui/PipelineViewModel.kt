package com.example.ui

import android.app.Application
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
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
    var isSimulating by mutableStateOf(value = false)
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
                isSimulating = true
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

    fun startMockCall(context: Context, isScam: Boolean) {
        val simulatedNumber = if (isScam) "+91 140 900 1122" else "+91 94401 12233"
        isSimulating = true
        activeCaller = simulatedNumber
        liveScore = 0
        liveTranscript = ""
        liveHitKeyword = ""
        activeStage = "BOOTING"

        val intent = Intent(context, ScamDetectionService::class.java).apply {
            putExtra(Constants.EXTRA_CALLER_NUMBER, simulatedNumber)
            putExtra("is_scam_scenario", isScam)
        }
        if (true) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    fun stopCall(context: Context) {
        context.stopService(Intent(context, ScamDetectionService::class.java))
        resetTelemetry()
    }

    private fun resetTelemetry() {
        isSimulating = false
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

        permissionStates["Overlay Screen"] = Settings.canDrawOverlays(context)

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
