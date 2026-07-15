package com.sabah.bikhushue

data class VerseModel(
    val id: Int,
    val sura: Int,
    val aya: Int,
    val textTajweed: String,
    val tafsirJalalayn: String,
    val page: Int,
    val ruku: Int,
    val manzil: Int,
    val juz: Int,
    val hizbQuarter: Int,
    var suraName: String = "",
    var isEndOfRuku: Boolean = false,
    var isEndOfHizb: Boolean = false
)

data class VerseBlock(
    val verses: ArrayList<VerseModel>,
    val pageNumber: Int,
    val juzNumber: Int,
    val suraNumber: Int,
    val suraName: String,
    val showHeader: Boolean = false
)
