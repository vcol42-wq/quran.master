package com.sabah.bikhushue

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            AlarmScheduler.scheduleAlarms(context)
            
            // Trigger an immediate widget update
            val updateIntent = Intent(context, PrayerWidgetProvider::class.java).apply {
                action = "com.sabah.bikhushue.ACTION_MINUTE_UPDATE"
            }
            context.sendBroadcast(updateIntent)
        }
    }
}
