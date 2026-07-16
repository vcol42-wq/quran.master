package com.sabah.bikhushue

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class PrayerNotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val prayerName = intent?.getStringExtra("prayer_name") ?: "الصلاة"
        
        val nameAr = when (prayerName) {
            "FAJR" -> "الفجر"
            "DHUHR" -> "الظهر"
            "ASR" -> "العصر"
            "MAGHRIB" -> "المغرب"
            "ISHA" -> "العشاء"
            else -> "الصلاة"
        }

        NotificationHelper.createChannels(context)
        NotificationHelper.showPrayerNotification(
            context,
            "تنبيه دخول وقت الصلاة",
            "حان الآن موعد صلاة $nameAr"
        )

        // Schedule next prayer alarm
        AlarmScheduler.scheduleAlarms(context)
    }
}
