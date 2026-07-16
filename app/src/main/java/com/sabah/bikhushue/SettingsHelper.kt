package com.sabah.bikhushue

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
        val themes = arrayOf("قمري", "كريمي", "زمردي", "سماوي", "ليلي", "زهري", "قرمزي")
        spinnerTheme.adapter = ArrayAdapter(activity, android.R.layout.simple_spinner_dropdown_item, themes)
        
        val currentBg = prefs.getString("bg_color", "#455A64")
        spinnerTheme.setSelection(when (currentBg) {
            "#455A64", "#37474F", "#263238", "#D4CEC4" -> 0
            "#FDFBF7" -> 1
            "#E0F2F1" -> 2
            "#E3F2FD" -> 3
            "#121212" -> 4
            "#FFF0F5" -> 5
            "#FBF3F4" -> 6
            else -> 0
        })

        spinnerTheme.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, v: View?, position: Int, id: Long) {
                val newBg = when (position) {
                    0 -> "#455A64"
                    1 -> "#FDFBF7"
                    2 -> "#E0F2F1"
                    3 -> "#E3F2FD"
                    4 -> "#121212"
                    5 -> "#FFF0F5"
                    6 -> "#FBF3F4"
                    else -> "#455A64"
                }
                if (newBg != prefs.getString("bg_color", "#455A64")) {
                    when (position) {
                        0 -> applyThemeToPrefs(prefs, "#455A64", "#FDF5E6", "#37474F")
                        1 -> applyThemeToPrefs(prefs, "#FDFBF7", "#212121", "#F9F6F0")
                        2 -> applyThemeToPrefs(prefs, "#E0F2F1", "#004D40", "#B2DFDB")
                        3 -> applyThemeToPrefs(prefs, "#E3F2FD", "#0D47A1", "#BBDEFB")
                        4 -> applyThemeToPrefs(prefs, "#121212", "#E0E0E0", "#1E1E1E")
                        5 -> applyThemeToPrefs(prefs, "#FFF0F5", "#C2185B", "#F8BBD0")
                        6 -> applyThemeToPrefs(prefs, "#FBF3F4", "#9C143A", "#F0D5DA")
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

        val btnAboutApp = view.findViewById<android.widget.TextView>(R.id.btnAboutApp)
        btnAboutApp?.setOnClickListener {
            val aboutText = "1- (¼ ح) وما يشابهها تدل على الأحزاب حيث يُقسم القرآن إلى 60 حزباً.\n\n2- (ع) هو اختصار لكلمة ركوع حيث يستطيع المصلي التوقف عندها لاكتمال المعنى وهي محببة.\n\n3- المنزل هي طريقة لتقسيم القرآن لقراءته في 7 أيام أي مشابه للأجزاء.\n\n4- الألوان وضعت لتمييز أحكام التجويد وفق معايير معتمدة.\n\n5- يمكنك الاستفادة من البحث في شريط البحث في تطبيق سبح بخشوع والأذكار لتجد كل ما تبحث عنه من أذكار وتفسير مختصر لأن التطبيق مزود بقاعدة بيانات محلية.\n\n6- يمكن الاستزادة من البحث في جُمني بعد وضع مفتاحك الخاص لكي يأخذك إلى عالم أوسع لكن محدد بالصحيح منه.\n\n7- نرجو أن تستفيد من التطبيق فينالك الأجر العظيم من ذكر الله.\n\n8- التزام الملكية: نؤكد أن النص القرآني المستخدم والخطوط المدمجة مأخوذة من مصادر عامة ومفتوحة (Public Domain) مثل مجمع الملك فهد، ولا تنتهك أي حقوق نشر.\n\n9- يمكنك الآن الاستماع إلى تلاوة الآيات أو السور كاملة بثاً مباشراً عبر الإنترنت، مع إمكانية تحميل السورة للاستماع إليها لاحقاً بدون إنترنت، وذلك عبر النقر على خيار الصوت في قائمة خيارات الآية.\n\n10- للتواصل والمقترحات:\nvcol42@gmail.com"
            val tv = android.widget.TextView(activity).apply {
                text = aboutText
                textSize = 14f
                setPadding(40, 40, 40, 40)
                setTextColor(android.graphics.Color.parseColor("#3D2B1F"))
                setTextIsSelectable(true)
            }
            androidx.appcompat.app.AlertDialog.Builder(activity)
                .setTitle("حول التطبيق")
                .setView(
                    android.widget.ScrollView(activity).apply {
                        addView(tv)
                    }
                )
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
