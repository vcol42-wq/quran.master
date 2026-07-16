package com.sabah.bikhushue

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class AzkarAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val isMorning = intent?.getBooleanExtra("is_morning", true) ?: true
        
        NotificationHelper.createChannels(context)
        NotificationHelper.showAzkarNotification(context, isMorning)

        // Schedule next alarms
        AlarmScheduler.scheduleAlarms(context)
    }
}
