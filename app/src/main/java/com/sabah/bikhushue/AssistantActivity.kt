package com.sabah.bikhushue

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AssistantActivity : AppCompatActivity() {

    private lateinit var rvChat: RecyclerView
    private lateinit var etInput: EditText
    private lateinit var btnSend: ImageView
    private lateinit var adapter: ChatAdapter
    private val messages = mutableListOf<ChatMessage>()
    private var isWaitingForResponse = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_assistant)

        findViewById<View>(R.id.llAssistantRoot)?.let { root ->
            androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
                val systemBars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars())
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
                insets
            }
        }

        val theme = ThemeHelper.getThemeColors(this)
        ThemeHelper.applySystemWindowsColors(this)
        findViewById<View>(R.id.llAssistantRoot)?.setBackgroundColor(theme.bg)

        BottomBarHelper.setupBottomBar(this, onThemeChanged = {
            recreate()
        })

        val tvTitle = findViewById<android.widget.TextView>(R.id.tvAssistantTitle)
        if (tvTitle != null) {
            tvTitle.setTextColor(theme.txt)
        }

        val cvTitleContainer = findViewById<com.google.android.material.card.MaterialCardView>(R.id.cvTitleContainer)
        if (cvTitleContainer != null) {
            cvTitleContainer.setCardBackgroundColor(theme.cardBg)
            cvTitleContainer.strokeColor = theme.bar
        }

        etInput = findViewById(R.id.etAssistantInput)
        etInput.setTextColor(theme.txt)
        etInput.setHintTextColor(theme.txt and 0x00FFFFFF or 0x66000000)

        rvChat = findViewById(R.id.rvChat)

        val cardInner = findViewById<com.google.android.material.card.MaterialCardView>(R.id.cvInputInner)
        if (cardInner != null) {
            cardInner.setCardBackgroundColor(theme.cardBg)
            cardInner.strokeColor = theme.bar
        }
        
        val cardShadow = findViewById<com.google.android.material.card.MaterialCardView>(R.id.cvInputShadow)
        if (cardShadow != null) {
            cardShadow.setCardBackgroundColor(theme.shadow)
        }

        btnSend = findViewById(R.id.btnSend)
        btnSend.backgroundTintList = android.content.res.ColorStateList.valueOf(theme.bar)
        btnSend.setColorFilter(theme.txt)

        val layoutManager = LinearLayoutManager(this)
        layoutManager.stackFromEnd = false
        rvChat.layoutManager = layoutManager
        
        adapter = ChatAdapter(messages)
        rvChat.adapter = adapter

        // Welcome message
        val welcomeMsg = "السلام عليكم انا مساعدك الذكي في العلوم الشرعية قران حديث فقه وممكن ان اساعدك في أدوات ضبط التطبيق. " +
                "من هنا تحصل على مفتاح جمني: https://aistudio.google.com/app/apikey"
        adapter.addMessage(ChatMessage(welcomeMsg, false))

        btnSend.setOnClickListener {
            val query = etInput.text.toString().trim()
            if (query.isNotEmpty() && !isWaitingForResponse) {
                sendMessage(query)
            }
        }
    }

    private fun sendMessage(query: String) {
        val sp = getSharedPreferences("app", Context.MODE_PRIVATE)
        val apiKey = GeminiHelper.getEffectiveApiKey(this)

        etInput.setText("")
        isWaitingForResponse = true
        adapter.addMessage(ChatMessage(query, true))
        rvChat.smoothScrollToPosition(messages.size - 1)
        
        // Add loading placeholder
        val loadingIndex = messages.size
        adapter.addMessage(ChatMessage("... يكتب", false))
        rvChat.smoothScrollToPosition(messages.size - 1)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val text = GeminiHelper.queryGemini(apiKey, query)
                var displayText = text
                var needsRecreate = false
                
                if (text.contains("[THEME_DARK]")) {
                    displayText = text.replace("[THEME_DARK]", "").trim()
                    sp.edit().putString("bg_color", "#121212").putString("txt_color", "#E0E0E0").putString("bar_color", "#1E1E1E").apply()
                    needsRecreate = true
                } else if (text.contains("[THEME_LIGHT]")) {
                    displayText = text.replace("[THEME_LIGHT]", "").trim()
                    sp.edit().putString("bg_color", "#FDFBF7").putString("txt_color", "#212121").putString("bar_color", "#F9F6F0").apply()
                    needsRecreate = true
                } else if (text.contains("[THEME_BLUE]")) {
                    displayText = text.replace("[THEME_BLUE]", "").trim()
                    sp.edit().putString("bg_color", "#E3F2FD").putString("txt_color", "#0D47A1").putString("bar_color", "#BBDEFB").apply()
                    needsRecreate = true
                } else if (text.contains("[THEME_GREEN]")) {
                    displayText = text.replace("[THEME_GREEN]", "").trim()
                    sp.edit().putString("bg_color", "#E0F2F1").putString("txt_color", "#004D40").putString("bar_color", "#B2DFDB").apply()
                    needsRecreate = true
                } else if (text.contains("[THEME_PINK]")) {
                    displayText = text.replace("[THEME_PINK]", "").trim()
                    sp.edit().putString("bg_color", "#FCE4EC").putString("txt_color", "#880E4F").putString("bar_color", "#F8BBD0").apply()
                    needsRecreate = true
                } else if (text.contains("[THEME_LUNAR]")) {
                    displayText = text.replace("[THEME_LUNAR]", "").trim()
                    sp.edit().putString("bg_color", "#455A64").putString("txt_color", "#FDF5E6").putString("bar_color", "#37474F").apply()
                    needsRecreate = true
                } else if (text.contains("[THEME_CRIMSON]")) {
                    displayText = text.replace("[THEME_CRIMSON]", "").trim()
                    sp.edit().putString("bg_color", "#FBF3F4").putString("txt_color", "#9C143A").putString("bar_color", "#F0D5DA").apply()
                    needsRecreate = true
                } else if (text.contains("[TOGGLE_TAJWEED]")) {
                    displayText = text.replace("[TOGGLE_TAJWEED]", "").trim()
                    needsRecreate = true
                }

                withContext(Dispatchers.Main) {
                    messages[loadingIndex] = ChatMessage(displayText, false)
                    adapter.notifyItemChanged(loadingIndex)
                    rvChat.smoothScrollToPosition(messages.size - 1)
                    isWaitingForResponse = false
                    
                    if (needsRecreate) {
                        val intent = android.content.Intent(this@AssistantActivity, HomeActivity::class.java)
                        intent.flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    messages[loadingIndex] = ChatMessage("عذراً، حدث خطأ: ${e.message}", false)
                    adapter.notifyItemChanged(loadingIndex)
                    rvChat.smoothScrollToPosition(messages.size - 1)
                    isWaitingForResponse = false
                }
            }
        }
    }
}
