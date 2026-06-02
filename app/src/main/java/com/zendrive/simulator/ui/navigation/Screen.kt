package com.zendrive.simulator.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Garage
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Garage
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.QueryStats
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    data object Drive : Screen("drive", "驾驶", Icons.Filled.DirectionsCar, Icons.Outlined.DirectionsCar)
    data object History : Screen("history", "历史", Icons.Filled.History, Icons.Outlined.History)
    data object Stats : Screen("stats", "统计", Icons.Filled.QueryStats, Icons.Outlined.QueryStats)
    data object Garage : Screen("garage", "车库", Icons.Filled.Garage, Icons.Outlined.Garage)
    data object Settings : Screen("settings", "我的", Icons.Filled.Person, Icons.Outlined.Person)

    companion object {
        val items = listOf(Drive, History, Stats, Garage, Settings)
    }
}
