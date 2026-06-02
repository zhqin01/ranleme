package com.zendrive.simulator.services

import com.zendrive.simulator.domain.GeoPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 跨组件共享定位数据的单例发布者。
 * LocationService 写入，UI 层收集。
 */
object LocationPublisher {
    private val _location = MutableStateFlow<GeoPoint?>(null)
    val location: StateFlow<GeoPoint?> = _location.asStateFlow()

    fun publish(point: GeoPoint) {
        _location.value = point
    }
}
