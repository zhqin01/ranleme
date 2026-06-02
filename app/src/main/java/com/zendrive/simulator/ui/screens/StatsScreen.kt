package com.zendrive.simulator.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zendrive.simulator.App
import com.zendrive.simulator.ui.theme.AccentBlue
import com.zendrive.simulator.ui.theme.PrimaryGreen
import com.zendrive.simulator.ui.theme.AlertOrange

@Composable
fun StatsScreen() {
    val app = androidx.compose.ui.platform.LocalContext.current.applicationContext as App
    val tripRepo = app.tripRepo
    val trips by tripRepo.allTrips.collectAsState(initial = emptyList())

    val totalOrders = trips.size
    val totalDistanceKm = trips.sumOf { it.estimatedDistanceMeters } / 1000.0
    val totalCoins = trips.sumOf { it.coinsEarned }
    // 估算驾驶时长：每公里约 3 分钟（城市驾驶均值 20 km/h）
    val totalMinutes = totalDistanceKm * 3.0
    val hours = (totalMinutes / 60).toInt()
    val mins = (totalMinutes % 60).toInt()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            "驾驶统计",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (totalOrders == 0) {
            EmptyState("还没有数据\n完成订单后这里会显示统计")
        } else {
            // 大字报
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "总里程",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                    )
                    Text(
                        "%.1f".format(totalDistanceKm),
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold,
                        color = AccentBlue
                    )
                    Text(
                        "公里",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // 三格数据卡
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatCard("完单", "$totalOrders", PrimaryGreen, Modifier.weight(1f))
                StatCard("金币", "$totalCoins", AlertOrange, Modifier.weight(1f))
                StatCard("时长", "${hours}h${mins}m", AccentBlue, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, color: androidx.compose.ui.graphics.Color, modifier: Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                value,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }
    }
}
