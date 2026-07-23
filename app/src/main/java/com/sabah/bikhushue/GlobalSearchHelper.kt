package com.sabah.bikhushue

import android.content.Intent
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog

object GlobalSearchHelper {
    private var allVerses: List<VerseModel>? = null

    private fun String.removeTashkeel(): String {
        return this.replace(Regex("<[^>]*>"), "")
            .replace(Regex("\\[[^\\]]*\\]"), "")
            .replace(Regex("[\\u064B-\\u065F\\u0670\\u06D6-\\u06ED]"), "")
            .replace(Regex("[أإآٱ]"), "ا")
            .replace("ة", "ه")
            .replace("ى", "ي")
    }

    fun show(activity: AppCompatActivity, initialQuery: String = "") {
        val dialog = BottomSheetDialog(activity)
        val view = activity.layoutInflater.inflate(R.layout.dialog_modern_search, null)
        dialog.setContentView(view)

        dialog.setOnShowListener {
            val bottomSheet = (it as BottomSheetDialog).findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.let { sheet ->
                val behavior = BottomSheetBehavior.from(sheet)
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
                behavior.skipCollapsed = true
            }
        }

        val et = view.findViewById<EditText>(R.id.etModernSearch)
        val btnQuran = view.findViewById<Button>(R.id.btnSearchQuran)
        val btnTafseer = view.findViewById<Button>(R.id.btnSearchTafseer)
        val btnAzkar = view.findViewById<Button>(R.id.btnSearchAzkar)
        val rv = view.findViewById<RecyclerView>(R.id.rvSearchResults)
        val llEmpty = view.findViewById<View>(R.id.llSearchEmptyState)
        val tvEmpty = view.findViewById<TextView>(R.id.tvEmptyState)
        val pb = view.findViewById<ProgressBar>(R.id.pbSearchLoading)

        rv.layoutManager = LinearLayoutManager(activity)

        if (initialQuery.isNotEmpty()) {
            et.setText(initialQuery)
        }

        var currentSearchType = 0

        if (allVerses == null) {
            pb.visibility = View.VISIBLE
            Thread {
                val dbHelper = DatabaseHelper(activity)
                dbHelper.checkAndCopyDatabase()
                allVerses = dbHelper.getAllQuranVerses()
                activity.runOnUiThread {
                    pb.visibility = View.GONE
                    if (et.text.toString().trim().isNotEmpty()) {
                        performSearch(et.text.toString().trim(), currentSearchType, activity, rv, llEmpty, tvEmpty, pb, dialog)
                    }
                }
            }.start()
        }

        btnQuran.setOnClickListener {
            currentSearchType = 0
            performSearch(et.text.toString().trim(), currentSearchType, activity, rv, llEmpty, tvEmpty, pb, dialog)
        }
        btnTafseer.setOnClickListener {
            currentSearchType = 1
            performSearch(et.text.toString().trim(), currentSearchType, activity, rv, llEmpty, tvEmpty, pb, dialog)
        }
        btnAzkar?.setOnClickListener {
            currentSearchType = 2
            val q = et.text.toString().trim()
            if (q.isNotEmpty()) {
                val intentAzkar = Intent(activity, AzkarActivity::class.java)
                intentAzkar.putExtra("search_query", q)
                activity.startActivity(intentAzkar)
                dialog.dismiss()
            } else {
                Toast.makeText(activity, "يرجى كتابة كلمة للبحث", Toast.LENGTH_SHORT).show()
            }
        }

        et.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (currentSearchType != 2) {
                    performSearch(s.toString().trim(), currentSearchType, activity, rv, llEmpty, tvEmpty, pb, dialog)
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        dialog.show()
    }

    private fun performSearch(q: String, type: Int, activity: AppCompatActivity, rv: RecyclerView, llEmpty: View?, tvEmpty: TextView?, pb: ProgressBar?, dialog: BottomSheetDialog) {
        if (q.isEmpty()) {
            rv.visibility = View.GONE
            llEmpty?.visibility = View.GONE
            return
        }

        val verses = allVerses
        if (verses == null) {
            pb?.visibility = View.VISIBLE
            return
        }

        rv.visibility = View.GONE
        llEmpty?.visibility = View.GONE
        pb?.visibility = View.VISIBLE

        val qClean = q.removeTashkeel()

        Thread {
            val res = ArrayList<Triple<VerseModel, String, Int>>()
            for (v in verses) {
                val match = if (type == 0) {
                    v.textClean.removeTashkeel().contains(qClean) || v.textTajweed.removeTashkeel().contains(qClean)
                } else {
                    v.textClean.removeTashkeel().contains(qClean) || v.textTajweed.removeTashkeel().contains(qClean) || v.tafsirAr.contains(q)
                }
                if (match) {
                    res.add(Triple(v, v.textTajweed, -1))
                }
            }

            activity.runOnUiThread {
                pb?.visibility = View.GONE
                if (res.isNotEmpty()) {
                    rv.visibility = View.VISIBLE
                    rv.adapter = SearchAdapter(res, { verse, action ->
                        val intent = Intent(activity, MainActivity::class.java)
                        intent.putExtra("SCROLL_TO_SURA", verse.sura)
                        intent.putExtra("SCROLL_TO_AYA", verse.aya)
                        if (action == "audio") intent.putExtra("ACTION_AUDIO", true)
                        else if (action == "gemini") intent.putExtra("ACTION_GEMINI", true)
                        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                        activity.startActivity(intent)
                        dialog.dismiss()
                    }, { _, verse ->
                        val intent = Intent(activity, MainActivity::class.java)
                        intent.putExtra("SCROLL_TO_SURA", verse.sura)
                        intent.putExtra("SCROLL_TO_AYA", verse.aya)
                        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                        activity.startActivity(intent)
                        dialog.dismiss()
                    })
                } else {
                    llEmpty?.visibility = View.VISIBLE
                    tvEmpty?.text = "لا توجد نتائج لـ '$q'"
                }
            }
        }.start()
    }
}
