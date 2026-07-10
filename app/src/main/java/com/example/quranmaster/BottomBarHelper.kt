package com.example.quranmaster

import androidx.appcompat.app.AppCompatActivity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.view.View
import android.widget.ImageView
import com.google.android.material.card.MaterialCardView

object BottomBarHelper {

    fun setupBottomBar(activity: AppCompatActivity, searchAction: (() -> Unit)? = null) {
        val bottomBarLayout = activity.findViewById<MaterialCardView>(R.id.bottomBarLayout) ?: return
        val btnHome = activity.findViewById<ImageView>(R.id.btnHome)
        val btnSearch = activity.findViewById<ImageView>(R.id.btnSearch)
        val btnAssistant = activity.findViewById<ImageView>(R.id.btnAssistant)
        val btnSettings = activity.findViewById<ImageView>(R.id.btnSettings)
        
        val prefs = activity.getSharedPreferences("app", Context.MODE_PRIVATE)
        val barColorInt = Color.parseColor(prefs.getString("bar_color", "#E6DCC8") ?: "#E6DCC8")
        val txtColorInt = Color.parseColor(prefs.getString("text_color", "#3D2B1F") ?: "#3D2B1F")
        val subtleBorder = Color.argb(30, Color.red(txtColorInt), Color.green(txtColorInt), Color.blue(txtColorInt))

        bottomBarLayout.setCardBackgroundColor(barColorInt)
        bottomBarLayout.strokeColor = subtleBorder
        bottomBarLayout.strokeWidth = 1
        
        activity.findViewById<View>(R.id.bottomBarInnerLayout)?.setBackgroundColor(Color.TRANSPARENT)

        btnHome?.setColorFilter(txtColorInt)
        btnSearch?.setColorFilter(txtColorInt)
        btnAssistant?.setColorFilter(txtColorInt)
        btnSettings?.setColorFilter(txtColorInt)

        btnHome?.setOnClickListener { 
            activity.finish() 
        }
        
        btnSearch?.setOnClickListener {
            if (searchAction != null) {
                searchAction.invoke()
            } else {
                val intent = Intent(activity, MainActivity::class.java).apply {
                    putExtra("OPEN_SEARCH", true)
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                }
                activity.startActivity(intent)
            }
        }
        
        btnAssistant?.setOnClickListener {
            GeminiHelper.showAssistantDialog(activity)
        }
        
        btnSettings?.setOnClickListener {
            SettingsHelper.showSettingsDialog(activity, onThemeChanged = {
                activity.recreate()
            })
        }
    }
}
