package com.sabah.bikhushue

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PathMeasure
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import kotlin.math.min

class SpiralBeadsView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val totalBeads = 100
    private val beadsPositions = FloatArray(totalBeads * 2)

    private val beadPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }
    
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    
    private val staticPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val stringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f
        color = Color.parseColor("#8C5C38")
    }

    private var idleColor = Color.parseColor("#66CDAA") 
    private var pearlColor = Color.parseColor("#FFFFFF") 
    private var pulseColor = Color.parseColor("#FFFFFF") 
    private var staticCenterColor = Color.parseColor("#FDF5E6") 

    fun setThemeColors(idle: Int, pearl: Int, pulse: Int, center: Int, string: Int) {
        idleColor = idle
        pearlColor = pearl
        pulseColor = pulse
        staticCenterColor = center
        stringPaint.color = string
        invalidate()
    }

    private var currentCount = 0
    private var beadRadius = 16f // Reduced to allow gaps between 99 beads

    private var animatingBeadIndex = -1
    private var animFraction = 0f
    private var animator: ValueAnimator? = null

    private var spiralPath = Path()

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        calculateSpiral()
    }

    private fun calculateSpiral() {
        spiralPath.reset()
        
        if (width <= 0 || height <= 0) return

        val margin = beadRadius * 2.5f
        val rect = RectF(margin, margin, width - margin, height - margin)
        
        if (rect.width() <= 0 || rect.height() <= 0) return

        // Create a beautifully rounded rectangle path for the beads
        val cornerRadius = 60f
        
        // We draw it manually so we can control the starting point (top center)
        spiralPath.moveTo(width / 2f, margin)
        spiralPath.lineTo(rect.right - cornerRadius, rect.top)
        spiralPath.arcTo(RectF(rect.right - cornerRadius * 2, rect.top, rect.right, rect.top + cornerRadius * 2), -90f, 90f)
        spiralPath.lineTo(rect.right, rect.bottom - cornerRadius)
        spiralPath.arcTo(RectF(rect.right - cornerRadius * 2, rect.bottom - cornerRadius * 2, rect.right, rect.bottom), 0f, 90f)
        spiralPath.lineTo(rect.left + cornerRadius, rect.bottom)
        spiralPath.arcTo(RectF(rect.left, rect.bottom - cornerRadius * 2, rect.left + cornerRadius * 2, rect.bottom), 90f, 90f)
        spiralPath.lineTo(rect.left, rect.top + cornerRadius)
        spiralPath.arcTo(RectF(rect.left, rect.top, rect.left + cornerRadius * 2, rect.top + cornerRadius * 2), 180f, 90f)
        spiralPath.close()

        val measure = PathMeasure(spiralPath, false)
        val length = measure.length
        
        // 99 beads on the rectangular border
        val perimeterBeads = totalBeads - 1 
        val step = length / perimeterBeads.toFloat()
        
        // Bead 0: The Dangling Bead (المئذنة المتدلية)
        beadsPositions[0] = width / 2f
        beadsPositions[1] = margin + beadRadius * 3.5f
        
        val pos = FloatArray(2)
        // Beads 1 to 99: On the perimeter
        for (i in 1 until totalBeads) {
            val distance = (i - 1) * step
            measure.getPosTan(distance, pos, null)
            beadsPositions[i * 2] = pos[0]
            beadsPositions[i * 2 + 1] = pos[1]
        }
    }

    fun setGoalAndCount(goal: Int, count: Int, animate: Boolean) {
        val newVisible = min(count, totalBeads)
        val oldVisible = min(currentCount, totalBeads)
        
        currentCount = count
        
        if (animate) {
            if (newVisible > oldVisible) {
                animatingBeadIndex = newVisible - 1
            } else if (newVisible == oldVisible && count > 0) {
                animatingBeadIndex = newVisible - 1
            } else {
                animatingBeadIndex = -1
            }
            startFlashAnimation()
        } else {
            animatingBeadIndex = -1
            invalidate()
        }
    }
    
    private fun startFlashAnimation() {
        animator?.cancel()
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 400
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener {
                animFraction = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    private fun evaluateColor(fraction: Float, startColor: Int, endColor: Int): Int {
        val startA = Color.alpha(startColor)
        val startR = Color.red(startColor)
        val startG = Color.green(startColor)
        val startB = Color.blue(startColor)

        val endA = Color.alpha(endColor)
        val endR = Color.red(endColor)
        val endG = Color.green(endColor)
        val endB = Color.blue(endColor)

        val a = (startA + (fraction * (endA - startA))).toInt()
        val r = (startR + (fraction * (endR - startR))).toInt()
        val g = (startG + (fraction * (endG - startG))).toInt()
        val b = (startB + (fraction * (endB - startB))).toInt()

        return Color.argb(a, r, g, b)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        // Draw the connecting string on the border
        stringPaint.style = Paint.Style.STROKE
        canvas.drawPath(spiralPath, stringPaint)

        // Draw string connecting top-center to the dangling bead
        val margin = beadRadius * 2.5f
        canvas.drawLine(width / 2f, margin, beadsPositions[0], beadsPositions[1], stringPaint)

        // Draw 100 beads
        for (i in 0 until totalBeads) {
            val bx = beadsPositions[i * 2]
            val by = beadsPositions[i * 2 + 1]

            val isCounted = i < currentCount
            
            if (i == animatingBeadIndex && animFraction > 0f) {
                val p = animFraction
                beadPaint.style = Paint.Style.FILL
                beadPaint.color = evaluateColor(p, idleColor, pearlColor)
                beadPaint.alpha = 255
                val currentRadius = beadRadius * (1f + p * 0.2f)
                canvas.drawCircle(bx, by, currentRadius, beadPaint)
            } else if (isCounted) {
                beadPaint.style = Paint.Style.FILL
                beadPaint.color = pearlColor
                beadPaint.alpha = 255
                canvas.drawCircle(bx, by, beadRadius, beadPaint)
            } else {
                beadPaint.style = Paint.Style.FILL
                beadPaint.color = idleColor
                beadPaint.alpha = 200 
                canvas.drawCircle(bx, by, beadRadius, beadPaint)
            }
        }
    }
}
