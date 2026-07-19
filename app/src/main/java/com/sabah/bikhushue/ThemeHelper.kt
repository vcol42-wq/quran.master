package com.sabah.bikhushue

import android.content.Context
import android.graphics.Color

data class ThemeColors(
    val bg: Int,
    val txt: Int,
    val bar: Int,
    val cardBg: Int,
    val stroke: Int,
    val shadow: Int,
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
        
        // Define gradients/shades based on theme
        val cardBgHex = when (bgHex) {
            "#121212" -> "#2D2D2D" // Night
            "#455A64", "#37474F", "#263238", "#D4CEC4" -> "#37474F" // Lunar light tint (card)
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
            "#455A64", "#37474F", "#263238", "#D4CEC4" -> "#A4B2C6" // Lunar shadow (creamy with a touch of dark blue)
            "#E0F2F1" -> "#B2DFDB" // Emerald shadow
            "#E3F2FD" -> "#BBDEFB" // Sky Blue shadow
            "#FFF0F5" -> "#F8BBD0" // Pink shadow
            "#FBF3F4" -> "#E8B8C1" // Crimson shadow
            else -> "#D2B48C"      // Creamy shadow
        }

        return ThemeColors(
            bg = Color.parseColor(bgHex),
            txt = if (isDark) Color.parseColor("#E0E0E0") else Color.parseColor(txtHex),
            bar = if (isDark) Color.parseColor("#2D2D2D") else Color.parseColor(barHex),
            cardBg = Color.parseColor(cardBgHex),
            stroke = Color.parseColor(strokeHex),
            shadow = Color.parseColor(shadowHex),
            isDark = isDark
        )
    }
}
