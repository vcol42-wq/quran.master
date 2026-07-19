package com.sabah.bikhushue

import android.graphics.Color
import android.text.Html
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class QuranAdapter(
    private val blocks: ArrayList<VerseBlock>,
    private val onBlockClickListener: () -> Unit,
    private val onVerseClickListener: (VerseModel) -> Unit
) : RecyclerView.Adapter<QuranAdapter.VerseViewHolder>() {

    var quranFontSize = 26f
    var highlightedSura = -1
    var highlightedAya = -1
    var showTajweedColors = true
    
    var currentBgColor = "#F4ECD8"
    var currentTextColor = "#000000"

    class VerseViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val quranVerseText: TextView = view.findViewById(R.id.quranVerseText)
        val sectionTitleText: TextView = view.findViewById(R.id.sectionTitleText)
        val suraHeaderLayout: View = view.findViewById(R.id.suraHeaderLayout)
        val basmalahCard: com.google.android.material.card.MaterialCardView? = view.findViewById(R.id.basmalahCard)
        val basmalahText: TextView = view.findViewById(R.id.basmalahText)
        val suraTypeAndAyahsText: TextView? = view.findViewById(R.id.suraTypeAndAyahsText)
        val suraNameAndNumberText: TextView? = view.findViewById(R.id.suraNameAndNumberText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VerseViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_verse, parent, false)
        return VerseViewHolder(view)
    }

    override fun onBindViewHolder(holder: VerseViewHolder, position: Int) {
        val block = blocks[position]
        val textColorInt = Color.parseColor(currentTextColor)

        holder.itemView.setBackgroundColor(Color.TRANSPARENT)
        holder.quranVerseText.setTextColor(textColorInt)
        holder.quranVerseText.textSize = quranFontSize
        holder.sectionTitleText.text = block.pageNumber.toString()

        if (block.showHeader) {
            holder.suraHeaderLayout.visibility = View.VISIBLE
            holder.basmalahText.setTextColor(textColorInt)
            

            holder.suraNameAndNumberText?.text = "سورة ${block.suraName.replace("سورة ", "")} (${block.suraNumber})"
            val suraInfo = getSuraInfo(block.suraNumber)
            holder.suraTypeAndAyahsText?.text = "${suraInfo.first} - ${suraInfo.second} آية"
            
            if (block.suraNumber == 9) {
                holder.basmalahText.text = ""
                holder.basmalahText.visibility = View.GONE
            } else {
                holder.basmalahText.text = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ"
                holder.basmalahText.visibility = View.VISIBLE
            }
            
            val isCardDark = currentBgColor == "#121212"
            val cardBg = if (isCardDark) Color.parseColor("#1A3025") else Color.parseColor("#E8F5E9")
            val cardStroke = if (isCardDark) Color.parseColor("#2E503F") else Color.parseColor("#A5D6A7")
            val cardText = if (isCardDark) Color.parseColor("#A5D6A7") else Color.parseColor("#1B5E20")
            
            holder.basmalahCard?.setCardBackgroundColor(cardBg)
            holder.basmalahCard?.strokeColor = cardStroke
            holder.suraNameAndNumberText?.setTextColor(cardText)
            holder.suraTypeAndAyahsText?.setTextColor(cardText)
        } else {
            holder.suraHeaderLayout.visibility = View.GONE
        }

        val ssb = android.text.SpannableStringBuilder()
        val verseRanges = mutableListOf<Triple<Int, Int, VerseModel>>()

        for (v in block.verses) {
            val start = ssb.length
            
            // Clean up any potential HTML tags just in case, since we use raw Uthmani text now
            val cleanText = v.textTajweed.trim().replace(Regex("<[^>]*>"), "")
            
            val verseSpannable = if (showTajweedColors) {
                val isDarkMode = currentBgColor.lowercase() == "#121212" || currentBgColor.lowercase() == "#2d2d2d"
                AutoTajweedParser.parse(cleanText, isDarkMode)
            } else {
                val ssbPlain = SpannableStringBuilder(cleanText)
                ssbPlain.setSpan(ForegroundColorSpan(Color.parseColor(currentTextColor)), 0, cleanText.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                ssbPlain
            }
            
            ssb.append(verseSpannable)
            
            if (v.isEndOfRuku && v.sura > 1) {
                val adjustedRuku = v.ruku - 1
                if (adjustedRuku > 0) {
                    val marker = " (ع-$adjustedRuku) "
                    val mStart = ssb.length
                    ssb.append(marker)
                    ssb.setSpan(ForegroundColorSpan(Color.parseColor("#1E9E49")), mStart, mStart + marker.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                    ssb.setSpan(RelativeSizeSpan(0.6f), mStart, mStart + marker.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
            }

            if (v.isEndOfHizb) {
                val hq = v.hizbQuarter
                val hizbNum = ((hq - 1) / 4) + 1
                val quarterPos = (hq - 1) % 4
                val frac = when (quarterPos) {
                    0 -> "¼"
                    1 -> "½"
                    2 -> "¾"
                    else -> ""
                }
                val marker = " $frac (ح) $hizbNum "
                val mStart = ssb.length
                ssb.append(marker)
                ssb.setSpan(ForegroundColorSpan(Color.parseColor("#D4AF37")), mStart, mStart + marker.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                ssb.setSpan(RelativeSizeSpan(0.6f), mStart, mStart + marker.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            }

            ssb.append(" ")
            val end = ssb.length
            verseRanges.add(Triple(start, end, v))
        }

        val plainText = ssb.toString()
        for (range in verseRanges) {
            if (range.third.sura == highlightedSura && range.third.aya == highlightedAya) {
                val isDarkMode = currentBgColor.lowercase() == "#121212" || currentBgColor.lowercase() == "#2d2d2d"
                val highlightColor = if (isDarkMode) Color.parseColor("#4D3D1F") else Color.parseColor("#FFF8E1")
                val vText = range.third.textTajweed.trim().replace(Regex("<[^>]*>"), "")
                val s = plainText.indexOf(vText, range.first)
                if (s != -1) {
                    ssb.setSpan(BackgroundColorSpan(highlightColor), s, s + vText.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
            }
        }

        holder.quranVerseText.text = ssb
        
        holder.quranVerseText.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                v.performClick()
                val tv = v as TextView
                val layout = tv.layout
                if (layout != null) {
                    val line = layout.getLineForVertical(event.y.toInt())
                    val offset = layout.getOffsetForHorizontal(line, event.x)
                    for (range in verseRanges) {
                        if (offset >= range.first && offset <= range.second) {
                            highlightedSura = range.third.sura
                            highlightedAya = range.third.aya
                            notifyDataSetChanged()
                            onVerseClickListener(range.third)
                            return@setOnTouchListener true
                        }
                    }
                }
                onBlockClickListener()
            }
            true
        }
    }



    private fun getSuraInfo(suraNumber: Int): Pair<String, Int> {
        val types = arrayOf(
            "مكية", "مدنية", "مدنية", "مدنية", "مدنية", "مكية", "مكية", "مدنية", "مدنية", "مكية",
            "مكية", "مكية", "مدنية", "مكية", "مكية", "مكية", "مكية", "مكية", "مكية", "مكية",
            "مكية", "مدنية", "مكية", "مدنية", "مكية", "مكية", "مكية", "مكية", "مكية", "مكية",
            "مكية", "مكية", "مدنية", "مكية", "مكية", "مكية", "مكية", "مكية", "مكية", "مكية",
            "مكية", "مكية", "مكية", "مكية", "مكية", "مكية", "مدنية", "مدنية", "مدنية", "مكية",
            "مكية", "مكية", "مكية", "مكية", "مدنية", "مكية", "مدنية", "مدنية", "مدنية", "مدنية",
            "مدنية", "مدنية", "مدنية", "مدنية", "مدنية", "مدنية", "مكية", "مكية", "مكية", "مكية",
            "مكية", "مكية", "مكية", "مكية", "مكية", "مدنية", "مكية", "مكية", "مكية", "مكية",
            "مكية", "مكية", "مكية", "مكية", "مكية", "مكية", "مكية", "مكية", "مكية", "مكية",
            "مكية", "مكية", "مكية", "مكية", "مكية", "مكية", "مكية", "مدنية", "مدنية", "مكية",
            "مكية", "مكية", "مكية", "مكية", "مكية", "مكية", "مكية", "مكية", "مكية", "مدنية",
            "مكية", "مكية", "مكية", "مكية"
        )
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
            return Pair(types[suraNumber - 1], ayahs[suraNumber - 1])
        }
        return Pair("مكية", 0)
    }

    override fun getItemCount(): Int = blocks.size
}
