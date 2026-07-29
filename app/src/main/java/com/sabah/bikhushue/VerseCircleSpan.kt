package com.sabah.bikhushue

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.text.style.ReplacementSpan

class VerseCircleSpan(
    private val textColor: Int,
    private val circleColor: Int,
    private val bgCircleColor: Int = Color.TRANSPARENT,
    private val numberTextColor: Int = textColor
) : ReplacementSpan() {

    override fun getSize(
        paint: Paint,
        text: CharSequence,
        start: Int,
        end: Int,
        fm: Paint.FontMetricsInt?
    ): Int {
        val textWidth = paint.measureText(text, start, end)
        val ry = paint.textSize * 0.40f
        val rx = Math.max(ry * 1.1f, textWidth / 2f + 2f)
        val spanWidth = (rx * 2f) + 2f

        if (fm != null) {
            val fontMetricsInt = paint.fontMetricsInt
            fm.top = fontMetricsInt.top
            fm.ascent = fontMetricsInt.ascent
            fm.descent = fontMetricsInt.descent
            fm.bottom = fontMetricsInt.bottom
        }
        return spanWidth.toInt()
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
        val textWidth = paint.measureText(text, start, end)
        val ry = paint.textSize * 0.40f
        val rx = Math.max(ry * 1.1f, textWidth / 2f + 2f)

        val spanWidth = (rx * 2f) + 2f
        val centerX = x + (spanWidth / 2f)
        val lineBoxCenterY = (top + bottom) / 2f

        val ovalRect = RectF(centerX - rx, y.toFloat() - ry, centerX + rx, y.toFloat() + ry)

        // 1. خلفية بيضوية مدمجة
        if (bgCircleColor != Color.TRANSPARENT) {
            val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
                color = bgCircleColor
            }
            canvas.drawOval(ovalRect, bgPaint)
        }

        // 2. إطار بيضوي
        val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = circleColor
            strokeWidth = 1.4f
        }
        canvas.drawOval(ovalRect, circlePaint)

        // 3. رسم رمز ورقم الآية في المنتصف تماماً بوضوح عالي
        val textPaint = Paint(paint).apply {
            color = numberTextColor
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
        }
        val fontMetrics = paint.fontMetrics
        val textY = lineBoxCenterY - ((fontMetrics.descent + fontMetrics.ascent) / 2f)
        canvas.drawText(text, start, end, centerX, textY, textPaint)
    }
}
