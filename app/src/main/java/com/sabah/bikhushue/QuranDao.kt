package com.sabah.bikhushue

import androidx.room.Dao
import androidx.room.Query

@Dao
interface QuranDao {
    @Query("SELECT * FROM quran_main_table WHERE ayahNumber != 0 ORDER BY id ASC")
    fun getAllQuranVerses(): List<AyahEntity>

    @Query("SELECT * FROM quran_main_table WHERE surahIndex = :suraId ORDER BY id ASC")
    fun getVersesBySura(suraId: Int): List<AyahEntity>
}
