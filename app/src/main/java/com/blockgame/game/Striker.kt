package com.blockgame.game

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class Striker(
    var x: Float,
    var y: Float,
    val width: Float,
    val height: Float
) {
    private var animationTime: Float = 0f
    private var pulsePhase: Float = 0f
    
    // Paint objects
    private val mainPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
    }
    
    private val glowPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
    }
    
    private val trailPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
    }
    
    private var previousPositions: MutableList<Pair<Float, Float>> = mutableListOf()

    fun draw(canvas: Canvas) {
        animationTime += 0.016f // ~60fps
        pulsePhase += 0.08f
        
        // Update trail
        previousPositions.add(0, x to y)
        if (previousPositions.size > 10) previousPositions.removeLast()
        
        // Draw trail
        drawTrail(canvas)
        
        // Draw outer glow rings
        drawGlowRings(canvas)
        
        // Draw main striker body
        drawStrikerBody(canvas)
    }

    private fun drawTrail(canvas: Canvas) {
        if (previousPositions.size < 2) return
        
        for (i in 1 until previousPositions.size) {
            val (px1, py1) = previousPositions[i - 1]
            val (px2, py2) = previousPositions[i]
            val alpha = (1f - i.toFloat() / previousPositions.size) * 0.25f
            val trailSize = width * 0.12f * (1f - i.toFloat() / previousPositions.size)
            
            if (trailSize > 1f && alpha > 0.02f) {
                trailPaint.color = 0xFF194C61.toInt() // Teal accent
                trailPaint.alpha = (255 * alpha).toInt()
                canvas.drawCircle(px1, py1, trailSize, trailPaint)
            }
        }
    }

    private fun drawGlowRings(canvas: Canvas) {
        val baseRadius = width * 0.5f
        val pulse = 1f + 0.12f * kotlin.math.sin(pulsePhase)
        
        // Outer glow rings - teal
        for (i in 0..2) {
            val ringRadius = baseRadius * pulse + i * 12f
            val ringAlpha = (50f * (1f - i * 0.28f) / pulse).toInt()
            
            glowPaint.color = 0xFF194C61.toInt() // Teal
            glowPaint.alpha = ringAlpha
            canvas.drawCircle(x, y, ringRadius, glowPaint)
        }
        
        // Pulsing ring - teal bright
        val ringPaint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        ringPaint.color = 0xFF2A7A9A.toInt() // Brighter teal
        ringPaint.alpha = (160 * (0.5f + 0.5f * kotlin.math.sin(pulsePhase * 1.5f))).toInt()
        canvas.drawCircle(x, y, baseRadius * pulse, ringPaint)
    }

    private fun drawStrikerBody(canvas: Canvas) {
        // Main triangular pointer - WHITE
        val path = Path()
        val halfW = width * 0.5f
        val halfH = height * 0.5f
        
        // Gradient for main body - white to light gray
        val gradient = LinearGradient(
            x - halfW, y - halfH, x + halfW, y + halfH,
            intArrayOf(0xFFFBFAFD.toInt(), 0xFFE8EAF0.toInt(), 0xFFD0D5E0.toInt()),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP
        )
        mainPaint.shader = gradient
        
        path.moveTo(x, y - halfH * 1.15f)
        path.lineTo(x - halfW * 0.65f, y + halfH * 0.35f)
        path.quadTo(x, y + halfH * 0.15f, x + halfW * 0.65f, y + halfH * 0.35f)
        path.close()
        
        canvas.drawPath(path, mainPaint)
        mainPaint.shader = null
        
        // Border highlight - teal
        val borderPaint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeWidth = 1.5f
            color = 0xFF194C61.toInt() // Teal accent
            alpha = 180
        }
        canvas.drawPath(path, borderPaint)
        
        // Inner accent triangle - teal
        val innerPath = Path()
        innerPath.moveTo(x, y - halfH * 0.65f)
        innerPath.lineTo(x - halfW * 0.35f, y + halfH * 0.05f)
        innerPath.lineTo(x + halfW * 0.35f, y + halfH * 0.05f)
        innerPath.close()
        
        val accentPaint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.FILL
        }
        accentPaint.color = 0xFF194C61.toInt() // Teal
        accentPaint.alpha = 180
        canvas.drawPath(innerPath, accentPaint)
    }

    fun updatePosition(newX: Float, newY: Float) {
        x = newX
        y = newY
    }

    fun getBounds(): android.graphics.RectF {
        return android.graphics.RectF(
            x - width / 2, y - height / 2,
            x + width / 2, y + height / 2
        )
    }
}