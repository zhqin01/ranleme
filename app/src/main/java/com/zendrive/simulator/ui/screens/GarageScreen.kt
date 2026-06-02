package com.zendrive.simulator.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zendrive.simulator.App
import com.zendrive.simulator.domain.GarageItem
import com.zendrive.simulator.ui.theme.AlertOrange
import com.zendrive.simulator.ui.theme.AccentBlue
import com.zendrive.simulator.ui.theme.PrimaryGreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun GarageScreen() {
    val app = LocalContext.current.applicationContext as App
    val state by app.garageRepo.garageState.collectAsState(initial = com.zendrive.simulator.domain.GarageState())
    val selectedFrame by app.prefs.selectedFrame.collectAsState(initial = "")
    val selectedCar by app.prefs.selectedCar.collectAsState(initial = "")

    var activeTab by remember { mutableStateOf(0) }
    val tabs = listOf("头像框", "车标", "成就")

    val frameItems = state.items.filter { it.id.startsWith("frame_") }
    val carItems = state.items.filter { it.id.startsWith("car_") }
    val badgeItems = state.items.filter { it.id.startsWith("badge_") }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("个人车库", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("${state.coins} 金币", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = AlertOrange)
        }

        Spacer(Modifier.height(12.dp))

        // 子标签
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(0.dp)) {
            tabs.forEachIndexed { i, label ->
                val active = i == activeTab
                Box(
                    modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp))
                        .background(if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface)
                        .clickable { activeTab = i }.padding(horizontal = 16.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(label, fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                        color = if (active) Color.White else MaterialTheme.colorScheme.onSurface)
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        // 内容
        when (activeTab) {
            0 -> FrameList(frameItems, selectedFrame, state.coins) { id ->
                if (frameItems.any { it.id == id && it.unlocked }) {
                    CoroutineScope(Dispatchers.IO).launch { app.prefs.setSelectedFrame(id) }
                } else {
                    CoroutineScope(Dispatchers.IO).launch { app.garageRepo.unlockItem(id) }
                }
            }
            1 -> CarList(carItems, selectedCar, state.coins) { id ->
                if (carItems.any { it.id == id && it.unlocked }) {
                    CoroutineScope(Dispatchers.IO).launch { app.prefs.setSelectedCar(id) }
                } else {
                    CoroutineScope(Dispatchers.IO).launch { app.garageRepo.unlockItem(id) }
                }
            }
            2 -> BadgeList(badgeItems)
        }
    }
}

@Composable
private fun FrameList(items: List<GarageItem>, selected: String, coins: Int, onClick: (String) -> Unit) {
    if (items.isEmpty()) { Text("暂无头像框", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)); return }
    items.forEach { item ->
        val isSelected = selected == item.id
        val frameColor = when (item.id) {
            "frame_gold" -> Color(0xFFFFD700)
            "frame_neon" -> Color(0xFF00D4FF)
            "frame_ocean" -> Color(0xFF1A66FF)
            else -> Color.Gray
        }
        Card(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = if (isSelected) frameColor.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surface)) {
            Row(modifier = Modifier.padding(14.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(44.dp).clip(CircleShape).background(frameColor.copy(alpha = 0.3f)).then(
                    if (isSelected) Modifier.padding(3.dp).clip(CircleShape).background(frameColor)
                    else Modifier.padding(3.dp)
                ), contentAlignment = Alignment.Center) {
                    Text("👤", fontSize = 20.sp)
                }
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(item.name, fontWeight = FontWeight.Medium)
                    Text(if (item.unlocked) if (isSelected) "使用中" else "已解锁" else "${item.price} 金币",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                }
                Button(onClick = { onClick(item.id) },
                    enabled = item.unlocked || coins >= item.price,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(if (isSelected) PrimaryGreen else AlertOrange)) {
                    Text(if (isSelected) "使用中" else if (item.unlocked) "使用" else "解锁", fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
private fun CarList(items: List<GarageItem>, selected: String, coins: Int, onClick: (String) -> Unit) {
    if (items.isEmpty()) { Text("暂无车标", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)); return }
    items.forEach { item ->
        val isSelected = selected == item.id
        val carEmoji = when (item.id) {
            "car_porsche" -> "🏎️"; "car_ferrari" -> "🏁"; "car_lambo" -> "🔥"; "car_gtr" -> "⚡"; else -> "🚗"
        }
        Card(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = if (isSelected) AccentBlue.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surface)) {
            Row(modifier = Modifier.padding(14.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(carEmoji, fontSize = 28.sp)
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(item.name, fontWeight = FontWeight.Medium)
                    Text(if (item.unlocked) if (isSelected) "行驶中" else "已解锁" else "${item.price} 金币",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                }
                Button(onClick = { onClick(item.id) },
                    enabled = item.unlocked || coins >= item.price,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(if (isSelected) PrimaryGreen else AlertOrange)) {
                    Text(if (isSelected) "使用中" else if (item.unlocked) "使用" else "解锁", fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
private fun BadgeList(items: List<GarageItem>) {
    items.forEach { item ->
        Card(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(if (item.unlocked) Icons.Filled.Star else Icons.Filled.Lock, null,
                    tint = if (item.unlocked) AlertOrange else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f))
                Spacer(Modifier.width(10.dp))
                Column { Text(item.name, fontWeight = FontWeight.Medium); Text(if (item.unlocked) "已获得" else "未解锁", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)) }
            }
        }
    }
}
