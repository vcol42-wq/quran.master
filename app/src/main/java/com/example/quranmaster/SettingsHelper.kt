package com.example.quranmaster

import android.content.Context
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.switchmaterial.SwitchMaterial

object SettingsHelper {

    fun showSettingsDialog(
        activity: AppCompatActivity,
        onThemeChanged: () -> Unit,
        onTajweedChanged: ((Boolean) -> Unit)? = null
    ) {
        val dialog = BottomSheetDialog(activity)
        val view = activity.layoutInflater.inflate(R.layout.dialog_settings, null)
        dialog.setContentView(view)

        val spinnerTheme = view.findViewById<Spinner>(R.id.spinnerTheme)
        val switchTajweed = view.findViewById<SwitchMaterial>(R.id.switchTajweed)
        val switchVibration = view.findViewById<SwitchMaterial>(R.id.switchVibration)
        val switchSound = view.findViewById<SwitchMaterial>(R.id.switchSound)
        val switchAzan = view.findViewById<SwitchMaterial>(R.id.switchAzan)
        val etApiKey = view.findViewById<EditText>(R.id.etApiKey)
        val btnSave = view.findViewById<android.widget.TextView>(R.id.btnSaveSettings)

        val prefs = activity.getSharedPreferences("app", Context.MODE_PRIVATE)
        
        // Theme Setup
        val themes = arrayOf("كريمي", "زمردي", "سماوي", "ليلي")
        spinnerTheme.adapter = ArrayAdapter(activity, android.R.layout.simple_spinner_dropdown_item, themes)
        
        val currentBg = prefs.getString("bg_color", "#F4ECD8")
        spinnerTheme.setSelection(when (currentBg) {
            "#E0F2F1" -> 1
            "#E3F2FD" -> 2
            "#121212" -> 3
            else -> 0
        })

        spinnerTheme.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, v: View?, position: Int, id: Long) {
                val newBg = when (position) {
                    1 -> "#E0F2F1"
                    2 -> "#E3F2FD"
                    3 -> "#121212"
                    else -> "#F4ECD8"
                }
                if (newBg != prefs.getString("bg_color", "#F4ECD8")) {
                    when (position) {
                        0 -> applyThemeToPrefs(prefs, "#F4ECD8", "#000000", "#E6DCC8")
                        1 -> applyThemeToPrefs(prefs, "#E0F2F1", "#004D40", "#B2DFDB")
                        2 -> applyThemeToPrefs(prefs, "#E3F2FD", "#0D47A1", "#BBDEFB")
                        3 -> applyThemeToPrefs(prefs, "#121212", "#E0E0E0", "#1E1E1E")
                    }
                    onThemeChanged()
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Switches Setup
        switchTajweed.isChecked = prefs.getBoolean("tajweed_on", true)
        switchTajweed.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("tajweed_on", isChecked).apply()
            onTajweedChanged?.invoke(isChecked)
        }

        switchVibration.isChecked = prefs.getBoolean("vibration_on", true)
        switchVibration.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("vibration_on", isChecked).apply()
        }

        switchSound.isChecked = prefs.getBoolean("sound_on", true)
        switchSound.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("sound_on", isChecked).apply()
        }

        switchAzan.isChecked = prefs.getBoolean("azan_on", true)
        switchAzan.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("azan_on", isChecked).apply()
        }

        // API Key
        etApiKey.setText(prefs.getString("api", ""))



        // Save Button
        btnSave.setOnClickListener {
            prefs.edit().putString("api", etApiKey.text.toString().trim()).apply()
            dialog.dismiss()
        }

        val btnPrivacyPolicy = view.findViewById<android.widget.TextView>(R.id.btnPrivacyPolicy)
        btnPrivacyPolicy?.setOnClickListener {
            val webView = android.webkit.WebView(activity).apply {
                loadUrl("file:///android_asset/privacy_policy.html")
            }
            androidx.appcompat.app.AlertDialog.Builder(activity)
                .setTitle("سياسة الخصوصية")
                .setView(webView)
                .setPositiveButton("حسنا", null)
                .show()
        }

        dialog.show()
    }

    private fun applyThemeToPrefs(prefs: android.content.SharedPreferences, bg: String, txt: String, bar: String) {
        prefs.edit()
            .putString("bg_color", bg)
            .putString("txt_color", txt)
            .putString("bar_color", bar)
            .apply()
    }


}
