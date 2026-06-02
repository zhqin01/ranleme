package com.zendrive.simulator

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zendrive.simulator.data.repository.GarageRepository
import com.zendrive.simulator.data.repository.TripRepository
import com.zendrive.simulator.domain.DriveScene
import com.zendrive.simulator.domain.DriveStage
import com.zendrive.simulator.domain.GeoPoint
import com.zendrive.simulator.domain.TripRecord
import com.zendrive.simulator.domain.VirtualOrder
import com.zendrive.simulator.domain.ZenDriveEngine
import com.amap.api.maps.model.LatLng
import com.zendrive.simulator.map.AmapView
import com.zendrive.simulator.map.MapTarget
import com.zendrive.simulator.services.LocationPublisher
import com.zendrive.simulator.services.LocationService
import com.zendrive.simulator.services.SoundManager
import com.zendrive.simulator.services.TtsSpeaker
import com.zendrive.simulator.services.VibrationController
import com.zendrive.simulator.ui.ZenDriveApp
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var speaker: TtsSpeaker
    private lateinit var tripRepo: TripRepository
    private lateinit var garageRepo: GarageRepository
    private var lastKnownLocation: GeoPoint? = null

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { granted ->
        val locationGranted = granted[Manifest.permission.ACCESS_FINE_LOCATION] == true
                || granted[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (locationGranted) {
            startLocationService()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val app = application as App
        tripRepo = app.tripRepo
        garageRepo = app.garageRepo

        speaker = TtsSpeaker(this)
        val engine = ZenDriveEngine()

        requestPermissionsAndStart()

        setContent {
            val location by LocationPublisher.location.collectAsState()
            val adminMode by app.prefs.isAdminMode.collectAsState(initial = false)
            val orderMode by app.prefs.orderMode.collectAsState(initial = "auto")
            var uiState by remember { mutableStateOf(engine.state) }
            var bubbleOrders by remember { mutableStateOf<List<VirtualOrder>>(emptyList()) }
            var routePoints by remember { mutableStateOf<List<LatLng>>(emptyList()) }
            var locationText by remember { mutableStateOf("GPS 未锁定") }

            // 同步定位到引擎
            LaunchedEffect(location) {
                location?.let { point ->
                    lastKnownLocation = point
                    locationText = "%.5f, %.5f".format(point.latitude, point.longitude)
                    uiState = engine.updateLocation(point)
                }
            }

            // 路线规划：TODO 单独修复 — 先禁用以保证地图稳定
            // LaunchedEffect(uiState.stage, uiState.order) { ... }

            // 断开定位标记焦点
            DisposableEffect(Unit) {
                onDispose { speaker.shutdown() }
            }

            // ── 状态机 TTS + 音效震动 + 数据持久化 ──
            LaunchedEffect(uiState.stage) {
                when (uiState.stage) {
                    DriveStage.Dispatching -> {
                        delay((3_000L..5_000L).random())
                        val origin = lastKnownLocation ?: return@LaunchedEffect
                        uiState = engine.assignOrder(origin)
                        SoundManager.playClick()
                        VibrationController.short(this@MainActivity)
                        speaker.speak("您有新的订单，请前往接送点。")
                    }
                    DriveStage.WaitingPassenger -> {
                        delay((5_000L..10_000L).random())
                        uiState = engine.passengerBoarded()
                        SoundManager.playArrival()
                        VibrationController.medium(this@MainActivity)
                        speaker.speak("乘客已上车，开始前往目的地。")
                    }
                    DriveStage.Trip -> {
                        delay(12_000L)
                        if (engine.state.stage == DriveStage.Trip) {
                            speaker.speak(engine.randomChat())
                        }
                    }
                    DriveStage.Arriving -> {
                        delay((10_000L..15_000L).random())
                        uiState = engine.completeOrder()
                        SoundManager.playComplete()
                        VibrationController.long(this@MainActivity)
                        speaker.speak("已抵达目的地，请提醒乘客收拾好随身物品。本次模拟行程已结束。")

                        // 加金币
                        val app = application as App
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                            app.prefs.addCoins(60)
                        }

                        // 持久化行程记录
                        val order = engine.state.order
                        if (order != null && lastKnownLocation != null) {
                            tripRepo.insert(
                                TripRecord(
                                    sceneTitle = engine.state.selectedScene.title,
                                    passengerName = order.passengerName,
                                    orderTitle = order.title,
                                    startLat = order.pickup.latitude,
                                    startLng = order.pickup.longitude,
                                    endLat = order.destination.latitude,
                                    endLng = order.destination.longitude,
                                    estimatedDistanceMeters = order.pickup.distanceMetersTo(order.destination),
                                    coinsEarned = 60,
                                    completedAtMillis = System.currentTimeMillis()
                                )
                            )
                        }
                    }
                    DriveStage.Complete -> {
                        if (uiState.selectedScene == DriveScene.RouteRelay) {
                            speaker.speak("当前位置风景绝佳，建议您熄火休息三分钟。")
                        }
                    }
                    else -> Unit
                }
            }

            ZenDriveApp(
                state = uiState,
                locationText = locationText,
                currentLocation = location,
                isAdminMode = adminMode,
                orderMode = orderMode,
                bubbleOrders = bubbleOrders,
                routePoints = routePoints,
                onSceneSelected = {
                    uiState = engine.selectScene(it)
                },
                onStart = {
                    if (orderMode == "bubble" && lastKnownLocation != null) {
                        bubbleOrders = engine.generateBubbleOrders(lastKnownLocation!!)
                    } else {
                        uiState = engine.beginDispatch(lastKnownLocation)
                        speaker.speak("开始接单，请保持专注驾驶。")
                    }
                },
                onCancel = {
                    bubbleOrders = emptyList()
                    routePoints = emptyList()
                    uiState = engine.finishShift()
                    speaker.speak("师傅，看你挺累的，那我在这儿下车就行。回去路上注意安全，辛苦了。")
                },
                onContinue = {
                    uiState = engine.beginDispatch(lastKnownLocation)
                    speaker.speak("继续为您寻找轻松路线。")
                },
                onConfirm = {
                    uiState = engine.confirmArrival()
                    if (uiState.stage == DriveStage.WaitingPassenger) {
                        SoundManager.playArrival()
                        VibrationController.medium(this@MainActivity)
                    } else if (uiState.stage == DriveStage.Arriving) {
                        SoundManager.playComplete()
                        VibrationController.long(this@MainActivity)
                    }
                },
                onBubbleSelect = { order ->
                    bubbleOrders = emptyList()
                    uiState = engine.selectBubbleOrder(order)
                    speaker.speak("已接单：${order.title}，请前往接送点。")
                },
                onSimToPickup = {
                    uiState = engine.simulateToPickup()
                    speaker.speak("[模拟] 已到达接人点")
                },
                onSimBoarded = {
                    uiState = engine.simulateBoarded()
                    speaker.speak("[模拟] 乘客已上车")
                },
                onSimArrive = {
                    uiState = engine.simulateArriveDest()
                    speaker.speak("[模拟] 已到达目的地")
                },
                onSimComplete = {
                    uiState = engine.simulateComplete()
                    speaker.speak("[模拟] 行程已完成，金币+60")
                    val app2 = application as App
                    GlobalScope.launch { app2.prefs.addCoins(60) }
                },
                onUnlockGarage = {
                    uiState = engine.unlockGarageItem(it)
                }
            )
        }
    }

    private fun requestPermissionsAndStart() {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        permissionLauncher.launch(permissions.toTypedArray())
    }

    private fun startLocationService() {
        val intent = Intent(this, LocationService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // 停止前台服务
        stopService(Intent(this, LocationService::class.java))
    }
}
