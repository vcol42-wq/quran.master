package com.sabah.bikhushue

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.widget.RemoteViews
import com.batoulapps.adhan.CalculationMethod
import com.batoulapps.adhan.Coordinates
import com.batoulapps.adhan.data.DateComponents
import com.batoulapps.adhan.Prayer
import com.batoulapps.adhan.PrayerTimes
import java.text.SimpleDateFormat
import java.util.*

class PrayerSmallWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val views = RemoteViews(context.packageName, R.layout.prayer_widget_small)

        try {
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

            val formatter = SimpleDateFormat("hh:mm a", Locale.US)
            val nextPrayer = prayerTimes.nextPrayer()
            
            if (nextPrayer != Prayer.NONE) {
                var nextTime = prayerTimes.timeForPrayer(nextPrayer)
                val offset = prefsCore.getInt("offset_${nextPrayer.name}", 0)
                nextTime = Date(nextTime.time + (offset * 60 * 1000))
                
                val nameAr = when (nextPrayer.name) {
                    "FAJR" -> "الفجر"
                    "DHUHR" -> "الظهر"
                    "ASR" -> "العصر"
                    "MAGHRIB" -> "المغرب"
                    "ISHA" -> "العشاء"
                    else -> "الصلاة"
                }
                
                views.setTextViewText(R.id.tv_next_prayer_name, nameAr)
                views.setTextViewText(R.id.tv_next_prayer_time, formatter.format(nextTime))
            } else {
                views.setTextViewText(R.id.tv_next_prayer_name, "الفجر")
                // Tomorrow's Fajr
                val tomorrow = Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, 1) }
                val tomorrowPrayers = PrayerTimes(coordinates, DateComponents.from(tomorrow.time), params)
                var nextTime = tomorrowPrayers.fajr
                val offset = prefsCore.getInt("offset_FAJR", 0)
                nextTime = Date(nextTime.time + (offset * 60 * 1000))
                views.setTextViewText(R.id.tv_next_prayer_time, formatter.format(nextTime))
            }

            val intent = Intent(context, SplashActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_small_root, pendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getCalcMethodParameters(index: Int): com.batoulapps.adhan.CalculationParameters {
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
