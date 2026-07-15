package com.sabah.bikhushue

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import java.io.FileOutputStream

class DatabaseHelper(private val context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "quran_factory.db"
        private const val DATABASE_VERSION = 1
        private const val TAG = "QuranDBFactory"
    }

    override fun onCreate(db: SQLiteDatabase?) {}

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {}

    fun checkAndCopyDatabase() {
        val dbFile = context.getDatabasePath(DATABASE_NAME)
        if (!dbFile.exists() || dbFile.length() < 10000) {
            Log.d(TAG, "قاعدة البيانات غير موجودة، جاري نسخها من assets...")
            dbFile.parentFile?.mkdirs()
            context.assets.open(DATABASE_NAME).use { input ->
                FileOutputStream(dbFile).use { output ->
                    input.copyTo(output)
                }
            }
            Log.d(TAG, "تم نسخ قاعدة البيانات بنجاح.")
        } else {
            Log.d(TAG, "✅ قاعدة البيانات مثبتة مسبقاً ومستقرة تماماً.")
        }
    }

    // 🚀 دالة سحب كل الأسطر والآيات من جدول quran_table وترتيبها تصاعدياً
    fun getAllQuranVerses(): ArrayList<VerseModel> {
        val list = ArrayList<VerseModel>()
        val db = this.readableDatabase

        // الاستعلام الرسمي المتوافق مع أسماء أعمدة ملفك بالملي
        val query = "SELECT * FROM quran_table ORDER BY id ASC"
        val cursor = db.rawQuery(query, null)

        try {
            if (cursor.moveToFirst()) {
                val idIdx = cursor.getColumnIndex("id")
                val suraIdx = cursor.getColumnIndex("sura")
                val ayaIdx = cursor.getColumnIndex("aya")
                val textIdx = cursor.getColumnIndex("text_tajweed")
                val tafsirIdx = cursor.getColumnIndex("tafsir_jalalayn")
                val pageIdx = cursor.getColumnIndex("page")
                val rukuIdx = cursor.getColumnIndex("ruku")
                val manzilIdx = cursor.getColumnIndex("manzil")
                val juzIdx = cursor.getColumnIndex("juz")
                val hizbIdx = cursor.getColumnIndex("hizb_quarter")
                
                // محاولة البحث عن اسم السورة داخل نفس الجدول
                var suraNameIdx = cursor.getColumnIndex("sura_name_ar")
                if (suraNameIdx == -1) suraNameIdx = cursor.getColumnIndex("sura_name")
                if (suraNameIdx == -1) suraNameIdx = cursor.getColumnIndex("sura_ar")

                do {
                    val currentSura = if (suraIdx != -1) cursor.getInt(suraIdx) else 0
                    val currentAya = if (ayaIdx != -1) cursor.getInt(ayaIdx) else 0
                    var textT = if (textIdx != -1) cursor.getString(textIdx) else ""

                    if (currentAya == 1) {
                        val hasB = hasBasmalah(textT)
                        if (hasB) {
                            textT = removeBasmalah(textT)
                        }
                    }

                    val verse = VerseModel(
                        id = if (idIdx != -1) cursor.getInt(idIdx) else 0,
                        sura = currentSura,
                        aya = currentAya,
                        textTajweed = textT,
                        tafsirJalalayn = if (tafsirIdx != -1) cursor.getString(tafsirIdx) else "",
                        page = if (pageIdx != -1) cursor.getInt(pageIdx) else 0,
                        ruku = if (rukuIdx != -1) cursor.getInt(rukuIdx) else 0,
                        manzil = if (manzilIdx != -1) cursor.getInt(manzilIdx) else 0,
                        juz = if (juzIdx != -1) cursor.getInt(juzIdx) else 0,
                        hizbQuarter = if (hizbIdx != -1) cursor.getInt(hizbIdx) else 0,
                        suraName = if (suraNameIdx != -1) cursor.getString(suraNameIdx) else ""
                    )
                    list.add(verse)
                } while (cursor.moveToNext())
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ خطأ أثناء جلب الآيات من الجدول: ${e.message}")
        } finally {
            cursor.close()
            db.close()
        }

        return list
    }

    // دالة لجلب أسماء السور بمحاولة البحث في جداول مختلفة مع قائمة احتياطية
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
                Log.d(TAG, "✅ تم جلب أسماء السور من جدول: $table")
                break 
            } catch (ignore: Exception) {}
        }

        if (map.isEmpty() || map.values.all { it.toIntOrNull() != null }) {
            Log.e(TAG, "⚠️ جدول أسماء السور غير صالح أو يحتوي أرقاماً، استخدام القائمة الاحتياطية.")
            val names = arrayOf("الفاتحة","البقرة","آل عمران","النساء","المائدة","الأنعام","الأعراف","الأنفال","التوبة","يونس","هود","يوسف","الرعد","إبراهيم","الحجر","النحل","الإسراء","الكهف","مريم","طه","الأنبياء","الحج","المؤمنون","النور","الفرقان","الشعراء","النمل","القصص","العنكبوت","الروم","لقمان","السجدة","الأحزاب","سبأ","فاطر","يس","الصافات","ص","الزمر","غافر","فصلت","الشورى","الزخرف","الدخان","الجاثية","الأحقاف","محمد","الفتح","الحجرات","ق","الذاريات","الطور","النجم","القمر","الرحمن","الواقعة","الحديد","المجادلة","الحشر","الممتحنة","الصف","الجمعة","المنافقون","التغابن","الطلاق","التحريم","الملك","القلم","الحاقة","المعارج","نوح","الجن","المزمل","المدثر","القيامة","الإنسان","المرسلات","النبأ","النازعات","عبس","التكوير","الانفطار","المطففين","الانشقاق","البروج","الطارق","الأعلى","الغاشية","الفجر","البلد","الشمس","الليل","الضحى","الشرح","التين","العلق","القدر","البينة","الزلزلة","العاديات","القارعة","التكاثر","العصر","الهمزة","الفيل","قريش","الماعون","الكوثر","الكافرون","النصر","المسد","الإخلاص","الفلق","الناس")
            for (i in names.indices) map[i + 1] = names[i]
        }
        db.close()
        return map
    }

    // 🕵️ دالة الفحص السريع للاطمئنان على الجداول في اللوج
    fun inspectDatabaseStructure() {
        try {
            val db = this.readableDatabase
            val c = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table'", null)
            if (c.moveToFirst()) {
                while (!c.isAfterLast) {
                    val tableName = c.getString(0)
                    Log.d(TAG, "📁 وجدنا جدول باسم: $tableName")
                    val infoCursor = db.rawQuery("PRAGMA table_info($tableName)", null)
                    if (infoCursor.moveToFirst()) {
                        val nameIndex = infoCursor.getColumnIndex("name")
                        while (!infoCursor.isAfterLast) {
                            val columnName = infoCursor.getString(nameIndex)
                            Log.d(TAG, "   └── ✏️ عمود: $columnName")
                            infoCursor.moveToNext()
                        }
                    }
                    infoCursor.close()
                    c.moveToNext()
                }
            }
            c.close()
            db.close()
        } catch (e: Exception) {
            Log.e(TAG, "❌ خطأ أثناء فحص البنية: ${e.message}")
        }
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