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

class AthanActivity : AppCompatActivity() {

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
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_athan)

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
        findViewById<TextView>(R.id.tvCityName).text = cityName
        findViewById<TextView>(R.id.tvRegion).text = regionName
        findViewById<TextView>(R.id.tvCoordinates).text = String.format(Locale.US, "الإحداثيات: %.4f, %.4f", latitude, longitude)

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

        // Spinner Setup for Calculation Method
        val spinnerCalcMethod: Spinner = findViewById(R.id.spinnerCalcMethod)
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
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, methods)
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
        findViewById<MaterialButton>(R.id.btnChooseSound).setOnClickListener {
            val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION or RingtoneManager.TYPE_RINGTONE)
                putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "اختر صوت الأذان")
                putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true)
                if (selectedRingtoneUri != null && selectedRingtoneUri != "default") {
                    putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(selectedRingtoneUri))
                }
            }
            startActivityForResult(intent, REQUEST_CODE_RINGTONE)
        }

        updateSoundPathText()
        applyTheme()
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
        super.onResume()
        applyTheme()
        updatePrayerTimes()
    }

    private fun applyTheme() {
        val theme = ThemeHelper.getThemeColors(this)
        val bgColor = theme.bg
        val txtColor = theme.txt
        val barColor = theme.bar
        val cardBgColor = theme.cardBg

        val root: View = findViewById(R.id.scrollView)
        root.setBackgroundColor(bgColor)

        window.statusBarColor = barColor
        window.navigationBarColor = barColor
        if (theme.isDark) {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = window.decorView.systemUiVisibility and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = window.decorView.systemUiVisibility or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        }

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

        val timeFormat = SimpleDateFormat("hh:mm a", Locale("ar"))
        timeFormat.timeZone = TimeZone.getDefault()

        findViewById<TextView>(R.id.tvTimeFajr).text = timeFormat.format(todayPrayers.fajr)
        findViewById<TextView>(R.id.tvTimeSunrise).text = timeFormat.format(todayPrayers.sunrise)
        findViewById<TextView>(R.id.tvTimeDhuhr).text = timeFormat.format(todayPrayers.dhuhr)
        findViewById<TextView>(R.id.tvTimeAsr).text = timeFormat.format(todayPrayers.asr)
        findViewById<TextView>(R.id.tvTimeMaghrib).text = timeFormat.format(todayPrayers.maghrib)
        findViewById<TextView>(R.id.tvTimeIsha).text = timeFormat.format(todayPrayers.isha)

        highlightNextPrayerRow(todayPrayers)
    }

    private fun highlightNextPrayerRow(todayPrayers: PrayerTimes) {
        val now = Date()

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

        val prefs = getSharedPreferences("app", MODE_PRIVATE)
        val barColorStr = prefs.getString("bar_color", "#E6DCC8") ?: "#E6DCC8"
        val highlightColor = Color.parseColor(barColorStr)

        val nextPrayerName = when {
            now.before(todayPrayers.fajr) -> "الفجر"
            now.before(todayPrayers.sunrise) -> "الشروق"
            now.before(todayPrayers.dhuhr) -> "الظهر"
            now.before(todayPrayers.asr) -> "العصر"
            now.before(todayPrayers.maghrib) -> "المغرب"
            now.before(todayPrayers.isha) -> "العشاء"
            else -> "الفجر"
        }

        when (nextPrayerName) {
            "الفجر" -> layoutFajr.setBackgroundColor(highlightColor)
            "الشروق" -> layoutSunrise.setBackgroundColor(highlightColor)
            "الظهر" -> layoutDhuhr.setBackgroundColor(highlightColor)
            "العصر" -> layoutAsr.setBackgroundColor(highlightColor)
            "المغرب" -> layoutMaghrib.setBackgroundColor(highlightColor)
            "العشاء" -> layoutIsha.setBackgroundColor(highlightColor)
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
        var nextPrayerTime = Date()

        when {
            now.before(todayPrayers.fajr) -> {
                nextPrayerName = "الفجر"
                nextPrayerTime = todayPrayers.fajr
            }
            now.before(todayPrayers.sunrise) -> {
                nextPrayerName = "الشروق"
                nextPrayerTime = todayPrayers.sunrise
            }
            now.before(todayPrayers.dhuhr) -> {
                nextPrayerName = "الظهر"
                nextPrayerTime = todayPrayers.dhuhr
            }
            now.before(todayPrayers.asr) -> {
                nextPrayerName = "العصر"
                nextPrayerTime = todayPrayers.asr
            }
            now.before(todayPrayers.maghrib) -> {
                nextPrayerName = "المغرب"
                nextPrayerTime = todayPrayers.maghrib
            }
            now.before(todayPrayers.isha) -> {
                nextPrayerName = "العشاء"
                nextPrayerTime = todayPrayers.isha
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
                if (tomorrowPrayers != null) {
                    nextPrayerName = "الفجر"
                    nextPrayerTime = tomorrowPrayers.fajr
                }
            }
        }

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

        findViewById<TextView>(R.id.tvCityName).text = cityName
        findViewById<TextView>(R.id.tvRegion).text = regionName
        findViewById<TextView>(R.id.tvCoordinates).text = String.format(Locale.US, "الإحداثيات: %.4f, %.4f", latitude, longitude)

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

                        findViewById<TextView>(R.id.tvCityName).text = cityName
                        findViewById<TextView>(R.id.tvRegion).text = regionName
                        findViewById<TextView>(R.id.tvCoordinates).text = String.format(Locale.US, "الإحداثيات: %.4f, %.4f", latitude, longitude)

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
        val hijriStr = "$hd $hijriMonthName $hy"
        findViewById<android.widget.TextView>(R.id.tvHijriDate)?.text = hijriStr
    }
}
