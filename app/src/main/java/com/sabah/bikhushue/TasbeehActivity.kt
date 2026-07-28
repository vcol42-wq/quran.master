package com.sabah.bikhushue

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
import androidx.activity.enableEdgeToEdge
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
    
    private lateinit var tvPrevZikr: ImageView
    private lateinit var tvNextZikr: ImageView
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
        ZikrState("أستغفر الله", 0, 100)
    )
    private var currentZikrIndex = 0

    private lateinit var audioManager: AudioManager
    private lateinit var vibrator: Vibrator
    private lateinit var gestureDetector: GestureDetector

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_tasbeeh)

        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.tasbeehRoot)) { v, insets ->
            val systemBars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

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

        setupZikrWheel()
        applyTheme()
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
        val theme = ThemeHelper.getThemeColors(this)
        ThemeHelper.applySystemWindowsColors(this)
        
        beadsCircle.setNightOrLunar(theme.isDark)

        tasbeehRoot.setBackgroundColor(theme.bg)
        
        val tasbeehMainCard = findViewById<com.google.android.material.card.MaterialCardView>(R.id.tasbeehMainCard)
        val cardZikrInner = findViewById<com.google.android.material.card.MaterialCardView>(R.id.cardZikrInner)
        val tasbeehMainShadow = findViewById<com.google.android.material.card.MaterialCardView>(R.id.tasbeehMainShadow)
        val cardZikrShadow = findViewById<com.google.android.material.card.MaterialCardView>(R.id.cardZikrShadow)

        val cardColor = theme.cardBg
        val shadowColor = theme.shadow
        
        tasbeehMainCard?.setCardBackgroundColor(cardColor)
        cardZikrInner?.setCardBackgroundColor(cardColor)
        tasbeehMainShadow?.setCardBackgroundColor(shadowColor)
        cardZikrShadow?.setCardBackgroundColor(shadowColor)
        tasbeehMainCard?.strokeColor = shadowColor
        
        val tasbeehTextColor = theme.txt
        
        tvMainCounter.setTextColor(tasbeehTextColor)
        tvGoal10.setTextColor(tasbeehTextColor)
        tvGoal33.setTextColor(tasbeehTextColor)
        tvGoal100.setTextColor(tasbeehTextColor)
        tvGoal1000.setTextColor(tasbeehTextColor)
        
        val circleCenterColor = theme.cardBg
        beadsCircle.setCenterColor(circleCenterColor)


        updateTextColors(tasbeehRoot as android.view.ViewGroup, tasbeehTextColor)
        tvResetCounter.setColorFilter(tasbeehTextColor)
        tvAddZikr.setColorFilter(tasbeehTextColor)
    }

    private fun updateTextColors(viewGroup: android.view.ViewGroup, txtColor: Int) {
        for (i in 0 until viewGroup.childCount) {
            val child = viewGroup.getChildAt(i)
            if (child is android.widget.TextView) {
                child.setTextColor(txtColor)
            } else if (child is android.view.ViewGroup) {
                updateTextColors(child, txtColor)
            }
        }
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
            TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(t, 16, 50, 1, android.util.TypedValue.COMPLEX_UNIT_SP)
            t.maxLines = 2
            val theme = ThemeHelper.getThemeColors(this@TasbeehActivity)
            t.setTextColor(theme.txt)
            t.setShadowLayer(2f, 1f, 1f, if (theme.isDark) Color.TRANSPARENT else Color.parseColor("#40D2B48C"))
            val customFont = androidx.core.content.res.ResourcesCompat.getFont(this, R.font.amiri_quran)
            t.setTypeface(customFont, android.graphics.Typeface.BOLD_ITALIC)
            t.translationY = -8f * resources.displayMetrics.density // Shift up by 8dp
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
        
        tvPrevZikr.setOnClickListener { switchZikr(direction = -1) }
        tvNextZikr.setOnClickListener { switchZikr(direction = 1) }

        gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDown(e: MotionEvent): Boolean {
                return true
            }

            override fun onFling(
                e1: MotionEvent?, e2: MotionEvent,
                velocityX: Float, velocityY: Float
            ): Boolean {
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
        val oldZikr = zikrs[currentZikrIndex]
        currentZikrIndex = index
        val newZikr = zikrs[currentZikrIndex]
        
        // Preserve the global count and goal across zikrs
        newZikr.count = oldZikr.count
        newZikr.goal = oldZikr.goal
        
        tsZikrWheel.setText(newZikr.text)
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
        val theme = ThemeHelper.getThemeColors(this)
        ThemeHelper.applySystemWindowsColors(this)
        val tasbeehTextColor = theme.txt
        
        val currentGoal = zikrs[currentZikrIndex].goal
        val goals = listOf(tvGoal10 to 10, tvGoal33 to 33, tvGoal100 to 100, tvGoal1000 to 1000)
        
        for ((tv, g) in goals) {
            if (g == currentGoal) {
                tv.alpha = 1.0f
                tv.setTextColor(Color.parseColor("#7E2954")) // Bluish crimson to stand out
                tv.setTypeface(null, android.graphics.Typeface.BOLD)
                tv.animate().scaleX(1.2f).scaleY(1.2f).setDuration(200).start()
            } else {
                tv.alpha = 0.7f
                tv.setTextColor(tasbeehTextColor)
                tv.setTypeface(null, android.graphics.Typeface.NORMAL)
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
        tvMainCounter.text = (currentZikr.goal - currentZikr.count).toString()
        beadsCircle.setGoalAndCount(currentZikr.goal, currentZikr.count, animate)
        
        if (currentZikr.count == 0) {
            tvMainCounter.setTextColor(android.graphics.Color.parseColor("#4CAF50"))
        } else {
            tvMainCounter.setTextColor(android.graphics.Color.parseColor("#B0BEC5"))
        }
    }

    private fun toggleBottomBar() {
        val bottomBarLayout = findViewById<View>(R.id.bottomBarLayout) ?: return
        isBottomBarVisible = !isBottomBarVisible
        bottomBarLayout.animate()
            .translationY(if (isBottomBarVisible) 0f else bottomBarLayout.height.toFloat() + 100f)
            .setDuration(300).start()
    }
}
