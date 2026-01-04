package com.dailydrug.presentation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.rememberNavController
import com.dailydrug.di.getPermissionRepository
import com.dailydrug.permission.DailyDrugPermissionRequestContract
import com.dailydrug.permission.DailyDrugPermissions
import com.dailydrug.presentation.navigation.AppNavHost
import com.dailydrug.presentation.navigation.AppDestination
import com.permissionmodule.domain.repository.PermissionRepository
import com.permissionmodule.presentation.component.PermissionRequester
import dagger.hilt.EntryPointAccessors

@Composable
fun DailyDrugApp(
    targetMedicineId: Long? = null,
    onNavigationConsumed: () -> Unit = {},
    onFinish: () -> Unit = {}
) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val activity = context.findActivity()
    val permissionRepository: PermissionRepository? = remember(activity) {
        activity?.let { getPermissionRepository(it) }
    }
    val permissionContract = remember { DailyDrugPermissionRequestContract() }

    Scaffold { innerPadding ->
        if (permissionRepository != null) {
            PermissionRequester(
                permissions = DailyDrugPermissions.ALL,
                contract = permissionContract,
                repository = permissionRepository,
                onAllGranted = {
                    // All permissions granted - app can function normally
                },
                onAnyDenied = { deniedPermission ->
                    // Handle denied permission if needed
                }
            )
        }
        AppNavHost(
            navController = navController,
            onFinish = onFinish,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        )
    }
    LaunchedEffect(targetMedicineId) {
        targetMedicineId?.let { medicineId ->
            navController.navigate(AppDestination.MedicineDetail.createRoute(medicineId)) {
                launchSingleTop = true
            }
            onNavigationConsumed()
        }
    }
}
