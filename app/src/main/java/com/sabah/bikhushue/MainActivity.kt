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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN

        initViews()
        
        // تطبيق الثيم الأولي المحفوظ
        val prefs = getSharedPreferences("app", MODE_PRIVATE)
        val savedBg = prefs.getString("bg_color", "#F4ECD8") ?: "#F4ECD8"
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
    }
    
    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra("OPEN_SEARCH", false)) {
            openSearchDialog()
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

        val onThemeChangedAction = {
            val prefs = getSharedPreferences("app", MODE_PRIVATE)
            val savedBg = prefs.getString("bg_color", "#F4ECD8") ?: "#F4ECD8"
            val savedTxt = prefs.getString("txt_color", "#000000") ?: "#000000"
            val savedBar = prefs.getString("bar_color", "#E6DCC8") ?: "#E6DCC8"
            applyTheme(savedBg, savedTxt, savedBar)
        }
        val onTajweedChangedAction = { isChecked: Boolean ->
            if (::quranAdapter.isInitialized) {
                quranAdapter.showTajweedColors = isChecked
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
        topBarTimerText.setOnClickListener { showTimerDialog() }
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
                        val savedBg = prefs.getString("bg_color", "#F4ECD8") ?: "#F4ECD8"
                        val savedTxt = prefs.getString("txt_color", "#000000") ?: "#000000"
                        val savedBar = prefs.getString("bar_color", "#E6DCC8") ?: "#E6DCC8"
                        quranAdapter.showTajweedColors = prefs.getBoolean("tajweed_on", true)
                        
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
            if (i < verses.size - 1) {
                val n = verses[i + 1]
                v.isEndOfRuku = v.ruku != n.ruku
                v.isEndOfHizb = v.hizbQuarter != n.hizbQuarter
            }
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
        topBarPageText.text = b.pageNumber.toString()
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
        val txtColor = if (isDarkMode) Color.parseColor("#E0E0E0") else Color.parseColor(txt)
        val barColor = if (isDarkMode) Color.parseColor("#2D2D2D") else Color.parseColor(bar)
        val subtleBorder = if (isDarkMode) Color.parseColor("#555555") else Color.parseColor("#E4D7B4")

        mainRootLayout.setBackgroundColor(bgColor)
        quranRecyclerView.setBackgroundColor(Color.TRANSPARENT)
        
        window.statusBarColor = barColor
        window.navigationBarColor = barColor

        if (bg == "#121212") {
            window.decorView.systemUiVisibility = window.decorView.systemUiVisibility and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
        } else {
            window.decorView.systemUiVisibility = window.decorView.systemUiVisibility or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        }

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

        val onThemeChangedAction = {
            val prefs = getSharedPreferences("app", MODE_PRIVATE)
            val savedBg = prefs.getString("bg_color", "#F4ECD8") ?: "#F4ECD8"
            val savedTxt = prefs.getString("txt_color", "#000000") ?: "#000000"
            val savedBar = prefs.getString("bar_color", "#E6DCC8") ?: "#E6DCC8"
            applyTheme(savedBg, savedTxt, savedBar)
        }
        val onTajweedChangedAction = { isChecked: Boolean ->
            if (::quranAdapter.isInitialized) {
                quranAdapter.showTajweedColors = isChecked
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
        val options = arrayOf("5 دقائق", "10 دقائق", "15 دقيقة", "20 دقيقة", "وقت مخصص")
        AlertDialog.Builder(this).setTitle("المؤقت").setItems(options) { _, which ->
            if (which < 4) startTimer((which + 1) * 5) else showCustomTimerInput()
        }.show()
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
        dialog.setContentView(view)
        setupExpandedBottomSheet(dialog)
        
        val et = view.findViewById<EditText>(R.id.etModernSearch)
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
                        v.textClean.removeTashkeel().contains(qClean) || v.textTajweed.removeTashkeel().contains(qClean) || v.tafsirJalalayn.contains(q)
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

        val preview = if (v.textTajweed.length > 50) v.textTajweed.take(50) + "..." else v.textTajweed
        view.findViewById<TextView>(R.id.tvVersePreviewTitle).text = preview

        view.findViewById<View>(R.id.btnOptionTafsir).setOnClickListener {
            dialog.dismiss()
            showTafsirDialog(v)
        }
        view.findViewById<View>(R.id.btnOptionAudioAyah).setOnClickListener {
            dialog.dismiss()
            playInternalAudio(v) // Online streaming for Ayah
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

    private fun showTafsirDialog(v: VerseModel) {
        AlertDialog.Builder(this).setTitle("تفسير").setMessage(v.tafsirJalalayn).show()
    }

    private fun searchGemini(v: VerseModel) {
        val apiKey = getSharedPreferences("app", MODE_PRIVATE).getString("api", "") ?: ""
        
        val dialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.dialog_gemini_response, null)
        dialog.setContentView(view)
        setupExpandedBottomSheet(dialog)
        val tvResponse = view.findViewById<TextView>(R.id.tvGeminiResponse)
        view.findViewById<Button>(R.id.btnCloseGemini).setOnClickListener { dialog.dismiss() }
        dialog.show()

        if (apiKey.isEmpty()) {
            tvResponse.text = "التفسير المعتمد (الجلالين):\n\n${v.tafsirJalalayn}\n\n\n(ملاحظة: يمكنك إدخال مفتاح جُمني في الإعدادات لاحقاً للحصول على استزادة موسعة وفوائد إضافية)."
            return
        }
        
        val prompt = "أريد تفسيراً وفوائد واستزادة لهذه الآية: سورة ${v.suraName} الآية ${v.aya} '${v.textTajweed}'. التفسير مقيد بالصحيح من تفسير أهل السنة والجماعة والمصادر الموثوقة وبأسلوب ميسر."
        tvResponse.text = "جاري الاتصال بالمساعد..."
        
        Thread {
            try {
                val url = java.net.URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$apiKey")
                val conn = url.openConnection() as java.net.HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.doOutput = true
                
                val jsonBody = org.json.JSONObject().apply {
                    put("contents", org.json.JSONArray().apply {
                        put(org.json.JSONObject().apply {
                            put("parts", org.json.JSONArray().apply {
                                put(org.json.JSONObject().apply {
                                    put("text", prompt)
                                })
                            })
                        })
                    })
                }.toString()
                
                conn.outputStream.use { it.write(jsonBody.toByteArray(Charsets.UTF_8)) }
                
                if (conn.responseCode == 200) {
                    val response = conn.inputStream.bufferedReader().use { it.readText() }
                    try {
                        val jsonResponse = org.json.JSONObject(response)
                        val text = jsonResponse.getJSONArray("candidates")
                            .getJSONObject(0)
                            .getJSONObject("content")
                            .getJSONArray("parts")
                            .getJSONObject(0)
                            .getString("text")
                        runOnUiThread { tvResponse.text = text }
                    } catch (e: Exception) {
                        runOnUiThread { tvResponse.text = "تعذر استخراج النص من الاستجابة." }
                    }
                } else {
                    val error = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                    runOnUiThread { 
                        tvResponse.text = "لم يتمكن جُمني من الرد (تأكد من صحة المفتاح).\n\nالتفسير المحلي:\n${v.tafsirJalalayn}"
                    }
                }
            } catch (e: Exception) {
                runOnUiThread { 
                    tvResponse.text = "حدث خطأ في الاتصال.\n\nالتفسير المحلي:\n${v.tafsirJalalayn}"
                }
            }
        }.start()
    }

    private fun openGoogle(v: VerseModel) {
        val q = "تفسير سورة ${v.suraName} آية ${v.aya}"
        startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://www.google.com/search?q=$q")))
    }

    private fun openAssistantDialog() {
        GeminiHelper.showAssistantDialog(this)
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
                quranRecyclerView.post {
                    val lm = quranRecyclerView.layoutManager as LinearLayoutManager
                    lm.scrollToPositionWithOffset(target, 0)
                    updateTopBar(blockList[target])
                }
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

        val url = String.format(java.util.Locale.ENGLISH, currentReciterUrlTemplate, currentAudioSura, currentAudioAya)
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
