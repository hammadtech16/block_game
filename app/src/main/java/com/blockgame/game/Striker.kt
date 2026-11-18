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
        val pulse = 1f + 0.15f * kotlin.math.sin(pulsePhase)
        
        // Outer glow rings - teal (more prominent)
        for (i in 0..3) {
            val ringRadius = baseRadius * pulse + i * 14f
            val ringAlpha = (45f * (1f - i * 0.22f) / pulse).toInt()
            
            glowPaint.color = 0xFF194C61.toInt() // Teal
            glowPaint.alpha = ringAlpha
            canvas.drawCircle(x, y, ringRadius, glowPaint)
        }
        
        // Strong pulsing outer ring - teal bright
        val pulseRing = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }
        pulseRing.color = 0xFF2A7A9A.toInt() // Brighter teal
        pulseRing.alpha = (200 * (0.5f + 0.5f * kotlin.math.sin(pulsePhase * 1.5f))).toInt()
        canvas.drawCircle(x, y, baseRadius * pulse + 8f, pulseRing)
        
        // Inner glow fill
        val innerGlow = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.FILL
        }
        innerGlow.color = 0xFF2A7A9A.toInt()
        innerGlow.alpha = (60 * (0.6f + 0.4f * kotlin.math.sin(pulsePhase))).toInt()
        canvas.drawCircle(x, y, baseRadius * 0.55f * pulse, innerGlow)
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