package com.sabah.bikhushue

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.card.MaterialCardView
import com.google.android.material.switchmaterial.SwitchMaterial

object SettingsHelper {

    fun showSettingsDialog(
        activity: AppCompatActivity,
        onThemeChanged: (() -> Unit)? = null,
        onTajweedChanged: ((Boolean) -> Unit)? = null
    ) {
        val intent = Intent(activity, SettingsActivity::class.java)
        activity.startActivity(intent)
    }

    private fun applyThemeToPrefs(prefs: android.content.SharedPreferences, bg: String, txt: String, bar: String) {
        prefs.edit()
            .putString("bg_color", bg)
            .putString("txt_color", txt)
            .putString("bar_color", bar)
            .apply()
    }
}
