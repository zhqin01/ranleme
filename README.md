# 跑了没 RanLeMe

一款轻量 Android 自驾解压模拟器。模拟网约车接单流程，生成虚拟订单和语音陪伴，让散心驾驶有一个温和的目标感——不提供真实载客服务。

## 功能

- **四种散心场景**：城市霓虹、山路攻弯、松弛省道、网红路线接力
- **五阶段状态机**：待机 → 派单中 → 前往接人 → 行程中 → 抵达结算
- **高德地图**：实时显示 GPS 位置和虚拟目标点
- **TTS 语音播报**：订单通知、乘客闲聊、到点提醒
- **音效 + 震动反馈**：按钮点击、到点提示、完单庆祝
- **历史行程记录**：每次完单自动保存，支持删除
- **驾驶统计**：总里程、完单数、金币、驾驶时长
- **车库经济**：完单得金币，解锁虚拟车漆、徽章、挂件
- **深色/浅色/自动主题切换**
- **锁屏后台运行**：前台服务持续定位
- **无惩罚退出**：任意状态可取消订单或收车

## 技术栈

| 模块 | 方案 |
|------|------|
| 语言 | Kotlin |
| UI | Jetpack Compose + Material 3 |
| 地图 | 高德 3D 地图 SDK |
| 定位 | 高德定位 SDK（前台服务） |
| 存储 | Room (行程+车库) + DataStore (偏好+金币) |
| 语音 | Android TextToSpeech |
| 音效 | ToneGenerator（无额外音频文件） |
| 崩溃 | CrashLogger 本地日志 |

## 构建 Release APK

### 前提

- Android Studio (Hedgehog 或更新)
- Java JDK 17+

### 步骤

1. 用 Android Studio 打开本目录，等待 Gradle 同步。
2. 运行 `build_release.bat` 或在终端执行：

```bash
# 生成签名密钥（仅首次）
keytool -genkey -v -keystore ranleme.keystore -alias ranleme -keyalg RSA -keysize 2048 -validity 10000 -storepass ranleme2024 -keypass ranleme2024 -dname "CN=RanLeMe, OU=Dev, O=RanLeMe, L=Chongqing, ST=Chongqing, C=CN"

# 构建
./gradlew assembleRelease
```

### 产物

```text
app/build/outputs/apk/release/app-release.apk
```

### 调试构建

```bash
./gradlew assembleDebug
# → app/build/outputs/apk/debug/app-debug.apk
```

## 项目结构

```
app/src/main/java/com/zendrive/simulator/
  MainActivity.kt          # 入口 Activity
  App.kt                   # Application 初始化
  domain/                  # 纯业务逻辑
    Models.kt              # 数据模型 + 状态机枚举
    ZenDriveEngine.kt      # 核心状态机引擎
  data/
    db/                    # Room 数据库
    prefs/                 # DataStore 键值偏好
    repository/            # 数据仓库层
  services/
    LocationService.kt     # 前台定位服务（高德）
    LocationPublisher.kt   # 跨组件定位共享
    TtsSpeaker.kt          # TTS 语音播报
    SoundManager.kt        # 音效管理
    VibrationController.kt # 震动反馈
    CrashLogger.kt         # 崩溃日志
  map/
    AmapView.kt            # Compose 高德地图封装
    RouteHelper.kt         # 虚拟路线计算
  ui/
    theme/                 # 主题系统（深色/浅色/自动）
    navigation/            # 底部导航
    screens/               # 5 个页面（驾驶/历史/统计/车库/设置）
    ZenDriveApp.kt         # 顶层 Scaffold
```

## 运行测试

```bash
./gradlew test
```

## 安全边界

- 驾驶中以 TTS 为主，不要求盯屏
- 按钮面积适中，适合安全停靠时操作
- GPS 数据全部本地，不上传任何服务器
- 隐私政策页面可在「设置 → 隐私政策」查看
