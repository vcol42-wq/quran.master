package com.sabah.bikhushue

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.ImageView
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.card.MaterialCardView

class ExperimentalHomeActivity : AppCompatActivity() {

    private var isBottomBarVisible = true
    private val floatingAnimators = mutableListOf<ObjectAnimator>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_experimental_home)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.expHomeRoot)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        BottomBarHelper.setupBottomBar(this, onThemeChanged = { applyTheme() })

        val scrollView = findViewById<ScrollView>(R.id.expScrollView)
        scrollView?.setOnScrollChangeListener { _, _, scrollY, _, oldScrollY ->
            val dy = scrollY - oldScrollY
            if (dy > 30 && isBottomBarVisible) toggleBottomBar()
            else if (dy < -30 && !isBottomBarVisible) toggleBottomBar()
        }

        // 1. Top Bar Button: "سبح بخشوع" scroll to bottom ("عند الضغط عليه تذهب الى اخر الصفحة")
        val scrollToBottomAction = View.OnClickListener {
            scrollView?.post {
                scrollView.fullScroll(ScrollView.FOCUS_DOWN)
            }
        }
        findViewById<View>(R.id.btnScrollToTopCard)?.setOnClickListener(scrollToBottomAction)
        findViewById<View>(R.id.btnScrollToTop)?.setOnClickListener(scrollToBottomAction)

        // Bottom Footer Button: scroll back to top
        val scrollToTopAction = View.OnClickListener {
            scrollView?.smoothScrollTo(0, 0)
        }
        findViewById<View>(R.id.cardFooterScrollToTop)?.setOnClickListener(scrollToTopAction)
        findViewById<View>(R.id.btnFooterScrollToTop)?.setOnClickListener(scrollToTopAction)

        // 2. Switch back to standard interface
        findViewById<View>(R.id.btnSwitchToStandardHome)?.setOnClickListener {
            getSharedPreferences("app", MODE_PRIVATE)
                .edit()
                .putBoolean("use_experimental_home", false)
                .apply()
            startActivity(Intent(this, HomeActivity::class.java))
            finish()
        }

        // Top Panel: اللوحة في الأعلى تتفاعل مع اللمس لتفتح هدف (ففروا إلى الله)
        val panelTopHeader = findViewById<MaterialCardView>(R.id.panelTopHeader)
        panelTopHeader?.setOnClickListener {
            animateBeadSlideAndOpen(panelTopHeader, directionUp = true) {
                val intent = Intent(this, AzkarActivity::class.java)
                intent.putExtra("category", "دعاء المحزون")
                startActivity(intent)
            }
        }

        // 3. Tasbeeh Bead click & slide handlers ("عند لمسها تتحرك الى الاعلى او الاسفل وتفتح النافذة")
        val cardAg = findViewById<View>(R.id.cardAg)
        val cardMushaf: MaterialCardView = findViewById(R.id.cardMushaf)
        val cardAzkar: MaterialCardView = findViewById(R.id.cardAzkar)
        val cardPrayer: MaterialCardView = findViewById(R.id.cardPrayer)
        val cardTasbeeh: MaterialCardView = findViewById(R.id.cardTasbeeh)
        val cardFafirru: MaterialCardView? = findViewById(R.id.cardFafirru)
        val cardSalawat: MaterialCardView = findViewById(R.id.cardSalawat)

        // Goal 1: الصورة ag (الضبط)
        cardAg?.setOnClickListener {
            animateBeadSlideAndOpen(cardAg, directionUp = true) {
                startActivity(Intent(this, SettingsActivity::class.java))
            }
        }

        // Goal 2: الصورة nnbb (القرآن الكريم)
        cardMushaf.setOnClickListener {
            animateBeadSlideAndOpen(cardMushaf, directionUp = true) {
                if (DatabaseDownloader.isQuranDbReady(this)) {
                    startActivity(Intent(this, MainActivity::class.java))
                } else {
                    Toast.makeText(this, "جاري تنزيل بيانات المصحف، يرجى الانتظار قليلاً...", Toast.LENGTH_SHORT).show()
                    if (!DatabaseDownloader.isDownloading) {
                        startDatabaseDownload()
                    }
                }
            }
        }

        cardAzkar.setOnClickListener {
            animateBeadSlideAndOpen(cardAzkar, directionUp = false) {
                startActivity(Intent(this, AzkarActivity::class.java))
            }
        }

        cardPrayer.setOnClickListener {
            animateBeadSlideAndOpen(cardPrayer, directionUp = true) {
                startActivity(Intent(this, AthanActivity::class.java))
            }
        }

        cardTasbeeh.setOnClickListener {
            animateBeadSlideAndOpen(cardTasbeeh, directionUp = false) {
                startActivity(Intent(this, TasbeehActivity::class.java))
            }
        }

        // Goal Fafirru: ففروا إلى الله (خلفية خشبية)
        cardFafirru?.setOnClickListener {
            animateBeadSlideAndOpen(cardFafirru, directionUp = true) {
                val intent = Intent(this, AzkarActivity::class.java)
                intent.putExtra("category", "دعاء المحزون")
                startActivity(intent)
            }
        }

        cardSalawat.setOnClickListener {
            animateBeadSlideAndOpen(cardSalawat, directionUp = true) {
                startActivity(Intent(this, SpiralTasbeehActivity::class.java))
            }
        }

        // Small beads click handler -> Goal "استغفر الله"
        // ("تدخل على هذا الهدف من خلال الضغط على الخرزات الصغيرة المكتوب عليها استغفر الله")
        val openIstighfarGoal = View.OnClickListener { beadView ->
            animateBeadSlideAndOpen(beadView, directionUp = true) {
                val intent = Intent(this, SpiralTasbeehActivity::class.java)
                intent.putExtra("mode", "istighfar")
                startActivity(intent)
            }
        }

        val smallBeadsToClick = listOfNotNull(
            findViewById<View>(R.id.topConnectedBead),
            findViewById<View>(R.id.topHeadBeadInner),
            findViewById<View>(R.id.smallBead1),
            findViewById<View>(R.id.smallBead2),
            findViewById<View>(R.id.smallBead3),
            findViewById<View>(R.id.smallBead4),
            findViewById<View>(R.id.smallBead5),
            findViewById<View>(R.id.smallBead6),
            findViewById<View>(R.id.freeDanglingBead)
        )
        smallBeadsToClick.forEach { bead ->
            bead.setOnClickListener(openIstighfarGoal)
        }

        // 4. Article Card (انقر لفتح صفحة المقال الكاملة)
        val cardFooterGhg = findViewById<View>(R.id.cardFooterGhg)
        cardFooterGhg?.setOnClickListener {
            startActivity(Intent(this, ArticleActivity::class.java))
        }

        // Setup subtle floating strand motion for beads along the wire
        setupFloatingAnimations()

        // Background initialization if needed
        startDatabaseDownload()

        setupDynamicHeader()
    }

    private fun showWhyNameArticleDialog() {
        performHapticClick()
        startActivity(Intent(this, ArticleActivity::class.java))
    }

    private fun setupFloatingAnimations() {
        val targets = listOf(
            Pair(findViewById<View>(R.id.unifiedGoal1), 2500L),
            Pair(findViewById<View>(R.id.unifiedGoal2), 2600L),
            Pair(findViewById<View>(R.id.unifiedGoal3), 3100L),
            Pair(findViewById<View>(R.id.unifiedGoal4), 2400L),
            Pair(findViewById<View>(R.id.unifiedGoal5), 2900L),
            Pair(findViewById<View>(R.id.unifiedGoalFafirru), 2800L),
            Pair(findViewById<View>(R.id.unifiedGoal6), 2700L)
        )

        targets.forEachIndexed { index, (unifiedPiece, duration) ->
            unifiedPiece?.let { piece ->
                val floatDistance = if (index % 2 == 0) -12f else -18f

                // 1. Translation Y float for the unified piece (حركة الصورة وإطار الخرز والظل تحتهم كقطعة واحدة)
                val yAnim = ObjectAnimator.ofFloat(piece, "translationY", 0f, floatDistance).apply {
                    this.duration = duration
                    repeatCount = ValueAnimator.INFINITE
                    repeatMode = ValueAnimator.REVERSE
                    interpolator = AccelerateDecelerateInterpolator()
                    startDelay = index * 150L
                }
                yAnim.start()
                floatingAnimators.add(yAnim)
            }
        }

        // Top Header Panel floating shadow motion (حركة ظل اللوحة العلوية المتناغمة)
        val panelTopHeader = findViewById<View>(R.id.panelTopHeader)
        val panelTopHeaderShadow = findViewById<View>(R.id.panelTopHeaderShadow)
        panelTopHeader?.let { panel ->
            val panelYAnim = ObjectAnimator.ofFloat(panel, "translationY", 0f, -8f).apply {
                duration = 3000L
                repeatCount = ValueAnimator.INFINITE
                repeatMode = ValueAnimator.REVERSE
                interpolator = AccelerateDecelerateInterpolator()
            }
            panelYAnim.start()
            floatingAnimators.add(panelYAnim)

            panelTopHeaderShadow?.let { shadow ->
                val shadowYAnim = ObjectAnimator.ofFloat(shadow, "translationY", 0f, -4f).apply {
                    duration = 3000L
                    repeatCount = ValueAnimator.INFINITE
                    repeatMode = ValueAnimator.REVERSE
                    interpolator = AccelerateDecelerateInterpolator()
                }
                shadowYAnim.start()
                floatingAnimators.add(shadowYAnim)
            }
        }

        // 1. Top connected head bead gentle dangling animation (خرزة علوية متصلة توحي بالتعليق)
        val topBead = findViewById<View>(R.id.topConnectedBead)
        topBead?.let {
            val topAnim = ObjectAnimator.ofFloat(it, "translationY", 0f, -8f).apply {
                duration = 2300L
                repeatCount = ValueAnimator.INFINITE
                repeatMode = ValueAnimator.REVERSE
                interpolator = AccelerateDecelerateInterpolator()
            }
            topAnim.start()
            floatingAnimators.add(topAnim)
        }

        // 2. Bottom free dangling bead pendulum sway (خرزة حرّة تتدلى يميناً ويساراً)
        val bottomDanglingContainer = findViewById<View>(R.id.bottomDanglingBeadContainer)
        bottomDanglingContainer?.let { container ->
            container.post {
                container.pivotX = container.width / 2f
                container.pivotY = 0f
            }
            val swayAnim = ObjectAnimator.ofFloat(container, "rotation", -12f, 12f).apply {
                duration = 3200L
                repeatCount = ValueAnimator.INFINITE
                repeatMode = ValueAnimator.REVERSE
                interpolator = AccelerateDecelerateInterpolator()
            }
            swayAnim.start()
            floatingAnimators.add(swayAnim)
        }

        // 3. Fluttering ribbons wave animation (أشرطة ترفرف بالهواء)
        val ribbonLeft = findViewById<View>(R.id.ribbonLeft)
        val ribbonRight = findViewById<View>(R.id.ribbonRight)
        val ribbonCenter = findViewById<View>(R.id.ribbonCenter)

        ribbonLeft?.let {
            val leftRibbonAnim = ObjectAnimator.ofFloat(it, "rotation", -6f, 10f).apply {
                duration = 1700L
                repeatCount = ValueAnimator.INFINITE
                repeatMode = ValueAnimator.REVERSE
                interpolator = AccelerateDecelerateInterpolator()
            }
            leftRibbonAnim.start()
            floatingAnimators.add(leftRibbonAnim)
        }

        ribbonRight?.let {
            val rightRibbonAnim = ObjectAnimator.ofFloat(it, "rotation", 8f, -8f).apply {
                duration = 1900L
                repeatCount = ValueAnimator.INFINITE
                repeatMode = ValueAnimator.REVERSE
                interpolator = AccelerateDecelerateInterpolator()
            }
            rightRibbonAnim.start()
            floatingAnimators.add(rightRibbonAnim)
        }

        ribbonCenter?.let {
            val centerRibbonAnim = ObjectAnimator.ofFloat(it, "scaleY", 0.95f, 1.06f).apply {
                duration = 1400L
                repeatCount = ValueAnimator.INFINITE
                repeatMode = ValueAnimator.REVERSE
                interpolator = AccelerateDecelerateInterpolator()
            }
            centerRibbonAnim.start()
            floatingAnimators.add(centerRibbonAnim)
        }
    }

    private fun startDatabaseDownload() {
        DatabaseHelper(this).checkAndCopyDatabase()
        AzkarDatabaseHelper(this)
    }

    private fun setupDynamicHeader() {
        // Subtitle removed per user request
    }

    override fun onResume() {
        super.onResume()
        setupDynamicHeader()
        applyTheme()
    }

    override fun onDestroy() {
        super.onDestroy()
        floatingAnimators.forEach { it.cancel() }
        floatingAnimators.clear()
    }

    private fun applyTheme() {
        val theme = ThemeHelper.getThemeColors(this)
        ThemeHelper.applySystemWindowsColors(this)
        val bgColor = theme.bg

        val root: View = findViewById(R.id.expHomeRoot)
        root.setBackgroundColor(bgColor)

        val windowInsetsController = WindowInsetsControllerCompat(window, window.decorView)
        windowInsetsController.isAppearanceLightStatusBars = !theme.isDark
        windowInsetsController.isAppearanceLightNavigationBars = !theme.isDark

        val cardBg = theme.cardBg
        val strokeColor = theme.stroke
        val txtColor = theme.txt

        // 1. Header pill cards
        val btnScrollToTopCard = findViewById<MaterialCardView>(R.id.btnScrollToTopCard)
        btnScrollToTopCard?.setCardBackgroundColor(cardBg)
        btnScrollToTopCard?.strokeColor = strokeColor

        val tvHeaderTitle = findViewById<TextView>(R.id.tvHeaderTitle)
        tvHeaderTitle?.setTextColor(txtColor)

        val ivHeaderArrow = findViewById<ImageView>(R.id.ivHeaderArrow)
        ivHeaderArrow?.setColorFilter(txtColor)

        val btnSwitchCard = findViewById<MaterialCardView>(R.id.btnSwitchToStandardHome)
        btnSwitchCard?.setCardBackgroundColor(cardBg)
        btnSwitchCard?.strokeColor = strokeColor

        val tvSwitchText = findViewById<TextView>(R.id.tvSwitchText)
        tvSwitchText?.setTextColor(txtColor)

        // 2. 3D Top Header Image Card & 3D Shadow Layer Ground
        val panelTopHeaderShadow = findViewById<MaterialCardView>(R.id.panelTopHeaderShadow)
        panelTopHeaderShadow?.setCardBackgroundColor(theme.shadow)

        val panelTopHeader = findViewById<MaterialCardView>(R.id.panelTopHeader)
        panelTopHeader?.setCardBackgroundColor(cardBg)
        panelTopHeader?.strokeColor = strokeColor

        // 3. Main Outer Beaded Wire (حبل التعليق الذهبي والخرز بدون حجب خلفية الأرضية)
        val mainBeadedFrameLayout = findViewById<BeadedFrameLayout>(R.id.mainBeadedFrameLayout)
        mainBeadedFrameLayout?.setColors(
            wireColor = Color.parseColor("#D4AF37"),
            beadColor = strokeColor
        )
        mainBeadedFrameLayout?.background = null

        // 4. Bead Cards Grounds and Circular Bead Frame Colors (أرضيات وألوان خرز الأهداف الدائرية)
        val beadCardIds = listOf(
            R.id.cardAg,
            R.id.cardMushaf,
            R.id.cardAzkar,
            R.id.cardPrayer,
            R.id.cardTasbeeh,
            R.id.cardSalawat
        )
        beadCardIds.forEach { id ->
            val card = findViewById<MaterialCardView>(id)
            card?.setCardBackgroundColor(cardBg)
            card?.strokeColor = strokeColor
        }

        val beadFrameIds = listOf(
            R.id.beadFrameGoal1,
            R.id.beadFrameGoal2,
            R.id.beadFrameGoal3,
            R.id.beadFrameGoal4,
            R.id.beadFrameGoal5,
            R.id.beadFrameGoalFafirru,
            R.id.beadFrameGoal6
        )
        beadFrameIds.forEach { id ->
            val frame = findViewById<BeadedFrameLayout>(id)
            frame?.setColors(
                wireColor = Color.parseColor("#D4AF37"),
                beadColor = strokeColor
            )
        }

        // 5. Connected Top Bead, Free Dangling Bead, and Small Separator Beads Ground (أرضيات الخرزات الصغيرة)
        findViewById<View>(R.id.topHeadBeadInner)?.background = createThemedBeadDrawable(theme)
        findViewById<View>(R.id.freeDanglingBead)?.background = createThemedBeadDrawable(theme)

        val smallBeadIds = listOf(
            R.id.smallBead1,
            R.id.smallBead2,
            R.id.smallBead3,
            R.id.smallBead4,
            R.id.smallBead5,
            R.id.smallBead6
        )
        smallBeadIds.forEach { id ->
            findViewById<View>(id)?.background = createThemedBeadDrawable(theme)
        }

        // 6. Bottom Article Card Ground
        val cardFooterGhg = findViewById<MaterialCardView>(R.id.cardFooterGhg)
        cardFooterGhg?.setCardBackgroundColor(cardBg)
        cardFooterGhg?.strokeColor = strokeColor

        // 7. Bottom Scroll To Top Card Ground
        val cardFooterScrollToTop = findViewById<MaterialCardView>(R.id.cardFooterScrollToTop)
        cardFooterScrollToTop?.setCardBackgroundColor(cardBg)
        cardFooterScrollToTop?.strokeColor = strokeColor

        val tvFooterTitle = findViewById<TextView>(R.id.tvFooterTitle)
        tvFooterTitle?.setTextColor(txtColor)

        val tvFooterSubtitle = findViewById<TextView>(R.id.tvFooterSubtitle)
        if (theme.isDark) {
            tvFooterSubtitle?.setTextColor(Color.parseColor("#B0BEC5"))
        } else {
            tvFooterSubtitle?.setTextColor(Color.parseColor("#8B7355"))
        }

        BottomBarHelper.setupBottomBar(this, onThemeChanged = { applyTheme() })
    }

    private fun createThemedBeadDrawable(theme: ThemeColors): android.graphics.drawable.Drawable {
        val drawable = android.graphics.drawable.GradientDrawable()
        drawable.shape = android.graphics.drawable.GradientDrawable.OVAL
        if (theme.isDark) {
            drawable.orientation = android.graphics.drawable.GradientDrawable.Orientation.TL_BR
            drawable.colors = intArrayOf(Color.parseColor("#1F4230"), Color.parseColor("#132A1F"), Color.parseColor("#0C1B14"))
        } else {
            drawable.orientation = android.graphics.drawable.GradientDrawable.Orientation.TL_BR
            drawable.colors = intArrayOf(Color.parseColor("#2E5B42"), Color.parseColor("#1B3B2B"), Color.parseColor("#122A1E"))
        }
        drawable.setStroke((2.5f * resources.displayMetrics.density).toInt(), Color.parseColor("#FFD700"))
        return drawable
    }

    private fun toggleBottomBar() {
        val bottomBarLayout = findViewById<View>(R.id.bottomBarLayout) ?: return
        isBottomBarVisible = !isBottomBarVisible
        bottomBarLayout.animate()
            .translationY(if (isBottomBarVisible) 0f else bottomBarLayout.height.toFloat() + 100f)
            .setDuration(300).start()
    }

    /**
     * Bead Touch & Slide Animation along the wire before opening target window
     * ("عند لمسها تتحرك الى الاعلى او الاسفل وتفتح النافذة مع حركة الظلال")
     */
    private fun animateBeadSlideAndOpen(view: View, directionUp: Boolean, onEnd: () -> Unit) {
        performHapticClick()

        val shiftDistance = if (directionUp) -35f else 35f
        val animatedTarget = (view.parent as? View) ?: view

        // Dynamic 3D elevation shadow lift on touch
        view.animate()
            .translationZ(16f)
            .setDuration(130)
            .start()

        animatedTarget.animate()
            .translationYBy(shiftDistance)
            .scaleX(1.08f)
            .scaleY(1.08f)
            .setDuration(130)
            .setInterpolator(OvershootInterpolator())
            .withEndAction {
                view.animate()
                    .translationZ(0f)
                    .setDuration(110)
                    .start()

                animatedTarget.animate()
                    .translationYBy(-shiftDistance)
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .setDuration(110)
                    .withEndAction { onEnd() }
                    .start()
            }.start()
    }

    private fun performHapticClick() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
            } else {
                val v = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    v.vibrate(VibrationEffect.createOneShot(25, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    v.vibrate(25)
                }
            }
        } catch (e: Exception) {
            // Fallback if vibration permission or hardware unavailable
        }
    }
}
