package com.sabah.bikhushue

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat

object NotificationHelper {

    private const val CHANNEL_ID_PRAYER = "prayer_channel"
    private const val CHANNEL_ID_AZKAR = "azkar_channel"

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            
            val prayerChannel = NotificationChannel(
                CHANNEL_ID_PRAYER,
                "تنبيهات الصلاة",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "إشعارات دخول وقت الصلاة"
                enableVibration(true)
            }
            
            val azkarChannel = NotificationChannel(
                CHANNEL_ID_AZKAR,
                "أذكار الصباح والمساء",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "تنبيهات قراءة الأذكار"
            }

            manager.createNotificationChannel(prayerChannel)
            manager.createNotificationChannel(azkarChannel)
        }
    }

    fun showPrayerNotification(context: Context, title: String, message: String) {
        val intent = Intent(context, SplashActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID_PRAYER)
            .setSmallIcon(R.drawable.nnbb)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(1001, builder.build())
    }

    fun showAzkarNotification(context: Context, isMorning: Boolean) {
        val intent = Intent(context, AzkarActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("azkar_type", if (isMorning) "صباح" else "مساء")
        }
        val pendingIntent = PendingIntent.getActivity(
            context, if (isMorning) 1 else 2, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val title = if (isMorning) "أذكار الصباح" else "أذكار المساء"
        val message = if (isMorning) "حان الآن وقت أذكار الصباح، فاذكر الله." else "حان الآن وقت أذكار المساء، فاذكر الله."

        val builder = NotificationCompat.Builder(context, CHANNEL_ID_AZKAR)
            .setSmallIcon(R.drawable.nnbb)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(if (isMorning) 2001 else 2002, builder.build())
    }
}
