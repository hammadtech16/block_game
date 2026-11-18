package com.blockgame

import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.blockgame.game.GameView
import kotlin.math.min

class MainActivity : AppCompatActivity() {

    private lateinit var gameView: GameView
    private var gameLoopRunnable: Runnable? = null
    private var lastFrameTime = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Fullscreen
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY

        gameView = GameView(this, null)
        setContentView(gameView)
    }

    override fun onResume() {
        super.onResume()
        startGameLoop()
    }

    override fun onPause() {
        super.onPause()
        stopGameLoop()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        }
    }

    private fun startGameLoop() {
        lastFrameTime = System.nanoTime()
        gameLoopRunnable = object : Runnable {
            override fun run() {
                val currentTime = System.nanoTime()
                val deltaTime = (currentTime - lastFrameTime) / 1_000_000_000f
                lastFrameTime = currentTime

                // Cap delta time to prevent huge jumps
                val cappedDeltaTime = kotlin.math.min(deltaTime, 0.05f)

                gameView.update(cappedDeltaTime)
                gameView.invalidate()

                gameView.postDelayed(this, 16) // ~60 FPS
            }
        }
        gameView.post(gameLoopRunnable!!)
    }

    private fun stopGameLoop() {
        gameLoopRunnable?.let { runnable ->
            gameView.removeCallbacks(runnable)
        }
        gameLoopRunnable = null
    }
}