package com.sabah.bikhushue

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import java.io.FileOutputStream
import java.util.Locale

class DatabaseHelper(private val context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    private fun loadRukuDataFromAsset(fileName: String): Map<Int, List<Int>> {
        val rukuMap = mutableMapOf<Int, List<Int>>()
        try {
            context.assets.open(fileName).bufferedReader().useLines { lines ->
                lines.drop(1).forEach { line ->
                    val parts = line.split("\t")
                    if (parts.size >= 3) {
                        val suraId = parts[0].trim().toIntOrNull()
                        if (suraId != null) {
                            val regex = Regex("\\d+")
                            val ayahs = regex.findAll(parts[2]).map { it.value.toInt() }.toList()
                            rukuMap[suraId] = ayahs
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing $fileName: ${e.message}")
        }
        return rukuMap
    }

    companion object {
        private const val DATABASE_NAME = "quran_factory.db"
        private const val DATABASE_VERSION = 1
        private const val TAG = "QuranDBFactory"
    }

    override fun onCreate(db: SQLiteDatabase?) {}

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {}

    fun checkAndCopyDatabase() {
        val prefs = context.getSharedPreferences("app", Context.MODE_PRIVATE)
        val isV7Copied = prefs.getBoolean("db_v7_asbab_copied", false)
        val dbFile = context.getDatabasePath(DATABASE_NAME)
        
        val needsCopy = !isV7Copied || !dbFile.exists() || dbFile.length() < 5000000 || !isAsbabTablePresent(dbFile)

        if (needsCopy) {
            Log.d(TAG, "قاعدة البيانات غير موجودة أو قديمة، جاري إعادة نسخها ناصعة من assets...")
            if (dbFile.exists()) dbFile.delete()
            dbFile.parentFile?.mkdirs()
            context.assets.open(DATABASE_NAME).use { input ->
                FileOutputStream(dbFile).use { output ->
                    input.copyTo(output)
                }
            }
            prefs.edit().putBoolean("db_v7_asbab_copied", true).apply()
            Log.d(TAG, "تم نسخ قاعدة البيانات الجديدة بنجاح.")
        } else {
            Log.d(TAG, "✅ قاعدة البيانات مثبتة مسبقاً ومستقرة تماماً.")
        }
    }

    private fun isAsbabTablePresent(dbFile: java.io.File): Boolean {
        if (!dbFile.exists()) return false
        var testDb: SQLiteDatabase? = null
        var cursor: android.database.Cursor? = null
        return try {
            testDb = SQLiteDatabase.openDatabase(dbFile.absolutePath, null, SQLiteDatabase.OPEN_READONLY)
            cursor = testDb.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name='asbab_nuzul'", null)
            cursor.moveToFirst()
        } catch (e: Exception) {
            false
        } finally {
            cursor?.close()
            testDb?.close()
        }
    }

    fun getAllQuranVerses(): ArrayList<VerseModel> {
        val list = ArrayList<VerseModel>()
        val db = this.readableDatabase

        val queryWithAsbab = "SELECT q.*, a.occasion AS asbab_val FROM quran_main_table q LEFT JOIN asbab_nuzul a ON q.surahIndex = a.surah AND q.ayahNumber = a.ayah WHERE q.ayahNumber != 0 ORDER BY q.id ASC"
        val queryStandard = "SELECT * FROM quran_main_table WHERE ayahNumber != 0 ORDER BY id ASC"

        val cursor = try {
            db.rawQuery(queryWithAsbab, null)
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ تعذر تنفيذ استعلام أسباب النزول، استخدام الاستعلام القياسي: ${e.message}")
            try {
                db.rawQuery(queryStandard, null)
            } catch (e2: Exception) {
                Log.e(TAG, "❌ خطأ في الاستعلام القياسي: ${e2.message}")
                null
            }
        }

        if (cursor == null) {
            db.close()
            return list
        }

        try {
            if (cursor.moveToFirst()) {
                val idIdx = cursor.getColumnIndex("id")
                val suraIdx = cursor.getColumnIndex("surahIndex")
                val ayaIdx = cursor.getColumnIndex("ayahNumber")
                val textIdx = cursor.getColumnIndex("ayahText")
                val tafsirIdx = cursor.getColumnIndex("tafsir_ar")
                val transEnIdx = cursor.getColumnIndex("translation_en")
                val transIdIdx = cursor.getColumnIndex("translation_id")
                val audioUrlIdx = cursor.getColumnIndex("audio_url")
                val pageIdx = cursor.getColumnIndex("pageNumber")
                val manzilIdx = cursor.getColumnIndex("manzil_number")
                val juzIdx = cursor.getColumnIndex("juzIndex")
                val hizbIdx = cursor.getColumnIndex("hizbQuarter")
                val suraNameIdx = cursor.getColumnIndex("surahNameAr")

                val hizbDisplayIdx = cursor.getColumnIndex("hizb_quarter_display")
                val rukooArDisplayIdx = cursor.getColumnIndex("rukoo_ar_display")
                val rukooShDisplayIdx = cursor.getColumnIndex("rukoo_sh_display")
                val manzilDisplayIdx = cursor.getColumnIndex("manzil_display")
                val rukooArSuraIdx = cursor.getColumnIndex("rukoo_ar_sura")
                val rukooArTotalIdx = cursor.getColumnIndex("rukoo_ar_total")
                val rukooShSuraIdx = cursor.getColumnIndex("rukoo_sh_sura")
                val rukooShTotalIdx = cursor.getColumnIndex("rukoo_sh_total")
                
                val asbabValIdx = cursor.getColumnIndex("asbab_val")
                val asbabNuzulIdx = cursor.getColumnIndex("asbab_nuzul")
                val asbabShortIdx = cursor.getColumnIndex("asbab")

                val htmlRegex = Regex("<[^>]*>")
                do {
                    val currentSura = if (suraIdx != -1) cursor.getInt(suraIdx) else 0
                    val currentAya = if (ayaIdx != -1) cursor.getInt(ayaIdx) else 0
                    val textT = if (textIdx != -1) cursor.getString(textIdx) else ""

                    val asbabText = when {
                        asbabValIdx != -1 -> cursor.getString(asbabValIdx)
                        asbabNuzulIdx != -1 -> cursor.getString(asbabNuzulIdx)
                        asbabShortIdx != -1 -> cursor.getString(asbabShortIdx)
                        else -> ""
                    } ?: ""

                    val verse = VerseModel(
                        id = if (idIdx != -1) cursor.getInt(idIdx) else 0,
                        sura = currentSura,
                        aya = currentAya,
                        textTajweed = textT,
                        tafsirAr = if (tafsirIdx != -1) cursor.getString(tafsirIdx) ?: "" else "",
                        translationEn = if (transEnIdx != -1) cursor.getString(transEnIdx) ?: "" else "",
                        translationId = if (transIdIdx != -1) cursor.getString(transIdIdx) ?: "" else "",
                        audioUrl = if (audioUrlIdx != -1) cursor.getString(audioUrlIdx) ?: "" else "",
                        page = if (pageIdx != -1) cursor.getInt(pageIdx) else 0,
                        ruku = 0,
                        manzil = if (manzilIdx != -1) cursor.getInt(manzilIdx) else 0,
                        juz = if (juzIdx != -1) cursor.getInt(juzIdx) else 0,
                        hizbQuarter = if (hizbIdx != -1) cursor.getInt(hizbIdx) else 0,
                        suraName = if (suraNameIdx != -1) cursor.getString(suraNameIdx) ?: "" else "",
                        textClean = htmlRegex.replace(textT, ""),
                        hizbQuarterDisplay = if (hizbDisplayIdx != -1) cursor.getString(hizbDisplayIdx) ?: "" else "",
                        rukooArDisplay = if (rukooArDisplayIdx != -1) cursor.getString(rukooArDisplayIdx) ?: "" else "",
                        rukooShDisplay = if (rukooShDisplayIdx != -1) cursor.getString(rukooShDisplayIdx) ?: "" else "",
                        manzilDisplay = if (manzilDisplayIdx != -1) cursor.getString(manzilDisplayIdx) ?: "" else "",
                        rukooArSura = if (rukooArSuraIdx != -1) cursor.getInt(rukooArSuraIdx) else 0,
                        rukooArTotal = if (rukooArTotalIdx != -1) cursor.getInt(rukooArTotalIdx) else 0,
                        rukooShSura = if (rukooShSuraIdx != -1) cursor.getInt(rukooShSuraIdx) else 0,
                        rukooShTotal = if (rukooShTotalIdx != -1) cursor.getInt(rukooShTotalIdx) else 0,
                        asbabNuzul = asbabText
                    )
                    list.add(verse)
                } while (cursor.moveToNext())
                
                for (i in 0 until list.size) {
                    val v = list[i]
                    val showHizb = (i == 0 || list[i - 1].hizbQuarter != v.hizbQuarter)
                    val hDisplay = if (showHizb) v.hizbQuarterDisplay else ""
                    if (hDisplay != v.hizbQuarterDisplay) {
                        list[i] = v.copy(hizbQuarterDisplay = hDisplay)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ خطأ أثناء جلب الآيات من الجدول: ${e.message}")
        } finally {
            cursor.close()
            db.close()
        }

        return list
    }

    fun getAsbabNuzul(surah: Int, ayah: Int): String? {
        val db = this.readableDatabase
        var occasion: String? = null
        var cursor: android.database.Cursor? = null
        try {
            cursor = db.rawQuery(
                "SELECT occasion FROM asbab_nuzul WHERE surah = ? AND ayah = ? LIMIT 1",
                arrayOf(surah.toString(), ayah.toString())
            )
            if (cursor.moveToFirst()) {
                occasion = cursor.getString(0)
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ خطأ أثناء جلب سبب النزول: ${e.message}")
        } finally {
            cursor?.close()
            db.close()
        }
        return occasion
    }

    fun getSuraNames(): Map<Int, String> {
        val map = mutableMapOf<Int, String>()
        val db = this.readableDatabase
        val tablesToTry = arrayOf("sura_names", "chapters", "suras")
        for (table in tablesToTry) {
            try {
                val cursor = db.rawQuery("SELECT * FROM $table LIMIT 1", null)
                val allCols = cursor.columnNames
                cursor.close()
                val idCol = allCols.find { it.contains("id", true) || it.contains("no", true) || it.contains("index", true) } ?: allCols[0]
                val nameCol = allCols.find { it.contains("name", true) || it.contains("title", true) || it.contains("ar", true) } ?: allCols[1]
                val finalCursor = db.rawQuery("SELECT $idCol, $nameCol FROM $table", null)
                if (finalCursor.moveToFirst()) {
                    do {
                        map[finalCursor.getInt(0)] = finalCursor.getString(1)
                    } while (finalCursor.moveToNext())
                }
                finalCursor.close()
                break 
            } catch (ignore: Exception) {}
        }
        if (map.isEmpty()) {
            val names = arrayOf("الفاتحة","البقرة","آل عمران","النساء","المائدة","الأنعام","الأعراف","الأنفال","التوبة","يونس","هود","يوسف","الرعد","إبراهيم","الحجر","النحل","الإسراء","الكهف","مريم","طه","الأنبياء","الحج","المؤمنون","النور","الفرقان","الشعراء","النمل","القصص","العنكبوت","الروم","لقمان","السجدة","الأحزاب","سبأ","فاطر","يس","الصافات","ص","الزمر","غافر","فصلت","الشورى","الزخرف","الدخان","الجاثية","الأحقاف","محمد","الفتح","الحجرات","ق","الذاريات","الطور","النجم","القمر","الرحمن","الواقعة","الحديد","المجادلة","الحشر","الممتحنة","الصف","الجمعة","المنافقون","التغابن","الطلاق","التحريم","الملك","القلم","الحاقة","المعارج","نوح","الجن","المزمل","المدثر","القيامة","الإنسان","المرسلات","النبأ","النازعات","عبس","التكوير","الانفطار","المطففين","الانشقاق","البروج","الطارق","الأعلى","الغاشية","الفجر","البلد","الشمس","الليل","الضحى","الشرح","التين","العلق","القدر","البينة","الزلزلة","العاديات","القارعة","التكاثر","العصر","الهمزة","الفيل","قريش","الماعون","الكوثر","الكافرون","النصر","المسد","الإخلاص","الفلق","الناس")
            for (i in names.indices) map[i + 1] = names[i]
        }
        db.close()
        return map
    }

    fun inspectDatabaseStructure() {
        try {
            val db = this.readableDatabase
            val c = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table'", null)
            if (c.moveToFirst()) {
                while (!c.isAfterLast) {
                    val tableName = c.getString(0)
                    val infoCursor = db.rawQuery("PRAGMA table_info($tableName)", null)
                    if (infoCursor.moveToFirst()) {
                        val nameIndex = infoCursor.getColumnIndex("name")
                        while (!infoCursor.isAfterLast) {
                            infoCursor.moveToNext()
                        }
                    }
                    infoCursor.close()
                    c.moveToNext()
                }
            }
            c.close()
            db.close()
        } catch (e: Exception) {}
    }

    private fun hasBasmalah(text: String): Boolean {
        val targetLetters = "بسماللهالرحمنالرحيم"
        var letterMatchCount = 0
        for (i in text.indices) {
            val c = text[i]
            val normalizedC = when (c) {
                'ٱ', 'أ', 'إ', 'آ' -> 'ا'
                else -> c
            }
            if (normalizedC in '\u0621'..'\u064A') {
                if (letterMatchCount < targetLetters.length && normalizedC == targetLetters[letterMatchCount]) {
                    letterMatchCount++
                } else if (letterMatchCount < targetLetters.length) {
                    return false
                }
            }
            if (letterMatchCount == targetLetters.length) return true
        }
        return false
    }

    private fun removeBasmalah(text: String): String {
        val targetLetters = "بسماللهالرحمنالرحيم"
        var letterMatchCount = 0
        var originalIndex = 0
        for (i in text.indices) {
            val c = text[i]
            val normalizedC = when (c) {
                'ٱ', 'أ', 'إ', 'آ' -> 'ا'
                else -> c
            }
            if (normalizedC in '\u0621'..'\u064A') {
                if (letterMatchCount < targetLetters.length && normalizedC == targetLetters[letterMatchCount]) {
                    letterMatchCount++
                } else {
                    break
                }
            }
            if (letterMatchCount == targetLetters.length) {
                originalIndex = i
                break
            }
        }
        if (letterMatchCount == targetLetters.length) {
            var trimIndex = originalIndex + 1
            while (trimIndex < text.length) {
                val c = text[trimIndex]
                val normalizedC = when (c) {
                    'ٱ', 'أ', 'إ', 'آ' -> 'ا'
                    else -> c
                }
                if (normalizedC in '\u0621'..'\u064A') {
                    break
                }
                trimIndex++
            }
            return text.substring(trimIndex)
        }
        return text
    }
}
