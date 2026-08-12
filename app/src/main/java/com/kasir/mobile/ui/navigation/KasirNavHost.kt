package com.kasir.mobile.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.kasir.mobile.ui.screen.auth.LoginScreen
import com.kasir.mobile.ui.screen.dashboard.DashboardScreen
import com.kasir.mobile.ui.screen.inventory.InventoryAddScreen
import com.kasir.mobile.ui.screen.inventory.InventoryEditScreen
import com.kasir.mobile.ui.screen.inventory.InventoryScreen
import com.kasir.mobile.ui.screen.pos.PosScreen
import com.kasir.mobile.ui.screen.rental.RentalReturnScreen
import com.kasir.mobile.ui.screen.rental.RentalScreen
import com.kasir.mobile.ui.screen.session.SessionScreen

@Composable
fun KasirNavHost(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = NavRoutes.LOGIN
    ) {
        composable(NavRoutes.LOGIN) {
            LoginScreen(navController = navController)
        }
        composable(NavRoutes.DASHBOARD) {
            DashboardScreen(navController = navController)
        }
        composable(NavRoutes.POS) {
            PosScreen(navController = navController)
        }
        composable(NavRoutes.RENTAL) {
            RentalScreen(navController = navController)
        }
        composable(NavRoutes.RENTAL_RETURN) {
            RentalReturnScreen(navController = navController)
        }
        composable(NavRoutes.INVENTORY) {
            InventoryScreen(navController = navController)
        }
        composable(NavRoutes.INVENTORY_ADD) {
            InventoryAddScreen(navController = navController)
        }
        composable(
            route = NavRoutes.INVENTORY_EDIT,
            arguments = listOf(navArgument("itemId") { type = NavType.LongType })
        ) { backStack ->
            val itemId = backStack.arguments?.getLong("itemId") ?: return@composable
            InventoryEditScreen(navController = navController, itemId = itemId)
        }
        composable(NavRoutes.SESSION) {
            SessionScreen(navController = navController)
        }
    }
}
