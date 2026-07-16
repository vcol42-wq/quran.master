package com.sabah.bikhushue

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.card.MaterialCardView

class HomeActivity : AppCompatActivity() {

    private var isBottomBarVisible = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

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

        cardSettings.setOnClickListener {
            SettingsHelper.showSettingsDialog(this, onThemeChanged = {
                applyTheme()
            })
        }

        cardQuran.setOnClickListener {
            if (DatabaseDownloader.isQuranDbReady(this)) {
                startActivity(Intent(this, MainActivity::class.java))
            } else {
                Toast.makeText(this, "جاري تنزيل بيانات المصحف، يرجى الانتظار قليلاً...", Toast.LENGTH_SHORT).show()
                // Restart download if it somehow failed
                if (!DatabaseDownloader.isDownloading) {
                    startDatabaseDownload()
                }
            }
        }

        cardAzkar.setOnClickListener {
            startActivity(Intent(this, AzkarActivity::class.java))
        }

        cardTasbeeh.setOnClickListener {
            startActivity(Intent(this, TasbeehActivity::class.java))
        }

        cardPrayer.setOnClickListener {
            startActivity(Intent(this, AthanActivity::class.java))
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
    }
    
    private fun startDatabaseDownload() {
        // Initialize databases locally from assets
        DatabaseHelper(this).checkAndCopyDatabase()
        AzkarDatabaseHelper(this) // The init block handles copyExternalDatabaseIfNeeded
    }

    override fun onResume() {
        super.onResume()
        applyTheme()
    }

    private fun applyTheme() {
        val theme = ThemeHelper.getThemeColors(this)
        val bgColor = theme.bg
        val txtColor = theme.txt
        val barColor = theme.bar
        val cardBgColor = theme.cardBg

        val root: View = findViewById(R.id.homeRoot)
        root.setBackgroundColor(bgColor)

        window.statusBarColor = barColor
        window.navigationBarColor = barColor
        if (theme.isDark) {
            window.decorView.systemUiVisibility = window.decorView.systemUiVisibility and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
        } else {
            window.decorView.systemUiVisibility = window.decorView.systemUiVisibility or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        }

        findViewById<MaterialCardView>(R.id.cardSettings)?.apply {
            setCardBackgroundColor(barColor)
            strokeColor = barColor
        }


        val gridCards = listOf(R.id.cardQuran, R.id.cardAzkar, R.id.cardTasbeeh, R.id.cardPrayer, R.id.cardDaily)
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
    }

    private fun toggleBottomBar() {
        val bottomBarLayout = findViewById<View>(R.id.bottomBarLayout) ?: return
        isBottomBarVisible = !isBottomBarVisible
        bottomBarLayout.animate()
            .translationY(if (isBottomBarVisible) 0f else bottomBarLayout.height.toFloat() + 100f)
            .setDuration(300).start()
    }

    private fun setupDailyCard() {
        val messages = listOf(
            "اللهم إني أسألك علماً نافعاً، ورزقاً طيباً، وعملاً متقبلاً.",
            "وَمَن يَتَوَكَّلْ عَلَى اللَّهِ فَهُوَ حَسْبُهُ",
            "اللهم أنت ربي لا إله إلا أنت، خلقتني وأنا عبدك، وأنا على عهدك ووعدك ما استطعت.",
            "رَبِّ اشْرَحْ لِي صَدْرِي * وَيَسِّرْ لِي أَمْرِي",
            "سبحان الله وبحمده، عدد خلقه، ورضا نفسه، وزنة عرشه، ومداد كلماته.",
            "إِنَّ مَعَ الْعُسْرِ يُسْرًا",
            "اللهم أعني على ذكرك وشكرك وحسن عبادتك.",
            "وَرَحْمَتِي وَسِعَتْ كُلَّ شَيْءٍ",
            "لا حول ولا قوة إلا بالله، كنز من كنوز الجنة.",
            "فَإِنِّي قَرِيبٌ أُجِيبُ دَعْوَةَ الدَّاعِ إِذَا دَعَانِ",
            "اللهم صل وسلم وبارك على نبينا محمد."
        )
        
        val calendar = java.util.Calendar.getInstance()
        val dayOfYear = calendar.get(java.util.Calendar.DAY_OF_YEAR)
        
        val tvDailyContent = findViewById<TextView>(R.id.tvDailyContent)
        tvDailyContent?.text = messages[dayOfYear % messages.size]
    }
}
