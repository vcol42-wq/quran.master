package com.sabah.bikhushue

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.card.MaterialCardView
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.button.MaterialButton
import com.batoulapps.adhan.CalculationMethod
import com.batoulapps.adhan.Coordinates
import com.batoulapps.adhan.Madhab
import com.batoulapps.adhan.PrayerTimes
import com.batoulapps.adhan.data.DateComponents
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

class AthanActivity : AppCompatActivity() {

    private fun adjustTime(date: Date?, offsetMins: Int): Date? {
        if (date == null) return null
        return Date(date.time + (offsetMins * 60 * 1000))
    }


    private var latitude: Double = 24.7136
    private var longitude: Double = 46.6753
    private var cityName: String = "الرياض"
    private var regionName: String = "Asia/Riyadh"
    private var selectedCalcMethodIndex: Int = 0
    private var isSoundEnabled: Boolean = true
    private var selectedRingtoneUri: String? = null
    
    private var isBottomBarVisible = true

    private val PERMISSION_REQUEST_LOCATION = 100
    private val REQUEST_CODE_RINGTONE = 999

    private val handler = Handler(Looper.getMainLooper())
    private val countdownRunnable = object : Runnable {
        override fun run() {
            updateCountdown()
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        try {
            doOnCreate(savedInstanceState)
        } catch (e: Throwable) {
            android.widget.Toast.makeText(this, "Athan Crash: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
            android.util.Log.e("AthanActivity", "Crash in onCreate", e)
            finish()
        }
    }

    private fun doOnCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }

        setContentView(R.layout.activity_athan)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.scrollView)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        BottomBarHelper.setupBottomBar(this)

        val scrollView = findViewById<android.widget.ScrollView>(R.id.scrollView)
        scrollView?.setOnScrollChangeListener { _, _, scrollY, _, oldScrollY ->
            val dy = scrollY - oldScrollY
            if (dy > 30 && isBottomBarVisible) toggleBottomBar()
            else if (dy < -30 && !isBottomBarVisible) toggleBottomBar()
        }

        // Load saved settings
        val prefs = getSharedPreferences("app", MODE_PRIVATE)
        latitude = prefs.getFloat("athan_latitude", 24.7136f).toDouble()
        longitude = prefs.getFloat("athan_longitude", 46.6753f).toDouble()
        cityName = prefs.getString("athan_city_name", "الرياض") ?: "الرياض"
        regionName = prefs.getString("athan_region_name", "Asia/Riyadh") ?: "Asia/Riyadh"
        selectedCalcMethodIndex = prefs.getInt("athan_calc_method", 0)
        isSoundEnabled = prefs.getBoolean("athan_sound_enabled", true)
        selectedRingtoneUri = prefs.getString("athan_sound_uri", "default")

        // Setup views
        findViewById<TextView>(R.id.tvCityName)?.text = cityName
        findViewById<TextView>(R.id.tvRegion)?.text = regionName
        findViewById<TextView>(R.id.tvCoordinates)?.text = String.format(Locale.US, "الإحداثيات: %.4f, %.4f", latitude, longitude)

        updateDates()

        // Switch Sound Setup
        val switchSound: SwitchMaterial = findViewById(R.id.switchSound)
        val tvSoundStatusText: TextView = findViewById(R.id.tvSoundStatusText)
        val tvSoundStatusIcon: TextView = findViewById(R.id.tvSoundStatusIcon)

        switchSound.isChecked = isSoundEnabled
        tvSoundStatusText.text = if (isSoundEnabled) "صوت الأذان مفعل" else "صوت الأذان مكتوم"
        tvSoundStatusIcon.text = if (isSoundEnabled) "🔊" else "🔇"

        switchSound.setOnCheckedChangeListener { _, isChecked ->
            isSoundEnabled = isChecked
            tvSoundStatusText.text = if (isSoundEnabled) "صوت الأذان مفعل" else "صوت الأذان مكتوم"
            tvSoundStatusIcon.text = if (isSoundEnabled) "🔊" else "🔇"
            prefs.edit().putBoolean("athan_sound_enabled", isChecked).apply()
        }

        // Expandable Settings Block
        var isSettingsExpanded = false
        val layoutSettingsHeader: View = findViewById(R.id.layoutSettingsHeader)
        val layoutSettingsBody: View = findViewById(R.id.layoutSettingsBody)
        val tvDropdownIndicator: TextView = findViewById(R.id.tvDropdownIndicator)

        layoutSettingsHeader.setOnClickListener {
            isSettingsExpanded = !isSettingsExpanded
            if (isSettingsExpanded) {
                layoutSettingsBody.visibility = View.VISIBLE
                tvDropdownIndicator.text = "▲"
            } else {
                layoutSettingsBody.visibility = View.GONE
                tvDropdownIndicator.text = "▼"
            }
        }

        val themeColors = ThemeHelper.getThemeColors(this)
        
        // Spinner Setup for Calculation Method
        val spinnerCalcMethod: Spinner = findViewById(R.id.spinnerCalcMethod)
        spinnerCalcMethod.setPopupBackgroundDrawable(android.graphics.drawable.ColorDrawable(themeColors.dropdownBg))
        val methods = arrayOf(
            "جامعة أم القرى (مكة المكرمة)",
            "رابطة العالم الإسلامي",
            "الهيئة العامة المصرية للمساحة",
            "جامعة العلوم الإسلامية بكراتشي",
            "الجمعية الإسلامية لأمريكا الشمالية (ISNA)",
            "دبي",
            "قطر",
            "الكويت",
            "سنغافورة",
            "تركيا (رئاسة الشؤون الدينية)",
            "معهد الجيوفيزياء بجامعة طهران"
        )
        // themeColors already loaded above
        val adapter = object : ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, methods) {
            override fun getView(position: Int, convertView: View?, parent: android.view.ViewGroup): View {
                val view = super.getView(position, convertView, parent) as android.widget.TextView
                view.setTextColor(themeColors.txt)
                return view
            }

            override fun getDropDownView(position: Int, convertView: View?, parent: android.view.ViewGroup): View {
                val view = super.getDropDownView(position, convertView, parent) as android.widget.TextView
                view.setBackgroundColor(themeColors.dropdownBg)
                view.setTextColor(themeColors.txt)
                return view
            }
        }
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCalcMethod.adapter = adapter
        spinnerCalcMethod.setSelection(selectedCalcMethodIndex)
        spinnerCalcMethod.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position != selectedCalcMethodIndex) {
                    selectedCalcMethodIndex = position
                    prefs.edit().putInt("athan_calc_method", position).apply()
                    updatePrayerTimes()
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Location Buttons
        findViewById<MaterialButton>(R.id.btnAutoLocation).setOnClickListener {
            requestAutoLocation()
        }

        findViewById<MaterialButton>(R.id.btnManualLocation).setOnClickListener {
            showManualLocationDialog()
        }

        // Choose Sound Button
        findViewById<com.google.android.material.button.MaterialButton>(R.id.btnChooseSound).setOnClickListener {
            val options = arrayOf("اختر من الهاتف 🎵", "تحميل أصوات الأذان مجاناً 🌐")
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("صوت الأذان")
                .setItems(options) { _, which ->
                    if (which == 0) {
                        val pickerIntent = Intent(android.media.RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                            putExtra(android.media.RingtoneManager.EXTRA_RINGTONE_TYPE, android.media.RingtoneManager.TYPE_NOTIFICATION or android.media.RingtoneManager.TYPE_RINGTONE)
                            putExtra(android.media.RingtoneManager.EXTRA_RINGTONE_TITLE, "اختر صوت الأذان")
                            putExtra(android.media.RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                            putExtra(android.media.RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true)
                            if (selectedRingtoneUri != null && selectedRingtoneUri != "default") {
                                putExtra(android.media.RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(selectedRingtoneUri))
                            }
                        }
                        startActivityForResult(pickerIntent, REQUEST_CODE_RINGTONE)
                    } else {
                        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.islamcan.com/audio/adhans/index.shtml"))
                        startActivity(browserIntent)
                    }
                }
                .show()
        }

        updateSoundPathText()
        applyTheme()

        // Setup offset controls
        val prefsCore = getSharedPreferences("TasbihCore", Context.MODE_PRIVATE)
        val prayers = listOf("FAJR", "SUNRISE", "DHUHR", "ASR", "MAGHRIB", "ISHA")
        
        fun updateOffsetUI(p: String, tv: TextView) {
            val o = prefsCore.getInt("offset_$p", 0)
            tv.text = if (o > 0) "+$o" else o.toString()
        }
        
        prayers.forEach { p ->
            val tvOffsetId = resources.getIdentifier("tvOffset$p", "id", packageName)
            val btnIncId = resources.getIdentifier("btnInc$p", "id", packageName)
            val btnDecId = resources.getIdentifier("btnDec$p", "id", packageName)
            
            val tvOffset = findViewById<TextView>(tvOffsetId)
            val btnInc = findViewById<View>(btnIncId)
            val btnDec = findViewById<View>(btnDecId)
            
            if (tvOffset != null && btnInc != null && btnDec != null) {
                updateOffsetUI(p, tvOffset)
                btnInc.setOnClickListener {
                    val o = prefsCore.getInt("offset_$p", 0)
                    prefsCore.edit().putInt("offset_$p", o + 1).apply()
                    updateOffsetUI(p, tvOffset)
                    updatePrayerTimes()
                    updateCountdown()
                    AlarmScheduler.scheduleAlarms(this)
                }
                btnDec.setOnClickListener {
                    val o = prefsCore.getInt("offset_$p", 0)
                    prefsCore.edit().putInt("offset_$p", o - 1).apply()
                    updateOffsetUI(p, tvOffset)
                    updatePrayerTimes()
                    updateCountdown()
                    AlarmScheduler.scheduleAlarms(this)
                }
            }
        }

        updatePrayerTimes()
    }

    override fun onStart() {
        super.onStart()
        handler.post(countdownRunnable)
    }

    override fun onStop() {
        super.onStop()
        handler.removeCallbacks(countdownRunnable)
    }

    override fun onResume() {
        try {
            doOnResume()
            updatePrayerTimes()
            updateDates()
            updateCountdown()
        } catch (e: Throwable) {
            android.widget.Toast.makeText(this, "Athan Resume Crash: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
            android.util.Log.e("AthanActivity", "Crash in onResume", e)
            finish()
        }
    }

    private fun doOnResume() {
        super.onResume()
        applyTheme()
        updatePrayerTimes()
    }

    private fun applyTheme() {
        val theme = ThemeHelper.getThemeColors(this)
        ThemeHelper.applySystemWindowsColors(this)
        val bgColor = theme.bg
        val txtColor = theme.txt
        val barColor = theme.bar
        val cardBgColor = theme.cardBg

        val root: View = findViewById(R.id.scrollView)
        root.setBackgroundColor(bgColor)

                        val windowInsetsController = WindowInsetsControllerCompat(window, window.decorView)
        windowInsetsController.isAppearanceLightStatusBars = !theme.isDark
        windowInsetsController.isAppearanceLightNavigationBars = !theme.isDark

        // Apply color to cards
        findViewById<MaterialCardView>(R.id.cardTimesTable).apply {
            setCardBackgroundColor(cardBgColor)
            strokeColor = theme.shadow
        }
        findViewById<MaterialCardView>(R.id.cardAudioToggle).apply {
            setCardBackgroundColor(cardBgColor)
            strokeColor = theme.shadow
        }
        findViewById<MaterialCardView>(R.id.cardGeneralSettings).apply {
            setCardBackgroundColor(cardBgColor)
            strokeColor = theme.shadow
        }
        val cardNextPrayer = findViewById<MaterialCardView>(R.id.cardNextPrayer)
        if (cardNextPrayer != null) {
            cardNextPrayer.setCardBackgroundColor(cardBgColor)
            cardNextPrayer.strokeColor = theme.shadow
            cardNextPrayer.strokeWidth = 3
        }

        // Apply theme to shadow cards
        findViewById<MaterialCardView>(R.id.shadowNextPrayer)?.setCardBackgroundColor(theme.shadow)
        findViewById<MaterialCardView>(R.id.shadowAudioToggle)?.setCardBackgroundColor(theme.shadow)
        findViewById<MaterialCardView>(R.id.shadowGeneralSettings)?.setCardBackgroundColor(theme.shadow)
        findViewById<MaterialCardView>(R.id.shadowCalcMethod)?.setCardBackgroundColor(theme.shadow)

        // Apply theme to buttons
        findViewById<MaterialButton>(R.id.btnAutoLocation)?.apply {
            setTextColor(txtColor)
            strokeColor = android.content.res.ColorStateList.valueOf(txtColor)
        }
        findViewById<MaterialButton>(R.id.btnManualLocation)?.apply {
            setTextColor(txtColor)
            strokeColor = android.content.res.ColorStateList.valueOf(txtColor)
        }
        findViewById<MaterialButton>(R.id.btnChooseSound)?.apply {
            backgroundTintList = android.content.res.ColorStateList.valueOf(barColor)
            setTextColor(txtColor)
        }

        fun updateTextViews(view: View) {
            if (view is TextView) {
                // Allow all text views inside to take the theme color
                view.setTextColor(txtColor)
            } else if (view is ViewGroup) {
                for (i in 0 until view.childCount) {
                    updateTextViews(view.getChildAt(i))
                }
            }
        }
        updateTextViews(root)
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
            9 -> CalculationMethod.MUSLIM_WORLD_LEAGUE.parameters // Turkey Diyanet parameters are identical
            10 -> {
                // Tehran: Fajr 17.7, Isha 14
                val params = CalculationMethod.OTHER.parameters
                params.fajrAngle = 17.7
                params.ishaAngle = 14.0
                params
            }
            else -> CalculationMethod.UMM_AL_QURA.parameters
        }
    }

    private fun updatePrayerTimes() {
        val coordinates = Coordinates(latitude, longitude)
        val todayComponents = DateComponents.from(Date())
        val params = getCalcMethodParameters(selectedCalcMethodIndex)
        
        val todayPrayers = try {
            PrayerTimes(coordinates, todayComponents, params)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }

        if (todayPrayers == null) {
            return
        }

        val timeFormat = SimpleDateFormat("hh:mm a", Locale.US)
        timeFormat.timeZone = TimeZone.getDefault()

        val prefsCore = getSharedPreferences("TasbihCore", Context.MODE_PRIVATE)
        val oFajr = prefsCore.getInt("offset_FAJR", 0)
        val oSunrise = prefsCore.getInt("offset_SUNRISE", 0)
        val oDhuhr = prefsCore.getInt("offset_DHUHR", 0)
        val oAsr = prefsCore.getInt("offset_ASR", 0)
        val oMaghrib = prefsCore.getInt("offset_MAGHRIB", 0)
        val oIsha = prefsCore.getInt("offset_ISHA", 0)

        fun formatSafe(date: Date?): String {
            var formatted = date?.let { timeFormat.format(it).replace(" AM", "ص").replace(" PM", "م").replace(" am", "ص").replace(" pm", "م").replace("AM", "ص").replace("PM", "م").replace("am", "ص").replace("pm", "م") } ?: "--:--"
            val arabicToEnglish = mapOf('٠' to '0', '١' to '1', '٢' to '2', '٣' to '3', '٤' to '4', '٥' to '5', '٦' to '6', '٧' to '7', '٨' to '8', '٩' to '9')
            for ((ar, en) in arabicToEnglish) {
                formatted = formatted.replace(ar, en)
            }
            return formatted
        }

        findViewById<TextView>(R.id.tvTimeFajr).text = formatSafe(adjustTime(todayPrayers.fajr, oFajr))
        findViewById<TextView>(R.id.tvTimeSunrise).text = formatSafe(adjustTime(todayPrayers.sunrise, oSunrise))
        findViewById<TextView>(R.id.tvTimeDhuhr).text = formatSafe(adjustTime(todayPrayers.dhuhr, oDhuhr))
        findViewById<TextView>(R.id.tvTimeAsr).text = formatSafe(adjustTime(todayPrayers.asr, oAsr))
        findViewById<TextView>(R.id.tvTimeMaghrib).text = formatSafe(adjustTime(todayPrayers.maghrib, oMaghrib))
        findViewById<TextView>(R.id.tvTimeIsha).text = formatSafe(adjustTime(todayPrayers.isha, oIsha))

        highlightCurrentPrayerRow(todayPrayers)
    }

    private fun highlightCurrentPrayerRow(todayPrayers: PrayerTimes) {
        val layoutFajr: View = findViewById(R.id.layoutFajr)
        val layoutSunrise: View = findViewById(R.id.layoutSunrise)
        val layoutDhuhr: View = findViewById(R.id.layoutDhuhr)
        val layoutAsr: View = findViewById(R.id.layoutAsr)
        val layoutMaghrib: View = findViewById(R.id.layoutMaghrib)
        val layoutIsha: View = findViewById(R.id.layoutIsha)

        layoutFajr.setBackgroundColor(Color.TRANSPARENT)
        layoutSunrise.setBackgroundColor(Color.TRANSPARENT)
        layoutDhuhr.setBackgroundColor(Color.TRANSPARENT)
        layoutAsr.setBackgroundColor(Color.TRANSPARENT)
        layoutMaghrib.setBackgroundColor(Color.TRANSPARENT)
        layoutIsha.setBackgroundColor(Color.TRANSPARENT)

        val theme = ThemeHelper.getThemeColors(this)
        ThemeHelper.applySystemWindowsColors(this)
        // Use a very soft alpha for highlight
        val r = android.graphics.Color.red(theme.txt)
        val g = android.graphics.Color.green(theme.txt)
        val b = android.graphics.Color.blue(theme.txt)
        val highlightColor = if (theme.isDark) {
            android.graphics.Color.argb(12, 255, 255, 255)
        } else {
            android.graphics.Color.argb(20, r, g, b)
        }

        val highlightDrawable = android.graphics.drawable.GradientDrawable().apply {
            shape = android.graphics.drawable.GradientDrawable.RECTANGLE
            cornerRadius = 24f * resources.displayMetrics.density
            setColor(highlightColor)
            setStroke((1.5f * resources.displayMetrics.density).toInt(), theme.stroke)
        }

        val currentPrayer = todayPrayers.currentPrayer()
        val now = Date()

        when (currentPrayer) {
            com.batoulapps.adhan.Prayer.FAJR -> layoutFajr.background = highlightDrawable
            com.batoulapps.adhan.Prayer.SUNRISE -> layoutSunrise.background = highlightDrawable
            com.batoulapps.adhan.Prayer.DHUHR -> layoutDhuhr.background = highlightDrawable
            com.batoulapps.adhan.Prayer.ASR -> layoutAsr.background = highlightDrawable
            com.batoulapps.adhan.Prayer.MAGHRIB -> layoutMaghrib.background = highlightDrawable
            com.batoulapps.adhan.Prayer.ISHA -> layoutIsha.background = highlightDrawable
            com.batoulapps.adhan.Prayer.NONE -> {
                layoutIsha.background = highlightDrawable
            }
        }
    }

    private fun updateCountdown() {
        val coordinates = Coordinates(latitude, longitude)
        val todayComponents = DateComponents.from(Date())
        val params = getCalcMethodParameters(selectedCalcMethodIndex)

        val todayPrayers = try {
            PrayerTimes(coordinates, todayComponents, params)
        } catch (e: Exception) {
            null
        } ?: return

        val now = Date()
        var nextPrayerName = ""
        var nextPrayerTime: Date? = null

        val prefsCore = getSharedPreferences("TasbihCore", Context.MODE_PRIVATE)
        val oFajr = prefsCore.getInt("offset_FAJR", 0)
        val oSunrise = prefsCore.getInt("offset_SUNRISE", 0)
        val oDhuhr = prefsCore.getInt("offset_DHUHR", 0)
        val oAsr = prefsCore.getInt("offset_ASR", 0)
        val oMaghrib = prefsCore.getInt("offset_MAGHRIB", 0)
        val oIsha = prefsCore.getInt("offset_ISHA", 0)

        val cFajr = adjustTime(todayPrayers.fajr, oFajr)
        val cSunrise = adjustTime(todayPrayers.sunrise, oSunrise)
        val cDhuhr = adjustTime(todayPrayers.dhuhr, oDhuhr)
        val cAsr = adjustTime(todayPrayers.asr, oAsr)
        val cMaghrib = adjustTime(todayPrayers.maghrib, oMaghrib)
        val cIsha = adjustTime(todayPrayers.isha, oIsha)

        when {
            cFajr != null && now.before(cFajr) -> {
                nextPrayerName = "الفجر"
                nextPrayerTime = cFajr
            }
            cSunrise != null && now.before(cSunrise) -> {
                nextPrayerName = "الشروق"
                nextPrayerTime = cSunrise
            }
            cDhuhr != null && now.before(cDhuhr) -> {
                nextPrayerName = "الظهر"
                nextPrayerTime = cDhuhr
            }
            cAsr != null && now.before(cAsr) -> {
                nextPrayerName = "العصر"
                nextPrayerTime = cAsr
            }
            cMaghrib != null && now.before(cMaghrib) -> {
                nextPrayerName = "المغرب"
                nextPrayerTime = cMaghrib
            }
            cIsha != null && now.before(cIsha) -> {
                nextPrayerName = "العشاء"
                nextPrayerTime = cIsha
            }
            else -> {
                val calendar = Calendar.getInstance()
                calendar.add(Calendar.DAY_OF_YEAR, 1)
                val tomorrowComponents = DateComponents.from(calendar.time)
                val tomorrowPrayers = try {
                    PrayerTimes(coordinates, tomorrowComponents, params)
                } catch (e: Exception) {
                    null
                }
                if (tomorrowPrayers != null && tomorrowPrayers.fajr != null) {
                    nextPrayerName = "الفجر"
                    nextPrayerTime = adjustTime(tomorrowPrayers.fajr, oFajr)
                }
            }
        }

        if (nextPrayerTime != null) {
            val diffMs = nextPrayerTime.time - now.time
            if (diffMs > 0) {
                val totalSecs = diffMs / 1000
                val hours = totalSecs / 3600
                val mins = (totalSecs % 3600) / 60
                val secs = totalSecs % 60

                findViewById<TextView>(R.id.tvNextPrayerTitle).text = "متبقي لصلاة $nextPrayerName"
                findViewById<TextView>(R.id.tvCountdown).text = String.format(Locale.US, "%02d:%02d:%02d", hours, mins, secs)
            } else {
                findViewById<TextView>(R.id.tvNextPrayerTitle).text = "حان الآن وقت صلاة $nextPrayerName"
                findViewById<TextView>(R.id.tvCountdown).text = "00:00:00"
            }
        } else {
            findViewById<TextView>(R.id.tvNextPrayerTitle).text = "غير متاح"
            findViewById<TextView>(R.id.tvCountdown).text = "--:--:--"
        }
    }

    private fun requestAutoLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                PERMISSION_REQUEST_LOCATION
            )
        } else {
            getLocationAndCalculate()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_LOCATION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getLocationAndCalculate()
            } else {
                Toast.makeText(this, "عذراً، يجب السماح بالوصول للموقع لتحديده تلقائياً", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun getLocationAndCalculate() {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        Toast.makeText(this, "جاري تحديد الموقع تلقائياً...", Toast.LENGTH_SHORT).show()

        val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        val isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

        if (!isGpsEnabled && !isNetworkEnabled) {
            Toast.makeText(this, "الرجاء تفعيل خدمة تحديد الموقع (GPS)", Toast.LENGTH_LONG).show()
            startActivity(Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            return
        }

        var loc: Location? = null
        if (isNetworkEnabled) {
            loc = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
        }
        if (loc == null && isGpsEnabled) {
            loc = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        }

        if (loc != null) {
            onNewLocationFetched(loc)
        } else {
            val provider = if (isNetworkEnabled) LocationManager.NETWORK_PROVIDER else LocationManager.GPS_PROVIDER
            try {
                @Suppress("DEPRECATION")
                locationManager.requestSingleUpdate(provider, object : LocationListener {
                    override fun onLocationChanged(location: Location) {
                        onNewLocationFetched(location)
                    }
                    @Deprecated("Deprecated in Java")
                    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                    override fun onProviderEnabled(provider: String) {}
                    override fun onProviderDisabled(provider: String) {}
                }, null)
            } catch (e: Exception) {
                Toast.makeText(this, "عذراً، تعذر جلب الموقع الحالي تلقائياً", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun onNewLocationFetched(loc: Location) {
        latitude = loc.latitude
        longitude = loc.longitude
        cityName = "جاري البحث..."
        regionName = "جاري البحث..."

        findViewById<TextView>(R.id.tvCityName)?.text = cityName
        findViewById<TextView>(R.id.tvRegion)?.text = regionName
        findViewById<TextView>(R.id.tvCoordinates)?.text = String.format(Locale.US, "الإحداثيات: %.4f, %.4f", latitude, longitude)

        val prefs = getSharedPreferences("app", MODE_PRIVATE)
        prefs.edit().apply {
            putFloat("athan_latitude", latitude.toFloat())
            putFloat("athan_longitude", longitude.toFloat())
        }.apply()

        fetchCityNameFromCoords(latitude, longitude)
        updatePrayerTimes()
        Toast.makeText(this, "تم تحديث الموقع بنجاح", Toast.LENGTH_SHORT).show()
    }

    private fun fetchCityNameFromCoords(lat: Double, lng: Double) {
        Thread {
            try {
                val geocoder = Geocoder(this@AthanActivity, Locale("ar"))
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(lat, lng, 1)
                if (!addresses.isNullOrEmpty()) {
                    val address = addresses[0]
                    cityName = address.locality ?: address.subAdminArea ?: address.adminArea ?: "موقع مخصص"
                    
                    // Format region name
                    val country = address.countryName ?: ""
                    val adminArea = address.adminArea ?: ""
                    regionName = if (country.isNotEmpty() && adminArea.isNotEmpty()) {
                        "$adminArea, $country"
                    } else if (country.isNotEmpty()) {
                        country
                    } else {
                        TimeZone.getDefault().id
                    }

                    runOnUiThread {
                        findViewById<TextView>(R.id.tvCityName).text = cityName
                        findViewById<TextView>(R.id.tvRegion).text = regionName

                        val prefs = getSharedPreferences("app", MODE_PRIVATE)
                        prefs.edit().apply {
                            putString("athan_city_name", cityName)
                            putString("athan_region_name", regionName)
                        }.apply()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }

    private fun showManualLocationDialog() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val density = resources.displayMetrics.density
            val paddingDp = (16 * density).toInt()
            setPadding(paddingDp, paddingDp, paddingDp, paddingDp)
        }

        val etLat = EditText(this).apply {
            hint = "خط العرض (Latitude) مثل: 24.7136"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL or android.text.InputType.TYPE_NUMBER_FLAG_SIGNED
            setText(latitude.toString())
        }

        val etLng = EditText(this).apply {
            hint = "خط الطول (Longitude) مثل: 46.6753"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL or android.text.InputType.TYPE_NUMBER_FLAG_SIGNED
            setText(longitude.toString())
        }

        layout.addView(etLat)
        layout.addView(etLng)

        AlertDialog.Builder(this)
            .setTitle("إدخال الإحداثيات يدوياً")
            .setView(layout)
            .setPositiveButton("حفظ") { _, _ ->
                val latStr = etLat.text.toString().trim()
                val lngStr = etLng.text.toString().trim()
                if (latStr.isNotEmpty() && lngStr.isNotEmpty()) {
                    val newLat = latStr.toDoubleOrNull()
                    val newLng = lngStr.toDoubleOrNull()
                    if (newLat != null && newLng != null) {
                        latitude = newLat
                        longitude = newLng
                        cityName = "موقع مخصص"
                        regionName = "موقع مخصص"

                        findViewById<TextView>(R.id.tvCityName)?.text = cityName
                        findViewById<TextView>(R.id.tvRegion)?.text = regionName
                        findViewById<TextView>(R.id.tvCoordinates)?.text = String.format(Locale.US, "الإحداثيات: %.4f, %.4f", latitude, longitude)

                        val prefs = getSharedPreferences("app", MODE_PRIVATE)
                        prefs.edit().apply {
                            putFloat("athan_latitude", newLat.toFloat())
                            putFloat("athan_longitude", newLng.toFloat())
                            putString("athan_city_name", cityName)
                            putString("athan_region_name", regionName)
                        }.apply()

                        fetchCityNameFromCoords(newLat, newLng)
                        updatePrayerTimes()
                        Toast.makeText(this, "تم حفظ الإحداثيات يدوياً", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "إحداثيات غير صالحة", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("إلغاء", null)
            .show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_RINGTONE && resultCode == RESULT_OK) {
            @Suppress("DEPRECATION")
            val uri = data?.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            selectedRingtoneUri = uri?.toString() ?: "default"

            val prefs = getSharedPreferences("app", MODE_PRIVATE)
            prefs.edit().putString("athan_sound_uri", selectedRingtoneUri).apply()

            updateSoundPathText()
        }
    }

    private fun updateSoundPathText() {
        val tvSelectedSoundPath: TextView = findViewById(R.id.tvSelectedSoundPath)
        if (selectedRingtoneUri == null || selectedRingtoneUri == "default") {
            tvSelectedSoundPath.text = "الصوت المحدد: الافتراضي"
        } else {
            try {
                val ringtone = RingtoneManager.getRingtone(this, Uri.parse(selectedRingtoneUri))
                val title = ringtone.getTitle(this)
                tvSelectedSoundPath.text = "الصوت المحدد: $title"
            } catch (e: Exception) {
                tvSelectedSoundPath.text = "الصوت المحدد: مخصص"
            }
        }
    }

    private fun toggleBottomBar() {
        val bottomBarLayout = findViewById<View>(R.id.bottomBarLayout) ?: return
        isBottomBarVisible = !isBottomBarVisible
        bottomBarLayout.animate()
            .translationY(if (isBottomBarVisible) 0f else bottomBarLayout.height.toFloat() + 100f)
            .setDuration(300).start()
    }

    private fun updateDates() {
        val gregorianCalendar = java.util.Calendar.getInstance()
        val d = gregorianCalendar.get(java.util.Calendar.DAY_OF_MONTH)
        val m = gregorianCalendar.get(java.util.Calendar.MONTH)
        val y = gregorianCalendar.get(java.util.Calendar.YEAR)
        
        val gregMonths = arrayOf(
            "يناير (كانون الثاني)", "فبراير (شباط)", "مارس (آذار)", "أبريل (نيسان)", 
            "مايو (أيار)", "يونيو (حزيران)", "يوليو (تموز)", "أغسطس (آب)", 
            "سبتمبر (أيلول)", "أكتوبر (تشرين الأول)", "نوفمبر (تشرين الثاني)", "ديسمبر (كانون الأول)"
        )
        val gregorianStr = "$d ${gregMonths[m]} $y"
        findViewById<android.widget.TextView>(R.id.tvGregorianDate)?.text = gregorianStr

        var hijriStr = ""
        try {
            val hijriCalendar = android.icu.util.IslamicCalendar()
            hijriCalendar.add(android.icu.util.Calendar.DAY_OF_MONTH, 2) // Fix Hijri date offset
            val hd = hijriCalendar.get(android.icu.util.Calendar.DAY_OF_MONTH)
            val hm = hijriCalendar.get(android.icu.util.Calendar.MONTH)
            val hy = hijriCalendar.get(android.icu.util.Calendar.YEAR)
            
            val hijriMonths = arrayOf(
                "محرم", "صفر", "ربيع الأول", "ربيع الآخر", 
                "جمادى الأولى", "جمادى الآخرة", "رجب", "شعبان", 
                "رمضان", "شوال", "ذو القعدة", "ذو الحجة"
            )
            val hijriMonthName = if (hm in 0..11) hijriMonths[hm] else ""
            hijriStr = "$hd $hijriMonthName $hy"
        } catch (e: Throwable) {
            hijriStr = "التاريخ الهجري غير مدعوم"
        }
        findViewById<android.widget.TextView>(R.id.tvHijriDate)?.text = hijriStr
    }
}
