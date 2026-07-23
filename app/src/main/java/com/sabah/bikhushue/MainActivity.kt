package com.sabah.bikhushue

import android.graphics.Color
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.ScaleGestureDetector
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.tabs.TabLayout
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var mainRootLayout: RelativeLayout
    private lateinit var quranRecyclerView: RecyclerView
    private lateinit var topBarContainer: com.google.android.material.card.MaterialCardView
    private lateinit var bottomBarLayout: com.google.android.material.card.MaterialCardView
    private lateinit var topBarJuzText: TextView
    private lateinit var topBarSuraText: TextView
    private lateinit var topBarPageText: TextView
    private lateinit var topBarTimerText: TextView
    
    private lateinit var dbHelper: DatabaseHelper
    private var blockList = ArrayList<VerseBlock>()
    private lateinit var quranAdapter: QuranAdapter
    private var isBarsVisible = true
    private var countdownTimer: android.os.CountDownTimer? = null
    private lateinit var scaleGestureDetector: ScaleGestureDetector
    
    private var mediaPlayer: MediaPlayer? = null
    
    // Prayer Mode Variables
    private var isPrayerModeActive = false
    private var isPrayerModePaused = false
    private var prayerMsElapsed = 0L
    private var prayerRukuCount = 0
    private var countedRukus = mutableSetOf<Int>()
    private var prayerTimerRunnable: Runnable? = null
    private var autoScrollRunnable: Runnable? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private var scrollSpeedDelay = 40L
    private lateinit var prayerTimerWidget: View
    private lateinit var prayerTimeText: TextView
    private lateinit var prayerTotalRukuText: TextView
    private lateinit var btnSpeedUp: TextView
    private lateinit var btnSpeedDown: TextView
    private lateinit var btnStopPrayer: TextView

    // Open Timer Variables
    private var isOpenTimerActive = false
    private var isOpenTimerPaused = false
    private var openTimerSeconds = 0
    private var openTimerRunnable: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        stopService(android.content.Intent(this, AthanService::class.java))
        enableEdgeToEdge()
        ThemeHelper.applySystemWindowsColors(this)
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mainRootLayout)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        initViews()
        
        // تطبيق الثيم الأولي المحفوظ
        val prefs = getSharedPreferences("app", MODE_PRIVATE)
        val savedBg = prefs.getString("bg_color", "#121212") ?: "#121212"
        val savedTxt = prefs.getString("txt_color", "#000000") ?: "#000000"
        val savedBar = prefs.getString("bar_color", "#E6DCC8") ?: "#E6DCC8"
        applyTheme(savedBg, savedTxt, savedBar)
        
        setupZoom()
        showMainContent()
        cleanTempAudioFiles()

        // Hide bars if landscape
        if (resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE) {
            isBarsVisible = false
            topBarContainer.visibility = View.GONE
            bottomBarLayout.visibility = View.GONE
        }
        
        if (intent.getBooleanExtra("OPEN_SEARCH", false)) {
            openSearchDialog()
        }
        
        handleSearchIntents(intent)
    }
    
    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra("OPEN_SEARCH", false)) {
            openSearchDialog()
        }
        handleSearchIntents(intent)
    }

    private fun handleSearchIntents(intent: android.content.Intent) {
        if (intent.hasExtra("SCROLL_TO_SURA")) {
            val sura = intent.getIntExtra("SCROLL_TO_SURA", 1)
            val aya = intent.getIntExtra("SCROLL_TO_AYA", 1)
            
            Thread {
                while(blockList.isEmpty()) Thread.sleep(100)
                val blockIndex = blockList.indexOfFirst { b -> b.verses.any { it.sura == sura && it.aya == aya } }
                val verse = blockList.flatMap { it.verses }.find { it.sura == sura && it.aya == aya }
                if (blockIndex != -1 && verse != null) {
                    runOnUiThread {
                        val lm = quranRecyclerView.layoutManager as LinearLayoutManager
                        lm.scrollToPositionWithOffset(blockIndex, 0)
                        quranAdapter.highlightedSura = sura
                        quranAdapter.highlightedAya = aya
                        quranAdapter.notifyDataSetChanged()
                        updateTopBar(blockList[blockIndex])
                        
                        if (intent.getBooleanExtra("ACTION_AUDIO", false)) {
                            playInternalAudio(verse)
                        } else if (intent.getBooleanExtra("ACTION_GEMINI", false)) {
                            searchGemini(verse)
                        }
                    }
                }
            }.start()
        }
    }

    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        if (newConfig.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE) {
            isBarsVisible = false
            topBarContainer.visibility = View.GONE
            bottomBarLayout.visibility = View.GONE
        } else {
            isBarsVisible = true
            topBarContainer.visibility = View.VISIBLE
            bottomBarLayout.visibility = View.VISIBLE
            bottomBarLayout.translationY = 0f
            topBarContainer.translationY = 0f
        }
    }

    private fun cleanTempAudioFiles() {
        Thread {
            cacheDir.listFiles()?.forEach { 
                if (it.name.startsWith("temp_audio_")) it.delete() 
            }
        }.start()
    }

    private fun initViews() {
        mainRootLayout = findViewById(R.id.mainRootLayout)
        quranRecyclerView = findViewById(R.id.quranRecyclerView)
        topBarContainer = findViewById(R.id.topBarContainer)
        bottomBarLayout = findViewById(R.id.bottomBarLayout)
        topBarJuzText = findViewById(R.id.topBarJuzText)
        topBarSuraText = findViewById(R.id.topBarSuraText)
        topBarPageText = findViewById(R.id.topBarPageText)
        topBarTimerText = findViewById(R.id.topBarTimerText)
        
        prayerTimerWidget = findViewById(R.id.prayerTimerWidget)
        prayerTimeText = findViewById(R.id.prayerTimeText)
        prayerTotalRukuText = findViewById(R.id.prayerTotalRukuText)
        btnSpeedUp = findViewById(R.id.btnSpeedUp)
        btnSpeedDown = findViewById(R.id.btnSpeedDown)
        btnStopPrayer = findViewById(R.id.btnStopPrayer)
        
        setupPrayerTimerWidget()

        val onThemeChangedAction = {
            val prefs = getSharedPreferences("app", MODE_PRIVATE)
            val savedBg = prefs.getString("bg_color", "#121212") ?: "#121212"
            val savedTxt = prefs.getString("txt_color", "#000000") ?: "#000000"
            val savedBar = prefs.getString("bar_color", "#E6DCC8") ?: "#E6DCC8"
            applyTheme(savedBg, savedTxt, savedBar)
            
            // تحديث الودجت فورا عند تغيير الثيم
            val intent = android.content.Intent(this, PrayerWidgetProvider::class.java).apply {
                action = android.appwidget.AppWidgetManager.ACTION_APPWIDGET_UPDATE
                val ids = android.appwidget.AppWidgetManager.getInstance(application).getAppWidgetIds(android.content.ComponentName(application, PrayerWidgetProvider::class.java))
                putExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            }
            sendBroadcast(intent)
        }
        val onTajweedChangedAction = { isChecked: Boolean ->
            if (::quranAdapter.isInitialized) {
                val prefs = getSharedPreferences("app", MODE_PRIVATE)
                val savedSepStr = prefs.getString("separator_type", "PAGE")
                quranAdapter.currentSeparator = SeparatorType.fromString(savedSepStr)
                quranAdapter.showTajweedColors = prefs.getBoolean("tajweed_on", true)
                quranAdapter.showHizb = prefs.getBoolean("show_hizb", false)
                quranAdapter.showManzil = prefs.getBoolean("show_manzil", false)
                quranAdapter.notifyDataSetChanged()
            }
        }
        BottomBarHelper.setupBottomBar(
            this,
            searchAction = { openSearchDialog() },
            onThemeChanged = onThemeChangedAction,
            onTajweedChanged = onTajweedChangedAction
        )
        
        topBarSuraText.setOnClickListener { openUnifiedIndex(0) }
        topBarJuzText.setOnClickListener { openUnifiedIndex(1) }
        topBarTimerText.setOnClickListener {
            if (isOpenTimerActive) {
                isOpenTimerPaused = !isOpenTimerPaused
                val topBarTimerCard = findViewById<com.google.android.material.card.MaterialCardView>(R.id.topBarTimerCard)
                val bgColor = if (isOpenTimerPaused) "#FFCDD2" else "#D2B48C"
                topBarTimerCard?.setCardBackgroundColor(Color.parseColor(bgColor))
                Toast.makeText(this, if (isOpenTimerPaused) "تم إيقاف المؤقت مؤقتاً" else "تم استئناف المؤقت", Toast.LENGTH_SHORT).show()
            } else {
                showTimerDialog()
            }
        }
        topBarTimerText.setOnLongClickListener {
            if (isOpenTimerActive) {
                stopOpenTimer()
                Toast.makeText(this, "تم إيقاف وإعادة ضبط المؤقت", Toast.LENGTH_SHORT).show()
                true
            } else {
                false
            }
        }
    }

    private fun setupZoom() {
        scaleGestureDetector = ScaleGestureDetector(this, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                if (::quranAdapter.isInitialized) {
                    if (detector.scaleFactor > 1.0f) {
                        if (quranAdapter.quranFontSize < 80f) quranAdapter.quranFontSize += 0.6f
                    } else {
                        if (quranAdapter.quranFontSize > 18f) quranAdapter.quranFontSize -= 0.6f
                    }
                    quranAdapter.notifyDataSetChanged()
                }
                return true
            }
        })
        quranRecyclerView.setOnTouchListener { _, event ->
            scaleGestureDetector.onTouchEvent(event)
            false
        }
    }

    private fun showMainContent() {
        quranRecyclerView.visibility = View.VISIBLE
        topBarContainer.visibility = View.VISIBLE
        bottomBarLayout.visibility = View.VISIBLE

        loadQuranData()
    }

    private fun loadQuranData() {
        Thread {
            dbHelper = DatabaseHelper(this@MainActivity)
            dbHelper.checkAndCopyDatabase()
            try {
                val verses = dbHelper.getAllQuranVerses()
                val suraNames = dbHelper.getSuraNames()
                val processedBlocks = groupVersesByPage(verses, suraNames)
                
                runOnUiThread {
                    blockList = processedBlocks
                    if (blockList.isNotEmpty()) {
                        quranAdapter = QuranAdapter(blockList, { toggleBarsVisibility() }, { showVerseOptions(it) })
                        
                        val prefs = getSharedPreferences("app", MODE_PRIVATE)
                        val savedBg = prefs.getString("bg_color", "#121212") ?: "#121212"
                        val savedTxt = prefs.getString("txt_color", "#000000") ?: "#000000"
                        val savedBar = prefs.getString("bar_color", "#E6DCC8") ?: "#E6DCC8"
                        val savedSepStr = prefs.getString("separator_type", "PAGE")
                        quranAdapter.currentSeparator = SeparatorType.fromString(savedSepStr)
                        quranAdapter.showTajweedColors = prefs.getBoolean("tajweed_on", true)
                        quranAdapter.showHizb = prefs.getBoolean("show_hizb", false)
                        quranAdapter.showManzil = prefs.getBoolean("show_manzil", false)
                        
                        applyTheme(savedBg, savedTxt, savedBar)

                        quranRecyclerView.layoutManager = LinearLayoutManager(this@MainActivity)
                        quranRecyclerView.adapter = quranAdapter
                        
                        quranRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                            override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
                                if (dy > 30 && isBarsVisible) toggleBarsVisibility()
                                else if (dy < -30 && !isBarsVisible) toggleBarsVisibility()

                                val layoutManager = rv.layoutManager as LinearLayoutManager
                                val pos = layoutManager.findFirstVisibleItemPosition()
                                if (pos != RecyclerView.NO_POSITION && pos < blockList.size) {
                                    val firstView = layoutManager.findViewByPosition(pos)
                                    val offset = firstView?.top ?: 0
                                    updateTopBar(blockList[pos])
                                    getSharedPreferences("app", MODE_PRIVATE).edit()
                                        .putInt("last_page", pos)
                                        .putInt("last_offset", offset)
                                        .apply()
                                }
                            }
                        })
                        
                        val lastPage = prefs.getInt("last_page", 0)
                        val lastOffset = prefs.getInt("last_offset", 0)
                        if (lastPage < blockList.size) {
                            (quranRecyclerView.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(lastPage, lastOffset)
                            updateTopBar(blockList[lastPage])
                        } else {
                            updateTopBar(blockList[0])
                        }
                    }
                }
            } catch (e: Exception) { Log.e("Quran", "Load: ${e.message}") }
        }.start()
    }

    private fun groupVersesByPage(verses: List<VerseModel>, suraNames: Map<Int, String>): ArrayList<VerseBlock> {
        val blocks = ArrayList<VerseBlock>()
        for (i in verses.indices) {
            val v = verses[i]
            v.suraName = suraNames[v.sura] ?: "سورة ${v.sura}"
        }
        var curPage = -1; var curVerses = ArrayList<VerseModel>()
        for (v in verses) {
            val isStartOfSura = (v.sura != 1 && v.aya == 1) || (v.sura == 1 && v.aya == 2)
            if (v.page != curPage || isStartOfSura) {
                if (curVerses.isNotEmpty()) {
                    val firstV = curVerses[0]
                    val showH = (firstV.sura != 1 && firstV.aya == 1) || (firstV.sura == 1 && firstV.aya == 2)
                    blocks.add(VerseBlock(curVerses, curPage, firstV.juz, firstV.sura, firstV.suraName, showH))
                }
                curPage = v.page; curVerses = ArrayList()
            }
            curVerses.add(v)
        }
        if (curVerses.isNotEmpty()) {
            val firstV = curVerses[0]
            val showH = (firstV.sura != 1 && firstV.aya == 1) || (firstV.sura == 1 && firstV.aya == 2)
            blocks.add(VerseBlock(curVerses, curPage, firstV.juz, firstV.sura, firstV.suraName, showH))
        }
        return blocks
    }

    private fun updateTopBar(b: VerseBlock) {
        topBarPageText.text = "ص ${b.pageNumber}"
        topBarSuraText.text = "${b.suraNumber} ${b.suraName}"
        topBarJuzText.text = "الجزء ${b.juzNumber}"
    }

    private fun getJuzArabicName(num: Int): String {
        val names = arrayOf("الأول","الثاني","الثالث","الرابع","الخامس","السادس","السابع","الثامن","التاسع","العاشر","الحادي عشر","الثاني عشر","الثالث عشر","الرابع عشر","الخامس عشر","السادس عشر","السابع عشر","الثامن عشر","التاسع عشر","العشرون","الحادي والعشرون","الثاني والعشرون","الثالث والعشرون","الرابع والعشرون","الخامس والعشرون","السادس والعشرون","السابع والعشرون","الثامن والعشرون","التاسع والعشرون","الثلاثون")
        return if (num in 1..30) names[num-1] else num.toString()
    }

    private fun toggleBarsVisibility() {
        if (resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE) {
            isBarsVisible = false
            topBarContainer.visibility = View.GONE
            bottomBarLayout.visibility = View.GONE
            return
        }
        isBarsVisible = !isBarsVisible
        bottomBarLayout.animate().translationY(if (isBarsVisible) 0f else bottomBarLayout.height.toFloat()).setDuration(300).start()
        topBarContainer.animate().translationY(if (isBarsVisible) 0f else -topBarContainer.height.toFloat()).setDuration(300).start()
    }



    private fun applyTheme(bg: String, txt: String, bar: String) {
        val isDarkMode = bg == "#121212"
        val bgColor = Color.parseColor(bg)
        val resolvedTxtHex = if (isDarkMode) "#E0E0E0" else (if (txt == "#000000" || txt.isBlank()) "#212121" else txt)
        val txtColor = Color.parseColor(resolvedTxtHex)
        val barColor = if (isDarkMode) Color.parseColor("#2D2D2D") else Color.parseColor(bar)
        val subtleBorder = if (isDarkMode) Color.parseColor("#555555") else Color.parseColor("#E4D7B4")

        if (::quranAdapter.isInitialized) {
            quranAdapter.currentBgColor = bg
            quranAdapter.currentTextColor = resolvedTxtHex
            quranAdapter.notifyDataSetChanged()
        }

        mainRootLayout.setBackgroundColor(bgColor)
        quranRecyclerView.setBackgroundColor(Color.TRANSPARENT)
        
        val windowInsetsController = WindowInsetsControllerCompat(window, window.decorView)
        windowInsetsController.isAppearanceLightStatusBars = !isDarkMode
        windowInsetsController.isAppearanceLightNavigationBars = !isDarkMode

        topBarContainer.setCardBackgroundColor(barColor)
        bottomBarLayout.setCardBackgroundColor(barColor)
        topBarContainer.strokeColor = subtleBorder
        bottomBarLayout.strokeColor = subtleBorder
        topBarContainer.strokeWidth = 1
        bottomBarLayout.strokeWidth = 1

        val topBarTimerCard = findViewById<com.google.android.material.card.MaterialCardView>(R.id.topBarTimerCard)
        val miniPlayer = findViewById<View>(R.id.mini_player)
        
        if (bg == "#121212") {
            topBarTimerCard?.setCardBackgroundColor(Color.parseColor("#555555"))
            miniPlayer?.setBackgroundColor(Color.parseColor("#2D2D2D"))
            findViewById<TextView>(R.id.tvMiniPlayerTitle)?.setTextColor(Color.parseColor("#E0E0E0"))
            findViewById<ImageView>(R.id.btnMiniRewind)?.setColorFilter(Color.parseColor("#E0E0E0"))
            findViewById<ImageView>(R.id.btnMiniPlayPause)?.setColorFilter(Color.parseColor("#E0E0E0"))
            findViewById<ImageView>(R.id.btnMiniForward)?.setColorFilter(Color.parseColor("#E0E0E0"))
            findViewById<ImageView>(R.id.btnMiniStop)?.setColorFilter(Color.parseColor("#E0E0E0"))
        } else {
            topBarTimerCard?.setCardBackgroundColor(Color.parseColor("#D2B48C"))
            miniPlayer?.setBackgroundColor(Color.parseColor("#D2B48C"))
            findViewById<TextView>(R.id.tvMiniPlayerTitle)?.setTextColor(txtColor)
            findViewById<ImageView>(R.id.btnMiniRewind)?.setColorFilter(txtColor)
            findViewById<ImageView>(R.id.btnMiniPlayPause)?.setColorFilter(txtColor)
            findViewById<ImageView>(R.id.btnMiniForward)?.setColorFilter(txtColor)
            findViewById<ImageView>(R.id.btnMiniStop)?.setColorFilter(txtColor)
        }

        findViewById<View>(R.id.topBarInnerLayout)?.setBackgroundColor(Color.TRANSPARENT)
        findViewById<View>(R.id.bottomBarInnerLayout)?.setBackgroundColor(Color.TRANSPARENT)

        topBarJuzText.setTextColor(txtColor)
        topBarSuraText.setTextColor(txtColor)
        topBarPageText.setTextColor(txtColor)
        topBarTimerText.setTextColor(txtColor)

        // تطبيق لون الخلفية الداخلية للشريط العلوي ليتناسق مع البطاقات
        val topBarInnerLayout = findViewById<View>(R.id.topBarInnerLayout)
        val innerBg = if (isDarkMode) Color.parseColor("#1A1A1A") else Color.parseColor("#F4ECD8")
        topBarInnerLayout?.setBackgroundColor(Color.TRANSPARENT)


        val onThemeChangedAction = {
            val prefs = getSharedPreferences("app", MODE_PRIVATE)
            val savedBg = prefs.getString("bg_color", "#121212") ?: "#121212"
            val savedTxt = prefs.getString("txt_color", "#000000") ?: "#000000"
            val savedBar = prefs.getString("bar_color", "#E6DCC8") ?: "#E6DCC8"
            applyTheme(savedBg, savedTxt, savedBar)
            
            val intent = android.content.Intent(this, PrayerWidgetProvider::class.java).apply {
                action = android.appwidget.AppWidgetManager.ACTION_APPWIDGET_UPDATE
                val ids = android.appwidget.AppWidgetManager.getInstance(application).getAppWidgetIds(android.content.ComponentName(application, PrayerWidgetProvider::class.java))
                putExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            }
            sendBroadcast(intent)
        }
        val onTajweedChangedAction = { isChecked: Boolean ->
            if (::quranAdapter.isInitialized) {
                val prefs = getSharedPreferences("app", MODE_PRIVATE)
                val savedSepStr = prefs.getString("separator_type", "PAGE")
                quranAdapter.currentSeparator = SeparatorType.fromString(savedSepStr)
                quranAdapter.showTajweedColors = prefs.getBoolean("tajweed_on", true)
                quranAdapter.showHizb = prefs.getBoolean("show_hizb", false)
                quranAdapter.showManzil = prefs.getBoolean("show_manzil", false)
                quranAdapter.notifyDataSetChanged()
            }
        }
        BottomBarHelper.setupBottomBar(
            this,
            searchAction = { openSearchDialog() },
            onThemeChanged = onThemeChangedAction,
            onTajweedChanged = onTajweedChangedAction
        )

        if (::quranAdapter.isInitialized) {
            quranAdapter.currentTextColor = txt
            quranAdapter.currentBgColor = bg
            quranAdapter.notifyDataSetChanged()
        }

        getSharedPreferences("app", MODE_PRIVATE).edit()
            .putString("bg_color", bg)
            .putString("txt_color", txt)
            .putString("bar_color", bar)
            .apply()
    }

    private fun showTimerDialog() {
        val options = arrayOf("5 دقائق", "10 دقائق", "15 دقيقة", "20 دقيقة", "الوقت المفتوح (تصاعدي)", "وضع الصلاة (مؤقت + تحريك للركوع)", "وقت مخصص")
        AlertDialog.Builder(this).setTitle("الخيارات").setItems(options) { _, which ->
            if (which < 4) {
                stopOpenTimer()
                startTimer((which + 1) * 5)
            } else if (which == 4) {
                startOpenTimer()
            } else if (which == 5) {
                stopOpenTimer()
                startPrayerMode()
            } else {
                stopOpenTimer()
                showCustomTimerInput()
            }
        }.show()
    }
    
    private fun startOpenTimer() {
        stopOpenTimer()
        countdownTimer?.cancel()
        isOpenTimerActive = true
        isOpenTimerPaused = false
        openTimerSeconds = 0
        topBarTimerText.text = "00:00"
        
        val topBarTimerCard = findViewById<com.google.android.material.card.MaterialCardView>(R.id.topBarTimerCard)
        topBarTimerCard?.setCardBackgroundColor(Color.parseColor("#D2B48C"))
        
        openTimerRunnable = object : Runnable {
            override fun run() {
                if (isOpenTimerActive) {
                    if (!isOpenTimerPaused) {
                        openTimerSeconds++
                        val mins = openTimerSeconds / 60
                        val secs = openTimerSeconds % 60
                        topBarTimerText.text = String.format(Locale.ENGLISH, "%02d:%02d", mins, secs)
                    }
                    mainHandler.postDelayed(this, 1000)
                }
            }
        }
        mainHandler.post(openTimerRunnable!!)
        Toast.makeText(this, "تم تشغيل الوقت المفتوح. نقرة للإيقاف المؤقت، نقرة مطولة للإلغاء.", Toast.LENGTH_LONG).show()
    }

    private fun stopOpenTimer() {
        isOpenTimerActive = false
        isOpenTimerPaused = false
        openTimerSeconds = 0
        openTimerRunnable?.let { mainHandler.removeCallbacks(it) }
        openTimerRunnable = null
        topBarTimerText.text = "00:00"
        val topBarTimerCard = findViewById<com.google.android.material.card.MaterialCardView>(R.id.topBarTimerCard)
        topBarTimerCard?.setCardBackgroundColor(Color.parseColor("#D2B48C"))
    }
    
    private fun setupPrayerTimerWidget() {
        val pauseResumeAction = {
            isPrayerModePaused = !isPrayerModePaused
            val bgColor = if (isPrayerModePaused) "#FFCDD2" else "#FFFFFF"
            val strokeColor = if (isPrayerModePaused) "#FF8A80" else "#A5D6A7"
            val card = prayerTimerWidget as com.google.android.material.card.MaterialCardView
            card.setCardBackgroundColor(Color.parseColor(bgColor))
            card.strokeColor = Color.parseColor(strokeColor)
            Toast.makeText(this, if (isPrayerModePaused) "تم الإيقاف مؤقتاً" else "تم الاستئناف", Toast.LENGTH_SHORT).show()
        }
        prayerTimerWidget.setOnClickListener { pauseResumeAction() }
        prayerTimeText.setOnClickListener { pauseResumeAction() }
        
        btnStopPrayer.setOnClickListener {
            stopPrayerMode()
            Toast.makeText(this, "تم إيقاف وضع الصلاة", Toast.LENGTH_SHORT).show()
        }

        btnSpeedUp.setOnClickListener {
            if (scrollSpeedDelay > 10L) {
                scrollSpeedDelay -= 5L
                Toast.makeText(this, "تم زيادة السرعة", Toast.LENGTH_SHORT).show()
            }
        }

        btnSpeedDown.setOnClickListener {
            if (scrollSpeedDelay < 100L) {
                scrollSpeedDelay += 5L
                Toast.makeText(this, "تم تقليل السرعة", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun startPrayerMode() {
        prayerTimerWidget.visibility = View.VISIBLE
        bottomBarLayout.visibility = View.GONE
        isPrayerModeActive = true
        isPrayerModePaused = false
        prayerMsElapsed = 0L
        prayerRukuCount = 0
        countedRukus.clear()
        
        val prefs = getSharedPreferences("app", MODE_PRIVATE)
        val showArabic = prefs.getBoolean("show_arabic_ruku", false)
        val showEastern = prefs.getBoolean("show_eastern_ruku", false)
        val lm = quranRecyclerView.layoutManager as LinearLayoutManager
        val firstPos = lm.findFirstVisibleItemPosition()
        val lastPos = lm.findLastVisibleItemPosition()
        if (firstPos != RecyclerView.NO_POSITION && lastPos != RecyclerView.NO_POSITION) {
            for (i in firstPos..lastPos) {
                val block = blockList.getOrNull(i) ?: continue
                for (v in block.verses) {
                    val isRuku = (showArabic && v.rukooArDisplay.isNotBlank()) || (showEastern && v.rukooShDisplay.isNotBlank())
                    if (isRuku) countedRukus.add(v.id)
                }
            }
        }
        
        scrollSpeedDelay = 40L
        prayerTimeText.text = "00:00"
        prayerTotalRukuText.text = "الركوعات: 0"
        
        val card = prayerTimerWidget as com.google.android.material.card.MaterialCardView
        card.setCardBackgroundColor(Color.parseColor("#FFFFFF"))
        card.strokeColor = Color.parseColor("#A5D6A7")
        
        prayerTimerRunnable?.let { mainHandler.removeCallbacks(it) }
        autoScrollRunnable?.let { mainHandler.removeCallbacks(it) }
        
        prayerTimerRunnable = object : Runnable {
            override fun run() {
                if (isPrayerModeActive && !isPrayerModePaused) {
                    prayerMsElapsed += 1000
                    prayerTimeText.text = String.format(Locale.ENGLISH, "%02d:%02d", (prayerMsElapsed / 60000), (prayerMsElapsed % 60000) / 1000)
                }
                if (isPrayerModeActive) mainHandler.postDelayed(this, 1000)
            }
        }
        mainHandler.postDelayed(prayerTimerRunnable!!, 1000)
        
        autoScrollRunnable = object : Runnable {
            override fun run() {
                if (isPrayerModeActive && !isPrayerModePaused) {
                    quranRecyclerView.scrollBy(0, 2)
                    checkPrayerRuku()
                }
                if (isPrayerModeActive) mainHandler.postDelayed(this, scrollSpeedDelay)
            }
        }
        mainHandler.postDelayed(autoScrollRunnable!!, scrollSpeedDelay)
        Toast.makeText(this, "تم تشغيل وضع الصلاة. انقر على المؤقت للإيقاف المؤقت.", Toast.LENGTH_LONG).show()
    }

    private fun stopPrayerMode() {
        isPrayerModeActive = false
        prayerTimerWidget.visibility = View.GONE
        bottomBarLayout.visibility = View.VISIBLE
        prayerTimerRunnable?.let { mainHandler.removeCallbacks(it) }
        autoScrollRunnable?.let { mainHandler.removeCallbacks(it) }
    }

    private fun checkPrayerRuku() {
        val lm = quranRecyclerView.layoutManager as LinearLayoutManager
        val lastPos = lm.findLastVisibleItemPosition()
        if (lastPos == RecyclerView.NO_POSITION) return
        
        val prefs = getSharedPreferences("app", MODE_PRIVATE)
        val showArabic = prefs.getBoolean("show_arabic_ruku", false)
        val showEastern = prefs.getBoolean("show_eastern_ruku", false)
        
        val block = blockList.getOrNull(lastPos) ?: return
        for (v in block.verses) {
            val isRuku = (showArabic && v.rukooArDisplay.isNotBlank()) || (showEastern && v.rukooShDisplay.isNotBlank())
            if (isRuku && !countedRukus.contains(v.id)) {
                countedRukus.add(v.id)
                isPrayerModePaused = true
                prayerRukuCount++
                prayerTotalRukuText.text = "الركوعات: $prayerRukuCount"
                val card = prayerTimerWidget as com.google.android.material.card.MaterialCardView
                card.setCardBackgroundColor(Color.parseColor("#FFCDD2"))
                card.strokeColor = Color.parseColor("#FF8A80")
                return
            }
        }
    }

    private fun startTimer(minutes: Int) {
        countdownTimer?.cancel()
        countdownTimer = object : android.os.CountDownTimer(minutes * 60 * 1000L, 1000) {
            override fun onTick(ms: Long) {
                topBarTimerText.text = String.format(Locale.ENGLISH, "%02d:%02d", (ms / 60000), (ms % 60000) / 1000)
            }
            override fun onFinish() {
                topBarTimerText.text = "00:00"
                Toast.makeText(this@MainActivity, "انتهى وقتك المحدد", Toast.LENGTH_LONG).show()
            }
        }.start()
    }

    private fun showCustomTimerInput() {
        val input = EditText(this); input.inputType = android.text.InputType.TYPE_CLASS_NUMBER
        AlertDialog.Builder(this).setTitle("دقائق").setView(input).setPositiveButton("بدء") { _, _ ->
            startTimer(input.text.toString().toIntOrNull() ?: 0)
        }.show()
    }

    private fun setupExpandedBottomSheet(dialog: BottomSheetDialog) {
        dialog.setOnShowListener {
            val bottomSheet = (it as BottomSheetDialog).findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.let { sheet ->
                val behavior = com.google.android.material.bottomsheet.BottomSheetBehavior.from(sheet)
                behavior.state = com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
                behavior.skipCollapsed = true
            }
        }
    }

    fun openSearchDialog() {
        val dialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.dialog_modern_search, null)
        val themeColors = ThemeHelper.getThemeColors(this)
        view.setBackgroundColor(themeColors.bg)
        
        view.findViewById<TextView>(R.id.tvSearchDialogTitle)?.setTextColor(themeColors.txt)
        val et = view.findViewById<EditText>(R.id.etModernSearch)
        et?.setTextColor(themeColors.txt)
        et?.setHintTextColor(themeColors.txt)
        
        view.findViewById<com.google.android.material.card.MaterialCardView>(R.id.cvSearchShadow)?.setCardBackgroundColor(themeColors.shadow)
        view.findViewById<com.google.android.material.card.MaterialCardView>(R.id.cvSearchCard)?.apply {
            setCardBackgroundColor(themeColors.cardBg)
            strokeColor = themeColors.stroke
        }
        
        dialog.setContentView(view)
        setupExpandedBottomSheet(dialog)
        
        // et is already defined above
        val btnQuran = view.findViewById<Button>(R.id.btnSearchQuran)
        val btnTafseer = view.findViewById<Button>(R.id.btnSearchTafseer)
        val btnAzkar = view.findViewById<Button>(R.id.btnSearchAzkar)
        val rv = view.findViewById<RecyclerView>(R.id.rvSearchResults)
        rv.layoutManager = LinearLayoutManager(this)
        
        val incomingQuery = intent.getStringExtra("search_query")
        if (!incomingQuery.isNullOrEmpty()) {
            et.setText(incomingQuery)
            intent.removeExtra("search_query")
        }
        
        var currentSearchType = 0
        
        btnQuran.setOnClickListener {
            currentSearchType = 0
            performSearch(et.text.toString().trim(), currentSearchType, rv, dialog)
        }
        btnTafseer.setOnClickListener {
            currentSearchType = 1
            performSearch(et.text.toString().trim(), currentSearchType, rv, dialog)
        }
        btnAzkar?.setOnClickListener {
            currentSearchType = 2
            val q = et.text.toString().trim()
            if (q.isNotEmpty()) {
                val intentAzkar = android.content.Intent(this, AzkarActivity::class.java)
                intentAzkar.putExtra("search_query", q)
                startActivity(intentAzkar)
                dialog.dismiss()
            } else {
                Toast.makeText(this, "يرجى كتابة كلمة للبحث", Toast.LENGTH_SHORT).show()
            }
        }

        et.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (currentSearchType != 2) {
                    performSearch(s.toString().trim(), currentSearchType, rv, dialog)
                }
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })
        
        dialog.show()
    }

    private fun String.removeTashkeel(): String {
        return this.replace(Regex("<[^>]*>"), "")
            .replace(Regex("\\[[^\\]]*\\]"), "")
            .replace(Regex("[\\u064B-\\u065F\\u0670\\u06D6-\\u06ED]"), "")
            .replace(Regex("[أإآٱ]"), "ا")
            .replace("ة", "ه")
            .replace("ى", "ي")
    }

    private fun performSearch(q: String, type: Int, rv: RecyclerView, dialog: BottomSheetDialog) {
        if (q.isEmpty()) return
        
        val llEmpty = dialog.findViewById<View>(R.id.llSearchEmptyState)
        val tvEmpty = dialog.findViewById<TextView>(R.id.tvEmptyState)
        val pb = dialog.findViewById<ProgressBar>(R.id.pbSearchLoading)
        
        rv.visibility = View.GONE
        llEmpty?.visibility = View.GONE
        pb?.visibility = View.VISIBLE
        
        val qClean = q.removeTashkeel()
        
        Thread {
            val res = ArrayList<Triple<VerseModel, String, Int>>()
            for (i in blockList.indices) {
                for (v in blockList[i].verses) {
                    val match = if (type == 0) {
                        v.textClean.removeTashkeel().contains(qClean) || v.textTajweed.removeTashkeel().contains(qClean)
                    } else {
                        v.textClean.removeTashkeel().contains(qClean) || v.textTajweed.removeTashkeel().contains(qClean) || v.tafsirAr.contains(q)
                    }
                    if (match) {
                        res.add(Triple(v, v.textTajweed, i))
                    }
                }
            }
            
            runOnUiThread {
                pb?.visibility = View.GONE
                if (res.isNotEmpty()) {
                    rv.visibility = View.VISIBLE
                    rv.adapter = SearchAdapter(res, { verse, action ->
                        if (action == "audio") playInternalAudio(verse)
                        else if (action == "gemini") searchGemini(verse)
                    }, { blockIndex, verse ->
                        dialog.dismiss()
                        quranRecyclerView.post {
                            val lm = quranRecyclerView.layoutManager as LinearLayoutManager
                            lm.scrollToPositionWithOffset(blockIndex, 0)
                            quranAdapter.highlightedSura = verse.sura
                            quranAdapter.highlightedAya = verse.aya
                            quranAdapter.notifyDataSetChanged()
                            updateTopBar(blockList[blockIndex])
                        }
                    })
                } else {
                    llEmpty?.visibility = View.VISIBLE
                    tvEmpty?.text = "لا توجد نتائج لـ '$q'"
                }
            }
        }.start()
    }

    private fun showAboutDialog() {
        AlertDialog.Builder(this)            .setTitle("مساعدة")
            .setMessage("سبح بخشوع\n\n- (¼ ح): أرباع الأحزاب.\n- (ع): الركوعات (اكتمال المعنى).\n- المنزل: تقسيم القراءة على 7 أيام.\n\nتطبيق مصمم للتدبر والقراءة اليومية.")
            .setPositiveButton("حسناً", null).show()
    }

    private fun showVerseOptions(v: VerseModel) {
        val dialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.dialog_verse_options, null)
        dialog.setContentView(view)

        val preview = if (v.textClean.length > 50) v.textClean.take(50) + "..." else v.textClean
        view.findViewById<TextView>(R.id.tvVersePreviewTitle).text = preview
        
        val tvTranslation = view.findViewById<TextView>(R.id.tvVerseTranslation)
        val translationLang = getSharedPreferences("app", MODE_PRIVATE).getString("translation_lang", "none") ?: "none"
        if (translationLang != "none") {
            tvTranslation.visibility = View.VISIBLE
            val transText = when (translationLang) {
                "en" -> v.translationEn
                "id" -> v.translationId
                "ar" -> v.tafsirAr
                else -> ""
            }
            tvTranslation.text = transText
        } else {
            tvTranslation.visibility = View.GONE
        }

        view.findViewById<View>(R.id.btnOptionTafsir).setOnClickListener {
            dialog.dismiss()
            showTafsirDialog(v)
        }
        view.findViewById<View>(R.id.btnOptionAudioAyah).setOnClickListener {
            dialog.dismiss()
            playVerseAudioFromUrl(v) // New method to play audio_url
        }
        view.findViewById<View>(R.id.btnOptionAudioSurah).setOnClickListener {
            dialog.dismiss()
            showAudioOptionsDialog(v) // Online/Offline options for Surah
        }
        view.findViewById<View>(R.id.btnOptionGoogle).setOnClickListener {
            dialog.dismiss()
            openGoogle(v)
        }
        view.findViewById<View>(R.id.btnOptionAssistant).setOnClickListener {
            dialog.dismiss()
            searchGemini(v)
        }

        dialog.show()
    }

    private fun playVerseAudioFromUrl(v: VerseModel) {
        if (v.audioUrl.isNotEmpty()) {
            currentReciterId = "EveryAyah_DB"
            currentReciterUrlTemplate = v.audioUrl // Use the full URL
            currentAudioSura = v.sura
            currentAudioAya = v.aya
            playNextVerse()
        } else {
            // Fallback
            playInternalAudio(v)
        }
    }

    private fun showTafsirDialog(v: VerseModel) {
        val dialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.dialog_tafsir, null)
        dialog.setContentView(view)
        setupExpandedBottomSheet(dialog)

        val tvVerse = view.findViewById<TextView>(R.id.tvTafsirVerseText)
        val tvTafsir = view.findViewById<TextView>(R.id.tvTafsirText)
        val btnClose = view.findViewById<android.widget.Button>(R.id.btnCloseTafsir)

        tvVerse.text = v.textTajweed.replace(Regex("<[^>]*>"), "")
        tvTafsir.text = v.tafsirAr

        btnClose.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun searchGemini(v: VerseModel) {
        val apiKey = getSharedPreferences("app", MODE_PRIVATE).getString("api", "") ?: ""
        
        if (apiKey.isEmpty()) {
            showTafsirDialog(v)
            return
        }
        
        val dialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.dialog_gemini_response, null)
        dialog.setContentView(view)
        setupExpandedBottomSheet(dialog)
        val tvResponse = view.findViewById<TextView>(R.id.tvGeminiResponse)
        view.findViewById<Button>(R.id.btnCloseGemini).setOnClickListener { dialog.dismiss() }
        dialog.show()
        
        val prompt = "أريد تفسيراً وفوائد واستزادة لهذه الآية: سورة ${v.suraName} الآية ${v.aya} '${v.textTajweed}'. التفسير مقيد بالصحيح من تفسير أهل السنة والجماعة والمصادر الموثوقة وبأسلوب ميسر."
        tvResponse.text = "جاري الاتصال بالمساعد..."
        
        Thread {
            try {
                val text = GeminiHelper.queryGemini(apiKey, prompt)
                runOnUiThread { tvResponse.text = text }
            } catch (e: Exception) {
                runOnUiThread { 
                    tvResponse.text = "تعذر الحصول على الاستزادة (${e.message}).\n\nالتفسير المحلي:\n${v.tafsirAr}"
                }
            }
        }.start()
    }

    private fun openGoogle(v: VerseModel) {
        val q = "تفسير سورة ${v.suraName} آية ${v.aya}"
        startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://www.google.com/search?q=$q")))
    }

    private fun openAssistantDialog() {
        startActivity(android.content.Intent(this, AssistantActivity::class.java))
    }

    private fun openUnifiedIndex(tab: Int) {
        val dialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.dialog_unified_index, null)
        dialog.setContentView(view)
        setupExpandedBottomSheet(dialog)
        val rv = view.findViewById<RecyclerView>(R.id.indexRecyclerView)
        rv.layoutManager = LinearLayoutManager(this)
        
        val suras = ArrayList<IndexItem>()
        val juzs = ArrayList<IndexItem>()
        val seenSuras = HashSet<Int>()
        val seenJuz = HashSet<Int>()

        for (i in blockList.indices) {
            val b = blockList[i]
            if (b.verses.isNotEmpty()) {
                val v = b.verses[0]
                if (seenSuras.add(v.sura)) {
                    suras.add(IndexItem(v.sura, v.suraName, "صفحة ${b.pageNumber}", i))
                }
                if (seenJuz.add(v.juz)) {
                    juzs.add(IndexItem(v.juz, getJuzArabicName(v.juz), "صفحة ${b.pageNumber}", i))
                }
            }
        }

        fun update(items: List<IndexItem>) {
            rv.adapter = IndexAdapter(items) { 
                val target = it.targetPosition
                dialog.dismiss()
                // تأخير بسيط لضمان انتهاء حركة إغلاق النافذة وتحديث الشاشة
                quranRecyclerView.postDelayed({
                    val lm = quranRecyclerView.layoutManager as LinearLayoutManager
                    lm.scrollToPositionWithOffset(target, 0)
                    updateTopBar(blockList[target])
                }, 150)
            }
        }

        val tabs = view.findViewById<TabLayout>(R.id.indexTabLayout)
        tabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(t: TabLayout.Tab?) {
                update(if (t?.position == 0) suras else juzs)
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        tabs.getTabAt(tab)?.select()
        update(if (tab == 0) suras else juzs)
        dialog.show()
    }

    private var isPlayingAudio = false
    private var currentAudioSura = -1
    private var currentAudioAya = -1
    private var currentReciterId = ""
    private var currentReciterUrlTemplate = ""
    private var isDownloading = false

    private fun getAudioDir(reciterId: String, sura: Int): java.io.File {
        val dir = java.io.File(getExternalFilesDir(null), "audio/$reciterId/$sura")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    private fun getSuraAyahCount(suraNumber: Int): Int {
        val ayahs = intArrayOf(
            7, 286, 200, 176, 120, 165, 206, 75, 129, 109,
            123, 111, 43, 52, 99, 128, 111, 110, 98, 135,
            112, 78, 118, 64, 77, 227, 93, 88, 69, 60,
            34, 30, 73, 54, 45, 83, 182, 88, 75, 85,
            54, 53, 89, 59, 37, 35, 38, 29, 18, 45,
            60, 49, 62, 55, 78, 96, 29, 22, 24, 13,
            14, 11, 11, 18, 12, 12, 30, 52, 52, 44,
            28, 28, 20, 56, 40, 31, 50, 40, 46, 42,
            29, 19, 36, 25, 22, 17, 19, 26, 30, 20,
            15, 21, 11, 8, 8, 19, 5, 8, 8, 11,
            11, 8, 3, 9, 5, 4, 7, 3, 6, 3,
            5, 4, 5, 6
        )
        if (suraNumber in 1..114) {
            return ayahs[suraNumber - 1]
        }
        return 0
    }

    private fun playInternalAudio(v: VerseModel) {
        currentReciterId = "Alafasy"
        currentReciterUrlTemplate = "https://everyayah.com/data/Alafasy_128kbps/%03d%03d.mp3"
        currentAudioSura = v.sura
        currentAudioAya = v.aya
        playNextVerse()
    }

    private fun isSuraDownloaded(reciterId: String, sura: Int, totalAyas: Int): Boolean {
        val dir = getAudioDir(reciterId, sura)
        var count = 0
        for (i in 1..totalAyas) {
            val file = java.io.File(dir, String.format("%03d%03d.mp3", sura, i))
            if (file.exists() && file.length() > 0) count++
        }
        return count == totalAyas
    }

    private fun updateDownloadStatus(view: View, sura: Int, reciter: Triple<String, String, String>) {
        val tvStatus = view.findViewById<android.widget.TextView>(R.id.tvDownloadStatus)
        val btnDownload = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnDownloadSurah)
        val pbDownload = view.findViewById<android.widget.ProgressBar>(R.id.pbDownload)
        
        val totalAyas = getSuraAyahCount(sura)
        val downloaded = isSuraDownloaded(reciter.first, sura, totalAyas)
        
        if (downloaded) {
            tvStatus.text = "متاح للاستماع أوفلاين"
            tvStatus.setTextColor(Color.parseColor("#00A86B"))
            btnDownload.isEnabled = false
            btnDownload.text = "تم التحميل"
            btnDownload.setIconResource(0)
            pbDownload.visibility = View.GONE
        } else {
            tvStatus.text = "يحتاج إلى إنترنت للتحميل أو الاستماع"
            tvStatus.setTextColor(Color.parseColor("#888888"))
            if (!isDownloading) {
                btnDownload.isEnabled = true
                btnDownload.text = "تحميل السورة"
                btnDownload.setIconResource(android.R.drawable.stat_sys_download)
            }
        }
    }

    private fun showAudioOptionsDialog(v: VerseModel) {
        val dialog = com.google.android.material.bottomsheet.BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.dialog_audio_options, null)
        dialog.setContentView(view)

        view.findViewById<android.widget.TextView>(R.id.tvAudioSurahTitle)?.text = "خيارات الصوت لسورة ${v.suraName}"

        val reciters = listOf(
            Triple("Alafasy", "مشاري العفاسي", "https://everyayah.com/data/Alafasy_128kbps/%03d%03d.mp3"),
            Triple("Abdul_Basit", "عبد الباسط عبد الصمد", "https://everyayah.com/data/Abdul_Basit_Murattal_192kbps/%03d%03d.mp3")
        )

        var selectedReciter = reciters[0]
        val rgReciters = view.findViewById<android.widget.RadioGroup>(R.id.rgReciters)
        rgReciters.setOnCheckedChangeListener { _, checkedId ->
            selectedReciter = if (checkedId == R.id.rbAlafasy) reciters[0] else reciters[1]
            updateDownloadStatus(view, v.sura, selectedReciter)
        }

        updateDownloadStatus(view, v.sura, selectedReciter)

        view.findViewById<View>(R.id.btnPlaySurah)?.setOnClickListener {
            dialog.dismiss()
            currentReciterId = selectedReciter.first
            currentReciterUrlTemplate = selectedReciter.third
            currentAudioSura = v.sura
            currentAudioAya = v.aya
            playNextVerse()
        }

        view.findViewById<View>(R.id.btnDownloadSurah)?.setOnClickListener {
            val pbDownload = view.findViewById<android.widget.ProgressBar>(R.id.pbDownload)
            val btnDownload = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnDownloadSurah)
            val tvStatus = view.findViewById<android.widget.TextView>(R.id.tvDownloadStatus)
            downloadSuraVerses(v.sura, selectedReciter, pbDownload, btnDownload, tvStatus, view)
        }

        dialog.show()
    }

    private fun playNextVerse() {
        if (currentAudioSura == -1 || currentAudioAya == -1) return
        val totalAyas = getSuraAyahCount(currentAudioSura)
        if (currentAudioAya > totalAyas) {
            stopAudio()
            return
        }

        val verse = blockList.flatMap { it.verses }.find { it.sura == currentAudioSura && it.aya == currentAudioAya }
        val url = if (currentReciterId == "EveryAyah_DB" && verse != null && verse.audioUrl.isNotEmpty()) {
            verse.audioUrl
        } else {
            try {
                String.format(java.util.Locale.ENGLISH, currentReciterUrlTemplate, currentAudioSura, currentAudioAya)
            } catch (e: Exception) {
                currentReciterUrlTemplate
            }
        }
        
        val file = java.io.File(getAudioDir(currentReciterId, currentAudioSura), String.format("%03d%03d.mp3", currentAudioSura, currentAudioAya))
        
        val dataSource = if (file.exists() && file.length() > 0) file.absolutePath else url

        try {
            mediaPlayer?.release()
            mediaPlayer = android.media.MediaPlayer().apply {
                setAudioAttributes(android.media.AudioAttributes.Builder().setContentType(android.media.AudioAttributes.CONTENT_TYPE_MUSIC).build())
                setDataSource(dataSource)
                prepareAsync()
                setOnPreparedListener { 
                    it.start() 
                    setupMiniPlayerControls()
                    findViewById<View>(R.id.mini_player)?.visibility = View.VISIBLE
                    findViewById<android.widget.TextView>(R.id.tvMiniPlayerTitle)?.text = "سورة $currentAudioSura - آية $currentAudioAya"
                    findViewById<android.widget.ImageView>(R.id.btnMiniPlayPause)?.setImageResource(android.R.drawable.ic_media_pause)
                    
                    if (::quranAdapter.isInitialized) {
                        quranAdapter.highlightedSura = currentAudioSura
                        quranAdapter.highlightedAya = currentAudioAya
                        quranAdapter.notifyDataSetChanged()
                    }
                }
                setOnCompletionListener {
                    currentAudioAya++
                    playNextVerse()
                }
                setOnErrorListener { _, _, _ ->
                    currentAudioAya++
                    playNextVerse()
                    true
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun downloadSuraVerses(sura: Int, reciter: Triple<String, String, String>, pb: android.widget.ProgressBar, btn: android.widget.Button, tvStatus: android.widget.TextView, parentView: View) {
        if (isDownloading) return
        isDownloading = true
        val totalAyas = getSuraAyahCount(sura)
        val dir = getAudioDir(reciter.first, sura)
        
        runOnUiThread {
            pb.visibility = View.VISIBLE
            pb.max = totalAyas
            pb.progress = 0
            btn.isEnabled = false
            tvStatus.text = "جاري التحميل... 0 / $totalAyas"
        }

        Thread {
            try {
                for (i in 1..totalAyas) {
                    val file = java.io.File(dir, String.format("%03d%03d.mp3", sura, i))
                    if (!file.exists() || file.length() == 0L) {
                        val url = String.format(java.util.Locale.ENGLISH, reciter.third, sura, i)
                        val connection = java.net.URL(url).openConnection()
                        connection.connect()
                        connection.getInputStream().use { input ->
                            java.io.FileOutputStream(file).use { output ->
                                input.copyTo(output)
                            }
                        }
                    }
                    runOnUiThread {
                        pb.progress = i
                        tvStatus.text = "جاري التحميل... $i / $totalAyas"
                    }
                }
                runOnUiThread {
                    isDownloading = false
                    updateDownloadStatus(parentView, sura, reciter)
                    android.widget.Toast.makeText(this@MainActivity, "اكتمل تحميل السورة", android.widget.Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    isDownloading = false
                    tvStatus.text = "فشل التحميل، يرجى المحاولة لاحقاً"
                    btn.isEnabled = true
                }
            }
        }.start()
    }

    private fun setupMiniPlayerControls() {
        val btnPlayPause = findViewById<android.widget.ImageView>(R.id.btnMiniPlayPause)
        val btnStop = findViewById<android.widget.ImageView>(R.id.btnMiniStop)
        val btnRewind = findViewById<android.widget.ImageView>(R.id.btnMiniRewind)
        val btnForward = findViewById<android.widget.ImageView>(R.id.btnMiniForward)

        btnPlayPause?.setOnClickListener {
            mediaPlayer?.let { mp ->
                if (mp.isPlaying) {
                    mp.pause()
                    btnPlayPause.setImageResource(android.R.drawable.ic_media_play)
                } else {
                    mp.start()
                    btnPlayPause.setImageResource(android.R.drawable.ic_media_pause)
                }
            }
        }

        btnRewind?.setOnClickListener {
            if (currentAudioAya > 1) {
                currentAudioAya--
                playNextVerse()
            }
        }

        btnForward?.setOnClickListener {
            currentAudioAya++
            playNextVerse()
        }

        btnStop?.setOnClickListener {
            stopAudio()
        }
    }

    private fun stopAudio() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        currentAudioSura = -1
        currentAudioAya = -1
        findViewById<View>(R.id.mini_player)?.visibility = View.GONE
        if (::quranAdapter.isInitialized) {
            quranAdapter.highlightedSura = -1
            quranAdapter.highlightedAya = -1
            quranAdapter.notifyDataSetChanged()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopAudio()
    }
}
