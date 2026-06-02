package com.zendrive.simulator.services

import android.util.Log
import java.io.StringWriter
import java.io.PrintWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 轻量崩溃日志记录器。
 * 不依赖 Google Play Services，将所有未捕获异常写入 Logcat。
 * 后续可替换为 Bugly/Crashlytics 等专业 SDK。
 */
object CrashLogger {
    private const val TAG = "RanLeMe_Crash"

    fun init() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            val sw = StringWriter()
            throwable.printStackTrace(PrintWriter(sw))
            val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINESE).format(Date())
            Log.e(TAG, "[$timestamp] Thread: ${thread.name}")
            Log.e(TAG, sw.toString())
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    fun log(msg: String) {
        Log.d(TAG, msg)
    }

    fun recordNonFatal(throwable: Throwable) {
        Log.w(TAG, "Non-fatal: ${throwable.message}", throwable)
    }
}
