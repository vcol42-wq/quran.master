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

    private var counter = 0
    private var hundredsCounter = 0
    private val GOAL = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_spiral_tasbeeh)

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
            if (counter < GOAL) {
                playClickHaptic()
            } else {
                counter = 0
                hundredsCounter++
                playGoalHaptic()
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
    }

    private fun applyTheme() {
        val theme = ThemeHelper.getThemeColors(this)
        
        findViewById<View>(R.id.tasbeehRoot).setBackgroundColor(theme.bg)
        window.statusBarColor = theme.bar
        window.navigationBarColor = theme.bar
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
        val prefs = getSharedPreferences("spiral_tasbeeh", Context.MODE_PRIVATE)
        counter = prefs.getInt("counter", 0)
        hundredsCounter = prefs.getInt("hundreds", 0)
    }

    private fun saveCounter() {
        val prefs = getSharedPreferences("spiral_tasbeeh", Context.MODE_PRIVATE)
        prefs.edit()
            .putInt("counter", counter)
            .putInt("hundreds", hundredsCounter)
            .apply()
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
}
