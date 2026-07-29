package com.sabah.bikhushue

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
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
    var showHizb = false
    var showManzil = false
    var currentSeparator: SeparatorType = SeparatorType.PAGE
    
    var currentBgColor = "#F4ECD8"
    var currentTextColor = "#000000"

    class VerseViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val topSeparatorContainer: ViewGroup? = view.findViewById(R.id.topSeparatorContainer)
        val quranVerseText: TextView? = view.findViewById(R.id.quranVerseText)
        val versesContainer: ViewGroup? = view.findViewById(R.id.versesContainer)
        val sectionTitleText: TextView = view.findViewById(R.id.sectionTitleText)
        val indicatorLeft: View? = view.findViewById(R.id.indicatorLeft)
        val indicatorRight: View? = view.findViewById(R.id.indicatorRight)
        val suraHeaderLayout: View = view.findViewById(R.id.suraHeaderLayout)
        val basmalahCard: com.google.android.material.card.MaterialCardView? = view.findViewById(R.id.basmalahCard)
        val basmalahText: TextView = view.findViewById(R.id.basmalahText)
        val suraTypeAndAyahsText: TextView? = view.findViewById(R.id.suraTypeAndAyahsText)
        val suraNameAndNumberText: TextView? = view.findViewById(R.id.suraNameAndNumberText)
        val khatmQuranCard: com.google.android.material.card.MaterialCardView? = view.findViewById(R.id.khatmQuranCard)
        val khatmTitleText: TextView? = view.findViewById(R.id.khatmTitleText)
        val khatmBodyText: TextView? = view.findViewById(R.id.khatmBodyText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VerseViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_verse, parent, false)
        return VerseViewHolder(view)
    }

    override fun onBindViewHolder(holder: VerseViewHolder, position: Int) {
        val block = blocks[position]
        val textColorInt = Color.parseColor(currentTextColor)
        val context = holder.itemView.context
        val isDarkMode = currentBgColor.lowercase() == "#121212" || currentBgColor.lowercase() == "#2d2d2d"

        val isHizbActive = currentSeparator == SeparatorType.HIZB
        val isRukooActive = currentSeparator == SeparatorType.RUKOO_KHATMA_29 || currentSeparator == SeparatorType.RUKOO_KHATMA_30
        val isKhatma30 = currentSeparator == SeparatorType.RUKOO_KHATMA_30

        holder.itemView.setBackgroundColor(Color.TRANSPARENT)

        // Reset
        holder.topSeparatorContainer?.removeAllViews()
        holder.topSeparatorContainer?.visibility = View.GONE
        
        holder.sectionTitleText.text = "ص ${block.pageNumber}"
        holder.sectionTitleText.setTextColor(textColorInt)
        holder.sectionTitleText.visibility = View.VISIBLE

        // Page side indicators logic
        val pageNum = block.pageNumber
        holder.indicatorLeft?.visibility = View.INVISIBLE
        holder.indicatorRight?.visibility = View.INVISIBLE

        if (pageNum >= 3) {
            if (pageNum % 2 != 0) {
                // فردي -> يمين المستخدم -> أخضر
                holder.indicatorRight?.visibility = View.VISIBLE
            } else {
                // زوجي -> يسار المستخدم -> أحمر
                holder.indicatorLeft?.visibility = View.VISIBLE
            }
        }

        if (block.showHeader) {
            holder.suraHeaderLayout.visibility = View.VISIBLE
            holder.basmalahText.setTextColor(textColorInt)
            holder.suraNameAndNumberText?.text = "۞ سورة ${block.suraName.replace("سورة ", "")} (${block.suraNumber}) ۞"
            val suraInfo = getSuraInfo(block.suraNumber)
            val mAbbr = getManzilAbbr(block.suraNumber)
            holder.suraTypeAndAyahsText?.textSize = 10f 
            holder.suraTypeAndAyahsText?.text = "${suraInfo.first} - ${suraInfo.second} آية - ($mAbbr)"
            if (block.suraNumber == 1) {
                val mSep = createManzilSeparator(context, isDarkMode, "المنزل الأول", "م ف 1")
                holder.topSeparatorContainer?.addView(mSep)
                holder.topSeparatorContainer?.visibility = View.VISIBLE
            }
            if (block.suraNumber == 1 || block.suraNumber == 9) {
                holder.basmalahText.text = ""
                holder.basmalahText.visibility = View.GONE
            } else {
                holder.basmalahText.text = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ"
                holder.basmalahText.visibility = View.VISIBLE
            }
            val cardBg = if (isDarkMode) Color.parseColor("#1A3025") else Color.parseColor("#FAF3E0")
            val cardStroke = if (isDarkMode) Color.parseColor("#2E503F") else Color.parseColor("#D2B48C")
            val cardText = if (isDarkMode) Color.parseColor("#A5D6A7") else Color.parseColor("#1B5E20")
            holder.basmalahCard?.setCardBackgroundColor(cardBg)
            holder.basmalahCard?.strokeColor = cardStroke
            holder.suraNameAndNumberText?.setTextColor(cardText)
            holder.suraTypeAndAyahsText?.setTextColor(cardText)
        } else {
            holder.suraHeaderLayout.visibility = View.GONE
        }

        if (position == blocks.size - 1) {
            holder.khatmQuranCard?.visibility = View.VISIBLE
            val khatmBg = if (isDarkMode) Color.parseColor("#1A3025") else Color.parseColor("#FAF3E0")
            val khatmStroke = if (isDarkMode) Color.parseColor("#2E503F") else Color.parseColor("#D2B48C")
            val khatmTitleColor = if (isDarkMode) Color.parseColor("#81C784") else Color.parseColor("#1B5E20")
            val khatmBodyColor = if (isDarkMode) Color.parseColor("#E0E0E0") else Color.parseColor("#3D2B1F")
            holder.khatmQuranCard?.setCardBackgroundColor(khatmBg)
            holder.khatmQuranCard?.strokeColor = khatmStroke
            holder.khatmTitleText?.setTextColor(khatmTitleColor)
            holder.khatmBodyText?.setTextColor(khatmBodyColor)
        } else {
            holder.khatmQuranCard?.visibility = View.GONE
        }

        val container = holder.versesContainer
        if (container == null) return
        container.removeAllViews()

        val firstVerse = block.verses.firstOrNull()
        if (firstVerse != null && isHizbActive && firstVerse.hizbQuarterDisplay.isNotBlank() && block.showHeader) {
            val (fraction, hizbNum) = getHizbValues(firstVerse)
            val topSepView = createSeparatorView(context, isDarkMode, "ح", fraction, hizbNum)
            holder.topSeparatorContainer?.addView(topSepView)
            holder.topSeparatorContainer?.visibility = View.VISIBLE
        }

        fun renderChunk(chunk: List<VerseModel>) {
            if (chunk.isEmpty()) return
            val ssb = SpannableStringBuilder()
            val verseRanges = mutableListOf<Triple<Int, Int, VerseModel>>()

            for (v in chunk) {
                val start = ssb.length
                if ((isHizbActive || showHizb) && v.hizbQuarterDisplay.isNotBlank()) {
                    val hMarker = " ۞ " + formatHizbDisplay(v.hizbQuarterDisplay) + " ۞ "
                    val hStart = ssb.length
                    ssb.append(hMarker)
                    val hColor = if (isDarkMode) Color.parseColor("#FFD700") else Color.parseColor("#00A86B")
                    ssb.setSpan(ForegroundColorSpan(hColor), hStart, hStart + hMarker.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                    ssb.setSpan(RelativeSizeSpan(0.75f), hStart, hStart + hMarker.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                }

                if (v.manzilDisplay.isNotBlank()) {
                    val marker = " " + v.manzilDisplay + " "
                    val mStart = ssb.length
                    ssb.append(marker)
                    ssb.setSpan(ForegroundColorSpan(Color.parseColor("#9C27B0")), mStart, mStart + marker.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                    ssb.setSpan(RelativeSizeSpan(0.65f), mStart, mStart + marker.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                }

                // Ensure the Ayah end symbol is directly attached to the text without spaces
                val rawVerseText = v.textTajweed.replace(Regex("[\\s\\u00A0\\u2000-\\u200F\\u202F\\uFEFF]+([\\u06DD۝])"), "$1")
                val verseStartInSsb = ssb.length
                ssb.append(rawVerseText)

                val circleMatch = verseCircleRegex.find(rawVerseText)
                if (circleMatch != null) {
                    val circleStart = verseStartInSsb + circleMatch.range.first
                    val circleEnd = verseStartInSsb + circleMatch.range.last + 1
                    
                    val isEvery5 = v.aya % 5 == 0
                    
                    // Colors for normal ayahs
                    var circleColor = if (isDarkMode) Color.parseColor("#81C784") else Color.parseColor("#4CAF50")
                    var bgFill = if (isDarkMode) Color.parseColor("#1B382B") else Color.parseColor("#E8F5E9")
                    
                    // Distinctive Gold color for every 5th ayah
                    if (isEvery5) {
                        circleColor = Color.parseColor("#FFD700") // Gold
                        bgFill = if (isDarkMode) Color.parseColor("#3E3200") else Color.parseColor("#FFF9C4")
                    }
                    
                    ssb.setSpan(
                        VerseCircleSpan(textColorInt, circleColor, bgFill),
                        circleStart,
                        circleEnd,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }

                if (isRukooActive) {
                    val rTotal = if (isKhatma30) v.rukooShTotal else v.rukooArTotal
                    if (rTotal > 0) {
                        val rukuMarker = "(ع)"
                        val rStart = ssb.length
                        ssb.append(rukuMarker)
                        val rColor = if (isDarkMode) Color.parseColor("#FFD700") else Color.parseColor("#00A86B")
                        ssb.setSpan(ForegroundColorSpan(rColor), rStart, rStart + rukuMarker.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                        ssb.setSpan(RelativeSizeSpan(0.7f), rStart, rStart + rukuMarker.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                    }
                }
                
                // Reduced spacing between ayahs
                ssb.append("\u2004") // One-third em space (narrower than regular space)
                
                val end = ssb.length
                verseRanges.add(Triple(start, end, v))
            }

            val highlightColor = when {
                currentBgColor.equals("#581825", ignoreCase = true) || currentBgColor.equals("#4A0E17", ignoreCase = true) || currentBgColor.equals("#800020", ignoreCase = true) -> Color.parseColor("#7E1E30")
                isDarkMode -> Color.parseColor("#4D3D1F")
                else -> Color.parseColor("#FFF8E1")
            }

            val plainText = ssb.toString()
            for (range in verseRanges) {
                if (range.third.sura == highlightedSura && range.third.aya == highlightedAya) {
                    val vText = range.third.textTajweed
                    val s = plainText.indexOf(vText, range.first)
                    if (s != -1) {
                        ssb.setSpan(android.text.style.BackgroundColorSpan(highlightColor), s, s + vText.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                        if (isDarkMode || highlightColor == Color.parseColor("#7E1E30")) {
                            ssb.setSpan(ForegroundColorSpan(Color.WHITE), s, s + vText.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                        }
                    }
                }
            }

            lateinit var tv: TextView
            val gestureDetector = androidx.core.view.GestureDetectorCompat(context, object : android.view.GestureDetector.SimpleOnGestureListener() {
                override fun onSingleTapConfirmed(e: MotionEvent): Boolean { tv.performClick(); onBlockClickListener(); return true }
                override fun onLongPress(e: MotionEvent) {
                    val layout = tv.layout ?: return
                    val line = layout.getLineForVertical(e.y.toInt())
                    val offset = layout.getOffsetForHorizontal(line, e.x)
                    for (range in verseRanges) {
                        if (offset >= range.first && offset <= range.second) {
                            highlightedSura = range.third.sura; highlightedAya = range.third.aya; notifyDataSetChanged(); onVerseClickListener(range.third); return
                        }
                    }
                }
            })

            tv = TextView(context).apply {
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                gravity = android.view.Gravity.CENTER
                textSize = quranFontSize; setTextColor(textColorInt); setLineSpacing(0f, 1.0f); includeFontPadding = false
                try { typeface = androidx.core.content.res.ResourcesCompat.getFont(context, R.font.amiri_quran) } catch (e: Exception) {}
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) { 
                    @android.annotation.SuppressLint("WrongConstant")
                    justificationMode = 1 // JUSTIFICATION_MODE_INTER_WORD
                }
                text = ssb
                setOnTouchListener { _, event -> gestureDetector.onTouchEvent(event); true }
            }
            container.addView(tv)
        }
        renderChunk(block.verses)
    }

    private fun getHizbValues(v: VerseModel): Pair<String, String> {
        val rawStr = v.hizbQuarterDisplay
        val fraction = when { rawStr.contains("¼") -> "¼"; rawStr.contains("½") -> "½"; rawStr.contains("¾") -> "¾"; else -> "" }
        val hizbNum = if (v.hizbQuarter > 0) ((v.hizbQuarter - 1) / 4) + 1 else 1
        return Pair(fraction, hizbNum.toString())
    }

    private fun createSeparatorView(context: android.content.Context, isDarkMode: Boolean, symbol: String, rightValue: String, leftValue: String): View {
        val sepView = LayoutInflater.from(context).inflate(R.layout.view_rukoo_separator, null, false)
        val tvRight = sepView.findViewById<TextView>(R.id.tvRightValue); val tvSymbol = sepView.findViewById<TextView>(R.id.tvSymbol); val tvLeft = sepView.findViewById<TextView>(R.id.tvLeftValue); val cardRoot = sepView.findViewById<com.google.android.material.card.MaterialCardView>(R.id.rukooSeparatorRoot)
        tvRight?.text = rightValue; tvSymbol?.text = symbol; tvLeft?.text = leftValue
        val cardBg = if (isDarkMode) Color.parseColor("#1A3025") else Color.parseColor("#FAF3E0")
        val cardStroke = if (isDarkMode) Color.parseColor("#2E503F") else Color.parseColor("#D2B48C")
        val accentColor = if (isDarkMode) Color.parseColor("#81C784") else Color.parseColor("#00A86B")
        cardRoot?.setCardBackgroundColor(cardBg); cardRoot?.strokeColor = cardStroke; tvRight?.setTextColor(accentColor); tvSymbol?.setTextColor(accentColor); tvLeft?.setTextColor(accentColor)
        return sepView
    }

    private fun getSuraInfo(suraNumber: Int): Pair<String, Int> {
        val types = arrayOf("مكية", "مدنية", "مدنية", "مدنية", "مدنية", "مكية", "مكية", "مدنية", "مدنية", "مكية", "مكية", "مكية", "مدنية", "مكية", "مكية", "مكية", "مكية", "مكية", "مكية", "مكية", "مكية", "مدنية", "مكية", "مدنية", "مكية", "مكية", "مكية", "مكية", "مكية", "مكية", "مكية", "مكية", "مدنية", "مكية", "مكية", "مكية", "مكية", "مكية", "مكية", "مكية", "مكية", "مكية", "مكية", "مكية", "مكية", "مكية", "مدنية", "مدنية", "مدنية", "مكية", "مكية", "مكية", "مكية", "مكية", "مدنية", "مكية", "مدنية", "مدنية", "مدنية", "مدنية", "مدنية", "مدنية", "مدنية", "مدنية", "مدنية", "مدنية", "مكية", "مكية", "مكية", "مكية", "مكية", "مكية", "مكية", "مكية", "مكية", "مدنية", "مكية", "مكية", "مكية", "مكية", "مكية", "مكية", "مكية", "مكية", "مكية", "مكية", "مكية", "مكية", "مكية", "مكية", "مكية", "مكية", "مكية", "مكية", "مكية", "مكية", "مكية", "مدنية", "مدنية", "مكية", "مكية", "مكية", "مكية", "مكية", "مكية", "مكية", "مكية", "مكية", "مكية", "مدنية", "مكية", "مكية", "مكية", "مكية")
        val ayahs = intArrayOf(7, 286, 200, 176, 120, 165, 206, 75, 129, 109, 123, 111, 43, 52, 99, 128, 111, 110, 98, 135, 112, 78, 118, 64, 77, 227, 93, 88, 69, 60, 34, 30, 73, 54, 45, 83, 182, 88, 75, 85, 54, 53, 89, 59, 37, 35, 38, 29, 18, 45, 60, 49, 62, 55, 78, 96, 29, 22, 24, 13, 14, 11, 11, 18, 12, 12, 30, 52, 52, 44, 28, 28, 20, 56, 40, 31, 50, 40, 46, 42, 29, 19, 36, 25, 22, 17, 19, 26, 30, 20, 15, 21, 11, 8, 8, 19, 5, 8, 8, 11, 11, 8, 3, 9, 5, 4, 7, 3, 6, 3, 5, 4, 5, 6)
        return if (suraNumber in 1..114) Pair(types[suraNumber - 1], ayahs[suraNumber - 1]) else Pair("مكية", 0)
    }

    private fun formatHizbDisplay(raw: String): String {
        if (raw.isBlank()) return ""
        var str = raw.replace("(*", "").replace("*)", "").trim()
        str = str.replace("¼", "ربع ").replace("½", "نصف ").replace("¾", "ثلاثة أرباع ").replace("ح ", "الحزب ").replace("ح", "الحزب ")
        return str.replace("\\s+".toRegex(), " ").trim()
    }

    override fun getItemCount(): Int = blocks.size

    private fun getManzilAbbr(sura: Int): String {
        return when (sura) {
            in 1..4 -> "ف 1"; in 5..9 -> "م 2"; in 10..16 -> "ي 3"; in 17..25 -> "ب 4"; in 26..36 -> "ش 5"; in 37..49 -> "و 6"; in 50..114 -> "ق 7"; else -> ""
        }
    }

    private fun createManzilSeparator(context: android.content.Context, isDarkMode: Boolean, title: String, abbr: String): View {
        val sepView = LayoutInflater.from(context).inflate(R.layout.view_rukoo_separator, null, false)
        val tvRight = sepView.findViewById<TextView>(R.id.tvRightValue); val tvSymbol = sepView.findViewById<TextView>(R.id.tvSymbol); val tvLeft = sepView.findViewById<TextView>(R.id.tvLeftValue); val cardRoot = sepView.findViewById<com.google.android.material.card.MaterialCardView>(R.id.rukooSeparatorRoot)
        tvRight?.text = title; tvSymbol?.text = "۞"; tvLeft?.text = abbr
        val cardBg = if (isDarkMode) Color.parseColor("#1A3025") else Color.parseColor("#FAF3E0")
        val cardStroke = if (isDarkMode) Color.parseColor("#2E503F") else Color.parseColor("#D2B48C")
        val accentColor = if (isDarkMode) Color.parseColor("#81C784") else Color.parseColor("#00A86B")
        cardRoot?.setCardBackgroundColor(cardBg); cardRoot?.strokeColor = cardStroke; tvRight?.setTextColor(accentColor); tvSymbol?.setTextColor(accentColor); tvLeft?.setTextColor(accentColor)
        return sepView
    }

    companion object {
        val verseCircleRegex = Regex("([\\u06DD۝][\\u0660-\\u0669\\u06F0-\\u06F9]+)")
    }
}
