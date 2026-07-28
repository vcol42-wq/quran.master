package com.sabah.bikhushue

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        NotificationHelper.createChannels(this)
        stopService(Intent(this, AthanService::class.java))
        enableEdgeToEdge()
        setContentView(R.layout.activity_splash)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.splashRoot)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        applyTheme()

        val splashLogo: ImageView = findViewById(R.id.splashLogo)
        val splashTitle: View = findViewById(R.id.splashTitle)

        // Initial state
        splashLogo.alpha = 0f
        splashLogo.scaleX = 0.8f
        splashLogo.scaleY = 0.8f

        splashTitle.alpha = 0f
        splashTitle.translationY = 50f

        // Animations
        splashLogo.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(1200)
            .setInterpolator(android.view.animation.DecelerateInterpolator())
            .start()

        splashTitle.animate()
            .alpha(1f)
            .translationY(0f)
            .setStartDelay(500)
            .setDuration(800)
            .setInterpolator(android.view.animation.DecelerateInterpolator())
            .start()

        // Transition to ExperimentalHomeActivity (Interactive Interface) or HomeActivity after 2.5 seconds
        Handler(Looper.getMainLooper()).postDelayed({
            val prefs = getSharedPreferences("app", MODE_PRIVATE)
            val useExp = prefs.getBoolean("use_experimental_home", true)
            val targetClass = if (useExp) ExperimentalHomeActivity::class.java else HomeActivity::class.java
            startActivity(Intent(this, targetClass))
            finish()
        }, 2500)
    }

    private fun applyTheme() {
        ThemeHelper.applySystemWindowsColors(this)
    }
}
