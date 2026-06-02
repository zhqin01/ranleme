package com.zendrive.simulator.services

import android.media.AudioAttributes
import android.media.ToneGenerator
import android.os.Build

/**
 * 用 Android 内置 ToneGenerator 生成音效，无需额外音频文件。
 * - 按钮点击：短促 DTMF "D" 音
 * - 到点提示：上升双音
 * - 完单提示：上升三连音
 */
object SoundManager {
    private var toneGen: ToneGenerator? = null

    private fun ensureTone() {
        if (toneGen == null) {
            toneGen = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ToneGenerator(AudioAttributes.USAGE_NOTIFICATION, 60)
            } else {
                @Suppress("DEPRECATION")
                ToneGenerator(android.media.AudioManager.STREAM_NOTIFICATION, 60)
            }
        }
    }

    /** 按钮点击音效（短促） */
    fun playClick() {
        ensureTone()
        toneGen?.startTone(ToneGenerator.TONE_PROP_ACK, 60)
    }

    /** 到达接送点/目的地 */
    fun playArrival() {
        ensureTone()
        // 双音：低→高
        toneGen?.startTone(ToneGenerator.TONE_DTMF_4, 100)
        Thread.sleep(80)
        toneGen?.startTone(ToneGenerator.TONE_DTMF_8, 150)
    }

    /** 完单提示音 */
    fun playComplete() {
        ensureTone()
        // 三连上升音
        toneGen?.startTone(ToneGenerator.TONE_DTMF_3, 80)
        Thread.sleep(60)
        toneGen?.startTone(ToneGenerator.TONE_DTMF_6, 80)
        Thread.sleep(60)
        toneGen?.startTone(ToneGenerator.TONE_DTMF_9, 150)
    }

    fun release() {
        toneGen?.release()
        toneGen = null
    }
}
