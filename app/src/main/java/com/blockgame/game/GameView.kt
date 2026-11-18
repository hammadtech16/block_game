package com.blockgame.game

import android.content.Context
import android.graphics.Bitmap
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
import kotlin.random.Random

class GameView(context: Context, attrs: AttributeSet?) : View(context, attrs) {

    // Game state
    private var gameState = GameState.PLAYING
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

    // Game objects
    private val blocks = mutableListOf<Block>()
    private val particles = mutableListOf<Particle>()
    private lateinit var striker: Striker

    // Game config
    private var screenWidth = 0f
    private var screenHeight = 0f
    private var gameAreaTop = 0f
    private var gameAreaBottom = 0f
    private var strikerY = 0f

    // Spawning
    private var spawnTimer = 0f
    private var spawnInterval = 1.5f
    private var blockSpeed = 150f
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

    // Modern color palette
    private val COLOR_BG_DARK = 0xFF0A0A0F.toInt()
    private val COLOR_BG_CARD = 0xFF16161E.toInt()
    private val COLOR_BG_CARD_LIGHT = 0xFF1F1F2A.toInt()
    private val COLOR_PRIMARY = 0xFF6200EE.toInt()
    private val COLOR_SECONDARY = 0xFF03DAC5.toInt()
    private val COLOR_ACCENT = 0xFFFFD600.toInt()
    private val COLOR_TEXT_PRIMARY = 0xFFFFFFFF.toInt()
    private val COLOR_TEXT_SECONDARY = 0xFFB0B0B0.toInt()
    private val COLOR_DANGER = 0xFFFF3B30.toInt()
    private val COLOR_SUCCESS = 0xFF34C759.toInt()
    private val COLOR_WARNING = 0xFFFF9F0A.toInt()

    // Paint objects
    private val scorePaint = Paint().apply {
        color = COLOR_TEXT_PRIMARY
        textSize = 36f
        isAntiAlias = true
        textAlign = Paint.Align.LEFT
        typeface = android.graphics.Typeface.DEFAULT_BOLD
    }

    private val levelPaint = Paint().apply {
        color = COLOR_TEXT_SECONDARY
        textSize = 24f
        isAntiAlias = true
        textAlign = Paint.Align.RIGHT
    }

    private val titlePaint = Paint().apply {
        color = COLOR_TEXT_PRIMARY
        textSize = 56f
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
        typeface = android.graphics.Typeface.DEFAULT_BOLD
    }

    private val subtitlePaint = Paint().apply {
        color = COLOR_TEXT_SECONDARY
        textSize = 22f
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
    }

    private val buttonPaint = Paint().apply {
        color = COLOR_PRIMARY
        isAntiAlias = true
        style = Paint.Style.FILL
    }

    private val buttonTextPaint = Paint().apply {
        color = COLOR_TEXT_PRIMARY
        textSize = 24f
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
        typeface = android.graphics.Typeface.DEFAULT_BOLD
    }

    private val comboPaint = Paint().apply {
        color = COLOR_ACCENT
        textSize = 48f
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
        typeface = android.graphics.Typeface.DEFAULT_BOLD
    }

    private val backgroundPaint = Paint().apply {
        color = COLOR_BG_DARK
    }

    private val gridPaint = Paint().apply {
        color = 0xFF1A1A24.toInt()
        strokeWidth = 0.5f
    }

    private val dangerLinePaint = Paint().apply {
        color = COLOR_DANGER
        strokeWidth = 2f
        isAntiAlias = true
    }

    private val cardPaint = Paint().apply {
        color = COLOR_BG_CARD
        isAntiAlias = true
    }

    private val cardBorderPaint = Paint().apply {
        color = 0xFF2A2A3A.toInt()
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = 1f
    }

    private val progressPaint = Paint().apply {
        color = COLOR_PRIMARY
        isAntiAlias = true
        style = Paint.Style.FILL
    }

    private val progressBgPaint = Paint().apply {
        color = 0xFF2A2A3A.toInt()
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

    enum class GameState {
        PLAYING,
        GAME_OVER,
        LEVEL_COMPLETE,
        WAITING_START
    }

    init {
        setWillNotDraw(false)
        dpScale = resources.displayMetrics.density
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        screenWidth = w.toFloat()
        screenHeight = h.toFloat()

        gameAreaTop = screenHeight * 0.15f
        gameAreaBottom = screenHeight * 0.78f
        strikerY = gameAreaBottom + 60 * dpScale

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
        gameState = GameState.WAITING_START
        overlayAlpha = 0f
        popupScale = 0f
        popupTargetScale = 1f

        striker = Striker(
            x = screenWidth / 2,
            y = strikerY,
            width = 80 * dpScale,
            height = 100 * dpScale
        )

        spawnInterval = 1.5f
        blockSpeed = 150f
        maxBlocks = 10
        blocksToClear = 15
    }

    fun startGame() {
        if (gameState == GameState.WAITING_START || gameState == GameState.GAME_OVER) {
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
        blocksToClear = 15 + level * 5
        isLevelTransitioning = true
        levelTransitionProgress = 0f

        spawnInterval = max(0.4f, 1.5f - level * 0.1f)
        blockSpeed = min(600f, 150f + level * 35f)
        maxBlocks = min(35, 10 + level * 2)

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

        // Draw particles
        for (particle in particles) {
            particle.draw(canvas)
        }

        // Draw combo text
        drawComboText(canvas)

        // Draw striker
        if (gameState == GameState.PLAYING) {
            striker.draw(canvas)
        }

        // Draw UI
        drawGameUI(canvas)

        // Draw state overlays with animations
        when (gameState) {
            GameState.WAITING_START -> drawStartOverlay(canvas)
            GameState.GAME_OVER -> drawGameOverOverlay(canvas)
            GameState.LEVEL_COMPLETE -> drawLevelCompleteOverlay(canvas)
            GameState.PLAYING -> {} // No overlay during gameplay
        }

        // Draw level transition
        if (isLevelTransitioning) {
            drawLevelTransition(canvas)
        }
    }

    private fun drawBackground(canvas: Canvas) {
        // Base background
        canvas.drawRect(0f, 0f, screenWidth, screenHeight, backgroundPaint)
        
        // Top gradient area
        val topGradient = LinearGradient(
            0f, 0f, 0f, gameAreaTop,
            intArrayOf(0xFF0D0D14.toInt(), 0xFF0A0A0F.toInt()),
            null,
            Shader.TileMode.CLAMP
        )
        backgroundPaint.shader = topGradient
        canvas.drawRect(0f, 0f, screenWidth, gameAreaTop, backgroundPaint)
        
        // Bottom gradient area
        val bottomGradient = LinearGradient(
            0f, gameAreaBottom, 0f, screenHeight,
            intArrayOf(0xFF0A0A0F.toInt(), 0xFF0D0D14.toInt()),
            null,
            Shader.TileMode.CLAMP
        )
        backgroundPaint.shader = bottomGradient
        canvas.drawRect(0f, gameAreaBottom, screenWidth, screenHeight, backgroundPaint)
        
        backgroundPaint.shader = null
        
        // Subtle pattern dots
        drawPatternDots(canvas)
        
        // Accent lines
        drawAccentLines(canvas)
    }

    private fun drawPatternDots(canvas: Canvas) {
        val dotPaint = Paint().apply {
            color = 0xFF1A1A24.toInt()
            isAntiAlias = true
            style = Paint.Style.FILL
        }
        val spacing = 80 * dpScale
        val offset = (System.currentTimeMillis() / 1000) % spacing
        
        for (x in (-offset).toInt() .. screenWidth.toInt() step spacing.toInt()) {
            for (y in (-offset).toInt() .. screenHeight.toInt() step spacing.toInt()) {
                if (y < gameAreaTop || y > gameAreaBottom) {
                    canvas.drawCircle(x.toFloat(), y.toFloat(), 0.5f, dotPaint)
                }
            }
        }
    }

    private fun drawAccentLines(canvas: Canvas) {
        val linePaint = Paint().apply {
            color = COLOR_PRIMARY
            strokeWidth = 1f
            alpha = 30
            isAntiAlias = true
        }
        
        // Diagonal accent lines
        for (i in -5..5) {
            val offset = i * 200 * dpScale + (animationTime * 50f % (200 * dpScale))
            canvas.drawLine(
                -screenHeight + offset, screenHeight,
                screenWidth + offset, -screenHeight,
                linePaint
            )
        }
    }

    private var animationTime: Float = 0f

    private fun drawGrid(canvas: Canvas) {
        val spacing = 60 * dpScale
        val linePaint = Paint().apply {
            color = 0xFF1A1A24.toInt()
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
        val lineY = strikerY - 40 * dpScale
        
        // Animated danger line
        val dashPath = Path()
        val dashLength = 20 * dpScale
        val gapLength = 10 * dpScale
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
        
        dangerLinePaint.color = COLOR_DANGER
        dangerLinePaint.alpha = (150 + 100 * kotlin.math.sin(animationTime * 4f)).toInt()
        canvas.drawPath(dashPath, dangerLinePaint)
        
        // Warning glow
        val glowPaint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.FILL
            color = COLOR_DANGER
            alpha = (30 + 20 * kotlin.math.sin(animationTime * 4f)).toInt()
        }
        canvas.drawRect(0f, lineY, screenWidth, strikerY + 20 * dpScale, glowPaint)
        
        // Warning text
        val warningPaint = Paint().apply {
            color = COLOR_DANGER
            textSize = 18 * dpScale
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        canvas.drawText("⚠ DANGER ZONE ⚠", screenWidth / 2, lineY - 12 * dpScale, warningPaint)
    }

    private fun drawGameUI(canvas: Canvas) {
        val padding = 24 * dpScale
        val cardHeight = 80 * dpScale
        val cornerRadius = 16 * dpScale
        
        // Score card (left)
        val scoreCardRect = RectF(
            padding, padding,
            screenWidth / 2 - padding / 2, padding + cardHeight
        )
        drawCard(canvas, scoreCardRect, cornerRadius)
        
        // Score text
        val scoreValuePaint = Paint(scorePaint).apply { textSize = 32 * dpScale }
        canvas.drawText("SCORE", scoreCardRect.left + 20 * dpScale, scoreCardRect.top + 24 * dpScale, 
            Paint(levelPaint).apply { textSize = 16 * dpScale; color = COLOR_TEXT_SECONDARY })
        canvas.drawText("$score", scoreCardRect.left + 20 * dpScale, scoreCardRect.top + 58 * dpScale, scoreValuePaint)
        
        // Level card (right)
        val levelCardRect = RectF(
            screenWidth / 2 + padding / 2, padding,
            screenWidth - padding, padding + cardHeight
        )
        drawCard(canvas, levelCardRect, cornerRadius)
        
        canvas.drawText("LEVEL", levelCardRect.right - 20 * dpScale, levelCardRect.top + 24 * dpScale, 
            Paint(levelPaint).apply { textSize = 16 * dpScale; color = COLOR_TEXT_SECONDARY })
        canvas.drawText("$level", levelCardRect.right - 20 * dpScale, levelCardRect.top + 58 * dpScale, 
            Paint(scorePaint).apply { textSize = 32 * dpScale; textAlign = Paint.Align.RIGHT })
        
        // Progress bar at bottom of game area
        drawProgressBar(canvas)
    }

    private fun drawCard(canvas: Canvas, rect: RectF, radius: Float) {
        // Shadow
        val shadowPaint = Paint().apply {
            color = 0xFF000000.toInt()
            isAntiAlias = true
            setShadowLayer(10 * dpScale, 0f, 4 * dpScale, 0x40000000.toInt())
        }
        canvas.drawRoundRect(rect, radius, radius, shadowPaint)
        
        // Card background
        canvas.drawRoundRect(rect, radius, radius, cardPaint)
        
        // Card border
        canvas.drawRoundRect(rect, radius, radius, cardBorderPaint)
        
        // Top accent line
        val accentLine = Paint().apply {
            color = COLOR_PRIMARY
            strokeWidth = 3 * dpScale
            isAntiAlias = true
        }
        val topRect = RectF(rect.left, rect.top, rect.right, rect.top + 3 * dpScale)
        canvas.drawRect(topRect, accentLine)
    }

    private fun drawProgressBar(canvas: Canvas) {
        val barHeight = 6 * dpScale
        val barWidth = screenWidth * 0.8f
        val barX = (screenWidth - barWidth) / 2
        val barY = gameAreaBottom - 20 * dpScale
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
            progressPaint.setShadowLayer(8 * dpScale, 0f, 0f, COLOR_PRIMARY)
            canvas.drawRoundRect(progressRect, radius, radius, progressPaint)
            progressPaint.clearShadowLayer()
        }
        
        // Progress text
        val progressTextPaint = Paint().apply {
            color = COLOR_TEXT_SECONDARY
            textSize = 14 * dpScale
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("$levelScore / ${blocksToClear * 10}", screenWidth / 2, barY - 8 * dpScale, progressTextPaint)
    }

    private fun drawComboText(canvas: Canvas) {
        if (comboCount > 1 && System.currentTimeMillis() - lastComboTime < 2000) {
            val alpha = (1f - (System.currentTimeMillis() - lastComboTime) / 2000f)
            comboPaint.alpha = (255 * alpha).toInt()
            comboPaint.setShadowLayer(10 * dpScale, 0f, 0f, COLOR_ACCENT)
            
            val y = gameAreaTop + 100 * dpScale
            canvas.drawText("COMBO x$comboCount!", screenWidth / 2, y, comboPaint)
            comboPaint.clearShadowLayer()
        }
    }

    private fun drawStartOverlay(canvas: Canvas) {
        overlayAlpha = min(1f, overlayAlpha + 0.05f)
        overlayPaint.alpha = (200 * overlayAlpha).toInt()
        canvas.drawRect(0f, 0f, screenWidth, screenHeight, overlayPaint)
        
        popupScale = popupScale + (popupTargetScale - popupScale) * 0.2f
        
        canvas.save()
        canvas.translate(screenWidth / 2, screenHeight / 2)
        canvas.scale(popupScale, popupScale)
        canvas.translate(-screenWidth / 2, -screenHeight / 2)
        
        drawStartPopup(canvas)
        canvas.restore()
    }

    private fun drawStartPopup(canvas: Canvas) {
        val popupWidth = screenWidth * 0.85f
        val popupHeight = screenHeight * 0.6f
        val popupX = (screenWidth - popupWidth) / 2
        val popupY = (screenHeight - popupHeight) / 2
        val radius = 28 * dpScale
        
        val popupRect = RectF(popupX, popupY, popupX + popupWidth, popupY + popupHeight)
        
        // Shadow
        val shadowPaint = Paint().apply {
            color = 0xFF000000.toInt()
            isAntiAlias = true
            setShadowLayer(30 * dpScale, 0f, 10 * dpScale, 0x60000000.toInt())
        }
        canvas.drawRoundRect(popupRect, radius, radius, shadowPaint)
        
        // Background
        val popupBgPaint = Paint().apply {
            color = COLOR_BG_CARD
            isAntiAlias = true
        }
        canvas.drawRoundRect(popupRect, radius, radius, popupBgPaint)
        
        // Border
        cardBorderPaint.color = 0xFF2A2A3A.toInt()
        canvas.drawRoundRect(popupRect, radius, radius, cardBorderPaint)
        
        // Top accent bar
        val accentRect = RectF(popupX, popupY, popupX + popupWidth, popupY + 5 * dpScale)
        val accentPaint = Paint().apply {
            color = COLOR_PRIMARY
            isAntiAlias = true
            shader = LinearGradient(
                popupX, popupY, popupX + popupWidth, popupY,
                intArrayOf(COLOR_PRIMARY, COLOR_SECONDARY, COLOR_ACCENT),
                null, Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(accentRect, accentPaint)
        
        // Title
        canvas.drawText("BLOCK GAME", screenWidth / 2, popupY + 100 * dpScale, titlePaint)
        
        // Subtitle
        canvas.drawText("Tap matching colors to destroy blocks", screenWidth / 2, popupY + 150 * dpScale, subtitlePaint)
        canvas.drawText("Don't let them reach the danger zone!", screenWidth / 2, popupY + 190 * dpScale, subtitlePaint)
        
        // Feature list
        val featurePaint = Paint().apply {
            color = COLOR_TEXT_PRIMARY
            textSize = 20 * dpScale
            isAntiAlias = true
            textAlign = Paint.Align.LEFT
        }
        val features = listOf(
            "🎯 Match 2+ same colors to clear",
            "⚡ Combos give bonus points",
            "🎨 8 shapes × 8 colors",
            "📈 Progressive difficulty"
        )
        features.forEachIndexed { index, feature ->
            canvas.drawText(feature, popupX + 40 * dpScale, popupY + 250 * dpScale + index * 40 * dpScale, featurePaint)
        }
        
        // Start button
        val buttonWidth = 220 * dpScale
        val buttonHeight = 56 * dpScale
        val buttonX = (screenWidth - buttonWidth) / 2
        val buttonY = popupY + popupHeight - 100 * dpScale
        val buttonRect = RectF(buttonX, buttonY, buttonX + buttonWidth, buttonY + buttonHeight)
        val buttonRadius = buttonHeight / 2
        
        // Button shadow
        buttonPaint.color = COLOR_PRIMARY
        buttonPaint.setShadowLayer(12 * dpScale, 0f, 4 * dpScale, 0x406200EE.toInt())
        canvas.drawRoundRect(buttonRect, buttonRadius, buttonRadius, buttonPaint)
        buttonPaint.clearShadowLayer()
        
        // Button border
        val buttonBorderPaint = Paint().apply {
            color = 0xFF7C4DFF.toInt()
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeWidth = 2 * dpScale
        }
        canvas.drawRoundRect(buttonRect, buttonRadius, buttonRadius, buttonBorderPaint)
        
        canvas.drawText("TAP TO START", screenWidth / 2, buttonY + buttonHeight * 0.65f, buttonTextPaint)
        
        // Animated hint
        val hintPaint = Paint().apply {
            color = COLOR_TEXT_SECONDARY
            textSize = 16 * dpScale
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            alpha = (150 + 100 * kotlin.math.sin(animationTime * 3f)).toInt()
        }
        canvas.drawText("Touch and drag to move the striker", screenWidth / 2, buttonY + buttonHeight + 30 * dpScale, hintPaint)
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
        val popupWidth = screenWidth * 0.85f
        val popupHeight = screenHeight * 0.55f
        val popupX = (screenWidth - popupWidth) / 2
        val popupY = (screenHeight - popupHeight) / 2
        val radius = 28 * dpScale
        
        val popupRect = RectF(popupX, popupY, popupX + popupWidth, popupY + popupHeight)
        
        // Shadow
        val shadowPaint = Paint().apply {
            color = 0xFF000000.toInt()
            isAntiAlias = true
            setShadowLayer(30 * dpScale, 0f, 10 * dpScale, 0x80000000.toInt())
        }
        canvas.drawRoundRect(popupRect, radius, radius, shadowPaint)
        
        // Background with danger tint
        val popupBgPaint = Paint().apply {
            color = COLOR_BG_CARD
            isAntiAlias = true
        }
        canvas.drawRoundRect(popupRect, radius, radius, popupBgPaint)
        
        // Top accent bar - danger color
        val accentRect = RectF(popupX, popupY, popupX + popupWidth, popupY + 5 * dpScale)
        val accentPaint = Paint().apply {
            color = COLOR_DANGER
            isAntiAlias = true
        }
        canvas.drawRect(accentRect, accentPaint)
        
        // Game Over title
        val gameOverPaint = Paint(titlePaint).apply {
            color = COLOR_DANGER
            textSize = 48 * dpScale
        }
        canvas.drawText("GAME OVER", screenWidth / 2, popupY + 100 * dpScale, gameOverPaint)
        
        // Skull/icon
        drawSkullIcon(canvas, screenWidth / 2, popupY + 180 * dpScale)
        
        // Stats
        val statPaint = Paint().apply {
            color = COLOR_TEXT_PRIMARY
            textSize = 28 * dpScale
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        
        canvas.drawText("Final Score", screenWidth / 2, popupY + 280 * dpScale, 
            Paint().apply { color = COLOR_TEXT_SECONDARY; textSize = 18 * dpScale; isAntiAlias = true; textAlign = Paint.Align.CENTER })
        canvas.drawText("$score", screenWidth / 2, popupY + 320 * dpScale, statPaint)
        
        canvas.drawText("Level Reached", screenWidth / 2, popupY + 370 * dpScale,
            Paint().apply { color = COLOR_TEXT_SECONDARY; textSize = 18 * dpScale; isAntiAlias = true; textAlign = Paint.Align.CENTER })
        canvas.drawText("$level", screenWidth / 2, popupY + 410 * dpScale, statPaint)
        
        // Restart button
        val buttonWidth = 220 * dpScale
        val buttonHeight = 56 * dpScale
        val buttonX = (screenWidth - buttonWidth) / 2
        val buttonY = popupY + popupHeight - 90 * dpScale
        val buttonRect = RectF(buttonX, buttonY, buttonX + buttonWidth, buttonY + buttonHeight)
        val buttonRadius = buttonHeight / 2
        
        buttonPaint.color = COLOR_DANGER
        buttonPaint.setShadowLayer(12 * dpScale, 0f, 4 * dpScale, 0x40FF3B30.toInt())
        canvas.drawRoundRect(buttonRect, buttonRadius, buttonRadius, buttonPaint)
        buttonPaint.clearShadowLayer()
        
        val buttonBorderPaint = Paint().apply {
            color = 0xFFFF6B6B.toInt()
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeWidth = 2 * dpScale
        }
        canvas.drawRoundRect(buttonRect, buttonRadius, buttonRadius, buttonBorderPaint)
        
        canvas.drawText("TAP TO RETRY", screenWidth / 2, buttonY + buttonHeight * 0.65f, buttonTextPaint)
    }

    private fun drawSkullIcon(canvas: Canvas, cx: Float, cy: Float) {
        val size = 50 * dpScale
        val paint = Paint().apply {
            color = COLOR_DANGER
            isAntiAlias = true
            style = Paint.Style.FILL
            alpha = 180
        }
        
        // Simple skull shape using circles and rects
        canvas.drawCircle(cx, cy - size * 0.1f, size * 0.45f, paint) // Head
        canvas.drawRect(cx - size * 0.35f, cy + size * 0.2f, cx + size * 0.35f, cy + size * 0.5f, paint) // Jaw
        
        // Eyes
        paint.color = COLOR_BG_DARK
        canvas.drawCircle(cx - size * 0.15f, cy - size * 0.1f, size * 0.08f, paint)
        canvas.drawCircle(cx + size * 0.15f, cy - size * 0.1f, size * 0.08f, paint)
        
        // Nose
        canvas.drawCircle(cx, cy + size * 0.1f, size * 0.05f, paint)
    }

    private fun drawLevelCompleteOverlay(canvas: Canvas) {
        overlayAlpha = min(1f, overlayAlpha + 0.05f)
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
        val popupWidth = screenWidth * 0.85f
        val popupHeight = screenHeight * 0.6f
        val popupX = (screenWidth - popupWidth) / 2
        val popupY = (screenHeight - popupHeight) / 2
        val radius = 28 * dpScale
        
        val popupRect = RectF(popupX, popupY, popupX + popupWidth, popupY + popupHeight)
        
        // Shadow
        val shadowPaint = Paint().apply {
            color = 0xFF000000.toInt()
            isAntiAlias = true
            setShadowLayer(30 * dpScale, 0f, 10 * dpScale, 0x60000000.toInt())
        }
        canvas.drawRoundRect(popupRect, radius, radius, shadowPaint)
        
        // Background
        val popupBgPaint = Paint().apply {
            color = COLOR_BG_CARD
            isAntiAlias = true
        }
        canvas.drawRoundRect(popupRect, radius, radius, popupBgPaint)
        
        // Border with success color
        cardBorderPaint.color = COLOR_SUCCESS
        cardBorderPaint.strokeWidth = 2 * dpScale
        canvas.drawRoundRect(popupRect, radius, radius, cardBorderPaint)
        cardBorderPaint.strokeWidth = 1 * dpScale
        cardBorderPaint.color = 0xFF2A2A3A.toInt()
        
        // Top accent bar - success gradient
        val accentRect = RectF(popupX, popupY, popupX + popupWidth, popupY + 5 * dpScale)
        val accentPaint = Paint().apply {
            isAntiAlias = true
            shader = LinearGradient(
                popupX, popupY, popupX + popupWidth, popupY,
                intArrayOf(COLOR_SUCCESS, COLOR_ACCENT, COLOR_SECONDARY),
                null, Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(accentRect, accentPaint)
        
        // Level Complete title
        val completePaint = Paint(titlePaint).apply {
            color = COLOR_SUCCESS
            textSize = 42 * dpScale
        }
        canvas.drawText("LEVEL COMPLETE!", screenWidth / 2, popupY + 100 * dpScale, completePaint)
        
        // Level number
        val levelNumPaint = Paint().apply {
            color = COLOR_TEXT_SECONDARY
            textSize = 24 * dpScale
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("Level $level", screenWidth / 2, popupY + 140 * dpScale, levelNumPaint)
        
        // Trophy/star icon
        drawTrophyIcon(canvas, screenWidth / 2, popupY + 220 * dpScale)
        
        // Points earned
        val pointsPaint = Paint().apply {
            color = COLOR_ACCENT
            textSize = 48 * dpScale
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        canvas.drawText("+$levelScore", screenWidth / 2, popupY + 320 * dpScale, pointsPaint)
        canvas.drawText("POINTS EARNED", screenWidth / 2, popupY + 360 * dpScale,
            Paint().apply { color = COLOR_TEXT_SECONDARY; textSize = 18 * dpScale; isAntiAlias = true; textAlign = Paint.Align.CENTER })
        
        // Total score
        canvas.drawText("Total Score: $score", screenWidth / 2, popupY + 410 * dpScale,
            Paint().apply { color = COLOR_TEXT_PRIMARY; textSize = 24 * dpScale; isAntiAlias = true; textAlign = Paint.Align.CENTER })
        
        // Continue button
        val buttonWidth = 240 * dpScale
        val buttonHeight = 56 * dpScale
        val buttonX = (screenWidth - buttonWidth) / 2
        val buttonY = popupY + popupHeight - 100 * dpScale
        val buttonRect = RectF(buttonX, buttonY, buttonX + buttonWidth, buttonY + buttonHeight)
        val buttonRadius = buttonHeight / 2
        
        buttonPaint.color = COLOR_SUCCESS
        buttonPaint.setShadowLayer(12 * dpScale, 0f, 4 * dpScale, 0x4034C759.toInt())
        canvas.drawRoundRect(buttonRect, buttonRadius, buttonRadius, buttonPaint)
        buttonPaint.clearShadowLayer()
        
        val buttonBorderPaint = Paint().apply {
            color = 0xFF69F0AE.toInt()
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeWidth = 2 * dpScale
        }
        canvas.drawRoundRect(buttonRect, buttonRadius, buttonRadius, buttonBorderPaint)
        
        canvas.drawText("CONTINUE TO LEVEL ${level + 1}", screenWidth / 2, buttonY + buttonHeight * 0.65f, buttonTextPaint)
    }

    private fun drawTrophyIcon(canvas: Canvas, cx: Float, cy: Float) {
        val size = 60 * dpScale
        val paint = Paint().apply {
            color = COLOR_ACCENT
            isAntiAlias = true
            style = Paint.Style.FILL
        }
        
        // Cup
        val cupPath = Path()
        cupPath.moveTo(cx - size * 0.3f, cy - size * 0.2f)
        cupPath.lineTo(cx - size * 0.4f, cy + size * 0.3f)
        cupPath.lineTo(cx + size * 0.4f, cy + size * 0.3f)
        cupPath.lineTo(cx + size * 0.3f, cy - size * 0.2f)
        cupPath.close()
        canvas.drawPath(cupPath, paint)
        
        // Handles
        val handlePaint = Paint(paint).apply { style = Paint.Style.STROKE; strokeWidth = 4 * dpScale }
        val leftHandle = Path()
        leftHandle.moveTo(cx - size * 0.3f, cy - size * 0.1f)
        leftHandle.quadTo(cx - size * 0.5f, cy, cx - size * 0.4f, cy + size * 0.2f)
        canvas.drawPath(leftHandle, handlePaint)
        
        val rightHandle = Path()
        rightHandle.moveTo(cx + size * 0.3f, cy - size * 0.1f)
        rightHandle.quadTo(cx + size * 0.5f, cy, cx + size * 0.4f, cy + size * 0.2f)
        canvas.drawPath(rightHandle, handlePaint)
        
        // Base
        val basePaint = Paint(paint).apply { color = 0xFFE6C000.toInt() }
        canvas.drawRect(cx - size * 0.45f, cy + size * 0.3f, cx + size * 0.45f, cy + size * 0.4f, basePaint)
        canvas.drawRect(cx - size * 0.3f, cy + size * 0.4f, cx + size * 0.3f, cy + size * 0.45f, basePaint)
    }

    private fun drawLevelTransition(canvas: Canvas) {
        levelTransitionProgress += 0.02f
        if (levelTransitionProgress >= 1f) {
            levelTransitionProgress = 1f
            isLevelTransitioning = false
        }
        
        val alpha = kotlin.math.sin(levelTransitionProgress * PI)
        val overlayPaint = Paint().apply {
            color = COLOR_PRIMARY
            alpha = (150 * alpha).toInt()
            isAntiAlias = true
        }
        canvas.drawRect(0f, 0f, screenWidth, screenHeight, overlayPaint)
        
        // Level text
        val textPaint = Paint(titlePaint).apply {
            color = COLOR_TEXT_PRIMARY
            alpha = (255 * alpha).toInt()
        }
        canvas.drawText("LEVEL $level", screenWidth / 2, screenHeight / 2, textPaint)
        canvas.drawText("GET READY!", screenWidth / 2, screenHeight / 2 + 80 * dpScale,
            Paint().apply { color = COLOR_TEXT_SECONDARY; textSize = 28 * dpScale; isAntiAlias = true; textAlign = Paint.Align.CENTER; alpha = (200 * alpha).toInt() })
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                val touchX = event.x
                val touchY = event.y

                when (gameState) {
                    GameState.PLAYING -> {
                        // Move striker horizontally
                        striker.updatePosition(
                            max(striker.width / 2, min(screenWidth - striker.width / 2, touchX)),
                            strikerY
                        )

                        // Check block tap
                        checkBlockTap(touchX, touchY)
                    }
                    GameState.WAITING_START, GameState.GAME_OVER -> {
                        popupTargetScale = 1f
                        overlayAlpha = 0f
                        startGame()
                    }
                    GameState.LEVEL_COMPLETE -> {
                        popupTargetScale = 1f
                        overlayAlpha = 0f
                        startGame()
                    }
                }
                invalidate()
                return true
            }
            MotionEvent.ACTION_UP -> {
                if (gameState == GameState.PLAYING) {
                    // Could add tap feedback here
                }
            }
        }
        return super.onTouchEvent(event)
    }

    private fun checkBlockTap(touchX: Float, touchY: Float) {
        // Find the tapped block
        var tappedBlock: Block? = null
        for (block in blocks) {
            if (!block.isDestroyed && block.containsPoint(touchX, touchY)) {
                tappedBlock = block
                break
            }
        }

        tappedBlock?.let { block ->
            // Find all blocks of same color
            val sameColorBlocks = blocks.filter { !it.isDestroyed && it.color == block.color }
            
            if (sameColorBlocks.size >= 2) {
                // Combo system
                val now = System.currentTimeMillis()
                if (now - lastComboTime < 1500) {
                    comboCount++
                } else {
                    comboCount = 1
                }
                lastComboTime = now
                
                // Destroy all same color blocks
                for (targetBlock in sameColorBlocks) {
                    destroyBlock(targetBlock)
                }
                
                // Create combo burst particles
                if (comboCount > 1) {
                    particles.addAll(Particle.createComboBurst(block.x, block.y, block.color, comboCount))
                    triggerScreenShake(5f * comboCount)
                }
                
                // Bonus points for combo
                val comboBonus = (sameColorBlocks.size - 2) * 50 + (comboCount - 1) * 100
                score += comboBonus
                levelScore += comboBonus
            }
        }
    }

    private fun destroyBlock(block: Block) {
        block.startDestroyAnimation()
        
        // Create explosion particles
        particles.addAll(Particle.createExplosion(block.x, block.y, block.color, 1.2f))
        
        score += block.points
        levelScore += block.points
    }

    private fun triggerScreenShake(intensity: Float) {
        screenShakeTime = 0.3f
        screenShakeX = (Random.nextFloat() - 0.5f) * intensity * dpScale
        screenShakeY = (Random.nextFloat() - 0.5f) * intensity * dpScale
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

        // Update blocks
        val iterator = blocks.iterator()
        while (iterator.hasNext()) {
            val block = iterator.next()
            block.update(deltaTime)

            // Check if block reached striker (game over)
            if (!block.isDestroyed && block.y + block.size / 2 >= strikerY - 50 * dpScale) {
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
        val size = (50f + Random.nextFloat() * 30f) * dpScale
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
    }

    fun getGameState(): GameState = gameState
    fun getScore(): Int = score
    fun getLevel(): Int = level
    fun getLevelScore(): Int = levelScore
}