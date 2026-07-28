package com.sabah.bikhushue

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.google.android.material.card.MaterialCardView

class HomeActivity : AppCompatActivity() {

    private var isBottomBarVisible = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_home)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.homeRoot)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        BottomBarHelper.setupBottomBar(this, onThemeChanged = { applyTheme() })

        val scrollView = findViewById<android.widget.ScrollView>(R.id.homeScrollView)
        scrollView?.setOnScrollChangeListener { _, _, scrollY, _, oldScrollY ->
            val dy = scrollY - oldScrollY
            if (dy > 30 && isBottomBarVisible) toggleBottomBar()
            else if (dy < -30 && !isBottomBarVisible) toggleBottomBar()
        }

        val cardSettings: MaterialCardView = findViewById(R.id.cardSettings)
        val cardQuran: MaterialCardView = findViewById(R.id.cardQuran)
        val cardAzkar: MaterialCardView = findViewById(R.id.cardAzkar)
        val cardTasbeeh: MaterialCardView = findViewById(R.id.cardTasbeeh)
        val cardPrayer: MaterialCardView = findViewById(R.id.cardPrayer)

        findViewById<View>(R.id.btnSwitchToExpHome)?.setOnClickListener {
            getSharedPreferences("app", MODE_PRIVATE)
                .edit()
                .putBoolean("use_experimental_home", true)
                .apply()
            startActivity(Intent(this, ExperimentalHomeActivity::class.java))
            finish()
        }

        cardSettings.setOnClickListener {
            animateCardClick(cardSettings) {
                startActivity(Intent(this, ArticleActivity::class.java))
            }
        }

        cardQuran.setOnClickListener {
            animateCardClick(cardQuran) {
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
            animateCardClick(cardAzkar) {
                startActivity(Intent(this, AzkarActivity::class.java))
            }
        }

        cardTasbeeh.setOnClickListener {
            animateCardClick(cardTasbeeh) {
                startActivity(Intent(this, TasbeehActivity::class.java))
            }
        }

        cardPrayer.setOnClickListener {
            animateCardClick(cardPrayer) {
                startActivity(Intent(this, AthanActivity::class.java))
            }
        }
        
        // Start background download for databases if not ready
        startDatabaseDownload()

        val prefs = getSharedPreferences("app", MODE_PRIVATE)
        if (prefs.getBoolean("is_first_time_app", true)) {
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("مرحباً بك!")
                .setMessage("هذا التطبيق يساعدك على قراءة القرآن الكريم والمداومة على الأذكار بطريقة سهلة وميسرة.\n\n" +
                        "نرجو لك الفائدة والأجر العظيم.")
                .setPositiveButton("فهمت") { dialog, _ -> dialog.dismiss() }
                .setCancelable(false)
                .show()
            prefs.edit().putBoolean("is_first_time_app", false).apply()
        }

        setupDailyCard()
        setupDynamicHeader()
    }
    
    private fun startDatabaseDownload() {
        // Initialize databases locally from assets
        DatabaseHelper(this).checkAndCopyDatabase()
        AzkarDatabaseHelper(this) // The init block handles copyExternalDatabaseIfNeeded
    }

    override fun onResume() {
        super.onResume()
        setupDynamicHeader()
        applyTheme()
    }

    private fun applyTheme() {
        val theme = ThemeHelper.getThemeColors(this)
        ThemeHelper.applySystemWindowsColors(this)
        val bgColor = theme.bg
        val txtColor = theme.txt
        val barColor = theme.bar
        val cardBgColor = theme.cardBg

        val root: View = findViewById(R.id.homeRoot)
        root.setBackgroundColor(bgColor)

                        val windowInsetsController = WindowInsetsControllerCompat(window, window.decorView)
        windowInsetsController.isAppearanceLightStatusBars = !theme.isDark
        windowInsetsController.isAppearanceLightNavigationBars = !theme.isDark

        findViewById<MaterialCardView>(R.id.cardSettings)?.apply {
            setCardBackgroundColor(barColor)
            strokeColor = barColor
        }


        val gridCards = listOf(R.id.cardQuran, R.id.cardAzkar, R.id.cardTasbeeh, R.id.cardPrayer, R.id.cardDynamicHeader, R.id.cardSalawat)
        gridCards.forEach { id ->
            findViewById<MaterialCardView>(id)?.apply {
                setCardBackgroundColor(cardBgColor)
                strokeColor = barColor
            }
        }

        fun updateViews(view: View) {
            if (view is TextView) {
                val text = view.text.toString()
                if (text.any { it.isLetter() }) {
                    view.setTextColor(txtColor)
                }
            } else if (view is MaterialCardView) {
                if (view.childCount == 0 && view.id == View.NO_ID) {
                    view.setCardBackgroundColor(theme.shadow)
                }
            }
            if (view is ViewGroup) {
                for (i in 0 until view.childCount) {
                    updateViews(view.getChildAt(i))
                }
            }
        }
        updateViews(root)

        BottomBarHelper.setupBottomBar(this, onThemeChanged = { applyTheme() })
    }

    private fun toggleBottomBar() {
        val bottomBarLayout = findViewById<View>(R.id.bottomBarLayout) ?: return
        isBottomBarVisible = !isBottomBarVisible
        bottomBarLayout.animate()
            .translationY(if (isBottomBarVisible) 0f else bottomBarLayout.height.toFloat() + 100f)
            .setDuration(300).start()
    }

    private fun setupDynamicHeader() {
        val cardDynamicHeader = findViewById<MaterialCardView>(R.id.cardDynamicHeader)
        val tvSub = findViewById<TextView>(R.id.tvDynamicHeaderSubtitle)
        val prefs = getSharedPreferences("app", MODE_PRIVATE)
        val lastPage = prefs.getInt("last_page", 0)
        
        if (lastPage > 0) {
            tvSub?.text = "وردك القرآني • صفحة ${lastPage + 1} (انقر للمتابعة)"
        } else {
            tvSub?.text = "سورة البقرة • صفحة 1 (انقر للمتابعة)"
        }

        cardDynamicHeader?.setOnClickListener {
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

    private fun setupDailyCard() {
        val cardSalawat = findViewById<MaterialCardView>(R.id.cardSalawat)
        cardSalawat?.setOnClickListener {
            val intent = Intent(this, SpiralTasbeehActivity::class.java)
            startActivity(intent)
        }
    }

    private fun showWhyKhushueDialog() {
        val whyText = "التسبيح ليس مجرد كلمات باردة يُرددها اللسان، بل هو حركة الروح وسرعتها الفائقة في الهروب نحو الله، والانعتاق من قيود الأرض والجسد. فكما يشتق التسبيح لغوياً من \"السَّبْح\" وهو الجري السريع والسباحة، فهو في حقيقته سباق وجداني تتسارع فيه نبضات القلب وأفعال الجوارح لتقديس الخالق ونفي النقص عنه.\n\n" +
                "هذا التدفق الروحي المتسارع يتجسد في أسمى العبادات:\n\n" +
                "۞ تلاوة القرآن:\nجريان الآيات الشريفة على اللسان، وتغلغلها السريع في الوجدان كالسيل المُنهمر الذي يغسل قلب المؤمن ويقذفه في أنوار التنزيه.\n\n" +
                "۞ إقامة الصلاة:\nحركة البدن السريعة بالخضوع، وهبوط الجسد مستسلماً في ركوع وسجود يبث في الروح طاقة حركية تقربها من عرش الرحمن وتنزّهه بفعل الجوارح.\n\n" +
                "۞ ترديد الأذكار:\nانطلاق اللسان بلهفة وسرعة بكلمات التعظيم (سبحان الله) التي تطير بها الروح شوقاً وحباً كالسهم المصوّب نحو رضا الله جل وعلا."

        val theme = ThemeHelper.getThemeColors(this)
        val isDark = theme.isDark

        val tv = TextView(this).apply {
            text = whyText
            textSize = 15f
            setPadding(48, 36, 48, 36)
            setLineSpacing(0f, 1.3f)
            setTextColor(if (isDark) Color.parseColor("#E0E0E0") else Color.parseColor("#3D2B1F"))
            setTextIsSelectable(true)
            try {
                typeface = androidx.core.content.res.ResourcesCompat.getFont(this@HomeActivity, R.font.amiri_quran)
            } catch (e: Exception) {}
        }

        val scrollView = android.widget.ScrollView(this).apply {
            addView(tv)
        }

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("لماذا سبح بخشوع؟")
            .setView(scrollView)
            .setPositiveButton("حسناً", null)
            .show()
    }

    private fun animateCardClick(view: View, onEnd: () -> Unit) {
        view.animate()
            .scaleX(0.95f)
            .scaleY(0.95f)
            .setDuration(90)
            .withEndAction {
                view.animate()
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .setDuration(90)
                    .withEndAction { onEnd() }
                    .start()
            }.start()
    }
}
