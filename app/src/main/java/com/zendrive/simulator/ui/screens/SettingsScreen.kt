package com.zendrive.simulator.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zendrive.simulator.App
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val app = context.applicationContext as App
    val prefs = app.prefs
    val scope = rememberCoroutineScope()

    val themeMode by prefs.themeMode.collectAsState(initial = "auto")
    val soundEnabled by prefs.soundEnabled.collectAsState(initial = true)
    val vibrationEnabled by prefs.vibrationEnabled.collectAsState(initial = true)
    val ttsVolume by prefs.ttsVolume.collectAsState(initial = 0.8f)
    val adminMode by prefs.isAdminMode.collectAsState(initial = false)
    val orderMode by prefs.orderMode.collectAsState(initial = "auto")

    var themeDialog by remember { mutableStateOf(false) }
    var orderModeDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            "设置",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // ── 头像区域 ──
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Row(modifier = Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(56.dp).clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) { Text("🚗", fontSize = 28.sp) }
                Spacer(Modifier.width(14.dp))
                Column {
                    Text("张师傅", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("跑了没 · 散心司机", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        // ── 主题切换 ──
        SettingsCard {
            SettingsRow(
                icon = Icons.Filled.DarkMode,
                title = "主题模式",
                subtitle = when (themeMode) {
                    "dark" -> "深色"
                    "light" -> "浅色"
                    else -> "跟随系统"
                },
                onClick = { themeDialog = true }
            )
        }

        Spacer(Modifier.height(10.dp))

        // ── 音效开关 ──
        SettingsCard {
            SettingsSwitchRow(
                icon = Icons.Filled.MusicNote,
                title = "音效",
                checked = soundEnabled,
                onCheckedChange = { scope.launch { prefs.setSoundEnabled(it) } }
            )
        }

        Spacer(Modifier.height(10.dp))

        // ── 震动开关 ──
        SettingsCard {
            SettingsSwitchRow(
                icon = Icons.Filled.Vibration,
                title = "震动反馈",
                checked = vibrationEnabled,
                onCheckedChange = { scope.launch { prefs.setVibrationEnabled(it) } }
            )
        }

        Spacer(Modifier.height(10.dp))

        // ── TTS 音量 ──
        SettingsCard {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.VolumeUp,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(12.dp))
                    Text("语音音量", style = MaterialTheme.typography.titleMedium)
                }
                Spacer(Modifier.height(6.dp))
                Slider(
                    value = ttsVolume,
                    onValueChange = { scope.launch { prefs.setTtsVolume(it) } },
                    valueRange = 0f..1f
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        // ── 管理员模式 ──
        SettingsCard {
            SettingsSwitchRow(
                icon = Icons.Filled.AdminPanelSettings,
                title = "管理员模式",
                subtitle = if (adminMode) "已开启 — 驾驶页显示模拟测试按钮" else "仅用于办公室功能测试",
                checked = adminMode,
                onCheckedChange = { scope.launch { prefs.setAdminMode(it) } }
            )
        }

        Spacer(Modifier.height(10.dp))

        // ── 接单模式 ──
        SettingsCard {
            SettingsRow(
                icon = Icons.Filled.DirectionsCar,
                title = "接单模式",
                subtitle = if (orderMode == "bubble") "气泡选单" else "自动派单",
                onClick = { orderModeDialog = true }
            )
        }

        Spacer(Modifier.height(10.dp))

        // ── 隐私政策 ──
        SettingsCard {
            SettingsRow(
                icon = Icons.Filled.Info,
                title = "隐私政策",
                subtitle = "了解我们如何使用位置权限",
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.example.com/privacy"))
                    context.startActivity(intent)
                }
            )
        }

        Spacer(Modifier.height(24.dp))

        Text(
            "跑了没 v0.1.0",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
            modifier = Modifier.padding(bottom = 32.dp)
        )
    }

    // 接单模式弹窗
    if (orderModeDialog) {
        OrderModeDialog(
            current = orderMode,
            onSelect = { mode ->
                scope.launch { prefs.setOrderMode(mode) }
                orderModeDialog = false
            },
            onDismiss = { orderModeDialog = false }
        )
    }

    // 主题选择弹窗
    if (themeDialog) {
        ThemeDialog(
            current = themeMode,
            onSelect = { mode ->
                scope.launch { prefs.setThemeMode(mode) }
                themeDialog = false
            },
            onDismiss = { themeDialog = false }
        )
    }
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) { content() }
}

@Composable
private fun SettingsRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        }
        Icon(
            Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
        )
    }
}

@Composable
private fun SettingsSwitchRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String = "",
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium)
                if (subtitle.isNotEmpty()) {
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun OrderModeDialog(
    current: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择接单模式") },
        text = {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect("auto") }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    androidx.compose.material3.RadioButton(
                        selected = current == "auto",
                        onClick = { onSelect("auto") }
                    )
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text("自动派单", style = MaterialTheme.typography.bodyLarge)
                        Text("系统自动分配订单，适合真实驾驶", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect("bubble") }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    androidx.compose.material3.RadioButton(
                        selected = current == "bubble",
                        onClick = { onSelect("bubble") }
                    )
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text("气泡选单", style = MaterialTheme.typography.bodyLarge)
                        Text("地图显示多个订单气泡，自由选择", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            Text("取消", modifier = Modifier.clickable { onDismiss() }, color = MaterialTheme.colorScheme.primary)
        }
    )
}

@Composable
private fun ThemeDialog(
    current: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择主题模式") },
        text = {
            Column {
                ThemeOption("跟随系统", "auto", current == "auto", onSelect)
                ThemeOption("浅色模式", "light", current == "light", onSelect)
                ThemeOption("深色模式", "dark", current == "dark", onSelect)
            }
        },
        confirmButton = {},
        dismissButton = {
            Text(
                "取消",
                modifier = Modifier.clickable { onDismiss() },
                color = MaterialTheme.colorScheme.primary
            )
        }
    )
}

@Composable
private fun ThemeOption(label: String, value: String, selected: Boolean, onClick: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(value) }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        androidx.compose.material3.RadioButton(
            selected = selected,
            onClick = { onClick(value) }
        )
        Spacer(Modifier.width(8.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
fun EmptyState(message: String) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
        )
    }
}
