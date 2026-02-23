package com.farmapp.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.farmapp.ui.crop.AddFieldScreen
import com.farmapp.ui.crop.CropManagerScreen
import com.farmapp.ui.crop.FieldDetailScreen
import com.farmapp.ui.dashboard.DashboardScreen
import com.farmapp.ui.finance.FinanceScreen
import com.farmapp.ui.guide.GuideDetailScreen
import com.farmapp.ui.guide.PestGuideScreen
import com.farmapp.ui.inventory.InventoryScreen
import com.farmapp.ui.poultry.AddBatchScreen
import com.farmapp.ui.poultry.BatchDetailScreen
import com.farmapp.ui.poultry.PoultryManagerScreen
import com.farmapp.ui.settings.SettingsScreen

sealed class Screen(val route: String) {
    object Dashboard      : Screen("dashboard")
    object CropManager    : Screen("crop_manager")
    object AddField       : Screen("add_field")
    object FieldDetail    : Screen("field_detail/{fieldId}") {
        fun createRoute(fieldId: Long) = "field_detail/$fieldId"
    }
    object PoultryManager : Screen("poultry_manager")
    object AddBatch       : Screen("add_batch")
    object BatchDetail    : Screen("batch_detail/{batchId}") {
        fun createRoute(batchId: Long) = "batch_detail/$batchId"
    }
    object Inventory      : Screen("inventory")
    object Finance        : Screen("finance")
    object PestGuide      : Screen("pest_guide")
    object GuideDetail    : Screen("guide_detail/{pestId}") {
        fun createRoute(pestId: String) = "guide_detail/$pestId"
    }
    object Settings       : Screen("settings")
}

@Composable
fun FarmNavHost(navController: NavHostController) {
    NavHost(navController = navController, startDestination = Screen.Dashboard.route) {

        composable(Screen.Dashboard.route) {
            DashboardScreen(navController = navController)
        }
        composable(Screen.CropManager.route) {
            CropManagerScreen(navController = navController)
        }
        composable(Screen.AddField.route) {
            AddFieldScreen(navController = navController)
        }
        composable(
            route = Screen.FieldDetail.route,
            arguments = listOf(navArgument("fieldId") { type = NavType.LongType })
        ) { backStack ->
            val fieldId = backStack.arguments?.getLong("fieldId") ?: return@composable
            FieldDetailScreen(fieldId = fieldId, navController = navController)
        }
        composable(Screen.PoultryManager.route) {
            PoultryManagerScreen(navController = navController)
        }
        composable(Screen.AddBatch.route) {
            AddBatchScreen(navController = navController)
        }
        composable(
            route = Screen.BatchDetail.route,
            arguments = listOf(navArgument("batchId") { type = NavType.LongType })
        ) { backStack ->
            val batchId = backStack.arguments?.getLong("batchId") ?: return@composable
            BatchDetailScreen(batchId = batchId, navController = navController)
        }
        composable(Screen.Inventory.route) {
            InventoryScreen(navController = navController)
        }
        composable(Screen.Finance.route) {
            FinanceScreen(navController = navController)
        }
        composable(Screen.PestGuide.route) {
            PestGuideScreen(navController = navController)
        }
        composable(
            route = Screen.GuideDetail.route,
            arguments = listOf(navArgument("pestId") { type = NavType.StringType })
        ) { backStack ->
            val pestId = backStack.arguments?.getString("pestId") ?: return@composable
            GuideDetailScreen(pestId = pestId, navController = navController)
        }
        composable(Screen.Settings.route) {
            SettingsScreen(navController = navController)
        }
    }
}
