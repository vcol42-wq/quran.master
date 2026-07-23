package com.sabah.bikhushue

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import com.batoulapps.adhan.CalculationMethod
import com.batoulapps.adhan.Coordinates
import com.batoulapps.adhan.Prayer
import com.batoulapps.adhan.PrayerTimes
import com.batoulapps.adhan.data.DateComponents
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

open class PrayerWidgetProvider : AppWidgetProvider() {

    open fun isNightWidget(): Boolean = true

    companion object {
        const val ACTION_COUNT_SALAWAT = "com.sabah.bikhushue.ACTION_COUNT_SALAWAT"
        const val ACTION_RESET_SALAWAT = "com.sabah.bikhushue.ACTION_RESET_SALAWAT"
        const val ACTION_MINUTE_UPDATE = "com.sabah.bikhushue.ACTION_MINUTE_UPDATE"
        const val ACTION_OPEN_APP = "com.sabah.bikhushue.ACTION_OPEN_APP"
        const val ACTION_THEME_DAY = "com.sabah.bikhushue.ACTION_THEME_DAY"
        const val ACTION_THEME_NIGHT = "com.sabah.bikhushue.ACTION_THEME_NIGHT"
        const val KEY_SALAWAT_COUNT = "salawat_count"
        private const val TAG = "PrayerWidget"
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        Log.d(TAG, "onReceive action: ${intent.action}")

        fun updateAllWidgets() {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            
            // Update Prayer Widgets
            val idsNight = appWidgetManager.getAppWidgetIds(ComponentName(context, PrayerWidgetProvider::class.java))
            val idsDay = appWidgetManager.getAppWidgetIds(ComponentName(context, PrayerWidgetDayProvider::class.java))
            
            val nightProvider = PrayerWidgetProvider()
            val dayProvider = PrayerWidgetDayProvider()
            
            for (id in idsNight) nightProvider.updateAppWidget(context, appWidgetManager, id)
            for (id in idsDay) dayProvider.updateAppWidget(context, appWidgetManager, id)

            // Update Tasbeeh Widgets
            val tasbeehIntent = Intent(context, TasbeehWidgetProvider::class.java).apply { 
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE 
            }
            val tasbeehIds = appWidgetManager.getAppWidgetIds(ComponentName(context, TasbeehWidgetProvider::class.java))
            tasbeehIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, tasbeehIds)
            context.sendBroadcast(tasbeehIntent)
        }

        when (intent.action) {
            ACTION_COUNT_SALAWAT -> {
                val prefs = context.getSharedPreferences("SalawatProgress", Context.MODE_PRIVATE)
                var currentCount = prefs.getInt(KEY_SALAWAT_COUNT, 0)
                var currentRounds = prefs.getInt("salawat_rounds", 0)

                currentCount++
                
                // Logic: check total goal 100
                if (currentCount > 100) {
                    currentCount = 0
                    currentRounds++
                }
                
                prefs.edit()
                    .putInt(KEY_SALAWAT_COUNT, currentCount)
                    .putInt("salawat_rounds", currentRounds)
                    .apply()

                // Notify Activity if it's open
                context.sendBroadcast(Intent("com.sabah.bikhushue.SYNC_SALAWAT").apply {
                    setPackage(context.packageName)
                })

                try {
                    @Suppress("DEPRECATION")
                    val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        vibrator.vibrate(VibrationEffect.createOneShot(35, 120))
                    } else {
                        vibrator.vibrate(35)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Vibration error", e)
                }

                updateAllWidgets()
            }

            ACTION_RESET_SALAWAT -> {
                val prefs = context.getSharedPreferences("SalawatProgress", Context.MODE_PRIVATE)
                prefs.edit()
                    .putInt(KEY_SALAWAT_COUNT, 0)
                    .putInt("salawat_rounds", 0)
                    .apply()

                // Notify Activity if it's open
                context.sendBroadcast(Intent("com.sabah.bikhushue.SYNC_SALAWAT").apply {
                    setPackage(context.packageName)
                })

                try {
                    @Suppress("DEPRECATION")
                    val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK))
                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        vibrator.vibrate(VibrationEffect.createOneShot(50, 200))
                    } else {
                        vibrator.vibrate(50)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Vibration error", e)
                }

                updateAllWidgets()
            }

            ACTION_MINUTE_UPDATE -> {
                updateAllWidgets()
            }

            ACTION_OPEN_APP -> {
                val prefs = context.getSharedPreferences("TasbihCore", Context.MODE_PRIVATE)
                val lastClickTime = prefs.getLong("widget_last_click_time", 0L)
                val now = System.currentTimeMillis()
                if (now - lastClickTime < 800) {
                    try {
                        val openIntent = Intent(context, SplashActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        }
                        context.startActivity(openIntent)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to start Activity directly", e)
                    }
                    prefs.edit().putLong("widget_last_click_time", 0L).apply()
                } else {
                    prefs.edit().putLong("widget_last_click_time", now).apply()
                }
            }
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle?
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        updateAppWidget(context, appWidgetManager, appWidgetId)
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        Log.d(TAG, "onUpdate: ${appWidgetIds.size} widgets")
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
        scheduleNextUpdate(context)
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        Log.d(TAG, "onEnabled")
        scheduleNextUpdate(context)
    }

    private fun scheduleNextUpdate(context: Context) {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, javaClass).apply {
                action = ACTION_MINUTE_UPDATE
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val nextUpdate = Calendar.getInstance()
            nextUpdate.add(Calendar.MINUTE, 1)
            nextUpdate.set(Calendar.SECOND, 0)
            nextUpdate.set(Calendar.MILLISECOND, 0)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        nextUpdate.timeInMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        nextUpdate.timeInMillis,
                        pendingIntent
                    )
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    nextUpdate.timeInMillis,
                    pendingIntent
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Schedule error", e)
        }
    }

    fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val views = RemoteViews(context.packageName, R.layout.prayer_widget)

        try {
            val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
            val minHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 100)

            // Responsive Layout Logic
            if (minHeight < 90) {
                // Level 1: Only prayers
                views.setViewVisibility(R.id.layout_widget_salawat, View.GONE)
                views.setViewVisibility(R.id.layout_widget_info, View.GONE)
            } else if (minHeight < 150) {
                // Level 2: Prayers + Salawat
                views.setViewVisibility(R.id.layout_widget_salawat, View.VISIBLE)
                views.setViewVisibility(R.id.layout_widget_info, View.GONE)
            } else if (minHeight < 220) {
                // Level 3: Prayers + Salawat + Info
                views.setViewVisibility(R.id.layout_widget_salawat, View.VISIBLE)
                views.setViewVisibility(R.id.layout_widget_info, View.VISIBLE)
            } else {
                // Level 4: Full (Header added)
                views.setViewVisibility(R.id.layout_widget_salawat, View.VISIBLE)
                views.setViewVisibility(R.id.layout_widget_info, View.VISIBLE)
            }

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

            val formatter = SimpleDateFormat("hh:mm", Locale.US)
            fun getAdjusted(time: Date?, prayerName: String): String {
                if (time == null) return "--:--"
                val offset = prefsCore.getInt("offset_${prayerName}", 0)
                val cal = Calendar.getInstance()
                    .apply { timeInMillis = time.time; add(Calendar.MINUTE, offset) }
                return formatter.format(cal.time)
            }

            // Dynamic background based on theme
            val isNightOrLunar = isNightWidget()
            val colorWhite = if (isNightOrLunar) Color.WHITE else Color.parseColor("#212121") // Dark text for Day
            val colorGold = if (isNightOrLunar) Color.parseColor("#FEF3C7") else Color.parseColor("#5D4037") // Brown text for Day

            // Reset and Set Times
            views.setTextColor(R.id.tv_fajr_label, colorGold)
            views.setTextColor(R.id.tv_fajr_time, colorWhite)
            views.setTextColor(R.id.tv_dhuhr_label, colorGold)
            views.setTextColor(R.id.tv_dhuhr_time, colorWhite)
            views.setTextColor(R.id.tv_asr_label, colorGold)
            views.setTextColor(R.id.tv_asr_time, colorWhite)
            views.setTextColor(R.id.tv_maghrib_label, colorGold)
            views.setTextColor(R.id.tv_maghrib_time, colorWhite)
            views.setTextColor(R.id.tv_isha_label, colorGold)
            views.setTextColor(R.id.tv_isha_time, colorWhite)
            
            // Add missing Sunrise colors
            views.setTextColor(R.id.tv_sunrise_time, colorWhite)
            
            // Missing label for next prayer
            views.setTextColor(R.id.tv_next_prayer_title_label, colorGold)

            views.setTextViewText(R.id.tv_fajr_time, getAdjusted(prayerTimes.fajr, "FAJR"))
            views.setTextViewText(R.id.tv_sunrise_time, getAdjusted(prayerTimes.sunrise, "SUNRISE"))
            views.setTextViewText(R.id.tv_dhuhr_time, getAdjusted(prayerTimes.dhuhr, "DHUHR"))
            views.setTextViewText(R.id.tv_asr_time, getAdjusted(prayerTimes.asr, "ASR"))
            views.setTextViewText(R.id.tv_maghrib_time, getAdjusted(prayerTimes.maghrib, "MAGHRIB"))
            views.setTextViewText(R.id.tv_isha_time, getAdjusted(prayerTimes.isha, "ISHA"))

            val currentPrayer = prayerTimes.currentPrayer()
            val activeCurrent = if (currentPrayer == Prayer.NONE) {
                val now = Calendar.getInstance()
                val fajrCal = Calendar.getInstance().apply { time = prayerTimes.fajr }
                if (now.before(fajrCal)) {
                    Prayer.ISHA
                } else {
                    Prayer.NONE
                }
            } else {
                currentPrayer
            }
            val colorCurrentHighlight = if (isNightOrLunar) Color.parseColor("#00FFCC") else Color.parseColor("#1B5E20") // Dark green for Day
            when (activeCurrent) {
                Prayer.FAJR -> {
                    views.setTextColor(R.id.tv_fajr_label, colorCurrentHighlight)
                    views.setTextColor(R.id.tv_fajr_time, colorCurrentHighlight)
                }
                Prayer.DHUHR -> {
                    views.setTextColor(R.id.tv_dhuhr_label, colorCurrentHighlight)
                    views.setTextColor(R.id.tv_dhuhr_time, colorCurrentHighlight)
                }
                Prayer.ASR -> {
                    views.setTextColor(R.id.tv_asr_label, colorCurrentHighlight)
                    views.setTextColor(R.id.tv_asr_time, colorCurrentHighlight)
                }
                Prayer.MAGHRIB -> {
                    views.setTextColor(R.id.tv_maghrib_label, colorCurrentHighlight)
                    views.setTextColor(R.id.tv_maghrib_time, colorCurrentHighlight)
                }
                Prayer.ISHA -> {
                    views.setTextColor(R.id.tv_isha_label, colorCurrentHighlight)
                    views.setTextColor(R.id.tv_isha_time, colorCurrentHighlight)
                }
                else -> {}
            }

            val cityName = prefsApp.getString("athan_city_name", null) 
                ?: prefsCore.getString("user_city_name", "الرياض")
            views.setTextViewText(R.id.tv_widget_city, cityName)
            views.setTextColor(R.id.tv_widget_city, colorWhite)

            val currentSalawatCount = context.getSharedPreferences("SalawatProgress", Context.MODE_PRIVATE)
                .getInt(KEY_SALAWAT_COUNT, 0)
            val currentRounds = context.getSharedPreferences("SalawatProgress", Context.MODE_PRIVATE)
                .getInt("salawat_rounds", 0)

            views.setTextViewText(R.id.tv_widget_salawat_counter, currentSalawatCount.toString())
            views.setTextColor(R.id.tv_widget_salawat_counter, colorGold)

            views.setTextViewText(R.id.tv_widget_rounds, currentRounds.toString())
            views.setTextColor(R.id.tv_widget_rounds, colorGold)

            val is24 = android.text.format.DateFormat.is24HourFormat(context)
            val timeFormat = SimpleDateFormat(if (is24) "HH:mm" else "h:mm", Locale.ENGLISH)
            views.setTextViewText(R.id.widget_clock, timeFormat.format(Date()))
            views.setTextColor(R.id.widget_clock, colorWhite)

            views.setTextColor(R.id.tv_motivational_msg, colorWhite)

            var nextPrayer = prayerTimes.nextPrayer()
            
            // Skip Sunrise and point to Dhuhr instead
            if (nextPrayer == Prayer.SUNRISE) {
                nextPrayer = Prayer.DHUHR
            }

            if (nextPrayer != Prayer.NONE) {
                val nextTime = prayerTimes.timeForPrayer(nextPrayer)
                val offset = prefsCore.getInt("offset_${nextPrayer.name}", 0)
                val adjustedNextTime = nextTime.time + (offset * 60 * 1000)
                val diff = adjustedNextTime - Date().time
                val h = TimeUnit.MILLISECONDS.toHours(diff)
                val m = TimeUnit.MILLISECONDS.toMinutes(diff) % 60

                val nameAr = when (nextPrayer) {
                    Prayer.FAJR -> "الفجر"
                    Prayer.DHUHR -> "الظهر"
                    Prayer.ASR -> "العصر"
                    Prayer.MAGHRIB -> "المغرب"
                    Prayer.ISHA -> "العشاء"
                    else -> "الصلاة"
                }
                views.setTextViewText(
                    R.id.tv_next_prayer_name_info,
                    nameAr
                )
                views.setTextColor(R.id.tv_next_prayer_name_info, colorWhite)
                views.setTextViewText(
                    R.id.tv_remaining_time,
                    String.format(Locale.US, "%02d:%02d", h, m)
                )
                views.setTextColor(R.id.tv_remaining_time, colorGold)
            }

            // Click Intents
            val providerClass = if (isNightWidget()) PrayerWidgetProvider::class.java else PrayerWidgetDayProvider::class.java
            
            val clickIntent = Intent(context, providerClass).apply {
                action = ACTION_COUNT_SALAWAT
            }
            val clickPendingIntent = PendingIntent.getBroadcast(
                context,
                1,
                clickIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )
            // Making the entire root clickable for counting!
            views.setOnClickPendingIntent(R.id.widget_root, clickPendingIntent)

            val appIntent = Intent(context, providerClass).apply { 
                action = ACTION_OPEN_APP 
            }
            val appPendingIntent = PendingIntent.getBroadcast(
                context,
                2,
                appIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            // Element that opens the app instead of counting
            views.setOnClickPendingIntent(R.id.view_center_open_app, appPendingIntent)

            val resetIntent = Intent(context, providerClass).apply {
                action = ACTION_RESET_SALAWAT
            }
            val resetPendingIntent = PendingIntent.getBroadcast(
                context,
                3,
                resetIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )
            views.setOnClickPendingIntent(R.id.iv_widget_reset, resetPendingIntent)
            
            if (isNightOrLunar) {
                views.setImageViewResource(R.id.iv_widget_bg_mosque, R.drawable.rfrf)
            } else {
                views.setImageViewResource(R.id.iv_widget_bg_mosque, R.drawable.pop)
            }

        } catch (e: Exception) {
            Log.e(TAG, "Update error", e)
            views.setTextViewText(R.id.tv_motivational_msg, "افتح التطبيق لتحديث المواقيت")
        }

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    private fun getCalcMethodParameters(index: Int): com.batoulapps.adhan.CalculationParameters {
        return when (index) {
            0 -> CalculationMethod.UMM_AL_QURA.parameters
            1 -> CalculationMethod.MUSLIM_WORLD_LEAGUE.parameters
            2 -> CalculationMethod.EGYPTIAN.parameters
            3 -> CalculationMethod.KARACHI.parameters
            4 -> CalculationMethod.NORTH_AMERICA.parameters
            5 -> CalculationMethod.DUBAI.parameters
            6 -> CalculationMethod.QATAR.parameters
            7 -> CalculationMethod.KUWAIT.parameters
            8 -> CalculationMethod.SINGAPORE.parameters
            9 -> CalculationMethod.MUSLIM_WORLD_LEAGUE.parameters
            10 -> {
                val params = CalculationMethod.OTHER.parameters
                params.fajrAngle = 17.7
                params.ishaAngle = 14.0
                params
            }
            else -> CalculationMethod.UMM_AL_QURA.parameters
        }
    }
}
