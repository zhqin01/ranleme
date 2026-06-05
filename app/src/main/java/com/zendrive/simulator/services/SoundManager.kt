package com.zendrive.simulator.services

import android.media.AudioAttributes
import android.media.ToneGenerator
import android.os.Build
import kotlinx.coroutines.delay

object SoundManager {
    private var toneGen: ToneGenerator? = null

    init { ensureTone() }

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

    fun playClick() { ensureTone(); toneGen?.startTone(ToneGenerator.TONE_PROP_ACK, 60) }

    suspend fun playArrival() {
        ensureTone()
        toneGen?.startTone(ToneGenerator.TONE_DTMF_4, 100)
        delay(80)
        toneGen?.startTone(ToneGenerator.TONE_DTMF_8, 150)
    }

    suspend fun playComplete() {
        ensureTone()
        toneGen?.startTone(ToneGenerator.TONE_DTMF_3, 80)
        delay(60)
        toneGen?.startTone(ToneGenerator.TONE_DTMF_6, 80)
        delay(60)
        toneGen?.startTone(ToneGenerator.TONE_DTMF_9, 150)
    }

    fun release() { toneGen?.release(); toneGen = null }
}
