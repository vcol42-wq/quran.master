package com.sabah.bikhushue

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.io.FileOutputStream

class AzkarDatabaseHelper(private val context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "azkar_db.sqlite"
        private const val DATABASE_VERSION = 5
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
        seedSadAzkar(db)
        seedPropheticAzkar(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            seedSadAzkar(db)
        }
        if (oldVersion < 3) {
            seedPropheticAzkar(db)
        }
        if (oldVersion < 4) {
            db.execSQL("DELETE FROM $TABLE_AZKAR WHERE $COL_CATEGORY = 'دعاء المحزون'")
            seedSadAzkar(db)
        }
        if (oldVersion < 5) {
            db.execSQL("DELETE FROM $TABLE_AZKAR WHERE $COL_CATEGORY = 'أذكار الصباح والمساء'")
            seedInitialAzkar(db)
        }
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
            AzkarItem(0, defaultCategory, "دعاء العافية والاستتار", "اللَّهُمَّ إِنِّي أَسْأَلُكَ العَفْوَ وَالعَافِيَةَ فِي الدُّنْيَا وَالآخِرَةِ، اللَّهُمَّ إِنِّي أَسْأَلُكَ العَفْوَ وَالعَافِيَةَ فِي دِينِي وَدُنْيَايَ وَأَهْلِي وَمَالِي، اللَّهُمَّ اسْتُرْ عَوْرَاتِي وَآمِنْ رَوْعَاتِي، اللَّهُمَّ احْفَظْنِي مِنْ بَيْنِ يَدَيَّ، وَمِنْ خَلْفِي، وَعَنْ يَمِينِي، وَعَنْ شِمَالِي، وَمِنْ فَوْقِي، وَأَعُوذُ بِعَظَمَتِكَ أَنْ أُغْتَالَ مِنْ تَحْتِي", "", 1, 1, false),
            AzkarItem(0, defaultCategory, "دعاء التوكل والتفويض", "يَا حَيُّ يَا قَيُّومُ بِرَحْمَتِكَ أَسْتَغِيثُ، أَصْلِحْ لِي شَأْنِي كُلَّهُ، وَلاَ تَكِلْنِي إِلَى نَفْسِي طَرْفَةَ عَيْنٍ", "", 1, 1, false),
            AzkarItem(0, defaultCategory, "العافية في البدن والسمع والبصر", "اللَّهُمَّ عَافِنِي فِي بَدَنِي، اللَّهُمَّ عَافِنِي فِي سَمْعِي، اللَّهُمَّ عَافِنِي فِي بَصَرِي، لاَ إِلَهَ إِلاَّ أَنْتَ", "", 3, 3, false),
            AzkarItem(0, defaultCategory, "جوامع الكلم (في الصباح فقط)", "سُبْحَانَ اللَّهِ وَبِحَمْدِهِ: عَدَدَ خَلْقِهِ، وَرِضَا نَفْسِهِ، وَزِنَةَ عَرْشِهِ، وَمِدَادَ كَلِمَاتِهِ", "", 3, 3, false),
            AzkarItem(0, defaultCategory, "الصلاة على النبي ﷺ", "اللَّهُمَّ صَلِّ وَسَلِّمْ عَلَى نَبِيِّنَا مُحَمَّدٍ", "", 10, 10, false),
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

    private fun seedSadAzkar(db: SQLiteDatabase) {
        val category = "دعاء المحزون"
        
        val sadAzkar = listOf(
            AzkarItem(0, category, "دعاء", "اللهم إليك أشكو ضعف قوتي، وقلة حيلتي، وهواني على الناس يا أرحم الراحمين، أنت رب المستضعفين وأنت ربي إلى من تكِلني ؟ إلى بعيدٍ يَتَجَهَّمُنِي (يلقاني بغلظة) أمْ إلى عدو ملَّكْتَه أمْري؟ إن لم يكن بك عليَّ غضب فلا أبالي، ولكن عافيتك أوسع لي، أعوذ بنور وجهك الذي أشرقت له الظلمات، وصلح عليه أمر الدنيا والآخرة, أن ينزل بي غضبك، أو يحل بي سخطك، لك العُتبى حتى ترضى ولا قوة إلا بالله", "", 1, 1, false),
            AzkarItem(0, category, "دعاء", "اللهم أنت الملك لا إله إلا أنت. أنت ربي وأنا عبدك ظلمت نفسي واعترفت بذنبي فاغفر لي ذنوبي جميعاً إنه لا يغفر الذنوب إلا أنت، واهدني لأحسن الأخلاق لا يهدي لأحسنها إلا أنت، واصرف عني سيئها لا يصرف عني سيئها إلا أنت، لبيك وسعديك والخير كله في يديك والشر ليس إليك، أنا بك وإليك تباركت وتعاليت أستغفرك وأتوب إليك", "", 1, 1, false),
            AzkarItem(0, category, "دعاء", "اللَّهُمَّ أَنْتَ أَحَقُّ مَنْ ذُكِرَ، وَأَحَقُّ مَنْ عُبِدَ، وَأَنْصَرُ مَنِ ابْتُغِيَ، وَأَرْأَفُ مَنْ مَلَكَ، وَأَجْوَدُ مَنْ سُئِلَ، وَأَوْسَعُ مَنْ أَعْطَى، أَنْتَ الْمَلِكُ لَا شَرِيكَ لَكَ، وَالْفَرْدُ لَا نِدّ لَك ، كُلُّ شَيْءٍ هَالِكٌ إِلَّا وَجْهَكَ، لَنْ تُطَاعَ إِلَّا بِإِذْنِكَ، وَلَنْ تُعْصَى إِلَّا بِعِلْمِكَ، تُطَاعُ فَتَشْكُرُ، وَتُعْصَى فَتَغْفِرُ، أَقْرَبُ شَهِيدٍ، وَأَدْنَى حَفِيظٍ، حُلْتَ دُونَ النُّفُوس وَأَخَذْتَ بِالنَّوَاصِي، وَكَتَبْتَ الْآثَارَ وَنَسَخْتَ الْآجَالَ، الْقُلُوبُ لَكَ مفضيه وَالسِّرُّ عِنْدَكَ عَلَانِيَةٌ، الْحَلَالُ مَا أَحْلَلْتَ وَالْحَرَامُ مَا حَرَّمْتَ، وَالدِّينُ مَا شَرَعْتَ، وَالْأَمْرُ مَا قَضَيْتَ، وَالْخَلْقُ خَلْقُكَ، وَالْعَبْدُ عَبْدُكَ، وَأَنْتَ اللَّهُ الرَّؤوفُ الرَّحِيمُ، أَسْأَلُكَ بِنُورِ وَجْهِكَ الَّذِي أَشْرَقَتْ لَهُ السَّمَوَاتُ وَالْأَرْضُ، وَبِكُلِّ حَقٍّ هُوَ لَكَ، وَبِحَقِّ السَّائِلِينَ عَلَيْكَ، أَنْ تَقْبَلَنِي فِي هَذِهِ الْغَدَاةِ أَوْ فِي هَذِهِ الْعَشِيَّةِ، وَأَنْ تُجِيرَنِي مِنَ النَّارِ بِقُدْرَتِكَ", "", 1, 1, false),
            AzkarItem(0, category, "دعاء", "اللهم اغفر لي خطيئتي، وجهلي وإسرافي، في أمري وما أنت أعلم به مني، اللهم اغفر لي جدي وهزلي وخطئي وعمدي وكل ذلك عندي، اللهم اغفر لي ما قدمت وما أخرت وما أسررت وما أعلنت وما أنت أعلم به مني أنت المقدم وأنت المؤخر، وأنت على كل شيء قدير.", "", 1, 1, false),
            AzkarItem(0, category, "دعاء", "اللهم أنت ربي لا إله إلا أنت، خلقتني وأنا عبدك، وأنا على عهدك ووعدك ما استطعت، أعوذ بك من شر ما صنعت، أبوء لك بنعمتك علي وأبوء لك بذنبي فاغفر لي، فإنه لا يغفر الذنوب إلا أنت.", "", 1, 1, false),
            AzkarItem(0, category, "دعاء", "اللهم إني عبدك، وابن عبدك، وابن أمتك، ناصيتي بيدك، ماض في حكمك، عدل في قضاؤك، أسألك بكل اسم هو لك سميت به نفسك، أو أنزلته في كتابك، أو علمته أحدا من خلقك، أو استأثرت به في علم الغيب عندك، أن تجعل القرآن ربيع قلبي، ونور صدري، وجلاء حزني، وذهاب همي.", "", 1, 1, false),
            AzkarItem(0, category, "دعاء", "اللهم بعلمك الغيب وقدرتك على الخلق أحيني ما علمت الحياة خيرا لي وتوفني إذا علمت الوفاة خيرا لي اللهم وأسألك خشيتك في الغيب والشهادة وأسألك كلمة الحق في الرضا والغضب وأسألك القصد في الفقر والغنى وأسألك نعيما لا ينفد وأسألك قرة عين لا تنقطع وأسألك الرضاء بعد القضاء وأسألك برد العيش بعد الموت وأسألك لذة النظر إلى وجهك والشوق إلى لقائك في غير ضراء مضرة ولا فتنة مضلة اللهم زينا بزينة الإيمان واجعلنا هداة مهتدين.", "", 1, 1, false),
            AzkarItem(0, category, "دعاء", "لا إله إلا أنت، سبحانك إني كنت من الظالمين", "", 1, 1, false),
            AzkarItem(0, category, "دعاء", "أَمَّن يُجِيبُ الْمُضْطَرَّ إِذَا دَعَاهُ وَيَكْشِفُ السُّوءَ وَيَجْعَلُكُمْ خُلَفَاء الْأَرْضِ أَإِلَهٌ مَّعَ اللَّهِ قَلِيلاً مَّا تَذَكَّرُونَ", "", 1, 1, false),
            AzkarItem(0, category, "دعاء", "امين و الحمد لله رب العالمين", "", 1, 1, false)
        )

        for (item in sadAzkar) {
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
        val cursor = db.rawQuery("SELECT $COL_CATEGORY FROM $TABLE_AZKAR GROUP BY $COL_CATEGORY ORDER BY MIN($COL_ID) ASC", null)
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

    private fun seedPropheticAzkar(db: SQLiteDatabase) {
        val category = "أدعية نبوية"
        val propheticAzkar = listOf(
            AzkarItem(0, category, "دعاء", "اللَّهُمَّ أَنْتَ أَحَقُّ مَنْ ذُكِرَ، وَأَحَقُّ مَنْ عُبِدَ، وَأَنْصَرُ مَنِ ابْتُغِيَ، وَأَرْأَفُ مَنْ مَلَكَ، وَأَجْوَدُ مَنْ سُئِلَ، وَأَوْسَعُ مَنْ أَعْطَى، أَنْتَ الْمَلِكُ لَا شَرِيكَ لَكَ، وَالْفَرْدُ لَا نِدّ لَك ، كُلُّ شَيْءٍ هَالِكٌ إِلَّا وَجْهَكَ، لَنْ تُطَاعَ إِلَّا بِإِذْنِكَ، وَلَنْ تُعْصَى إِلَّا بِعِلْمِكَ، تُطَاعُ فَتَشْكُرُ، وَتُعْصَى فَتَغْفِرُ، أَقْرَبُ شَهِيدٍ، وَأَدْنَى حَفِيظٍ، حُلْتَ دُونَ النُّفُوس وَأَخَذْتَ بِالنَّوَاصِي، وَكَتَبْتَ الْآثَارَ وَنَسَخْتَ الْآجَالَ، الْقُلُوبُ لَكَ مُفْضِيَةٌ وَالسِّرُّ عِنْدَكَ عَلَانِيَةٌ، الْحَلَالُ مَا أَحْلَلْتَ وَالْحَرَامُ مَا حَرَّمْتَ، وَالدِّينُ مَا شَرَعْتَ، وَالْأَمْرُ مَا قَضَيْتَ، وَالْخَلْقُ خَلْقُكَ، وَالْعَبْدُ عَبْدُكَ، وَأَنْتَ اللَّهُ الرَّؤوفُ الرَّحِيمُ، أَسْأَلُكَ بِنُورِ وَجْهِكَ الَّذِي أَشْرَقَتْ لَهُ السَّمَوَاتُ وَالْأَرْضُ، وَبِكُلِّ حَقٍّ هُوَ لَكَ، وَبِحَقِّ السَّائِلِينَ عَلَيْكَ، أَنْ تَقْبَلَنِي فِي هَذِهِ الْغَدَاةِ أَوْ فِي هَذِهِ الْعَشِيَّةِ، وَأَنْ تُجِيرَنِي مِنَ النَّارِ بِقُدْرَتِكَ", "", 1, 1, false),
            AzkarItem(0, category, "دعاء", "اللهم أنت الملك لا إله إلا أنت. أنت ربي وأنا عبدك ظلمت نفسي واعترفت بذنبي فاغفر لي ذنوبي جميعاً إنه لا يغفر الذنوب إلا أنت، واهدني لأحسن الأخلاق لا يهدي لأحسنها إلا أنت، واصرف عني سيئها لا يصرف عني سيئها إلا أنت، لبيك وسعديك والخير كله في يديك والشر ليس إليك، أنا بك وإليك تباركت وتعاليت أستغفرك وأتوب إليك", "", 1, 1, false),
            AzkarItem(0, category, "دعاء", "اللهم إليك أشكو ضعف قوتي، وقلة حيلتي، وهواني على الناس  يا أرحم الراحمين، أنت رب المستضعفين وأنت ربي إلى من تكِلني ؟ إلى بعيدٍ يَتَجَهَّمُنِي (يلقاني بغلظة (أمْ إلى عدو ملَّكْتَه أمْري؟ إن لم يكن بك عليَّ غضب فلا أبالي، ولكن عافيتك أوسع لي، أعوذ بنور وجهك الذي أشرقت له الظلمات، وصلح عليه أمر الدنيا والآخرة, أن ينزل بي غضبك، أو يحل بي سخطك، لك العُتبى (الاسترضاء) حتى ترضى ولا قوة إلا بالله)", "", 1, 1, false),
            AzkarItem(0, category, "دعاء", "أَمَّن يُجِيبُ الْمُضْطَرَّ إِذَا دَعَاهُ وَيَكْشِفُ السُّوءَ وَيَجْعَلُكُمْ خُلَفَاء الْأَرْضِ أَإِلَهٌ مَّعَ اللَّهِ قَلِيلاً مَّا تَذَكَّرُونَ", "", 1, 1, false),
            AzkarItem(0, category, "دعاء", "اللهم لك أسلمت، وبك آمنت، وعليك توكلت، وإليك خاصمت، وبك حاكمت، فاغفر لي ما قدمت وما أخرت، وأسررت وأعلنت، وما أنت أعلم به مني، لا إله إلا أنت", "متفق عليه", 1, 1, false),
            AzkarItem(0, category, "دعاء", "اللهم ربنا لك الحمد، ملء السماوات وملء الأرض، وملء ما شئت من شيء بعد أهل الثناء والمجد، لا مانع لما أعطيت، ولا معطي لما منعت ولا ينفع ذا الجد منك الجد", "أخرجه مسلم", 1, 1, false),
            AzkarItem(0, category, "دعاء", "اللهم إني ظلمت نفسي ظلما كثيرا ولا يغفر الذنوب إلا أنت، فاغفر لي مغفرة من عندك وارحمني، إنك أنت الغفور الرحيم", "متفق عليه", 1, 1, false),
            AzkarItem(0, category, "دعاء", "اللهم إني أعوذ بك من العجز والكسل، والجبن والبخل،  والهرم وعذاب القبر، اللهم آت نفسي تقواها، وزكها أنت خير من زكاها، أنت وليها ومولاها اللهم إني أعوذ بك من علم لا ينفع ومن قلب لا يخشع، ومن نفس لا تشبع، ومن دعوة لا يستجاب لها", "أخرجه مسلم", 1, 1, false),
            AzkarItem(0, category, "دعاء", "اللهم إني أعوذ بك من الكسل والهرم، والمغرم والمأثم، اللهم إني أعوذ بك من عذاب النار وفتنة النار، وفتنة القبر وعذاب القبر، وشر فتنة الغنى، وشر فتنة الفقر، ومن شر فتنة المسيح الدجال، اللهم اغسل خطاياي بماء الثلج والبرد، ونق قلبي من الخطايا، كما ينقى الثوب الأبيض من الدنس، وباعد بيني وبين خطاياي كما باعدت بين المشرق والمغرب", "متفق عليه", 1, 1, false),
            AzkarItem(0, category, "دعاء", "اللهم إني أعوذ بك من الجبن، وأعوذ بك من البخل، وأعوذ بك من أن أرد إلى أرذل العمر، وأعوذ بك من فتنة الدنيا، وعذاب القبر", "أخرجه البخاري", 1, 1, false),
            AzkarItem(0, category, "دعاء", "اللهم إني أعوذ بك من شر ما عملت، ومن شر ما لم أعمل", "أخرجه مسلم", 1, 1, false),
            AzkarItem(0, category, "دعاء", "اللهم إني أعوذ بك من فتنة النار ومن عذاب النار، وأعوذ بك من فتنة القبر، وأعوذ بك من عذاب القبر، وأعوذ بك من فتنة الغنى، وأعوذ بك من فتنة الفقر، وأعوذ بك من فتنة المسيح الدجال", "متفق عليه", 1, 1, false),
            AzkarItem(0, category, "دعاء", "اللهم إني أعوذ بك من زوال نعمتك، وتحول عافيتك وفجاءة نقمتك، وجميع سخطك", "أخرجه مسلم", 1, 1, false),
            AzkarItem(0, category, "دعاء", "اللهم إني أعوذ بك من البرص والجنون والجذام ومن سيئ الأسقام", "أخرجه أبو داود والنسائي", 1, 1, false),
            AzkarItem(0, category, "دعاء", "اللهم إني أعوذ بك من منكرات الأخلاق والأعمال والأهواء", "أخرجه الترمذي", 1, 1, false),
            AzkarItem(0, category, "دعاء", "اللهم إني أعوذ بك من شر سمعي ومن شر بصري ومن شر لساني ومن شر قلبي ومن شر منيي", "أخرجه الترمذي والنسائي", 1, 1, false),
            AzkarItem(0, category, "دعاء", "اللهم إني أعوذ بك من الفقر والقلة والذلة وأعوذ بك من أن أظلم أو أظلم", "أخرجه أبو داود والنسائي", 1, 1, false),
            AzkarItem(0, category, "دعاء", "اللهم إني أعوذ بك من الهدم وأعوذ بك من التردي وأعوذ بك من الغرق والحرق والهرم وأعوذ بك أن يتخبطني الشيطان عند الموت وأعوذ بك أن أموت في سبيلك مدبرا وأعوذ بك أن أموت لديغا", "أخرجه أبو داود والنسائي", 1, 1, false),
            AzkarItem(0, category, "دعاء", "اللهم إني أسألك من الخير كله عاجله وآجله ما علمت منه وما لم أعلم وأعوذ بك من الشر كله عاجله وآجله ما علمت منه وما لم أعلم اللهم إني أسألك من خير ما سألك عبدك ونبيك وأعوذ بك من شر ما عاذ به عبدك ونبيك اللهم إني أسألك الجنة وما قرب إليها من قول أو عمل وأعوذ بك من النار وما قرب إليها من قول أو عمل وأسألك أن تجعل كل قضاء قضيته لي خيرا", "أخرجه أحمد وابن ماجه", 1, 1, false),
            AzkarItem(0, category, "دعاء", "اللهم إني أسألك يا الله بأنك الواحد الأحد الصمد الذي لم يلد ولم يولد ولم يكن له كفوا أحد أن تغفر لي ذنوبي إنك أنت الغفور الرحيم", "أخرجه أبو داود والنسائي", 1, 1, false),
            AzkarItem(0, category, "دعاء", "اللهم إني أسألك بأن لك الحمد لا إله إلا أنت المنان بديع السماوات والأرض يا ذا الجلال والإكرام يا حي يا قيوم إني أسألك", "أخرجه أبو داود والنسائي", 1, 1, false),
            AzkarItem(0, category, "دعاء", "اللهم إني أسألك بأني أشهد أنك أنت الله لا إله إلا أنت الأحد الصمد الذي لم يلد ولم يولد ولم يكن له كفوا أحد", "أخرجه الترمذي وابن ماجه", 1, 1, false),
            AzkarItem(0, category, "دعاء", "اللهم إني أسألك فعل الخيرات وترك المنكرات وحب المساكين وإذا أردت بعبادك فتنة فاقبضني إليك غير مفتون", "أخرجه الترمذي", 1, 1, false),
            AzkarItem(0, category, "دعاء", "اللهم إني أسألك الهدى والتقى والعفاف والغنى", "أخرجه مسلم", 1, 1, false),
            AzkarItem(0, category, "دعاء", "اللهم أعني على ذكرك وشكرك وحسن عبادتك", "أخرجه البخاري وأبو داود", 1, 1, false),
            AzkarItem(0, category, "دعاء", "اللهم أصلح لي ديني الذي هو عصمة أمري، وأصلح لي دنياي التي فيها معاشي، وأصلح لي آخرتي التي فيها معادي، واجعل الحياة زيادة لي في كل خير، واجعل الموت راحة لي من كل شر", "أخرجه مسلم", 1, 1, false),
            AzkarItem(0, category, "دعاء", "اللهم اغفر لي خطيئتي، وجهلي وإسرافي، في أمري وما أنت أعلم به مني، اللهم اغفر لي جدي وهزلي وخطئي وعمدي وكل ذلك عندي، اللهم اغفر لي ما قدمت وما أخرت وما أسررت وما أعلنت وما أنت أعلم به مني أنت المقدم وأنت المؤخر، وأنت على كل شيء قدير", "متفق عليه", 1, 1, false),
            AzkarItem(0, category, "دعاء", "اللهم حبب إلينا الإيمان وزينه في قلوبنا، وكره إلينا الكفر والفسوق والعصيان، واجعلنا من الراشدين، اللهم توفنا مسلمين وأحينا مسلمين وألحقنا بالصالحين غير خزايا ولا مفتونين", "أخرجه أحمد والبخاري", 1, 1, false),
            AzkarItem(0, category, "دعاء", "اللهم اغفر لي ما قدمت وما أخرت، وما أسررت وما أعلنت، وما أسرفت، وما أنت أعلم به مني، أنت المقدم وأنت المؤخر، لا إله إلا أنت", "أخرجه مسلم", 1, 1, false),
            AzkarItem(0, category, "دعاء", "اللهم أنت ربي لا إله إلا أنت، خلقتني وأنا عبدك، وأنا على عهدك ووعدك ما استطعت، أعوذ بك من شر ما صنعت، أبوء لك بنعمتك علي وأبوء لك بذنبي فاغفر لي، فإنه لا يغفر الذنوب إلا أنت", "أخرجه البخاري", 1, 1, false),
            AzkarItem(0, category, "دعاء", "اللهم اهدني فيمن هديت، وعافني فيمن عافيت، وتولني فيمن توليت، وبارك لي فيما أعطيت وقني شر ما قضيت، إنك تقضي ولا يقضى عليك، وإنه لا يذل من واليت ولا يعز من عاديت تباركت ربنا وتعاليت", "أخرجه أبو داود والترمذي", 1, 1, false),
            AzkarItem(0, category, "دعاء", "اللهم لك أسلمت، وبك آمنت، وعليك توكلت، وإليك أنبت وبك خاصمت اللهم إني أعوذ بعزتك لا إله إلا أنت أن تضلني، أنت الحي الذي لا يموت، والجن والإنس يموتون", "متفق عليه", 1, 1, false),
            AzkarItem(0, category, "دعاء", "اللهم ربنا آتنا في الدنيا حسنة، وفي الآخرة حسنة، وقنا عذاب النار", "متفق عليه", 1, 1, false),
            AzkarItem(0, category, "دعاء", "يا مقلب القلوب ثبت قلبي على دينك", "أخرجه أحمد والترمذي", 1, 1, false),
            AzkarItem(0, category, "دعاء", "اللهم إني عبدك، وابن عبدك، وابن أمتك، ناصيتي بيدك، ماض في حكمك، عدل في قضاؤك، أسألك بكل اسم هو لك سميت به نفسك، أو أنزلته في كتابك، أو علمته أحدا من خلقك، أو استأثرت به في علم الغيب عندك، أن تجعل القرآن ربيع قلبي، ونور صدري، وجلاء حزني، وذهاب همي", "أخرجه أحمد", 1, 1, false),
            AzkarItem(0, category, "دعاء", "اللهم بعلمك الغيب وقدرتك على الخلق أحيني ما علمت الحياة خيرا لي وتوفني إذا علمت الوفاة خيرا لي اللهم وأسألك خشيتك في الغيب والشهادة وأسألك كلمة الحق في الرضا والغضب وأسألك القصد في الفقر والغنى وأسألك نعيما لا ينفد وأسألك قرة عين لا تنقطع وأسألك الرضاء بعد القضاء وأسألك برد العيش بعد الموت وأسألك لذة النظر إلى وجهك والشوق إلى لقائك في غير ضراء مضرة ولا فتنة مضلة اللهم زينا بزينة الإيمان واجعلنا هداة مهتدين", "أخرجه النسائي", 1, 1, false),
            AzkarItem(0, category, "دعاء", "اللهم إنك عفو كريم تحب العفو فاعف عني", "أخرجه الترمذي وابن ماجه", 1, 1, false),
            AzkarItem(0, category, "دعاء", "اللهم رب جبرائيل وميكائيل وإسرافيل، فاطر السماوات والأرض، عالم الغيب والشهادة، أنت تحكم بين عبادك فيما كانوا فيه يختلفون، اهدني لما اختلف فيه من الحق بإذنك إنك تهدي من تشاء إلى صراط مستقيم", "أخرجه مسلم", 1, 1, false),
            AzkarItem(0, category, "دعاء", "اللهم أعوذ برضاك من سخطك، وبمعافاتك من عقوبتك، وأعوذ بك منك، لا أحصي ثناء عليك، أنت كما أثنيت على نفسك", "أخرجه مسلم", 1, 1, false),
            AzkarItem(0, category, "دعاء", "دعوة ذي النون، إذ دعا في بطن الحوت، قال: لا إله إلا أنت، سبحانك إني كنت من الظالمين : ما دعا بها أحد قط إلا استجيب له", "رواه أحمد", 1, 1, false),
            AzkarItem(0, category, "دعاء", "ولكن هناك أوقات يزيد فيها رجاء الإجابة ينبغي للمؤمن تحريها مثل الثلث الأخير من الليل، وعند الأذان، وبين الأذان والإقامة، وأدبار الصلوات، والساعة التي في يوم الجمعة، وقد اختلف العلماء في تحديدها لاختلاف الأحاديث في ذلك، ففي بعض الروايات أنها من حين صعود الإمام إلى أن تقضى الصلاة، وفي بعضها أنها آخر ساعة بعد العصر. فينبغي الاجتهاد في الساعتين.", "", 1, 1, false),
            AzkarItem(0, category, "دعاء", "ومن الأوقات التي ترجى فيها الإجابة أيضًا: عند نزول المطر، وعند التقاء الجيشين للقتال، وعند السجود، وعند الإفطار من الصيام.", "", 1, 1, false),
            AzkarItem(0, category, "دعاء", "اللَّهُمَّ إِنِّى أَسْأَلُكَ الْهُدَى وَالتُّقَى وَالْعَفَافَ وَالْغِنَى.", "", 1, 1, false),
            AzkarItem(0, category, "دعاء", "اللهم إني أعوذ بك من جَهْدِ الْبَلاَءِ وَدَرَكِ الشَّقَاءِ وَسُوءِ الْقَضَاءِ وَشَمَاتَةِ الأَعْدَاءِ)(ثلاث مرات).", "", 1, 1, false),
            AzkarItem(0, category, "دعاء", "اللهمَّ إني أسألك فواتح الخير وخواتمه، وجوامعه، وأوله وآخره، وظاهره وباطنه، والدرجات العلى من الجنة آمين.", "", 1, 1, false),
            AzkarItem(0, category, "دعاء", "اللهمَّ إني أسألك خير ما آتي، وخير ما أفعل، وخير ما أعمل، وخير ما أبطن، وخير ما أظهر، والدرجات العلى من الجنة آمين.", "", 1, 1, false),
            AzkarItem(0, category, "دعاء", "اللهمَّ إني أسألك أن ترفع ذكري، وتضع وزري، وتصلح أمري، وتطهر قلبي، وتحصن فرجي، وتنور قلبي، وتغفر لي ذنبي، وأسألك الدرجات العلى من الجنة آمين.", "", 1, 1, false),
            AzkarItem(0, category, "دعاء", "يا مقلب القلوب ثبت قلبي على دينك.", "", 1, 1, false),
            AzkarItem(0, category, "دعاء", "اللهم مصرف القلوب صرف قلوبنا على طاعتك.", "", 1, 1, false),
            AzkarItem(0, category, "دعاء", "اللهمَّ إني أسألك أن تبارك لي في نفسي، وفي سمعي، وفي بصري، وفي روحي، وفي خَلْقي ، وفي خُلُقي، وفي أهلي، وفي محياي، وفي مماتي، وفي عملي، فتقبل حسناتي، وأسألك الدرجات العلى من الجنة آمين.", "", 1, 1, false),
            AzkarItem(0, category, "دعاء", "اللهم إنك عفو تحب العفو فاعف عني. هذا من أجمع الدعوات التي أرشد إليها النبي ﷺ.", "", 1, 1, false),
            AzkarItem(0, category, "دعاء", "يقول في سجوده: اللهم اغفر لي ذنبي كله، دقه وجله، وأوله وآخره، وعلانيته وسره", "", 1, 1, false),
            AzkarItem(0, category, "دعاء", "اللَّهُمَّ اقْسِمْ لَنَا مِنْ خَشْيَتِكَ مَا تَحُولُ بِهِ بَيْنَنَا وَبَيْنَ مَعَاصِيكَ، وَمِنْ طَاعَتِكَ مَا تُبَلِّغُنَا بِهِ جَنَّتَكَ، وَمِنَ اليَقِينِ مَا تُهَوِّنُ بِهِ عَلَيْنَا مَصَائِبَ الدُّنْيَا", "", 1, 1, false),
            AzkarItem(0, category, "دعاء", "«اللَّهُمَّ إِنَّا نَسْتَعِينُكَ، وَنُؤْمِنُ بِكَ، وَنَتَوَكَّلُ عَلَيْكَ، وَنُثْنِي عَلَيْكَ الْخَيْرَ، وَلَا نَكْفُرُكَ، اللَّهُمَّ إِيَّاكَ نَعْبُدُ، وَلَكَ نُصَلِّى «اللَّهُمَّ أنْجِ المُسْتَضْعَفِينَ مِنَ المُؤْمِنِينَ. اللَّهُمَّ اشْدُدْ وَطْأَتَكَ عَلَى الظَّالِمِينَ. اللَّهُمَّ اجْعَلْهَا عَلَيْهِمْ سِنِينَ كَسِنِي «اللهم انقطع الرجاء إلا منك، وخابت الآمال إلا فيك، وانسدت الطرق إلا إليك. اللهم إنا نجعلك في نحور أعدائنا ونعوذ بك من شرورهم. اللهم ثبت أقدامهم، وقوي عزائمهم، وانصرهم على من عاداهم يا رب العالمين».", "", 1, 1, false),
            AzkarItem(0, category, "دعاء", "«اللَّهُمَّ اقْسِمْ لَنَا مِنْ خَشْيَتِكَ مَا تَحُولُ بِهِ بَيْنَنَا وَبَيْنَ مَعَاصِيكَ، وَمِنْ طَاعَتِكَ مَا تُبَلِّغُنَا بِهِ جَنَّتَكَ، وَمِنَ الْيَقِينِ مَا تُهَوِّنُ بِهِ عَلَيْنَا مَصَائِبَ الدنيا]", "", 1, 1, false),
            AzkarItem(0, category, "دعاء", "«اللهم طهر قلبي من النفاق، وعملي من الرياء، ولساني من الكذب، وعيني من الخيانة».", "", 1, 1, false),
            AzkarItem(0, category, "دعاء", "«اللَّهُمَّ إِنِّي أَعُوذُ بِكَ مِنْ قَلْبٍ لاَ يَخْشَعُ، وَمِنْ دُعَاءٍ لاَ يُسْمَعُ، وَمِنْ نَفْسٍ لاَ تَشْبَعُ، وَمِنْ عِلْمٍ لاَ يَنْفَعُ».", "", 1, 1, false),
            AzkarItem(0, category, "دعاء", "«اللهم أنت ربي لا إله إلا أنت، خلقتني وأنا عبدك، وأنا على عهدك ووعدك ما استطعت، أعوذ بك من شر ما صنعت، أبوء لك بنعمتك عليّ، وأبوء بذنبي فاغفر لي، فإنه لا يغفر الذنوب إلا أنت", "", 1, 1, false),
            AzkarItem(0, category, "دعاء", "(اللهمَّ إني أسألك قلباً خاشعاً وأسالك في صلاتي ودعائي بركة تطهر بها قلبي، وتكشف بها كربي، وتغفر بها ذنبي، وتصلح بها أمري، وتغني بها فقري، وتذهب بها شري، وتكشف بها همي وغمي، وتشفي بها سقمي، وتقضي بها ديني، وتجلو بها حزني، وتجمع بها شملي، وتبيض بها وجهي، اللهم إني أستغفرك من كل فريضة أوجبتها علي في آناء الليل والنهار تركتها خطأ أو عمداً أو نسياناً أو جهلاً، وأستغفرك من كل سنة من سنن سيد المرسلين وخاتم النبيين سيدنا محمد صلى الله عليه وآله وسلم تركتها غفلة أو سهواً أو نسياناً أو تهاوناً أو جهلاً أو قلة مبالاة بها، أستغفر الله العظيم وأتوب إليه.", "", 1, 1, false)
        )
        for (item in propheticAzkar) {
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
}
