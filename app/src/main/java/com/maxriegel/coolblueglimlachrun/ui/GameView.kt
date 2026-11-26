package com.maxriegel.coolblueglimlachrun.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import com.maxriegel.coolblueglimlachrun.game.CharacterType
import com.maxriegel.coolblueglimlachrun.game.GameEngine
import com.maxriegel.coolblueglimlachrun.game.GameScreen
import com.maxriegel.coolblueglimlachrun.game.InputState

@SuppressLint("ViewConstructor")
class GameView(
    context: Context,
    val engine: GameEngine,
    val renderer: GameRenderer = GameRenderer()
) : View(context) {

    private var keyLeft = false
    private var keyRight = false
    private var keyJump = false
    private var jumpQueued = false

    private var touchLeft = false
    private var touchRight = false
    private var touchJump = false

    private var lastFrameNanos = 0L
    private var running = false

    private val activePointers = mutableMapOf<Int, UiAction>()
    private val pressedActions = mutableSetOf<UiAction>()

    init {
        isFocusable = true
        isFocusableInTouchMode = true
        keepScreenOn = true
    }

    fun setSafeInsets(leftPx: Float, topPx: Float, rightPx: Float, bottomPx: Float) {
        renderer.setSafeInsets(leftPx, topPx, rightPx, bottomPx)
        invalidate()
    }

    fun startLoop() {
        if (running) return
        running = true
        lastFrameNanos = System.nanoTime()
        postInvalidateOnAnimation()
    }

    fun stopLoop() {
        running = false
        clearHeldInputs()
    }

    private fun clearHeldInputs() {
        keyLeft = false
        keyRight = false
        keyJump = false
        touchLeft = false
        touchRight = false
        touchJump = false
        activePointers.clear()
        pressedActions.clear()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val now = System.nanoTime()
        val dt = if (lastFrameNanos == 0L) 0.016f else (now - lastFrameNanos) / 1_000_000_000f
        lastFrameNanos = now

        // Update viewportWidth before engine step so camera clamping matches screen form factor
        engine.viewportWidth = renderer.computeWorldViewportWidth(width, height)

        val input = InputState(
            left = keyLeft || touchLeft,
            right = keyRight || touchRight,
            jumpHeld = keyJump || touchJump,
            jumpJustPressed = jumpQueued
        )
        jumpQueued = false

        pressedActions.clear()
        if (input.left) pressedActions.add(UiAction.TOUCH_LEFT)
        if (input.right) pressedActions.add(UiAction.TOUCH_RIGHT)
        if (input.jumpHeld) pressedActions.add(UiAction.TOUCH_JUMP)

        engine.update(dt, input)
        renderer.render(canvas, width, height, engine, pressedActions)

        if (running) {
            postInvalidateOnAnimation()
        }
    }

    fun handleHardwareKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        val isRepeat = (event?.repeatCount ?: 0) > 0
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_A -> {
                if (engine.screen == GameScreen.TITLE && !isRepeat) {
                    engine.cycleCharacter(-1)
                } else {
                    keyLeft = true
                }
                return true
            }
            KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_D -> {
                if (engine.screen == GameScreen.TITLE && !isRepeat) {
                    engine.cycleCharacter(1)
                } else {
                    keyRight = true
                }
                return true
            }
            KeyEvent.KEYCODE_SPACE, KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_W, KeyEvent.KEYCODE_Z, KeyEvent.KEYCODE_BUTTON_A -> {
                if (!isRepeat) {
                    when (engine.screen) {
                        GameScreen.TITLE -> engine.startNewGame(1)
                        GameScreen.LEVEL_CLEAR -> engine.advanceFromLevelClear()
                        GameScreen.GAME_OVER -> engine.restartCurrentLevel()
                        GameScreen.VICTORY -> engine.startNewGame(1)
                        GameScreen.PLAYING -> {
                            keyJump = true
                            jumpQueued = true
                        }
                    }
                }
                return true
            }
            KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_NUMPAD_ENTER, KeyEvent.KEYCODE_BUTTON_START -> {
                if (!isRepeat) {
                    when (engine.screen) {
                        GameScreen.TITLE -> engine.startNewGame(1)
                        GameScreen.LEVEL_CLEAR -> engine.advanceFromLevelClear()
                        GameScreen.GAME_OVER -> engine.restartCurrentLevel()
                        GameScreen.VICTORY -> engine.startNewGame(1)
                        GameScreen.PLAYING -> {}
                    }
                }
                return true
            }
            KeyEvent.KEYCODE_R -> {
                if (!isRepeat && engine.screen != GameScreen.TITLE) {
                    engine.restartCurrentLevel()
                }
                return true
            }
            KeyEvent.KEYCODE_M -> {
                if (!isRepeat) engine.toggleMute()
                return true
            }
            KeyEvent.KEYCODE_B -> {
                if (!isRepeat) engine.toggleMusic()
                return true
            }
            KeyEvent.KEYCODE_G -> {
                if (!isRepeat) engine.toggleDemoMode()
                return true
            }
            KeyEvent.KEYCODE_T -> {
                if (!isRepeat) engine.toggleTouchControls()
                return true
            }
            KeyEvent.KEYCODE_1 -> {
                if (!isRepeat) engine.jumpToLevel(1)
                return true
            }
            KeyEvent.KEYCODE_2 -> {
                if (!isRepeat) engine.jumpToLevel(2)
                return true
            }
            KeyEvent.KEYCODE_ESCAPE, KeyEvent.KEYCODE_BACK -> {
                if (!isRepeat && engine.screen != GameScreen.TITLE) {
                    engine.returnToTitle()
                    return true
                }
            }
        }
        return false
    }

    fun handleHardwareKeyUp(keyCode: Int): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_A -> {
                keyLeft = false
                return true
            }
            KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_D -> {
                keyRight = false
                return true
            }
            KeyEvent.KEYCODE_SPACE, KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_W, KeyEvent.KEYCODE_Z, KeyEvent.KEYCODE_BUTTON_A -> {
                keyJump = false
                return true
            }
        }
        return false
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return handleHardwareKeyDown(keyCode, event) || super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        return handleHardwareKeyUp(keyCode) || super.onKeyUp(keyCode, event)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        requestFocus()
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                val idx = event.actionIndex
                val pointerId = event.getPointerId(idx)
                val (vx, vy) = renderer.screenToVirtual(event.getX(idx), event.getY(idx))
                val hit = findHitAction(vx, vy, allowSlop = true)
                if (hit != null) {
                    if (isContinuousTouchAction(hit)) {
                        activePointers[pointerId] = hit
                        if (hit == UiAction.TOUCH_JUMP) {
                            jumpQueued = true
                        }
                        recomputeTouchFlags()
                    } else {
                        dispatchClickAction(hit)
                    }
                }
            }
            MotionEvent.ACTION_MOVE -> {
                for (i in 0 until event.pointerCount) {
                    val pointerId = event.getPointerId(i)
                    if (activePointers.containsKey(pointerId)) {
                        val (vx, vy) = renderer.screenToVirtual(event.getX(i), event.getY(i))
                        val hit = findHitAction(vx, vy, allowSlop = true)
                        if (hit != null && isContinuousTouchAction(hit)) {
                            if (activePointers[pointerId] != UiAction.TOUCH_JUMP && hit == UiAction.TOUCH_JUMP) {
                                jumpQueued = true
                            }
                            activePointers[pointerId] = hit
                        } else {
                            activePointers.remove(pointerId)
                        }
                    }
                }
                recomputeTouchFlags()
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_CANCEL -> {
                if (event.actionMasked == MotionEvent.ACTION_CANCEL) {
                    activePointers.clear()
                } else {
                    val pointerId = event.getPointerId(event.actionIndex)
                    activePointers.remove(pointerId)
                }
                recomputeTouchFlags()
            }
        }
        return true
    }

    private fun isContinuousTouchAction(action: UiAction): Boolean {
        return action == UiAction.TOUCH_LEFT || action == UiAction.TOUCH_RIGHT || action == UiAction.TOUCH_JUMP
    }

    private fun recomputeTouchFlags() {
        touchLeft = activePointers.values.contains(UiAction.TOUCH_LEFT)
        touchRight = activePointers.values.contains(UiAction.TOUCH_RIGHT)
        touchJump = activePointers.values.contains(UiAction.TOUCH_JUMP)
    }

    private fun findHitAction(vx: Float, vy: Float, allowSlop: Boolean): UiAction? {
        for (i in renderer.activeButtons.indices.reversed()) {
            val btn = renderer.activeButtons[i]
            if (btn.rect.contains(vx, vy)) {
                return btn.action
            }
        }
        if (allowSlop) {
            // Forgiving hit slop for fast arcade thumb controls on mobile & tablet
            for (i in renderer.activeButtons.indices.reversed()) {
                val btn = renderer.activeButtons[i]
                if (!isContinuousTouchAction(btn.action)) continue
                val pad = 22f
                if (vx >= btn.rect.left - pad && vx <= btn.rect.right + pad &&
                    vy >= btn.rect.top - pad && vy <= btn.rect.bottom + pad
                ) {
                    return btn.action
                }
            }
        }
        return null
    }

    private fun dispatchClickAction(action: UiAction) {
        when (action) {
            UiAction.SELECT_MAX -> engine.selectCharacter(CharacterType.MAX)
            UiAction.SELECT_REMI -> engine.selectCharacter(CharacterType.REMI)
            UiAction.SELECT_VERO -> engine.selectCharacter(CharacterType.VERO)
            UiAction.START_GAME -> engine.startNewGame(1)
            UiAction.START_LEVEL_1 -> engine.jumpToLevel(1)
            UiAction.START_LEVEL_2 -> engine.jumpToLevel(2)
            UiAction.TOGGLE_DEMO -> engine.toggleDemoMode()
            UiAction.TOGGLE_TOUCH -> engine.toggleTouchControls()
            UiAction.TOGGLE_MUSIC -> engine.toggleMusic()
            UiAction.TOGGLE_MUTE -> engine.toggleMute()
            UiAction.RESTART -> engine.restartCurrentLevel()
            UiAction.RETURN_TITLE -> engine.returnToTitle()
            UiAction.NEXT_LEVEL -> engine.advanceFromLevelClear()
            else -> {}
        }
    }
}
