package com.kasir.mobile.ui.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.kasir.mobile.ui.screen.admin.AdminMenuScreen
import com.kasir.mobile.ui.screen.auth.LoginScreen
import com.kasir.mobile.ui.screen.dashboard.DashboardScreen
import com.kasir.mobile.ui.screen.deletion.DeletionLogScreen
import com.kasir.mobile.ui.screen.history.HistoryScreen
import com.kasir.mobile.ui.screen.printer.PrinterScreen
import com.kasir.mobile.ui.screen.settings.SettingsScreen
import com.kasir.mobile.ui.screen.users.UserManagementScreen
import com.kasir.mobile.ui.viewmodel.KasirViewModel

@Composable
fun KasirNavHost(navController: NavHostController) {
    val kasirViewModel: KasirViewModel = viewModel()

    NavHost(
        navController = navController,
        startDestination = NavRoutes.LOGIN,
        enterTransition = { EnterTransition.None },
        exitTransition = { ExitTransition.None },
        popEnterTransition = { EnterTransition.None },
        popExitTransition = { ExitTransition.None }
    ) {
        composable(NavRoutes.LOGIN) {
            LoginScreen(navController = navController, kasirViewModel = kasirViewModel)
        }
        composable(NavRoutes.ADMIN_MENU) {
            AdminMenuScreen(navController = navController, viewModel = kasirViewModel)
        }
        composable(NavRoutes.DASHBOARD) {
            DashboardScreen(navController = navController, viewModel = kasirViewModel)
        }
        composable(NavRoutes.POS) {
            HistoryScreen(navController = navController, viewModel = kasirViewModel)
        }
        composable(NavRoutes.DELETION_LOGS) {
            DeletionLogScreen(navController = navController, viewModel = kasirViewModel)
        }
        composable(NavRoutes.USERS) {
            UserManagementScreen(navController = navController, viewModel = kasirViewModel)
        }
        composable(NavRoutes.SETTINGS) {
            SettingsScreen(navController = navController, viewModel = kasirViewModel)
        }
        composable(NavRoutes.PRINTER) {
            PrinterScreen(navController = navController)
        }
    }
}
