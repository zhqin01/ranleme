package com.zendrive.simulator.ui

import androidx.compose.foundation.layout.padding
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.amap.api.maps.model.LatLng
import com.zendrive.simulator.App
import com.zendrive.simulator.domain.DriveScene
import com.zendrive.simulator.domain.GeoPoint
import com.zendrive.simulator.domain.VirtualOrder
import com.zendrive.simulator.domain.ZenDriveUiState
import com.zendrive.simulator.ui.navigation.Screen
import com.zendrive.simulator.ui.screens.DriveScreen
import com.zendrive.simulator.ui.screens.GarageScreen
import com.zendrive.simulator.ui.screens.HistoryScreen
import com.zendrive.simulator.ui.screens.ProfileScreen
import com.zendrive.simulator.ui.screens.SettingsScreen
import com.zendrive.simulator.ui.screens.StatsScreen
import com.zendrive.simulator.ui.theme.RanlemeTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun ZenDriveApp(
    state: ZenDriveUiState,
    locationText: String,
    currentLocation: GeoPoint? = null,
    isAdminMode: Boolean = false,
    orderMode: String = "auto",
    bubbleOrders: List<VirtualOrder> = emptyList(),
    routePoints: List<LatLng> = emptyList(),
    onSceneSelected: (DriveScene) -> Unit,
    onStart: () -> Unit,
    onCancel: () -> Unit,
    onContinue: () -> Unit,
    onConfirm: () -> Unit = {},
    onBubbleSelect: (VirtualOrder) -> Unit = {},
    onSimToPickup: () -> Unit = {},
    onSimBoarded: () -> Unit = {},
    onSimArrive: () -> Unit = {},
    onSimComplete: () -> Unit = {},
    onAddCoins: (Int) -> Unit = {},
    onUnlockGarage: (String) -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as App
    var selectedTab by remember { mutableStateOf<Screen>(Screen.Drive) }
    var showProfile by remember { mutableStateOf(false) }

    if (showProfile) {
        ProfileScreen(onBack = { showProfile = false })
        return
    }

    RanlemeTheme {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 0.dp
                ) {
                    // 手动枚举避免 sealed class 初始化顺序问题
                    val tabs = listOf(
                        Tab("驾驶", Screen.Drive, Icons.Filled.DirectionsCar, Icons.Outlined.DirectionsCar),
                        Tab("历史", Screen.History, Icons.Filled.History, Icons.Outlined.History),
                        Tab("统计", Screen.Stats, Icons.Filled.QueryStats, Icons.Outlined.QueryStats),
                        Tab("车库", Screen.Garage, Icons.Filled.Garage, Icons.Outlined.Garage),
                        Tab("我的", Screen.Settings, Icons.Filled.Person, Icons.Outlined.Person),
                    )
                    tabs.forEach { tab ->
                        val selected = tab.screen == selectedTab
                        NavigationBarItem(
                            selected = selected,
                            onClick = { selectedTab = tab.screen },
                            icon = {
                                Icon(
                                    imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                    contentDescription = tab.title
                                )
                            },
                            label = {
                                Text(
                                    tab.title,
                                    fontSize = 11.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                            )
                        )
                    }
                }
            }
        ) { paddingValues ->
            androidx.compose.foundation.layout.Box(
                modifier = Modifier.padding(paddingValues)
            ) {
            when (selectedTab) {
                Screen.Drive -> DriveScreen(
                    state = state,
                    locationText = locationText,
                    currentLocation = currentLocation,
                    isAdminMode = isAdminMode,
                    orderMode = orderMode,
                    bubbleOrders = bubbleOrders,
                    routePoints = routePoints,
                    onSceneSelected = onSceneSelected,
                    onStart = onStart,
                    onCancel = onCancel,
                    onContinue = onContinue,
                    onConfirm = onConfirm,
                    onBubbleSelect = onBubbleSelect,
                    onSimToPickup = onSimToPickup,
                    onSimBoarded = onSimBoarded,
                    onSimArrive = onSimArrive,
                    onSimComplete = onSimComplete,
                    onAddCoins = onAddCoins
                )
                Screen.History -> HistoryScreen(
                    onDelete = { id ->
                        CoroutineScope(Dispatchers.IO).launch {
                            app.tripRepo.deleteById(id)
                        }
                    }
                )
                Screen.Stats -> StatsScreen()
                Screen.Garage -> GarageScreen()
                Screen.Settings -> SettingsScreen(onProfileClick = { showProfile = true })
            }
            }
        }
    }
}

private data class Tab(
    val title: String,
    val screen: Screen,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)
