package com.sabah.bikhushue

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
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.google.android.material.card.MaterialCardView

class AzkarActivity : AppCompatActivity() {

    private lateinit var dbHelper: AzkarDatabaseHelper
    private lateinit var rvAzkar: RecyclerView
    private lateinit var azkarList: MutableList<AzkarItem>
    private lateinit var adapter: AzkarAdapter

    private var currentCategory = "أذكار الصباح والمساء"
    private var txtColorInt = Color.BLACK
    private var barColorInt = Color.GRAY
    private var cardBgColorInt = Color.WHITE
    private var isBottomBarVisible = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_azkar)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.azkarRoot)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        dbHelper = AzkarDatabaseHelper(this)
        rvAzkar = findViewById(R.id.rvAzkar)
        rvAzkar.layoutManager = LinearLayoutManager(this)
        
        applyTheme()
        BottomBarHelper.setupBottomBar(this)
        
        val initialCategory = intent.getStringExtra("category")
        if (!initialCategory.isNullOrEmpty()) {
            currentCategory = initialCategory
        } else {
            currentCategory = "أذكار الصباح والمساء"
        }

        setupCategoryCards()
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
        
        val initialQuery = intent.getStringExtra("search_query")
        if (!initialQuery.isNullOrEmpty()) {
            etSearchAzkar.setText(initialQuery)
            searchAzkar(initialQuery)
        }
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
        val theme = ThemeHelper.getThemeColors(this)
        ThemeHelper.applySystemWindowsColors(this)
        val bgColor = theme.bg
        txtColorInt = theme.txt
        barColorInt = theme.bar
        cardBgColorInt = theme.cardBg

        val root: View = findViewById(R.id.azkarRoot)
        root.setBackgroundColor(bgColor)

                        val windowInsetsController = WindowInsetsControllerCompat(window, window.decorView)
        windowInsetsController.isAppearanceLightStatusBars = !theme.isDark
        windowInsetsController.isAppearanceLightNavigationBars = !theme.isDark

        findViewById<MaterialCardView>(R.id.azkarTopBar).setCardBackgroundColor(barColorInt)
        findViewById<MaterialCardView>(R.id.cardSearch)?.setCardBackgroundColor(cardBgColorInt)
        findViewById<EditText>(R.id.etSearchAzkar)?.let {
            it.setTextColor(if (theme.isDark) Color.WHITE else txtColorInt)
            it.setHintTextColor(if (theme.isDark) Color.parseColor("#A0FFFFFF") else Color.argb(160, Color.red(theme.txt), Color.green(theme.txt), Color.blue(theme.txt)))
        }

        // Theme the newly styled category cards
        val shadowColor = if (theme.isDark) Color.parseColor("#121212") else Color.parseColor("#D2B48C")
        findViewById<MaterialCardView>(R.id.cardMorningEvening)?.setCardBackgroundColor(cardBgColorInt)
        findViewById<MaterialCardView>(R.id.cardSadAzkar)?.setCardBackgroundColor(cardBgColorInt)
        
        // Find their shadow cards (the first child of their FrameLayout parents)
        val parentMorning = findViewById<MaterialCardView>(R.id.cardMorningEvening)?.parent as? android.widget.FrameLayout
        val shadowMorning = parentMorning?.getChildAt(0) as? MaterialCardView
        shadowMorning?.setCardBackgroundColor(shadowColor)

        val parentSad = findViewById<MaterialCardView>(R.id.cardSadAzkar)?.parent as? android.widget.FrameLayout
        val shadowSad = parentSad?.getChildAt(0) as? MaterialCardView
        shadowSad?.setCardBackgroundColor(shadowColor)
    }

    private fun setupCategoryCards() {
        val cardMorning = findViewById<MaterialCardView>(R.id.cardMorningEvening)
        val cardSad = findViewById<MaterialCardView>(R.id.cardSadAzkar)
        
        cardMorning?.setOnClickListener {
            currentCategory = "أذكار الصباح والمساء"
            updateCategorySelection()
            loadAzkar(currentCategory)
        }
        
        cardSad?.setOnClickListener {
            currentCategory = "دعاء المحزون"
            updateCategorySelection()
            loadAzkar(currentCategory)
        }
        
        updateCategorySelection()
    }
    
    private fun updateCategorySelection() {
        val tvMorning = findViewById<TextView>(R.id.tvMorningEvening)
        val tvSad = findViewById<TextView>(R.id.tvSadAzkar)
        
        tvMorning?.setTextColor(txtColorInt)
        tvSad?.setTextColor(txtColorInt)
        
        if (currentCategory == "أذكار الصباح والمساء") {
            tvMorning?.setTypeface(androidx.core.content.res.ResourcesCompat.getFont(this, R.font.amiri_quran) ?: android.graphics.Typeface.DEFAULT_BOLD, android.graphics.Typeface.BOLD)
            tvMorning?.alpha = 1f
            tvSad?.setTypeface(androidx.core.content.res.ResourcesCompat.getFont(this, R.font.amiri_quran) ?: android.graphics.Typeface.DEFAULT, android.graphics.Typeface.NORMAL)
            tvSad?.alpha = 0.5f
        } else if (currentCategory == "دعاء المحزون") {
            tvSad?.setTypeface(androidx.core.content.res.ResourcesCompat.getFont(this, R.font.amiri_quran) ?: android.graphics.Typeface.DEFAULT_BOLD, android.graphics.Typeface.BOLD)
            tvSad?.alpha = 1f
            tvMorning?.setTypeface(androidx.core.content.res.ResourcesCompat.getFont(this, R.font.amiri_quran) ?: android.graphics.Typeface.DEFAULT, android.graphics.Typeface.NORMAL)
            tvMorning?.alpha = 0.5f
        } else {
            tvMorning?.setTypeface(androidx.core.content.res.ResourcesCompat.getFont(this, R.font.amiri_quran) ?: android.graphics.Typeface.DEFAULT, android.graphics.Typeface.NORMAL)
            tvMorning?.alpha = 0.5f
            tvSad?.setTypeface(androidx.core.content.res.ResourcesCompat.getFont(this, R.font.amiri_quran) ?: android.graphics.Typeface.DEFAULT, android.graphics.Typeface.NORMAL)
            tvSad?.alpha = 0.5f
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
                    updateCategorySelection()
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
