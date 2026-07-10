package com.example.quranmaster

import android.app.AlertDialog
import android.text.Editable
import android.text.TextWatcher
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView

class AzkarActivity : AppCompatActivity() {

    private lateinit var dbHelper: AzkarDatabaseHelper
    private lateinit var rvAzkar: RecyclerView
    private lateinit var llCategories: LinearLayout
    private lateinit var azkarList: MutableList<AzkarItem>
    private lateinit var adapter: AzkarAdapter

    private var currentCategory = "أذكار الصباح والمساء"
    private var txtColorInt = Color.BLACK
    private var barColorInt = Color.GRAY
    private var cardBgColorInt = Color.WHITE
    private var isBottomBarVisible = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_azkar)

        dbHelper = AzkarDatabaseHelper(this)
        rvAzkar = findViewById(R.id.rvAzkar)
        rvAzkar.layoutManager = LinearLayoutManager(this)
        llCategories = findViewById(R.id.llCategories)

        applyTheme()
        BottomBarHelper.setupBottomBar(this)
        loadCategories()
        loadAzkar(currentCategory)

        rvAzkar.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
                if (dy > 30 && isBottomBarVisible) toggleBottomBar()
                else if (dy < -30 && !isBottomBarVisible) toggleBottomBar()
            }
        })

        findViewById<View>(R.id.btnBackAzkar).setOnClickListener { finish() }
        findViewById<View>(R.id.btnAddZikr).setOnClickListener { showAddCustomZikrDialog() }
        
        val etSearchAzkar = findViewById<EditText>(R.id.etSearchAzkar)
        etSearchAzkar.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString().trim()
                if (query.isNotEmpty()) {
                    searchAzkar(query)
                } else {
                    loadAzkar(currentCategory)
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun searchAzkar(query: String) {
        val rawList = dbHelper.searchAzkar(query)
        
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        val isMorning = hour < 12
        
        azkarList = rawList.map { item ->
            if (item.category == "أذكار الصباح والمساء" || item.category.contains("الصباح") || item.category.contains("المساء")) {
                item.copy(text = adjustZikrTextForTime(item.text, isMorning))
            } else {
                item
            }
        }.toMutableList()
        
        adapter = AzkarAdapter(this, azkarList, txtColorInt, cardBgColorInt, barColorInt)
        rvAzkar.adapter = adapter
    }

    private fun applyTheme() {
        val prefs = getSharedPreferences("app", MODE_PRIVATE)
        val savedBg = prefs.getString("bg_color", "#F4ECD8") ?: "#F4ECD8"
        val savedTxt = prefs.getString("txt_color", "#000000") ?: "#000000"
        val savedBar = prefs.getString("bar_color", "#E6DCC8") ?: "#E6DCC8"

        val bgColor = Color.parseColor(savedBg)
        txtColorInt = Color.parseColor(savedTxt)
        barColorInt = Color.parseColor(savedBar)
        cardBgColorInt = if (savedBg == "#121212") Color.parseColor("#2D2D2D") else Color.WHITE

        val root: View = findViewById(android.R.id.content)
        root.setBackgroundColor(bgColor)

        window.statusBarColor = barColorInt
        window.navigationBarColor = barColorInt
        if (savedBg == "#121212") {
            window.decorView.systemUiVisibility = window.decorView.systemUiVisibility and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
        } else {
            window.decorView.systemUiVisibility = window.decorView.systemUiVisibility or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        }

        findViewById<MaterialCardView>(R.id.azkarTopBar).setCardBackgroundColor(barColorInt)
    }

    private fun loadCategories() {
        llCategories.removeAllViews()
        val categories = dbHelper.getCategories()
        
        for (category in categories) {
            val tv = TextView(this).apply {
                text = category
                textSize = 18f
                setTextColor(if (category == currentCategory) txtColorInt else Color.GRAY)
                setPadding(24, 8, 24, 8)
                setOnClickListener {
                    currentCategory = category
                    loadCategories() // Refresh selection color
                    loadAzkar(category)
                }
            }
            llCategories.addView(tv)
        }
    }

    private fun adjustZikrTextForTime(text: String, isMorning: Boolean): String {
        var adjusted = text
        if (isMorning) {
            adjusted = adjusted.replace("أمسيت", "أصبحت")
            adjusted = adjusted.replace("أمسى", "أصبح")
            adjusted = adjusted.replace("أمسينا", "أصبحنا")
            adjusted = adjusted.replace("أَمْسَيْنَا", "أَصْبَحْنَا")
            adjusted = adjusted.replace("وَأَمْسَى", "وَأَصْبَحَ")
            adjusted = adjusted.replace("اللَّيْلَةِ", "الْيَوْمِ")
            adjusted = adjusted.replace("هذه الليلة", "هذا اليوم")
            adjusted = adjusted.replace("ليلتنا", "يومنا")
            adjusted = adjusted.replace("امسى", "اصبح")
        } else {
            adjusted = adjusted.replace("أصبحت", "أمسيت")
            adjusted = adjusted.replace("أصبح", "أمسى")
            adjusted = adjusted.replace("أصبحنا", "أمسينا")
            adjusted = adjusted.replace("أَصْبَحْنَا", "أَمْسَيْنَا")
            adjusted = adjusted.replace("وَأَصْبَحَ", "وَأَمْسَى")
            adjusted = adjusted.replace("الْيَوْمِ", "اللَّيْلَةِ")
            adjusted = adjusted.replace("هذا اليوم", "هذه الليلة")
            adjusted = adjusted.replace("يومنا", "ليلتنا")
            adjusted = adjusted.replace("اصبح", "امسى")
        }
        return adjusted
    }

    private fun loadAzkar(category: String) {
        val rawList = dbHelper.getAzkarByCategory(category)
        
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        val isMorning = hour < 12
        
        azkarList = rawList.map { item ->
            if (item.category == "أذكار الصباح والمساء") {
                item.copy(text = adjustZikrTextForTime(item.text, isMorning))
            } else {
                item
            }
        }.toMutableList()
        
        adapter = AzkarAdapter(this, azkarList, txtColorInt, cardBgColorInt, barColorInt)
        rvAzkar.adapter = adapter
    }

    private fun showAddCustomZikrDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("إضافة ذكر جديد")

        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(40, 20, 40, 20)

        val titleInput = EditText(this)
        titleInput.hint = "عنوان الذكر"
        layout.addView(titleInput)

        val textInput = EditText(this)
        textInput.hint = "نص الذكر"
        layout.addView(textInput)

        val countInput = EditText(this)
        countInput.hint = "عدد التكرار (مثال: 33)"
        countInput.inputType = android.text.InputType.TYPE_CLASS_NUMBER
        layout.addView(countInput)

        builder.setView(layout)

        builder.setPositiveButton("حفظ") { _, _ ->
            val title = titleInput.text.toString().trim()
            val text = textInput.text.toString().trim()
            val countStr = countInput.text.toString().trim()

            if (title.isNotEmpty() && text.isNotEmpty() && countStr.isNotEmpty()) {
                val count = countStr.toIntOrNull() ?: 1
                val newItem = AzkarItem(0, "أذكاري الخاصة", title, text, "", count, count, true)
                dbHelper.insertCustomZikr(newItem)
                Toast.makeText(this, "تمت الإضافة بنجاح", Toast.LENGTH_SHORT).show()
                
                if (currentCategory != "أذكاري الخاصة") {
                    currentCategory = "أذكاري الخاصة"
                    loadCategories()
                }
                loadAzkar(currentCategory)
            } else {
                Toast.makeText(this, "يرجى تعبئة جميع الحقول", Toast.LENGTH_SHORT).show()
            }
        }
        builder.setNegativeButton("إلغاء", null)
        builder.show()
    }

    private fun toggleBottomBar() {
        val bottomBarLayout = findViewById<View>(R.id.bottomBarLayout) ?: return
        isBottomBarVisible = !isBottomBarVisible
        bottomBarLayout.animate()
            .translationY(if (isBottomBarVisible) 0f else bottomBarLayout.height.toFloat() + 100f)
            .setDuration(300).start()
    }
}
