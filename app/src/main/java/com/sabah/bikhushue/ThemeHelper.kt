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
            "#121212", "#455A64", "#37474F", "#263238", "#581825", "#4A0E17", "#800020" -> true
            else -> false
        }

        val isLunar = bgHex == "#455A64" || bgHex == "#37474F" || bgHex == "#263238"
        val isBurgundy = bgHex == "#581825" || bgHex == "#4A0E17" || bgHex == "#800020"
        
        // Define gradients/shades based on theme
        val cardBgHex = when (bgHex) {
            "#121212" -> "#2D2D2D" // Night
            "#455A64", "#37474F", "#263238", "#D4CEC4" -> if (isLunar) "#263238" else "#37474F"
            "#581825", "#4A0E17", "#800020" -> "#421019" // Burgundy card bg
            "#E0F2F1" -> "#E8F5E9" // Emerald light tint
            "#E3F2FD" -> "#E1F5FE" // Sky Blue light tint
            "#FFF0F5", "#FCE4EC" -> "#FCE4EC" // Pink light tint
            "#FBF3F4" -> "#F8BBD0" // Crimson light tint
            else -> "#FFFFFF"      // Creamy / Default
        }

        val strokeHex = when (bgHex) {
            "#121212" -> "#555555" // Night
            "#455A64", "#37474F", "#263238", "#D4CEC4" -> "#78909C" // Lunar stroke
            "#581825", "#4A0E17", "#800020" -> "#D4AF37" // Burgundy stroke (Gold)
            "#E0F2F1" -> "#4DB6AC" // Emerald stroke
            "#E3F2FD" -> "#64B5F6" // Sky Blue stroke
            "#FFF0F5", "#FCE4EC" -> "#F06292" // Pink stroke
            "#FBF3F4" -> "#C2185B" // Crimson stroke
            else -> "#D2B48C"      // Creamy stroke
        }

        val shadowHex = when (bgHex) {
            "#121212" -> "#000000" // Night shadow
            "#455A64", "#37474F", "#263238", "#D4CEC4" -> "#263238" // Lunar shadow
            "#581825", "#4A0E17", "#800020" -> "#2A050B" // Burgundy shadow
            "#E0F2F1" -> "#80CBC4" // Emerald shadow
            "#E3F2FD" -> "#90CAF9" // Sky Blue shadow
            "#FFF0F5", "#FCE4EC" -> "#F48FB1" // Pink shadow
            "#FBF3F4" -> "#D38C9B" // Crimson shadow
            else -> "#C5A059"      // Creamy shadow
        }

        val dropdownBgHex = when {
            bgHex == "#121212" -> "#1E1E1E" // Night dark dropdown
            isBurgundy -> "#3E0F19" // Burgundy dropdown
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
        if (android.os.Build.VERSION.SDK_INT < 35) {
            @Suppress("DEPRECATION")
            activity.window.statusBarColor = theme.bg
            @Suppress("DEPRECATION")
            activity.window.navigationBarColor = theme.bar
        }
        
        val controller = WindowInsetsControllerCompat(activity.window, activity.window.decorView)
        controller.isAppearanceLightStatusBars = !theme.isDark
        controller.isAppearanceLightNavigationBars = !theme.isDark
    }
}
