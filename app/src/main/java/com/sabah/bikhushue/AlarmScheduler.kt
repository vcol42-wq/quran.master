package com.sabah.bikhushue

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.batoulapps.adhan.CalculationMethod
import com.batoulapps.adhan.CalculationParameters
import com.batoulapps.adhan.Coordinates
import com.batoulapps.adhan.data.DateComponents
import com.batoulapps.adhan.Prayer
import com.batoulapps.adhan.PrayerTimes
import java.util.*

object AlarmScheduler {
    private const val TAG = "AlarmScheduler"

    fun scheduleAlarms(context: Context) {
        val prefsApp = context.getSharedPreferences("app", Context.MODE_PRIVATE)
        val prefsCore = context.getSharedPreferences("TasbihCore", Context.MODE_PRIVATE)

        var lat = prefsApp.getFloat("athan_latitude", 0.0f).toDouble()
        var lon = prefsApp.getFloat("athan_longitude", 0.0f).toDouble()

        if (lat == 0.0 && lon == 0.0) {
            lat = prefsCore.getFloat("location_lat", 21.4225f).toDouble()
            lon = prefsCore.getFloat("location_lon", 39.8262f).toDouble()
        }
        if (lat == 0.0 && lon == 0.0) {
            lat = 24.7136
            lon = 46.6753
        }

        val methodIdx = prefsApp.getInt("athan_calc_method", -1)
        val params = if (methodIdx != -1) {
            getCalcMethodParameters(methodIdx)
        } else {
            val coreMethodIdx = prefsCore.getInt("calculation_method", 3)
            val methods = arrayOf(
                CalculationMethod.KARACHI, CalculationMethod.NORTH_AMERICA,
                CalculationMethod.EGYPTIAN, CalculationMethod.UMM_AL_QURA,
                CalculationMethod.DUBAI, CalculationMethod.KUWAIT, CalculationMethod.QATAR
            )
            val method = if (coreMethodIdx in methods.indices) methods[coreMethodIdx] else CalculationMethod.UMM_AL_QURA
            method.parameters
        }

        val coordinates = Coordinates(lat, lon)
        val dateComponents = DateComponents.from(Date())
        val prayerTimes = PrayerTimes(coordinates, dateComponents, params)

        val nextPrayer = prayerTimes.nextPrayer()
        if (nextPrayer != Prayer.NONE) {
            var nextTime = prayerTimes.timeForPrayer(nextPrayer)
            val offset = prefsCore.getInt("offset_${nextPrayer.name}", 0)
            nextTime = Date(nextTime.time + (offset * 60 * 1000))
            scheduleExactAlarm(context, nextTime.time, 100, Intent(context, PrayerNotificationReceiver::class.java).apply {
                putExtra("prayer_name", nextPrayer.name)
            })
        }

        val fajrTime = prayerTimes.fajr
        val offsetFajr = prefsCore.getInt("offset_FAJR", 0)
        val morningAzkarTime = Date(fajrTime.time + (offsetFajr * 60 * 1000) + (2 * 60 * 60 * 1000)) // Fajr + 2 hours
        
        if (morningAzkarTime.time > System.currentTimeMillis()) {
            scheduleExactAlarm(context, morningAzkarTime.time, 201, Intent(context, AzkarAlarmReceiver::class.java).apply {
                putExtra("is_morning", true)
            })
        } else {
            // Schedule for tomorrow
            val tomorrow = Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, 1) }
            val tomorrowPrayers = PrayerTimes(coordinates, DateComponents.from(tomorrow.time), params)
            val tomorrowAzkarTime = Date(tomorrowPrayers.fajr.time + (offsetFajr * 60 * 1000) + (2 * 60 * 60 * 1000))
            scheduleExactAlarm(context, tomorrowAzkarTime.time, 201, Intent(context, AzkarAlarmReceiver::class.java).apply {
                putExtra("is_morning", true)
            })
        }

        val asrTime = prayerTimes.asr
        val offsetAsr = prefsCore.getInt("offset_ASR", 0)
        val eveningAzkarTime = Date(asrTime.time + (offsetAsr * 60 * 1000) + (15 * 60 * 1000)) // Asr + 15 mins
        
        if (eveningAzkarTime.time > System.currentTimeMillis()) {
            scheduleExactAlarm(context, eveningAzkarTime.time, 202, Intent(context, AzkarAlarmReceiver::class.java).apply {
                putExtra("is_morning", false)
            })
        } else {
            // Schedule for tomorrow
            val tomorrow = Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, 1) }
            val tomorrowPrayers = PrayerTimes(coordinates, DateComponents.from(tomorrow.time), params)
            val tomorrowAzkarTime = Date(tomorrowPrayers.asr.time + (offsetAsr * 60 * 1000) + (15 * 60 * 1000))
            scheduleExactAlarm(context, tomorrowAzkarTime.time, 202, Intent(context, AzkarAlarmReceiver::class.java).apply {
                putExtra("is_morning", false)
            })
        }
    }

    private fun scheduleExactAlarm(context: Context, timeInMillis: Long, requestCode: Int, intent: Intent) {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timeInMillis, pendingIntent)
                } else {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timeInMillis, pendingIntent)
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timeInMillis, pendingIntent)
            }
            Log.d(TAG, "Scheduled alarm with requestCode $requestCode for time $timeInMillis")
        } catch (e: Exception) {
            Log.e(TAG, "Schedule error", e)
        }
    }

    private fun getCalcMethodParameters(index: Int): CalculationParameters {
        val methods = arrayOf(
            CalculationMethod.MUSLIM_WORLD_LEAGUE,
            CalculationMethod.EGYPTIAN,
            CalculationMethod.KARACHI,
            CalculationMethod.UMM_AL_QURA,
            CalculationMethod.DUBAI,
            CalculationMethod.QATAR,
            CalculationMethod.KUWAIT,
            CalculationMethod.MOON_SIGHTING_COMMITTEE,
            CalculationMethod.SINGAPORE,
            CalculationMethod.NORTH_AMERICA,
            CalculationMethod.OTHER
        )
        val method = if (index in methods.indices) methods[index] else CalculationMethod.UMM_AL_QURA
        return method.parameters
    }
}
