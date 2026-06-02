package com.zendrive.simulator.map

import android.content.Context
import com.amap.api.maps.model.LatLng
import com.amap.api.services.core.LatLonPoint
import com.amap.api.services.route.DrivePath
import com.amap.api.services.route.DriveRouteResult
import com.amap.api.services.route.RouteSearch
import com.zendrive.simulator.domain.GeoPoint
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

object AmapRouteUtil {

    suspend fun searchDriveRoute(
        context: Context,
        from: GeoPoint,
        to: GeoPoint
    ): List<LatLng> = suspendCancellableCoroutine { cont ->
        val routeSearch = RouteSearch(context)
        routeSearch.setRouteSearchListener(object : RouteSearch.OnRouteSearchListener {
            override fun onDriveRouteSearched(result: DriveRouteResult?, code: Int) {
                if (code == 1000 && result != null && result.paths.isNotEmpty()) {
                    val path: DrivePath = result.paths[0]
                    val points = path.steps.flatMap { step ->
                        step.polyline.map { pt -> LatLng(pt.latitude, pt.longitude) }
                    }
                    cont.resume(points)
                } else {
                    // 失败降级：直线
                    cont.resume(listOf(
                        LatLng(from.latitude, from.longitude),
                        LatLng(to.latitude, to.longitude)
                    ))
                }
            }
            override fun onWalkRouteSearched(result: com.amap.api.services.route.WalkRouteResult?, code: Int) {}
            override fun onRideRouteSearched(result: com.amap.api.services.route.RideRouteResult?, code: Int) {}
            override fun onBusRouteSearched(result: com.amap.api.services.route.BusRouteResult?, code: Int) {}
        })

        val query = RouteSearch.DriveRouteQuery(
            RouteSearch.FromAndTo(
                LatLonPoint(from.latitude, from.longitude),
                LatLonPoint(to.latitude, to.longitude)
            ),
            RouteSearch.DRIVING_SINGLE_DEFAULT,
            emptyList(), null, ""
        )
        routeSearch.calculateDriveRouteAsyn(query)
    }
}
