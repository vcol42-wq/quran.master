package com.sabah.bikhushue

data class AzkarItem(
    val id: Int,
    val category: String,
    val title: String,
    val text: String,
    val virtues: String,
    val targetCount: Int,
    var currentCount: Int,
    val isCustom: Boolean
)
