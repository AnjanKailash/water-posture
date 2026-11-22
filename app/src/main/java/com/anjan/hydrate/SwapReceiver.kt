package com.anjan.hydrate

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** "🔀 Swap" button: pick a different activity and update the reminder quietly. */
class SwapReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        AlarmSound.stop()
        Recommender.pickNext(context)
        ReminderReceiver.show(context, withAlert = false)
    }
}
