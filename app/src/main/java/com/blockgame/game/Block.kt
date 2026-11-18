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
import kotlin.math.sqrt

data class Block(
    var x: Float,
    var y: Float,
    val size: Float,
    val shape: BlockShape,
    val color: Int,
    val points: Int = 10,
    var velocityX: Float = 0f,
    var velocityY: Float = 0f,
    var isDestroyed: Boolean = false,
    var destroyAnimationProgress: Float = 0f,
    var rotation: Float = 0f,
    var scale: Float = 1f,
    var pulsePhase: Float = 0f,
    var glowIntensity: Float = 0f
) {

    private val mainPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
    }

    private val glowPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
    }

    private val borderPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = 2.5f
    }

    private val innerGlowPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
    }

    private val highlightPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
    }

    companion object {
        private val colorCache = mutableMapOf<Int, Int>()
        
        fun getLighterColor(color: Int, factor: Float): Int {
            val key = (color shl 8) or (factor * 255).toInt()
            return colorCache.getOrPut(key) {
                val r = (Color.red(color) * factor + 255 * (1 - factor)).toInt()
                val g = (Color.green(color) * factor + 255 * (1 - factor)).toInt()
                val b = (Color.blue(color) * factor + 255 * (1 - factor)).toInt()
                Color.argb(Color.alpha(color), r.coerceIn(0, 255), g.coerceIn(0, 255), b.coerceIn(0, 255))
            }
        }
        
        fun getDarkerColor(color: Int, factor: Float): Int {
            val key = (color shl 8) or ((1 - factor) * 255).toInt()
            return colorCache.getOrPut(key) {
                val r = (Color.red(color) * factor).toInt()
                val g = (Color.green(color) * factor).toInt()
                val b = (Color.blue(color) * factor).toInt()
                Color.argb(Color.alpha(color), r.coerceIn(0, 255), g.coerceIn(0, 255), b.coerceIn(0, 255))
            }
        }
    }

    fun draw(canvas: Canvas) {
        if (isDestroyed && destroyAnimationProgress >= 1f) return

        canvas.save()
        canvas.translate(x, y)
        canvas.rotate(rotation)
        canvas.scale(scale, scale)

        val alpha = if (isDestroyed) (1f - destroyAnimationProgress) else 1f
        val baseAlpha = (255 * alpha).toInt()
        
        // Update pulse animation for idle blocks
        if (!isDestroyed) {
            pulsePhase += 0.05f
            glowIntensity = 0.3f + 0.2f * kotlin.math.sin(pulsePhase)
        }

        val halfSize = size / 2
        
        // Draw outer glow
        if (!isDestroyed && glowIntensity > 0.3f) {
            drawOuterGlow(canvas, halfSize, baseAlpha)
        }
        
        // Draw main shape with gradient
        drawMainShape(canvas, halfSize, baseAlpha)
        
        // Draw inner highlight/reflection
        drawInnerHighlight(canvas, halfSize, baseAlpha)
        
        // Draw shape-specific details
        drawShapeDetails(canvas, halfSize, baseAlpha)
        
        // Draw border
        drawBorder(canvas, halfSize, baseAlpha)

        canvas.restore()
    }

    private fun drawOuterGlow(canvas: Canvas, halfSize: Float, baseAlpha: Int) {
        val glowRadius = halfSize * (1.3f + glowIntensity * 0.3f)
        val glowAlpha = (baseAlpha * 0.15f * glowIntensity).toInt()
        
        glowPaint.color = color
        glowPaint.alpha = glowAlpha
        glowPaint.setShadowLayer(20f * glowIntensity, 0f, 0f, color)
        
        drawShapePath(canvas, halfSize * 1.1f).let { path ->
            canvas.drawPath(path, glowPaint)
        }
    }

    private fun drawMainShape(canvas: Canvas, halfSize: Float, baseAlpha: Int) {
        val lighterColor = Block.getLighterColor(color, 0.3f)
        val darkerColor = Block.getDarkerColor(color, 0.7f)
        
        val gradient = LinearGradient(
            -halfSize, -halfSize, halfSize, halfSize,
            intArrayOf(lighterColor, color, darkerColor),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP
        )
        
        mainPaint.shader = gradient
        mainPaint.alpha = baseAlpha
        
        drawShapePath(canvas, halfSize).let { path ->
            canvas.drawPath(path, mainPaint)
        }
        
        mainPaint.shader = null
    }

    private fun drawInnerHighlight(canvas: Canvas, halfSize: Float, baseAlpha: Int) {
        val highlightColor = Block.getLighterColor(color, 0.6f)
        highlightPaint.color = highlightColor
        highlightPaint.alpha = (baseAlpha * 0.4f).toInt()
        
        val innerSize = halfSize * 0.5f
        drawShapePath(canvas, innerSize).let { path ->
            canvas.drawPath(path, highlightPaint)
        }
    }

    private fun drawShapeDetails(canvas: Canvas, halfSize: Float, baseAlpha: Int) {
        val detailPaint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.FILL
            alpha = (baseAlpha * 0.3f).toInt()
        }
        
        when (shape) {
            BlockShape.CIRCLE -> {
                // Draw concentric circles for depth
                detailPaint.color = Block.getLighterColor(color, 0.5f)
                canvas.drawCircle(0f, 0f, halfSize * 0.3f, detailPaint)
                detailPaint.color = Block.getDarkerColor(color, 0.5f)
                canvas.drawCircle(0f, 0f, halfSize * 0.15f, detailPaint)
            }
            BlockShape.HEXAGON -> {
                // Draw inner hexagon
                val innerPath = Path()
                for (i in 0..5) {
                    val angle = (i * PI / 3 - PI / 6).toFloat()
                    val px = halfSize * 0.4f * cos(angle)
                    val py = halfSize * 0.4f * sin(angle)
                    if (i == 0) innerPath.moveTo(px, py) else innerPath.lineTo(px, py)
                }
                innerPath.close()
                detailPaint.color = Block.getLighterColor(color, 0.5f)
                canvas.drawPath(innerPath, detailPaint)
            }
            BlockShape.STAR -> {
                // Draw inner star
                val innerPath = Path()
                val innerOuter = halfSize * 0.35f
                val innerInner = halfSize * 0.15f
                for (i in 0..9) {
                    val angle = (i * PI / 5 - PI / 2).toFloat()
                    val radius = if (i % 2 == 0) innerOuter else innerInner
                    val px = radius * cos(angle)
                    val py = radius * sin(angle)
                    if (i == 0) innerPath.moveTo(px, py) else innerPath.lineTo(px, py)
                }
                innerPath.close()
                detailPaint.color = Block.getLighterColor(color, 0.5f)
                canvas.drawPath(innerPath, detailPaint)
            }
            BlockShape.DIAMOND -> {
                // Draw inner diamond
                val innerPath = Path()
                val innerSize = halfSize * 0.4f
                innerPath.moveTo(0f, -innerSize)
                innerPath.lineTo(innerSize, 0f)
                innerPath.lineTo(0f, innerSize)
                innerPath.lineTo(-innerSize, 0f)
                innerPath.close()
                detailPaint.color = Block.getLighterColor(color, 0.5f)
                canvas.drawPath(innerPath, detailPaint)
            }
            BlockShape.TRIANGLE -> {
                // Draw inner triangle
                val innerPath = Path()
                val innerSize = halfSize * 0.4f
                innerPath.moveTo(0f, -innerSize)
                innerPath.lineTo(-innerSize * 0.866f, innerSize * 0.5f)
                innerPath.lineTo(innerSize * 0.866f, innerSize * 0.5f)
                innerPath.close()
                detailPaint.color = Block.getLighterColor(color, 0.5f)
                canvas.drawPath(innerPath, detailPaint)
            }
            BlockShape.SQUARE -> {
                // Draw rounded inner square
                val innerRect = RectF(-halfSize * 0.4f, -halfSize * 0.4f, halfSize * 0.4f, halfSize * 0.4f)
                detailPaint.color = Block.getLighterColor(color, 0.5f)
                canvas.drawRoundRect(innerRect, halfSize * 0.1f, halfSize * 0.1f, detailPaint)
            }
        }
    }

    private fun drawBorder(canvas: Canvas, halfSize: Float, baseAlpha: Int) {
        val borderColor = Block.getLighterColor(color, 0.8f)
        borderPaint.color = borderColor
        borderPaint.alpha = (baseAlpha * 0.6f).toInt()
        borderPaint.strokeWidth = (2.5f * scale).coerceAtMost(4f)
        
        drawShapePath(canvas, halfSize).let { path ->
            canvas.drawPath(path, borderPaint)
        }
    }

    private fun drawShapePath(canvas: Canvas, halfSize: Float): Path {
        val path = Path()
        when (shape) {
            BlockShape.SQUARE -> {
                val radius = halfSize * 0.15f
                val rect = RectF(-halfSize, -halfSize, halfSize, halfSize)
                path.addRoundRect(rect, radius, radius, Path.Direction.CW)
            }
            BlockShape.CIRCLE -> {
                path.addCircle(0f, 0f, halfSize, Path.Direction.CW)
            }
            BlockShape.TRIANGLE -> {
                path.moveTo(0f, -halfSize)
                path.lineTo(-halfSize * 0.866f, halfSize * 0.5f)
                path.lineTo(halfSize * 0.866f, halfSize * 0.5f)
                path.close()
            }
            BlockShape.HEXAGON -> {
                for (i in 0..5) {
                    val angle = (i * PI / 3 - PI / 6).toFloat()
                    val px = halfSize * cos(angle)
                    val py = halfSize * sin(angle)
                    if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
                }
                path.close()
            }
            BlockShape.STAR -> {
                val outerRadius = halfSize
                val innerRadius = halfSize * 0.38f
                for (i in 0..9) {
                    val angle = (i * PI / 5 - PI / 2).toFloat()
                    val radius = if (i % 2 == 0) outerRadius else innerRadius
                    val px = radius * cos(angle)
                    val py = radius * sin(angle)
                    if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
                }
                path.close()
            }
            BlockShape.DIAMOND -> {
                path.moveTo(0f, -halfSize)
                path.lineTo(halfSize, 0f)
                path.lineTo(0f, halfSize)
                path.lineTo(-halfSize, 0f)
                path.close()
            }
        }
        return path
    }

    fun update(deltaTime: Float) {
        if (isDestroyed) {
            destroyAnimationProgress += deltaTime * 3f
            scale = 1f + destroyAnimationProgress * 0.8f
            rotation += deltaTime * 360f
        } else {
            x += velocityX * deltaTime
            y += velocityY * deltaTime
            rotation += deltaTime * 25f
        }
    }

    fun containsPoint(px: Float, py: Float): Boolean {
        val dx = px - x
        val dy = py - y
        val distance = sqrt(dx * dx + dy * dy)
        return distance <= size * 0.65f
    }

    fun startDestroyAnimation() {
        isDestroyed = true
        destroyAnimationProgress = 0f
        scale = 1f
    }

    fun isAnimationComplete(): Boolean {
        return isDestroyed && destroyAnimationProgress >= 1f
    }
}