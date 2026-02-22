package com.farmapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.farmapp.ui.navigation.FarmNavHost
import com.farmapp.ui.navigation.Screen
import com.farmapp.ui.theme.FarmAppTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FarmAppTheme {
                FarmApp()
            }
        }
    }
}

data class BottomNavItem(
    val label: String,
    val icon: ImageVector,
    val route: String
)

val bottomNavItems = listOf(
    BottomNavItem("Home",     Icons.Default.Home,        Screen.Dashboard.route),
    BottomNavItem("Crops",    Icons.Default.Grass,       Screen.CropManager.route),
    BottomNavItem("Poultry",  Icons.Default.Egg,         Screen.PoultryManager.route),
    BottomNavItem("Inventory",Icons.Default.Inventory,   Screen.Inventory.route),
    BottomNavItem("Finance",  Icons.Default.AttachMoney, Screen.Finance.route),
    BottomNavItem("Guide",    Icons.Default.MenuBook,    Screen.PestGuide.route)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FarmApp() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Routes where the bottom bar is visible
    val topLevelRoutes = bottomNavItems.map { it.route }

    Scaffold(
        bottomBar = {
            if (currentRoute in topLevelRoutes) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        NavigationBarItem(
                            selected = currentRoute == item.route,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(Screen.Dashboard.route) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        FarmNavHost(navController = navController)
    }
}
