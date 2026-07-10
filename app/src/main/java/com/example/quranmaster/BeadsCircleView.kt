package com.example.quranmaster

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

class BeadsCircleView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val totalBeads = 33
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
    
    // Theme colors
    private val idleColor = Color.parseColor("#66CDAA") // Light Emerald (زمردي فاتح)
    private val pearlColor = Color.parseColor("#FDF2E9") // Pearl (لؤلؤي)
    private val pulseColor = Color.parseColor("#FDF2E9") // Pearl glow
    private val staticCenterColor = Color.parseColor("#FDF5E6") // Creamy (كريمي)
    private val stringColor = Color.parseColor("#8C5C38") // Brown string
    
    private var currentCount = 33
    
    // Animation state
    private var animatingBeadIndex = -1
    private var animFraction = 0f
    private var animator: ValueAnimator? = null

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
            duration = 600 // Longer pulse
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener {
                animFraction = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        val cx = width / 2f
        val isLandscape = resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
        
        var radius: Float
        var cy: Float
        
        if (isLandscape) {
            cy = height / 2f
            radius = min(cx, cy) - 60f
        } else {
            radius = min(cx * 2 - 80f, height - 120f) / 2f
            cy = height / 2f
            if (radius < 50f) radius = 50f
        }
        
        // 34 total slots for 33 beads (1 gap)
        val numSlots = 34
        // Calculate bead radius so they fit perfectly in the slots with a tiny gap
        val beadRadius = (Math.PI * radius / numSlots).toFloat() * 0.85f
        
        // Static center circle
        staticPaint.color = staticCenterColor
        staticPaint.alpha = 255
        canvas.drawCircle(cx, cy, radius - 80f, staticPaint)
        
        // Center strong pulse
        if (animFraction > 0f) {
            glowPaint.color = evaluateColor(animFraction, staticCenterColor, pulseColor)
            glowPaint.alpha = ((1f - animFraction) * 150).toInt()
            val pulseRadius = radius - 80f + (animFraction * 120f)
            canvas.drawCircle(cx, cy, pulseRadius, glowPaint)
        }
        
        // Draw string connecting the beads
        beadPaint.style = Paint.Style.STROKE
        beadPaint.strokeWidth = 3f
        beadPaint.color = stringColor
        canvas.drawCircle(cx, cy, radius, beadPaint)
        
        val filledBeads = min(currentCount, totalBeads)
        
        for (i in 0 until totalBeads) {
            var slot: Float
            
            if (i < filledBeads) {
                // Counted beads are at slots 0 to C-1
                slot = i.toFloat()
            } else {
                // Uncounted beads are at slots C+1 to 33
                slot = i.toFloat() + 1f
            }
            
            // If this is the animating bead (the one just counted)
            if (i == animatingBeadIndex && animFraction > 0f) {
                // It moves from slot i+1 to slot i
                slot = (i + 1) - animFraction
            }
            
            // Slot 0 is Top (-PI/2), moving clockwise
            val angle = -Math.PI / 2 + (slot * 2 * Math.PI / numSlots)
            val x = cx + radius * cos(angle).toFloat()
            val y = cy + radius * sin(angle).toFloat()
            
            if (i == animatingBeadIndex && animFraction > 0f) {
                val p = animFraction
                beadPaint.style = Paint.Style.FILL
                beadPaint.color = evaluateColor(p, idleColor, pearlColor)
                beadPaint.alpha = 255
                val currentRadius = beadRadius * (1f + p * 0.2f)
                canvas.drawCircle(x, y, currentRadius, beadPaint)
            } else if (i < filledBeads) {
                beadPaint.style = Paint.Style.FILL
                beadPaint.color = pearlColor
                beadPaint.alpha = 255
                canvas.drawCircle(x, y, beadRadius, beadPaint)
            } else {
                beadPaint.style = Paint.Style.FILL
                beadPaint.color = idleColor
                beadPaint.alpha = 200 
                canvas.drawCircle(x, y, beadRadius, beadPaint)
            }
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
}
