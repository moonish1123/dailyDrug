package com.dailydrug.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.dailydrug.presentation.detail.MedicineDetailScreen
import com.dailydrug.presentation.llm.LlmSettingsScreen
import com.dailydrug.presentation.main.MainRoute
import com.dailydrug.presentation.schedule.ScheduleInputScreen
import com.dailydrug.presentation.settings.SettingsScreen

sealed class AppDestination(val route: String) {
    data object Main : AppDestination("main")
    data object ScheduleInput : AppDestination("schedule") {
        const val ROUTE_WITH_ARGS = "schedule?medicineId={medicineId}&scheduleId={scheduleId}"
        const val ARG_MEDICINE_ID = "medicineId"
        const val ARG_SCHEDULE_ID = "scheduleId"

        fun createRoute(medicineId: Long? = null, scheduleId: Long? = null): String {
            val params = buildList {
                medicineId?.let { add("$ARG_MEDICINE_ID=$it") }
                scheduleId?.let { add("$ARG_SCHEDULE_ID=$it") }
            }
            return if (params.isEmpty()) {
                route
            } else {
                "${route}?${params.joinToString("&")}"
            }
        }
    }
    data object MedicineDetail : AppDestination("detail/{medicineId}") {
        fun createRoute(medicineId: Long) = "detail/$medicineId"
        const val ARG_MEDICINE_ID = "medicineId"
    }
    data object Settings : AppDestination("settings")
    data object LlmSettings : AppDestination("llm_settings")
}

@Composable
fun AppNavHost(
    navController: NavHostController,
    onFinish: () -> Unit,
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
                },
                onBack = onFinish
            )
        }
        composable(
            route = AppDestination.ScheduleInput.ROUTE_WITH_ARGS,
            arguments = listOf(
                navArgument(AppDestination.ScheduleInput.ARG_MEDICINE_ID) {
                    type = NavType.LongType
                    defaultValue = -1L
                },
                navArgument(AppDestination.ScheduleInput.ARG_SCHEDULE_ID) {
                    type = NavType.LongType
                    defaultValue = -1L
                }
            )
        ) {
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
                onBack = { navController.popBackStack() },
                onEditSchedule = { scheduleId ->
                    navController.navigate(AppDestination.ScheduleInput.createRoute(scheduleId = scheduleId))
                }
            )
        }
        composable(route = AppDestination.Settings.route) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onOpenLlmSettings = { navController.navigate(AppDestination.LlmSettings.route) }
            )
        }
        composable(route = AppDestination.LlmSettings.route) {
            LlmSettingsScreen(onNavigateBack = { navController.popBackStack() })
        }
    }
}
