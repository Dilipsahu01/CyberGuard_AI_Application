package com.example.ui

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.ui.platform.LocalContext
import android.content.Context
import android.util.Log

@Composable
fun AppNavigation(viewModel: PipelineViewModel) {
    val navController = rememberNavController()
    val context = LocalContext.current

    // Smooth Navigation Transitions: Premium feel with horizontal slides and fades
    NavHost(
        navController = navController,
        startDestination = "dashboard",
        enterTransition = {
            slideInHorizontally(initialOffsetX = { 1000 }, animationSpec = tween(400)) + fadeIn(animationSpec = tween(400))
        },
        exitTransition = {
            slideOutHorizontally(targetOffsetX = { -1000 }, animationSpec = tween(400)) + fadeOut(animationSpec = tween(400))
        },
        popEnterTransition = {
            slideInHorizontally(initialOffsetX = { -1000 }, animationSpec = tween(400)) + fadeIn(animationSpec = tween(400))
        },
        popExitTransition = {
            slideOutHorizontally(targetOffsetX = { 1000 }, animationSpec = tween(400)) + fadeOut(animationSpec = tween(400))
        }
    ) {

        composable("dashboard") {
            DashboardScreen(
                viewModel = viewModel,
                onNavigateToDialer = { navController.navigate("dialer") },
                onNavigateToSettings = { navController.navigate("settings") },
                onNavigateToLogs = { navController.navigate("call_logs") }
            )
        }

        composable("dialer") {
            DialerScreen(
                onCallClick = { number ->
                    try {
                        Log.e("DIALER_TRACE", "==========================================================")
                        Log.e("DIALER_TRACE", "INITIATING CALL: $number")
                        val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as android.telecom.TelecomManager
                        val uri = android.net.Uri.fromParts("tel", number, null)
                        val bundle = android.os.Bundle()

                        Log.e("DIALER_TRACE", "REQUESTING TELECOM_MANAGER.placeCall()...")
                        telecomManager.placeCall(uri, bundle)
                        Log.e("DIALER_TRACE", "placeCall() COMMAND DISPATCHED SUCCESSFULLY.")
                        Log.e("DIALER_TRACE", "==========================================================")
                    } catch (e: SecurityException) {
                        Log.e("DIALER_TRACE", "FATAL SECURITY ERROR: CALL_PHONE permission or PhoneAccount missing.")
                        Log.e("DIALER_TRACE", "ERROR MESSAGE: ${e.message}")
                        android.widget.Toast.makeText(context, "CALL_PHONE permission required", android.widget.Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Log.e("DIALER_TRACE", "UNEXPECTED TELECOM FAILURE: ${e.message}")
                        android.widget.Toast.makeText(context, "Failed to place call: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }

        composable("call_logs") {
            CallLogsScreen(
                viewModel = viewModel,
                onNavigateToRecordings = { navController.navigate("call_recordings") },
                onOpenDialer = { navController.navigate("dialer") }
            )
        }

        composable("call_recordings") {
            CallRecordingsScreen(
                onNavigateToCallLogs = { navController.popBackStack() },
                onOpenDialer = { navController.navigate("dialer") }
            )
        }

        composable("settings") {
            AdvancedSettingsScreen(
                onBackClick = { navController.popBackStack() },
                onCheckForUpdates = { /* Handle check for updates */ }
            )
        }

        composable("whitelist") {
            WhitelistScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        composable("scam_history") {
            val appDb = com.example.database.AppDatabase.getDatabase(context)
            val scamRepo = com.example.database.ScamRepository.getInstance(appDb)
            
            val scamHistoryViewModel: ScamHistoryViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                factory = ScamHistoryViewModel.Factory(scamRepo)
            )

            ScamHistoryScreen(
                viewModel = scamHistoryViewModel,
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}
