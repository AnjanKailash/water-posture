package com.anjan.hydrate

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** After a reboot or app update, restart reminders if they were on. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (Prefs.isEnabled(context)) Scheduler.scheduleNext(context)
        Blink.resumeIfRunning(context)
    }
}
