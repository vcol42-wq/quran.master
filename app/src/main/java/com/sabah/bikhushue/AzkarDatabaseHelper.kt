package com.sabah.bikhushue

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.io.FileOutputStream

class AzkarDatabaseHelper(private val context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "azkar_db.sqlite"
        private const val DATABASE_VERSION = 1
        const val TABLE_AZKAR = "azkar"
        
        const val COL_ID = "id"
        const val COL_CATEGORY = "category"
        const val COL_TITLE = "title"
        const val COL_TEXT = "text"
        const val COL_VIRTUES = "virtues"
        const val COL_COUNT = "target_count"
        const val COL_IS_CUSTOM = "is_custom"
    }

    // External DB connection
    private var externalDb: SQLiteDatabase? = null

    init {
        copyExternalDatabaseIfNeeded()
    }

    private fun copyExternalDatabaseIfNeeded() {
        val extDbName = "azkar.db"
        val dbFile = context.getDatabasePath(extDbName)
        
        if (!dbFile.exists() || dbFile.length() < 10000) {
            dbFile.parentFile?.mkdirs()
            try {
                context.assets.open(extDbName).use { input ->
                    FileOutputStream(dbFile).use { output ->
                        input.copyTo(output)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        
        if (dbFile.exists() && dbFile.length() > 50000) {
            try {
                externalDb = SQLiteDatabase.openDatabase(dbFile.absolutePath, null, SQLiteDatabase.OPEN_READONLY)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTable = """
            CREATE TABLE $TABLE_AZKAR (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_CATEGORY TEXT,
                $COL_TITLE TEXT,
                $COL_TEXT TEXT,
                $COL_VIRTUES TEXT,
                $COL_COUNT INTEGER,
                $COL_IS_CUSTOM INTEGER DEFAULT 0
            )
        """.trimIndent()
        db.execSQL(createTable)
        seedInitialAzkar(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_AZKAR")
        onCreate(db)
    }

    private fun seedInitialAzkar(db: SQLiteDatabase) {
        val defaultCategory = "أذكار الصباح والمساء"
        
        val initialAzkar = listOf(
            AzkarItem(0, defaultCategory, "سيد الاستغفار", "اللَّهُمَّ أَنْتَ رَبِّي لا إِلَهَ إِلا أَنْتَ، خَلَقْتَنِي وَأَنَا عَبْدُكَ، وَأَنَا عَلَى عَهْدِكَ وَوَعْدِكَ مَا اسْتَطَعْتُ، أَعُوذُ بِكَ مِنْ شَرِّ مَا صَنَعْتُ، أَبُوءُ لَكَ بِنِعْمَتِكَ عَلَيَّ، وَأَبُوءُ بِذَنْبِي، فَاغْفِرْ لِي، فَإِنَّهُ لا يَغْفِرُ الذُّنُوبَ إِلا أَنْتَ.", "من قاله موقناً به ومات دخل الجنة", 1, 1, false),
            AzkarItem(0, defaultCategory, "التسليم والخضوع", "اللهم ما اصبح بي من نعمة او بأحد من خلقك فمنك وحدك لا شريك لك فلك الحمد ولك الشكر", "من قالها فقد أدى شكر يومه", 1, 1, false),
            AzkarItem(0, defaultCategory, "التشهد", "اللهم اني أصبحت اشهدك واشهد حملة عرشك وملائكتك وجميع خلقك انك انت الله لا اله الا انت وحدك لا شريك لك وان محمدا عبدك ورسولك", "من قالها أعتقه الله من النار", 4, 4, false),
            AzkarItem(0, defaultCategory, "الرضا", "رضيت بالله ربا وبالإسلام دينا وبمحمد صلى الله عليه وسلم نبيا", "كان حقاً على الله أن يرضيه", 3, 3, false),
            AzkarItem(0, defaultCategory, "التوكل", "حسبي الله لا اله الا هو عليه توكلت وهو رب العرش العظيم", "كفاه الله ما أهمه من أمر الدنيا والآخرة", 7, 7, false),
            AzkarItem(0, defaultCategory, "التعوذ", "اعوذ بكلمات الله التامات من شر ما خلق", "لم يضره شيء", 3, 3, false),
            AzkarItem(0, defaultCategory, "الالتجاء الى الله", "بسم الله الذي لا يضر مع اسمه شيء في الأرض ولا في السماء وهو السميع العليم", "لم يصبه فجأة بلاء", 3, 3, false),
            AzkarItem(0, defaultCategory, "الذكر الجامع", "أَمْسَيْنَا وَأَمْسَى الْمُلْكُ لِلَّهِ، وَالْحَمْدُ لِلَّهِ، لا إِلَهَ إِلا اللَّهُ وَحْدَهُ لا شَرِيكَ لَهُ، لَهُ الْمُلْكُ وَلَهُ الْحَمْدُ وَهُوَ عَلَى كُلِّ شَيْءٍ قَدِيرٌ، رَبِّ أَسْأَلُكَ خَيْرَ مَا فِي هَذِهِ اللَّيْلَةِ وَخَيْرَ مَا بَعْدَهَا، وَأَعُوذُ بِكَ مِنْ شَرِّ مَا فِي هَذِهِ اللَّيْلَةِ وَشَرِّ مَا بَعْدَهَا، رَبِّ أَعُوذُ بِكَ مِنَ الْكَسَلِ وَسُوءِ الْكِبَرِ، رَبِّ أَعُوذُ بِكَ مِنْ عَذَابٍ فِي النَّارِ وَعَذَابٍ فِي الْقَبْرِ.", "", 1, 1, false),
            AzkarItem(0, defaultCategory, "آية الكرسي", "اللَّهُ لَا إِلَهَ إِلَّا هُوَ الْحَيُّ الْقَيُّومُ ۚ لَا تَأْخُذُهُ سِنَةٌ وَلَا نَوْمٌ ۚ لَّهُ مَا فِي السَّمَاوَاتِ وَمَا فِي الْأَرْضِ ۗ مَن ذَا الَّذِي يَشْفَعُ عِندَهُ إِلَّا بِإِذْنِهِ ۚ يَعْلَمُ مَا بَيْنَ أَيْدِيهِمْ وَمَا خَلْفَهُمْ ۖ وَلَا يُحِيطُونَ بِشَيْءٍ مِّنْ عِلْمِهِ إِلَّا بِمَا شَاءَ ۚ وَسِعَ كُرْسِيُّهُ السَّمَاوَاتِ وَالْأَرْضَ ۖ وَلَا يَئُودُهُ حِفْظُهُمَا ۚ وَهُوَ الْعَلِيُّ الْعَظِيمُ", "حفظ من الشيطان", 1, 1, false),
            AzkarItem(0, defaultCategory, "الإخلاص", "قُلْ هُوَ اللَّهُ أَحَدٌ ۝ اللَّهُ الصَّمَدُ ۝ لَمْ يَلِدْ وَلَمْ يُولَدْ ۝ وَلَمْ يَكُن لَّهُ كُفُوًا أَحَدٌ", "تكفيه من كل شيء", 3, 3, false),
            AzkarItem(0, defaultCategory, "الفلق", "قُلْ أَعُوذُ بِرَبِّ الْفَلَقِ ۝ مِن شَرِّ مَا خَلَقَ ۝ وَمِن شَرِّ غَاسِقٍ إِذَا وَقَبَ ۝ وَمِن شَرِّ النَّفَّاثَاتِ فِي الْعُقَدِ ۝ وَمِن شَرِّ حَاسِدٍ إِذَا حَسَدَ", "تكفيه من كل شيء", 3, 3, false),
            AzkarItem(0, defaultCategory, "الناس", "قُلْ أَعُوذُ بِرَبِّ النَّاسِ ۝ مَلِكِ النَّاسِ ۝ إِلَهِ النَّاسِ ۝ مِن شَرِّ الْوَسْوَاسِ الْخَنَّاسِ ۝ الَّذِي يُوَسْوِسُ فِي صُدُورِ النَّاسِ ۝ مِنَ الْجِنَّةِ وَالنَّاسِ", "تكفيه من كل شيء", 3, 3, false),
            AzkarItem(0, defaultCategory, "الاستغفار", "استغفر الله", "", 100, 100, false),
            AzkarItem(0, defaultCategory, "التسبيح", "سبحان الله وبحمده", "حطت خطاياه وإن كانت مثل زبد البحر", 100, 100, false),
            AzkarItem(0, defaultCategory, "التوحيد والتحصين", "لا إِلَهَ إِلا اللَّهُ وَحْدَهُ لا شَرِيكَ لَهُ، لَهُ الْمُلْكُ وَلَهُ الْحَمْدُ، وَهُوَ عَلَى كُلِّ شَيْءٍ قَدِيرٌ", "حرز من الشيطان وعتق رقاب", 100, 100, false)
        )

        for (item in initialAzkar) {
            val values = ContentValues().apply {
                put(COL_CATEGORY, item.category)
                put(COL_TITLE, item.title)
                put(COL_TEXT, item.text)
                put(COL_VIRTUES, item.virtues)
                put(COL_COUNT, item.targetCount)
                put(COL_IS_CUSTOM, if (item.isCustom) 1 else 0)
            }
            db.insert(TABLE_AZKAR, null, values)
        }
    }

    fun getAzkarByCategory(category: String): List<AzkarItem> {
        val list = mutableListOf<AzkarItem>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_AZKAR, null, "$COL_CATEGORY = ?", arrayOf(category),
            null, null, "$COL_ID ASC"
        )
        
        while (cursor.moveToNext()) {
            list.add(
                AzkarItem(
                    id = cursor.getInt(cursor.getColumnIndexOrThrow(COL_ID)),
                    category = cursor.getString(cursor.getColumnIndexOrThrow(COL_CATEGORY)),
                    title = cursor.getString(cursor.getColumnIndexOrThrow(COL_TITLE)),
                    text = cursor.getString(cursor.getColumnIndexOrThrow(COL_TEXT)),
                    virtues = cursor.getString(cursor.getColumnIndexOrThrow(COL_VIRTUES)),
                    targetCount = cursor.getInt(cursor.getColumnIndexOrThrow(COL_COUNT)),
                    currentCount = cursor.getInt(cursor.getColumnIndexOrThrow(COL_COUNT)),
                    isCustom = cursor.getInt(cursor.getColumnIndexOrThrow(COL_IS_CUSTOM)) == 1
                )
            )
        }
        cursor.close()
        return list
    }
    
    fun getCategories(): List<String> {
        val list = mutableListOf<String>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT DISTINCT $COL_CATEGORY FROM $TABLE_AZKAR", null)
        while(cursor.moveToNext()) {
            list.add(cursor.getString(0))
        }
        cursor.close()
        return list
    }

    private fun normalizeArabic(text: String): String {
        var normalized = text.replace(Regex("[\u064B-\u065F]"), "") // Remove tashkeel
        normalized = normalized.replace(Regex("[إأآا]"), "ا") // Normalize Alef
        normalized = normalized.replace("ة", "ه") // Normalize Ta Marbuta
        normalized = normalized.replace("ى", "ي") // Normalize Alef Maksura
        return normalized
    }

    fun searchAzkar(query: String): List<AzkarItem> {
        val list = mutableListOf<AzkarItem>()
        val db = readableDatabase
        
        val normalizedQuery = normalizeArabic(query.trim())
        
        // 1. Search in Local (Custom/Morning) Adhkar
        val localCursor = db.query(TABLE_AZKAR, null, null, null, null, null, "$COL_ID ASC")
        while (localCursor.moveToNext()) {
            val title = localCursor.getString(localCursor.getColumnIndexOrThrow(COL_TITLE))
            val text = localCursor.getString(localCursor.getColumnIndexOrThrow(COL_TEXT))
            
            val normalizedTitle = normalizeArabic(title)
            val normalizedText = normalizeArabic(text)
            
            if (normalizedTitle.contains(normalizedQuery) || normalizedText.contains(normalizedQuery)) {
                list.add(
                    AzkarItem(
                        id = localCursor.getInt(localCursor.getColumnIndexOrThrow(COL_ID)),
                        category = localCursor.getString(localCursor.getColumnIndexOrThrow(COL_CATEGORY)),
                        title = title,
                        text = text,
                        virtues = localCursor.getString(localCursor.getColumnIndexOrThrow(COL_VIRTUES)),
                        targetCount = localCursor.getInt(localCursor.getColumnIndexOrThrow(COL_COUNT)),
                        currentCount = localCursor.getInt(localCursor.getColumnIndexOrThrow(COL_COUNT)),
                        isCustom = localCursor.getInt(localCursor.getColumnIndexOrThrow(COL_IS_CUSTOM)) == 1
                    )
                )
            }
        }
        localCursor.close()

        // 2. Search in External azkar.db
        if (externalDb == null) {
            val extDbName = "azkar.db"
            val dbFile = context.getDatabasePath(extDbName)
            if (dbFile.exists() && dbFile.length() > 50000) {
                try {
                    externalDb = SQLiteDatabase.openDatabase(dbFile.absolutePath, null, SQLiteDatabase.OPEN_READONLY)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        
        externalDb?.let { extDb ->
            val extCursor = extDb.query("azkar_table", null, null, null, null, null, "id ASC")
            while (extCursor.moveToNext()) {
                val categoryName = extCursor.getString(extCursor.getColumnIndexOrThrow("category_name"))
                val text = extCursor.getString(extCursor.getColumnIndexOrThrow("zekr_text"))
                
                val normalizedCategory = normalizeArabic(categoryName ?: "")
                val normalizedText = normalizeArabic(text ?: "")
                
                if (normalizedCategory.contains(normalizedQuery) || normalizedText.contains(normalizedQuery)) {
                    val count = extCursor.getInt(extCursor.getColumnIndexOrThrow("repeat_count"))
                    list.add(
                        AzkarItem(
                            id = extCursor.getInt(extCursor.getColumnIndexOrThrow("id")),
                            category = categoryName ?: "",
                            title = categoryName ?: "ذكر",
                            text = text ?: "",
                            virtues = "", // External DB doesn't have virtues
                            targetCount = count,
                            currentCount = count,
                            isCustom = false
                        )
                    )
                }
            }
            extCursor.close()
        }

        return list
    }

    fun insertCustomZikr(item: AzkarItem) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_CATEGORY, item.category)
            put(COL_TITLE, item.title)
            put(COL_TEXT, item.text)
            put(COL_VIRTUES, item.virtues)
            put(COL_COUNT, item.targetCount)
            put(COL_IS_CUSTOM, 1)
        }
        db.insert(TABLE_AZKAR, null, values)
    }
}
