package com.example.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.ui.platform.LocalContext
import android.content.Context
import android.util.Log

@Composable
fun AppNavigation(viewModel: PipelineViewModel, startDestination: String = "dashboard") {
    val navController = rememberNavController()
    val context = LocalContext.current

    // Smooth Navigation Transitions: Premium feel with horizontal slides and fades
    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = {
            slideInHorizontally(initialOffsetX = { fullWidth -> fullWidth }, animationSpec = tween(250)) + fadeIn(animationSpec = tween(250))
        },
        exitTransition = {
            slideOutHorizontally(targetOffsetX = { fullWidth -> -fullWidth }, animationSpec = tween(250)) + fadeOut(animationSpec = tween(250))
        },
        popEnterTransition = {
            slideInHorizontally(initialOffsetX = { fullWidth -> -fullWidth }, animationSpec = tween(250)) + fadeIn(animationSpec = tween(250))
        },
        popExitTransition = {
            slideOutHorizontally(targetOffsetX = { fullWidth -> fullWidth }, animationSpec = tween(250)) + fadeOut(animationSpec = tween(250))
        }
    ) {

        composable("dashboard") {
            DashboardScreen(
                viewModel = viewModel,
                onNavigateToDialer = { navController.navigate("dialer") },
                onNavigateToSettings = { navController.navigate("settings") },
                onNavigateToLogs = { navController.navigate("call_logs") },
                onNavigateToContacts = { navController.navigate("contacts") },
                onNavigateToScamHistory = { navController.navigate("scam_history") },
                onNavigateToFavorites = { navController.navigate("favorites") },
                onNavigateToVoicemail = { navController.navigate("voicemail") }
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
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() },
                onCheckForUpdates = { /* Handle check for updates */ },
                onNavigateToBlockedNumbers = { navController.navigate("blocked_numbers") },
                onNavigateToQuickResponses = { navController.navigate("quick_responses") },
                onNavigateToWhitelist = { navController.navigate("whitelist") },
                onNavigateToPrivacyPolicy = { navController.navigate("privacy_policy") }
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

        // ---- New Routes ----

        composable("contacts") {
            AllContactsScreen(
                onOpenDialer = { navController.navigate("dialer") },
                onAddContact = { navController.navigate("create_contact") },
                onContactClick = { contactId -> navController.navigate("contact_detail/$contactId") },
                onEditContact = { contactId -> navController.navigate("edit_contact/$contactId") },
                onViewCallLogs = { navController.navigate("call_logs") }
            )
        }

        composable("favorites") {
            FavoritesScreen(
                onOpenDialer = { navController.navigate("dialer") },
                onBackClick = { navController.popBackStack() }
            )
        }

        composable("voicemail") {
            VoicemailScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(
            "contact_detail/{contactId}",
            arguments = listOf(navArgument("contactId") { type = NavType.LongType })
        ) { backStackEntry ->
            val contactId = backStackEntry.arguments?.getLong("contactId") ?: -1L
            ContactDetailScreen(
                contactId = contactId,
                onBackClick = { navController.popBackStack() },
                onEditClick = { id -> navController.navigate("edit_contact/$id") }
            )
        }

        composable("create_contact") {
            CreateEditContactScreen(
                contactId = -1L,
                onBackClick = { navController.popBackStack() },
                onSaved = { navController.popBackStack() }
            )
        }

        composable(
            "edit_contact/{contactId}",
            arguments = listOf(navArgument("contactId") { type = NavType.LongType })
        ) { backStackEntry ->
            val contactId = backStackEntry.arguments?.getLong("contactId") ?: -1L
            CreateEditContactScreen(
                contactId = contactId,
                onBackClick = { navController.popBackStack() },
                onSaved = { navController.popBackStack() }
            )
        }

        composable(
            "call_detail/{phoneNumber}",
            arguments = listOf(navArgument("phoneNumber") { type = NavType.StringType })
        ) { backStackEntry ->
            val phoneNumber = backStackEntry.arguments?.getString("phoneNumber") ?: ""
            CallDetailScreen(
                phoneNumber = phoneNumber,
                onBackClick = { navController.popBackStack() }
            )
        }

        composable("blocked_numbers") {
            BlockedNumbersScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        composable("quick_responses") {
            QuickResponsesScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        composable("privacy_policy") {
            PrivacyPolicyScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(
            "post_call_review/{number}",
            arguments = listOf(navArgument("number") { type = NavType.StringType })
        ) { backStackEntry ->
            val number = backStackEntry.arguments?.getString("number") ?: "Unknown"
            PostCallReviewScreen(
                blockedNumber = number,
                onContributeHash = {
                    android.widget.Toast.makeText(context, "Hash uploaded to Swarm", android.widget.Toast.LENGTH_SHORT).show()
                    navController.navigate("dashboard") {
                        popUpTo("dashboard") { inclusive = true }
                    }
                },
                onReturnToDialer = {
                    navController.navigate("dialer") {
                        popUpTo("dashboard")
                    }
                }
            )
        }

        composable("blinket_slider") {
            BlinketSliderScreen()
        }
    }
}
