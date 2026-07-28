package com.sabah.bikhushue

import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlin.math.sqrt

class AthanService : Service(), SensorEventListener {

    private var mediaPlayer: MediaPlayer? = null
    private val handler = Handler(Looper.getMainLooper())
    private var lastPrayerNameAr = "الصلاة"
    
    private var sensorManager: SensorManager? = null
    private var accelerometer: Sensor? = null
    private var hasInitialSensorReading = false
    private var initialZ = 0f

    private val stopRunnable = Runnable {
        Log.d("AthanService", "Auto stopping after 30 seconds")
        stopAthan()
    }

    companion object {
        const val ACTION_STOP_ATHAN = "com.sabah.bikhushue.ACTION_STOP_ATHAN"
        private const val NOTIFICATION_ID = 1002
        private const val SHAKE_THRESHOLD = 2.5f
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP_ATHAN) {
            stopAthan()
            return START_NOT_STICKY
        }

        val prayerName = intent?.getStringExtra("prayer_name") ?: "الصلاة"
        lastPrayerNameAr = when (prayerName) {
            "FAJR" -> "الفجر"
            "DHUHR" -> "الظهر"
            "ASR" -> "العصر"
            "MAGHRIB" -> "المغرب"
            "ISHA" -> "العشاء"
            else -> "الصلاة"
        }

        NotificationHelper.createChannels(this)
        startForeground(NOTIFICATION_ID, createNotification(lastPrayerNameAr, true))
        playAthanSound()

        // Register sensor listener to stop on explicit flip/shake
        hasInitialSensorReading = false
        accelerometer?.let {
            sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }

        // Auto stop after 30 seconds
        handler.postDelayed(stopRunnable, 30000)

        return START_NOT_STICKY
    }

    private fun playAthanSound() {
        try {
            val prefs = getSharedPreferences("app", Context.MODE_PRIVATE)
            val uriString = prefs.getString("athan_sound_uri", "default")

            val soundUri = if (uriString == null || uriString == "default") {
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            } else {
                Uri.parse(uriString)
            }

            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .build()
                )
                setDataSource(this@AthanService, soundUri)
                prepare()
                setOnCompletionListener {
                    stopAthan()
                }
                start()
            }
        } catch (e: Exception) {
            Log.e("AthanService", "Failed to play Athan sound", e)
            stopAthan()
        }
    }

    private fun createNotification(prayerNameAr: String, isPlaying: Boolean): android.app.Notification {
        val intent = Intent(this, SplashActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(this, "prayer_channel")
            .setSmallIcon(R.drawable.nnbb)
            .setContentTitle("تنبيه دخول وقت الصلاة")
            .setContentText("حان الآن موعد صلاة $prayerNameAr")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        if (isPlaying) {
            builder.setOngoing(true)
            val stopIntent = Intent(this, AthanService::class.java).apply {
                action = ACTION_STOP_ATHAN
            }
            val stopPendingIntent = PendingIntent.getService(
                this, 1, stopIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            builder.addAction(0, "إيقاف", stopPendingIntent)
        } else {
            builder.setOngoing(false)
        }

        return builder.build()
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]

            val gX = x / SensorManager.GRAVITY_EARTH
            val gY = y / SensorManager.GRAVITY_EARTH
            val gZ = z / SensorManager.GRAVITY_EARTH

            // Shake or pickup detection
            val gForce = sqrt((gX * gX + gY * gY + gZ * gZ).toDouble()).toFloat()

            // Flip detection (Z goes negative, meaning screen is facing down)
            if (!hasInitialSensorReading) {
                hasInitialSensorReading = true
                initialZ = gZ
            } else {
                // If it was face up (Z > 0) and now face down (Z < -0.5)
                val flipped = initialZ > 0.5f && gZ < -0.5f
                
                // If picked up / shaken (gForce > 1.3) or flipped
                if (gForce > SHAKE_THRESHOLD || flipped) {
                    Log.d("AthanService", "Stopped by sensor: gForce=$gForce, flipped=$flipped")
                    stopAthan()
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun stopAthan() {
        handler.removeCallbacks(stopRunnable)
        sensorManager?.unregisterListener(this)
        
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
        } catch (e: Exception) {
            Log.e("AthanService", "Error stopping Athan", e)
        }
        
        // Keep the notification but make it dismissable
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, createNotification(lastPrayerNameAr, false))
        
        stopForeground(STOP_FOREGROUND_DETACH)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopAthan()
    }
}
