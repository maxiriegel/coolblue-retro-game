package com.maxriegel.coolblueglimlachrun

import android.app.Activity
import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
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

        val repository: ScoreRepository = SharedPreferencesScoreRepository(applicationContext)
        soundPlayer = ChiptuneSoundPlayer()
        val engine = GameEngine(soundPlayer = soundPlayer, scoreRepository = repository)

        gameView = GameView(this, engine)
        setContentView(gameView)
        gameView.requestFocus()
    }

    override fun onResume() {
        super.onResume()
        gameView.startLoop()
        gameView.requestFocus()
    }

    override fun onPause() {
        gameView.stopLoop()
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
