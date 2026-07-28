package com.sabah.bikhushue

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.text.style.ReplacementSpan

class VerseCircleSpan(
    private val textColor: Int,
    private val circleColor: Int,
    private val bgCircleColor: Int = Color.TRANSPARENT
) : ReplacementSpan() {

    override fun getSize(
        paint: Paint,
        text: CharSequence,
        start: Int,
        end: Int,
        fm: Paint.FontMetricsInt?
    ): Int {
        val textWidth = paint.measureText(text, start, end)
        val fontMetrics = paint.fontMetrics
        val fontHeight = fontMetrics.descent - fontMetrics.ascent
        val contentSize = Math.max(textWidth, fontHeight)

        if (fm != null) {
            val fontMetricsInt = paint.fontMetricsInt
            fm.top = fontMetricsInt.top
            fm.ascent = fontMetricsInt.ascent
            fm.descent = fontMetricsInt.descent
            fm.bottom = fontMetricsInt.bottom
        }
        return (contentSize + 4f).toInt()
    }

    override fun draw(
        canvas: Canvas,
        text: CharSequence,
        start: Int,
        end: Int,
        x: Float,
        top: Int,
        y: Int,
        bottom: Int,
        paint: Paint
    ) {
        val fullSize = getSize(paint, text, start, end, null).toFloat()
        
        // تصغير أبعاد الشكل البيضوي قليلاً جداً ليكون لطيفاً ودقيقاً
        val ry = ((fullSize / 2f) - 0.5f) * 0.38f
        val rx = ry * 1.25f // نسبة محكمة ومصغرة جداً للشكل البيضوي

        val centerX = x + (fullSize / 2f)
        val lineBoxCenterY = (top + bottom) / 2f
        val circleCenterY = y.toFloat()

        val ovalRect = RectF(centerX - rx, circleCenterY - ry, centerX + rx, circleCenterY + ry)

        // 1. خلفية بيضوية مصغرة جداً
        if (bgCircleColor != Color.TRANSPARENT) {
            val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
                color = bgCircleColor
            }
            canvas.drawOval(ovalRect, bgPaint)
        }

        // 2. إطار بيضوي مصغر جداً
        val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = circleColor
            strokeWidth = 1.3f
        }
        canvas.drawOval(ovalRect, circlePaint)

        // 3. رسم الشكل ورقم الآية بحجمهما الكامل في منتصف الإطار البيضوي
        val textPaint = Paint(paint).apply {
            color = textColor
            textAlign = Paint.Align.CENTER
        }
        val fontMetrics = paint.fontMetrics
        val textY = lineBoxCenterY - ((fontMetrics.descent + fontMetrics.ascent) / 2f)
        canvas.drawText(text, start, end, centerX, textY, textPaint)
    }
}
