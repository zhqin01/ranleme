package com.zendrive.simulator.services

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

class TtsSpeaker(context: Context) : TextToSpeech.OnInitListener {
    private var ready = false
    private val tts = TextToSpeech(context.applicationContext, this)

    override fun onInit(status: Int) {
        ready = status == TextToSpeech.SUCCESS
        if (ready) {
            tts.language = Locale.SIMPLIFIED_CHINESE
            tts.setSpeechRate(0.92f)
            tts.setPitch(0.96f)
        }
    }

    fun speak(text: String) {
        if (!ready) return
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "zendrive-${System.nanoTime()}")
    }

    fun shutdown() {
        tts.stop()
        tts.shutdown()
    }
}
