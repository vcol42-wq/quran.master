package com.sabah.bikhushue

object QuranMetaData {

    // [Sura, Aya] of the starting verse for each page
    val PAGE_STARTS = arrayOf(
        intArrayOf(0, 0), // index 0 dummy
    )

    // [Sura, Aya] of the starting verse for each Hizb Quarter
    val HIZB_QUARTER_STARTS = arrayOf(
        intArrayOf(0, 0), // index 0 dummy
    )

    // [Sura, Aya] of the starting verse for each Manzil
    val MANZIL_STARTS = arrayOf(
        intArrayOf(0, 0), // index 0 dummy
    )

    // Map of Sura Number -> List of Ayah Numbers that are end of an Arabic Ruku
    val ARABIC_RUKUS = mapOf<Int, List<Int>>(
        1 to listOf(7),
        2 to listOf(7, 20, 29, 39, 46, 59, 61, 71, 82, 86, 96, 103, 112, 121, 129, 141, 147, 163, 167, 176, 182, 188, 196, 203, 210, 216, 221, 228, 231, 235, 242, 248, 253, 257, 260, 266, 273, 281, 283, 286),
        3 to listOf(9, 20, 30, 41, 54, 63, 71, 80, 91, 101, 109, 120, 129, 143, 148, 160, 171, 180, 189, 200),
        4 to listOf(10, 14, 22, 25, 28, 33, 42, 46, 50, 59, 70, 76, 87, 91, 96, 100, 104, 112, 115, 126, 134, 141, 152, 176),
        5 to listOf(11, 19, 26, 34, 40, 43, 50, 56, 66, 77, 86, 93, 100, 108, 115, 120),
        6 to listOf(10, 20, 30, 41, 50, 55, 60, 70, 82, 90, 94, 103, 110, 121, 129, 135, 140, 144, 150, 165),
        7 to listOf(10, 25, 31, 39, 47, 53, 58, 64, 72, 84, 93, 99, 108, 126, 137, 141, 147, 155, 162, 171, 181, 188, 198, 206),
        8 to listOf(10, 19, 28, 40, 44, 48, 60, 64, 69, 75),
        9 to listOf(6, 16, 24, 29, 37, 42, 59, 66, 72, 80, 89, 99, 110, 118, 122, 129),
        10 to listOf(10, 20, 30, 40, 53, 60, 70, 82, 92, 103, 109),
        11 to listOf(8, 24, 35, 49, 60, 68, 83, 95, 109, 123),
        12 to listOf(6, 20, 29, 35, 42, 49, 57, 68, 79, 87, 93, 111),
        13 to listOf(7, 18, 26, 31, 37, 43),
        14 to listOf(6, 12, 21, 27, 34, 41, 52),
        15 to listOf(15, 25, 44, 60, 79, 99),
        16 to listOf(9, 21, 25, 40, 50, 60, 65, 70, 76, 83, 89, 97, 100, 110, 119, 128),
        17 to listOf(10, 22, 30, 40, 52, 60, 70, 84, 93, 100, 104, 111),
        18 to listOf(12, 17, 22, 31, 44, 49, 53, 59, 74, 82, 101, 110),
        19 to listOf(15, 40, 65, 74, 82, 98),
        20 to listOf(24, 54, 76, 89, 98, 115, 128, 135),
        21 to listOf(10, 29, 41, 50, 75, 93, 112),
        22 to listOf(10, 22, 25, 33, 38, 48, 57, 64, 72, 78),
        23 to listOf(22, 32, 50, 77, 92, 118),
        24 to listOf(10, 20, 26, 34, 40, 45, 57, 61, 64),
        25 to listOf(9, 20, 34, 44, 60, 77),
        26 to listOf(9, 33, 68, 104, 122, 140, 159, 175, 191, 220, 227),
        27 to listOf(14, 31, 44, 58, 66, 75, 93),
        28 to listOf(13, 21, 28, 42, 50, 60, 75, 82, 88),
        29 to listOf(13, 22, 30, 44, 51, 63, 69),
        30 to listOf(10, 19, 27, 40, 53, 60),
        31 to listOf(11, 19, 26, 34),
        32 to listOf(11, 22, 30),
        33 to listOf(8, 20, 27, 34, 40, 52, 58, 68, 73),
        34 to listOf(9, 21, 30, 36, 45, 54),
        35 to listOf(7, 14, 26, 37, 45),
        36 to listOf(12, 32, 50, 67, 83),
        37 to listOf(21, 74, 113, 148, 182),
        38 to listOf(14, 26, 40, 64, 88),
        39 to listOf(9, 21, 31, 41, 52, 63, 70, 75),
        40 to listOf(9, 20, 27, 37, 50, 60, 68, 78, 85),
        41 to listOf(8, 18, 25, 32, 44, 54),
        42 to listOf(9, 19, 29, 43, 53),
        43 to listOf(15, 25, 35, 45, 56, 67, 89),
        44 to listOf(16, 39, 59),
        45 to listOf(11, 21, 26, 37),
        46 to listOf(10, 20, 26, 35),
        47 to listOf(11, 19, 31, 38),
        48 to listOf(10, 17, 26, 29),
        49 to listOf(10, 18),
        50 to listOf(15, 35, 45),
        51 to listOf(23, 46, 60),
        52 to listOf(28, 49),
        53 to listOf(25, 32, 62),
        54 to listOf(22, 40, 55),
        55 to listOf(25, 45, 78),
        56 to listOf(38, 74, 96),
        57 to listOf(10, 19, 25, 29),
        58 to listOf(10, 13, 22),
        59 to listOf(10, 17, 24),
        60 to listOf(9, 13),
        61 to listOf(9, 14),
        62 to listOf(8, 11),
        63 to listOf(8, 11),
        64 to listOf(10, 18),
        65 to listOf(7, 12),
        66 to listOf(7, 12),
        67 to listOf(14, 30),
        68 to listOf(33, 52),
        69 to listOf(37, 52),
        70 to listOf(35, 44),
        71 to listOf(20, 28),
        72 to listOf(19, 28),
        73 to listOf(19, 20),
        74 to listOf(31, 56),
        75 to listOf(25, 40),
        76 to listOf(22, 31),
        77 to listOf(28, 50),
        78 to listOf(30, 40),
        79 to listOf(26, 46),
        80 to listOf(42),
        81 to listOf(29),
        82 to listOf(19),
        83 to listOf(36),
        84 to listOf(25),
        85 to listOf(22),
        86 to listOf(17),
        87 to listOf(19),
        88 to listOf(26),
        89 to listOf(30),
        90 to listOf(20),
        91 to listOf(15),
        92 to listOf(21),
        93 to listOf(11),
        94 to listOf(8),
        95 to listOf(8),
        96 to listOf(19),
        97 to listOf(5),
        98 to listOf(8),
        99 to listOf(8),
        100 to listOf(11),
        101 to listOf(11),
        102 to listOf(8),
        103 to listOf(3),
        104 to listOf(9),
        105 to listOf(5),
        106 to listOf(4),
        107 to listOf(7),
        108 to listOf(3),
        109 to listOf(6),
        110 to listOf(3),
        111 to listOf(5),
        112 to listOf(4),
        113 to listOf(5),
        114 to listOf(6),
    )

    // Map of Sura Number -> List of Ayah Numbers that are end of an Eastern Ruku
    val EASTERN_RUKUS = mapOf<Int, List<Int>>(
        1 to listOf(176),
        2 to listOf(129),
        3 to listOf(128),
        4 to listOf(77),
        5 to listOf(83),
        6 to listOf(18),
        7 to listOf(6),
        8 to listOf(10, 19, 28, 40, 44, 48, 60, 64, 69, 75),
        9 to listOf(6, 16, 24, 29, 37, 42, 59, 66, 72, 80, 89, 99, 110, 118, 122, 129),
        10 to listOf(10, 20, 30, 40, 53, 60, 70, 82, 92, 103, 109),
        11 to listOf(8, 24, 35, 49, 60, 68, 83, 95, 109, 123),
        12 to listOf(6, 20, 29, 35, 42, 49, 57, 68, 79, 87, 93, 111),
        13 to listOf(7, 18, 26, 31, 37, 43),
        14 to listOf(6, 12, 21, 27, 34, 41, 52),
        15 to listOf(15, 25, 44, 60, 79, 99),
        16 to listOf(9, 21, 25, 40, 50, 60, 65, 70, 76, 83, 89, 97, 100, 110, 119, 128),
        17 to listOf(10, 22, 30, 40, 52, 60, 70, 84, 93, 100, 104, 111),
        18 to listOf(12, 17, 22, 31, 44, 49, 53, 59, 74, 82, 101, 110),
        19 to listOf(15, 40, 65, 74, 82, 98),
        20 to listOf(24, 54, 76, 89, 98, 115, 128, 135),
        21 to listOf(10, 29, 41, 50, 75, 93, 112),
        22 to listOf(10, 22, 25, 33, 38, 48, 57, 64, 72, 78),
        23 to listOf(22, 32, 50, 77, 92, 118),
        24 to listOf(10, 20, 26, 34, 40, 45, 57, 61, 64),
        25 to listOf(9, 20, 34, 44, 60, 77),
        26 to listOf(9, 33, 68, 104, 122, 140, 159, 175, 191, 220, 227),
        27 to listOf(14, 31, 44, 58, 66, 75, 93),
        28 to listOf(13, 21, 28, 42, 50, 60, 75, 82, 88),
        29 to listOf(13, 22, 30, 44, 51, 63, 69),
        30 to listOf(10, 19, 27, 40, 53, 60),
        31 to listOf(11, 19, 26, 34),
        32 to listOf(11, 22, 30),
        33 to listOf(8, 20, 27, 34, 40, 52, 58, 68, 73),
        34 to listOf(9, 21, 30, 36, 45, 54),
        35 to listOf(7, 14, 26, 37, 45),
        36 to listOf(12, 32, 50, 67, 83),
        37 to listOf(21, 74, 113, 148, 182),
        38 to listOf(14, 26, 40, 64, 88),
        39 to listOf(9, 21, 31, 41, 52, 63, 70, 75),
        40 to listOf(9, 20, 27, 37, 50, 60, 68, 78, 85),
        41 to listOf(8, 18, 25, 32, 44, 54),
        42 to listOf(9, 19, 29, 43, 53),
        43 to listOf(15, 25, 35, 45, 56, 67, 89),
        44 to listOf(16, 39, 59),
        45 to listOf(11, 21, 26, 37),
        46 to listOf(10, 20, 26, 35),
        47 to listOf(11, 19, 31, 38),
        48 to listOf(10, 17, 26, 29),
        49 to listOf(10, 18),
        50 to listOf(15, 35, 45),
        51 to listOf(23, 46, 60),
        52 to listOf(28, 49),
        53 to listOf(25, 32, 62),
        54 to listOf(22, 40, 55),
        55 to listOf(25, 45, 78),
        56 to listOf(38, 74, 96),
        57 to listOf(10, 19, 25, 29),
        58 to listOf(10, 13, 22),
        59 to listOf(10, 17, 24),
        60 to listOf(9, 13),
        61 to listOf(9, 14),
        62 to listOf(8, 11),
        63 to listOf(8, 11),
        64 to listOf(10, 18),
        65 to listOf(7, 12),
        66 to listOf(7, 12),
        67 to listOf(14, 30),
        68 to listOf(33, 52),
        69 to listOf(37, 52),
        70 to listOf(10, 35, 44),
        71 to listOf(10, 28),
        72 to listOf(19, 28),
        73 to listOf(19, 20),
        74 to listOf(31, 56),
        75 to listOf(25, 40),
        76 to listOf(22, 31),
        77 to listOf(28, 50),
        78 to listOf(30, 40),
        79 to listOf(26, 46),
        80 to listOf(42),
        81 to listOf(29),
        82 to listOf(19),
        83 to listOf(36),
        84 to listOf(25),
        85 to listOf(22),
        86 to listOf(17),
        87 to listOf(19),
        88 to listOf(26),
        89 to listOf(30),
        90 to listOf(20),
        91 to listOf(15),
        92 to listOf(21),
        93 to listOf(11),
        94 to listOf(8),
        95 to listOf(8),
        96 to listOf(19),
        97 to listOf(5),
        98 to listOf(8),
        99 to listOf(8),
        100 to listOf(11),
        101 to listOf(11),
        102 to listOf(8),
        103 to listOf(3),
        104 to listOf(9),
        105 to listOf(5),
        106 to listOf(4),
        107 to listOf(7),
        108 to listOf(3),
        109 to listOf(6),
        110 to listOf(3),
        111 to listOf(5),
        112 to listOf(4),
        113 to listOf(5),
        114 to listOf(6),
    )
    
    // Helpers
    fun getPageNumber(sura: Int, aya: Int): Int {
        var page = 1
        for (i in 1 until PAGE_STARTS.size) {
            val pSura = PAGE_STARTS[i][0]
            val pAya = PAGE_STARTS[i][1]
            if (sura > pSura || (sura == pSura && aya >= pAya)) {
                page = i
            } else {
                break
            }
        }
        return page
    }
    
    fun getHizbQuarter(sura: Int, aya: Int): Int {
        var hq = 1
        for (i in 1 until HIZB_QUARTER_STARTS.size) {
            val hSura = HIZB_QUARTER_STARTS[i][0]
            val hAya = HIZB_QUARTER_STARTS[i][1]
            if (sura > hSura || (sura == hSura && aya >= hAya)) {
                hq = i
            } else {
                break
            }
        }
        return hq
    }

    fun getManzil(sura: Int, aya: Int): Int {
        var manzil = 1
        for (i in 1 until MANZIL_STARTS.size) {
            val mSura = MANZIL_STARTS[i][0]
            val mAya = MANZIL_STARTS[i][1]
            if (sura > mSura || (sura == mSura && aya >= mAya)) {
                manzil = i
            } else {
                break
            }
        }
        return manzil
    }

    fun isEndOfArabicRuku(sura: Int, aya: Int): Boolean {
        return ARABIC_RUKUS[sura]?.contains(aya) == true
    }

    fun isEndOfEasternRuku(sura: Int, aya: Int): Boolean {
        return EASTERN_RUKUS[sura]?.contains(aya) == true
    }
    
    // Count Arabic Ruku
    fun getArabicRukuIndexInSura(sura: Int, aya: Int): Int {
        val list = ARABIC_RUKUS[sura] ?: return -1
        return list.indexOf(aya) + 1
    }
    
    fun getEasternRukuIndexInSura(sura: Int, aya: Int): Int {
        val list = EASTERN_RUKUS[sura] ?: return -1
        return list.indexOf(aya) + 1
    }
    
    fun getTotalArabicRuku(sura: Int): Int {
        return ARABIC_RUKUS[sura]?.size ?: 0
    }
    
    fun getTotalEasternRuku(sura: Int): Int {
        return EASTERN_RUKUS[sura]?.size ?: 0
    }
}
