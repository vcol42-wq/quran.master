package com.sabah.bikhushue

import androidx.appcompat.app.AppCompatActivity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.view.View
import android.widget.ImageView
import com.google.android.material.card.MaterialCardView

object BottomBarHelper {

    fun setupBottomBar(
        activity: AppCompatActivity,
        searchAction: (() -> Unit)? = null,
        assistantAction: (() -> Unit)? = null,
        onThemeChanged: (() -> Unit)? = null,
        onTajweedChanged: ((Boolean) -> Unit)? = null
    ) {
        val bottomBarLayout = activity.findViewById<MaterialCardView>(R.id.bottomBarLayout) ?: return
        val btnHome = activity.findViewById<ImageView>(R.id.btnHome)
        val btnSearch = activity.findViewById<ImageView>(R.id.btnSearch)
        val btnAssistant = activity.findViewById<ImageView>(R.id.btnAssistant)
        val btnSettings = activity.findViewById<ImageView>(R.id.btnSettings)
        
        val theme = ThemeHelper.getThemeColors(activity)
        val barColorInt = theme.bar
        val txtColorInt = theme.txt
        val subtleBorder = theme.shadow

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
            if (assistantAction != null) {
                assistantAction.invoke()
            } else {
                GeminiHelper.showAssistantDialog(activity)
            }
        }
        
        btnSettings?.setOnClickListener {
            SettingsHelper.showSettingsDialog(activity, onThemeChanged = onThemeChanged ?: {
                activity.recreate()
            }, onTajweedChanged = onTajweedChanged)
        }
    }
}
