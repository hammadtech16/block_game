package com.blockgame.game

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

enum class ParticleType {
    SPARK,      // Small bright sparks
    SHARD,      // Shape fragments
    GLOW,       // Expanding glow rings
    TRAIL       // Motion trails
}

class Particle(
    var x: Float,
    var y: Float,
    private val baseColor: Int,
    private val initialVelocityX: Float,
    private val initialVelocityY: Float,
    private val baseSize: Float,
    private val type: ParticleType = ParticleType.SPARK
) {
    var velocityX: Float = initialVelocityX
    var velocityY: Float = initialVelocityY
    var life: Float = 1f
    var rotation: Float = 0f
    var angularVelocity: Float = (Random.nextFloat() - 0.5f) * 720f
    var scale: Float = 1f
    var trailPositions: MutableList<Pair<Float, Float>> = mutableListOf()
    var hueShift: Float = Random.nextFloat() * 60f - 30f

    private val paint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
    }

    private val glowPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
    }

    private val shapePath = Path()

    init {
        velocityX = initialVelocityX
        velocityY = initialVelocityY
        
        when (type) {
            ParticleType.SPARK -> {
                life = 0.8f + Random.nextFloat() * 0.4f
                angularVelocity = (Random.nextFloat() - 0.5f) * 1080f
            }
            ParticleType.SHARD -> {
                life = 1f + Random.nextFloat() * 0.5f
                angularVelocity = (Random.nextFloat() - 0.5f) * 720f
                scale = 0.5f + Random.nextFloat() * 0.5f
            }
            ParticleType.GLOW -> {
                life = 0.5f + Random.nextFloat() * 0.3f
                scale = 0.1f
                angularVelocity = 0f
            }
            ParticleType.TRAIL -> {
                life = 0.3f + Random.nextFloat() * 0.2f
                scale = 0.3f + Random.nextFloat() * 0.2f
                angularVelocity = 0f
            }
        }
    }

    fun update(deltaTime: Float): Boolean {
        // Store trail position
        if (type == ParticleType.TRAIL || type == ParticleType.SHARD) {
            trailPositions.add(0, x to y)
            if (trailPositions.size > 8) trailPositions.removeLast()
        }

        x += velocityX * deltaTime
        y += velocityY * deltaTime
        velocityY += 300f * deltaTime // gravity
        velocityX *= 0.97f // air resistance
        velocityY *= 0.97f
        life -= deltaTime * (when (type) {
            ParticleType.GLOW -> 2.5f
            ParticleType.TRAIL -> 3.5f
            else -> 1.2f
        })
        rotation += angularVelocity * deltaTime
        
        // Expand glow particles
        if (type == ParticleType.GLOW) {
            scale += deltaTime * 3f
        }
        
        // Fade shards
        if (type == ParticleType.SHARD) {
            scale *= 0.99f
        }

        return life > 0
    }

    fun draw(canvas: Canvas) {
        if (life <= 0) return

        val alpha = life.coerceIn(0f, 1f)
        val baseAlpha = (255 * alpha).toInt()
        
        canvas.save()
        canvas.translate(x, y)
        canvas.rotate(rotation)
        canvas.scale(scale, scale)

        when (type) {
            ParticleType.SPARK -> drawSpark(canvas, baseAlpha, alpha)
            ParticleType.SHARD -> drawShard(canvas, baseAlpha, alpha)
            ParticleType.GLOW -> drawGlow(canvas, baseAlpha, alpha)
            ParticleType.TRAIL -> drawTrail(canvas, baseAlpha, alpha)
        }

        canvas.restore()
    }

    private fun drawSpark(canvas: Canvas, baseAlpha: Int, alpha: Float) {
        // Draw glow
        glowPaint.color = baseColor
        glowPaint.alpha = (baseAlpha * 0.3f).toInt()
        val glowSize = baseSize * 2f
        canvas.drawCircle(0f, 0f, glowSize, glowPaint)
        
        // Draw core
        paint.color = baseColor
        paint.alpha = baseAlpha
        val coreSize = baseSize * (0.5f + 0.5f * alpha)
        canvas.drawCircle(0f, 0f, coreSize, paint)
        
        // Draw inner highlight
        paint.color = 0xFFFFFFFF.toInt()
        paint.alpha = (baseAlpha * 0.6f).toInt()
        canvas.drawCircle(0f, 0f, coreSize * 0.3f, paint)
    }

    private fun drawShard(canvas: Canvas, baseAlpha: Int, alpha: Float) {
        // Draw trail
        if (trailPositions.size > 1) {
            val trailPaint = Paint().apply {
                isAntiAlias = true
                style = Paint.Style.STROKE
                strokeWidth = baseSize * 0.5f * scale
                color = baseColor
            }
            
            val trailPath = Path()
            var first = true
            for (index in trailPositions.indices) {
                val (tx, ty) = trailPositions[index]
                val trailAlpha = alpha * (1f - index.toFloat() / trailPositions.size)
                trailPaint.alpha = (255 * trailAlpha * 0.4f).toInt()
                if (first) {
                    trailPath.moveTo(tx - x, ty - y)
                    first = false
                } else {
                    trailPath.lineTo(tx - x, ty - y)
                }
            }
            canvas.drawPath(trailPath, trailPaint)
        }
        
        // Draw shard shape (triangle)
        shapePath.reset()
        val s = baseSize * scale
        shapePath.moveTo(0f, -s)
        shapePath.lineTo(-s * 0.6f, s * 0.5f)
        shapePath.lineTo(s * 0.6f, s * 0.5f)
        shapePath.close()
        
        // Gradient fill for shard
        paint.color = baseColor
        paint.alpha = baseAlpha
        canvas.drawPath(shapePath, paint)
        
        // Highlight edge
        paint.color = 0xFFFFFFFF.toInt()
        paint.alpha = (baseAlpha * 0.3f).toInt()
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        canvas.drawPath(shapePath, paint)
        paint.style = Paint.Style.FILL
    }

    private fun drawGlow(canvas: Canvas, baseAlpha: Int, alpha: Float) {
        val glowAlpha = (baseAlpha * 0.4f * (1f - scale * 0.3f)).toInt()
        if (glowAlpha > 0) {
            glowPaint.color = baseColor
            glowPaint.alpha = glowAlpha
            val radius = baseSize * scale * 3f
            canvas.drawCircle(0f, 0f, radius, glowPaint)
        }
    }

    private fun drawTrail(canvas: Canvas, baseAlpha: Int, alpha: Float) {
        paint.color = baseColor
        paint.alpha = (baseAlpha * 0.6f).toInt()
        val s = baseSize * scale * alpha
        canvas.drawCircle(0f, 0f, s, paint)
    }

    companion object {
        fun createExplosion(x: Float, y: Float, color: Int, intensity: Float = 1f): List<Particle> {
            val particles = mutableListOf<Particle>()
            val count = (12 * intensity).toInt()
            
            for (i in 0 until count) {
                val angle = (i * 360f / count + Random.nextFloat() * 15f) * PI.toFloat() / 180f
                val speed = (150f + Random.nextFloat() * 400f) * intensity
                val px = (speed * cos(angle)).toFloat()
                val py = (speed * sin(angle)).toFloat()
                val size = 3f + Random.nextFloat() * 5f
                
                when (Random.nextInt(4)) {
                    0 -> particles.add(Particle(x, y, color, px, py, size, ParticleType.SPARK))
                    1 -> particles.add(Particle(x, y, color, px * 0.7f, py * 0.7f, size * 1.2f, ParticleType.SHARD))
                    2 -> particles.add(Particle(x, y, color, px * 0.3f, py * 0.3f, size * 0.8f, ParticleType.GLOW))
                    else -> particles.add(Particle(x, y, color, px * 0.5f, py * 0.5f, size * 0.5f, ParticleType.TRAIL))
                }
            }
            
            // Add central flash
            particles.add(Particle(x, y, 0xFFFFFFFF.toInt(), 0f, 0f, 20f * intensity, ParticleType.GLOW))
            
            return particles
        }
        
        fun createComboBurst(x: Float, y: Float, color: Int, comboCount: Int): List<Particle> {
            val particles = mutableListOf<Particle>()
            val rings = min(3, comboCount)
            
            for (ring in 1..rings) {
                val ringParticles = 16 + ring * 8
                for (i in 0 until ringParticles) {
                    val angle = (i * 360f / ringParticles) * PI.toFloat() / 180f
                    val speed = 200f + ring * 100f + Random.nextFloat() * 100f
                    val px = (speed * cos(angle)).toFloat()
                    val py = (speed * sin(angle)).toFloat()
                    val size = 4f + Random.nextFloat() * 4f
                    particles.add(Particle(x, y, color, px, py, size, ParticleType.SPARK))
                }
                
                // Ring glow
                particles.add(Particle(x, y, color, 0f, 0f, 30f + ring * 20f, ParticleType.GLOW))
            }
            
            return particles
        }
    }
}