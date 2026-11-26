package com.maxriegel.coolblueglimlachrun

import android.app.Activity
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.maxriegel.coolblueglimlachrun.audio.ChiptuneSoundPlayer
import com.maxriegel.coolblueglimlachrun.audio.SoundPlayer
import com.maxriegel.coolblueglimlachrun.data.ScoreRepository
import com.maxriegel.coolblueglimlachrun.data.SharedPreferencesScoreRepository
import com.maxriegel.coolblueglimlachrun.game.GameEngine
import com.maxriegel.coolblueglimlachrun.ui.GameView

class MainActivity : Activity() {

    lateinit var gameView: GameView
        private set

    private lateinit var soundPlayer: SoundPlayer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        configureImmersiveWindow()

        val repository: ScoreRepository = SharedPreferencesScoreRepository(applicationContext)
        soundPlayer = ChiptuneSoundPlayer()
        val engine = GameEngine(soundPlayer = soundPlayer, scoreRepository = repository)

        gameView = GameView(this, engine)
        setContentView(gameView)

        ViewCompat.setOnApplyWindowInsetsListener(gameView) { _, insets ->
            val safe = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            gameView.setSafeInsets(
                leftPx = safe.left.toFloat(),
                topPx = safe.top.toFloat(),
                rightPx = safe.right.toFloat(),
                bottomPx = safe.bottom.toFloat()
            )
            WindowInsetsCompat.CONSUMED
        }

        gameView.requestFocus()
    }

    private fun configureImmersiveWindow() {
        try {
            WindowCompat.setDecorFitsSystemWindows(window, false)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                window.attributes = window.attributes.apply {
                    layoutInDisplayCutoutMode =
                        WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                }
            }
            WindowInsetsControllerCompat(window, window.decorView).apply {
                hide(WindowInsetsCompat.Type.systemBars())
                systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } catch (_: Throwable) {
            // Safe fallback on non-standard window implementations
        }
    }

    override fun onResume() {
        super.onResume()
        configureImmersiveWindow()
        soundPlayer.onResume()
        gameView.startLoop()
        gameView.requestFocus()
    }

    override fun onPause() {
        gameView.stopLoop()
        soundPlayer.onPause()
        super.onPause()
    }

    override fun onDestroy() {
        soundPlayer.release()
        super.onDestroy()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return if (gameView.handleHardwareKeyDown(keyCode, event)) {
            true
        } else {
            super.onKeyDown(keyCode, event)
        }
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        return if (gameView.handleHardwareKeyUp(keyCode)) {
            true
        } else {
            super.onKeyUp(keyCode, event)
        }
    }
}
