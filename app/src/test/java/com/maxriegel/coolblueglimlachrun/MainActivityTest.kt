package com.maxriegel.coolblueglimlachrun

import android.app.Application
import android.view.KeyEvent
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.maxriegel.coolblueglimlachrun.game.CharacterType
import com.maxriegel.coolblueglimlachrun.game.GameScreen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(application = Application::class)
class MainActivityTest {

    @Test
    fun mainActivity_launchesCyclesMaxRemiVeroAndStartsGame() {
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        val activity = controller.get()

        assertNotNull(activity.gameView)
        assertEquals(GameScreen.TITLE, activity.gameView.engine.screen)
        assertEquals(CharacterType.MAX, activity.gameView.engine.selectedCharacter)

        // Arrow right selects Remi, then Vero
        activity.onKeyDown(KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_RIGHT))
        assertEquals(CharacterType.REMI, activity.gameView.engine.selectedCharacter)

        activity.onKeyDown(KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_RIGHT))
        assertEquals(CharacterType.VERO, activity.gameView.engine.selectedCharacter)

        // Enter starts Level 1 immediately as Vero
        activity.onKeyDown(KeyEvent.KEYCODE_ENTER, KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
        assertEquals(GameScreen.PLAYING, activity.gameView.engine.screen)
        assertEquals(CharacterType.VERO, activity.gameView.engine.selectedCharacter)

        controller.pause().stop().destroy()
    }
}
