package com.zendrive.simulator.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zendrive.simulator.domain.DriveScene
import com.zendrive.simulator.domain.DriveStage
import com.zendrive.simulator.domain.GeoPoint
import com.zendrive.simulator.domain.VirtualOrder
import com.zendrive.simulator.domain.ZenDriveUiState
import com.amap.api.maps.model.LatLng
import com.zendrive.simulator.map.AmapView
import com.zendrive.simulator.map.MapTarget
import com.zendrive.simulator.ui.theme.PrimaryGreen
import com.zendrive.simulator.ui.theme.AlertOrange
import com.zendrive.simulator.ui.theme.ErrorRed

@Composable
fun DriveScreen(
    state: ZenDriveUiState,
    locationText: String,
    currentLocation: GeoPoint?,
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
    onAddCoins: (Int) -> Unit = {}
) {
    val mapTarget = when (state.stage) {
        DriveStage.Pickup, DriveStage.WaitingPassenger -> state.order?.pickup?.let { MapTarget.Pickup(it) }
        DriveStage.Trip -> state.order?.destination?.let { MapTarget.Destination(it) }
        else -> null
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopStatusBar(state, locationText)

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            AmapView(
                modifier = Modifier.fillMaxSize(),
                userLocation = currentLocation,
                target = mapTarget,
                bubbleOrders = bubbleOrders,
                routePoints = routePoints,
                onBubbleTapped = { order -> onBubbleSelect(order) }
            )
        }

        BottomControlPanel(
            state = state,
            isAdminMode = isAdminMode,
            orderMode = orderMode,
            bubbleOrders = bubbleOrders,
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
    }
}

@Composable
private fun TopStatusBar(
    state: ZenDriveUiState,
    locationText: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "跑了没",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    state.lastMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    state.stage.label,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    locationText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                )
            }
        }
    }
}

@Composable
private fun BottomControlPanel(
    modifier: Modifier = Modifier,
    state: ZenDriveUiState,
    isAdminMode: Boolean = false,
    orderMode: String = "auto",
    bubbleOrders: List<VirtualOrder> = emptyList(),
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
    onAddCoins: (Int) -> Unit = {}
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = 360.dp)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 场景选择器
        SceneChips(state.selectedScene, onSceneSelected)

        // 气泡订单列表（仅在气泡模式 + 待机/接单中时显示）
        if (orderMode == "bubble" && bubbleOrders.isNotEmpty() && state.stage == DriveStage.Offline) {
            BubbleOrderList(bubbleOrders, currentLocation = null, onSelect = onBubbleSelect)
        }

        // 管理员测试按钮
        if (isAdminMode) {
            AdminTestPanel(state, onSimToPickup, onSimBoarded, onSimArrive, onSimComplete, onAddCoins = onAddCoins)
        }

        // 当前订单信息 + 操作按钮
        OrderActionCard(state, onStart, onCancel, onContinue, onConfirm)
    }
}

@Composable
private fun SceneChips(
    selected: DriveScene,
    onSceneSelected: (DriveScene) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        DriveScene.entries.forEach { scene ->
            val active = scene == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (active) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                    )
                    .clickable { onSceneSelected(scene) }
                    .padding(horizontal = 10.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        scene.title,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (active) Color.White else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                    Text(
                        scene.shortText,
                        fontSize = 10.sp,
                        color = if (active) Color.White.copy(alpha = 0.75f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun OrderActionCard(
    state: ZenDriveUiState,
    onStart: () -> Unit,
    onCancel: () -> Unit,
    onContinue: () -> Unit,
    onConfirm: () -> Unit = {}
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 距离指示
            val curDist = state.distanceToTargetMeters?.let {
                if (it >= 1000) "%.1f km".format(it / 1000) else "${it.toInt()} m"
            } ?: "--"

            val order = state.order
            val destToPickup = order?.pickup?.distanceMetersTo(order.destination)

            Text(
                order?.title ?: "准备出发",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            if (order != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    if (state.stage == DriveStage.Pickup || state.stage == DriveStage.WaitingPassenger) {
                        Text("接人点 $curDist", style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
                    } else {
                        Text("接人点 ✓", style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF22C55E), fontWeight = FontWeight.Medium)
                    }
                    val destDist = destToPickup?.let {
                        if (it >= 1000) "%.1f km".format(it / 1000) else "${it.toInt()} m"
                    } ?: "--"
                    Text("目的地 $destDist", style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f))
                }
            } else {
                Text("目标距离 $curDist", style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f))
            }

            Spacer(Modifier.height(2.dp))

            // 按钮
            if (state.needsConfirm) {
                Button(
                    onClick = onConfirm,
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2EE573), contentColor = Color.White)
                ) {
                    Text(
                        if (state.stage == DriveStage.Pickup) "确认乘客已上车" else "确认到达目的地",
                        fontSize = 17.sp, fontWeight = FontWeight.Bold
                    )
                }
            } else when (state.stage) {
                DriveStage.Offline -> Button(
                    onClick = onStart,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                ) {
                    Text("开始接单", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                }
                DriveStage.Complete -> Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onContinue,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                    ) { Text("继续派单", fontWeight = FontWeight.Bold) }
                    OutlinedButton(
                        onClick = onCancel,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) { Text("收车") }
                }
                else -> OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Filled.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("取消订单")
                }
            }
        }
    }
}

// ── 气泡订单列表 ──

@Composable
private fun BubbleOrderList(
    orders: List<VirtualOrder>,
    currentLocation: GeoPoint?,
    onSelect: (VirtualOrder) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("附近订单", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                orders.forEach { order ->
                    val dist = currentLocation?.distanceMetersTo(order.pickup) ?: 1200.0
                    val distText = if (dist >= 1000) "%.1fkm".format(dist / 1000) else "${dist.toInt()}m"
                    val color = when { dist < 800 -> PrimaryGreen; dist < 2000 -> AlertOrange; else -> ErrorRed }
                    Card(
                        modifier = Modifier.weight(1f).clickable { onSelect(order) },
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.15f))
                    ) {
                        Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(order.title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(4.dp))
                            Text(distText, fontSize = 12.sp, color = color, fontWeight = FontWeight.Bold)
                            Text(order.passengerName, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        }
                    }
                }
            }
        }
    }
}

// ── 管理员测试面板 ──

@Composable
private fun AdminTestPanel(
    state: ZenDriveUiState,
    onSimToPickup: () -> Unit,
    onSimBoarded: () -> Unit,
    onSimArrive: () -> Unit,
    onSimComplete: () -> Unit,
    onAddCoins: (Int) -> Unit = {}
) {
    var coinInput by remember { mutableStateOf("") }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("🔧 管理员测试", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE65100))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TestBtn("到接人点", state.stage in listOf(DriveStage.Pickup, DriveStage.WaitingPassenger), onSimToPickup)
                TestBtn("上车", state.stage == DriveStage.WaitingPassenger, onSimBoarded)
                TestBtn("到目的地", state.stage == DriveStage.Trip, onSimArrive)
                TestBtn("完单", state.stage != DriveStage.Offline && state.stage != DriveStage.Complete, onSimComplete)
            }
            // 金币修改
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("💰", fontSize = 14.sp)
                OutlinedTextField(
                    value = coinInput,
                    onValueChange = { coinInput = it.filter { c -> c.isDigit() } },
                    modifier = Modifier.weight(1f).height(40.dp),
                    placeholder = { Text("输入金币数量", fontSize = 11.sp) },
                    singleLine = true,
                    textStyle = TextStyle(fontSize = 13.sp)
                )
                Button(
                    onClick = {
                        val amount = coinInput.toIntOrNull() ?: 0
                        if (amount > 0) { onAddCoins(amount); coinInput = "" }
                    },
                    enabled = coinInput.isNotEmpty(),
                    modifier = Modifier.height(38.dp),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800))
                ) { Text("添加", fontSize = 12.sp) }
            }
        }
    }
}

@Composable
private fun TestBtn(label: String, enabled: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick, enabled = enabled,
        modifier = Modifier.height(32.dp),
        shape = RoundedCornerShape(8.dp),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800), disabledContainerColor = Color(0xFFFFE0B2))
    ) { Text(label, fontSize = 11.sp) }
}
