package com.sabah.bikhushue

import android.graphics.Color
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan

object AutoTajweedParser {

    fun parse(text: String, isDarkMode: Boolean): SpannableStringBuilder {
        val ssb = SpannableStringBuilder(text)
        
        // Define Professional Tajweed Colors based on Theme
        val colorMadd     = if (isDarkMode) Color.parseColor("#FF8A80") else Color.parseColor("#C62828") // Red for Madd
        val colorGhunna   = if (isDarkMode) Color.parseColor("#B9F6CA") else Color.parseColor("#1B5E20") // Green for Ghunnah/Ikhfa/Iqlab
        val colorQalqalah = if (isDarkMode) Color.parseColor("#82B1FF") else Color.parseColor("#0D47A1") // Blue for Qalqalah
        val colorSilent   = if (isDarkMode) Color.parseColor("#9E9E9E") else Color.parseColor("#757575") // Gray for Idgham Bila Ghunnah
        val colorOrange   = if (isDarkMode) Color.parseColor("#FFD180") else Color.parseColor("#E65100") // Orange for Idgham Bighunnah

        val qalqalahLetters = listOf('ق', 'ط', 'ب', 'ج', 'د')
        val idghamBighunnah = listOf('ي', 'ن', 'م', 'و')
        val idghamBilaGhunnah = listOf('ل', 'ر')
        val ikhfaLetters = listOf('ص', 'ذ', 'ث', 'ك', 'ج', 'ش', 'ق', 'س', 'د', 'ط', 'ز', 'ف', 'ت', 'ض', 'ظ')
        
        var i = 0
        while (i < text.length) {
            val c = text[i]
            
            // 1. Madd (\u0653 or \u06E4)
            if (c == '\u0653' || c == '\u06E4') {
                // Color the previous letter and this madd
                var start = i - 1
                while (start >= 0 && isDiacritic(text[start])) start--
                if (start >= 0) {
                    ssb.setSpan(ForegroundColorSpan(colorMadd), start, i + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
            }
            
            // 2. Qalqalah (Sukoon \u0652 or \u06E1)
            else if (c == '\u0652' || c == '\u06E1') {
                var start = i - 1
                while (start >= 0 && isDiacritic(text[start])) start--
                if (start >= 0 && qalqalahLetters.contains(text[start])) {
                    ssb.setSpan(ForegroundColorSpan(colorQalqalah), start, i + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
            }
            
            // 3. Ghunnah (Shadda \u0651 on Meem or Nun)
            else if (c == '\u0651') {
                var start = i - 1
                while (start >= 0 && isDiacritic(text[start])) start--
                if (start >= 0 && (text[start] == 'ن' || text[start] == 'م')) {
                    ssb.setSpan(ForegroundColorSpan(colorGhunna), start, i + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
            }
            
            // 4. Iqlab (Small Meem \u06E2)
            else if (c == '\u06E2') {
                var start = i - 1
                while (start >= 0 && isDiacritic(text[start])) start--
                if (start >= 0) {
                    ssb.setSpan(ForegroundColorSpan(colorGhunna), start, i + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
            }

            // 5. Nun Sakinah or Tanween (Idgham / Ikhfa)
            val isTanween = c == '\u064B' || c == '\u064C' || c == '\u064D'
            var isNunSakinah = false
            if (c == 'ن') {
                // Check if it has a vowel (Fatha, Damma, Kasra)
                var hasVowel = false
                var j = i + 1
                while (j < text.length && isDiacritic(text[j])) {
                    if (text[j] == '\u064E' || text[j] == '\u064F' || text[j] == '\u0650') {
                        hasVowel = true
                        break
                    }
                    j++
                }
                if (!hasVowel) isNunSakinah = true
            }

            if (isTanween || isNunSakinah) {
                // Find next actual letter
                var nextChar = ' '
                var j = i + 1
                while (j < text.length) {
                    val n = text[j]
                    // Ignore spaces, diacritics, end of ayah markers (\u06dd), and Alif Wasla (\u0671) or plain Alif (\u0627)
                    if (!isDiacritic(n) && n != ' ' && n != '\u06dd' && n != '\u0671' && n != '\u0627') {
                        nextChar = n
                        break
                    }
                    j++
                }
                
                if (nextChar != ' ') {
                    var targetColor = -1
                    if (idghamBighunnah.contains(nextChar)) targetColor = colorOrange
                    else if (idghamBilaGhunnah.contains(nextChar)) targetColor = colorSilent
                    else if (ikhfaLetters.contains(nextChar)) targetColor = colorGhunna
                    else if (nextChar == 'ب') targetColor = colorGhunna // Iqlab backup
                    
                    if (targetColor != -1) {
                        var start = if (isTanween) {
                            var s = i - 1
                            while (s >= 0 && isDiacritic(text[s])) s--
                            if (s >= 0) s else i
                        } else {
                            i
                        }
                        
                        ssb.setSpan(ForegroundColorSpan(targetColor), start, i + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                    }
                }
            }
            
            i++
        }
        return ssb
    }
    
    private fun isDiacritic(c: Char): Boolean {
        // Includes all standard Arabic diacritics, sukoon, maddah, and small letters
        return c in '\u064B'..'\u065F' || c == '\u06E1' || c == '\u0670' || c == '\u0653' || c == '\u06E4' || c == '\u06E2'
    }
}
