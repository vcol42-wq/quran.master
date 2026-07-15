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
            
            if (pendingExternalQuestion != null) {
                if (q.contains("نعم") || q.contains("بالتأكيد") || q.contains("أجل") || q.contains("yes", true)) {
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
                                    activity.runOnUiThread { 
                                        pbLoading.visibility = View.GONE
                                        tvResponse.append("\n\nالمساعد: $text") 
                                        view.findViewById<ScrollView>(R.id.svAssistant)?.let { sv -> sv.post { sv.fullScroll(View.FOCUS_DOWN) } }
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
                    }
                    return@setOnClickListener
                }
            }
            
            Thread {
                val prompt = "أنت الآن الذكاء الاصطناعي لبرنامج قرآن ماستر. المطور هو وليد محمد. أجب عن السؤال التالي بلغة عربية واضحة ومختصرة. إذا كان السؤال عن آية معينة، اذكرها بالتشكيل وضع اسم السورة ورقم الآية بين قوسين 'هكذا'. السؤال: $q"
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
                            activity.runOnUiThread { 
                                pbLoading.visibility = View.GONE
                                tvResponse.append("\n\nالمساعد: $text") 
                                view.findViewById<ScrollView>(R.id.svAssistant)?.let { sv -> sv.post { sv.fullScroll(View.FOCUS_DOWN) } }
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
}
