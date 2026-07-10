package com.example.quranmaster

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

        BottomBarHelper.setupBottomBar(this, searchAction = { openSearchDialog() })

        findViewById<View>(R.id.btnSettings).setOnClickListener { 
            SettingsHelper.showSettingsDialog(this, onThemeChanged = {
                val prefs = getSharedPreferences("app", MODE_PRIVATE)
                val savedBg = prefs.getString("bg_color", "#F4ECD8") ?: "#F4ECD8"
                val savedTxt = prefs.getString("txt_color", "#000000") ?: "#000000"
                val savedBar = prefs.getString("bar_color", "#E6DCC8") ?: "#E6DCC8"
                applyTheme(savedBg, savedTxt, savedBar)
            }, onTajweedChanged = { isChecked ->
                if (::quranAdapter.isInitialized) {
                    quranAdapter.showTajweedColors = isChecked
                    quranAdapter.notifyDataSetChanged()
                }
            })
        }
        
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
        val bgColor = Color.parseColor(bg)
        val txtColor = Color.parseColor(txt)
        val barColor = Color.parseColor(bar)
        val subtleBorder = if (bg == "#121212") Color.parseColor("#2D2D2D") else Color.parseColor("#E4D7B4")

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

        findViewById<View>(R.id.topBarInnerLayout)?.setBackgroundColor(Color.TRANSPARENT)
        findViewById<View>(R.id.bottomBarInnerLayout)?.setBackgroundColor(Color.TRANSPARENT)

        topBarJuzText.setTextColor(txtColor)
        topBarSuraText.setTextColor(txtColor)
        topBarPageText.setTextColor(txtColor)
        topBarTimerText.setTextColor(txtColor)

        BottomBarHelper.setupBottomBar(this, searchAction = { openSearchDialog() })

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

    private fun openSearchDialog() {
        val dialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.dialog_modern_search, null)
        dialog.setContentView(view)
        setupExpandedBottomSheet(dialog)
        
        val et = view.findViewById<EditText>(R.id.etModernSearch)
        val btnQuran = view.findViewById<Button>(R.id.btnSearchQuran)
        val btnTafseer = view.findViewById<Button>(R.id.btnSearchTafseer)
        val rv = view.findViewById<RecyclerView>(R.id.rvSearchResults)
        rv.layoutManager = LinearLayoutManager(this)
        
        btnQuran.setOnClickListener { performSearch(et.text.toString().trim(), 0, rv, dialog) }
        btnTafseer.setOnClickListener { performSearch(et.text.toString().trim(), 1, rv, dialog) }
        
        dialog.show()
    }

    private fun String.removeTashkeel(): String {
        return this.replace(Regex("[\\u064B-\\u065F\\u0670\\u06D6-\\u06ED]"), "")
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
                        v.textTajweed.removeTashkeel().contains(qClean)
                    } else {
                        v.textTajweed.removeTashkeel().contains(qClean) || v.tafsirJalalayn.contains(q)
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
                            quranAdapter.highlightedVerseId = verse.id
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
        view.findViewById<View>(R.id.btnOptionAudio).setOnClickListener {
            dialog.dismiss()
            playInternalAudio(v)
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
        val dialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.dialog_assistant, null)
        dialog.setContentView(view)
        setupExpandedBottomSheet(dialog)
        
        val tvResponse = view.findViewById<TextView>(R.id.tvAssistantResponse)
        val etInput = view.findViewById<EditText>(R.id.etAssistantInput)
        val btnSend = view.findViewById<ImageView>(R.id.btnAssistantSend)
        val pbLoading = view.findViewById<ProgressBar>(R.id.pbAssistantLoading)
        
        val apiKey = getSharedPreferences("app", MODE_PRIVATE).getString("api", "") ?: ""
        var pendingExternalQuestion: String? = null
        
        btnSend.setOnClickListener {
            val q = etInput.text.toString().trim()
            if (q.isEmpty()) return@setOnClickListener
            
            etInput.setText("")
            tvResponse.append("\n\nأنت: $q")
            view.findViewById<ScrollView>(R.id.svAssistant)?.let { sv -> sv.post { sv.fullScroll(View.FOCUS_DOWN) } }
            pbLoading.visibility = View.VISIBLE
            
            if (pendingExternalQuestion != null) {
                if (q.contains("نعم") || q.contains("موافق") || q.contains("اجل") || q.contains("yes", true)) {
                    val externalQ = pendingExternalQuestion!!
                    pendingExternalQuestion = null
                    
                    Thread {
                        val prompt = "أجب على هذا السؤال من مصادر موثوقة وغير مختلف عليها. الإجابة يجب أن تكون مختصرة ودقيقة وصحيحة. إذا كان السؤال لا يتعلق بالقرآن والأذكار، أجب فقط بـ 'أنا مخصص للقرآن والأذكار فقط ولا يمكنني الإجابة على هذا السؤال'. السؤال: $externalQ"
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
                                    runOnUiThread { 
                                        pbLoading.visibility = View.GONE
                                        tvResponse.append("\n\nالمساعد: $text") 
                                        view.findViewById<ScrollView>(R.id.svAssistant)?.let { sv -> sv.post { sv.fullScroll(View.FOCUS_DOWN) } }
                                    }
                                } catch (e: Exception) {
                                    runOnUiThread { 
                                        pbLoading.visibility = View.GONE
                                        tvResponse.append("\n\nتعذر استخراج النص من الاستجابة.") 
                                    }
                                }
                            } else {
                                val error = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                                runOnUiThread { 
                                    pbLoading.visibility = View.GONE
                                    tvResponse.append("\n\nخطأ في الاتصال: ${conn.responseCode}\n$error") 
                                }
                            }
                        } catch (e: Exception) {
                            runOnUiThread { 
                                pbLoading.visibility = View.GONE
                                tvResponse.append("\n\nحدث خطأ: ${e.message}") 
                            }
                        }
                    }.start()
                } else {
                    pendingExternalQuestion = null
                    tvResponse.append("\n\nالمساعد: حسناً، تم الإلغاء. كيف يمكنني مساعدتك؟")
                    view.findViewById<ScrollView>(R.id.svAssistant)?.let { sv -> sv.post { sv.fullScroll(View.FOCUS_DOWN) } }
                    pbLoading.visibility = View.GONE
                }
                return@setOnClickListener
            }
            
            if (q.contains("ليلي") || q.contains("داكن") || q.contains("اسود")) {
                applyTheme("#121212", "#E0E0E0", "#1E1E1E")
                tvResponse.append("\n\nالمساعد: تم تفعيل الثيم الليلي بنجاح.")
                pbLoading.visibility = View.GONE
                return@setOnClickListener
            } else if (q.contains("سماوي") || q.contains("ازرق")) {
                applyTheme("#E3F2FD", "#0D47A1", "#BBDEFB")
                tvResponse.append("\n\nالمساعد: تم تفعيل الثيم السماوي بنجاح.")
                pbLoading.visibility = View.GONE

                return@setOnClickListener
            } else if (q.contains("زمردي") || q.contains("اخضر")) {
                applyTheme("#E0F2F1", "#004D40", "#B2DFDB")
                tvResponse.append("\n\nالمساعد: تم تفعيل الثيم الزمردي بنجاح.")
                pbLoading.visibility = View.GONE
                return@setOnClickListener
            } else if (q.contains("كريمي") || q.contains("فاتح")) {
                applyTheme("#F4ECD8", "#000000", "#E6DCC8")
                tvResponse.append("\n\nالمساعد: تم تفعيل الثيم الكريمي بنجاح.")
                pbLoading.visibility = View.GONE
                return@setOnClickListener
            } else if (q.contains("تجويد")) {
                val newState = !quranAdapter.showTajweedColors
                quranAdapter.showTajweedColors = newState
                quranAdapter.notifyDataSetChanged()
                getSharedPreferences("app", MODE_PRIVATE).edit().putBoolean("tajweed_on", newState).apply()
                tvResponse.append("\n\nالمساعد: تم " + (if(newState) "تفعيل" else "إيقاف") + " التجويد.")
                pbLoading.visibility = View.GONE
                return@setOnClickListener
            }
            
            Thread {
                if (apiKey.isEmpty()) {
                    var localAnswer = ""
                    val qClean = q.removeTashkeel()
                    for (i in blockList.indices) {
                        for (v in blockList[i].verses) {
                            if (v.textTajweed.removeTashkeel().contains(qClean) || v.tafsirJalalayn.contains(qClean)) {
                                localAnswer += "سورة ${v.suraName} آية ${v.aya}: ${v.textTajweed}\nالتفسير: ${v.tafsirJalalayn}\n\n"
                            }
                        }
                    }
                    runOnUiThread {
                        pbLoading.visibility = View.GONE
                        if (localAnswer.isNotEmpty()) {
                            tvResponse.append("\n\nالمساعد (بحث محلي مجاني): \n$localAnswer\n\n(للحصول على شرح أعمق، أضف مفتاح جُمني في الإعدادات).")
                            view.findViewById<ScrollView>(R.id.svAssistant)?.let { sv -> sv.post { sv.fullScroll(View.FOCUS_DOWN) } }
                        } else {
                            tvResponse.append("\n\nالمساعد: لم أجد إجابة في قاعدة البيانات المحلية. يرجى إضافة مفتاح جُمني في الإعدادات للبحث المتقدم.")
                            view.findViewById<ScrollView>(R.id.svAssistant)?.let { sv -> sv.post { sv.fullScroll(View.FOCUS_DOWN) } }
                        }
                    }
                    return@Thread
                }
                
                val qWords = q.removeTashkeel().split(" ").filter { it.length > 2 }
                var localData = ""
                var matches = 0
                for (i in blockList.indices) {
                    for (v in blockList[i].verses) {
                        var matchCount = 0
                        val vClean = v.textTajweed.removeTashkeel()
                        for (w in qWords) {
                            if (vClean.contains(w) || v.tafsirJalalayn.contains(w)) matchCount++
                        }
                        if (matchCount > 0) {
                            localData += "سورة ${v.suraName} آية ${v.aya}: ${v.textTajweed}\nالتفسير: ${v.tafsirJalalayn}\n\n"
                            matches++
                            if (matches >= 3) break
                        }
                    }
                    if (matches >= 3) break
                }
                
                if (localData.isNotEmpty()) {
                    pendingExternalQuestion = q
                    runOnUiThread { 
                        pbLoading.visibility = View.GONE
                        tvResponse.append("\n\nالمساعد (نتيجة محلية من قاعدة البيانات):\n$localData\nهل ترغب في الاستزادة من المساعد الذكي (جُمني)؟ (نعم / لا)") 
                        view.findViewById<ScrollView>(R.id.svAssistant)?.let { sv -> sv.post { sv.fullScroll(View.FOCUS_DOWN) } }
                    }
                    return@Thread
                }
                
                val prompt = "أجب على هذا السؤال من مصادر موثوقة وغير مختلف عليها. الإجابة يجب أن تكون مختصرة ودقيقة وصحيحة. إذا كان السؤال لا يتعلق بالقرآن والأذكار، أجب فقط بـ 'أنا مخصص للقرآن والأذكار فقط ولا يمكنني الإجابة على هذا السؤال'. السؤال: $q"
                
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
                            
                            runOnUiThread { 
                                pbLoading.visibility = View.GONE
                                tvResponse.append("\n\nالمساعد (جُمني): $text") 
                                view.findViewById<ScrollView>(R.id.svAssistant)?.let { sv -> sv.post { sv.fullScroll(View.FOCUS_DOWN) } }
                            }
                        } catch (e: Exception) {
                            runOnUiThread { 
                                pbLoading.visibility = View.GONE
                                tvResponse.append("\n\nتعذر استخراج النص من الاستجابة.") 
                            }
                        }
                    } else {
                        val error = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                        runOnUiThread { 
                            pbLoading.visibility = View.GONE
                            tvResponse.append("\n\nخطأ في الاتصال: ${conn.responseCode}\n$error") 
                        }
                    }
                } catch (e: Exception) {
                    runOnUiThread { 
                        pbLoading.visibility = View.GONE
                        tvResponse.append("\n\nحدث خطأ: ${e.message}") 
                    }
                }
            }.start()
        }
        
        dialog.show()
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

    private fun playInternalAudio(v: VerseModel) {
        val url = String.format(Locale.ENGLISH, "https://everyayah.com/data/Husary_64kbps/%03d%03d.mp3", v.sura, v.aya)
        
        Toast.makeText(this, "جاري الاتصال للتشغيل...", Toast.LENGTH_SHORT).show()
        
        try {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(AudioAttributes.Builder().setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
                setDataSource(url)
                prepareAsync()
                setOnPreparedListener { 
                    it.start() 
                    Toast.makeText(this@MainActivity, "جاري الاستماع: ${v.suraName} (${v.aya})", Toast.LENGTH_SHORT).show()
                }
                setOnErrorListener { mp, what, extra ->
                    Toast.makeText(this@MainActivity, "خطأ في تشغيل الصوت: $what, $extra", Toast.LENGTH_SHORT).show()
                    Log.e("QuranAudio", "MediaPlayer error: what=$what extra=$extra")
                    false
                }
            }
        } catch (e: Exception) {
            Toast.makeText(this, "تعذر تشغيل الصوت", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
    }
}
