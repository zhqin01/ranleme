package com.zendrive.simulator.map

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.View
import android.widget.FrameLayout
import androidx.compose.foundation.Canvas as ComposeCanvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.amap.api.maps.AMap
import com.amap.api.maps.CameraUpdateFactory
import com.amap.api.maps.MapView
import com.amap.api.maps.model.BitmapDescriptorFactory
import com.amap.api.maps.model.LatLng
import com.amap.api.maps.model.MarkerOptions
import com.amap.api.maps.model.MyLocationStyle
import com.zendrive.simulator.domain.GeoPoint
import com.zendrive.simulator.domain.VirtualOrder

private const val TAG_MAP_READY = "map_ready"

sealed class MapTarget {
    data class Pickup(val point: GeoPoint) : MapTarget()
    data class Destination(val point: GeoPoint) : MapTarget()
}

@Composable
fun AmapView(
    modifier: Modifier = Modifier,
    userLocation: GeoPoint?,
    target: MapTarget? = null,
    bubbleOrders: List<VirtualOrder> = emptyList(),
    routePoints: List<LatLng> = emptyList(),
    onBubbleTapped: ((VirtualOrder) -> Unit)? = null
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val mapView = remember {
        runCatching {
            MapView(context).also { it.onCreate(null) }
        }.getOrNull()
    }
    var mapError by remember { mutableStateOf(mapView == null) }
    var firstCameraSet by remember { mutableStateOf(false) }

    if (mapView == null || mapError) {
        MapFallbackView(
            modifier = modifier,
            userLocation = userLocation,
            target = target,
            routePoints = routePoints
        )
        return
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onDestroy()
        }
    }

    AndroidView(
        factory = {
            FrameLayout(context).apply {
                addView(
                    mapView,
                    FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                    )
                )
            }
        },
        modifier = modifier,
        update = {
            val aMap = runCatching { mapView.map }.getOrNull() ?: return@AndroidView
            runCatching {
                setupMapOnce(aMap, mapView, hasLocationPermission(context))

                // 清除旧标记（保留定位蓝点）
                aMap.clear()

                val targetPoint = when (target) {
                    is MapTarget.Pickup -> target.point
                    is MapTarget.Destination -> target.point
                    null -> null
                }
                val focus = userLocation
                    ?: targetPoint
                    ?: routePoints.firstOrNull()?.let { GeoPoint(it.latitude, it.longitude) }
                    ?: DEFAULT_CITY_CENTER

                val latLng = LatLng(focus.latitude, focus.longitude)
                if (!firstCameraSet) {
                    firstCameraSet = true
                    aMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, if (userLocation == null) 12f else 17f))
                } else if (userLocation != null) {
                    aMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16f))
                }

                // 目标点
                when (target) {
                    is MapTarget.Pickup -> addMarker(aMap, target.point, "接人点", BitmapDescriptorFactory.HUE_GREEN)
                    is MapTarget.Destination -> addMarker(aMap, target.point, "目的地", BitmapDescriptorFactory.HUE_RED)
                    null -> {}
                }

                // 气泡订单
                if (bubbleOrders.isNotEmpty()) {
                    val hues = listOf(BitmapDescriptorFactory.HUE_GREEN, BitmapDescriptorFactory.HUE_CYAN, BitmapDescriptorFactory.HUE_ORANGE, BitmapDescriptorFactory.HUE_ROSE)
                    bubbleOrders.forEachIndexed { i, order ->
                        val m = aMap.addMarker(MarkerOptions()
                            .position(LatLng(order.pickup.latitude, order.pickup.longitude))
                            .title(order.title).snippet(order.passengerName)
                            .icon(BitmapDescriptorFactory.defaultMarker(hues[i % hues.size])))
                        m?.`object` = order
                    }
                    onBubbleTapped?.let { cb ->
                        aMap.setOnMarkerClickListener { marker ->
                            val o = marker.`object` as? VirtualOrder
                            if (o != null) { cb(o); true } else false
                        }
                    }
                }

                // 路线
                if (routePoints.size >= 2) {
                    aMap.addPolyline(com.amap.api.maps.model.PolylineOptions()
                        .addAll(routePoints).width(12f)
                        .color(Color.argb(200, 0, 140, 255)).zIndex(5f))
                }
            }.onFailure {
                mapError = true
            }
        }
    )
}

private fun setupMapOnce(aMap: AMap, mapView: View, locationAllowed: Boolean) {
    if (mapView.getTag() == TAG_MAP_READY) return
    mapView.setTag(TAG_MAP_READY)
    val style = MyLocationStyle().apply {
        myLocationType(MyLocationStyle.LOCATION_TYPE_LOCATION_ROTATE_NO_CENTER)
        interval(2000)
        strokeColor(Color.argb(60, 0, 150, 220))
        radiusFillColor(Color.argb(40, 0, 150, 220))
        strokeWidth(2f)
    }
    aMap.myLocationStyle = style
    aMap.isMyLocationEnabled = locationAllowed
    aMap.uiSettings.apply {
        isZoomControlsEnabled = false
        isScrollGesturesEnabled = true
        isZoomGesturesEnabled = true
        isMyLocationButtonEnabled = true
    }
}

private fun hasLocationPermission(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
}

@Composable
private fun MapFallbackView(
    modifier: Modifier,
    userLocation: GeoPoint?,
    target: MapTarget?,
    routePoints: List<LatLng>
) {
    Box(
        modifier = modifier
            .background(ComposeColor(0xFFEAF2EF))
            .padding(18.dp)
    ) {
        ComposeCanvas(modifier = Modifier.fillMaxSize()) {
            val roadColor = ComposeColor(0xFF67A48F)
            val accentColor = ComposeColor(0xFF1F8D68)
            val w = size.width
            val h = size.height

            repeat(6) { i ->
                val y = h * (i + 1) / 7f
                drawLine(
                    color = ComposeColor.White.copy(alpha = 0.75f),
                    start = Offset(0f, y),
                    end = Offset(w, y - 36f),
                    strokeWidth = 4f
                )
            }
            repeat(5) { i ->
                val x = w * (i + 1) / 6f
                drawLine(
                    color = ComposeColor.White.copy(alpha = 0.55f),
                    start = Offset(x, 0f),
                    end = Offset(x - 28f, h),
                    strokeWidth = 3f
                )
            }

            val points = if (routePoints.size >= 2) {
                routePoints.mapIndexed { index, _ ->
                    val t = index.toFloat() / (routePoints.lastIndex.coerceAtLeast(1))
                    Offset(42f + (w - 84f) * t, h * (0.72f - 0.38f * kotlin.math.sin(t * Math.PI).toFloat()))
                }
            } else {
                listOf(Offset(w * 0.22f, h * 0.68f), Offset(w * 0.48f, h * 0.42f), Offset(w * 0.78f, h * 0.35f))
            }

            points.zipWithNext().forEach { (a, b) ->
                drawLine(roadColor, a, b, strokeWidth = 14f)
                drawLine(accentColor, a, b, strokeWidth = 6f)
            }
            drawCircle(ComposeColor(0xFF2563EB), 12f, points.first())
            drawCircle(ComposeColor(0xFFEF4444), 14f, points.last())
        }

        val label = when {
            userLocation == null -> "等待 GPS，已启用地图降级"
            target != null -> "地图降级显示，主流程可继续"
            else -> "地图降级显示"
        }
        Text(
            text = label,
            modifier = Modifier.align(Alignment.TopStart),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
            style = MaterialTheme.typography.bodySmall
        )
    }
}

private val DEFAULT_CITY_CENTER = GeoPoint(29.5630, 106.5516)

private fun addMarker(aMap: AMap, point: GeoPoint, title: String, hue: Float) {
    val icon = createBubbleBitmap(title, hue)
    val m = aMap.addMarker(MarkerOptions()
        .position(LatLng(point.latitude, point.longitude))
        .title(title).icon(BitmapDescriptorFactory.fromBitmap(icon))
        .anchor(0.5f, 0.7f))
    m?.showInfoWindow()
}

private fun createBubbleBitmap(text: String, hue: Float): Bitmap {
    val w = 160; val h = 80
    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val c = Canvas(bmp)
    val bg = Paint(Paint.ANTI_ALIAS_FLAG)

    // 气泡背景色
    bg.color = when {
        hue >= 200f -> Color.rgb(255, 140, 60)   // 橙 Red
        hue >= 100f -> Color.rgb(34, 197, 94)     // 绿
        hue >= 60f  -> Color.rgb(59, 130, 246)    // 蓝
        else        -> Color.rgb(168, 85, 247)    // 紫
    }
    bg.style = Paint.Style.FILL
    c.drawRoundRect(RectF(0f, 0f, w.toFloat(), 60f), 16f, 16f, bg)

    // 小三角
    val tri = android.graphics.Path().apply {
        moveTo(w/2f - 10f, 60f); lineTo(w/2f, 74f); lineTo(w/2f + 10f, 60f); close()
    }
    c.drawPath(tri, bg)

    // 文字
    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE; textSize = 28f; isFakeBoldText = true; textAlign = Paint.Align.CENTER
    }
    c.drawText(text, w/2f, 38f, textPaint)

    return bmp
}
