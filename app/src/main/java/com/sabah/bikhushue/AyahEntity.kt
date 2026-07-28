package com.sabah.bikhushue

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "quran_main_table")
data class AyahEntity(
    @PrimaryKey
    @ColumnInfo(name = "id") val id: Int,
    @ColumnInfo(name = "surahIndex") val surahIndex: Int,
    @ColumnInfo(name = "ayahNumber") val ayahNumber: Int,
    @ColumnInfo(name = "ayahText") val ayahText: String,
    @ColumnInfo(name = "tafsir_ar") val tafsirAr: String?,
    @ColumnInfo(name = "translation_en") val translationEn: String?,
    @ColumnInfo(name = "translation_id") val translationId: String?,
    @ColumnInfo(name = "audio_url") val audioUrl: String?,
    @ColumnInfo(name = "pageNumber") val pageNumber: Int,
    @ColumnInfo(name = "manzil_number") val manzilNumber: Int,
    @ColumnInfo(name = "juzIndex") val juzIndex: Int,
    @ColumnInfo(name = "hizbQuarter") val hizbQuarter: Int,
    @ColumnInfo(name = "surahNameAr") val surahNameAr: String?,
    @ColumnInfo(name = "hizb_quarter_display") val hizbQuarterDisplay: String?,
    @ColumnInfo(name = "rukoo_ar_display") val rukooArDisplay: String?,
    @ColumnInfo(name = "rukoo_sh_display") val rukooShDisplay: String?,
    @ColumnInfo(name = "manzil_display") val manzilDisplay: String?,
    @ColumnInfo(name = "rukoo_ar_sura") val rukooArSura: Int,
    @ColumnInfo(name = "rukoo_ar_total") val rukooArTotal: Int = 0,
    @ColumnInfo(name = "rukoo_sh_sura") val rukooShSura: Int,
    @ColumnInfo(name = "rukoo_sh_total") val rukooShTotal: Int = 0
) {
    fun toVerseModel(): VerseModel {
        val htmlRegex = Regex("<[^>]*>")
        val textT = ayahText ?: ""
        return VerseModel(
            id = id,
            sura = surahIndex,
            aya = ayahNumber,
            textTajweed = textT,
            tafsirAr = tafsirAr ?: "",
            translationEn = translationEn ?: "",
            translationId = translationId ?: "",
            audioUrl = audioUrl ?: "",
            page = pageNumber,
            ruku = 0,
            manzil = manzilNumber,
            juz = juzIndex,
            hizbQuarter = hizbQuarter,
            suraName = surahNameAr ?: "",
            textClean = htmlRegex.replace(textT, ""),
            hizbQuarterDisplay = hizbQuarterDisplay ?: "",
            rukooArDisplay = rukooArDisplay ?: "",
            rukooShDisplay = rukooShDisplay ?: "",
            manzilDisplay = manzilDisplay ?: "",
            rukooArSura = rukooArSura,
            rukooArTotal = rukooArTotal,
            rukooShSura = rukooShSura,
            rukooShTotal = rukooShTotal,
            asbabNuzul = ""
        )
    }
}
