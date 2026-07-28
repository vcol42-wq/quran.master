package com.sabah.bikhushue

import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

class ArticleActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_article)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.articleRoot)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        findViewById<View>(R.id.btnBackArticle)?.setOnClickListener {
            finish()
        }

        applyTheme()
    }

    private fun applyTheme() {
        val theme = ThemeHelper.getThemeColors(this)
        ThemeHelper.applySystemWindowsColors(this)

        val root: View? = findViewById(R.id.articleRoot)
        root?.setBackgroundColor(theme.bg)

        val header: View? = findViewById(R.id.articleHeader)
        header?.setBackgroundColor(theme.bar)

        val windowInsetsController = WindowInsetsControllerCompat(window, window.decorView)
        windowInsetsController.isAppearanceLightStatusBars = !theme.isDark
        windowInsetsController.isAppearanceLightNavigationBars = !theme.isDark
    }
}
