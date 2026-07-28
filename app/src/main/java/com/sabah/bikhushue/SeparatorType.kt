package com.sabah.bikhushue

enum class SeparatorType {
    PAGE,
    RUKOO_KHATMA_29, // يقرأ من أعمدة rukoo_ar
    RUKOO_KHATMA_30, // يقرأ من أعمدة rukoo_sh
    HIZB,            // فواصل الأحزاب وأرباعها
    MANZIL,          // فواصل المنازل
    NONE;

    companion object {
        fun fromString(value: String?): SeparatorType {
            return try {
                if (value != null) valueOf(value) else PAGE
            } catch (e: Exception) {
                PAGE
            }
        }
    }
}
