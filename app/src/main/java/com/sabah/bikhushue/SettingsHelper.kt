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
        val themeColors = ThemeHelper.getThemeColors(activity)
        
        // Apply theme background to the root
        view.setBackgroundColor(themeColors.bg)
        dialog.setContentView(view)

        val spinnerTheme = view.findViewById<Spinner>(R.id.spinnerTheme)
        val switchTajweed = view.findViewById<SwitchMaterial>(R.id.switchTajweed)
        val switchVibration = view.findViewById<SwitchMaterial>(R.id.switchVibration)
        val switchSound = view.findViewById<SwitchMaterial>(R.id.switchSound)
        val switchAzan = view.findViewById<SwitchMaterial>(R.id.switchAzan)
        val switchHizb = view.findViewById<SwitchMaterial>(R.id.switchHizb)
        val switchManzil = view.findViewById<SwitchMaterial>(R.id.switchManzil)
        val rgSeparatorType = view.findViewById<android.widget.RadioGroup>(R.id.rgSeparatorType)
        val rbSeparatorPage = view.findViewById<android.widget.RadioButton>(R.id.rbSeparatorPage)
        val rbSeparatorKhatma29 = view.findViewById<android.widget.RadioButton>(R.id.rbSeparatorKhatma29)
        val rbSeparatorKhatma30 = view.findViewById<android.widget.RadioButton>(R.id.rbSeparatorKhatma30)
        val rbSeparatorHizb = view.findViewById<android.widget.RadioButton>(R.id.rbSeparatorHizb)
        val rbSeparatorNone = view.findViewById<android.widget.RadioButton>(R.id.rbSeparatorNone)
        val spinnerTranslation = view.findViewById<Spinner>(R.id.spinnerTranslation)
        val etApiKey = view.findViewById<EditText>(R.id.etApiKey)
        val btnSave = view.findViewById<android.widget.TextView>(R.id.btnSaveSettings)

        val prefs = activity.getSharedPreferences("app", Context.MODE_PRIVATE)
        
        view.findViewById<android.widget.TextView>(R.id.tvSettingsTitle)?.setTextColor(themeColors.txt)
        view.findViewById<android.widget.TextView>(R.id.tvThemeLabel)?.setTextColor(themeColors.txt)
        view.findViewById<android.widget.TextView>(R.id.tvApiKeyLabel)?.setTextColor(themeColors.txt)
        view.findViewById<android.widget.TextView>(R.id.tvSeparatorTitle)?.setTextColor(themeColors.txt)
        switchTajweed.setTextColor(themeColors.txt)
        switchVibration.setTextColor(themeColors.txt)
        switchSound.setTextColor(themeColors.txt)
        switchAzan.setTextColor(themeColors.txt)
        switchHizb.setTextColor(themeColors.txt)
        switchManzil.setTextColor(themeColors.txt)
        rbSeparatorPage?.setTextColor(themeColors.txt)
        rbSeparatorKhatma29?.setTextColor(themeColors.txt)
        rbSeparatorKhatma30?.setTextColor(themeColors.txt)
        rbSeparatorNone?.setTextColor(themeColors.txt)
        view.findViewById<android.widget.TextView>(R.id.tvTranslationLabel)?.setTextColor(themeColors.txt)
        etApiKey.setTextColor(themeColors.txt)
        etApiKey.setHintTextColor(themeColors.txt)

        // Theme application for categorized cards
        val shadowCards = listOf(
            R.id.cvAppearanceShadow, R.id.cvReadingShadow, R.id.cvAudioShadow, 
            R.id.cvAiShadow, R.id.cvPrivacyShadow, R.id.cvAboutShadow,
            R.id.cvSpinnerShadow, R.id.cvTranslationShadow, R.id.cvApiKeyShadow
        )
        val mainCards = listOf(
            R.id.cvAppearanceCard, R.id.cvReadingCard, R.id.cvAudioCard, 
            R.id.cvAiCard, R.id.cvPrivacyCard, R.id.cvAboutCard,
            R.id.cvSpinnerCard, R.id.cvTranslationCard, R.id.cvApiKeyCard
        )
        
        shadowCards.forEach { id ->
            view.findViewById<com.google.android.material.card.MaterialCardView>(id)?.setCardBackgroundColor(themeColors.shadow)
        }
        mainCards.forEach { id ->
            view.findViewById<com.google.android.material.card.MaterialCardView>(id)?.apply {
                setCardBackgroundColor(themeColors.cardBg)
                strokeColor = themeColors.stroke
            }
        }
        
        val titles = listOf(
            R.id.tvAppearanceTitle, R.id.tvReadingTitle, R.id.tvAudioTitle, R.id.tvAiTitle
        )
        titles.forEach { id ->
            view.findViewById<android.widget.TextView>(id)?.setTextColor(themeColors.stroke)
        }
        
        // Theme Setup
        val themes = arrayOf("ليلي", "قمري", "كريمي", "زمردي", "سماوي", "زهري", "قرمزي")
        val spinnerAdapter = object : ArrayAdapter<String>(activity, android.R.layout.simple_spinner_dropdown_item, themes) {
            override fun getView(position: Int, convertView: View?, parent: android.view.ViewGroup): View {
                val view = super.getView(position, convertView, parent) as android.widget.TextView
                view.setTextColor(themeColors.txt)
                view.gravity = android.view.Gravity.CENTER
                return view
            }

            override fun getDropDownView(position: Int, convertView: View?, parent: android.view.ViewGroup): View {
                val view = super.getDropDownView(position, convertView, parent) as android.widget.TextView
                view.setBackgroundColor(themeColors.dropdownBg)
                view.setTextColor(themeColors.txt)
                view.gravity = android.view.Gravity.CENTER
                view.setPadding(16, 24, 16, 24)
                return view
            }
        }
        spinnerTheme.adapter = spinnerAdapter
        spinnerTheme.setPopupBackgroundDrawable(android.graphics.drawable.ColorDrawable(themeColors.dropdownBg))

        
        val currentBg = prefs.getString("bg_color", "#121212")
        spinnerTheme.setSelection(when (currentBg) {
            "#121212" -> 0
            "#455A64", "#37474F", "#263238", "#D4CEC4" -> 1
            "#FDFBF7" -> 2
            "#E0F2F1" -> 3
            "#E3F2FD" -> 4
            "#FFF0F5" -> 5
            "#FBF3F4" -> 6
            else -> 0
        })

        spinnerTheme.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, v: View?, position: Int, id: Long) {
                val newBg = when (position) {
                    0 -> "#121212"
                    1 -> "#455A64"
                    2 -> "#FDFBF7"
                    3 -> "#E0F2F1"
                    4 -> "#E3F2FD"
                    5 -> "#FFF0F5"
                    6 -> "#FBF3F4"
                    else -> "#121212"
                }
                if (newBg != prefs.getString("bg_color", "#121212")) {
                    when (position) {
                        0 -> applyThemeToPrefs(prefs, "#121212", "#E0E0E0", "#1E1E1E")
                        1 -> applyThemeToPrefs(prefs, "#455A64", "#FDF5E6", "#37474F")
                        2 -> applyThemeToPrefs(prefs, "#FDFBF7", "#212121", "#F9F6F0")
                        3 -> applyThemeToPrefs(prefs, "#E0F2F1", "#004D40", "#B2DFDB")
                        4 -> applyThemeToPrefs(prefs, "#E3F2FD", "#0D47A1", "#BBDEFB")
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

        switchHizb.isChecked = prefs.getBoolean("show_hizb", false)
        switchHizb.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("show_hizb", isChecked).apply()
            onTajweedChanged?.invoke(isChecked)
        }

        // Separator Type RadioGroup (Mutually Exclusive Logic)
        val savedSepStr = prefs.getString("separator_type", "PAGE")
        val currentSep = SeparatorType.fromString(savedSepStr)
        when (currentSep) {
            SeparatorType.PAGE -> rbSeparatorPage?.isChecked = true
            SeparatorType.RUKOO_KHATMA_29 -> rbSeparatorKhatma29?.isChecked = true
            SeparatorType.RUKOO_KHATMA_30 -> rbSeparatorKhatma30?.isChecked = true
            SeparatorType.HIZB -> rbSeparatorHizb?.isChecked = true
            SeparatorType.NONE -> rbSeparatorNone?.isChecked = true
        }

        rgSeparatorType?.setOnCheckedChangeListener { _, checkedId ->
            val selectedType = when (checkedId) {
                R.id.rbSeparatorPage -> SeparatorType.PAGE
                R.id.rbSeparatorKhatma29 -> SeparatorType.RUKOO_KHATMA_29
                R.id.rbSeparatorKhatma30 -> SeparatorType.RUKOO_KHATMA_30
                R.id.rbSeparatorHizb -> SeparatorType.HIZB
                R.id.rbSeparatorNone -> SeparatorType.NONE
                else -> SeparatorType.PAGE
            }
            prefs.edit().putString("separator_type", selectedType.name).apply()
            onTajweedChanged?.invoke(true)
        }

        switchManzil.isChecked = prefs.getBoolean("show_manzil", false)
        switchManzil.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("show_manzil", isChecked).apply()
            onTajweedChanged?.invoke(isChecked)
        }

        // Translation Setup
        val translations = arrayOf("بدون ترجمة", "التفسير الميسر (العربية)", "الإنجليزية (English)", "الإندونيسية (Indonesian)")
        val transAdapter = object : ArrayAdapter<String>(activity, android.R.layout.simple_spinner_dropdown_item, translations) {
            override fun getView(position: Int, convertView: View?, parent: android.view.ViewGroup): View {
                val view = super.getView(position, convertView, parent) as android.widget.TextView
                view.setTextColor(themeColors.txt)
                view.gravity = android.view.Gravity.CENTER
                return view
            }
            override fun getDropDownView(position: Int, convertView: View?, parent: android.view.ViewGroup): View {
                val view = super.getDropDownView(position, convertView, parent) as android.widget.TextView
                view.setBackgroundColor(themeColors.dropdownBg)
                view.setTextColor(themeColors.txt)
                view.gravity = android.view.Gravity.CENTER
                view.setPadding(16, 24, 16, 24)
                return view
            }
        }
        spinnerTranslation.adapter = transAdapter
        spinnerTranslation.setPopupBackgroundDrawable(android.graphics.drawable.ColorDrawable(themeColors.dropdownBg))
        
        val currentTranslation = prefs.getString("translation_lang", "none")
        spinnerTranslation.setSelection(when (currentTranslation) {
            "ar" -> 1
            "en" -> 2
            "id" -> 3
            else -> 0
        })

        spinnerTranslation.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, v: View?, position: Int, id: Long) {
                val lang = when(position) {
                    1 -> "ar"
                    2 -> "en"
                    3 -> "id"
                    else -> "none"
                }
                prefs.edit().putString("translation_lang", lang).apply()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // API Key
        etApiKey.setText(prefs.getString("api", ""))
        
        val tilApiKey = view.findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.tilApiKey)
        tilApiKey?.setEndIconOnClickListener {
            val key = etApiKey.text.toString().trim()
            prefs.edit().putString("api", key).apply()
            android.widget.Toast.makeText(activity, "تم حفظ مفتاح جُمني بنجاح!", android.widget.Toast.LENGTH_SHORT).show()
        }

        val tvGetApiKey = view.findViewById<android.widget.TextView>(R.id.tvGetApiKey)
        tvGetApiKey?.setOnClickListener {
            val instructions = "للحصول على مفتاح جُمني (Gemini API Key) مجاناً، اتبع الخطوات البسيطة التالية:\n\n" +
                    "1- تأكد من تسجيل الدخول بحساب جوجل (Google) الخاص بك.\n" +
                    "2- اضغط على زر 'الانتقال للموقع' بالأسفل.\n" +
                    "3- في الموقع، اضغط على الزر الأزرق 'Create API key'.\n" +
                    "4- قم بنسخ المفتاح الذي يظهر لك.\n" +
                    "5- ارجع إلى هذا التطبيق والصق المفتاح في الحقل المخصص.\n\n" +
                    "هل تود الانتقال إلى الموقع الآن؟"

            androidx.appcompat.app.AlertDialog.Builder(activity)
                .setTitle("خطوات الحصول على المفتاح")
                .setMessage(instructions)
                .setPositiveButton("الانتقال للموقع") { _, _ ->
                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW)
                    intent.data = android.net.Uri.parse("https://aistudio.google.com/app/apikey")
                    activity.startActivity(intent)
                }
                .setNegativeButton("تراجع", null)
                .show()
        }

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
            val aboutText = "1- (¼ ح) وما يشابهها تدل على الأحزاب حيث يُقسم القرآن إلى 60 حزباً.\n\n" +
                    "2- (ع) هو اختصار لكلمة ركوع حيث يستطيع المصلي التوقف عندها لاكتمال المعنى وهي محببة.\n\n" +
                    "3- المنزل هي طريقة لتقسيم القرآن لقراءته في 7 أيام أي مشابه للأجزاء.\n\n" +
                    "4- الألوان وضعت لتمييز أحكام التجويد وفق معايير معتمدة.\n\n" +
                    "5- يمكنك الاستفادة من البحث في شريط البحث لتجد كل ما تبحث عنه من أذكار وتفسير لأن التطبيق مزود بقاعدة بيانات محلية.\n\n" +
                    "6- يمكن الاستزادة والبحث الذكي باستخدام مساعد جُمني (Gemini) بعد إحضار وإضافة مفتاحك الخاص (API Key) في الإعدادات. التطبيق لا يشارك هذا المفتاح مع أي جهة خارجية ويتم حفظه محلياً على جهازك فقط لضمان خصوصيتك.\n\n" +
                    "7- نرجو أن تستفيد من التطبيق فينالك الأجر العظيم من ذكر الله.\n\n" +
                    "8- التزام الملكية: النص القرآني المستخدم والخطوط مأخوذة من مصادر عامة ومفتوحة مثل مجمع الملك فهد، ولا تنتهك أي حقوق نشر.\n\n" +
                    "9- يمكنك الاستماع لتلاوة الآيات أو السور عبر الإنترنت، مع إمكانية التحميل للاستماع لاحقاً بدون إنترنت.\n\n" +
                    "10- للتواصل والمقترحات:\n" +
                    "vcol42@gmail.com"
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
