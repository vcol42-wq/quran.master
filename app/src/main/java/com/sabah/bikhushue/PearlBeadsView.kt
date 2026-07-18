package com.sabah.bikhushue

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import kotlin.random.Random

class PearlBeadsView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val pearls = mutableListOf<Pearl>()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    
    private var animator: ValueAnimator? = null

    data class Pearl(
        var x: Float,
        var y: Float,
        var radius: Float,
        var baseAlpha: Int,
        var currentAlpha: Int,
        var glowSpeed: Float,
        var angle: Float
    )

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w > 0 && h > 0 && pearls.isEmpty()) {
            val random = Random(System.currentTimeMillis())
            val numPearls = 50 // Number of glowing pearls
            for (i in 0 until numPearls) {
                val pearl = Pearl(
                    x = random.nextFloat() * w,
                    y = random.nextFloat() * h,
                    radius = random.nextFloat() * 6f + 3f, // 3 to 9 radius
                    baseAlpha = random.nextInt(150, 255),
                    currentAlpha = 0,
                    glowSpeed = random.nextFloat() * 0.05f + 0.02f,
                    angle = random.nextFloat() * Math.PI.toFloat() * 2f
                )
                pearls.add(pearl)
            }
            startAnimation()
        }
    }

    private fun startAnimation() {
        animator?.cancel()
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 16L
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener {
                pearls.forEach { pearl ->
                    pearl.angle += pearl.glowSpeed
                    pearl.currentAlpha = (pearl.baseAlpha * (0.5f + 0.5f * Math.sin(pearl.angle.toDouble()))).toInt()
                }
                invalidate()
            }
            start()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        pearls.forEach { pearl ->
            // A warm pearl-like color (slight gold/yellow tint) #FFF5EE (Seashell) or #FDF5E6 (Old Lace)
            paint.color = Color.argb(pearl.currentAlpha, 255, 245, 230)
            
            // Draw glow shadow
            paint.setShadowLayer(pearl.radius * 2.5f, 0f, 0f, Color.argb(pearl.currentAlpha / 2, 255, 255, 255))
            canvas.drawCircle(pearl.x, pearl.y, pearl.radius, paint)
            paint.clearShadowLayer()
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        animator?.cancel()
    }
}
