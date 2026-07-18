package com.sabah.bikhushue

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import kotlin.random.Random

class NebulaView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val particles = mutableListOf<Particle>()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    
    private val bgPaint = Paint().apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#050510") // Very dark blue/black
    }

    private var isInitialized = false

    data class Particle(
        var x: Float,
        var y: Float,
        var radius: Float,
        var alpha: Float,
        var dx: Float,
        var dy: Float,
        var color: Int
    )

    private fun initParticles(width: Int, height: Int) {
        particles.clear()
        val numParticles = 100
        val colors = listOf(
            Color.parseColor("#87CEEB"), // Sky blue
            Color.parseColor("#E6E6FA"), // Lavender
            Color.parseColor("#FFB6C1"), // Light pink
            Color.WHITE
        )

        for (i in 0 until numParticles) {
            particles.add(
                Particle(
                    x = Random.nextFloat() * width,
                    y = Random.nextFloat() * height,
                    radius = Random.nextFloat() * 4f + 1f,
                    alpha = Random.nextFloat(),
                    dx = (Random.nextFloat() - 0.5f) * 1.5f,
                    dy = (Random.nextFloat() - 0.5f) * 1.5f,
                    color = colors.random()
                )
            )
        }
        isInitialized = true
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        initParticles(w, h)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (!isInitialized) return

        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        for (p in particles) {
            paint.color = p.color
            paint.alpha = (p.alpha * 255).toInt()
            canvas.drawCircle(p.x, p.y, p.radius, paint)

            p.x += p.dx
            p.y += p.dy

            // Twinkle effect
            p.alpha += (Random.nextFloat() - 0.5f) * 0.05f
            if (p.alpha > 1f) p.alpha = 1f
            if (p.alpha < 0.1f) p.alpha = 0.1f

            // Wrap around
            if (p.x < 0) p.x = width.toFloat()
            if (p.x > width) p.x = 0f
            if (p.y < 0) p.y = height.toFloat()
            if (p.y > height) p.y = 0f
        }

        invalidate()
    }
}
