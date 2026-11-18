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
    private var targetRotation: Float = 0f
    private var currentRotation: Float = 0f
    
    // Paint objects
    private val mainPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
    }
    
    private val glowPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
    }
    
    private val ringPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }
    
    private val innerRingPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = 1.5f
    }
    
    private val centerPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
    }
    
    private val accentPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
    }
    
    private val trailPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
    }
    
    private val paths = mutableListOf<Path>()
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
        
        // Draw center targeting reticle
        drawTargetingReticle(canvas)
        
        // Draw rotating accent elements
        drawAccentElements(canvas)
    }

    private fun drawTrail(canvas: Canvas) {
        if (previousPositions.size < 2) return
        
        for (i in 1 until previousPositions.size) {
            val (px1, py1) = previousPositions[i - 1]
            val (px2, py2) = previousPositions[i]
            val alpha = (1f - i.toFloat() / previousPositions.size) * 0.3f
            val trailSize = width * 0.15f * (1f - i.toFloat() / previousPositions.size)
            
            if (trailSize > 1f && alpha > 0.02f) {
                trailPaint.color = 0xFF6200EE.toInt()
                trailPaint.alpha = (255 * alpha).toInt()
                canvas.drawCircle(px1, py1, trailSize, trailPaint)
            }
        }
    }

    private fun drawGlowRings(canvas: Canvas) {
        val baseRadius = width * 0.55f
        val pulse = 1f + 0.15f * kotlin.math.sin(pulsePhase)
        
        // Outer glow
        for (i in 0..2) {
            val ringRadius = baseRadius * pulse + i * 15f
            val ringAlpha = (60f * (1f - i * 0.3f) / pulse).toInt()
            
            glowPaint.color = 0xFF6200EE.toInt()
            glowPaint.alpha = ringAlpha
            canvas.drawCircle(x, y, ringRadius, glowPaint)
        }
        
        // Pulsing ring
        ringPaint.color = 0xFF6200EE.toInt()
        ringPaint.alpha = (180 * (0.5f + 0.5f * kotlin.math.sin(pulsePhase * 1.5f))).toInt()
        ringPaint.strokeWidth = 2.5f
        canvas.drawCircle(x, y, baseRadius * pulse, ringPaint)
        
        // Inner rotating ring
        innerRingPaint.color = 0xFF03DAC5.toInt()
        innerRingPaint.alpha = 120
        innerRingPaint.strokeWidth = 1.5f
        canvas.drawCircle(x, y, baseRadius * 0.7f, innerRingPaint)
    }

    private fun drawStrikerBody(canvas: Canvas) {
        // Main triangular pointer
        val path = Path()
        val halfW = width * 0.5f
        val halfH = height * 0.5f
        
        // Gradient for main body
        val gradient = LinearGradient(
            x - halfW, y - halfH, x + halfW, y + halfH,
            intArrayOf(0xFFFFFFFF.toInt(), 0xFFE0E0E0.toInt(), 0xFFB0B0B0.toInt()),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP
        )
        mainPaint.shader = gradient
        
        path.moveTo(x, y - halfH * 1.2f)
        path.lineTo(x - halfW * 0.7f, y + halfH * 0.4f)
        path.quadTo(x, y + halfH * 0.2f, x + halfW * 0.7f, y + halfH * 0.4f)
        path.close()
        
        canvas.drawPath(path, mainPaint)
        mainPaint.shader = null
        
        // Border highlight
        val borderPaint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeWidth = 2f
            color = 0xFF03DAC5.toInt()
            alpha = 180
        }
        canvas.drawPath(path, borderPaint)
        
        // Inner accent triangle
        val innerPath = Path()
        innerPath.moveTo(x, y - halfH * 0.7f)
        innerPath.lineTo(x - halfW * 0.4f, y + halfH * 0.1f)
        innerPath.lineTo(x + halfW * 0.4f, y + halfH * 0.1f)
        innerPath.close()
        
        accentPaint.color = 0xFF6200EE.toInt()
        accentPaint.alpha = 200
        canvas.drawPath(innerPath, accentPaint)
    }

    private fun drawTargetingReticle(canvas: Canvas) {
        val reticleRadius = width * 0.18f
        
        // Outer circle
        centerPaint.color = 0xFF111111.toInt()
        canvas.drawCircle(x, y, reticleRadius + 3f, centerPaint)
        
        // Main reticle
        centerPaint.color = 0xFF6200EE.toInt()
        canvas.drawCircle(x, y, reticleRadius, centerPaint)
        
        // Inner highlight
        centerPaint.color = 0xFF03DAC5.toInt()
        canvas.drawCircle(x, y, reticleRadius * 0.6f, centerPaint)
        
        // Center dot
        centerPaint.color = 0xFFFFFFFF.toInt()
        canvas.drawCircle(x, y, reticleRadius * 0.25f, centerPaint)
        
        // Crosshair lines
        val crosshairPaint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeWidth = 1.5f
            color = 0xFFFFFFFF.toInt()
            alpha = 150
        }
        
        val lineLen = reticleRadius * 1.8f
        val gap = reticleRadius * 0.5f
        
        canvas.drawLine(x - lineLen, y, x - gap, y, crosshairPaint)
        canvas.drawLine(x + gap, y, x + lineLen, y, crosshairPaint)
        canvas.drawLine(x, y - lineLen, x, y - gap, crosshairPaint)
        canvas.drawLine(x, y + gap, x, y + lineLen, crosshairPaint)
        
        // Rotating brackets
        drawRotatingBrackets(canvas, reticleRadius * 2.2f)
    }

    private fun drawRotatingBrackets(canvas: Canvas, radius: Float) {
        canvas.save()
        canvas.translate(x, y)
        canvas.rotate(animationTime * 50f)
        
        val bracketPaint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeWidth = 2.5f
            color = 0xFF03DAC5.toInt()
            alpha = 180
            strokeCap = Paint.Cap.ROUND
        }
        
        val bracketSize = radius * 0.4f
        val thickness = 2.5f
        
        // Four corner brackets
        for (i in 0..3) {
            canvas.save()
            canvas.rotate(i * 90f)
            
            val path = Path()
            path.moveTo(radius, -bracketSize)
            path.lineTo(radius, bracketSize)
            path.moveTo(radius - bracketSize, bracketSize)
            path.lineTo(radius + bracketSize, bracketSize)
            
            canvas.drawPath(path, bracketPaint)
            canvas.restore()
        }
        
        canvas.restore()
    }

    private fun drawAccentElements(canvas: Canvas) {
        // Rotating particles around striker
        val orbitRadius = width * 0.8f
        val particleCount = 4
        
        for (i in 0 until particleCount) {
            val angle = animationTime * 2f + i * (2f * PI / particleCount)
            val px = x + orbitRadius * cos(angle)
            val py = y + orbitRadius * sin(angle)
            val particleAlpha = (120 + 60 * kotlin.math.sin(animationTime * 3f + i)).toInt()
            
            accentPaint.color = if (i % 2 == 0) 0xFF6200EE.toInt() else 0xFF03DAC5.toInt()
            accentPaint.alpha = particleAlpha
            canvas.drawCircle(px, py, width * 0.06f, accentPaint)
        }
        
        // Scan line effect
        val scanY = y - height * 0.5f + (animationTime * 200f % (height * 1.5f))
        val scanPaint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.FILL
            color = 0xFF03DAC5.toInt()
            alpha = 60
        }
        canvas.drawRect(x - width * 0.6f, scanY, x + width * 0.6f, scanY + 2f, scanPaint)
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