package com.sabah.bikhushue

import android.app.Activity
import android.content.Context
import android.graphics.Color
import androidx.core.view.WindowInsetsControllerCompat

data class ThemeColors(
    val bg: Int,
    val txt: Int,
    val bar: Int,
    val cardBg: Int,
    val stroke: Int,
    val shadow: Int,
    val dropdownBg: Int,
    val isDark: Boolean
)

object ThemeHelper {
    fun getThemeColors(context: Context): ThemeColors {
        val prefs = context.getSharedPreferences("app", Context.MODE_PRIVATE)
        val bgHex = prefs.getString("bg_color", "#121212") ?: "#121212"
        val txtHex = prefs.getString("txt_color", "#212121") ?: "#212121"
        val barHex = prefs.getString("bar_color", "#F9F6F0") ?: "#F9F6F0"
        
        val isDark = when (bgHex) {
            "#121212", "#455A64", "#37474F", "#263238" -> true
            else -> false
        }

        val isLunar = bgHex == "#455A64" || bgHex == "#37474F" || bgHex == "#263238"
        
        // Define gradients/shades based on theme
        val cardBgHex = when (bgHex) {
            "#121212" -> "#2D2D2D" // Night
            "#455A64", "#37474F", "#263238", "#D4CEC4" -> if (isLunar) "#263238" else "#37474F"
            "#E0F2F1" -> "#F1F8F8" // Emerald light tint
            "#E3F2FD" -> "#F0F8FF" // Sky Blue light tint
            "#FFF0F5" -> "#FFF5F8" // Pink light tint
            "#FBF3F4" -> "#FCF6F7" // Crimson light tint
            else -> "#FFFFFF"      // Creamy / Default
        }

        val strokeHex = when (bgHex) {
            "#121212" -> "#555555" // Night
            "#455A64", "#37474F", "#263238", "#D4CEC4" -> "#78909C" // Lunar stroke
            "#E0F2F1" -> "#80CBC4" // Emerald stroke
            "#E3F2FD" -> "#90CAF9" // Sky Blue stroke
            "#FFF0F5" -> "#F48FB1" // Pink stroke
            "#FBF3F4" -> "#D38C9B" // Crimson stroke
            else -> "#E4D7B4"      // Creamy stroke
        }

        val shadowHex = when (bgHex) {
            "#121212" -> "#000000" // Night shadow
            "#455A64", "#37474F", "#263238", "#D4CEC4" -> "#A4B2C6" // Lunar shadow
            "#E0F2F1" -> "#B2DFDB" // Emerald shadow
            "#E3F2FD" -> "#BBDEFB" // Sky Blue shadow
            "#FFF0F5" -> "#F8BBD0" // Pink shadow
            "#FBF3F4" -> "#E8B8C1" // Crimson shadow
            else -> "#D2B48C"      // Creamy shadow
        }

        val dropdownBgHex = when {
            bgHex == "#121212" -> "#1E1E1E" // Night dark dropdown
            isLunar -> "#1C262B" // Lunar even darker dropdown
            else -> "#FFFFFF" // Force white for light themes to avoid creamy clash
        }

        val textColorHex = when {
            isDark -> "#E0E0E0"
            else -> txtHex
        }

        return ThemeColors(
            bg = Color.parseColor(bgHex),
            txt = Color.parseColor(textColorHex),
            bar = if (isDark) Color.parseColor("#2D2D2D") else Color.parseColor(barHex),
            cardBg = Color.parseColor(cardBgHex),
            stroke = Color.parseColor(strokeHex),
            shadow = Color.parseColor(shadowHex),
            dropdownBg = Color.parseColor(dropdownBgHex),
            isDark = isDark
        )
    }

    fun applySystemWindowsColors(activity: Activity) {
        val theme = getThemeColors(activity)
        activity.window.statusBarColor = theme.bg
        activity.window.navigationBarColor = theme.bar
        
        val controller = WindowInsetsControllerCompat(activity.window, activity.window.decorView)
        controller.isAppearanceLightStatusBars = !theme.isDark
        controller.isAppearanceLightNavigationBars = !theme.isDark
    }
}
