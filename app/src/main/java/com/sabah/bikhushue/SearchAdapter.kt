package com.sabah.bikhushue

import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class SearchAdapter(
    private val results: List<Triple<VerseModel, String, Int>>, 
    private val onAction: (VerseModel, String) -> Unit,
    private val onNavigate: (Int, VerseModel) -> Unit
) : RecyclerView.Adapter<SearchAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val suraInfo: TextView = view.findViewById(R.id.tvSearchSuraInfo)
        val verseText: TextView = view.findViewById(R.id.tvSearchVerseText)
        val tafseer: TextView = view.findViewById(R.id.tvSearchTafseer)
        val btnGemini: View = view.findViewById(R.id.btnSearchGemini)
        val btnAudio: View = view.findViewById(R.id.btnSearchPlayAudio)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_search_result, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = results[position]
        val verse = item.first
        
        holder.suraInfo.text = "سورة ${verse.suraName} - آية ${verse.aya}"
        
        val cleanText = verse.textTajweed.replace(Regex("color=['\"]?#[0-9a-fA-F]+['\"]?", RegexOption.IGNORE_CASE), "")
        holder.verseText.text = Html.fromHtml(cleanText, Html.FROM_HTML_MODE_LEGACY)
        
        holder.tafseer.text = verse.tafsirAr

        holder.btnGemini.setOnClickListener { onAction(verse, "gemini") }
        holder.btnAudio.setOnClickListener { onAction(verse, "audio") }
        
        holder.itemView.setOnClickListener { onNavigate(item.third, verse) }
    }

    override fun getItemCount() = results.size
}
