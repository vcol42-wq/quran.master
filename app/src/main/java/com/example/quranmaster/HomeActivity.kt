package com.example.quranmaster

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

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
        val prefs = getSharedPreferences("app", MODE_PRIVATE)
        val savedBg = prefs.getString("bg_color", "#F4ECD8") ?: "#F4ECD8"
        val savedTxt = prefs.getString("txt_color", "#000000") ?: "#000000"
        val savedBar = prefs.getString("bar_color", "#E6DCC8") ?: "#E6DCC8"

        val bgColor = Color.parseColor(savedBg)
        val txtColor = Color.parseColor(savedTxt)
        val barColor = Color.parseColor(savedBar)
        val cardBgColor = if (savedBg == "#121212") Color.parseColor("#2D2D2D") else Color.WHITE

        val root: View = findViewById(android.R.id.content)
        root.setBackgroundColor(bgColor)

        window.statusBarColor = barColor
        window.navigationBarColor = barColor
        if (savedBg == "#121212") {
            window.decorView.systemUiVisibility = window.decorView.systemUiVisibility and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
        } else {
            window.decorView.systemUiVisibility = window.decorView.systemUiVisibility or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        }

        findViewById<MaterialCardView>(R.id.cardSettings)?.apply {
            setCardBackgroundColor(barColor)
            strokeColor = barColor
        }
        
        findViewById<MaterialCardView>(R.id.cardAbout)?.apply {
            setCardBackgroundColor(barColor)
            strokeColor = barColor
        }

        val gridCards = listOf(R.id.cardQuran, R.id.cardAzkar, R.id.cardTasbeeh, R.id.cardPrayer)
        gridCards.forEach { id ->
            findViewById<MaterialCardView>(id)?.apply {
                setCardBackgroundColor(cardBgColor)
                strokeColor = barColor
            }
        }

        fun updateTextViews(view: View) {
            if (view is TextView) {
                val text = view.text.toString()
                if (text.any { it.isLetter() }) {
                    view.setTextColor(txtColor)
                }
            } else if (view is ViewGroup) {
                for (i in 0 until view.childCount) {
                    updateTextViews(view.getChildAt(i))
                }
            }
        }
        updateTextViews(root)
    }
}
