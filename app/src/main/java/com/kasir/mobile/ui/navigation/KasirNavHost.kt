package com.kasir.mobile.ui.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.kasir.mobile.ui.screen.settlement.SettlementScreen
import com.kasir.mobile.ui.viewmodel.AuthViewModel
import com.kasir.mobile.ui.viewmodel.KasirViewModel

@Composable
fun KasirNavHost(navController: NavHostController) {
    val kasirViewModel: KasirViewModel = viewModel()
    val authViewModel: AuthViewModel = viewModel()
    val kasirUiState by kasirViewModel.uiState.collectAsState()

    val onLogout: () -> Unit = {
        kasirViewModel.logout()
        authViewModel.logout()
        navController.navigate(NavRoutes.LOGIN) {
            popUpTo(0) { inclusive = true }
        }
    }

    LaunchedEffect(kasirUiState.sessionExpired) {
        if (kasirUiState.sessionExpired) {
            kasirViewModel.resetSessionExpired()
            authViewModel.logout()
            navController.navigate(NavRoutes.LOGIN) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = NavRoutes.LOGIN,
        enterTransition = { EnterTransition.None },
        exitTransition = { ExitTransition.None },
        popEnterTransition = { EnterTransition.None },
        popExitTransition = { ExitTransition.None }
    ) {
        composable(NavRoutes.LOGIN) {
            LoginScreen(
                navController = navController,
                kasirViewModel = kasirViewModel,
                authViewModel = authViewModel
            )
        }
        composable(NavRoutes.ADMIN_MENU) {
            AdminMenuScreen(
                navController = navController,
                viewModel = kasirViewModel,
                onLogout = onLogout
            )
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
            SettingsScreen(
                navController = navController,
                viewModel = kasirViewModel,
                onLogout = onLogout
            )
        }
        composable(NavRoutes.SETTLEMENT) {
            SettlementScreen(
                navController = navController,
                viewModel = kasirViewModel,
                onLogout = onLogout
            )
        }
        composable(NavRoutes.PRINTER) {
            PrinterScreen(navController = navController)
        }
    }
}
