package com.sabah.bikhushue

import android.graphics.Color
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.BackgroundColorSpan
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
    var showTajweedColors = true
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

        holder.itemView.setBackgroundColor(Color.TRANSPARENT)

        // إعادة تهيئة فاصل أعلى السورة
        holder.topSeparatorContainer?.removeAllViews()
        holder.topSeparatorContainer?.visibility = View.GONE
        
        // آلية فواصل الصفحات (Mutually Exclusive Page Separator)
        if (currentSeparator == SeparatorType.PAGE) {
            holder.sectionTitleText.text = "ص ${block.pageNumber}"
            holder.sectionTitleText.visibility = View.VISIBLE
        } else {
            holder.sectionTitleText.visibility = View.GONE
        }

        if (block.showHeader) {
            holder.suraHeaderLayout.visibility = View.VISIBLE
            holder.basmalahText.setTextColor(textColorInt)

            holder.suraNameAndNumberText?.text = "۞ سورة ${block.suraName.replace("سورة ", "")} (${block.suraNumber}) ۞"
            val suraInfo = getSuraInfo(block.suraNumber)
            holder.suraTypeAndAyahsText?.text = "${suraInfo.first} - ${suraInfo.second} آية"
            
            if (block.suraNumber == 1 || block.suraNumber == 9) {
                holder.basmalahText.text = ""
                holder.basmalahText.visibility = View.GONE
            } else {
                holder.basmalahText.text = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ"
                holder.basmalahText.visibility = View.VISIBLE
            }
            
            val isCardDark = currentBgColor.lowercase() == "#121212" || currentBgColor.lowercase() == "#2d2d2d"
            val cardBg = if (isCardDark) Color.parseColor("#1A3025") else Color.parseColor("#FAF3E0")
            val cardStroke = if (isCardDark) Color.parseColor("#2E503F") else Color.parseColor("#D2B48C")
            val cardText = if (isCardDark) Color.parseColor("#A5D6A7") else Color.parseColor("#1B5E20")
            
            holder.basmalahCard?.setCardBackgroundColor(cardBg)
            holder.basmalahCard?.strokeColor = cardStroke
            holder.suraNameAndNumberText?.setTextColor(cardText)
            holder.suraTypeAndAyahsText?.setTextColor(cardText)
        } else {
            holder.suraHeaderLayout.visibility = View.GONE
        }

        // إظهار دعاء ختم القرآن الكريم
        val isLastBlock = position == blocks.size - 1
        if (isLastBlock) {
            holder.khatmQuranCard?.visibility = View.VISIBLE
            val isCardDark = currentBgColor.lowercase() == "#121212" || currentBgColor.lowercase() == "#2d2d2d"
            val khatmBg = if (isCardDark) Color.parseColor("#1A3025") else Color.parseColor("#FAF3E0")
            val khatmStroke = if (isCardDark) Color.parseColor("#2E503F") else Color.parseColor("#D2B48C")
            val khatmTitleColor = if (isCardDark) Color.parseColor("#81C784") else Color.parseColor("#1B5E20")
            val khatmBodyColor = if (isCardDark) Color.parseColor("#E0E0E0") else Color.parseColor("#3D2B1F")

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

        val isRukooActive = currentSeparator == SeparatorType.RUKOO_KHATMA_29 || currentSeparator == SeparatorType.RUKOO_KHATMA_30
        val isHizbActive = currentSeparator == SeparatorType.HIZB
        val isKhatma30 = currentSeparator == SeparatorType.RUKOO_KHATMA_30
        val context = holder.itemView.context
        val isDarkMode = currentBgColor.lowercase() == "#121212" || currentBgColor.lowercase() == "#2d2d2d"

        // فحص ما إذا كانت الآية الأولى في الكتلة تتضمن فاصل حزب/ركوع
        val firstVerse = block.verses.firstOrNull()
        if (firstVerse != null) {
            val rTotal = if (isKhatma30) firstVerse.rukooShTotal else firstVerse.rukooArTotal
            val rukooSura = if (isKhatma30) firstVerse.rukooShSura else firstVerse.rukooArSura
            val isHizbStart = isHizbActive && firstVerse.hizbQuarterDisplay.isNotBlank()
            val isRukooStart = isRukooActive && rTotal > 0

            if ((isHizbStart || isRukooStart) && block.showHeader) {
                // إذا كان الفاصل في بداية السورة يوضع قبل البسملة وشاشات رأس السورة
                val topSepView = if (isHizbStart) {
                    val (fraction, hizbNum) = getHizbValues(firstVerse)
                    createSeparatorView(context, isDarkMode, "ح", fraction, hizbNum)
                } else {
                    createSeparatorView(context, isDarkMode, "ع", rukooSura.toString(), rTotal.toString())
                }
                holder.topSeparatorContainer?.addView(topSepView)
                holder.topSeparatorContainer?.visibility = View.VISIBLE
            }
        }

        // تقسيم الآيات إلى أجزاء عند الفواصل
        val verseChunks = mutableListOf<MutableList<VerseModel>>()
        var currentChunk = mutableListOf<VerseModel>()

        for (index in block.verses.indices) {
            val v = block.verses[index]
            val rTotal = if (isKhatma30) v.rukooShTotal else v.rukooArTotal
            val hasHizbSep = isHizbActive && v.hizbQuarterDisplay.isNotBlank()
            val hasRukooSep = isRukooActive && rTotal > 0

            // عدم التقسيم في الآية الأولى إذا كانت قد أضيفت في الأعلى قبل البسملة
            if (index > 0 && (hasHizbSep || hasRukooSep)) {
                if (currentChunk.isNotEmpty()) {
                    verseChunks.add(currentChunk)
                    currentChunk = mutableListOf()
                }
            }
            currentChunk.add(v)
        }
        if (currentChunk.isNotEmpty()) {
            verseChunks.add(currentChunk)
        }

        for (chunkIndex in verseChunks.indices) {
            val chunk = verseChunks[chunkIndex]
            if (chunk.isEmpty()) continue

            // إدراج الفاصل قبل المقطع (باستثناء المقطع الأول إذا كان في بداية السورة قبل البسملة)
            if (chunkIndex > 0) {
                val firstVerseInChunk = chunk.first()
                val rTotal = if (isKhatma30) firstVerseInChunk.rukooShTotal else firstVerseInChunk.rukooArTotal
                val rukooSura = if (isKhatma30) firstVerseInChunk.rukooShSura else firstVerseInChunk.rukooArSura
                val hasHizbSep = isHizbActive && firstVerseInChunk.hizbQuarterDisplay.isNotBlank()
                val hasRukooSep = isRukooActive && rTotal > 0

                if (hasHizbSep) {
                    val (fraction, hizbNum) = getHizbValues(firstVerseInChunk)
                    val sepView = createSeparatorView(context, isDarkMode, "ح", fraction, hizbNum)
                    container.addView(sepView)
                } else if (hasRukooSep) {
                    val sepView = createSeparatorView(context, isDarkMode, "ع", rukooSura.toString(), rTotal.toString())
                    container.addView(sepView)
                }
            }

            val ssb = SpannableStringBuilder()
            val verseRanges = mutableListOf<Triple<Int, Int, VerseModel>>()

            for (v in chunk) {
                val start = ssb.length

                if (showManzil && v.manzilDisplay.isNotBlank()) {
                    val marker = " " + v.manzilDisplay + " "
                    val mStart = ssb.length
                    ssb.append(marker)
                    ssb.setSpan(ForegroundColorSpan(Color.parseColor("#9C27B0")), mStart, mStart + marker.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                    ssb.setSpan(RelativeSizeSpan(0.65f), mStart, mStart + marker.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                }

                val cleanText = v.textTajweed
                val verseSpannable = TajweedColorHelper.getSpannableAyah(cleanText, v.tajweedMeta, showTajweedColors)
                ssb.append(verseSpannable)
                ssb.append(" ")
                val end = ssb.length
                verseRanges.add(Triple(start, end, v))
            }

            val plainText = ssb.toString()
            for (range in verseRanges) {
                if (range.third.sura == highlightedSura && range.third.aya == highlightedAya) {
                    val highlightColor = if (isDarkMode) Color.parseColor("#4D3D1F") else Color.parseColor("#FFF8E1")
                    val vText = range.third.textTajweed
                    val s = plainText.indexOf(vText, range.first)
                    if (s != -1) {
                        ssb.setSpan(BackgroundColorSpan(highlightColor), s, s + vText.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                    }
                }
            }

            val tv = TextView(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                gravity = android.view.Gravity.CENTER
                textSize = quranFontSize
                setTextColor(textColorInt)
                setLineSpacing(0f, 1.0f)
                includeFontPadding = false
                try {
                    typeface = androidx.core.content.res.ResourcesCompat.getFont(context, R.font.amiri_quran)
                } catch (e: Exception) {}
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    justificationMode = android.text.Layout.JUSTIFICATION_MODE_INTER_WORD
                }
                text = ssb

                setOnTouchListener { vView, event ->
                    if (event.action == MotionEvent.ACTION_UP) {
                        vView.performClick()
                        val textV = vView as TextView
                        val layout = textV.layout
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

            container.addView(tv)
        }
    }

    private fun getHizbValues(v: VerseModel): Pair<String, String> {
        val rawStr = v.hizbQuarterDisplay
        val fraction = when {
            rawStr.contains("¼") -> "¼"
            rawStr.contains("½") -> "½"
            rawStr.contains("¾") -> "¾"
            else -> ""
        }
        val hizbNum = if (v.hizbQuarter > 0) ((v.hizbQuarter - 1) / 4) + 1 else 1
        return Pair(fraction, hizbNum.toString())
    }

    private fun createSeparatorView(
        context: android.content.Context,
        isDarkMode: Boolean,
        symbol: String,
        rightValue: String,
        leftValue: String
    ): View {
        val sepView = LayoutInflater.from(context).inflate(R.layout.view_rukoo_separator, null, false)
        val tvRight = sepView.findViewById<TextView>(R.id.tvRightValue)
        val tvSymbol = sepView.findViewById<TextView>(R.id.tvSymbol)
        val tvLeft = sepView.findViewById<TextView>(R.id.tvLeftValue)
        val cardRoot = sepView.findViewById<com.google.android.material.card.MaterialCardView>(R.id.rukooSeparatorRoot)

        tvRight?.text = rightValue
        tvSymbol?.text = symbol
        tvLeft?.text = leftValue

        val cardBg = if (isDarkMode) Color.parseColor("#1A3025") else Color.parseColor("#FAF3E0")
        val cardStroke = if (isDarkMode) Color.parseColor("#2E503F") else Color.parseColor("#D2B48C")
        val accentColor = if (isDarkMode) Color.parseColor("#81C784") else Color.parseColor("#00A86B")

        cardRoot?.setCardBackgroundColor(cardBg)
        cardRoot?.strokeColor = cardStroke
        tvRight?.setTextColor(accentColor)
        tvSymbol?.setTextColor(accentColor)
        tvLeft?.setTextColor(accentColor)

        return sepView
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

    private fun formatHizbDisplay(raw: String): String {
        if (raw.isBlank()) return ""
        var str = raw.replace("(*", "").replace("*)", "").trim()
        str = str.replace("¼", "ربع ")
            .replace("½", "نصف ")
            .replace("¾", "ثلاثة أرباع ")
            .replace("ح ", "الحزب ")
            .replace("ح", "الحزب ")
        return str.replace("\\s+".toRegex(), " ").trim()
    }

    override fun getItemCount(): Int = blocks.size
}
