package com.dailydrug.presentation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.dailydrug.presentation.navigation.AppNavHost
import com.dailydrug.presentation.permission.NotificationPermissionRequester

@Composable
fun DailyDrugApp() {
    val navController = rememberNavController()
    Scaffold { innerPadding ->
        NotificationPermissionRequester()
        AppNavHost(
            navController = navController,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        )
    }
}
