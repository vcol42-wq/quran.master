package com.sabah.bikhushue

import android.content.Context
import android.graphics.Color
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.GestureDetector
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

class SalawatActivity : AppCompatActivity() {

    private lateinit var tvMainCounter: TextView
    private lateinit var tvGoal100: TextView
    private lateinit var tvGoal500: TextView
    private lateinit var tvGoal1000: TextView
    private lateinit var tvGoal5000: TextView
    private lateinit var beadsCircle: BeadsCircleView
    private lateinit var tvResetCounter: ImageView
    private lateinit var tasbeehRoot: ConstraintLayout

    private var currentCount = 0
    private var currentGoal = 1000

    private lateinit var audioManager: AudioManager
    private lateinit var vibrator: Vibrator
    private lateinit var gestureDetector: GestureDetector

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_salawat)

        tvMainCounter = findViewById(R.id.tvMainCounter)
        tvGoal100 = findViewById(R.id.tvGoal100)
        tvGoal500 = findViewById(R.id.tvGoal500)
        tvGoal1000 = findViewById(R.id.tvGoal1000)
        tvGoal5000 = findViewById(R.id.tvGoal5000)
        beadsCircle = findViewById(R.id.beadsCircle)
        tvResetCounter = findViewById(R.id.tvResetCounter)
        tasbeehRoot = findViewById(R.id.tasbeehRoot)

        ViewCompat.setOnApplyWindowInsetsListener(tasbeehRoot) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        loadProgress()
        applyTheme()
        setupListeners()
        refreshGoalButtonsUI()
        updateUI(animate = false)

        BottomBarHelper.setupBottomBar(this)

        tasbeehRoot.setOnClickListener {
            doCount()
        }
    }

    private fun applyTheme() {
        val theme = ThemeHelper.getThemeColors(this)
        
        // We override theme slightly for a distinct Salawat look
        val isDark = theme.isDark
        val bgColor = if (isDark) Color.parseColor("#0B2B1B") else Color.parseColor("#E8F5E9")
        val cardColor = if (isDark) Color.parseColor("#143B26") else Color.parseColor("#FFFFFF")
        val shadowColor = if (isDark) Color.parseColor("#1B5E20") else Color.parseColor("#81C784")
        val txtColor = if (isDark) Color.parseColor("#E8F5E9") else Color.parseColor("#1B5E20")
        
        tasbeehRoot.setBackgroundColor(bgColor)
        
        window.statusBarColor = shadowColor
        window.navigationBarColor = shadowColor
        val windowInsetsController = WindowInsetsControllerCompat(window, window.decorView)
        windowInsetsController.isAppearanceLightStatusBars = !isDark
        windowInsetsController.isAppearanceLightNavigationBars = !isDark
        
        val tasbeehMainCard = findViewById<com.google.android.material.card.MaterialCardView>(R.id.tasbeehMainCard)
        val tasbeehMainShadow = findViewById<com.google.android.material.card.MaterialCardView>(R.id.tasbeehMainShadow)

        tasbeehMainCard?.setCardBackgroundColor(cardColor)
        tasbeehMainShadow?.setCardBackgroundColor(shadowColor)
        tasbeehMainCard?.strokeColor = shadowColor
        
        tvMainCounter.setTextColor(txtColor)
        tvGoal100.setTextColor(txtColor)
        tvGoal500.setTextColor(txtColor)
        tvGoal1000.setTextColor(txtColor)
        tvGoal5000.setTextColor(txtColor)
        findViewById<TextView>(R.id.tvSalawatTitle)?.setTextColor(txtColor)
        
        beadsCircle.setCenterColor(cardColor)
        tvResetCounter.setColorFilter(txtColor)
    }

    private fun setupListeners() {
        val goals = listOf(tvGoal100 to 100, tvGoal500 to 500, tvGoal1000 to 1000, tvGoal5000 to 5000)
        
        for ((tv, g) in goals) {
            tv.setOnClickListener {
                currentGoal = g
                currentCount = 0
                refreshGoalButtonsUI()
                updateUI(animate = false)
            }
        }

        gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDown(e: MotionEvent): Boolean = true
            override fun onSingleTapUp(e: MotionEvent): Boolean {
                doCount()
                return true
            }
        })

        val tasbeehFrame = findViewById<View>(R.id.tasbeehFrame)
        if (tasbeehFrame != null) {
            tasbeehFrame.setOnTouchListener { _, event ->
                gestureDetector.onTouchEvent(event)
                true
            }
        }
        
        tvResetCounter.setOnClickListener {
            currentCount = 0
            updateUI(animate = false)
            Toast.makeText(this, "تم تصفير العداد", Toast.LENGTH_SHORT).show()
        }
    }

    private fun refreshGoalButtonsUI() {
        val theme = ThemeHelper.getThemeColors(this)
        val txtColor = if (theme.isDark) Color.parseColor("#E8F5E9") else Color.parseColor("#1B5E20")
        
        val goals = listOf(tvGoal100 to 100, tvGoal500 to 500, tvGoal1000 to 1000, tvGoal5000 to 5000)
        
        for ((tv, g) in goals) {
            if (g == currentGoal) {
                tv.alpha = 1.0f
                tv.setTextColor(Color.parseColor("#C62828")) // Reddish to stand out
                tv.setTypeface(null, android.graphics.Typeface.BOLD)
                tv.animate().scaleX(1.2f).scaleY(1.2f).setDuration(200).start()
            } else {
                tv.alpha = 0.7f
                tv.setTextColor(txtColor)
                tv.setTypeface(null, android.graphics.Typeface.NORMAL)
                tv.animate().scaleX(1.0f).scaleY(1.0f).setDuration(200).start()
            }
        }
    }

    private fun doCount() {
        if (currentCount < currentGoal) {
            currentCount++
            val prefs = getSharedPreferences("app", MODE_PRIVATE)
            if (prefs.getBoolean("vibration_on", true)) playVibrateTick()
            if (prefs.getBoolean("sound_on", true)) playSoundTick()
            updateUI(animate = true)
            
            if (currentCount == currentGoal) {
                if (prefs.getBoolean("vibration_on", true)) playVibrateGoalReached()
                if (prefs.getBoolean("sound_on", true)) playSoundGoalReached()
            }
        } else {
            currentCount = 1
            val prefs = getSharedPreferences("app", MODE_PRIVATE)
            if (prefs.getBoolean("vibration_on", true)) playVibrateTick()
            if (prefs.getBoolean("sound_on", true)) playSoundTick()
            updateUI(animate = true)
        }
    }
    
    private fun playVibrateTick() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
        } else {
            tasbeehRoot.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        }
    }
    
    private fun playSoundTick() {
        audioManager.playSoundEffect(AudioManager.FX_KEY_CLICK, 0.3f)
    }
    
    private fun playVibrateGoalReached() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 100, 50, 100), -1))
        } else {
            vibrator.vibrate(300)
        }
    }
    
    private fun playSoundGoalReached() {
        audioManager.playSoundEffect(AudioManager.FX_KEY_CLICK, 1.0f)
    }

    private fun updateUI(animate: Boolean) {
        tvMainCounter.text = (currentGoal - currentCount).toString()
        beadsCircle.setGoalAndCount(currentGoal, currentCount, animate)
    }

    override fun onPause() {
        super.onPause()
        saveProgress()
    }

    private fun loadProgress() {
        val prefs = getSharedPreferences("SalawatProgress", MODE_PRIVATE)
        currentCount = prefs.getInt("salawat_count", 0)
        currentGoal = prefs.getInt("salawat_goal", 1000)
    }

    private fun saveProgress() {
        val prefs = getSharedPreferences("SalawatProgress", MODE_PRIVATE)
        prefs.edit()
            .putInt("salawat_count", currentCount)
            .putInt("salawat_goal", currentGoal)
            .apply()
    }
}
