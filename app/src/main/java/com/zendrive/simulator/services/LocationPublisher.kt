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

    private val _status = MutableStateFlow("GPS 未锁定")
    val status: StateFlow<String> = _status.asStateFlow()

    fun publish(point: GeoPoint) {
        _location.value = point
        _status.value = "%.5f, %.5f".format(point.latitude, point.longitude)
    }

    fun publishStatus(status: String) {
        _status.value = status
    }
}
