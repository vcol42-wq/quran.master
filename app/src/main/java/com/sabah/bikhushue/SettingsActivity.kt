package com.sabah.bikhushue

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.card.MaterialCardView
import com.google.android.material.switchmaterial.SwitchMaterial

class SettingsActivity : AppCompatActivity() {

    private lateinit var spinnerTheme: Spinner
    private lateinit var spinnerTranslation: Spinner
    private lateinit var cvSpinnerCard: MaterialCardView
    private lateinit var cvTranslationCard: MaterialCardView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_settings)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.settingsRoot)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupSettingsUI()
        applyCurrentTheme()
    }

    private fun applyCurrentTheme() {
        val themeColors = ThemeHelper.getThemeColors(this)
        ThemeHelper.applySystemWindowsColors(this)

        val root = findViewById<View>(R.id.settingsRoot)
        root?.setBackgroundColor(themeColors.bg)

        val settingsHeader = findViewById<View>(R.id.settingsHeader)
        settingsHeader?.setBackgroundColor(themeColors.bar)

        val tvSettingsTitle = findViewById<TextView>(R.id.tvSettingsTitle)
        tvSettingsTitle?.setTextColor(themeColors.txt)

        val btnBackSettings = findViewById<ImageView>(R.id.btnBackSettings)
        btnBackSettings?.setColorFilter(themeColors.txt)

        cvSpinnerCard.setCardBackgroundColor(Color.parseColor("#FAF8F5"))
        cvSpinnerCard.strokeColor = Color.parseColor("#D2B48C")

        cvTranslationCard.setCardBackgroundColor(Color.parseColor("#FAF8F5"))
        cvTranslationCard.strokeColor = Color.parseColor("#D2B48C")

        val cards = listOf(
            R.id.cvAppearanceCard,
            R.id.cvReadingCard,
            R.id.cvAudioCard,
            R.id.cvAiCard
        )
        cards.forEach { id ->
            findViewById<MaterialCardView>(id)?.apply {
                setCardBackgroundColor(Color.parseColor("#FFFFFF"))
                strokeColor = Color.parseColor("#2E5B42")
            }
        }

        fun updateTextColors(viewGroup: ViewGroup) {
            for (i in 0 until viewGroup.childCount) {
                val child = viewGroup.getChildAt(i)
                if (child is TextView && child.id != R.id.btnSaveSettings && child.id != R.id.tvSettingsTitle) {
                    child.setTextColor(Color.BLACK)
                } else if (child is RadioButton) {
                    child.setTextColor(Color.BLACK)
                } else if (child is SwitchMaterial) {
                    child.setTextColor(Color.BLACK)
                } else if (child is ViewGroup) {
                    updateTextColors(child)
                }
            }
        }
        val mainContent = findViewById<ViewGroup>(R.id.settingsRoot)
        if (mainContent != null) updateTextColors(mainContent)

        val prefs = getSharedPreferences("app", Context.MODE_PRIVATE)

        // إعداد المحولات بمستطيل كريمي وخط أسود ثابت عالي التباين وواضح جداً
        val spinnerTextColor = Color.BLACK

        val themes = arrayOf("ليلي", "قمري", "كريمي", "زمردي", "سماوي", "زهري", "قرمزي", "عنابي ملكي")
        val spinnerAdapter = object : ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, themes) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val v = super.getView(position, convertView, parent) as TextView
                v.setTextColor(spinnerTextColor)
                v.gravity = Gravity.CENTER
                v.textSize = 14f
                return v
            }
            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                val v = super.getDropDownView(position, convertView, parent) as TextView
                v.setBackgroundColor(Color.parseColor("#FFFFFF"))
                v.setTextColor(spinnerTextColor)
                v.gravity = Gravity.CENTER
                v.setPadding(16, 24, 16, 24)
                return v
            }
        }
        spinnerTheme.adapter = spinnerAdapter
        spinnerTheme.setPopupBackgroundDrawable(ColorDrawable(Color.parseColor("#FFFFFF")))

        val currentBg = prefs.getString("bg_color", "#121212")
        spinnerTheme.setSelection(when (currentBg) {
            "#121212" -> 0
            "#455A64", "#37474F", "#263238", "#D4CEC4" -> 1
            "#FDFBF7" -> 2
            "#E0F2F1" -> 3
            "#E3F2FD" -> 4
            "#FFF0F5", "#FCE4EC" -> 5
            "#FBF3F4" -> 6
            "#581825", "#4A0E17", "#800020" -> 7
            else -> 0
        })

        val translations = arrayOf("بدون ترجمة", "التفسير الميسر (العربية)", "الإنجليزية (English)", "الإندونيسية (Indonesian)")
        val transAdapter = object : ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, translations) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val v = super.getView(position, convertView, parent) as TextView
                v.setTextColor(spinnerTextColor)
                v.gravity = Gravity.CENTER
                v.textSize = 14f
                return v
            }
            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                val v = super.getDropDownView(position, convertView, parent) as TextView
                v.setBackgroundColor(Color.parseColor("#FFFFFF"))
                v.setTextColor(spinnerTextColor)
                v.gravity = Gravity.CENTER
                v.setPadding(16, 24, 16, 24)
                return v
            }
        }
        spinnerTranslation.adapter = transAdapter
        spinnerTranslation.setPopupBackgroundDrawable(ColorDrawable(Color.parseColor("#FFFFFF")))

        val currentTranslation = prefs.getString("translation_lang", "none")
        spinnerTranslation.setSelection(when (currentTranslation) {
            "ar" -> 1
            "en" -> 2
            "id" -> 3
            else -> 0
        })
    }

    private fun setupSettingsUI() {
        findViewById<ImageView>(R.id.btnBackSettings)?.setOnClickListener { finish() }

        spinnerTheme = findViewById(R.id.spinnerTheme)
        spinnerTranslation = findViewById(R.id.spinnerTranslation)
        cvSpinnerCard = findViewById(R.id.cvSpinnerCard)
        cvTranslationCard = findViewById(R.id.cvTranslationCard)

        val rgSeparatorType = findViewById<RadioGroup>(R.id.rgSeparatorType)
        val rbSeparatorKhatma29 = findViewById<RadioButton>(R.id.rbSeparatorKhatma29)
        val rbSeparatorKhatma30 = findViewById<RadioButton>(R.id.rbSeparatorKhatma30)
        val rbSeparatorHizb = findViewById<RadioButton>(R.id.rbSeparatorHizb)
        val rbSeparatorNone = findViewById<RadioButton>(R.id.rbSeparatorNone)

        val switchSound = findViewById<SwitchMaterial>(R.id.switchSound)
        val switchVibration = findViewById<SwitchMaterial>(R.id.switchVibration)
        val switchAzan = findViewById<SwitchMaterial>(R.id.switchAzan)
        val btnLocationSettings = findViewById<View>(R.id.btnLocationSettings)

        val etApiKey = findViewById<EditText>(R.id.etApiKey)
        val tilApiKey = findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.tilApiKey)
        val tvGetApiKey = findViewById<TextView>(R.id.tvGetApiKey)
        val btnPrivacyPolicy = findViewById<View>(R.id.btnPrivacyPolicy)
        val btnAboutApp = findViewById<View>(R.id.btnAboutApp)
        val btnSave = findViewById<TextView>(R.id.btnSaveSettings)

        val prefs = getSharedPreferences("app", Context.MODE_PRIVATE)

        // 1. اختيار الثيم
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
                    7 -> "#581825"
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
                        7 -> applyThemeToPrefs(prefs, "#581825", "#F7E7C4", "#3E0F19")
                    }
                    applyCurrentTheme()
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

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

        // 2. فواصل القرآن والختمات
        val savedSepStr = prefs.getString("separator_type", "PAGE")
        val currentSep = SeparatorType.fromString(savedSepStr)
        when (currentSep) {
            SeparatorType.RUKOO_KHATMA_29 -> rbSeparatorKhatma29?.isChecked = true
            SeparatorType.RUKOO_KHATMA_30 -> rbSeparatorKhatma30?.isChecked = true
            SeparatorType.HIZB -> rbSeparatorHizb?.isChecked = true
            SeparatorType.NONE -> rbSeparatorNone?.isChecked = true
            else -> rbSeparatorNone?.isChecked = true
        }

        rgSeparatorType?.setOnCheckedChangeListener { _, checkedId ->
            val selectedType = when (checkedId) {
                R.id.rbSeparatorKhatma29 -> SeparatorType.RUKOO_KHATMA_29
                R.id.rbSeparatorKhatma30 -> SeparatorType.RUKOO_KHATMA_30
                R.id.rbSeparatorHizb -> SeparatorType.HIZB
                R.id.rbSeparatorNone -> SeparatorType.NONE
                else -> SeparatorType.NONE
            }
            prefs.edit().putString("separator_type", selectedType.name).apply()
        }

        // 3. التنبيهات والمواقيت
        val soundState = prefs.getBoolean("sound_on", true)
        switchSound.isChecked = soundState
        switchSound.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit()
                .putBoolean("sound_on", isChecked)
                .putBoolean("athan_sound_enabled", isChecked)
                .apply()
        }

        switchVibration.isChecked = prefs.getBoolean("vibration_on", true)
        switchVibration.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("vibration_on", isChecked).apply()
        }

        val azanState = prefs.getBoolean("azan_on", true)
        switchAzan.isChecked = azanState
        switchAzan.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("azan_on", isChecked).apply()
        }

        btnLocationSettings?.setOnClickListener {
            startActivity(Intent(this, AthanActivity::class.java))
        }

        // 4. الذكاء الاصطناعي والمعلومات
        val savedKey = prefs.getString("api", "") ?: ""
        etApiKey.setText(savedKey)
        if (savedKey.isEmpty()) {
            tilApiKey?.hint = "مفتاح جُمني الافتراضي مفعّل للجميع"
        }
        tilApiKey?.setEndIconOnClickListener {
            val key = etApiKey.text.toString().trim()
            prefs.edit().putString("api", key).apply()
            val msg = if (key.isNotEmpty()) "تم حفظ المفتاح الخاص بك بنجاح!" else "تمت العودة لاستخدام المفتاح الافتراضي"
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        }

        tvGetApiKey?.setOnClickListener {
            val instructions = "للحصول على مفتاح جُمني (Gemini API Key) مجاناً، اتبع الخطوات البسيطة التالية:\n\n" +
                    "1- تأكد من تسجيل الدخول بحساب جوجل (Google) الخاص بك.\n" +
                    "2- اضغط على زر 'الانتقال للموقع' بالأسفل.\n" +
                    "3- في الموقع، اضغط على الزر الأزرق 'Create API key'.\n" +
                    "4- قم بنسخ المفتاح الذي يظهر لك.\n" +
                    "5- ارجع إلى هذا التطبيق والصق المفتاح في الحقل المخصص.\n\n" +
                    "هل تود الانتقال إلى الموقع الآن؟"

            AlertDialog.Builder(this)
                .setTitle("خطوات الحصول على المفتاح")
                .setMessage(instructions)
                .setPositiveButton("الانتقال للموقع") { _, _ ->
                    val intent = Intent(Intent.ACTION_VIEW)
                    intent.data = Uri.parse("https://aistudio.google.com/app/apikey")
                    startActivity(intent)
                }
                .setNegativeButton("تراجع", null)
                .show()
        }

        btnPrivacyPolicy?.setOnClickListener {
            val webView = WebView(this).apply {
                loadUrl("file:///android_asset/privacy_policy.html")
            }
            AlertDialog.Builder(this)
                .setTitle("سياسة الخصوصية")
                .setView(webView)
                .setPositiveButton("حسناً", null)
                .show()
        }

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
            val tv = TextView(this).apply {
                text = aboutText
                textSize = 14f
                setPadding(40, 40, 40, 40)
                setTextColor(Color.parseColor("#3D2B1F"))
                setTextIsSelectable(true)
            }
            AlertDialog.Builder(this)
                .setTitle("حول التطبيق")
                .setView(
                    ScrollView(this).apply {
                        addView(tv)
                    }
                )
                .setPositiveButton("حسناً", null)
                .show()
        }

        btnSave?.setOnClickListener {
            prefs.edit().putString("api", etApiKey.text.toString().trim()).apply()
            Toast.makeText(this, "تم حفظ الإعدادات بنجاح", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun applyThemeToPrefs(prefs: android.content.SharedPreferences, bg: String, txt: String, bar: String) {
        prefs.edit()
            .putString("bg_color", bg)
            .putString("txt_color", txt)
            .putString("bar_color", bar)
            .apply()
    }
}
