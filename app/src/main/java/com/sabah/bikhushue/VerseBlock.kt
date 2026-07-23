package com.sabah.bikhushue

data class VerseModel(
    val id: Int,
    val sura: Int,
    val aya: Int,
    val textTajweed: String, // mapped to ayahText
    val tajweedMeta: String, // mapped to tajweed_meta
    val tafsirAr: String,    // mapped to tafsir_ar
    val translationEn: String,
    val translationId: String,
    val audioUrl: String,
    val page: Int,
    val ruku: Int,
    val manzil: Int,
    val juz: Int,
    val hizbQuarter: Int,
    var suraName: String = "",
    var textClean: String = "",
    val hizbQuarterDisplay: String = "",
    val rukooArDisplay: String = "",
    val rukooShDisplay: String = "",
    val manzilDisplay: String = "",
    val rukooArSura: Int = 0,
    val rukooArTotal: Int = 0,
    val rukooShSura: Int = 0,
    val rukooShTotal: Int = 0
)

data class VerseBlock(
    val verses: ArrayList<VerseModel>,
    val pageNumber: Int,
    val juzNumber: Int,
    val suraNumber: Int,
    val suraName: String,
    val showHeader: Boolean = false
)
