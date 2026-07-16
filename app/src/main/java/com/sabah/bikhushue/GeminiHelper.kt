package com.sabah.bikhushue

import androidx.appcompat.app.AppCompatActivity
import android.content.Context
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog

object GeminiHelper {

    fun showAssistantDialog(activity: AppCompatActivity) {
        val dialog = BottomSheetDialog(activity)
        val view = activity.layoutInflater.inflate(R.layout.dialog_assistant, null)
        dialog.setContentView(view)
        
        dialog.setOnShowListener {
            val bottomSheet = (it as BottomSheetDialog).findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.let { sheet ->
                val behavior = BottomSheetBehavior.from(sheet)
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
                behavior.skipCollapsed = true
            }
        }
        
        val tvResponse = view.findViewById<TextView>(R.id.tvAssistantResponse)
        val etInput = view.findViewById<EditText>(R.id.etAssistantInput)
        val btnSend = view.findViewById<ImageView>(R.id.btnAssistantSend)
        val pbLoading = view.findViewById<ProgressBar>(R.id.pbAssistantLoading)
        
        val apiKey = activity.getSharedPreferences("app", Context.MODE_PRIVATE).getString("api", "") ?: ""
        var pendingExternalQuestion: String? = null
        
        btnSend.setOnClickListener {
            val q = etInput.text.toString().trim()
            if (q.isEmpty()) return@setOnClickListener
            
            etInput.setText("")
            tvResponse.append("\n\nأنت: $q")
            view.findViewById<ScrollView>(R.id.svAssistant)?.let { sv -> sv.post { sv.fullScroll(View.FOCUS_DOWN) } }
            pbLoading.visibility = View.VISIBLE
            
            if (q.contains("ليلي") || q.contains("داكن") || q.contains("اسود")) {
                activity.getSharedPreferences("app", Context.MODE_PRIVATE).edit().putString("bg_color", "#121212").putString("txt_color", "#E0E0E0").putString("bar_color", "#1E1E1E").apply()
                tvResponse.append("\n\nالمساعد: تم تفعيل الثيم الليلي بنجاح.")
                pbLoading.visibility = View.GONE
                activity.recreate()
                return@setOnClickListener
            } else if (q.contains("سماوي") || q.contains("ازرق")) {
                activity.getSharedPreferences("app", Context.MODE_PRIVATE).edit().putString("bg_color", "#E3F2FD").putString("txt_color", "#0D47A1").putString("bar_color", "#BBDEFB").apply()
                tvResponse.append("\n\nالمساعد: تم تفعيل الثيم السماوي بنجاح.")
                pbLoading.visibility = View.GONE
                activity.recreate()
                return@setOnClickListener
            } else if (q.contains("زمردي") || q.contains("اخضر")) {
                activity.getSharedPreferences("app", Context.MODE_PRIVATE).edit().putString("bg_color", "#E0F2F1").putString("txt_color", "#004D40").putString("bar_color", "#B2DFDB").apply()
                tvResponse.append("\n\nالمساعد: تم تفعيل الثيم الزمردي بنجاح.")
                pbLoading.visibility = View.GONE
                activity.recreate()
                return@setOnClickListener
            } else if (q.contains("كريمي") || q.contains("فاتح")) {
                activity.getSharedPreferences("app", Context.MODE_PRIVATE).edit().putString("bg_color", "#FDFBF7").putString("txt_color", "#212121").putString("bar_color", "#F9F6F0").apply()
                tvResponse.append("\n\nالمساعد: تم تفعيل الثيم الكريمي بنجاح.")
                pbLoading.visibility = View.GONE
                activity.recreate()
                return@setOnClickListener
            } else if (q.contains("زهري") || q.contains("وردي")) {
                activity.getSharedPreferences("app", Context.MODE_PRIVATE).edit().putString("bg_color", "#FCE4EC").putString("txt_color", "#880E4F").putString("bar_color", "#F8BBD0").apply()
                tvResponse.append("\n\nالمساعد: تم تفعيل الثيم الزهري بنجاح.")
                pbLoading.visibility = View.GONE
                activity.recreate()
                return@setOnClickListener
            } else if (q.contains("قمري") || q.contains("قمر")) {
                activity.getSharedPreferences("app", Context.MODE_PRIVATE).edit().putString("bg_color", "#455A64").putString("txt_color", "#FDF5E6").putString("bar_color", "#37474F").apply()
                tvResponse.append("\n\nالمساعد: تم تفعيل الثيم القمري بنجاح.")
                pbLoading.visibility = View.GONE
                activity.recreate()
                return@setOnClickListener
            } else if (q.contains("قرمزي") || q.contains("احمر")) {
                activity.getSharedPreferences("app", Context.MODE_PRIVATE).edit().putString("bg_color", "#FBF3F4").putString("txt_color", "#9C143A").putString("bar_color", "#F0D5DA").apply()
                tvResponse.append("\n\nالمساعد: تم تفعيل الثيم القرمزي بنجاح.")
                pbLoading.visibility = View.GONE
                activity.recreate()
                return@setOnClickListener
            } else if (q.contains("تجويد")) {
                val prefs = activity.getSharedPreferences("app", Context.MODE_PRIVATE)
                val currentState = prefs.getBoolean("tajweed_on", true)
                prefs.edit().putBoolean("tajweed_on", !currentState).apply()
                tvResponse.append("\n\nالمساعد: تم " + (if(!currentState) "تفعيل" else "إيقاف") + " التجويد.")
                pbLoading.visibility = View.GONE
                activity.recreate()
                return@setOnClickListener
            }
            
            if (pendingExternalQuestion != null) {
                if (q.contains("نعم") || q.contains("بالتأكيد") || q.contains("أجل") || q.contains("yes", true)) {
                    if (apiKey.isEmpty()) {
                        activity.runOnUiThread {
                            pbLoading.visibility = View.GONE
                            tvResponse.append("\n\nالمساعد: يرجى إضافة مفتاح جُمني في الإعدادات للبحث الخارجي.")
                            view.findViewById<ScrollView>(R.id.svAssistant)?.let { sv -> sv.post { sv.fullScroll(View.FOCUS_DOWN) } }
                        }
                        pendingExternalQuestion = null
                        return@setOnClickListener
                    }
                    val externalQ = pendingExternalQuestion!!
                    pendingExternalQuestion = null
                    
                    Thread {
                        val prompt = "أنت الآن الذكاء الاصطناعي لبرنامج قرآن ماستر. المطور هو وليد محمد. أجب عن السؤال التالي بلغة عربية واضحة ومختصرة. إذا كان السؤال عن آية معينة، اذكرها بالتشكيل وضع اسم السورة ورقم الآية بين قوسين 'هكذا'. السؤال: $externalQ"
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
                                        
                                    var displayText = text
                                    var needsRecreate = false
                                    
                                    if (text.contains("[THEME_DARK]")) {
                                        displayText = text.replace("[THEME_DARK]", "").trim()
                                        activity.getSharedPreferences("app", Context.MODE_PRIVATE).edit().putString("bg_color", "#121212").putString("txt_color", "#E0E0E0").putString("bar_color", "#1E1E1E").apply()
                                        needsRecreate = true
                                    } else if (text.contains("[THEME_LIGHT]")) {
                                        displayText = text.replace("[THEME_LIGHT]", "").trim()
                                        activity.getSharedPreferences("app", Context.MODE_PRIVATE).edit().putString("bg_color", "#FDFBF7").putString("txt_color", "#212121").putString("bar_color", "#F5E9D3").apply()
                                        needsRecreate = true
                                    } else if (text.contains("[THEME_BLUE]")) {
                                        displayText = text.replace("[THEME_BLUE]", "").trim()
                                        activity.getSharedPreferences("app", Context.MODE_PRIVATE).edit().putString("bg_color", "#E3F2FD").putString("txt_color", "#0D47A1").putString("bar_color", "#BBDEFB").apply()
                                        needsRecreate = true
                                    } else if (text.contains("[THEME_GREEN]")) {
                                        displayText = text.replace("[THEME_GREEN]", "").trim()
                                        activity.getSharedPreferences("app", Context.MODE_PRIVATE).edit().putString("bg_color", "#E0F2F1").putString("txt_color", "#004D40").putString("bar_color", "#B2DFDB").apply()
                                        needsRecreate = true
                                    } else if (text.contains("[THEME_PINK]")) {
                                        displayText = text.replace("[THEME_PINK]", "").trim()
                                        activity.getSharedPreferences("app", Context.MODE_PRIVATE).edit().putString("bg_color", "#FCE4EC").putString("txt_color", "#880E4F").putString("bar_color", "#F8BBD0").apply()
                                        needsRecreate = true
                                    } else if (text.contains("[THEME_LUNAR]")) {
                                        displayText = text.replace("[THEME_LUNAR]", "").trim()
                                        activity.getSharedPreferences("app", Context.MODE_PRIVATE).edit().putString("bg_color", "#455A64").putString("txt_color", "#FDF5E6").putString("bar_color", "#37474F").apply()
                                        needsRecreate = true
                                    } else if (text.contains("[THEME_CRIMSON]")) {
                                        displayText = text.replace("[THEME_CRIMSON]", "").trim()
                                        activity.getSharedPreferences("app", Context.MODE_PRIVATE).edit().putString("bg_color", "#FBF3F4").putString("txt_color", "#9C143A").putString("bar_color", "#F0D5DA").apply()
                                        needsRecreate = true
                                    } else if (text.contains("[TOGGLE_TAJWEED]")) {
                                        displayText = text.replace("[TOGGLE_TAJWEED]", "").trim()
                                        val prefs = activity.getSharedPreferences("app", Context.MODE_PRIVATE)
                                        val currentState = prefs.getBoolean("tajweed_on", true)
                                        prefs.edit().putBoolean("tajweed_on", !currentState).apply()
                                        needsRecreate = true
                                    }
                                    
                                    activity.runOnUiThread { 
                                        pbLoading.visibility = View.GONE
                                        tvResponse.append("\n\nالمساعد: $displayText") 
                                        view.findViewById<ScrollView>(R.id.svAssistant)?.let { sv -> sv.post { sv.fullScroll(View.FOCUS_DOWN) } }
                                    }
                                    
                                    if (needsRecreate) {
                                        activity.runOnUiThread { activity.recreate() }
                                    }
                                } catch (e: Exception) {
                                    activity.runOnUiThread { 
                                        pbLoading.visibility = View.GONE
                                        tvResponse.append("\n\nخطأ في قراءة الرد من الخادم.") 
                                    }
                                }
                            } else {
                                activity.runOnUiThread { 
                                    pbLoading.visibility = View.GONE
                                    tvResponse.append("\n\nخطأ في الاتصال بالخادم. تأكد من صحة مفتاح API وتوفر الإنترنت.") 
                                }
                            }
                        } catch (e: Exception) {
                            activity.runOnUiThread { 
                                pbLoading.visibility = View.GONE
                                tvResponse.append("\n\nحدث خطأ أثناء الاتصال. يرجى المحاولة لاحقاً.") 
                            }
                        }
                    }.start()
                    return@setOnClickListener
                } else {
                    pendingExternalQuestion = null
                    activity.runOnUiThread {
                        pbLoading.visibility = View.GONE
                        tvResponse.append("\n\nالمساعد: حسناً، تفضل بسؤالك عن القرآن.")
                        view.findViewById<ScrollView>(R.id.svAssistant)?.let { sv -> sv.post { sv.fullScroll(View.FOCUS_DOWN) } }
                    }
                    return@setOnClickListener
                }
            }
            
            Thread {
                try {
                    val cleanQuery = q.removeTashkeel().trim()
                    var localData = ""
                    // Smart Local NLP Parsing
                    var finalLocalResponse = ""
                    
                    // 1. Check for Surah/Ayah specific requests
                    val regexTafsir = Regex("(?:تفسير|معنى)\\s+(?:سورة|سوره)?\\s*([\\p{L}\\s]+?)\\s+(?:اية|آية|آيه)\\s*(\\d+)")
                    val matchTafsir = regexTafsir.find(cleanQuery)
                    
                    val regexCount = Regex("(?:كم|ما)\\s+(?:عدد)\\s+(?:ايات|آيات)\\s+(?:سورة|سوره)?\\s*([\\p{L}\\s]+)")
                    val matchCount = regexCount.find(cleanQuery)
                    
                    val dbHelper = DatabaseHelper(activity)
                    val suraNamesMap = dbHelper.getSuraNames()
                
                    if (matchTafsir != null) {
                        val suraNameRequested = matchTafsir.groupValues[1].trim()
                        val ayaNumber = matchTafsir.groupValues[2].toIntOrNull()
                        var foundSuraId = -1
                        for ((id, name) in suraNamesMap) {
                            if (name.replace("سورة ", "").removeTashkeel().contains(suraNameRequested)) {
                                foundSuraId = id
                                break
                            }
                        }
                        if (foundSuraId != -1 && ayaNumber != null) {
                            val verses = dbHelper.getAllQuranVerses().filter { it.sura == foundSuraId }
                            val targetVerse = verses.find { it.aya == ayaNumber }
                            if (targetVerse != null) {
                                val sName = suraNamesMap[foundSuraId]
                                finalLocalResponse = "سورة $sName آية $ayaNumber:\n{${targetVerse.textTajweed}}\n\nالتفسير:\n${targetVerse.tafsirJalalayn}\n\n"
                            }
                        }
                    } else if (matchCount != null) {
                        val suraNameRequested = matchCount.groupValues[1].trim()
                        var foundSuraId = -1
                        for ((id, name) in suraNamesMap) {
                            if (name.replace("سورة ", "").removeTashkeel().contains(suraNameRequested)) {
                                foundSuraId = id
                                break
                            }
                        }
                        if (foundSuraId != -1) {
                            val count = dbHelper.getAllQuranVerses().filter { it.sura == foundSuraId }.size
                            val sName = suraNamesMap[foundSuraId]
                            finalLocalResponse = "تحتوي سورة $sName على $count آيات.\n\n"
                        }
                    }

                    if (finalLocalResponse.isEmpty() && (cleanQuery.contains("دعاء") || cleanQuery.contains("ذكر") || cleanQuery.contains("فضل") || cleanQuery.contains("ماذا اقول"))) {
                        val azkarDbHelper = AzkarDatabaseHelper(activity)
                        val stopWords = setOf("ما", "هو", "كيف", "متى", "هل", "عن", "في", "من", "على", "الى", "ماذا", "اقول")
                        val keywords = cleanQuery.split("\\s+".toRegex()).filter { it !in stopWords && it.length > 2 }
                        
                        val allAzkar = azkarDbHelper.getCategories().flatMap { azkarDbHelper.getAzkarByCategory(it) }
                        
                        val scoredAzkar = allAzkar.map { zikr ->
                            var score = 0
                            val searchTarget = (zikr.title + " " + zikr.virtues + " " + zikr.category).removeTashkeel()
                            for (kw in keywords) {
                                if (searchTarget.contains(kw)) score += 3
                                if (zikr.text.removeTashkeel().contains(kw)) score += 1
                            }
                            Pair(zikr, score)
                        }.filter { it.second > 0 }.sortedByDescending { it.second }
                        
                        if (scoredAzkar.isNotEmpty()) {
                            finalLocalResponse += "وجدت لك هذه الأذكار المناسبة:\n"
                            for (i in 0 until minOf(2, scoredAzkar.size)) {
                                val zikr = scoredAzkar[i].first
                                finalLocalResponse += "- ${zikr.title} (${zikr.category}):\n${zikr.text}\n"
                                if (zikr.virtues.isNotEmpty()) {
                                    finalLocalResponse += "الفضل: ${zikr.virtues}\n"
                                }
                                finalLocalResponse += "\n"
                            }
                        }
                    }
                    
                    // Removed generic fallback search to avoid providing bad search results.
                    
                    if (finalLocalResponse.isNotEmpty()) {
                        pendingExternalQuestion = q
                        activity.runOnUiThread { 
                            pbLoading.visibility = View.GONE
                            tvResponse.append("\n\nالمساعد (نتائج ذكية بدون إنترنت):\n$finalLocalResponse\nهل ترغب في الاستزادة من المساعد الخارجي الذكي؟ (نعم / لا)") 
                            view.findViewById<ScrollView>(R.id.svAssistant)?.let { sv -> sv.post { sv.fullScroll(View.FOCUS_DOWN) } }
                        }
                        return@Thread
                    }
                } catch (e: Exception) {
                    activity.runOnUiThread { 
                        pbLoading.visibility = View.GONE
                        tvResponse.append("\n\n[عذراً، حدث خطأ أثناء المعالجة المحلية: ${e.message}]")
                    }
                }
                
                if (apiKey.isEmpty()) {
                    activity.runOnUiThread {
                        pbLoading.visibility = View.GONE
                        android.widget.Toast.makeText(activity, "إذا لم تضع المفتاح سيتم تحويلك إلى البحث المحلي الموحد", android.widget.Toast.LENGTH_LONG).show()
                        
                        if (activity is MainActivity) {
                            activity.openSearchDialog()
                            // Set search query if possible, handled below inside MainActivity
                        } else {
                            val intent = android.content.Intent(activity, MainActivity::class.java)
                            intent.putExtra("OPEN_SEARCH", true)
                            intent.putExtra("search_query", q)
                            activity.startActivity(intent)
                        }
                        dialog.dismiss()
                    }
                    return@Thread
                }

                val prompt = "أنت الآن الذكاء الاصطناعي لبرنامج قرآن ماستر. المطور هو وليد محمد. أجب عن السؤال التالي بلغة عربية واضحة ومختصرة. إذا كان السؤال عن آية معينة، اذكرها بالتشكيل وضع اسم السورة ورقم الآية بين قوسين 'هكذا'. ملاحظة مهمة جداً: إذا طلب المستخدم تغيير مظهر أو لون التطبيق إلى لون معين (ليلي، فاتح، سماوي، زمردي، زهري، قرمزي) أو طلب تفعيل/إيقاف التجويد، فاكتب في بداية ردك الكود المناسب من الأكواد التالية فقط بين قوسين مربعين: [THEME_DARK] للوضع الليلي، [THEME_LIGHT] للوضع الفاتح، [THEME_BLUE] للسماوي، [THEME_GREEN] للزمردي، [THEME_PINK] للزهري، [THEME_CRIMSON] للقرمزي، [TOGGLE_TAJWEED] لتغيير حالة التجويد. ثم اكتب ردك العادي بعد الكود لتؤكد للمستخدم أنه تم التنفيذ. السؤال: $q"
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
                            
                            var displayText = text
                            var needsRecreate = false
                            
                            if (text.contains("[THEME_DARK]")) {
                                displayText = text.replace("[THEME_DARK]", "").trim()
                                activity.getSharedPreferences("app", Context.MODE_PRIVATE).edit().putString("bg_color", "#121212").putString("txt_color", "#E0E0E0").putString("bar_color", "#1E1E1E").apply()
                                needsRecreate = true
                            } else if (text.contains("[THEME_LIGHT]")) {
                                displayText = text.replace("[THEME_LIGHT]", "").trim()
                                activity.getSharedPreferences("app", Context.MODE_PRIVATE).edit().putString("bg_color", "#FDFBF7").putString("txt_color", "#212121").putString("bar_color", "#F5E9D3").apply()
                                needsRecreate = true
                            } else if (text.contains("[THEME_BLUE]")) {
                                displayText = text.replace("[THEME_BLUE]", "").trim()
                                activity.getSharedPreferences("app", Context.MODE_PRIVATE).edit().putString("bg_color", "#E3F2FD").putString("txt_color", "#0D47A1").putString("bar_color", "#BBDEFB").apply()
                                needsRecreate = true
                            } else if (text.contains("[THEME_GREEN]")) {
                                displayText = text.replace("[THEME_GREEN]", "").trim()
                                activity.getSharedPreferences("app", Context.MODE_PRIVATE).edit().putString("bg_color", "#E0F2F1").putString("txt_color", "#004D40").putString("bar_color", "#B2DFDB").apply()
                                needsRecreate = true
                            } else if (text.contains("[THEME_PINK]")) {
                                displayText = text.replace("[THEME_PINK]", "").trim()
                                activity.getSharedPreferences("app", Context.MODE_PRIVATE).edit().putString("bg_color", "#FCE4EC").putString("txt_color", "#880E4F").putString("bar_color", "#F8BBD0").apply()
                                needsRecreate = true
                            } else if (text.contains("[THEME_LUNAR]")) {
                                displayText = text.replace("[THEME_LUNAR]", "").trim()
                                activity.getSharedPreferences("app", Context.MODE_PRIVATE).edit().putString("bg_color", "#455A64").putString("txt_color", "#FDF5E6").putString("bar_color", "#37474F").apply()
                                needsRecreate = true
                            } else if (text.contains("[THEME_CRIMSON]")) {
                                displayText = text.replace("[THEME_CRIMSON]", "").trim()
                                activity.getSharedPreferences("app", Context.MODE_PRIVATE).edit().putString("bg_color", "#FBF3F4").putString("txt_color", "#9C143A").putString("bar_color", "#F0D5DA").apply()
                                needsRecreate = true
                            } else if (text.contains("[TOGGLE_TAJWEED]")) {
                                displayText = text.replace("[TOGGLE_TAJWEED]", "").trim()
                                val prefs = activity.getSharedPreferences("app", Context.MODE_PRIVATE)
                                prefs.edit().putBoolean("tajweed_on", !prefs.getBoolean("tajweed_on", true)).apply()
                                needsRecreate = true
                            }
                            
                            activity.runOnUiThread { 
                                pbLoading.visibility = View.GONE
                                tvResponse.append("\n\nالمساعد: $displayText") 
                                view.findViewById<ScrollView>(R.id.svAssistant)?.let { sv -> sv.post { sv.fullScroll(View.FOCUS_DOWN) } }
                            }
                            
                            if (needsRecreate) {
                                activity.runOnUiThread { activity.recreate() }
                            }
                        } catch (e: Exception) {
                            activity.runOnUiThread { 
                                pbLoading.visibility = View.GONE
                                tvResponse.append("\n\nخطأ في قراءة الرد من الخادم.") 
                            }
                        }
                    } else {
                        activity.runOnUiThread { 
                            pbLoading.visibility = View.GONE
                            tvResponse.append("\n\nخطأ في الاتصال بالخادم. تأكد من صحة مفتاح API وتوفر الإنترنت.") 
                        }
                    }
                } catch (e: Exception) {
                    activity.runOnUiThread { 
                        pbLoading.visibility = View.GONE
                        tvResponse.append("\n\nحدث خطأ أثناء الاتصال. يرجى المحاولة لاحقاً.") 
                    }
                }
            }.start()
        }
        
        dialog.show()
    }
    
    private fun String.removeTashkeel(): String {
        return this.replace(Regex("[\\u064B-\\u065F\\u0670\\u06D6-\\u06ED]"), "")
            .replace(Regex("[أإآٱ]"), "ا")
            .replace("ة", "ه")
            .replace("ى", "ي")
    }
}
