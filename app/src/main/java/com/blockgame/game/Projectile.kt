package com.blockgame.game

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class Projectile(
    var x: Float,
    var y: Float,
    val velocityX: Float,
    val velocityY: Float,
    val radius: Float = 8f,
    val color: Int = 0xFF41D443.toInt()
) {
    private var life: Float = 3f
    private var rotation: Float = 0f
    private var trailPositions: MutableList<Pair<Float, Float>> = mutableListOf()
    
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
    
    private val corePaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
    }

    fun update(deltaTime: Float): Boolean {
        // Store trail
        trailPositions.add(0, x to y)
        if (trailPositions.size > 8) trailPositions.removeLast()
        
        x += velocityX * deltaTime
        y += velocityY * deltaTime
        life -= deltaTime
        rotation += deltaTime * 360f
        
        return life > 0 && y > -radius
    }

    fun draw(canvas: Canvas) {
        // Draw trail
        if (trailPositions.size > 1) {
            for (i in 1 until trailPositions.size) {
                val (tx, ty) = trailPositions[i]
                val alpha = (1f - i.toFloat() / trailPositions.size) * 0.6f
                val trailRadius = radius * 0.5f * (1f - i.toFloat() / trailPositions.size)
                
                if (trailRadius > 0.5f && alpha > 0.02f) {
                    trailPaint.color = color
                    trailPaint.alpha = (255 * alpha).toInt()
                    canvas.drawCircle(tx, ty, trailRadius, trailPaint)
                }
            }
        }
        
        canvas.save()
        canvas.translate(x, y)
        canvas.rotate(rotation)
        
        // Outer glow
        glowPaint.color = color
        glowPaint.alpha = 80
        glowPaint.setShadowLayer(15f, 0f, 0f, color)
        canvas.drawCircle(0f, 0f, radius * 1.3f, glowPaint)
        glowPaint.clearShadowLayer()
        
        // Main body
        mainPaint.color = color
        mainPaint.alpha = 255
        canvas.drawCircle(0f, 0f, radius, mainPaint)
        
        // Inner core - brighter
        corePaint.color = 0xFF6AFF6C.toInt()
        corePaint.alpha = 255
        canvas.drawCircle(0f, 0f, radius * 0.5f, corePaint)
        
        // Center dot - white
        corePaint.color = 0xFFFFFFFF.toInt()
        canvas.drawCircle(0f, 0f, radius * 0.2f, corePaint)
        
        canvas.restore()
    }
    
    fun getBounds(): android.graphics.RectF {
        return android.graphics.RectF(
            x - radius, y - radius,
            x + radius, y + radius
        )
    }
}