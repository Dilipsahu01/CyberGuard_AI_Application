package com.example.ui

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

import androidx.compose.ui.platform.LocalContext

@Composable
fun AppNavigation(viewModel: PipelineViewModel) {
    val navController = rememberNavController()
    val context = LocalContext.current

    NavHost(navController = navController, startDestination = "dashboard") {
        
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
                    // Trigger the background service simulation
                    val isScam = number.contains("140") || number.contains("scam")
                    viewModel.startMockCall(context, isScam = isScam)
                    navController.navigate("active_call")
                }
            )
        }

        composable("active_call") {
            ActiveCallScreen(
                phoneNumber = viewModel.activeCaller,
                callDuration = "Live Call",
                isScamDetected = viewModel.liveScore > 60 || viewModel.liveHitKeyword.isNotEmpty(),
                liveTranscript = viewModel.liveTranscript,
                onEndCall = { 
                    viewModel.stopCall(context)
                    navController.popBackStack("dashboard", false)
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
    }
}
