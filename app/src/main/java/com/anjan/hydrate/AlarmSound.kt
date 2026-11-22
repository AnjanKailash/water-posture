package com.anjan.hydrate

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Handler
import android.os.Looper

/**
 * Plays the phone's alarm tone once, on the alarm volume, for at most [MAX_MS].
 * Alarm tones are often long or looping, so playback is cut off after a few seconds.
 */
object AlarmSound {
    private const val MAX_MS = 7_000L

    private var player: MediaPlayer? = null
    private val handler = Handler(Looper.getMainLooper())
    private var onFinished: (() -> Unit)? = null

    fun playOnce(c: Context, done: () -> Unit = {}) {
        stop()
        onFinished = done
        val uri = RingtoneManager.getActualDefaultRingtoneUri(c, RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
        try {
            player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                setDataSource(c.applicationContext, uri)
                isLooping = false
                setOnCompletionListener { stop() }
                prepare()
                start()
            }
            handler.postDelayed({ stop() }, MAX_MS)
        } catch (e: Exception) {
            stop()
        }
    }

    fun stop() {
        handler.removeCallbacksAndMessages(null)
        player?.let {
            try { if (it.isPlaying) it.stop() } catch (ignored: Exception) { }
            it.release()
        }
        player = null
        val cb = onFinished
        onFinished = null
        cb?.invoke()
    }
}
