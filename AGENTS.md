# ZenDrive Simulator 项目规范

## 项目目标

闲驱模拟器是一个轻量 Android 原型，目标产物为标准 `.apk`。核心原则是低打扰、低体积、原生能力优先，不引入重型地图 SDK、音频包或跨端运行时。

## 技术约束

- 语言：Kotlin。
- UI：Jetpack Compose。
- 语音：仅使用 Android 原生 `TextToSpeech`，不打包本地音频。
- 定位：优先使用 Android 原生 `LocationManager`，避免 Google Play Services 依赖。
- 地图：MVP 不内嵌地图 SDK，仅保留路线状态与外部导航扩展位。
- 体积：禁止加入大图片、视频、音频、字体包和重型图形库。

## 目录结构

```text
app/src/main/java/com/zendrive/simulator/
  MainActivity.kt                 # 应用入口、权限、服务组装
  domain/                         # 纯业务状态机和数据模型
  services/                       # Android 原生能力封装
  ui/                             # Compose 界面
app/src/main/res/values/          # 主题、颜色、字符串
```

## 命名约定

- 状态机枚举使用 `DriveStage`。
- UI 状态使用 `ZenDriveUiState`。
- 场景偏好使用 `DriveScene`。
- 与 Android 平台绑定的类放在 `services` 或入口层，避免污染 `domain`。

## 开发纪律

- 修改业务逻辑前先更新本文件中相关约束。
- 不写入 `.env`、密钥、Token 或 CI/CD 配置。
- 不删除文件、目录或 Git 历史；如需清理产物，先说明范围。
- 生成文件仅限 `build/`、`.gradle/`、APK/AAB 等标准构建产物。
- 改完代码必须尝试验证；若本机缺少 Android 构建环境，需明确说明未能构建的原因。

## 清理机制

- Android Studio/Gradle 产生的 `build/`、`.gradle/`、`local.properties` 不纳入版本管理。
- 大体积素材不得进入仓库；确需素材时使用矢量或 Compose 绘制。
