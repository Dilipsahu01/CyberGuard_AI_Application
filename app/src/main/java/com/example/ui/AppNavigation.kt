package com.example.ui

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "dashboard") {
        
        composable("dashboard") {
            DashboardScreen(
                onNavigateToDialer = { navController.navigate("dialer") },
                onNavigateToSettings = { navController.navigate("settings") },
                onNavigateToLogs = { navController.navigate("call_logs") }
            )
        }

        composable("dialer") {
            DialerScreen(
                onCallClick = { number ->
                    // In a real app, you would launch an intent here.
                    // For the UI preview, we just go to the active call screen.
                    // Currently ActiveCallScreen doesn't take navigation params, 
                    // but we can just pop back for now or show it if we add it to the NavHost.
                    navController.popBackStack()
                }
            )
        }

        composable("call_logs") {
            CallLogsScreen(
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
