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
    
    private lateinit var thinTopBar: View
    private lateinit var tvSuraRukoo: TextView
    private lateinit var tvSessionRukoo: TextView
    private lateinit var tvSessionTimer: TextView
    private lateinit var tvTotalRukoo: TextView

    private lateinit var quranInfoBar: View
    private lateinit var topBarJuzText: TextView
    private lateinit var topBarSuraText: TextView
    private lateinit var topBarPageText: TextView
    private lateinit var rukooProgressBar: ProgressBar

    private lateinit var tvScrollSpeedOverlay: TextView
    private lateinit var fixedBottomBar: com.google.android.material.card.MaterialCardView
    private lateinit var btnExitApp: ImageView
    private lateinit var btnAutoScrollToggle: ImageView
    private lateinit var tvRukooClock: TextView
    private lateinit var btnMainSearch: ImageView
    private lateinit var btnTopSettings: ImageView

    private lateinit var dbHelper: DatabaseHelper
    private var blockList = ArrayList<VerseBlock>()
    private lateinit var quranAdapter: QuranAdapter
    private var isBarsVisible = true
    private var countdownTimer: android.os.CountDownTimer? = null
    private lateinit var scaleGestureDetector: ScaleGestureDetector
    
    private var mediaPlayer: MediaPlayer? = null
    
    private val mainHandler = Handler(Looper.getMainLooper())
    private var isAutoScrolling = false
    private var scrollSpeedDelay = 20L
    private var lastTouchY = 0f
    private var isDraggingForSpeed = false

    private val topBarContainer: View get() = if (::quranInfoBar.isInitialized) quranInfoBar else mainRootLayout
    private val bottomBarLayout: View get() = if (::fixedBottomBar.isInitialized) fixedBottomBar else mainRootLayout
    private var isPrayerModeActive: Boolean
        get() = isAutoScrolling
        set(value) { isAutoScrolling = value }
    private var isPrayerModePaused = false
    private var immersiveHideRunnable: Runnable? = null

    private var sessionRukooCount = 0
    private var rukooClockSeconds = 0
    private var sessionTimerSeconds = 0
    private var currentCumulativeRuku = -1
    private var isRukooTimerRunning = false
    private var isSessionTimerRunning = false

    private val sessionTimerRunnable = object : Runnable {
        override fun run() {
            if (isSessionTimerRunning) {
                sessionTimerSeconds++
                val hrs = sessionTimerSeconds / 3600
                val mins = (sessionTimerSeconds % 3600) / 60
                val secs = sessionTimerSeconds % 60
                if (::tvSessionTimer.isInitialized) {
                    tvSessionTimer.text = String.format(Locale.US, "%02d:%02d:%02d", hrs, mins, secs)
                }
                mainHandler.postDelayed(this, 1000)
            }
        }
    }

    private val rukooClockRunnable = object : Runnable {
        override fun run() {
            if (isRukooTimerRunning) {
                rukooClockSeconds++
                val mins = rukooClockSeconds / 60
                val secs = rukooClockSeconds % 60
                if (::tvRukooClock.isInitialized) {
                    tvRukooClock.text = String.format(Locale.US, "%02d:%02d", mins, secs)
                }
                mainHandler.postDelayed(this, 1000)
            }
        }
    }

    private var autoScrollRunnable: Runnable? = null
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

        thinTopBar = findViewById(R.id.thinTopBar)
        tvSuraRukoo = findViewById(R.id.tvSuraRukoo)
        tvSessionRukoo = findViewById(R.id.tvSessionRukoo)
        tvSessionTimer = findViewById(R.id.tvSessionTimer)
        tvTotalRukoo = findViewById(R.id.tvTotalRukoo)

        quranInfoBar = findViewById(R.id.quranInfoBar)
        topBarJuzText = findViewById(R.id.topBarJuzText)
        topBarSuraText = findViewById(R.id.topBarSuraText)
        topBarPageText = findViewById(R.id.topBarPageText)
        rukooProgressBar = findViewById(R.id.rukooProgressBar)

        tvScrollSpeedOverlay = findViewById(R.id.tvScrollSpeedOverlay)
        fixedBottomBar = findViewById(R.id.fixedBottomBar)
        btnExitApp = findViewById(R.id.btnExitApp)
        btnAutoScrollToggle = findViewById(R.id.btnAutoScrollToggle)
        val btnSpeedUp = findViewById<ImageView>(R.id.btnSpeedUp)
        val btnSpeedDown = findViewById<ImageView>(R.id.btnSpeedDown)
        tvRukooClock = findViewById(R.id.tvRukooClock)
        btnMainSearch = findViewById(R.id.btnMainSearch)
        btnTopSettings = findViewById(R.id.btnTopSettings)

        btnExitApp.setOnClickListener { finish() }
        btnMainSearch.setOnClickListener { openSearchDialog() }

        btnTopSettings.setOnClickListener {
            startActivity(android.content.Intent(this, SettingsActivity::class.java))
        }

        btnAutoScrollToggle.setOnClickListener {
            if (isAutoScrolling) {
                stopAutoScroll()
            } else {
                startAutoScroll()
            }
        }

        btnSpeedUp?.setOnClickListener {
            // Decrease delay to increase speed
            scrollSpeedDelay = (scrollSpeedDelay - 2L).coerceAtLeast(4L)
            showSpeedOverlay()
        }

        btnSpeedDown?.setOnClickListener {
            // Increase delay to decrease speed
            scrollSpeedDelay = (scrollSpeedDelay + 2L).coerceAtMost(100L)
            showSpeedOverlay()
        }

        topBarSuraText.setOnClickListener { openUnifiedIndex(0) }
        topBarJuzText.setOnClickListener { openUnifiedIndex(1) }
        tvSessionTimer.setOnClickListener { showTimerDialog() }
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
            
            if (event.action == android.view.MotionEvent.ACTION_DOWN) {
                lastTouchY = event.y
                isDraggingForSpeed = true
            } else if (event.action == android.view.MotionEvent.ACTION_MOVE && isDraggingForSpeed && isAutoScrolling) {
                val deltaY = event.y - lastTouchY
                if (Math.abs(deltaY) > 4) { // Ultra-responsive threshold
                    val step = (Math.abs(deltaY) / 3).toInt().coerceAtLeast(1)
                    if (deltaY < 0) { // Dragged UP -> increase speed (smaller delay)
                        scrollSpeedDelay = (scrollSpeedDelay - step).coerceAtLeast(4L)
                    } else { // Dragged DOWN -> decrease speed (larger delay)
                        scrollSpeedDelay = (scrollSpeedDelay + step).coerceAtMost(100L)
                    }
                    lastTouchY = event.y
                    showSpeedOverlay()
                }
            } else if (event.action == android.view.MotionEvent.ACTION_UP || event.action == android.view.MotionEvent.ACTION_CANCEL) {
                isDraggingForSpeed = false
            }
            false
        }
    }

    private fun startAutoScroll() {
        isAutoScrolling = true
        isSessionTimerRunning = true
        isRukooTimerRunning = true

        if (::btnAutoScrollToggle.isInitialized) {
            btnAutoScrollToggle.setImageResource(R.drawable.ic_pause_scroll)
        }
        if (::quranInfoBar.isInitialized) {
            quranInfoBar.animate().alpha(0f).setDuration(300).withEndAction {
                quranInfoBar.visibility = View.GONE
            }.start()
        }

        mainHandler.removeCallbacks(sessionTimerRunnable)
        mainHandler.removeCallbacks(rukooClockRunnable)
        mainHandler.post(sessionTimerRunnable)
        mainHandler.post(rukooClockRunnable)

        autoScrollRunnable?.let { mainHandler.removeCallbacks(it) }
        autoScrollRunnable = object : Runnable {
            override fun run() {
                if (isAutoScrolling && ::quranRecyclerView.isInitialized) {
                    quranRecyclerView.scrollBy(0, 1)
                    mainHandler.postDelayed(this, scrollSpeedDelay)
                }
            }
        }
        mainHandler.post(autoScrollRunnable!!)
    }

    private fun stopAutoScroll() {
        isAutoScrolling = false
        isSessionTimerRunning = false
        isRukooTimerRunning = false

        if (::btnAutoScrollToggle.isInitialized) {
            btnAutoScrollToggle.setImageResource(R.drawable.ic_play_scroll)
        }
        autoScrollRunnable?.let { mainHandler.removeCallbacks(it) }
        mainHandler.removeCallbacks(sessionTimerRunnable)
        mainHandler.removeCallbacks(rukooClockRunnable)

        if (::quranInfoBar.isInitialized) {
            quranInfoBar.visibility = View.VISIBLE
            quranInfoBar.animate().alpha(1f).setDuration(300).start()
        }
    }

    private fun showSpeedOverlay() {
        if (::tvScrollSpeedOverlay.isInitialized) {
            val speedFactor = String.format(Locale.US, "%.1fx", 20.0 / scrollSpeedDelay.toDouble())
            tvScrollSpeedOverlay.text = "سرعة التمرير: $speedFactor"
            tvScrollSpeedOverlay.visibility = View.VISIBLE
            tvScrollSpeedOverlay.alpha = 1f
            mainHandler.removeCallbacks(hideSpeedOverlayRunnable)
            mainHandler.postDelayed(hideSpeedOverlayRunnable, 1200)
        }
    }

    private val hideSpeedOverlayRunnable = Runnable {
        if (::tvScrollSpeedOverlay.isInitialized) {
            tvScrollSpeedOverlay.animate().alpha(0f).setDuration(300).withEndAction {
                tvScrollSpeedOverlay.visibility = View.GONE
            }.start()
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

                        updateRukooBarsVisibility()
                    }
                }
            } catch (e: Exception) { Log.e("Quran", "Load: ${e.message}") }
        }.start()
    }

    private fun groupVersesByPage(verses: List<VerseModel>, suraNames: Map<Int, String>): ArrayList<VerseBlock> {
        val blocks = ArrayList<VerseBlock>()
        for (v in verses) {
            v.suraName = suraNames[v.sura] ?: "سورة ${v.sura}"
        }
        
        var currentBlockVerses = ArrayList<VerseModel>()
        var currentBlockPage = if (verses.isNotEmpty()) verses[0].page else -1
        
        for (v in verses) {
            val isNewSura = (v.sura != 1 && v.aya == 1) || (v.sura == 1 && v.aya == 2)
            
            if (v.page != currentBlockPage || isNewSura) {
                if (currentBlockVerses.isNotEmpty()) {
                    val firstV = currentBlockVerses[0]
                    // Determine showHeader: if it's the start of a surah
                    val showH = (firstV.sura != 1 && firstV.aya == 1) || (firstV.sura == 1 && firstV.aya == 2)
                    blocks.add(VerseBlock(ArrayList(currentBlockVerses), currentBlockPage, firstV.juz, firstV.sura, firstV.suraName, showH))
                }
                currentBlockPage = v.page
                currentBlockVerses = ArrayList()
            }
            currentBlockVerses.add(v)
        }
        
        if (currentBlockVerses.isNotEmpty()) {
            val firstV = currentBlockVerses[0]
            val showH = (firstV.sura != 1 && firstV.aya == 1) || (firstV.sura == 1 && firstV.aya == 2)
            blocks.add(VerseBlock(ArrayList(currentBlockVerses), currentBlockPage, firstV.juz, firstV.sura, firstV.suraName, showH))
        }
        return blocks
    }

    private fun updateTopBar(b: VerseBlock) {
        val firstV = b.verses.firstOrNull()
        if (firstV != null) {
            val rukuInSura = (QuranMetaData.ARABIC_RUKUS[firstV.sura]?.count { it < firstV.aya } ?: 0) + 1
            val cumulativeRuku = (1 until firstV.sura).sumOf { QuranMetaData.getTotalArabicRuku(it) } + rukuInSura

            if (currentCumulativeRuku != -1 && cumulativeRuku != currentCumulativeRuku) {
                sessionRukooCount++
                if (::tvSessionRukoo.isInitialized) {
                    tvSessionRukoo.text = "ع جلسة $sessionRukooCount"
                }
                if (::tvRukooClock.isInitialized) {
                    tvRukooClock.setTextColor(Color.parseColor("#00A86B"))
                    mainHandler.postDelayed({
                        if (::tvRukooClock.isInitialized) {
                            tvRukooClock.setTextColor(Color.parseColor("#3D2B1F"))
                        }
                    }, 2500)
                }
                rukooClockSeconds = 0
                stopTopBarPulsingAnimation()
            }
            currentCumulativeRuku = cumulativeRuku

            if (::tvSuraRukoo.isInitialized) {
                tvSuraRukoo.text = "ع سورة $rukuInSura"
            }
            if (::tvSessionRukoo.isInitialized) {
                tvSessionRukoo.text = "ع جلسة $sessionRukooCount"
            }
            if (::tvTotalRukoo.isInitialized) {
                tvTotalRukoo.text = "ع كلي $cumulativeRuku"
            }
            if (::topBarPageText.isInitialized) {
                topBarPageText.text = "ص ${b.pageNumber}"
            }
            if (::topBarSuraText.isInitialized) {
                topBarSuraText.text = "${b.suraNumber} ${b.suraName}"
            }
            if (::topBarJuzText.isInitialized) {
                topBarJuzText.text = "الجزء ${b.juzNumber}"
            }

            updateRukooProgressBar(firstV, rukuInSura)
        }
    }

    private var isTopBarPulsing = false

    private fun triggerTopBarPulsingAnimation() {
        if (isTopBarPulsing || !::thinTopBar.isInitialized) return
        isTopBarPulsing = true
        val anim = android.view.animation.AlphaAnimation(0.35f, 1.0f).apply {
            duration = 600
            repeatMode = android.view.animation.Animation.REVERSE
            repeatCount = android.view.animation.Animation.INFINITE
        }
        thinTopBar.startAnimation(anim)
    }

    private fun stopTopBarPulsingAnimation() {
        if (!isTopBarPulsing || !::thinTopBar.isInitialized) return
        isTopBarPulsing = false
        thinTopBar.clearAnimation()
    }

    override fun onResume() {
        super.onResume()
        if (::quranAdapter.isInitialized) {
            val prefs = getSharedPreferences("app", MODE_PRIVATE)
            val savedBg = prefs.getString("bg_color", "#121212") ?: "#121212"
            val savedTxt = prefs.getString("txt_color", "#000000") ?: "#000000"
            val savedBar = prefs.getString("bar_color", "#E6DCC8") ?: "#E6DCC8"
            applyTheme(savedBg, savedTxt, savedBar)

            val savedSepStr = prefs.getString("separator_type", "PAGE")
            quranAdapter.currentSeparator = SeparatorType.fromString(savedSepStr)
            quranAdapter.notifyDataSetChanged()
            updateRukooBarsVisibility()
        }
    }

    private fun updateRukooBarsVisibility() {
        val prefs = getSharedPreferences("app", MODE_PRIVATE)
        val savedSepStr = prefs.getString("separator_type", "PAGE")
        val isRukooActive = savedSepStr == "RUKOO_KHATMA_29" || savedSepStr == "RUKOO_KHATMA_30"

        if (::thinTopBar.isInitialized) {
            thinTopBar.visibility = if (isRukooActive && isBarsVisible) View.VISIBLE else View.GONE
        }
        if (::rukooProgressBar.isInitialized) {
            rukooProgressBar.visibility = if (isRukooActive && isBarsVisible) View.VISIBLE else View.GONE
        }
        if (::tvRukooClock.isInitialized) {
            tvRukooClock.visibility = if (isRukooActive) View.VISIBLE else View.GONE
        }
        if (::tvSessionRukoo.isInitialized) {
            tvSessionRukoo.text = if (isRukooActive) "ع جلسة $sessionRukooCount" else ""
            tvSessionRukoo.visibility = if (isRukooActive) View.VISIBLE else View.GONE
        }
        if (::quranRecyclerView.isInitialized) {
            val topPadding = if (isRukooActive) (75 * resources.displayMetrics.density).toInt() else (45 * resources.displayMetrics.density).toInt()
            quranRecyclerView.setPadding(quranRecyclerView.paddingLeft, topPadding, quranRecyclerView.paddingRight, quranRecyclerView.paddingBottom)
        }
    }

    private fun checkRukooPulseOnScreen() {
        if (!isBarsVisible || !::thinTopBar.isInitialized) return
        val prefs = getSharedPreferences("app", MODE_PRIVATE)
        val savedSepStr = prefs.getString("separator_type", "PAGE")
        val isRukooActive = savedSepStr == "RUKOO_KHATMA_29" || savedSepStr == "RUKOO_KHATMA_30"
        if (!isRukooActive) {
            stopTopBarPulsingAnimation()
            return
        }

        val layoutManager = quranRecyclerView.layoutManager as? LinearLayoutManager ?: return
        val firstPos = layoutManager.findFirstVisibleItemPosition()
        val lastPos = layoutManager.findLastVisibleItemPosition()

        var rukooInUpperQuarter = false
        val screenHeight = quranRecyclerView.height
        val topQuarterThreshold = screenHeight / 4

        if (firstPos != RecyclerView.NO_POSITION && lastPos != RecyclerView.NO_POSITION) {
            for (pos in firstPos..lastPos) {
                if (pos < blockList.size) {
                    val block = blockList[pos]
                    val hasRukooEnd = block.verses.any { v ->
                        val rTotal = if (savedSepStr == "RUKOO_KHATMA_30") v.rukooShTotal else v.rukooArTotal
                        rTotal > 0
                    }
                    if (hasRukooEnd) {
                        val view = layoutManager.findViewByPosition(pos)
                        if (view != null) {
                            val topY = view.top
                            if (topY in 0..topQuarterThreshold) {
                                rukooInUpperQuarter = true
                                break
                            }
                        }
                    }
                }
            }
        }

        if (rukooInUpperQuarter) {
            triggerTopBarPulsingAnimation()
        } else {
            stopTopBarPulsingAnimation()
        }
    }

    private fun updateRukooProgressBar(v: VerseModel, currentRukuInSura: Int) {
        val rukus = QuranMetaData.ARABIC_RUKUS[v.sura] ?: return
        val prevEndAya = if (currentRukuInSura > 1 && currentRukuInSura - 2 in rukus.indices) rukus[currentRukuInSura - 2] else 0
        val endAyaOfRuku = if (currentRukuInSura - 1 in rukus.indices) rukus[currentRukuInSura - 1] else 1
        
        val totalAyahsInRuku = (endAyaOfRuku - prevEndAya).coerceAtLeast(1)
        val ayahsCompleted = (v.aya - prevEndAya).coerceAtLeast(0)
        
        val progressPercent = ((ayahsCompleted.toFloat() / totalAyahsInRuku.toFloat()) * 100).toInt().coerceIn(0, 100)
        rukooProgressBar.progress = progressPercent

        checkRukooPulseOnScreen()
    }

    private fun getJuzArabicName(num: Int): String {
        val names = arrayOf("الأول","الثاني","الثالث","الرابع","الخامس","السادس","السابع","الثامن","التاسع","العاشر","الحادي عشر","الثاني عشر","الثالث عشر","الرابع عشر","الخامس عشر","السادس عشر","السابع عشر","الثامن عشر","التاسع عشر","العشرون","الحادي والعشرون","الثاني والعشرون","الثالث والعشرون","الرابع والعشرون","الخامس والعشرون","السادس والعشرون","السابع والعشرون","الثامن والعشرون","التاسع والعشرون","الثلاثون")
        return if (num in 1..30) names[num-1] else num.toString()
    }

    private fun toggleBarsVisibility() {
        if (resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE) {
            isBarsVisible = false
            if (::quranInfoBar.isInitialized) quranInfoBar.visibility = View.GONE
            return
        }
        isBarsVisible = !isBarsVisible
        if (::quranInfoBar.isInitialized) {
            quranInfoBar.animate()
                .alpha(if (isBarsVisible) 1f else 0f)
                .setDuration(300)
                .withEndAction {
                    quranInfoBar.visibility = if (isBarsVisible) View.VISIBLE else View.GONE
                }
                .start()
        }
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

        if (::thinTopBar.isInitialized) {
            thinTopBar.setBackgroundColor(if (isDarkMode) Color.parseColor("#1E1E1E") else Color.parseColor("#E6DCC8"))
        }
        if (::quranInfoBar.isInitialized) {
            quranInfoBar.setBackgroundColor(bgColor)
        }
        if (::fixedBottomBar.isInitialized) {
            fixedBottomBar.setCardBackgroundColor(barColor)
            fixedBottomBar.strokeColor = subtleBorder
            fixedBottomBar.strokeWidth = 1
        }

        // Apply theme to mini player
        val miniPlayer = findViewById<com.google.android.material.card.MaterialCardView>(R.id.mini_player)
        if (miniPlayer != null) {
            miniPlayer.setCardBackgroundColor(if (isDarkMode) Color.parseColor("#1A3025") else Color.parseColor("#FAF3E0"))
            miniPlayer.strokeColor = subtleBorder
            findViewById<TextView>(R.id.tvMiniPlayerTitle)?.setTextColor(txtColor)
            findViewById<ImageView>(R.id.btnMiniRewind)?.setColorFilter(txtColor)
            findViewById<ImageView>(R.id.btnMiniForward)?.setColorFilter(txtColor)
            // btnMiniPlayPause is inside another card, maybe handle it too if needed
        }

        if (::tvSuraRukoo.isInitialized) tvSuraRukoo.setTextColor(if (isDarkMode) Color.parseColor("#81C784") else Color.parseColor("#00A86B"))
        if (::tvSessionRukoo.isInitialized) tvSessionRukoo.setTextColor(if (isDarkMode) Color.parseColor("#81C784") else Color.parseColor("#00A86B"))
        if (::tvSessionTimer.isInitialized) tvSessionTimer.setTextColor(txtColor)
        if (::tvTotalRukoo.isInitialized) tvTotalRukoo.setTextColor(if (isDarkMode) Color.parseColor("#A5D6A7") else Color.parseColor("#1B5E20"))
        if (::topBarJuzText.isInitialized) topBarJuzText.setTextColor(txtColor)
        if (::topBarSuraText.isInitialized) topBarSuraText.setTextColor(txtColor)
        if (::topBarPageText.isInitialized) topBarPageText.setTextColor(txtColor)
        if (::btnExitApp.isInitialized) btnExitApp.setColorFilter(txtColor)
        if (::btnMainSearch.isInitialized) btnMainSearch.setColorFilter(txtColor)
        if (::btnTopSettings.isInitialized) btnTopSettings.setColorFilter(txtColor)
        if (::btnAutoScrollToggle.isInitialized) {
            val playIconColor = if (isDarkMode) Color.parseColor("#81C784") else (if (isAutoScrolling) Color.parseColor("#00A86B") else Color.parseColor("#2E5B42"))
            btnAutoScrollToggle.setColorFilter(playIconColor)
        }

        getSharedPreferences("app", MODE_PRIVATE).edit()
            .putString("bg_color", bg)
            .putString("txt_color", txt)
            .putString("bar_color", bar)
            .apply()
    }

    private fun showTimerDialog() {
        val options = arrayOf("5 دقائق", "10 دقائق", "15 دقيقة", "20 دقيقة", "الوقت المفتوح (تصاعدي)", "وضع التلاوة الذكي", "وقت مخصص")
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
        if (::tvSessionTimer.isInitialized) tvSessionTimer.text = "00:00"
        
        openTimerRunnable = object : Runnable {
            override fun run() {
                if (isOpenTimerActive) {
                    if (!isOpenTimerPaused) {
                        openTimerSeconds++
                        val mins = openTimerSeconds / 60
                        val secs = openTimerSeconds % 60
                        if (::tvSessionTimer.isInitialized) tvSessionTimer.text = String.format(Locale.ENGLISH, "%02d:%02d", mins, secs)
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
    }

    private fun scheduleImmersiveHide() {
        cancelImmersiveHide()
        immersiveHideRunnable = Runnable {
            if (isPrayerModeActive && !isPrayerModePaused) {
                hideBarsWithAnimation()
            }
        }
        mainHandler.postDelayed(immersiveHideRunnable!!, 3000)
    }

    private fun cancelImmersiveHide() {
        immersiveHideRunnable?.let { mainHandler.removeCallbacks(it) }
        immersiveHideRunnable = null
    }

    private fun hideBarsWithAnimation() {
        if (::quranInfoBar.isInitialized) {
            quranInfoBar.animate().alpha(0f).setDuration(400).withEndAction {
                quranInfoBar.visibility = View.GONE
            }.start()
        }
    }

    private fun setupPrayerTimerWidget() {
        // Updated for new persistent thin bar layout
    }

    private fun resumeNextRuku() {
        rukooClockSeconds = 0
    }

    private fun startPrayerMode() {
        startAutoScroll()
    }

    private fun stopPrayerMode() {
        stopAutoScroll()
    }

    private fun checkPrayerRuku() {
        // Handled dynamically in updateTopBar via cumulativeRuku
    }

    private fun startTimer(minutes: Int) {
        countdownTimer?.cancel()
        countdownTimer = object : android.os.CountDownTimer(minutes * 60 * 1000L, 1000) {
            override fun onTick(ms: Long) {
                if (::tvSessionTimer.isInitialized) {
                    tvSessionTimer.text = String.format(Locale.US, "%02d:%02d", (ms / 60000), (ms % 60000) / 1000)
                }
            }
            override fun onFinish() {
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

        // --- Conditional Logic for Asbab Nuzul ---
        val btnAsbab = view.findViewById<View>(R.id.btnOptionAsbabNuzul)
        val sepAsbab = view.findViewById<View>(R.id.sepAsbab)
        val asbab = if (v.asbabNuzul.isNotBlank()) v.asbabNuzul else dbHelper.getAsbabNuzul(v.sura, v.aya)
        if (asbab.isNullOrBlank()) {
            btnAsbab?.visibility = View.GONE
            sepAsbab?.visibility = View.GONE
        } else {
            btnAsbab?.visibility = View.VISIBLE
            sepAsbab?.visibility = View.VISIBLE
            btnAsbab?.setOnClickListener {
                dialog.dismiss()
                showAsbabNuzulDialog(v)
            }
        }

        // --- Conditional Logic for AI Assistant ---
        val btnAssistant = view.findViewById<View>(R.id.btnOptionAssistant)
        val sepAssistant = view.findViewById<View>(R.id.sepAssistant)
        val apiKey = getSharedPreferences("app", MODE_PRIVATE).getString("api", "") ?: ""
        if (apiKey.isBlank()) {
            btnAssistant?.visibility = View.GONE
            sepAssistant?.visibility = View.GONE
        } else {
            btnAssistant?.visibility = View.VISIBLE
            sepAssistant?.visibility = View.VISIBLE
            btnAssistant?.setOnClickListener {
                dialog.dismiss()
                searchGemini(v)
            }
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

    private fun showAsbabNuzulDialog(v: VerseModel) {
        val dialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.dialog_asbab_nuzul, null)
        dialog.setContentView(view)
        setupExpandedBottomSheet(dialog)

        val tvVerse = view.findViewById<TextView>(R.id.tvAsbabVerseText)
        val tvAsbab = view.findViewById<TextView>(R.id.tvAsbabText)
        val btnClose = view.findViewById<android.widget.Button>(R.id.btnCloseAsbab)

        tvVerse.text = v.textTajweed.replace(Regex("<[^>]*>"), "")

        val asbab = if (v.asbabNuzul.isNotBlank()) {
            v.asbabNuzul
        } else {
            dbHelper.getAsbabNuzul(v.sura, v.aya) ?: ""
        }

        if (asbab.isNotBlank()) {
            tvAsbab.text = asbab
        } else {
            tvAsbab.text = "نعتذر، لم يرد سبب نزول خاص لهذه الآية الكريمة في المصادر المعتمدة."
        }

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
