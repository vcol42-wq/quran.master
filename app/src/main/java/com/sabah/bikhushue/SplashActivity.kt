package com.sabah.bikhushue

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        applyTheme()

        val splashLogo: ImageView = findViewById(R.id.splashLogo)
        val splashTitle: TextView = findViewById(R.id.splashTitle)

        // Initial state
        splashLogo.alpha = 0f
        splashLogo.scaleX = 0.5f
        splashLogo.scaleY = 0.5f

        splashTitle.alpha = 0f
        splashTitle.translationY = 50f

        // Animations
        splashLogo.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(1000)
            .start()

        splashTitle.animate()
            .alpha(1f)
            .translationY(0f)
            .setStartDelay(500)
            .setDuration(800)
            .start()

        // Transition to HomeActivity after 2.5 seconds
        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, HomeActivity::class.java))
            finish()
        }, 2500)
    }

    private fun applyTheme() {
        val theme = ThemeHelper.getThemeColors(this)
        val root: View = findViewById(R.id.splashRoot)
        root.setBackgroundColor(theme.bg)

        window.statusBarColor = theme.bg
        window.navigationBarColor = theme.bg
        if (theme.isDark) {
            window.decorView.systemUiVisibility = window.decorView.systemUiVisibility and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
        } else {
            window.decorView.systemUiVisibility = window.decorView.systemUiVisibility or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        }

        val splashTitle: TextView = findViewById(R.id.splashTitle)
        splashTitle.setTextColor(theme.txt)
    }
}
