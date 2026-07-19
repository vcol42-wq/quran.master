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

    fun queryGemini(apiKey: String, prompt: String): String {
        val cleanKey = apiKey.trim()
        val endpoints = listOf(
            "v1beta/models/gemini-1.5-flash",
            "v1beta/models/gemini-3.5-flash",
            "v1beta/models/gemini-flash-latest",
            "v1beta/models/gemini-3.1-pro-preview",
            "v1/models/gemini-1.5-flash",
            "v1beta/models/gemini-1.5-pro",
            "v1/models/gemini-1.5-pro",
            "v1beta/models/gemini-pro",
            "v1beta/models/gemini-1.0-pro"
        )
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
            put("systemInstruction", org.json.JSONObject().apply {
                put("parts", org.json.JSONArray().apply {
                    put(org.json.JSONObject().apply {
                        put("text", "أنت مساعد إسلامي متخصص في تطبيق للقرآن الكريم. ممنوع ذكر اسم المطور بتاتاً. إذا سُئلت عن المطور أو عن التطبيق، أجب فقط: 'تطبيق يساعدك على ذكر الله طُوّر للأجر والثواب'. لا تستخدم أي مقدمات، ادخل في الموضوع أو الإجابة مباشرة. يجب أن تكون جميع إجاباتك متوافقة مع الشريعة الإسلامية (القرآن والسنة). إذا سُئلت عن أمور غير دينية أو غير أخلاقية، اعتذر بلباقة. حافظ على أسلوب محترم ومفيد. إجاباتك يجب أن تكون دقيقة وموجزة. ملاحظة هامة: إذا طلب المستخدم تغيير لون أو مظهر التطبيق إلى لون معين (ليلي، فاتح، سماوي، زمردي، زهري، قرمزي) أو تفعيل/إيقاف التجويد، فاكتب في بداية ردك الكود المناسب من الأكواد التالية فقط بين قوسين مربعين: [THEME_DARK] للوضع الليلي، [THEME_LIGHT] للوضع الفاتح، [THEME_BLUE] للسماوي، [THEME_GREEN] للزمردي، [THEME_PINK] للزهري، [THEME_CRIMSON] للقرمزي، [TOGGLE_TAJWEED] لتغيير حالة التجويد. ثم أضف ردك العادي بعد الكود لتؤكد للمستخدم التنفيذ.")
                    })
                })
            })
            put("safetySettings", org.json.JSONArray().apply {
                put(org.json.JSONObject().apply { put("category", "HARM_CATEGORY_HARASSMENT"); put("threshold", "BLOCK_NONE") })
                put(org.json.JSONObject().apply { put("category", "HARM_CATEGORY_HATE_SPEECH"); put("threshold", "BLOCK_NONE") })
                put(org.json.JSONObject().apply { put("category", "HARM_CATEGORY_SEXUALLY_EXPLICIT"); put("threshold", "BLOCK_NONE") })
                put(org.json.JSONObject().apply { put("category", "HARM_CATEGORY_DANGEROUS_CONTENT"); put("threshold", "BLOCK_NONE") })
            })
        }.toString()
        
        var lastException: Exception? = null
        var lastErrorMsg = ""
        for (endpoint in endpoints) {
            try {
                val url = java.net.URL("https://generativelanguage.googleapis.com/$endpoint:generateContent?key=$cleanKey")
                val conn = url.openConnection() as java.net.HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.doOutput = true
                conn.outputStream.use { it.write(jsonBody.toByteArray(Charsets.UTF_8)) }
                
                if (conn.responseCode == 200) {
                    val response = conn.inputStream.bufferedReader().use { it.readText() }
                    val jsonResponse = org.json.JSONObject(response)
                    return jsonResponse.getJSONArray("candidates")
                        .getJSONObject(0)
                        .getJSONObject("content")
                        .getJSONArray("parts")
                        .getJSONObject(0)
                        .getString("text")
                } else {
                    val errorMsg = try { conn.errorStream?.bufferedReader()?.use { it.readText() } } catch(e:Exception){""}
                    if (conn.responseCode == 404) {
                        lastErrorMsg = "كود 404 للمسار $endpoint: $errorMsg"
                        continue
                    }
                    throw Exception("كود: ${conn.responseCode}, التفاصيل: $errorMsg")
                }
            } catch (e: Exception) {
                if (e.message?.contains("كود:") == true) throw e
                lastException = e
            }
        }
        val finalReason = if (lastErrorMsg.isNotEmpty()) lastErrorMsg else lastException?.message ?: "غير معروف"
        
        // If we hit 404s, let's try to query the list of available models for this key to see what is allowed.
        var availableModelsInfo = ""
        try {
            val listUrl = java.net.URL("https://generativelanguage.googleapis.com/v1beta/models?key=$cleanKey")
            val listConn = listUrl.openConnection() as java.net.HttpURLConnection
            listConn.requestMethod = "GET"
            if (listConn.responseCode == 200) {
                val listResponse = listConn.inputStream.bufferedReader().use { it.readText() }
                val jsonList = org.json.JSONObject(listResponse).getJSONArray("models")
                val names = mutableListOf<String>()
                for (i in 0 until jsonList.length()) {
                    names.add(jsonList.getJSONObject(i).getString("name"))
                }
                
                // Attempt to auto-discover a working conversational model
                val candidateModel = names.firstOrNull { name ->
                    val lower = name.lowercase()
                    (lower.contains("gemini") || lower.contains("antigravity")) && 
                    !lower.contains("embedding") && 
                    !lower.contains("aqa") && 
                    !lower.contains("imagen") && 
                    !lower.contains("veo") && 
                    !lower.contains("robotics")
                }
                
                if (candidateModel != null) {
                    val fallbackModelName = if (candidateModel.startsWith("models/")) candidateModel else "models/$candidateModel"
                    val fallbackEndpoint = "v1beta/$fallbackModelName"
                    try {
                        val url = java.net.URL("https://generativelanguage.googleapis.com/$fallbackEndpoint:generateContent?key=$cleanKey")
                        val conn = url.openConnection() as java.net.HttpURLConnection
                        conn.requestMethod = "POST"
                        conn.setRequestProperty("Content-Type", "application/json")
                        conn.doOutput = true
                        conn.outputStream.use { it.write(jsonBody.toByteArray(Charsets.UTF_8)) }
                        
                        if (conn.responseCode == 200) {
                            val response = conn.inputStream.bufferedReader().use { it.readText() }
                            val jsonResponse = org.json.JSONObject(response)
                            return jsonResponse.getJSONArray("candidates")
                                .getJSONObject(0)
                                .getJSONObject("content")
                                .getJSONArray("parts")
                                .getJSONObject(0)
                                .getString("text")
                        } else {
                            val err = try { conn.errorStream?.bufferedReader()?.use { it.readText() } } catch(e:Exception){""}
                            throw Exception("فشل الموديل المكتشف ($candidateModel). كود: ${conn.responseCode}, التفاصيل: $err")
                        }
                    } catch (e: Exception) {
                        throw Exception("تم اكتشاف موديل متاح ($candidateModel) لكن فشل استخدامه: ${e.message}")
                    }
                }
                
                availableModelsInfo = "\nالموديلات المتاحة لمفتاحك هي:\n" + names.joinToString("\n")
            } else {
                availableModelsInfo = "\n(لم نتمكن من جلب قائمة الموديلات المتاحة، كود الخطأ: ${listConn.responseCode})"
            }
        } catch (e: Exception) {
            availableModelsInfo = "\n(فشل جلب قائمة الموديلات المتاحة: ${e.message})"
        }

        throw Exception("جميع الموديلات المحددة غير متاحة لمفتاحك.$availableModelsInfo")
    }

    
    private fun String.removeTashkeel(): String {
        return this.replace(Regex("[\\u064B-\\u065F\\u0670\\u06D6-\\u06ED]"), "")
            .replace(Regex("[أإآٱ]"), "ا")
            .replace("ة", "ه")
            .replace("ى", "ي")
    }
}
