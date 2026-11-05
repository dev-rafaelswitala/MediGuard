package com.rafaelswitala.mediguard.ui.navigation

/**
 * Routendefinitionen und NavHost für Hauptscreens inkl. Profil.
 */

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.NavHostController
import androidx.navigation.navArgument
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.rafaelswitala.mediguard.ui.screens.AddMedicationScreen
import com.rafaelswitala.mediguard.ui.screens.HistoryScreen
import com.rafaelswitala.mediguard.ui.screens.HomeScreen
import com.rafaelswitala.mediguard.ui.screens.MedicationsScreen
import com.rafaelswitala.mediguard.ui.screens.PlanScheduleScreen
import com.rafaelswitala.mediguard.ui.screens.ProfileScreen
import com.rafaelswitala.mediguard.ui.screens.SettingsScreen
import com.rafaelswitala.mediguard.viewmodel.HistoryViewModel
import com.rafaelswitala.mediguard.viewmodel.MedicationViewModel
import com.rafaelswitala.mediguard.viewmodel.ProfileViewModel
import com.rafaelswitala.mediguard.viewmodel.SettingsViewModel

sealed class Route(val route: String) {
    data object Home : Route("home")
    data object Medications : Route("medications")
    data object AddMedication : Route("add_medication")
    data object EditMedication : Route("edit_medication/{medicationId}") {
        fun createRoute(medicationId: Long): String = "edit_medication/$medicationId"
    }
    data object PlanSchedule : Route("plan_schedule/{medicationId}") {
        fun createRoute(medicationId: Long): String = "plan_schedule/$medicationId"
    }
    data object History : Route("history")
    data object Profile : Route("profile")
    data object Settings : Route("settings")
}

@Composable
fun AppNavigation(navController: NavHostController) {
    val medicationViewModel: MedicationViewModel = hiltViewModel()
    val historyViewModel: HistoryViewModel = hiltViewModel()
    val settingsViewModel: SettingsViewModel = hiltViewModel()
    val profileViewModel: ProfileViewModel = hiltViewModel()

    fun navigateTab(route: String) {
        navController.navigate(route) {
            popUpTo(Route.Home.route) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    NavHost(
        navController = navController,
        startDestination = Route.Home.route
    ) {
        composable(Route.Home.route) {
            HomeScreen(
                viewModel = medicationViewModel,
                historyViewModel = historyViewModel,
                onNavigateToMedications = { navigateTab(Route.Medications.route) },
                onNavigateToHistory = { navigateTab(Route.History.route) },
                onNavigateToProfile = { navigateTab(Route.Profile.route) },
                onNavigateToSettings = { navigateTab(Route.Settings.route) }
            )
        }

        composable(Route.Medications.route) {
            MedicationsScreen(
                viewModel = medicationViewModel,
                onNavigateToAdd = { navController.navigate(Route.AddMedication.route) },
                onNavigateHome = { navigateTab(Route.Home.route) },
                onNavigateHistory = { navigateTab(Route.History.route) },
                onNavigateProfile = { navigateTab(Route.Profile.route) },
                onNavigateSettings = { navigateTab(Route.Settings.route) },
                onNavigateToEdit = { medicationId ->
                    navController.navigate(Route.EditMedication.createRoute(medicationId))
                },
                onNavigateToPlan = { medicationId ->
                    navController.navigate(Route.PlanSchedule.createRoute(medicationId))
                }
            )
        }

        composable(Route.AddMedication.route) {
            AddMedicationScreen(
                viewModel = medicationViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Route.EditMedication.route,
            arguments = listOf(navArgument("medicationId") { type = NavType.LongType })
        ) { backStackEntry ->
            AddMedicationScreen(
                viewModel = medicationViewModel,
                medicationId = backStackEntry.arguments?.getLong("medicationId"),
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Route.PlanSchedule.route,
            arguments = listOf(navArgument("medicationId") { type = NavType.LongType })
        ) { backStackEntry ->
            PlanScheduleScreen(
                viewModel = medicationViewModel,
                medicationId = backStackEntry.arguments?.getLong("medicationId") ?: return@composable,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Route.History.route) {
            HistoryScreen(
                viewModel = historyViewModel,
                onNavigateHome = { navigateTab(Route.Home.route) },
                onNavigateMedications = { navigateTab(Route.Medications.route) },
                onNavigateProfile = { navigateTab(Route.Profile.route) },
                onNavigateSettings = { navigateTab(Route.Settings.route) }
            )
        }

        composable(Route.Profile.route) {
            ProfileScreen(
                viewModel = profileViewModel,
                onNavigateHome = { navigateTab(Route.Home.route) },
                onNavigateMedications = { navigateTab(Route.Medications.route) },
                onNavigateHistory = { navigateTab(Route.History.route) },
                onNavigateMore = { navigateTab(Route.Settings.route) }
            )
        }

        composable(Route.Settings.route) {
            SettingsScreen(
                viewModel = settingsViewModel,
                profileViewModel = profileViewModel,
                onNavigateHome = { navigateTab(Route.Home.route) },
                onNavigateMedications = { navigateTab(Route.Medications.route) },
                onNavigateHistory = { navigateTab(Route.History.route) },
                onNavigateProfile = { navigateTab(Route.Profile.route) }
            )
        }
    }
}
