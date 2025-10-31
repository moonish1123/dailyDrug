package com.dailydrug.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.dailydrug.presentation.detail.MedicineDetailScreen
import com.dailydrug.presentation.main.MainRoute
import com.dailydrug.presentation.schedule.ScheduleInputScreen
import com.dailydrug.presentation.settings.SettingsScreen

sealed class AppDestination(val route: String) {
    data object Main : AppDestination("main")
    data object ScheduleInput : AppDestination("schedule")
    data object MedicineDetail : AppDestination("detail/{medicineId}") {
        fun createRoute(medicineId: Long) = "detail/$medicineId"
        const val ARG_MEDICINE_ID = "medicineId"
    }
    data object Settings : AppDestination("settings")
}

@Composable
fun AppNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = AppDestination.Main.route,
        modifier = modifier
    ) {
        composable(route = AppDestination.Main.route) {
            MainRoute(
                onAddSchedule = { navController.navigate(AppDestination.ScheduleInput.route) },
                onOpenSettings = { navController.navigate(AppDestination.Settings.route) },
                onOpenMedicineDetail = { medicineId ->
                    navController.navigate(AppDestination.MedicineDetail.createRoute(medicineId))
                }
            )
        }
        composable(route = AppDestination.ScheduleInput.route) {
            ScheduleInputScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            route = AppDestination.MedicineDetail.route,
            arguments = listOf(
                navArgument(AppDestination.MedicineDetail.ARG_MEDICINE_ID) {
                    type = NavType.LongType
                }
            )
        ) { backStackEntry ->
            val medicineId = backStackEntry.arguments?.getLong(AppDestination.MedicineDetail.ARG_MEDICINE_ID)
            MedicineDetailScreen(
                medicineId = medicineId,
                onBack = { navController.popBackStack() }
            )
        }
        composable(route = AppDestination.Settings.route) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
    }
}
