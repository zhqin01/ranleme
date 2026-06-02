@echo off
REM ============================================
REM  跑了没 - 生成签名密钥 + 构建 Release APK
REM  需要：Java JDK (keytool) + Android Studio
REM ============================================

echo [1/3] 生成签名密钥 ...
keytool -genkey -v ^
  -keystore ranleme.keystore ^
  -alias ranleme ^
  -keyalg RSA ^
  -keysize 2048 ^
  -validity 10000 ^
  -storepass ranleme2024 ^
  -keypass ranleme2024 ^
  -dname "CN=RanLeMe, OU=Dev, O=RanLeMe, L=Chongqing, ST=Chongqing, C=CN"

if %ERRORLEVEL% NEQ 0 (
    echo 密钥生成失败，请确认已安装 Java JDK
    pause
    exit /b 1
)

echo [2/3] 清理旧构建 ...
if exist app\build rmdir /s /q app\build

echo [3/3] 构建 Release APK ...
call gradlew assembleRelease

if %ERRORLEVEL% NEQ 0 (
    echo 构建失败，请确认 Android SDK 路径正确
    pause
    exit /b 1
)

echo.
echo ============================================
echo  构建成功！
echo  APK 位置: app\build\outputs\apk\release\app-release.apk
echo ============================================
pause
