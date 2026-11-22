package com.anjan.hydrate

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.drawable.Icon
import android.media.AudioAttributes
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * Blink reminder: a gentle double buzz every N seconds until the user stops it.
 * Runs as a foreground service (with a "Stop" notification) so it keeps going
 * with the screen off. Silent while Do Not Disturb or Airplane mode is on.
 */
class BlinkService : Service() {

    companion object {
        const val ACTION_STOP = "com.anjan.hydrate.BLINK_STOP"
        private const val CHANNEL = "blink_min"
        private const val NOTIFICATION_ID = 77

        @Volatile var running = false
            private set

        fun start(c: Context) = c.startForegroundService(Intent(c, BlinkService::class.java))
        fun stop(c: Context) { c.stopService(Intent(c, BlinkService::class.java)) }
    }

    private val handler = Handler(Looper.getMainLooper())
    private var wakeLock: PowerManager.WakeLock? = null

    private val tick = object : Runnable {
        override fun run() {
            val c = this@BlinkService
            if (!ReminderReceiver.isDndOn(c) && !ReminderReceiver.isAirplaneModeOn(c)) pulse()
            handler.postDelayed(this, Prefs.blinkSeconds(c) * 1000L)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        startInForeground()
        if (wakeLock == null) {
            // Keep the CPU awake just enough to buzz on time with the screen off (max 8 h).
            wakeLock = getSystemService(PowerManager::class.java)
                .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "SipMove:blink")
                .apply { acquire(8 * 60 * 60 * 1000L) }
        }
        running = true
        handler.removeCallbacks(tick)  // (re)start the rhythm, e.g. after an interval change
        handler.postDelayed(tick, Prefs.blinkSeconds(this) * 1000L)
        return START_STICKY
    }

    override fun onDestroy() {
        running = false
        handler.removeCallbacks(tick)
        wakeLock?.let { if (it.isHeld) it.release() }
        wakeLock = null
        super.onDestroy()
    }

    private fun startInForeground() {
        val nm = getSystemService(NotificationManager::class.java)
        nm.deleteNotificationChannel("blink")  // older, more visible channel
        if (nm.getNotificationChannel(CHANNEL) == null) {
            // MIN importance: no status-bar icon, just a collapsed line in the shade (Android requires one).
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL, "Blink buzz (running)", NotificationManager.IMPORTANCE_MIN).apply {
                    description = "Shown while the blink reminder is running"
                    setSound(null, null)
                    enableVibration(false)
                }
            )
        }
        val stopPi = PendingIntent.getService(
            this, 3001, Intent(this, BlinkService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val openPi = PendingIntent.getActivity(
            this, 3002, Intent(this, MoveActivity::class.java)
                .putExtra(MoveActivity.EXTRA_MOVE_ID, "eyes")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val n = Notification.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_water)
            .setContentTitle("👁 Blink buzz every ${Prefs.blinkSeconds(this)} s")
            .setContentText("Tap Stop when you're done")
            .setOngoing(true)
            .setContentIntent(openPi)
            .addAction(Notification.Action.Builder(null as Icon?, "■ Stop", stopPi).build())
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, n)
        }
    }

    private fun pulse() {
        val v: Vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            getSystemService(VibratorManager::class.java).defaultVibrator
        else @Suppress("DEPRECATION") getSystemService(Vibrator::class.java)
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        @Suppress("DEPRECATION")
        v.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 90, 120, 90), -1), attrs)
    }
}
