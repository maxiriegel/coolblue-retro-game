package com.maxriegel.coolblueglimlachrun.ui

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.RectF
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.maxriegel.coolblueglimlachrun.audio.NoOpSoundPlayer
import com.maxriegel.coolblueglimlachrun.data.InMemoryScoreRepository
import com.maxriegel.coolblueglimlachrun.game.CharacterType
import com.maxriegel.coolblueglimlachrun.game.GameEngine
import com.maxriegel.coolblueglimlachrun.game.InputState
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(application = Application::class)
class ResponsiveRendererTest {

    @Test
    fun rendersAcrossPhonePortraitUltrawideTabletAndDesktopWithValidHitboxes() {
        val formFactors = listOf(
            // Smartphone Portrait with punch-hole top cutout & gesture bottom bar
            Triple(1080, 2400, floatArrayOf(0f, 96f, 0f, 64f)),
            // Smartphone Ultrawide 20:9 Landscape with camera cutout on left
            Triple(2400, 1080, floatArrayOf(110f, 0f, 48f, 42f)),
            // Tablet 4:3 Landscape
            Triple(1600, 1200, floatArrayOf(0f, 36f, 0f, 36f)),
            // Chromebook / Desktop 16:9
            Triple(1280, 720, floatArrayOf(0f, 0f, 0f, 0f))
        )

        for ((w, h, insets) in formFactors) {
            val engine = GameEngine(NoOpSoundPlayer(), InMemoryScoreRepository())
            val renderer = GameRenderer()
            renderer.setSafeInsets(insets[0], insets[1], insets[2], insets[3])

            val bmp = Bitmap.createBitmap(320, 320, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bmp)

            // 1. Title screen: all 3 character cards must exist, fit inside viewport, and not overlap
            engine.selectCharacter(CharacterType.VERO)
            renderer.render(canvas, w, h, engine)

            val maxBtn = renderer.activeButtons.find { it.action == UiAction.SELECT_MAX }
            val remiBtn = renderer.activeButtons.find { it.action == UiAction.SELECT_REMI }
            val veroBtn = renderer.activeButtons.find { it.action == UiAction.SELECT_VERO }
            assertNotNull("MAX card missing on ${w}x${h}", maxBtn)
            assertNotNull("REMI card missing on ${w}x${h}", remiBtn)
            assertNotNull("VERO card missing on ${w}x${h}", veroBtn)
            assertFalse(RectF.intersects(maxBtn!!.rect, remiBtn!!.rect))
            assertFalse(RectF.intersects(remiBtn.rect, veroBtn!!.rect))
            assertTrue(veroBtn.rect.right <= renderer.virtualWidth)

            // 2. Gameplay Level 1 & Level 2: touch controls fit cleanly inside virtual screen
            for (lvl in listOf(1, 2)) {
                engine.jumpToLevel(lvl)
                engine.update(0.016f, InputState(right = true, jumpHeld = true, jumpJustPressed = true))
                renderer.render(canvas, w, h, engine, setOf(UiAction.TOUCH_RIGHT, UiAction.TOUCH_JUMP))

                val leftPad = renderer.activeButtons.find { it.action == UiAction.TOUCH_LEFT }
                val rightPad = renderer.activeButtons.find { it.action == UiAction.TOUCH_RIGHT }
                val jumpPad = renderer.activeButtons.find { it.action == UiAction.TOUCH_JUMP }
                assertNotNull(leftPad)
                assertNotNull(rightPad)
                assertNotNull(jumpPad)
                assertTrue(leftPad!!.rect.left >= 0f)
                assertTrue(jumpPad!!.rect.right <= renderer.virtualWidth)
                assertTrue(jumpPad.rect.bottom <= renderer.virtualHeight)
            }
        }
    }
}
