package com.example.quranmaster

import android.app.AlertDialog
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
import android.view.animation.AnimationUtils
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextSwitcher
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.widget.TextViewCompat
import kotlin.math.abs

data class ZikrState(var text: String, var count: Int, var goal: Int)

class TasbeehActivity : AppCompatActivity() {

    private lateinit var tsZikrWheel: TextSwitcher
    private lateinit var tvMainCounter: TextView
    private lateinit var tvGoal33: TextView
    private lateinit var tvGoal10: TextView
    private lateinit var tvGoal100: TextView
    private lateinit var tvGoal1000: TextView
    private lateinit var beadsCircle: BeadsCircleView
    
    private lateinit var tvAddZikr: ImageView
    private lateinit var tvResetCounter: ImageView
    
    private lateinit var tvPrevZikr: TextView
    private lateinit var tvNextZikr: TextView
    private lateinit var llZikrContainer: View
    
    private lateinit var tasbeehRoot: ConstraintLayout

    private var isBottomBarVisible = true
    private val hideHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private val showRunnable = Runnable { if (!isBottomBarVisible) toggleBottomBar() }

    private val zikrs = mutableListOf(
        ZikrState("سبحان الله", 0, 33),
        ZikrState("الحمد لله", 0, 33),
        ZikrState("الله أكبر", 0, 33),
        ZikrState("لا إله إلا الله", 0, 33),
        ZikrState("لا حول ولا قوة إلا بالله", 0, 33),
        ZikrState("أستغفر الله", 0, 100),
        ZikrState("اللهم صل وسلم على نبينا محمد", 0, 10)
    )
    private var currentZikrIndex = 0

    private lateinit var audioManager: AudioManager
    private lateinit var vibrator: Vibrator
    private lateinit var gestureDetector: GestureDetector

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tasbeeh)

        // Initialize Views
        tsZikrWheel = findViewById(R.id.tsZikrWheel)
        tvMainCounter = findViewById(R.id.tvMainCounter)
        tvGoal33 = findViewById(R.id.tvGoal33)
        tvGoal10 = findViewById(R.id.tvGoal10)
        tvGoal100 = findViewById(R.id.tvGoal100)
        tvGoal1000 = findViewById(R.id.tvGoal1000)
        beadsCircle = findViewById(R.id.beadsCircle)
        
        tvAddZikr = findViewById(R.id.tvAddZikr)
        tvResetCounter = findViewById(R.id.tvResetCounter)
        tvPrevZikr = findViewById(R.id.tvPrevZikr)
        tvNextZikr = findViewById(R.id.tvNextZikr)
        llZikrContainer = findViewById(R.id.llZikrContainer)
        tasbeehRoot = findViewById(R.id.tasbeehRoot)

        // Initialize Services
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        applyTheme()
        setupZikrWheel()
        setupListeners()
        updateUI(animate = false)

        BottomBarHelper.setupBottomBar(this)

        tasbeehRoot.setOnClickListener {
            doCount()
        }

        val prefs = getSharedPreferences("app", MODE_PRIVATE)
        if (prefs.getBoolean("is_first_time_goals", true)) {
            AlertDialog.Builder(this)
                .setTitle("تعليمات الأهداف")
                .setMessage("يمكنك تحديد هدفك من التسبيح (10، 33، 100، 1000) بالضغط على الأرقام في الأعلى.\n\n" +
                        "عند الوصول للهدف، سيقوم التطبيق بالاهتزاز والانتقال للذكر التالي تلقائياً.\n\n" +
                        "يمكنك أيضاً التمرير لليمين أو اليسار للتنقل بين الأذكار يدوياً.")
                .setPositiveButton("فهمت") { dialog, _ -> dialog.dismiss() }
                .setCancelable(false)
                .show()
            prefs.edit().putBoolean("is_first_time_goals", false).apply()
        }
    }

    private fun applyTheme() {
        val prefs = getSharedPreferences("app", MODE_PRIVATE)
        val savedBg = prefs.getString("bg_color", "#F4ECD8") ?: "#F4ECD8"
        val bgColor = Color.parseColor(savedBg)
        tasbeehRoot.setBackgroundColor(bgColor)

        val barColorInt = Color.parseColor(prefs.getString("bar_color", "#E6DCC8") ?: "#E6DCC8")
        window.statusBarColor = barColorInt
        window.navigationBarColor = barColorInt

        val isDarkMode = savedBg == "#121212"
        if (isDarkMode) {
            window.decorView.systemUiVisibility = window.decorView.systemUiVisibility and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
        } else {
            window.decorView.systemUiVisibility = window.decorView.systemUiVisibility or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        }
        
        val tasbeehMainCard = findViewById<com.google.android.material.card.MaterialCardView>(R.id.tasbeehMainCard)
        tasbeehMainCard?.setCardBackgroundColor(if (isDarkMode) Color.parseColor("#2D2D2D") else Color.WHITE)
        
        val baseTextColor = Color.parseColor(prefs.getString("text_color", "#3D2B1F") ?: "#3D2B1F")
        val tasbeehTextColor = if (isDarkMode) Color.WHITE else baseTextColor
        
        tvMainCounter.setTextColor(tasbeehTextColor)
        tvGoal10.setTextColor(tasbeehTextColor)
        tvGoal33.setTextColor(tasbeehTextColor)
        tvGoal100.setTextColor(tasbeehTextColor)
        tvGoal1000.setTextColor(tasbeehTextColor)
        tvPrevZikr.setTextColor(baseTextColor)
        tvNextZikr.setTextColor(baseTextColor)
        tvResetCounter.setColorFilter(tasbeehTextColor)
        tvAddZikr.setColorFilter(tasbeehTextColor)
    }

    private fun setupZikrWheel() {
        tsZikrWheel.setFactory {
            val t = TextView(this)
            t.gravity = android.view.Gravity.CENTER
            t.layoutParams = android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT
            )
            TextViewCompat.setAutoSizeTextTypeWithDefaults(t, TextViewCompat.AUTO_SIZE_TEXT_TYPE_UNIFORM)
            TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(t, 14, 28, 1, android.util.TypedValue.COMPLEX_UNIT_SP)
            // Force text color to dark brown so it's visible in both modes (since pill is always cream)
            t.setTextColor(Color.parseColor("#3D2B1F"))
            t.typeface = androidx.core.content.res.ResourcesCompat.getFont(this, R.font.amiri_quran)
            t
        }

        // Set default animations
        tsZikrWheel.inAnimation = AnimationUtils.loadAnimation(this, android.R.anim.slide_in_left)
        tsZikrWheel.outAnimation = AnimationUtils.loadAnimation(this, android.R.anim.slide_out_right)
        
        tsZikrWheel.setText(zikrs[currentZikrIndex].text)
    }

    private fun setupListeners() {
        val goals = listOf(tvGoal10 to 10, tvGoal33 to 33, tvGoal100 to 100, tvGoal1000 to 1000)
        
        for ((tv, g) in goals) {
            tv.setOnClickListener {
                setGoalForCurrent(g, goals.map { it.first })
            }
        }

        gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDown(e: MotionEvent): Boolean {
                return true
            }

            override fun onFling(
                e1: MotionEvent?, e2: MotionEvent,
                velocityX: Float, velocityY: Float
            ): Boolean {
                if (e1 != null && abs(velocityX) > abs(velocityY)) {
                    if (velocityX > 0) {
                        // Swipe Right -> Previous
                        switchZikr(direction = -1)
                    } else {
                        // Swipe Left -> Next
                        switchZikr(direction = 1)
                    }
                    return true
                }
                return false
            }

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
        } else {
            beadsCircle.setOnTouchListener { _, event ->
                gestureDetector.onTouchEvent(event)
                true
            }
        }
        
        tvAddZikr.setOnClickListener {
            showAddZikrDialog()
        }
        
        tvResetCounter.setOnClickListener {
            val currentZikr = zikrs[currentZikrIndex]
            currentZikr.count = 0
            updateUI(animate = false)
            Toast.makeText(this, "تم تصفير العداد", Toast.LENGTH_SHORT).show()
        }
        
        val zikrGestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDown(e: MotionEvent): Boolean {
                return true
            }

            override fun onFling(
                e1: MotionEvent?, e2: MotionEvent,
                velocityX: Float, velocityY: Float
            ): Boolean {
                if (e1 != null && abs(velocityX) > abs(velocityY)) {
                    if (velocityX > 0) {
                        switchZikr(direction = -1)
                    } else {
                        switchZikr(direction = 1)
                    }
                    return true
                }
                return false
            }

            override fun onSingleTapUp(e: MotionEvent): Boolean {
                val width = llZikrContainer.width
                if (e.x > width / 2) {
                    switchZikr(direction = 1)
                } else {
                    switchZikr(direction = -1)
                }
                return true
            }
        })

        llZikrContainer.setOnTouchListener { _, event ->
            zikrGestureDetector.onTouchEvent(event)
            true
        }
    }

    private fun showAddZikrDialog() {
        val editText = EditText(this)
        editText.hint = "اكتب الذكر هنا"
        editText.gravity = android.view.Gravity.CENTER
        editText.textSize = 20f
        editText.typeface = androidx.core.content.res.ResourcesCompat.getFont(this, R.font.amiri_quran)
        
        AlertDialog.Builder(this)
            .setTitle("إضافة ذكر جديد")
            .setView(editText)
            .setPositiveButton("إضافة") { _, _ ->
                val newText = editText.text.toString().trim()
                if (newText.isNotEmpty()) {
                    zikrs.add(ZikrState(newText, 33, 33))
                    Toast.makeText(this, "تمت الإضافة بنجاح", Toast.LENGTH_SHORT).show()
                    switchZikrToIndex(zikrs.size - 1)
                }
            }
            .setNegativeButton("إلغاء", null)
            .show()
    }

    private fun switchZikr(direction: Int) {
        if (direction > 0) {
            // Next -> Slide in from Right, slide out Left
            tsZikrWheel.inAnimation = AnimationUtils.loadAnimation(this, R.anim.slide_in_right)
            tsZikrWheel.outAnimation = AnimationUtils.loadAnimation(this, R.anim.slide_out_left)
        } else {
            // Prev -> Slide in from Left, slide out Right
            tsZikrWheel.inAnimation = AnimationUtils.loadAnimation(this, android.R.anim.slide_in_left)
            tsZikrWheel.outAnimation = AnimationUtils.loadAnimation(this, android.R.anim.slide_out_right)
        }
        
        var newIndex = currentZikrIndex + direction
        if (newIndex < 0) newIndex = zikrs.size - 1
        if (newIndex >= zikrs.size) newIndex = 0
        
        switchZikrToIndex(newIndex)
    }
    
    private fun switchZikrToIndex(index: Int) {
        currentZikrIndex = index
        tsZikrWheel.setText(zikrs[currentZikrIndex].text)
        updateUI(animate = false)
        refreshGoalButtonsUI()
    }

    private fun setGoalForCurrent(newGoal: Int, allTvs: List<TextView>) {
        val currentZikr = zikrs[currentZikrIndex]
        currentZikr.goal = newGoal
        currentZikr.count = 0
        
        refreshGoalButtonsUI()
        updateUI(animate = false)
    }
    
    private fun refreshGoalButtonsUI() {
        val prefs = getSharedPreferences("app", MODE_PRIVATE)
        val savedBg = prefs.getString("bg_color", "#F4ECD8") ?: "#F4ECD8"
        val isDarkMode = savedBg == "#121212"
        val baseTextColor = Color.parseColor(prefs.getString("text_color", "#3D2B1F") ?: "#3D2B1F")
        val tasbeehTextColor = if (isDarkMode) Color.WHITE else baseTextColor
        
        val currentZikr = zikrs[currentZikrIndex]
        val goals = listOf(tvGoal10 to 10, tvGoal33 to 33, tvGoal100 to 100, tvGoal1000 to 1000)
        
        for ((tv, g) in goals) {
            if (g == currentZikr.goal) {
                tv.setBackgroundResource(R.drawable.bg_filled_circle)
                tv.setTextColor(baseTextColor)
                tv.animate().scaleX(1.15f).scaleY(1.15f).setDuration(200).start()
            } else {
                tv.setBackgroundResource(R.drawable.bg_hollow_circle)
                tv.setTextColor(tasbeehTextColor)
                tv.animate().scaleX(1.0f).scaleY(1.0f).setDuration(200).start()
            }
        }
    }

    private fun doCount() {
        if (isBottomBarVisible) toggleBottomBar()
        hideHandler.removeCallbacks(showRunnable)
        hideHandler.postDelayed(showRunnable, 3000)

        val currentZikr = zikrs[currentZikrIndex]
        if (currentZikr.count < currentZikr.goal) {
            currentZikr.count++
            val prefs = getSharedPreferences("app", MODE_PRIVATE)
            if (prefs.getBoolean("vibration_on", true)) playVibrateTick()
            if (prefs.getBoolean("sound_on", true)) playSoundTick()
            updateUI(animate = true)
            
            if (currentZikr.count == currentZikr.goal) {
                // Goal reached!
                if (prefs.getBoolean("vibration_on", true)) playVibrateGoalReached()
                if (prefs.getBoolean("sound_on", true)) playSoundGoalReached()
            }
        } else {
            // Already at goal, auto switch to next and start at 1
            currentZikr.count = 0
            switchZikr(direction = 1)
            
            val nextZikr = zikrs[currentZikrIndex]
            nextZikr.count = 1
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
        val currentZikr = zikrs[currentZikrIndex]
        tvMainCounter.text = currentZikr.count.toString()
        beadsCircle.setGoalAndCount(currentZikr.goal, currentZikr.count, animate)
    }

    private fun toggleBottomBar() {
        val bottomBarLayout = findViewById<View>(R.id.bottomBarLayout) ?: return
        isBottomBarVisible = !isBottomBarVisible
        bottomBarLayout.animate()
            .translationY(if (isBottomBarVisible) 0f else bottomBarLayout.height.toFloat() + 100f)
            .setDuration(300).start()
    }
}
