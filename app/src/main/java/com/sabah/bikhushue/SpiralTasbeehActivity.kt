package com.sabah.bikhushue

import android.content.Context
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.google.android.material.card.MaterialCardView

class SpiralTasbeehActivity : AppCompatActivity() {

    private lateinit var tvMainCounter: TextView
    private lateinit var tvHundredsCounter: TextView
    private lateinit var tvSalawatStatic: TextView
    private lateinit var beadsCircle: SpiralBeadsView
    private lateinit var tasbeehFrame: View
    private lateinit var btnReset: ImageView
    private lateinit var vibrator: Vibrator
    private lateinit var audioManager: android.media.AudioManager

    private var counter = 0
    private var hundredsCounter = 0
    private val GOAL = 100

    private val isIstighfarMode: Boolean
        get() = intent.getStringExtra("mode") == "istighfar"

    private val syncReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: android.content.Intent?) {
            if (intent?.action == "com.sabah.bikhushue.SYNC_SALAWAT" || intent?.action == "com.sabah.bikhushue.SYNC_ISTIGHFAR") {
                loadCounter()
                updateUI(true)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_spiral_tasbeeh)

        audioManager = getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager

        val filter = android.content.IntentFilter().apply {
            addAction("com.sabah.bikhushue.SYNC_SALAWAT")
            addAction("com.sabah.bikhushue.SYNC_ISTIGHFAR")
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(syncReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(syncReceiver, filter)
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.tasbeehRoot)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        tvMainCounter = findViewById(R.id.tvMainCounter)
        tvHundredsCounter = findViewById(R.id.tvHundredsCounter)
        tvSalawatStatic = findViewById(R.id.tvSalawatStatic)
        beadsCircle = findViewById(R.id.beadsCircle)
        tasbeehFrame = findViewById(R.id.tasbeehFrame)
        btnReset = findViewById(R.id.tvResetCounter)

        if (isIstighfarMode) {
            tvSalawatStatic.text = "استغفر الله"
            findViewById<TextView>(R.id.tvSalawatQuote)?.text = "فَقُلْتُ اسْتَغْفِرُوا رَبَّكُمْ إِنَّهُ كَانَ غَفَّارًا"
        }

        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        BottomBarHelper.setupBottomBar(this, onThemeChanged = { applyTheme() })

        loadCounter()
        updateUI(false)

        tasbeehFrame.setOnClickListener {
            counter++
            
            // Fixed Goal 100 as per user request (number does not exceed 100)
            if (counter > 100) {
                counter = 0 // "Zeroes out" (يصفر)
                hundredsCounter++
                playGoalHaptic()
                playGoalSound()
            } else if (counter == 100) {
                // Keep showing 100
                playGoalHaptic()
                playGoalSound()
            } else {
                playClickHaptic()
                playClickSound()
            }

            saveCounter()
            updateUI(true)
        }

        btnReset.setOnClickListener {
            counter = 0
            hundredsCounter = 0
            saveCounter()
            updateUI(false)
        }
    }

    override fun onResume() {
        super.onResume()
        applyTheme()
        if (isIstighfarMode) {
            tvSalawatStatic.text = "استغفر الله"
            findViewById<TextView>(R.id.tvSalawatQuote)?.text = "فَقُلْتُ اسْتَغْفِرُوا رَبَّكُمْ إِنَّهُ كَانَ غَفَّارًا"
        }
        loadCounter()
        updateUI(false)
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(syncReceiver)
        } catch (e: Exception) {}
    }

    private fun applyTheme() {
        val theme = ThemeHelper.getThemeColors(this)
        ThemeHelper.applySystemWindowsColors(this)
        
        beadsCircle.setNightOrLunar(theme.isDark)
        
        findViewById<View>(R.id.tasbeehRoot).setBackgroundColor(theme.bg)
        val windowInsetsController = WindowInsetsControllerCompat(window, window.decorView)
        windowInsetsController.isAppearanceLightStatusBars = !theme.isDark
        windowInsetsController.isAppearanceLightNavigationBars = !theme.isDark

        findViewById<MaterialCardView>(R.id.tasbeehMainCard).strokeColor = theme.bar
        findViewById<MaterialCardView>(R.id.cardZikrShadow).setCardBackgroundColor(theme.shadow)
        findViewById<MaterialCardView>(R.id.cardZikrInner).setCardBackgroundColor(theme.cardBg)
        findViewById<MaterialCardView>(R.id.tasbeehMainShadow).setCardBackgroundColor(theme.shadow)
        findViewById<MaterialCardView>(R.id.tasbeehMainCard).setCardBackgroundColor(theme.cardBg)
        
        tvMainCounter.setTextColor(theme.txt)
        tvHundredsCounter.setTextColor(theme.txt)
        tvSalawatStatic.setTextColor(theme.txt)
        
        val tvSalawatQuote = findViewById<TextView>(R.id.tvSalawatQuote)
        tvSalawatQuote.setTextColor(if (theme.isDark) Color.parseColor("#BBBBBB") else Color.parseColor("#A08C73"))

        // Adjust beads colors based on theme
        val stringColor = if (theme.isDark) Color.parseColor("#444444") else Color.parseColor("#8C5C38")
        val pearl = if (theme.isDark) Color.parseColor("#E0E0E0") else Color.parseColor("#FFFFFF") // White / Light gray
        val idle = if (theme.isDark) Color.parseColor("#388E3C") else Color.parseColor("#81C784") // Light Green
        val center = if (theme.isDark) Color.parseColor("#222222") else Color.parseColor("#FDF5E6")
        beadsCircle.setThemeColors(idle, pearl, pearl, center, stringColor)
    }

    private fun updateUI(animate: Boolean) {
        tvMainCounter.text = counter.toString()
        tvHundredsCounter.text = "الدورات: $hundredsCounter"
        beadsCircle.setGoalAndCount(GOAL, counter, animate)
    }

    private fun loadCounter() {
        val prefs = getSharedPreferences("SalawatProgress", Context.MODE_PRIVATE)
        val countKey = if (isIstighfarMode) "istighfar_count" else "salawat_count"
        val roundsKey = if (isIstighfarMode) "istighfar_rounds" else "salawat_rounds"
        counter = prefs.getInt(countKey, 0)
        hundredsCounter = prefs.getInt(roundsKey, 0)
    }

    private fun saveCounter() {
        val prefs = getSharedPreferences("SalawatProgress", Context.MODE_PRIVATE)
        val countKey = if (isIstighfarMode) "istighfar_count" else "salawat_count"
        val roundsKey = if (isIstighfarMode) "istighfar_rounds" else "salawat_rounds"
        prefs.edit()
            .putInt(countKey, counter)
            .putInt(roundsKey, hundredsCounter)
            .apply()

        // Update all widgets
        updateAllWidgets()
        
        // Notify other parts of the app (if open)
        val action = if (isIstighfarMode) "com.sabah.bikhushue.SYNC_ISTIGHFAR" else "com.sabah.bikhushue.SYNC_SALAWAT"
        sendBroadcast(android.content.Intent(action).apply {
            setPackage(packageName)
        })
    }

    private fun updateAllWidgets() {
        val appWidgetManager = android.appwidget.AppWidgetManager.getInstance(this)
        
        // Update Tasbeeh Widgets
        val tasbeehIntent = android.content.Intent(this, TasbeehWidgetProvider::class.java).apply { 
            action = android.appwidget.AppWidgetManager.ACTION_APPWIDGET_UPDATE 
        }
        val tasbeehIds = appWidgetManager.getAppWidgetIds(android.content.ComponentName(this, TasbeehWidgetProvider::class.java))
        tasbeehIntent.putExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_IDS, tasbeehIds)
        sendBroadcast(tasbeehIntent)

        // Update Prayer Widgets (Night and Day)
        val prayerIntent = android.content.Intent(this, PrayerWidgetProvider::class.java).apply { action = android.appwidget.AppWidgetManager.ACTION_APPWIDGET_UPDATE }
        val idsNight = appWidgetManager.getAppWidgetIds(android.content.ComponentName(this, PrayerWidgetProvider::class.java))
        val idsDay = appWidgetManager.getAppWidgetIds(android.content.ComponentName(this, PrayerWidgetDayProvider::class.java))
        
        val allPrayerIds = idsNight + idsDay
        prayerIntent.putExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_IDS, allPrayerIds)
        sendBroadcast(prayerIntent)
    }

    private fun playClickHaptic() {
        if (!vibrator.hasVibrator()) return
        val prefs = getSharedPreferences("app", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("vibrate_enabled", true)) return
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(10, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(10)
        }
    }

    private fun playGoalHaptic() {
        if (!vibrator.hasVibrator()) return
        val prefs = getSharedPreferences("app", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("vibrate_enabled", true)) return
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val timings = longArrayOf(0, 100, 50, 100)
            val amplitudes = intArrayOf(0, 255, 0, 255)
            vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(longArrayOf(0, 100, 50, 100), -1)
        }
    }

    private fun playClickSound() {
        val prefs = getSharedPreferences("app", Context.MODE_PRIVATE)
        if (prefs.getBoolean("sound_on", true)) {
            try {
                audioManager.playSoundEffect(android.media.AudioManager.FX_KEY_CLICK, 0.4f)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun playGoalSound() {
        val prefs = getSharedPreferences("app", Context.MODE_PRIVATE)
        if (prefs.getBoolean("sound_on", true)) {
            try {
                audioManager.playSoundEffect(android.media.AudioManager.FX_KEY_CLICK, 1.0f)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
