package com.zendrive.simulator.map

import android.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
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
        MapView(context).also { it.onCreate(null) }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_DESTROY -> {
                    mapView.onDestroy()
                }
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
        factory = { mapView },
        modifier = modifier,
        update = { view ->
            val aMap = view.map ?: return@AndroidView
            setupMapOnce(aMap, view)

            // 用户位置
            userLocation?.let { loc ->
                aMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
                    LatLng(loc.latitude, loc.longitude), 16f
                ))
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
                        .title(order.title).snippet("${order.passengerName}")
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
        }
    )
}

private fun setupMapOnce(aMap: AMap, mapView: MapView) {
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
    aMap.isMyLocationEnabled = true
    aMap.uiSettings.apply {
        isZoomControlsEnabled = false
        isScrollGesturesEnabled = true
        isZoomGesturesEnabled = true
        isMyLocationButtonEnabled = false
    }
}

private fun addMarker(aMap: AMap, point: GeoPoint, title: String, hue: Float) {
    val m = aMap.addMarker(MarkerOptions()
        .position(LatLng(point.latitude, point.longitude))
        .title(title).icon(BitmapDescriptorFactory.defaultMarker(hue)))
    m?.showInfoWindow()
}
