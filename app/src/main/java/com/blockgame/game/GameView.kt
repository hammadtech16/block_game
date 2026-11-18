package com.blockgame.game

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

class GameView(context: Context, attrs: AttributeSet?) : View(context, attrs) {

    // Game state
    private var gameState = GameState.HOME
    private var score = 0
    private var level = 1
    private var levelScore = 0
    private var blocksToClear = 0
    private var comboCount = 0
    private var lastComboTime = 0L

    // Animation states
    private var overlayAlpha: Float = 0f
    private var popupScale: Float = 0f
    private var popupTargetScale: Float = 0f
    private var screenShakeX: Float = 0f
    private var screenShakeY: Float = 0f
    private var screenShakeTime: Float = 0f
    private var levelTransitionProgress: Float = 0f
    private var isLevelTransitioning = false

    // Theme
    private var isDarkTheme = true

    // Game objects
    private val blocks = mutableListOf<Block>()
    private val particles = mutableListOf<Particle>()
    private val projectiles = mutableListOf<Projectile>()
    private lateinit var striker: Striker

    // Game config
    private var screenWidth = 0f
    private var screenHeight = 0f
    private var gameAreaTop = 0f
    private var gameAreaBottom = 0f
    private var strikerY = 0f

    // Shooting
    private var lastShotTime = 0L
    private val shotCooldown = 300L // ms between shots
    private var isLongPressShooting = false
    private var longPressStartTime = 0L
    private val longPressDelay = 200L // ms before continuous shooting starts
    private var strikerTouching = false

    // Shape statistics
    private val shapeStats = mutableMapOf<BlockShape, Int>().apply {
        for (shape in BlockShape.values()) put(shape, 0)
    }
    private val shapeDestroyed = mutableMapOf<BlockShape, Int>().apply {
        for (shape in BlockShape.values()) put(shape, 0)
    }
    private var totalBlocksSpawned = 0
    private var totalBlocksDestroyed = 0

    // Spawning - SLOWER SPEEDS
    private var spawnTimer = 0f
    private var spawnInterval = 2.0f  // Slower spawn
    private var blockSpeed = 80f      // Much slower block speed
    private var maxBlocks = 15

    // Colors for blocks - vibrant modern palette
    private val blockColors = intArrayOf(
        0xFFFF3B30.toInt(), // Red
        0xFF34C759.toInt(), // Green
        0xFF007AFF.toInt(), // Blue
        0xFFFFCC00.toInt(), // Yellow
        0xFFFF9F0A.toInt(), // Orange
        0xFFFF2D92.toInt(), // Pink
        0xFF5AC8FA.toInt(), // Cyan
        0xFFAF52DE.toInt(), // Purple
    )

    private val blockShapes = BlockShape.values()

    // NEW DESIGN COLOR PALETTE (from redesign images)
    // Background: very dark blue-black #01020E, #0B1022
    // Cards: very dark #030612, #040512
    // Accent teal: #194C61
    // Danger/Progress green: #41D443
    // Progress bar: #2B5149
    // Striker: white #FBFAFD
    // Text: white/light gray
    
    // Dark theme colors
    private val COLOR_BG_DEEPEST_DARK = 0xFF01020E.toInt()    // Top bar
    private val COLOR_BG_DARK_DARK = 0xFF0B1022.toInt()      // Game area
    private val COLOR_BG_CARD_DARK = 0xFF030612.toInt()      // Cards
    private val COLOR_BG_CARD_LIGHT_DARK = 0xFF040512.toInt() // Card variant
    private val COLOR_TEAL_ACCENT_DARK = 0xFF194C61.toInt()  // Teal accent
    private val COLOR_TEAL_BRIGHT_DARK = 0xFF2A7A9A.toInt()  // Brighter teal
    private val COLOR_TEXT_PRIMARY_DARK = 0xFFFFFFFF.toInt()  // White text
    private val COLOR_TEXT_SECONDARY_DARK = 0xFF8A8FA5.toInt() // Muted text
    private val COLOR_TEXT_MUTED_DARK = 0xFF5A5F7A.toInt()    // More muted
    
    // Light theme colors
    private val COLOR_BG_DEEPEST_LIGHT = 0xFFF0F4F8.toInt()
    private val COLOR_BG_DARK_LIGHT = 0xFFE2E8F0.toInt()
    private val COLOR_BG_CARD_LIGHT = 0xFFFFFFFF.toInt()
    private val COLOR_BG_CARD_LIGHT_LIGHT = 0xFFF7FAFC.toInt()
    private val COLOR_TEAL_ACCENT_LIGHT = 0xFF0D9488.toInt()
    private val COLOR_TEAL_BRIGHT_LIGHT = 0xFF14B8A6.toInt()
    private val COLOR_TEXT_PRIMARY_LIGHT = 0xFF1E293B.toInt()
    private val COLOR_TEXT_SECONDARY_LIGHT = 0xFF64748B.toInt()
    private val COLOR_TEXT_MUTED_LIGHT = 0xFF94A3B8.toInt()
    
    // Shared colors
    private val COLOR_GREEN_DANGER = 0xFF41D443.toInt() // Bright green (danger/progress)
    private val COLOR_GREEN_DARK = 0xFF2B5149.toInt()   // Dark green (progress bg)
    private val COLOR_GREEN_DARK_LIGHT = 0xFF86EFAC.toInt() // Light theme progress bg
    private val COLOR_STRIKER_WHITE = 0xFFFBFAFD.toInt() // White striker
    private val COLOR_WHITE = 0xFFFFFFFF.toInt()
    private val COLOR_BLACK = 0xFF000000.toInt()

    // Theme-aware color getters
    private val colorBgDeepest: Int
        get() = if (isDarkTheme) COLOR_BG_DEEPEST_DARK else COLOR_BG_DEEPEST_LIGHT
    private val colorBgDark: Int
        get() = if (isDarkTheme) COLOR_BG_DARK_DARK else COLOR_BG_DARK_LIGHT
    private val colorBgCard: Int
        get() = if (isDarkTheme) COLOR_BG_CARD_DARK else COLOR_BG_CARD_LIGHT
    private val colorBgCardLight: Int
        get() = if (isDarkTheme) COLOR_BG_CARD_LIGHT_DARK else COLOR_BG_CARD_LIGHT_LIGHT
    private val colorTealAccent: Int
        get() = if (isDarkTheme) COLOR_TEAL_ACCENT_DARK else COLOR_TEAL_ACCENT_LIGHT
    private val colorTealBright: Int
        get() = if (isDarkTheme) COLOR_TEAL_BRIGHT_DARK else COLOR_TEAL_BRIGHT_LIGHT
    private val colorTextPrimary: Int
        get() = if (isDarkTheme) COLOR_TEXT_PRIMARY_DARK else COLOR_TEXT_PRIMARY_LIGHT
    private val colorTextSecondary: Int
        get() = if (isDarkTheme) COLOR_TEXT_SECONDARY_DARK else COLOR_TEXT_SECONDARY_LIGHT
    private val colorTextMuted: Int
        get() = if (isDarkTheme) COLOR_TEXT_MUTED_DARK else COLOR_TEXT_MUTED_LIGHT
    private val colorGreenDark: Int
        get() = if (isDarkTheme) COLOR_GREEN_DARK else COLOR_GREEN_DARK_LIGHT
    private val colorGrid: Int
        get() = if (isDarkTheme) 0xFF0D1428.toInt() else 0xFFE2E8F0.toInt()
    private val colorPatternDot: Int
        get() = if (isDarkTheme) 0xFF0D1428.toInt() else 0xFFCBD5E1.toInt()
    private val colorDangerLine: Int = COLOR_GREEN_DANGER

    // Paint objects
    private val scorePaint = Paint().apply {
        color = colorTextPrimary
        textSize = 36f
        isAntiAlias = true
        textAlign = Paint.Align.LEFT
        typeface = android.graphics.Typeface.DEFAULT_BOLD
    }

    private val levelPaint = Paint().apply {
        color = colorTextSecondary
        textSize = 24f
        isAntiAlias = true
        textAlign = Paint.Align.RIGHT
    }

    private val titlePaint = Paint().apply {
        color = colorTextPrimary
        textSize = 56f
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
        typeface = android.graphics.Typeface.DEFAULT_BOLD
    }

    private val subtitlePaint = Paint().apply {
        color = colorTextSecondary
        textSize = 22f
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
    }

    private val buttonPaint = Paint().apply {
        color = colorTealAccent
        isAntiAlias = true
        style = Paint.Style.FILL
    }

    private val buttonTextPaint = Paint().apply {
        color = colorTextPrimary
        textSize = 24f
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
        typeface = android.graphics.Typeface.DEFAULT_BOLD
    }

    private val comboPaint = Paint().apply {
        color = COLOR_GREEN_DANGER
        textSize = 48f
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
        typeface = android.graphics.Typeface.DEFAULT_BOLD
    }

    private val backgroundPaint = Paint().apply {
        color = colorBgDark
    }

    private val gridPaint = Paint().apply {
        color = colorGrid
        strokeWidth = 0.5f
        isAntiAlias = true
    }

    private val dangerLinePaint = Paint().apply {
        color = colorDangerLine
        strokeWidth = 2f
        isAntiAlias = true
        style = Paint.Style.STROKE
    }

    private val cardPaint = Paint().apply {
        color = colorBgCard
        isAntiAlias = true
    }

    private val cardBorderPaint = Paint().apply {
        color = colorTealAccent
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = 1f
    }

    private val progressPaint = Paint().apply {
        color = COLOR_GREEN_DANGER
        isAntiAlias = true
        style = Paint.Style.FILL
    }

    private val progressBgPaint = Paint().apply {
        color = colorGreenDark
        isAntiAlias = true
        style = Paint.Style.FILL
    }

    private val overlayPaint = Paint().apply {
        color = 0xCC000000.toInt()
        isAntiAlias = true
    }

    private val glowPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
    }

    private var lastFrameTime = 0L
    private var isInitialized = false
    private var dpScale = 1f

    // Home page button rects (for touch hit-testing)
    private var homeLevelBtn = RectF()
    private var homeLevelsBtn = RectF()
    private var homeThemeBtn = RectF()

    // Level complete home button rect (for touch hit-testing)
    private var levelCompleteHomeBtn = RectF()

    // Level select button rects (for touch hit-testing)
    private var levelSelectHomeBtn = RectF()
    private val levelSelectBtns = mutableListOf<RectF>()

    enum class GameState {
        HOME,
        PLAYING,
        GAME_OVER,
        LEVEL_COMPLETE,
        LEVEL_SELECT
    }

    init {
        setWillNotDraw(false)
        dpScale = resources.displayMetrics.density
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        screenWidth = w.toFloat()
        screenHeight = h.toFloat()

        gameAreaTop = screenHeight * 0.14f
        gameAreaBottom = screenHeight * 0.76f
        strikerY = gameAreaBottom + 50 * dpScale

        if (!isInitialized) {
            initializeGame()
            isInitialized = true
        }
    }

    private fun initializeGame() {
        score = 0
        level = 1
        levelScore = 0
        comboCount = 0
        blocks.clear()
        particles.clear()
        projectiles.clear()
        overlayAlpha = 0f
        popupScale = 0f
        popupTargetScale = 1f
        lastShotTime = 0
        isLongPressShooting = false
        
        // Reset stats
        for (shape in BlockShape.values()) {
            shapeStats[shape] = 0
            shapeDestroyed[shape] = 0
        }
        totalBlocksSpawned = 0
        totalBlocksDestroyed = 0

        striker = Striker(
            x = screenWidth / 2,
            y = strikerY,
            width = 95 * dpScale,
            height = 120 * dpScale
        )

        spawnInterval = 2.0f
        blockSpeed = 80f
        maxBlocks = 10
        blocksToClear = 15
    }

    fun startGame() {
        if (gameState == GameState.HOME || gameState == GameState.GAME_OVER || gameState == GameState.LEVEL_SELECT) {
            initializeGame()
            gameState = GameState.PLAYING
            lastFrameTime = System.nanoTime()
            popupTargetScale = 0f
            overlayAlpha = 0f
            invalidate()
        } else if (gameState == GameState.LEVEL_COMPLETE) {
            nextLevel()
        }
    }

    private fun nextLevel() {
        level++
        levelScore = 0
        blocks.clear()
        particles.clear()
        projectiles.clear()
        blocksToClear = 15 + level * 5
        isLevelTransitioning = true
        levelTransitionProgress = 0f

        spawnInterval = max(0.6f, 2.0f - level * 0.1f)
        blockSpeed = min(200f, 80f + level * 15f)  // Slower progression
        maxBlocks = min(30, 10 + level * 2)

        gameState = GameState.PLAYING
        lastFrameTime = System.nanoTime()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Apply screen shake
        if (screenShakeTime > 0) {
            canvas.translate(screenShakeX, screenShakeY)
        }

        drawBackground(canvas)
        drawGrid(canvas)
        drawDangerZone(canvas)

        // Draw blocks
        for (block in blocks) {
            block.draw(canvas)
        }

        // Draw projectiles
        for (projectile in projectiles) {
            projectile.draw(canvas)
        }

        // Draw particles
        for (particle in particles) {
            particle.draw(canvas)
        }

        // Draw striker
        if (gameState == GameState.PLAYING) {
            striker.draw(canvas)
        }

        // Draw UI
        drawGameUI(canvas)

        // Draw state overlays with animations
        when (gameState) {
            GameState.HOME -> drawStartOverlay(canvas)
            GameState.GAME_OVER -> drawGameOverOverlay(canvas)
            GameState.LEVEL_COMPLETE -> drawLevelCompleteOverlay(canvas)
            GameState.LEVEL_SELECT -> drawLevelSelectOverlay(canvas)
            GameState.PLAYING -> {} // No overlay during gameplay
        }

        // Draw level transition
        if (isLevelTransitioning) {
            drawLevelTransition(canvas)
        }
    }

    private fun drawBackground(canvas: Canvas) {
        // Base background - deep dark
        canvas.drawRect(0f, 0f, screenWidth, screenHeight, backgroundPaint)

        // Top bar area - deepest dark
        val topGradient = LinearGradient(
            0f, 0f, 0f, gameAreaTop,
            intArrayOf(colorBgDeepest, colorBgDeepest),
            null,
            Shader.TileMode.CLAMP
        )
        backgroundPaint.shader = topGradient
        canvas.drawRect(0f, 0f, screenWidth, gameAreaTop, backgroundPaint)

        // Game area - dark blue
        val gameGradient = LinearGradient(
            0f, gameAreaTop, 0f, gameAreaBottom,
            intArrayOf(0xFF0C1226.toInt(), colorBgDark, 0xFF0A0F1E.toInt()),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP
        )
        backgroundPaint.shader = gameGradient
        canvas.drawRect(0f, gameAreaTop, screenWidth, gameAreaBottom, backgroundPaint)

        // Bottom area - deepest dark
        val bottomGradient = LinearGradient(
            0f, gameAreaBottom, 0f, screenHeight,
            intArrayOf(colorBgDeepest, colorBgDeepest),
            null,
            Shader.TileMode.CLAMP
        )
        backgroundPaint.shader = bottomGradient
        canvas.drawRect(0f, gameAreaBottom, screenWidth, screenHeight, backgroundPaint)

        backgroundPaint.shader = null

        // Subtle pattern dots in top/bottom areas only
        drawPatternDots(canvas)

        // Accent line at top of game area
        drawTopAccentLine(canvas)
    }

    private fun drawPatternDots(canvas: Canvas) {
        val dotPaint = Paint().apply {
            color = colorPatternDot
            isAntiAlias = true
            style = Paint.Style.FILL
        }
        val spacing = (60 * dpScale).toInt()
        
        // Top area dots
        var x = 0f
        while (x <= screenWidth) {
            var y = 0f
            while (y <= gameAreaTop) {
                canvas.drawCircle(x, y, 0.5f * dpScale, dotPaint)
                y += spacing
            }
            x += spacing
        }
        
        // Bottom area dots
        x = 0f
        while (x <= screenWidth) {
            var y = gameAreaBottom
            while (y <= screenHeight) {
                canvas.drawCircle(x, y, 0.5f * dpScale, dotPaint)
                y += spacing
            }
            x += spacing
        }
    }

    private fun drawTopAccentLine(canvas: Canvas) {
        val linePaint = Paint().apply {
            color = colorTealAccent
            strokeWidth = 1.5f * dpScale
            isAntiAlias = true
            alpha = 180
        }
        val y = gameAreaTop
        canvas.drawLine(0f, y, screenWidth, y, linePaint)
        
        // Glow
        linePaint.strokeWidth = 6f * dpScale
        linePaint.alpha = 40
        canvas.drawLine(0f, y, screenWidth, y, linePaint)
    }

    private var animationTime: Float = 0f

    private fun drawGrid(canvas: Canvas) {
        val spacing = 50 * dpScale
        val linePaint = Paint().apply {
            color = colorGrid
            strokeWidth = 0.5f
            isAntiAlias = true
        }
        
        var x = 0f
        while (x <= screenWidth) {
            canvas.drawLine(x, gameAreaTop, x, gameAreaBottom, linePaint)
            x += spacing
        }
        var y = gameAreaTop
        while (y <= gameAreaBottom) {
            canvas.drawLine(0f, y, screenWidth, y, linePaint)
            y += spacing
        }
    }

    private fun drawDangerZone(canvas: Canvas) {
        val lineY = strikerY - 35 * dpScale
        
        // Main danger line - bright green
        val dashPath = Path()
        val dashLength = 25 * dpScale
        val gapLength = 12 * dpScale
        var currentX = 0f
        var drawDash = true
        
        while (currentX < screenWidth) {
            if (drawDash) {
                val endX = min(currentX + dashLength, screenWidth)
                dashPath.moveTo(currentX, lineY)
                dashPath.lineTo(endX, lineY)
            }
            currentX += if (drawDash) dashLength else gapLength
            drawDash = !drawDash
        }
        
        dangerLinePaint.color = colorDangerLine
        dangerLinePaint.strokeWidth = 2.5f * dpScale
        dangerLinePaint.alpha = (180 + 70 * kotlin.math.sin(animationTime * 5f)).toInt()
        canvas.drawPath(dashPath, dangerLinePaint)
        
        // Warning glow below line
        val glowPaint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.FILL
            color = colorDangerLine
            alpha = (20 + 15 * kotlin.math.sin(animationTime * 5f)).toInt()
        }
        canvas.drawRect(0f, lineY, screenWidth, strikerY + 15 * dpScale, glowPaint)
        
        // Warning text
        val warningPaint = Paint().apply {
            color = colorDangerLine
            textSize = 14 * dpScale
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            alpha = (200 + 50 * kotlin.math.sin(animationTime * 4f)).toInt()
        }
        canvas.drawText("⚠ DANGER ZONE ⚠", screenWidth / 2, lineY - 10 * dpScale, warningPaint)
    }

    private fun drawGameUI(canvas: Canvas) {
        val padding = 20 * dpScale
        val cardHeight = 70 * dpScale
        val cornerRadius = 14 * dpScale
        val cardGap = 12 * dpScale
        val cardWidth = (screenWidth - padding * 2 - cardGap) / 2
        
        // Score card (left)
        val scoreCardRect = RectF(
            padding, padding,
            padding + cardWidth, padding + cardHeight
        )
        drawCard(canvas, scoreCardRect, cornerRadius, true)
        
        // Score text
        val labelPaint = Paint().apply {
            color = colorTextMuted
            textSize = 13 * dpScale
            isAntiAlias = true
            textAlign = Paint.Align.LEFT
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            letterSpacing = 0.5f
        }
        val valuePaint = Paint(scorePaint).apply { 
            textSize = 28 * dpScale 
            color = colorTextPrimary
        }
        
        canvas.drawText("SCORE", scoreCardRect.left + 18 * dpScale, scoreCardRect.top + 22 * dpScale, labelPaint)
        canvas.drawText("$score", scoreCardRect.left + 18 * dpScale, scoreCardRect.top + 54 * dpScale, valuePaint)
        
        // Level card (right)
        val levelCardRect = RectF(
            padding + cardWidth + cardGap, padding,
            screenWidth - padding, padding + cardHeight
        )
        drawCard(canvas, levelCardRect, cornerRadius, false)
        
        labelPaint.textAlign = Paint.Align.RIGHT
        valuePaint.textAlign = Paint.Align.RIGHT
        
        canvas.drawText("LEVEL", levelCardRect.right - 18 * dpScale, levelCardRect.top + 22 * dpScale, labelPaint)
        canvas.drawText("$level", levelCardRect.right - 18 * dpScale, levelCardRect.top + 54 * dpScale, valuePaint)
        
        // Progress bar at bottom of game area
        drawProgressBar(canvas)
    }

    private fun drawCard(canvas: Canvas, rect: RectF, radius: Float, isLeft: Boolean) {
        // Card background
        canvas.drawRoundRect(rect, radius, radius, cardPaint)
        
        // Card border - teal accent
        cardBorderPaint.color = colorTealAccent
        cardBorderPaint.strokeWidth = 1.5f * dpScale
        cardBorderPaint.alpha = 150
        canvas.drawRoundRect(rect, radius, radius, cardBorderPaint)
        
        // Top accent line
        val accentLine = Paint().apply {
            color = colorTealBright
            strokeWidth = 3 * dpScale
            isAntiAlias = true
            alpha = 220
        }
        val topRect = RectF(rect.left, rect.top, rect.right, rect.top + 3 * dpScale)
        canvas.drawRect(topRect, accentLine)
        
        // Subtle inner glow at top
        val innerGlow = Paint().apply {
            color = colorTealAccent
            isAntiAlias = true
            alpha = 30
        }
        val glowRect = RectF(rect.left + 2 * dpScale, rect.top + 3 * dpScale, rect.right - 2 * dpScale, rect.top + 10 * dpScale)
        canvas.drawRect(glowRect, innerGlow)
    }

    private fun drawProgressBar(canvas: Canvas) {
        val barHeight = 5 * dpScale
        val barWidth = screenWidth * 0.85f
        val barX = (screenWidth - barWidth) / 2
        val barY = gameAreaBottom - 18 * dpScale
        val radius = barHeight / 2
        
        // Background
        val bgRect = RectF(barX, barY, barX + barWidth, barY + barHeight)
        canvas.drawRoundRect(bgRect, radius, radius, progressBgPaint)
        
        // Progress
        val progress = (levelScore.toFloat() / (blocksToClear * 10)).coerceIn(0f, 1f)
        val progressWidth = barWidth * progress
        if (progressWidth > 0) {
            val progressRect = RectF(barX, barY, barX + progressWidth, barY + barHeight)
            canvas.drawRoundRect(progressRect, radius, radius, progressPaint)
            
            // Progress glow
            progressPaint.setShadowLayer(6 * dpScale, 0f, 0f, colorDangerLine)
            canvas.drawRoundRect(progressRect, radius, radius, progressPaint)
            progressPaint.clearShadowLayer()
        }
        
        // Progress text
        val progressTextPaint = Paint().apply {
            color = colorTextMuted
            textSize = 12 * dpScale
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("$levelScore / ${blocksToClear * 10}", screenWidth / 2, barY - 6 * dpScale, progressTextPaint)
    }

    private fun drawComboText(canvas: Canvas) {
        if (comboCount > 1 && System.currentTimeMillis() - lastComboTime < 2000) {
            val alpha = (1f - (System.currentTimeMillis() - lastComboTime) / 2000f)
            comboPaint.alpha = (255 * alpha).toInt()
            comboPaint.setShadowLayer(8 * dpScale, 0f, 0f, colorDangerLine)
            
            val y = gameAreaTop + 80 * dpScale
            canvas.drawText("COMBO x$comboCount!", screenWidth / 2, y, comboPaint)
            comboPaint.clearShadowLayer()
        }
}
    
    private fun drawShapeStats(canvas: Canvas) {
        if (gameState != GameState.PLAYING && gameState != GameState.LEVEL_COMPLETE) return
        
        val total = totalBlocksSpawned
        if (total == 0) return
        
        val statsPaint = Paint().apply {
            color = colorTextSecondary
            textSize = 11 * dpScale
            isAntiAlias = true
            textAlign = Paint.Align.LEFT
        }
        
        val statsValuePaint = Paint().apply {
            color = colorTextPrimary
            textSize = 11 * dpScale
            isAntiAlias = true
            textAlign = Paint.Align.RIGHT
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        
        val startX = 12 * dpScale
        val startY = gameAreaBottom + 25 * dpScale
        val lineHeight = 16 * dpScale
        var y = startY
        
        // Title
        val titlePaint = Paint().apply {
            color = colorTealAccent
            textSize = 13 * dpScale
            isAntiAlias = true
            textAlign = Paint.Align.LEFT
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        canvas.drawText("SHAPE STATS", startX, y, titlePaint)
        y += lineHeight + 4 * dpScale
        
        // Sort shapes by spawn count
        val sortedShapes = BlockShape.values().sortedByDescending { shapeStats[it] ?: 0 }
        
        for (shape in sortedShapes) {
            val spawned = shapeStats[shape] ?: 0
            val destroyed = shapeDestroyed[shape] ?: 0
            val percentage = if (spawned > 0) (destroyed * 100 / spawned) else 0
            
            if (spawned > 0) {
                val shapeName = when (shape) {
                    BlockShape.SQUARE -> "SQUARE"
                    BlockShape.CIRCLE -> "CIRCLE"
                    BlockShape.TRIANGLE -> "TRIANGLE"
                    BlockShape.HEXAGON -> "HEXAGON"
                    BlockShape.STAR -> "STAR"
                    BlockShape.DIAMOND -> "DIAMOND"
                }
                
                canvas.drawText("$shapeName:", startX, y, statsPaint)
                canvas.drawText("$destroyed/$spawned ($percentage%)", screenWidth - 12 * dpScale, y, statsValuePaint)
                y += lineHeight
            }
        }
        
        // Total
        val totalPaint = Paint().apply {
            color = colorTealBright
            textSize = 12 * dpScale
            isAntiAlias = true
            textAlign = Paint.Align.LEFT
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        val totalValuePaint = Paint(totalPaint).apply { textAlign = Paint.Align.RIGHT }
        val totalPct = if (total > 0) (totalBlocksDestroyed * 100 / total) else 0
        canvas.drawText("TOTAL:", startX, y + 4 * dpScale, totalPaint)
        canvas.drawText("$totalBlocksDestroyed/$total ($totalPct%)", screenWidth - 12 * dpScale, y + 4 * dpScale, totalValuePaint)
    }

    private fun drawStartOverlay(canvas: Canvas) {
        overlayAlpha = min(1f, overlayAlpha + 0.04f)
        overlayPaint.alpha = (200 * overlayAlpha).toInt()
        canvas.drawRect(0f, 0f, screenWidth, screenHeight, overlayPaint)
        
        popupScale = popupScale + (popupTargetScale - popupScale) * 0.18f
        
        canvas.save()
        canvas.translate(screenWidth / 2, screenHeight / 2)
        canvas.scale(popupScale, popupScale)
        canvas.translate(-screenWidth / 2, -screenHeight / 2)
        
        drawHomePage(canvas)
        canvas.restore()
    }

    private fun drawHomePage(canvas: Canvas) {
        // Soft gradient background top to bottom
        val bgPaint = Paint().apply {
            shader = LinearGradient(
                0f, 0f, 0f, screenHeight,
                intArrayOf(colorBgDeepest, colorBgDark),
                null, Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, screenWidth, screenHeight, bgPaint)

        // Subtle pattern dots
        val dotPaint = Paint().apply {
            color = colorPatternDot
            isAntiAlias = true
            style = Paint.Style.FILL
        }
        val spacing = (70 * dpScale).toInt()
        var x = 0f
        while (x <= screenWidth) {
            var y = 0f
            while (y <= screenHeight) {
                canvas.drawCircle(x, y, 0.5f * dpScale, dotPaint)
                y += spacing
            }
            x += spacing
        }

        val centerX = screenWidth / 2
        val centerY = screenHeight * 0.42f

        // Center icon - large stylized block
        val iconSize = 90f * dpScale
        val iconPaint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.FILL
            shader = LinearGradient(
                centerX - iconSize / 2, centerY - iconSize / 2,
                centerX + iconSize / 2, centerY + iconSize / 2,
                intArrayOf(colorTealAccent, colorTealBright, colorDangerLine),
                null, Shader.TileMode.CLAMP
            )
        }
        val iconPath = Path()
        val h = iconSize / 2
        iconPath.moveTo(centerX, centerY - h)
        iconPath.lineTo(centerX + h * 0.6f, centerY + h * 0.3f)
        iconPath.lineTo(centerX, centerY + h)
        iconPath.lineTo(centerX - h * 0.6f, centerY + h * 0.3f)
        iconPath.close()
        canvas.drawPath(iconPath, iconPaint)

        // Icon glow
        val glowPaint = Paint().apply {
            isAntiAlias = true
            color = colorTealAccent
            alpha = 60
            setShadowLayer(24f, 0f, 0f, colorTealAccent)
        }
        canvas.drawPath(iconPath, glowPaint)

        // Title
        canvas.drawText("BLOCK GAME", centerX, centerY + 120 * dpScale, titlePaint)
        canvas.drawText("Match colors • Destroy blocks", centerX, centerY + 150 * dpScale, subtitlePaint)

        // Buttons area
        val buttonWidth = 260f * dpScale
        val buttonHeight = 56f * dpScale
        val buttonGap = 16f * dpScale
        val startY = centerY + 210 * dpScale

        // Current level button
        homeLevelBtn = RectF(
            centerX - buttonWidth / 2, startY,
            centerX + buttonWidth / 2, startY + buttonHeight
        )
        drawHomeButton(canvas, homeLevelBtn, "LEVEL: $level", colorTealAccent)

        // Levels button
        homeLevelsBtn = RectF(
            centerX - buttonWidth / 2, startY + buttonHeight + buttonGap,
            centerX + buttonWidth / 2, startY + buttonHeight * 2 + buttonGap
        )
        drawHomeButton(canvas, homeLevelsBtn, "LEVELS", colorTextPrimary)

        // Change theme button
        homeThemeBtn = RectF(
            centerX - buttonWidth / 2, startY + (buttonHeight + buttonGap) * 2,
            centerX + buttonWidth / 2, startY + (buttonHeight * 2 + buttonGap * 2)
        )
        val themeLabel = if (isDarkTheme) "THEME: DARK" else "THEME: LIGHT"
        drawHomeButton(canvas, homeThemeBtn, themeLabel, colorTextSecondary)

        // Start game hint
        val hintPaint = Paint().apply {
            color = colorTextMuted
            textSize = 15 * dpScale
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            alpha = (120 + 60 * kotlin.math.sin(animationTime * 3f)).toInt()
        }
        canvas.drawText("Tap any button to begin", centerX, startY + (buttonHeight + buttonGap) * 3 + 40 * dpScale, hintPaint)
    }

    private fun drawHomeButton(canvas: Canvas, rect: RectF, label: String, textColor: Int) {
        val radius = rect.height() / 2
        // Button background
        val bgPaint = Paint().apply {
            color = colorBgCard
            isAntiAlias = true
        }
        canvas.drawRoundRect(rect, radius, radius, bgPaint)
        // Button border
        val borderPaint = Paint().apply {
            color = colorTealAccent
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeWidth = 1.5f * dpScale
            alpha = 200
        }
        canvas.drawRoundRect(rect, radius, radius, borderPaint)
        // Top accent
        val accent = Paint().apply {
            color = colorTealBright
            strokeWidth = 3 * dpScale
            isAntiAlias = true
            alpha = 220
        }
        canvas.drawRect(rect.left, rect.top, rect.right, rect.top + 3 * dpScale, accent)
        // Label
        val labelPaint = Paint().apply {
            color = textColor
            textSize = 20 * dpScale
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        canvas.drawText(label, rect.centerX(), rect.centerY() + 8 * dpScale, labelPaint)
    }

    private fun drawGameOverOverlay(canvas: Canvas) {
        overlayAlpha = min(1f, overlayAlpha + 0.05f)
        overlayPaint.alpha = (220 * overlayAlpha).toInt()
        canvas.drawRect(0f, 0f, screenWidth, screenHeight, overlayPaint)
        
        popupScale = popupScale + (popupTargetScale - popupScale) * 0.15f
        
        canvas.save()
        canvas.translate(screenWidth / 2, screenHeight / 2)
        canvas.scale(popupScale, popupScale)
        canvas.translate(-screenWidth / 2, -screenHeight / 2)
        
        drawGameOverPopup(canvas)
        canvas.restore()
    }

    private fun drawGameOverPopup(canvas: Canvas) {
        val popupWidth = screenWidth * 0.88f
        val popupHeight = screenHeight * 0.55f
        val popupX = (screenWidth - popupWidth) / 2
        val popupY = (screenHeight - popupHeight) / 2
        val radius = 24 * dpScale
        
        val popupRect = RectF(popupX, popupY, popupX + popupWidth, popupY + popupHeight)
        
        // Shadow
        val shadowPaint = Paint().apply {
            color = COLOR_BLACK
            isAntiAlias = true
            setShadowLayer(30 * dpScale, 0f, 10 * dpScale, 0x90000000.toInt())
        }
        canvas.drawRoundRect(popupRect, radius, radius, shadowPaint)
        
        // Background - very dark
        val popupBgPaint = Paint().apply {
            color = colorBgDeepest
            isAntiAlias = true
        }
        canvas.drawRoundRect(popupRect, radius, radius, popupBgPaint)
        
        // Top accent bar - danger green
        val accentRect = RectF(popupX, popupY, popupX + popupWidth, popupY + 4 * dpScale)
        val accentPaint = Paint().apply {
            color = colorDangerLine
            isAntiAlias = true
        }
        canvas.drawRect(accentRect, accentPaint)
        
        // Game Over title
        val gameOverPaint = Paint(titlePaint).apply {
            color = COLOR_GREEN_DANGER
            textSize = 44 * dpScale
        }
        canvas.drawText("GAME OVER", screenWidth / 2, popupY + 90 * dpScale, gameOverPaint)
        
        // Skull/icon
        drawSkullIcon(canvas, screenWidth / 2, popupY + 170 * dpScale)
        
        // Stats
        val statPaint = Paint().apply {
            color = colorTextPrimary
            textSize = 26 * dpScale
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        
        val labelPaint = Paint().apply {
            color = colorTextMuted
            textSize = 16 * dpScale
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            letterSpacing = 0.3f
        }
        
        canvas.drawText("FINAL SCORE", screenWidth / 2, popupY + 270 * dpScale, labelPaint)
        canvas.drawText("$score", screenWidth / 2, popupY + 305 * dpScale, statPaint)
        
        canvas.drawText("LEVEL REACHED", screenWidth / 2, popupY + 350 * dpScale, labelPaint)
        canvas.drawText("$level", screenWidth / 2, popupY + 385 * dpScale, statPaint)
        
        // Restart button
        val buttonWidth = 200 * dpScale
        val buttonHeight = 52 * dpScale
        val buttonX = (screenWidth - buttonWidth) / 2
        val buttonY = popupY + popupHeight - 85 * dpScale
        val buttonRect = RectF(buttonX, buttonY, buttonX + buttonWidth, buttonY + buttonHeight)
        val buttonRadius = buttonHeight / 2
        
        // Button background - danger green
        buttonPaint.color = colorDangerLine
        buttonPaint.setShadowLayer(10 * dpScale, 0f, 4 * dpScale, 0x6041D443.toInt())
        canvas.drawRoundRect(buttonRect, buttonRadius, buttonRadius, buttonPaint)
        buttonPaint.clearShadowLayer()
        
        // Button border
        val buttonBorderPaint = Paint().apply {
            color = 0xFF6AFF6C.toInt()
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeWidth = 1.5f * dpScale
            alpha = 200
        }
        canvas.drawRoundRect(buttonRect, buttonRadius, buttonRadius, buttonBorderPaint)
        
        canvas.drawText("TAP TO RETRY", screenWidth / 2, buttonY + buttonHeight * 0.62f, buttonTextPaint)
    }

    private fun drawSkullIcon(canvas: Canvas, cx: Float, cy: Float) {
        val size = 45 * dpScale
        val paint = Paint().apply {
            color = colorDangerLine
            isAntiAlias = true
            style = Paint.Style.FILL
            alpha = 200
        }
        
        // Simple skull shape
        canvas.drawCircle(cx, cy - size * 0.1f, size * 0.42f, paint) // Head
        canvas.drawRect(cx - size * 0.32f, cy + size * 0.18f, cx + size * 0.32f, cy + size * 0.45f, paint) // Jaw
        
        // Eyes
        paint.color = colorBgDeepest
        canvas.drawCircle(cx - size * 0.14f, cy - size * 0.1f, size * 0.07f, paint)
        canvas.drawCircle(cx + size * 0.14f, cy - size * 0.1f, size * 0.07f, paint)
        
        // Nose
        canvas.drawCircle(cx, cy + size * 0.08f, size * 0.04f, paint)
    }

    private fun drawLevelCompleteOverlay(canvas: Canvas) {
        overlayAlpha = min(1f, overlayAlpha + 0.04f)
        overlayPaint.alpha = (200 * overlayAlpha).toInt()
        canvas.drawRect(0f, 0f, screenWidth, screenHeight, overlayPaint)
        
        popupScale = popupScale + (popupTargetScale - popupScale) * 0.15f
        
        canvas.save()
        canvas.translate(screenWidth / 2, screenHeight / 2)
        canvas.scale(popupScale, popupScale)
        canvas.translate(-screenWidth / 2, -screenHeight / 2)
        
        drawLevelCompletePopup(canvas)
        canvas.restore()
    }

    private fun drawLevelCompletePopup(canvas: Canvas) {
        val popupWidth = screenWidth * 0.88f
        val popupHeight = screenHeight * 0.6f
        val popupX = (screenWidth - popupWidth) / 2
        val popupY = (screenHeight - popupHeight) / 2
        val radius = 24 * dpScale
        
        val popupRect = RectF(popupX, popupY, popupX + popupWidth, popupY + popupHeight)
        
        // Shadow
        val shadowPaint = Paint().apply {
            color = COLOR_BLACK
            isAntiAlias = true
            setShadowLayer(25 * dpScale, 0f, 8 * dpScale, 0x80000000.toInt())
        }
        canvas.drawRoundRect(popupRect, radius, radius, shadowPaint)
        
        // Background - dark card
        val popupBgPaint = Paint().apply {
            color = colorBgCard
            isAntiAlias = true
        }
        canvas.drawRoundRect(popupRect, radius, radius, popupBgPaint)
        
        // Border - success green
        cardBorderPaint.color = colorDangerLine
        cardBorderPaint.strokeWidth = 2 * dpScale
        cardBorderPaint.alpha = 220
        canvas.drawRoundRect(popupRect, radius, radius, cardBorderPaint)
        cardBorderPaint.strokeWidth = 1.5f * dpScale
        cardBorderPaint.color = colorTealAccent
        cardBorderPaint.alpha = 150
        
        // Top accent bar - success gradient
        val accentRect = RectF(popupX, popupY, popupX + popupWidth, popupY + 4 * dpScale)
        val accentPaint = Paint().apply {
            isAntiAlias = true
            shader = LinearGradient(
                popupX, popupY, popupX + popupWidth, popupY,
                intArrayOf(colorDangerLine, 0xFF6AFF6C.toInt(), colorTealBright),
                null, Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(accentRect, accentPaint)
        
        // Level Complete title
        val completePaint = Paint(titlePaint).apply {
            color = colorDangerLine
            textSize = 38 * dpScale
        }
        canvas.drawText("LEVEL COMPLETE!", screenWidth / 2, popupY + 90 * dpScale, completePaint)
        
        // Level number
        val levelNumPaint = Paint().apply {
            color = colorTextSecondary
            textSize = 22 * dpScale
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("Level $level", screenWidth / 2, popupY + 130 * dpScale, levelNumPaint)
        
        // Trophy/star icon
        drawTrophyIcon(canvas, screenWidth / 2, popupY + 200 * dpScale)
        
        // Points earned
        val pointsPaint = Paint().apply {
            color = 0xFFFFD600.toInt() // Gold/amber
            textSize = 44 * dpScale
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        canvas.drawText("+$levelScore", screenWidth / 2, popupY + 300 * dpScale, pointsPaint)
        canvas.drawText("POINTS EARNED", screenWidth / 2, popupY + 335 * dpScale,
            Paint().apply { color = colorTextMuted; textSize = 16 * dpScale; isAntiAlias = true; textAlign = Paint.Align.CENTER })
        
        // Total score
        canvas.drawText("Total Score: $score", screenWidth / 2, popupY + 375 * dpScale,
            Paint().apply { color = colorTextPrimary; textSize = 22 * dpScale; isAntiAlias = true; textAlign = Paint.Align.CENTER })
        
        // Continue button
        val buttonWidth = 220 * dpScale
        val buttonHeight = 52 * dpScale
        val buttonX = (screenWidth - buttonWidth) / 2
        val buttonY = popupY + popupHeight - 132 * dpScale
        val buttonRect = RectF(buttonX, buttonY, buttonX + buttonWidth, buttonY + buttonHeight)
        val buttonRadius = buttonHeight / 2
        
        // Button background - teal
        buttonPaint.color = colorTealAccent
        buttonPaint.setShadowLayer(10 * dpScale, 0f, 4 * dpScale, 0x60194C61.toInt())
        canvas.drawRoundRect(buttonRect, buttonRadius, buttonRadius, buttonPaint)
        buttonPaint.clearShadowLayer()
        
        // Button border
        val buttonBorderPaint = Paint().apply {
            color = colorTealBright
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeWidth = 1.5f * dpScale
            alpha = 200
        }
        canvas.drawRoundRect(buttonRect, buttonRadius, buttonRadius, buttonBorderPaint)
        
        canvas.drawText("CONTINUE TO LEVEL ${level + 1}", screenWidth / 2, buttonY + buttonHeight * 0.62f, buttonTextPaint)
        
        // Home button
        val homeButtonY = buttonY + buttonHeight + 16 * dpScale
        val homeButtonRect = RectF(buttonX, homeButtonY, buttonX + buttonWidth, homeButtonY + buttonHeight)
        val homeButtonRadius = buttonHeight / 2
        levelCompleteHomeBtn = homeButtonRect
        buttonPaint.color = colorBgCard
        buttonPaint.setShadowLayer(8 * dpScale, 0f, 4 * dpScale, 0x40000000.toInt())
        canvas.drawRoundRect(homeButtonRect, homeButtonRadius, homeButtonRadius, buttonPaint)
        buttonPaint.clearShadowLayer()
        val homeBorderPaint = Paint().apply {
            color = colorTealAccent
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeWidth = 1.5f * dpScale
            alpha = 180
        }
        canvas.drawRoundRect(homeButtonRect, homeButtonRadius, homeButtonRadius, homeBorderPaint)
        canvas.drawText("HOME", screenWidth / 2, homeButtonY + buttonHeight * 0.62f, buttonTextPaint)
    }

    private fun drawLevelSelectOverlay(canvas: Canvas) {
        overlayAlpha = min(1f, overlayAlpha + 0.04f)
        overlayPaint.alpha = (200 * overlayAlpha).toInt()
        canvas.drawRect(0f, 0f, screenWidth, screenHeight, overlayPaint)

        popupScale = popupScale + (popupTargetScale - popupScale) * 0.18f

        canvas.save()
        canvas.translate(screenWidth / 2, screenHeight / 2)
        canvas.scale(popupScale, popupScale)
        canvas.translate(-screenWidth / 2, -screenHeight / 2)

        // Background card
        val cardW = screenWidth * 0.85f
        val cardH = screenHeight * 0.75f
        val cardX = (screenWidth - cardW) / 2
        val cardY = (screenHeight - cardH) / 2
        val cardRect = RectF(cardX, cardY, cardX + cardW, cardY + cardH)
        val radius = 24 * dpScale

        val shadow = Paint().apply {
            color = COLOR_BLACK
            isAntiAlias = true
            setShadowLayer(25 * dpScale, 0f, 8 * dpScale, 0x80000000.toInt())
        }
        canvas.drawRoundRect(cardRect, radius, radius, shadow)

        val cardBg = Paint().apply {
            color = colorBgCard
            isAntiAlias = true
        }
        canvas.drawRoundRect(cardRect, radius, radius, cardBg)

        cardBorderPaint.color = colorTealAccent
        cardBorderPaint.strokeWidth = 1.5f * dpScale
        cardBorderPaint.alpha = 200
        canvas.drawRoundRect(cardRect, radius, radius, cardBorderPaint)

        canvas.drawText("SELECT LEVEL", screenWidth / 2, cardY + 70 * dpScale, titlePaint)

        // Level buttons grid
        levelSelectBtns.clear()
        val cols = 3
        val btnW = 70f * dpScale
        val btnH = 70f * dpScale
        val gap = 16f * dpScale
        val totalW = cols * btnW + (cols - 1) * gap
        val startX = (screenWidth - totalW) / 2
        val startY = cardY + 130 * dpScale
        val rows = 4

        for (row in 0 until rows) {
            for (col in 0 until cols) {
                val lvl = row * cols + col + 1
                if (lvl > 20) break
                val bx = startX + col * (btnW + gap)
                val by = startY + row * (btnH + gap)
                val rect = RectF(bx, by, bx + btnW, by + btnH)
                levelSelectBtns.add(rect)
                val unlocked = lvl <= level
                val btnPaint = Paint().apply {
                    color = if (unlocked) colorTealAccent else colorTextMuted
                    isAntiAlias = true
                }
                canvas.drawRoundRect(rect, radius / 3, radius / 3, btnPaint)
                val txtPaint = Paint().apply {
                    color = if (unlocked) colorTextPrimary else colorTextMuted
                    textSize = 24 * dpScale
                    isAntiAlias = true
                    textAlign = Paint.Align.CENTER
                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                }
                canvas.drawText("$lvl", rect.centerX(), rect.centerY() + 8 * dpScale, txtPaint)
            }
        }

        // Home / back button
        val homeBtnW = 160f * dpScale
        val homeBtnH = 48f * dpScale
        levelSelectHomeBtn = RectF(
            screenWidth / 2 - homeBtnW / 2,
            cardY + cardH - 70 * dpScale,
            screenWidth / 2 + homeBtnW / 2,
            cardY + cardH - 70 * dpScale + homeBtnH
        )
        drawHomeButton(canvas, levelSelectHomeBtn, "HOME", colorTealAccent)

        canvas.restore()
    }

    private fun drawTrophyIcon(canvas: Canvas, cx: Float, cy: Float) {
        val size = 55 * dpScale
        val paint = Paint().apply {
            color = 0xFFFFD600.toInt() // Gold
            isAntiAlias = true
            style = Paint.Style.FILL
        }
        
        // Cup
        val cupPath = Path()
        cupPath.moveTo(cx - size * 0.28f, cy - size * 0.2f)
        cupPath.lineTo(cx - size * 0.38f, cy + size * 0.3f)
        cupPath.lineTo(cx + size * 0.38f, cy + size * 0.3f)
        cupPath.lineTo(cx + size * 0.28f, cy - size * 0.2f)
        cupPath.close()
        canvas.drawPath(cupPath, paint)
        
        // Handles
        val handlePaint = Paint(paint).apply { style = Paint.Style.STROKE; strokeWidth = 3.5f * dpScale }
        val leftHandle = Path()
        leftHandle.moveTo(cx - size * 0.28f, cy - size * 0.1f)
        leftHandle.quadTo(cx - size * 0.48f, cy, cx - size * 0.38f, cy + size * 0.2f)
        canvas.drawPath(leftHandle, handlePaint)
        
        val rightHandle = Path()
        rightHandle.moveTo(cx + size * 0.28f, cy - size * 0.1f)
        rightHandle.quadTo(cx + size * 0.48f, cy, cx + size * 0.38f, cy + size * 0.2f)
        canvas.drawPath(rightHandle, handlePaint)
        
        // Base
        val basePaint = Paint(paint).apply { color = 0xFFE6C000.toInt(); style = Paint.Style.FILL }
        canvas.drawRect(cx - size * 0.42f, cy + size * 0.3f, cx + size * 0.42f, cy + size * 0.38f, basePaint)
        canvas.drawRect(cx - size * 0.28f, cy + size * 0.38f, cx + size * 0.28f, cy + size * 0.42f, basePaint)
        
        // Shine
        val shinePaint = Paint().apply {
            color = COLOR_WHITE
            isAntiAlias = true
            alpha = 100
            style = Paint.Style.FILL
        }
        val shinePath = Path()
        shinePath.moveTo(cx - size * 0.25f, cy - size * 0.18f)
        shinePath.lineTo(cx - size * 0.1f, cy + size * 0.1f)
        shinePath.lineTo(cx + size * 0.05f, cy + size * 0.1f)
        shinePath.lineTo(cx + size * 0.1f, cy - size * 0.18f)
        shinePath.close()
        canvas.drawPath(shinePath, shinePaint)
    }

    private fun drawLevelTransition(canvas: Canvas) {
        levelTransitionProgress += 0.02f
        if (levelTransitionProgress >= 1f) {
            levelTransitionProgress = 1f
            isLevelTransitioning = false
        }
        
        val transitionAlpha = kotlin.math.sin(levelTransitionProgress * PI.toFloat())
        val overlayPaint = Paint().apply {
            color = colorTealAccent
            alpha = (120 * transitionAlpha).toInt()
            isAntiAlias = true
        }
        canvas.drawRect(0f, 0f, screenWidth, screenHeight, overlayPaint)
        
        // Level text
        val textPaint = Paint(titlePaint).apply {
            color = colorTextPrimary
            alpha = (255 * transitionAlpha).toInt()
        }
        canvas.drawText("LEVEL $level", screenWidth / 2, screenHeight / 2 - 20 * dpScale, textPaint)
        canvas.drawText("GET READY!", screenWidth / 2, screenHeight / 2 + 60 * dpScale,
            Paint().apply { color = colorTextSecondary; textSize = 26 * dpScale; isAntiAlias = true; textAlign = Paint.Align.CENTER; alpha = (200 * transitionAlpha).toInt() })
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                val touchX = event.x
                val touchY = event.y

                when (gameState) {
                    GameState.PLAYING -> {
                        // Check if touch is on striker area for shooting (long-press)
                        if (touchY > gameAreaBottom + 20 * dpScale &&
                            touchX >= striker.x - striker.width / 2 &&
                            touchX <= striker.x + striker.width / 2) {
                            strikerTouching = true
                            longPressStartTime = System.currentTimeMillis()
                            isLongPressShooting = false
                        } else {
                            // Move striker horizontally
                            striker.updatePosition(
                                max(striker.width / 2, min(screenWidth - striker.width / 2, touchX)),
                                strikerY
                            )
                        }
                    }
                    GameState.HOME -> {
                        if (popupScale > 0.9f) {
                            val cx = screenWidth / 2f
                            val cy = screenHeight / 2f
                            val invX = (touchX - cx) / popupScale + cx
                            val invY = (touchY - cy) / popupScale + cy
                            if (homeLevelBtn.contains(invX, invY)) {
                                startGame()
                            } else if (homeLevelsBtn.contains(invX, invY)) {
                                goToLevelSelect()
                            } else if (homeThemeBtn.contains(invX, invY)) {
                                toggleTheme()
                            } else {
                                startGame()
                            }
                        }
                    }
                    GameState.GAME_OVER -> {
                        popupTargetScale = 1f
                        overlayAlpha = 0f
                        startGame()
                    }
                    GameState.LEVEL_COMPLETE -> {
                        if (popupScale > 0.9f) {
                            val cx = screenWidth / 2f
                            val cy = screenHeight / 2f
                            val invX = (touchX - cx) / popupScale + cx
                            val invY = (touchY - cy) / popupScale + cy
                            if (levelCompleteHomeBtn.contains(invX, invY)) {
                                goToHome()
                            } else {
                                startGame()
                            }
                        }
                    }
                    GameState.LEVEL_SELECT -> {
                        if (popupScale > 0.9f) {
                            val cx = screenWidth / 2f
                            val cy = screenHeight / 2f
                            val invX = (touchX - cx) / popupScale + cx
                            val invY = (touchY - cy) / popupScale + cy
                            if (levelSelectHomeBtn.contains(invX, invY)) {
                                goToHome()
                            } else {
                                for (i in levelSelectBtns.indices) {
                                    if (levelSelectBtns[i].contains(invX, invY)) {
                                        val lvl = i + 1
                                        if (lvl <= level) {
                                            this.level = lvl
                                            startGame()
                                        }
                                        break
                                    }
                                }
                            }
                        }
                    }
                }
                invalidate()
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val touchX = event.x
                val touchY = event.y

                when (gameState) {
                    GameState.PLAYING -> {
                        if (strikerTouching) {
                            // Activate continuous shooting after long-press delay
                            if (System.currentTimeMillis() - longPressStartTime >= longPressDelay) {
                                isLongPressShooting = true
                            }
                        } else {
                            // Move striker horizontally
                            striker.updatePosition(
                                max(striker.width / 2, min(screenWidth - striker.width / 2, touchX)),
                                strikerY
                            )
                        }
                    }
                    else -> {}
                }
                invalidate()
                return true
            }
            MotionEvent.ACTION_UP -> {
                strikerTouching = false
                isLongPressShooting = false
            }
        }
        return super.onTouchEvent(event)
    }
    
    private fun destroyBlock(block: Block) {
        block.startDestroyAnimation()
        
        // Create explosion particles
        particles.addAll(Particle.createExplosion(block.x, block.y, block.color, 1.2f))
        
        score += block.points
        levelScore += block.points
        
        // Track destroyed stats
        shapeDestroyed[block.shape] = (shapeDestroyed[block.shape] ?: 0) + 1
        totalBlocksDestroyed++
    }

    private fun triggerScreenShake(intensity: Float) {
        screenShakeTime = 0.3f
        screenShakeX = (Random.nextFloat() - 0.5f) * intensity * dpScale
        screenShakeY = (Random.nextFloat() - 0.5f) * intensity * dpScale
    }

    private fun shootProjectile() {
        val now = System.currentTimeMillis()
        if (now - lastShotTime < shotCooldown) return
        lastShotTime = now
        
        val startX = striker.x
        val startY = striker.y - striker.height * 0.6f
        val speed = 800f
        // Shoot straight up
        val velocityX = 0f
        val velocityY = -speed
        
        // Get color based on level
        val projectileColor = getLevelProjectileColor()
        
        val projectile = Projectile(
            x = startX,
            y = startY,
            velocityX = velocityX,
            velocityY = velocityY,
            radius = 10f * dpScale,
            color = projectileColor
        )
        
        projectiles.add(projectile)
        
        // Muzzle flash particles
        particles.addAll(Particle.createExplosion(startX, startY, projectileColor, 0.5f))
    }

    private fun getLevelProjectileColor(): Int {
        return when (level % 6) {
            1 -> 0xFF41D443.toInt()  // Green
            2 -> 0xFF007AFF.toInt()  // Blue
            3 -> 0xFFFFCC00.toInt()  // Yellow
            4 -> 0xFFFF9F0A.toInt()  // Orange
            5 -> 0xFFFF2D92.toInt()  // Pink
            else -> 0xFF5AC8FA.toInt() // Cyan
        }
    }

    fun update(deltaTime: Float) {
        animationTime += deltaTime
        
        // Update screen shake
        if (screenShakeTime > 0) {
            screenShakeTime -= deltaTime
            if (screenShakeTime <= 0) {
                screenShakeX = 0f
                screenShakeY = 0f
            } else {
                screenShakeX = (Random.nextFloat() - 0.5f) * 8 * dpScale * screenShakeTime
                screenShakeY = (Random.nextFloat() - 0.5f) * 8 * dpScale * screenShakeTime
            }
        }
        
        if (gameState != GameState.PLAYING) return

        // Continuous shooting while long-pressing the striker
        if (isLongPressShooting) {
            val now = System.currentTimeMillis()
            if (now - lastShotTime >= shotCooldown) {
                shootProjectile()
            }
        }

        // Update blocks
        val iterator = blocks.iterator()
        while (iterator.hasNext()) {
            val block = iterator.next()
            block.update(deltaTime)

            // Check if block reached striker (game over)
            if (!block.isDestroyed && block.y + block.size / 2 >= strikerY - 45 * dpScale) {
                gameState = GameState.GAME_OVER
                triggerScreenShake(20f)
                popupTargetScale = 1f
                overlayAlpha = 0f
                return
            }

            // Remove completed destroy animations
            if (block.isAnimationComplete()) {
                iterator.remove()
            }
        }

        // Update particles
        val particleIterator = particles.iterator()
        while (particleIterator.hasNext()) {
            val particle = particleIterator.next()
            if (!particle.update(deltaTime)) {
                particleIterator.remove()
            }
        }

        // Update projectiles
        val projectileIterator = projectiles.iterator()
        while (projectileIterator.hasNext()) {
            val projectile = projectileIterator.next()
            if (!projectile.update(deltaTime)) {
                projectileIterator.remove()
                continue
            }
            
            // Check collision with blocks
            val projBounds = projectile.getBounds()
            for (block in blocks) {
                if (!block.isDestroyed) {
                    val blockLeft = block.x - block.size / 2
                    val blockRight = block.x + block.size / 2
                    val blockTop = block.y - block.size / 2
                    val blockBottom = block.y + block.size / 2
                    
                    if (projBounds.right > blockLeft && projBounds.left < blockRight &&
                        projBounds.bottom > blockTop && projBounds.top < blockBottom) {
                        // Hit!
                        destroyBlock(block)
                        projectileIterator.remove()
                        
                        // Impact particles
                        particles.addAll(Particle.createExplosion(block.x, block.y, COLOR_GREEN_DANGER, 0.8f))
                        triggerScreenShake(3f)
                        break
                    }
                }
            }
        }

        // Spawn new blocks
        spawnTimer += deltaTime
        if (spawnTimer >= spawnInterval && blocks.count { !it.isDestroyed } < maxBlocks) {
            spawnBlock()
            spawnTimer = 0f
        }

        // Check level complete
        if (levelScore >= blocksToClear * 10) {
            gameState = GameState.LEVEL_COMPLETE
            popupTargetScale = 1f
            overlayAlpha = 0f
        }
    }

    private fun spawnBlock() {
        val shape = blockShapes[Random.nextInt(blockShapes.size)]
        val color = blockColors[Random.nextInt(blockColors.size)]
        val size = (45f + Random.nextFloat() * 30f) * dpScale
        val x = (size + Random.nextFloat() * (screenWidth - 2 * size))
        val y = gameAreaTop - size

        val points = when (shape) {
            BlockShape.SQUARE -> 10
            BlockShape.CIRCLE -> 15
            BlockShape.TRIANGLE -> 20
            BlockShape.HEXAGON -> 25
            BlockShape.STAR -> 30
            BlockShape.DIAMOND -> 20
        }

        val block = Block(
            x = x,
            y = y,
            size = size,
            shape = shape,
            color = color,
            points = points,
            velocityY = blockSpeed
        )

        blocks.add(block)
        
        // Track stats
        shapeStats[shape] = (shapeStats[shape] ?: 0) + 1
        totalBlocksSpawned++
    }

    fun getGameState(): GameState = gameState
    fun getScore(): Int = score
    fun getLevel(): Int = level
    fun getLevelScore(): Int = levelScore

    fun goToHome() {
        gameState = GameState.HOME
        popupTargetScale = 1f
        overlayAlpha = 0f
        invalidate()
    }

    fun goToLevelSelect() {
        gameState = GameState.LEVEL_SELECT
        popupTargetScale = 1f
        overlayAlpha = 0f
        invalidate()
    }

    fun toggleTheme() {
        isDarkTheme = !isDarkTheme
        invalidate()
    }
}