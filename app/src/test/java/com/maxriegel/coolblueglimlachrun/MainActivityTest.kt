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
    fun mainActivity_launchesAndRespondsToKeyboardSelectionAndStart() {
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        val activity = controller.get()

        assertNotNull(activity.gameView)
        assertEquals(GameScreen.TITLE, activity.gameView.engine.screen)

        // Arrow right selects Remi on Title Screen
        activity.onKeyDown(KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_RIGHT))
        assertEquals(CharacterType.REMI, activity.gameView.engine.selectedCharacter)

        // Enter starts Level 1 immediately
        activity.onKeyDown(KeyEvent.KEYCODE_ENTER, KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
        assertEquals(GameScreen.PLAYING, activity.gameView.engine.screen)

        controller.pause().stop().destroy()
    }
}
