package com.zendrive.simulator.services

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * 震动反馈控制器
 * - 短震：按钮点击 (20ms)
 * - 中震：阶段切换 (80ms)
 * - 长震：完单/重要通知 (200ms)
 */
object VibrationController {

    private fun getVibrator(context: Context): Vibrator? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            manager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    fun short(context: Context) {
        vibrate(context, 20)
    }

    fun medium(context: Context) {
        vibrate(context, 80)
    }

    fun long(context: Context) {
        vibrate(context, 200)
    }

    private fun vibrate(context: Context, durationMs: Long) {
        val vibrator = getVibrator(context) ?: return
        val effect = VibrationEffect.createOneShot(
            durationMs,
            VibrationEffect.DEFAULT_AMPLITUDE
        )
        vibrator.vibrate(effect)
    }
}
