package com.sabah.bikhushue

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.widget.FrameLayout

/**
 * Custom FrameLayout that draws emerald green round beads linked by a wire string
 * (خرزات زمردية دائرية بحجم مصغر ومصفوفة بدقة دائرية حول البطاقة)
 */
class BeadedFrameLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    // Golden wire string linking beads together (سلك المسبحة الذهبي)
    private val wirePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2.5f * resources.displayMetrics.density
        color = Color.parseColor("#D4AF37") // Metallic gold wire
    }

    // Emerald Green Bead paint (خرز زمردي نقي)
    private val beadPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        color = Color.parseColor("#1B6B46") // Rich Emerald Green
    }

    private val path = Path()
    private val rect = RectF()

    // Bead diameter (6.5dp)
    var beadSizePx: Float = 6.5f * resources.displayMetrics.density
        set(value) {
            field = value
            updatePaint()
            invalidate()
        }

    // Smooth circular packing (8dp gap) for continuous round beaded ring
    var beadSpacingPx: Float = 8f * resources.displayMetrics.density
        set(value) {
            field = value
            updatePaint()
            invalidate()
        }

    var cornerRadiusPx: Float = 40f * resources.displayMetrics.density
        set(value) {
            field = value
            invalidate()
        }

    init {
        setWillNotDraw(false)
        updatePaint()
    }

    fun setColors(wireColor: Int, beadColor: Int) {
        wirePaint.color = wireColor
        beadPaint.color = beadColor
        invalidate()
    }

    private fun updatePaint() {
        beadPaint.strokeWidth = beadSizePx
        beadPaint.pathEffect = DashPathEffect(floatArrayOf(0f, beadSpacingPx), 0f)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val inset = beadSizePx / 2f
        rect.set(inset, inset, width - inset, height - inset)
        path.reset()

        // If width and height are equal or nearly equal, draw a perfect circular bead ring frame (إطار دائري للخرز حول الصورة)
        if (Math.abs(width - height) <= 15) {
            path.addOval(rect, Path.Direction.CW)
        } else {
            path.addRoundRect(rect, cornerRadiusPx, cornerRadiusPx, Path.Direction.CW)
        }

        // 1. Draw connecting wire string (سلك الرابط الذهبي)
        canvas.drawPath(path, wirePaint)

        // 2. Draw round beads on top (الخرز المصوف دائرياً بالكامل حول الصورة)
        canvas.drawPath(path, beadPaint)
    }
}
