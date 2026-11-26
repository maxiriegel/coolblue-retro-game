package com.maxriegel.coolblueglimlachrun.ui

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import com.maxriegel.coolblueglimlachrun.game.BoxReward
import com.maxriegel.coolblueglimlachrun.game.CharacterType
import com.maxriegel.coolblueglimlachrun.game.Enemy
import com.maxriegel.coolblueglimlachrun.game.EnemyType
import com.maxriegel.coolblueglimlachrun.game.GameConfig
import com.maxriegel.coolblueglimlachrun.game.GameEngine
import com.maxriegel.coolblueglimlachrun.game.GameScreen
import com.maxriegel.coolblueglimlachrun.game.LevelData
import com.maxriegel.coolblueglimlachrun.game.LevelTheme
import com.maxriegel.coolblueglimlachrun.game.Particle
import com.maxriegel.coolblueglimlachrun.game.ParticleShape
import com.maxriegel.coolblueglimlachrun.game.Platform
import com.maxriegel.coolblueglimlachrun.game.PlatformType
import com.maxriegel.coolblueglimlachrun.game.QuestionBox
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

enum class UiAction {
    SELECT_MAX,
    SELECT_REMI,
    SELECT_VERO,
    START_GAME,
    START_LEVEL_1,
    START_LEVEL_2,
    TOGGLE_DEMO,
    TOGGLE_TOUCH,
    TOGGLE_MUSIC,
    TOGGLE_MUTE,
    RESTART,
    RETURN_TITLE,
    NEXT_LEVEL,
    TOUCH_LEFT,
    TOUCH_RIGHT,
    TOUCH_JUMP
}

data class HitButton(
    val action: UiAction,
    val rect: RectF
)

class GameRenderer {
    val activeButtons: MutableList<HitButton> = mutableListOf()

    private val coolblueBlue = Color.parseColor("#0090E3")
    private val coolblueDark = Color.parseColor("#005F9E")
    private val coolblueNavy = Color.parseColor("#002B54")
    private val coolblueDeepNavy = Color.parseColor("#001B38")
    private val coolblueOrange = Color.parseColor("#FF6600")
    private val coolblueOrangeBright = Color.parseColor("#FF8822")
    private val veroTeal = Color.parseColor("#00A896")
    private val smileGold = Color.parseColor("#FFC400")
    private val white = Color.WHITE

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
    }
    private val rect = RectF()
    private val path = Path()

    // Dynamic responsive virtual coordinate space
    var virtualWidth: Float = GameConfig.VIEW_WIDTH
        private set
    var virtualHeight: Float = GameConfig.VIEW_HEIGHT
        private set
    var isPortraitLayout: Boolean = false
        private set
    var scaleFactor: Float = 1f
        private set
    var offsetX: Float = 0f
        private set
    var offsetY: Float = 0f
        private set

    private var rawInsetLeftPx: Float = 0f
    private var rawInsetTopPx: Float = 0f
    private var rawInsetRightPx: Float = 0f
    private var rawInsetBottomPx: Float = 0f

    private var safeLeft: Float = 14f
    private var safeTop: Float = 8f
    private var safeRight: Float = 14f
    private var safeBottom: Float = 10f

    fun setSafeInsets(leftPx: Float, topPx: Float, rightPx: Float, bottomPx: Float) {
        rawInsetLeftPx = leftPx
        rawInsetTopPx = topPx
        rawInsetRightPx = rightPx
        rawInsetBottomPx = bottomPx
    }

    fun computeWorldViewportWidth(viewWidth: Int, viewHeight: Int): Float {
        if (viewWidth <= 0 || viewHeight <= 0) return GameConfig.VIEW_WIDTH
        return if (viewHeight > viewWidth * 1.05f) {
            760f
        } else {
            val aspect = viewWidth.toFloat() / viewHeight.toFloat()
            (GameConfig.VIEW_HEIGHT * aspect).coerceIn(960f, 1680f)
        }
    }

    private fun updateViewportMetrics(viewWidth: Int, viewHeight: Int) {
        val w = viewWidth.coerceAtLeast(1).toFloat()
        val h = viewHeight.coerceAtLeast(1).toFloat()
        if (h > w * 1.05f) {
            isPortraitLayout = true
            virtualWidth = 760f
            virtualHeight = (760f * (h / w)).coerceIn(1020f, 1820f)
        } else {
            isPortraitLayout = false
            virtualHeight = GameConfig.VIEW_HEIGHT
            virtualWidth = (GameConfig.VIEW_HEIGHT * (w / h)).coerceIn(960f, 1680f)
        }
        val sx = w / virtualWidth
        val sy = h / virtualHeight
        scaleFactor = min(sx, sy).coerceAtLeast(0.001f)
        offsetX = (w - virtualWidth * scaleFactor) / 2f
        offsetY = (h - virtualHeight * scaleFactor) / 2f

        safeLeft = (rawInsetLeftPx / scaleFactor).coerceIn(0f, 96f)
        safeTop = (rawInsetTopPx / scaleFactor).coerceIn(0f, 84f)
        safeRight = (rawInsetRightPx / scaleFactor).coerceIn(0f, 96f)
        safeBottom = (rawInsetBottomPx / scaleFactor).coerceIn(0f, 84f)
    }

    fun screenToVirtual(sx: Float, sy: Float): Pair<Float, Float> {
        val vx = (sx - offsetX) / scaleFactor.coerceAtLeast(0.001f)
        val vy = (sy - offsetY) / scaleFactor.coerceAtLeast(0.001f)
        return vx to vy
    }

    fun render(
        canvas: Canvas,
        viewWidth: Int,
        viewHeight: Int,
        engine: GameEngine,
        pressedActions: Set<UiAction> = emptySet()
    ) {
        activeButtons.clear()
        updateViewportMetrics(viewWidth, viewHeight)
        engine.viewportWidth = virtualWidth

        canvas.drawColor(coolblueDeepNavy)

        canvas.save()
        canvas.translate(offsetX, offsetY)
        canvas.scale(scaleFactor, scaleFactor)
        canvas.clipRect(0f, 0f, virtualWidth, virtualHeight)

        if (engine.screen == GameScreen.TITLE) {
            if (isPortraitLayout) {
                drawTitleScreenPortrait(canvas, engine)
            } else {
                drawTitleScreenLandscape(canvas, engine)
            }
        } else {
            if (isPortraitLayout) {
                drawGameplayPortrait(canvas, engine, pressedActions)
            } else {
                drawGameplayWorld(canvas, engine, worldTopClip = 0f, worldOffsetY = 0f, worldBottomClip = virtualHeight)
                drawHudLandscape(canvas, engine)
                if (engine.screen == GameScreen.PLAYING && engine.touchControlsVisible) {
                    drawTouchControlsLandscape(canvas, pressedActions)
                }
            }

            when (engine.screen) {
                GameScreen.LEVEL_CLEAR -> drawLevelClearOverlay(canvas, engine)
                GameScreen.VICTORY -> drawVictoryOverlay(canvas, engine)
                GameScreen.GAME_OVER -> drawGameOverOverlay(canvas, engine)
                else -> {}
            }
        }

        canvas.restore()
    }

    // =========================================================================
    // TITLE SCREEN (LANDSCAPE / TABLET & PORTRAIT)
    // =========================================================================

    private fun drawTitleBackground(canvas: Canvas, time: Float) {
        fillPaint.shader = LinearGradient(
            0f, 0f, 0f, virtualHeight,
            Color.parseColor("#0098EE"), Color.parseColor("#004A87"),
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, virtualWidth, virtualHeight, fillPaint)
        fillPaint.shader = null

        // Subtle sunlit diagonal sheen
        fillPaint.color = Color.argb(22, 255, 255, 255)
        path.reset()
        path.moveTo(virtualWidth * 0.15f, 0f)
        path.lineTo(virtualWidth * 0.42f, 0f)
        path.lineTo(virtualWidth * 0.18f, virtualHeight)
        path.lineTo(-virtualWidth * 0.05f, virtualHeight)
        path.close()
        canvas.drawPath(path, fillPaint)

        drawDecorativeCanalSkyline(canvas, time)

        // Floating animated Glimlach bubbles
        for (i in 0..8) {
            val sx = ((i * 175f + time * 28f) % (virtualWidth + 90f)) - 45f
            val sy = 90f + sin(time * 2f + i) * 24f + (i % 4) * (virtualHeight * 0.19f)
            fillPaint.color = Color.argb(34, 255, 255, 255)
            canvas.drawCircle(sx, sy, 20f + (i % 3) * 5f, fillPaint)
            strokePaint.color = Color.argb(45, 255, 196, 0)
            strokePaint.strokeWidth = 2.2f
            canvas.drawArc(RectF(sx - 10f, sy - 6f, sx + 10f, sy + 9f), 20f, 140f, false, strokePaint)
        }
    }

    private fun drawTitleScreenLandscape(canvas: Canvas, engine: GameEngine) {
        drawTitleBackground(canvas, engine.worldTime)

        val contentW = (virtualWidth - safeLeft - safeRight - 48f).coerceIn(880f, 1320f)
        val leftX = (virtualWidth - contentW) / 2f
        val rightX = leftX + contentW
        val centerX = virtualWidth / 2f
        val topY = safeTop + 10f

        // Top right quick demo/music/mute toggles
        drawPillButton(
            canvas,
            RectF(rightX - 332f, topY, rightX - 186f, topY + 38f),
            if (engine.demoModeEnabled) "⚡ Demo: AAN" else "⚡ Demo: UIT",
            if (engine.demoModeEnabled) coolblueOrange else Color.argb(175, 0, 43, 84),
            white,
            UiAction.TOGGLE_DEMO,
            15.5f
        )
        drawPillButton(
            canvas,
            RectF(rightX - 178f, topY, rightX - 92f, topY + 38f),
            if (engine.isMusicEnabled) "🎵 Aan" else "🎵 Uit",
            if (engine.isMusicEnabled) Color.parseColor("#0069B4") else Color.argb(175, 0, 43, 84),
            white,
            UiAction.TOGGLE_MUSIC,
            15.5f
        )
        drawPillButton(
            canvas,
            RectF(rightX - 84f, topY, rightX, topY + 38f),
            if (engine.isMuted) "🔇 Uit" else "🔊 Aan",
            Color.argb(175, 0, 43, 84),
            white,
            UiAction.TOGGLE_MUTE,
            15.5f
        )

        // Coolblue Hero Logo Header (placed cleanly below top utility row)
        val titleSize = if (contentW < 1040f) 46f else 54f
        textPaint.textAlign = Paint.Align.CENTER
        textPaint.color = Color.argb(95, 0, 22, 48)
        textPaint.textSize = titleSize
        canvas.drawText("COOLBLUE GLIMLACH RUN", centerX + 4f, topY + 92f, textPaint)
        textPaint.color = white
        canvas.drawText("COOLBLUE GLIMLACH RUN", centerX, topY + 88f, textPaint)

        // Orange accent badge with Max, Remi & Vero
        rect.set(centerX - 280f, topY + 102f, centerX + 280f, topY + 140f)
        fillPaint.color = coolblueOrange
        canvas.drawRoundRect(rect, 19f, 19f, fillPaint)
        textPaint.color = white
        textPaint.textSize = 20f
        canvas.drawText("Alles voor een glimlach!  •  Max, Remi & Vero", centerX, topY + 128f, textPaint)

        // Character Selection Section (Left 66%) + Leaderboard (Right 32%)
        val cardsAreaW = contentW * 0.66f
        val gap = 14f
        val cardW = (cardsAreaW - gap * 2f) / 3f
        val cardTop = 206f
        val cardBottom = 494f

        textPaint.color = white
        textPaint.textSize = 22f
        textPaint.textAlign = Paint.Align.LEFT
        canvas.drawText("Kies je bezorger:", leftX + 4f, cardTop - 12f, textPaint)

        val characters = listOf(
            CharacterType.MAX to UiAction.SELECT_MAX,
            CharacterType.REMI to UiAction.SELECT_REMI,
            CharacterType.VERO to UiAction.SELECT_VERO
        )
        characters.forEachIndexed { idx, (charType, action) ->
            val cx0 = leftX + idx * (cardW + gap)
            drawCharacterSelectCard(
                canvas = canvas,
                cardRect = RectF(cx0, cardTop, cx0 + cardW, cardBottom),
                character = charType,
                selected = engine.selectedCharacter == charType,
                time = engine.worldTime,
                action = action
            )
        }

        // Leaderboard Panel on the right
        val lbLeft = leftX + cardsAreaW + 20f
        drawLeaderboardCard(canvas, RectF(lbLeft, cardTop, rightX, cardBottom), engine)

        // Bottom Action Row
        val btnTop = 516f
        val btnBot = 604f
        val startW = contentW * 0.39f
        val lvlW = contentW * 0.20f
        val touchW = contentW - startW - lvlW * 2f - 36f

        drawPillButton(
            canvas,
            RectF(leftX, btnTop, leftX + startW, btnBot),
            "▶  START ALS ${engine.selectedCharacter.displayName.uppercase()}!",
            coolblueOrange,
            white,
            UiAction.START_GAME,
            if (contentW < 1040f) 23f else 27f
        )
        val l1Left = leftX + startW + 12f
        drawPillButton(
            canvas,
            RectF(l1Left, btnTop, l1Left + lvlW, btnBot),
            "1-1 Magazijn",
            Color.parseColor("#004E89"),
            white,
            UiAction.START_LEVEL_1,
            21f
        )
        val l2Left = l1Left + lvlW + 12f
        drawPillButton(
            canvas,
            RectF(l2Left, btnTop, l2Left + lvlW, btnBot),
            "1-2 Grachten",
            Color.parseColor("#004E89"),
            white,
            UiAction.START_LEVEL_2,
            21f
        )
        val tLeft = l2Left + lvlW + 12f
        drawPillButton(
            canvas,
            RectF(tLeft, btnTop, rightX, btnBot),
            if (engine.touchControlsVisible) "🎮 Touch: AAN" else "🎮 Touch: UIT",
            Color.argb(190, 0, 43, 84),
            white,
            UiAction.TOGGLE_TOUCH,
            18f
        )

        // Controls footer hint
        val footerTop = 626f
        rect.set(leftX, footerTop, rightX, min(680f, virtualHeight - safeBottom - 6f))
        fillPaint.color = Color.argb(150, 0, 26, 56)
        canvas.drawRoundRect(rect, 15f, 15f, fillPaint)
        textPaint.textAlign = Paint.Align.CENTER
        textPaint.color = Color.parseColor("#D6F0FF")
        textPaint.textSize = if (contentW < 1040f) 14f else 16.5f
        canvas.drawText(
            "← → / A D = Rennen  •  Spatie / ↑ / W = Springen  •  1 / 2 = Level  •  G = Demo  •  B = Muziek  •  M = Geluid",
            centerX,
            rect.centerY() + 6f,
            textPaint
        )
    }

    private fun drawTitleScreenPortrait(canvas: Canvas, engine: GameEngine) {
        drawTitleBackground(canvas, engine.worldTime)

        val padX = max(24f, safeLeft + 14f)
        val leftX = padX
        val rightX = virtualWidth - max(24f, safeRight + 14f)
        val contentW = rightX - leftX
        val centerX = virtualWidth / 2f
        var curY = safeTop + 18f

        // Top quick bar
        val qGap = 10f
        val qW = (contentW - qGap * 3f) / 4f
        drawPillButton(
            canvas,
            RectF(leftX, curY, leftX + qW, curY + 46f),
            if (engine.demoModeEnabled) "⚡ Demo AAN" else "⚡ Demo UIT",
            if (engine.demoModeEnabled) coolblueOrange else Color.argb(180, 0, 43, 84),
            white,
            UiAction.TOGGLE_DEMO,
            17f
        )
        drawPillButton(
            canvas,
            RectF(leftX + (qW + qGap), curY, leftX + qW * 2f + qGap, curY + 46f),
            if (engine.isMusicEnabled) "🎵 Muziek" else "🎵 Stil",
            if (engine.isMusicEnabled) Color.parseColor("#0069B4") else Color.argb(180, 0, 43, 84),
            white,
            UiAction.TOGGLE_MUSIC,
            17f
        )
        drawPillButton(
            canvas,
            RectF(leftX + (qW + qGap) * 2f, curY, leftX + qW * 3f + qGap * 2f, curY + 46f),
            if (engine.isMuted) "🔇 Geluid" else "🔊 Geluid",
            Color.argb(180, 0, 43, 84),
            white,
            UiAction.TOGGLE_MUTE,
            17f
        )
        drawPillButton(
            canvas,
            RectF(leftX + (qW + qGap) * 3f, curY, rightX, curY + 46f),
            if (engine.touchControlsVisible) "🎮 Knoppen" else "🎮 Verborgen",
            Color.argb(180, 0, 43, 84),
            white,
            UiAction.TOGGLE_TOUCH,
            16f
        )
        curY += 76f

        // Hero Header
        textPaint.textAlign = Paint.Align.CENTER
        textPaint.color = Color.argb(95, 0, 22, 48)
        textPaint.textSize = 46f
        canvas.drawText("COOLBLUE GLIMLACH RUN", centerX + 3f, curY + 42f, textPaint)
        textPaint.color = white
        canvas.drawText("COOLBLUE GLIMLACH RUN", centerX, curY + 39f, textPaint)
        curY += 56f

        rect.set(centerX - 260f, curY, centerX + 260f, curY + 42f)
        fillPaint.color = coolblueOrange
        canvas.drawRoundRect(rect, 21f, 21f, fillPaint)
        textPaint.color = white
        textPaint.textSize = 21f
        canvas.drawText("Alles voor een glimlach!  •  Max, Remi & Vero", centerX, curY + 28f, textPaint)
        curY += 68f

        textPaint.textAlign = Paint.Align.LEFT
        textPaint.color = white
        textPaint.textSize = 24f
        canvas.drawText("Kies je bezorger:", leftX + 4f, curY, textPaint)
        curY += 16f

        // 3 Character Cards side-by-side
        val cardGap = 12f
        val cardW = (contentW - cardGap * 2f) / 3f
        val cardH = (virtualHeight * 0.22f).coerceIn(245f, 305f)
        val characters = listOf(
            CharacterType.MAX to UiAction.SELECT_MAX,
            CharacterType.REMI to UiAction.SELECT_REMI,
            CharacterType.VERO to UiAction.SELECT_VERO
        )
        characters.forEachIndexed { idx, (charType, action) ->
            val cx0 = leftX + idx * (cardW + cardGap)
            drawCharacterSelectCard(
                canvas = canvas,
                cardRect = RectF(cx0, curY, cx0 + cardW, curY + cardH),
                character = charType,
                selected = engine.selectedCharacter == charType,
                time = engine.worldTime,
                action = action
            )
        }
        curY += cardH + 22f

        // Big Full-Width Start Button
        drawPillButton(
            canvas,
            RectF(leftX, curY, rightX, curY + 86f),
            "▶  START ALS ${engine.selectedCharacter.displayName.uppercase()}!",
            coolblueOrange,
            white,
            UiAction.START_GAME,
            30f
        )
        curY += 102f

        // Level Select Row
        val halfW = (contentW - 14f) / 2f
        drawPillButton(
            canvas,
            RectF(leftX, curY, leftX + halfW, curY + 66f),
            "📦  1-1 Magazijn",
            Color.parseColor("#004E89"),
            white,
            UiAction.START_LEVEL_1,
            23f
        )
        drawPillButton(
            canvas,
            RectF(leftX + halfW + 14f, curY, rightX, curY + 66f),
            "🚲  1-2 Grachten",
            Color.parseColor("#004E89"),
            white,
            UiAction.START_LEVEL_2,
            23f
        )
        curY += 84f

        // Leaderboard fills remaining lower space cleanly
        val lbBottom = (virtualHeight - safeBottom - 20f).coerceAtLeast(curY + 180f)
        drawLeaderboardCard(canvas, RectF(leftX, curY, rightX, lbBottom), engine)
    }

    private fun drawCharacterSelectCard(
        canvas: Canvas,
        cardRect: RectF,
        character: CharacterType,
        selected: Boolean,
        time: Float,
        action: UiAction
    ) {
        activeButtons.add(HitButton(action, RectF(cardRect)))

        // Subtle drop shadow
        fillPaint.color = Color.argb(55, 0, 15, 36)
        canvas.drawRoundRect(
            RectF(cardRect.left + 3f, cardRect.top + 6f, cardRect.right + 3f, cardRect.bottom + 6f),
            22f, 22f, fillPaint
        )

        fillPaint.color = if (selected) white else Color.argb(232, 232, 246, 255)
        canvas.drawRoundRect(cardRect, 22f, 22f, fillPaint)

        strokePaint.color = if (selected) character.accentColorArgb else Color.argb(130, 255, 255, 255)
        strokePaint.strokeWidth = if (selected) 6.5f else 2.5f
        canvas.drawRoundRect(cardRect, 22f, 22f, strokePaint)

        // Soft stage spotlight circle behind hero
        fillPaint.color = if (selected) {
            Color.argb(38, Color.red(character.accentColorArgb), Color.green(character.accentColorArgb), Color.blue(character.accentColorArgb))
        } else {
            Color.argb(22, 0, 95, 158)
        }
        canvas.drawCircle(cardRect.centerX(), cardRect.top + cardRect.height() * 0.43f, min(cardRect.width() * 0.34f, 64f), fillPaint)

        // Selected badge
        if (selected) {
            val badgeW = min(118f, cardRect.width() - 20f)
            rect.set(cardRect.right - badgeW - 10f, cardRect.top + 10f, cardRect.right - 10f, cardRect.top + 38f)
            fillPaint.color = character.accentColorArgb
            canvas.drawRoundRect(rect, 12f, 12f, fillPaint)
            textPaint.textAlign = Paint.Align.CENTER
            textPaint.color = white
            textPaint.textSize = 15f
            canvas.drawText("GEKOZEN ✓", rect.centerX(), rect.centerY() + 5f, textPaint)
        }

        // Animated character showcase inside card
        val bounceY = if (selected) abs(sin(time * 5.2f)) * 12f else sin(time * 2.2f + character.ordinal) * 2f
        val spriteScale = (cardRect.width() / 160f).coerceIn(1.28f, 1.62f)
        canvas.save()
        val cx = cardRect.centerX()
        val cy = cardRect.top + cardRect.height() * 0.44f - bounceY
        canvas.translate(cx, cy)
        canvas.scale(spriteScale, spriteScale)
        drawHeroSprite(
            canvas = canvas,
            x = -22f,
            y = -31f,
            width = 44f,
            height = 62f,
            character = character,
            facingRight = true,
            runningAnim = if (selected) time * 6f else 0f,
            inAir = selected && bounceY > 5f,
            hasShield = false,
            worldTime = time,
            showNameTag = false,
            squashX = 1f,
            stretchY = 1f,
            isHurt = false
        )
        canvas.restore()

        // Name & role subtitle
        val compact = cardRect.width() < 225f
        textPaint.textAlign = Paint.Align.CENTER
        textPaint.color = coolblueNavy
        textPaint.textSize = if (compact) 27f else 32f
        canvas.drawText(character.displayName.uppercase(), cardRect.centerX(), cardRect.bottom - 48f, textPaint)

        textPaint.color = character.accentColorArgb
        textPaint.textSize = if (compact) 15.5f else 18f
        canvas.drawText(character.badgeTitle, cardRect.centerX(), cardRect.bottom - 20f, textPaint)
    }

    private fun drawLeaderboardCard(canvas: Canvas, cardRect: RectF, engine: GameEngine) {
        fillPaint.color = Color.argb(218, 0, 34, 70)
        canvas.drawRoundRect(cardRect, 22f, 22f, fillPaint)
        strokePaint.color = Color.argb(120, 255, 255, 255)
        strokePaint.strokeWidth = 2.5f
        canvas.drawRoundRect(cardRect, 22f, 22f, strokePaint)

        textPaint.textAlign = Paint.Align.CENTER
        textPaint.color = smileGold
        textPaint.textSize = 23f
        canvas.drawText("🏆 TOP BEZORGERS", cardRect.centerX(), cardRect.top + 38f, textPaint)

        val maxRows = ((cardRect.height() - 62f) / 40f).toInt().coerceIn(2, 5)
        val entries = engine.getLeaderboard(maxRows)
        val narrowLb = cardRect.width() < 335f
        if (entries.isEmpty()) {
            textPaint.color = Color.parseColor("#B8E2FF")
            textPaint.textSize = if (narrowLb) 15.5f else 19f
            canvas.drawText("Nog geen ritten opgeslagen.", cardRect.centerX(), cardRect.centerY() - 12f, textPaint)
            textPaint.color = white
            textPaint.textSize = if (narrowLb) 15f else 18f
            canvas.drawText("Wie pakt het record:", cardRect.centerX(), cardRect.centerY() + 20f, textPaint)
            textPaint.color = coolblueOrangeBright
            textPaint.textSize = if (narrowLb) 17.5f else 21f
            canvas.drawText("Max, Remi of Vero?", cardRect.centerX(), cardRect.centerY() + 50f, textPaint)
        } else {
            var y = cardRect.top + 78f
            entries.forEachIndexed { index, entry ->
                rect.set(cardRect.left + 14f, y - 24f, cardRect.right - 14f, y + 11f)
                fillPaint.color = if (index == 0) Color.argb(78, 255, 196, 0) else Color.argb(45, 255, 255, 255)
                canvas.drawRoundRect(rect, 10f, 10f, fillPaint)

                // Character color dot
                fillPaint.color = entry.character.accentColorArgb
                canvas.drawCircle(rect.left + 16f, y - 6f, 6f, fillPaint)

                textPaint.textAlign = Paint.Align.LEFT
                textPaint.color = if (index == 0) smileGold else white
                textPaint.textSize = 18.5f
                canvas.drawText("${index + 1}. ${entry.character.displayName}", rect.left + 28f, y, textPaint)

                textPaint.textAlign = Paint.Align.RIGHT
                textPaint.color = Color.parseColor("#7CE0FF")
                canvas.drawText("${entry.score} pt  •  😄${entry.smiles}", rect.right - 12f, y, textPaint)
                y += 39f
            }
        }
    }

    private fun drawDecorativeCanalSkyline(canvas: Canvas, time: Float) {
        val baseY = virtualHeight
        val colors = intArrayOf(
            Color.argb(65, 0, 32, 66),
            Color.argb(80, 0, 42, 82),
            Color.argb(55, 0, 25, 55)
        )
        var x = 0f
        var idx = 0
        while (x < virtualWidth + 90f) {
            val w = 78f + (idx % 3) * 18f
            val h = 115f + (idx % 4) * 28f
            fillPaint.color = colors[idx % colors.size]
            canvas.drawRect(x, baseY - h, x + w - 4f, baseY, fillPaint)
            canvas.drawRect(x + 12f, baseY - h - 18f, x + w - 16f, baseY - h, fillPaint)
            canvas.drawRect(x + 24f, baseY - h - 32f, x + w - 28f, baseY - h - 18f, fillPaint)
            x += w
            idx++
        }
        fillPaint.color = Color.argb(45, 255, 255, 255)
        val wave = sin(time * 2.4f) * 12f
        canvas.drawRect(0f, baseY - 22f + wave * 0.2f, virtualWidth, baseY, fillPaint)
    }

    // =========================================================================
    // PORTRAIT GAMEPLAY + COOLBLUE ARCADE CONTROL DECK
    // =========================================================================

    private fun drawGameplayPortrait(
        canvas: Canvas,
        engine: GameEngine,
        pressedActions: Set<UiAction>
    ) {
        val deckHeight = if (engine.touchControlsVisible) {
            (virtualHeight * 0.27f).coerceIn(250f, 430f)
        } else {
            92f + safeBottom
        }
        val worldBottomY = (virtualHeight - deckHeight).coerceAtLeast(720f)
        val worldOffsetY = worldBottomY - GameConfig.VIEW_HEIGHT

        drawGameplayWorld(
            canvas = canvas,
            engine = engine,
            worldTopClip = 0f,
            worldOffsetY = worldOffsetY,
            worldBottomClip = worldBottomY
        )
        drawHudPortrait(canvas, engine)
        drawPortraitArcadeDeck(canvas, engine, worldBottomY, pressedActions)
    }

    private fun drawHudPortrait(canvas: Canvas, engine: GameEngine) {
        val topPad = safeTop
        val barH = topPad + 76f
        fillPaint.color = Color.argb(220, 0, 34, 70)
        canvas.drawRect(0f, 0f, virtualWidth, barH, fillPaint)
        fillPaint.color = coolblueOrange
        canvas.drawRect(0f, barH - 3f, virtualWidth, barH, fillPaint)

        val row1Y = topPad + 31f
        val row2Y = topPad + 62f

        // Hero pill + Hearts
        fillPaint.color = engine.selectedCharacter.accentColorArgb
        rect.set(16f, topPad + 9f, 108f, topPad + 39f)
        canvas.drawRoundRect(rect, 12f, 12f, fillPaint)
        textPaint.textAlign = Paint.Align.CENTER
        textPaint.color = white
        textPaint.textSize = 18f
        canvas.drawText(engine.selectedCharacter.displayName.uppercase(), rect.centerX(), topPad + 30f, textPaint)

        val heartsText = buildString {
            for (i in 1..GameConfig.STARTING_HEARTS) {
                append(if (i <= engine.hearts) "♥" else "♡")
            }
        }
        textPaint.textAlign = Paint.Align.LEFT
        textPaint.color = Color.parseColor("#FF5252")
        textPaint.textSize = 25f
        canvas.drawText(heartsText, 120f, row1Y, textPaint)

        if (engine.player.hasBakfietsShield) {
            rect.set(206f, topPad + 9f, 314f, topPad + 39f)
            fillPaint.color = coolblueBlue
            canvas.drawRoundRect(rect, 12f, 12f, fillPaint)
            textPaint.textAlign = Paint.Align.CENTER
            textPaint.color = white
            textPaint.textSize = 14.5f
            canvas.drawText("🚲 TURBO", rect.centerX(), topPad + 29f, textPaint)
        }

        // Right side of Row 1: Smiles, Score, Timer
        textPaint.textAlign = Paint.Align.RIGHT
        textPaint.textSize = 21f
        textPaint.color = if (engine.timeRemaining < 20f) Color.parseColor("#FF6E6E") else Color.parseColor("#B8E2FF")
        canvas.drawText("⏱ ${engine.timeRemaining.toInt()}s", virtualWidth - 18f, row1Y, textPaint)
        textPaint.color = white
        canvas.drawText("${engine.score} pt", virtualWidth - 112f, row1Y, textPaint)
        textPaint.color = smileGold
        canvas.drawText("😄 ${engine.smilesCollected}", virtualWidth - 238f, row1Y, textPaint)

        // Row 2: Level title + wide progress bar
        textPaint.textAlign = Paint.Align.LEFT
        textPaint.color = Color.parseColor("#8CE0FF")
        textPaint.textSize = 17.5f
        canvas.drawText("${engine.level.code} ${engine.level.title}", 18f, row2Y, textPaint)

        val prog = (engine.player.x / engine.level.flagX).coerceIn(0f, 1f)
        val barLeft = 295f
        val barRight = virtualWidth - 18f
        rect.set(barLeft, row2Y - 12f, barRight, row2Y - 1f)
        fillPaint.color = Color.argb(95, 255, 255, 255)
        canvas.drawRoundRect(rect, 5.5f, 5.5f, fillPaint)
        fillPaint.color = coolblueOrange
        canvas.drawRoundRect(RectF(barLeft, row2Y - 12f, barLeft + (barRight - barLeft) * prog, row2Y - 1f), 5.5f, 5.5f, fillPaint)

        // Banner toast
        val msg = engine.bannerMessage
        if (msg != null && engine.screen == GameScreen.PLAYING) {
            val alpha = (min(1f, engine.bannerTimer * 1.4f) * 235).toInt()
            rect.set(36f, barH + 12f, virtualWidth - 36f, barH + 54f)
            fillPaint.color = Color.argb(alpha, 0, 144, 227)
            canvas.drawRoundRect(rect, 20f, 20f, fillPaint)
            textPaint.textAlign = Paint.Align.CENTER
            textPaint.color = Color.argb(alpha, 255, 255, 255)
            textPaint.textSize = 18.5f
            canvas.drawText(msg, rect.centerX(), rect.centerY() + 6f, textPaint)
        }
    }

    private fun drawPortraitArcadeDeck(
        canvas: Canvas,
        engine: GameEngine,
        deckTop: Float,
        pressedActions: Set<UiAction>
    ) {
        // Tactile Coolblue Arcade Control Deck below the world
        fillPaint.shader = LinearGradient(
            0f, deckTop, 0f, virtualHeight,
            Color.parseColor("#003566"), Color.parseColor("#001B38"),
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, deckTop, virtualWidth, virtualHeight, fillPaint)
        fillPaint.shader = null

        fillPaint.color = coolblueOrange
        canvas.drawRect(0f, deckTop, virtualWidth, deckTop + 5f, fillPaint)

        // Top utility strip inside the deck
        val utilTop = deckTop + 16f
        val utilBot = utilTop + 44f
        val padX = 18f
        val gap = 8f
        val count = 7
        val pillW = (virtualWidth - padX * 2f - gap * (count - 1)) / count

        val utils = listOf(
            Triple(if (engine.demoModeEnabled) "⚡ Demo" else "⚡ Norm", if (engine.demoModeEnabled) coolblueOrange else coolblueDark, UiAction.TOGGLE_DEMO),
            Triple("L1", if (engine.currentLevelNumber == 1) coolblueBlue else Color.parseColor("#1D4366"), UiAction.START_LEVEL_1),
            Triple("L2", if (engine.currentLevelNumber == 2) coolblueBlue else Color.parseColor("#1D4366"), UiAction.START_LEVEL_2),
            Triple("🎮", if (engine.touchControlsVisible) coolblueBlue else Color.parseColor("#1D4366"), UiAction.TOGGLE_TOUCH),
            Triple(if (engine.isMusicEnabled) "🎵" else "🎵✕", if (engine.isMusicEnabled) coolblueBlue else Color.parseColor("#1D4366"), UiAction.TOGGLE_MUSIC),
            Triple(if (engine.isMuted) "🔇" else "🔊", Color.parseColor("#1D4366"), UiAction.TOGGLE_MUTE),
            Triple("⌂", Color.parseColor("#1D4366"), UiAction.RETURN_TITLE)
        )
        utils.forEachIndexed { i, (label, bg, act) ->
            val lx = padX + i * (pillW + gap)
            drawPillButton(canvas, RectF(lx, utilTop, lx + pillW, utilBot), label, bg, white, act, 16.5f)
        }

        if (!engine.touchControlsVisible || engine.screen != GameScreen.PLAYING) return

        val controlsTop = utilBot + 18f
        val controlsBot = virtualHeight - max(18f, safeBottom + 10f)
        val availH = (controlsBot - controlsTop).coerceAtLeast(130f)
        val btnH = min(168f, availH * 0.88f)
        val cy = (controlsTop + controlsBot) / 2f

        // Ergonomic Left / Right thumb pads
        drawTouchPad(
            canvas,
            RectF(26f, cy - btnH / 2f, 184f, cy + btnH / 2f),
            "◀",
            UiAction.TOUCH_LEFT,
            isOrange = false,
            isPressed = pressedActions.contains(UiAction.TOUCH_LEFT)
        )
        drawTouchPad(
            canvas,
            RectF(198f, cy - btnH / 2f, 356f, cy + btnH / 2f),
            "▶",
            UiAction.TOUCH_RIGHT,
            isOrange = false,
            isPressed = pressedActions.contains(UiAction.TOUCH_RIGHT)
        )

        // Center Restart Pill + Coolblue Smile Emblem
        drawPillButton(
            canvas,
            RectF(382f, cy - 26f, 498f, cy + 26f),
            "↺ Herstart",
            Color.parseColor("#144272"),
            Color.parseColor("#B8E2FF"),
            UiAction.RESTART,
            17f
        )

        // Giant Bouncy Orange SPRING Button on Right
        val jumpSize = min(184f, availH * 0.94f)
        drawTouchPad(
            canvas,
            RectF(virtualWidth - 28f - jumpSize, cy - jumpSize / 2f, virtualWidth - 28f, cy + jumpSize / 2f),
            "SPRING",
            UiAction.TOUCH_JUMP,
            isOrange = true,
            isPressed = pressedActions.contains(UiAction.TOUCH_JUMP)
        )
    }

    // =========================================================================
    // WORLD RENDERING (PARALLAX, ENTITIES, SPRITES, PARTICLES)
    // =========================================================================

    private fun drawGameplayWorld(
        canvas: Canvas,
        engine: GameEngine,
        worldTopClip: Float,
        worldOffsetY: Float,
        worldBottomClip: Float
    ) {
        val shakeX = if (engine.screenShakeTimer > 0f) sin(engine.worldTime * 80f) * 7f else 0f
        val shakeY = if (engine.screenShakeTimer > 0f) cos(engine.worldTime * 75f) * 5f else 0f

        canvas.save()
        canvas.clipRect(0f, worldTopClip, virtualWidth, worldBottomClip)

        // Fill upper extended sky seamlessly if portrait offset is positive
        when (engine.level.theme) {
            LevelTheme.WAREHOUSE -> {
                if (worldOffsetY > 0f) {
                    fillPaint.color = Color.parseColor("#CBEBFF")
                    canvas.drawRect(0f, 0f, virtualWidth, worldOffsetY + 2f, fillPaint)
                }
                canvas.save()
                canvas.translate(0f, worldOffsetY)
                drawWarehouseBackground(canvas, engine.cameraX, engine.worldTime)
                canvas.restore()
            }
            LevelTheme.CANALS -> {
                if (worldOffsetY > 0f) {
                    fillPaint.color = Color.parseColor("#4FC3F7")
                    canvas.drawRect(0f, 0f, virtualWidth, worldOffsetY + 2f, fillPaint)
                }
                canvas.save()
                canvas.translate(0f, worldOffsetY)
                drawCanalsBackground(canvas, engine.cameraX, engine.worldTime)
                canvas.restore()
            }
        }

        // World space drawing
        canvas.save()
        canvas.translate(-engine.cameraX + shakeX, worldOffsetY + shakeY)

        if (engine.level.theme == LevelTheme.CANALS) {
            drawCanalWater(canvas, engine.cameraX, engine.worldTime)
        }

        // Platforms
        for (platform in engine.level.platforms) {
            if (platform.x + platform.width < engine.cameraX - 140f || platform.x > engine.cameraX + virtualWidth + 140f) {
                continue
            }
            drawPlatform(canvas, platform, engine.worldTime, engine.level.theme)
        }

        // Coolblue ?-Boxes
        for (box in engine.level.boxes) {
            if (box.x + box.size < engine.cameraX - 80f || box.x > engine.cameraX + virtualWidth + 80f) {
                continue
            }
            drawQuestionBox(canvas, box, engine.worldTime)
        }

        // Smile Coins
        for (coin in engine.level.coins) {
            if (coin.collected) continue
            if (coin.x < engine.cameraX - 50f || coin.x > engine.cameraX + virtualWidth + 50f) continue
            drawSmileCoin(
                canvas,
                coin.x,
                coin.y + sin(engine.worldTime * 4.2f + coin.bobPhase) * 6f,
                coin.radius,
                engine.worldTime + coin.bobPhase
            )
        }

        // Power-Up Items (Coolblue Bakfiets)
        for (item in engine.powerUps) {
            drawBakfietsPowerUp(canvas, item.x, item.y, item.width, item.height, engine.worldTime)
        }

        // Finish Area: Flagpole + Van / Customer House + BOTH Cheering Teammates!
        drawFinishArea(canvas, engine)

        // Enemies
        for (enemy in engine.level.enemies) {
            if (!enemy.alive && enemy.squashedTimer <= 0f) continue
            if (enemy.x + enemy.width < engine.cameraX - 80f || enemy.x > engine.cameraX + virtualWidth + 80f) continue
            drawEnemy(canvas, enemy, engine.level)
        }

        // Player Ground Shadow (helps judge jump landings across all screen sizes!)
        val p = engine.player
        drawProjectedShadow(canvas, p.x + p.width / 2f, p.y + p.height, 20f, engine.level)

        // Bakfiets Turbo Speed Afterimage Ghosts
        for (ghost in engine.trailGhosts) {
            val alpha = ((ghost.life / ghost.maxLife).coerceIn(0f, 1f) * 95).toInt()
            fillPaint.color = Color.argb(alpha, 0, 168, 243)
            canvas.drawRoundRect(
                RectF(ghost.x + 6f, ghost.y + 8f, ghost.x + p.width - 6f, ghost.y + p.height - 2f),
                14f, 14f, fillPaint
            )
        }

        // Player Hero (Max, Remi, or Vero)
        val blinkHide = p.invulnTimer > 0f && ((p.invulnTimer * 18f).toInt() % 2 == 0)
        if (!blinkHide) {
            drawHeroSprite(
                canvas = canvas,
                x = p.x,
                y = p.y,
                width = p.width,
                height = p.height,
                character = engine.selectedCharacter,
                facingRight = p.facingRight,
                runningAnim = p.animTime,
                inAir = !p.onGround,
                hasShield = p.hasBakfietsShield || engine.demoModeEnabled,
                worldTime = engine.worldTime,
                showNameTag = true,
                squashX = p.squashX,
                stretchY = p.stretchY,
                isHurt = p.invulnTimer > 1.1f
            )
        }

        // Particles
        for (pt in engine.particles) {
            drawParticle(canvas, pt)
        }

        // Confetti
        for (c in engine.confetti) {
            canvas.save()
            canvas.translate(c.x, c.y)
            canvas.rotate(c.rotation)
            fillPaint.color = c.colorArgb
            canvas.drawRoundRect(RectF(-c.width / 2f, -c.height / 2f, c.width / 2f, c.height / 2f), 2f, 2f, fillPaint)
            canvas.restore()
        }

        // Floating Score Popups
        textPaint.textAlign = Paint.Align.CENTER
        for (f in engine.floatingTexts) {
            val alpha = ((f.life / f.maxLife).coerceIn(0f, 1f) * 255).toInt()
            textPaint.textSize = 24f * f.scale
            textPaint.color = Color.argb(alpha, 0, 30, 60)
            canvas.drawText(f.text, f.x + 2f, f.y + 2f, textPaint)
            textPaint.color = Color.argb(alpha, Color.red(f.colorArgb), Color.green(f.colorArgb), Color.blue(f.colorArgb))
            canvas.drawText(f.text, f.x, f.y, textPaint)
        }

        canvas.restore()
        canvas.restore()
    }

    private fun drawProjectedShadow(
        canvas: Canvas,
        cx: Float,
        feetY: Float,
        baseRadiusX: Float,
        level: LevelData
    ) {
        var surfaceY: Float? = null
        for (plat in level.platforms) {
            if (cx >= plat.x + 4f && cx <= plat.x + plat.width - 4f && plat.y >= feetY - 6f) {
                if (surfaceY == null || plat.y < surfaceY) {
                    surfaceY = plat.y
                }
            }
        }
        for (box in level.boxes) {
            if (cx >= box.x + 2f && cx <= box.x + box.size - 2f && box.y >= feetY - 6f) {
                if (surfaceY == null || box.y < surfaceY) {
                    surfaceY = box.y
                }
            }
        }
        val sy = surfaceY ?: return
        val dist = (sy - feetY).coerceAtLeast(0f)
        if (dist > 280f) return
        val factor = (1f - dist / 280f).coerceIn(0.25f, 1f)
        fillPaint.color = Color.argb((85 * factor).toInt(), 0, 22, 48)
        canvas.drawOval(
            RectF(cx - baseRadiusX * factor, sy - 4f, cx + baseRadiusX * factor, sy + 5f),
            fillPaint
        )
    }

    private fun drawParticle(canvas: Canvas, pt: Particle) {
        val ratio = (pt.life / pt.maxLife).coerceIn(0f, 1f)
        val alpha = (ratio * 255).toInt()
        val r = Color.red(pt.colorArgb)
        val g = Color.green(pt.colorArgb)
        val b = Color.blue(pt.colorArgb)

        when (pt.shape) {
            ParticleShape.SMILE -> drawMiniSmileIcon(canvas, pt.x, pt.y, pt.size * 0.68f, alpha)
            ParticleShape.STAR -> {
                canvas.save()
                canvas.translate(pt.x, pt.y)
                canvas.rotate(pt.rotation)
                fillPaint.color = Color.argb(alpha, r, g, b)
                val rad = pt.size * 0.72f
                val inner = rad * 0.36f
                path.reset()
                for (i in 0 until 8) {
                    val ang = (i * Math.PI / 4.0).toFloat()
                    val d = if (i % 2 == 0) rad else inner
                    val px = cos(ang) * d
                    val py = sin(ang) * d
                    if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
                }
                path.close()
                canvas.drawPath(path, fillPaint)
                canvas.restore()
            }
            ParticleShape.DUST -> {
                fillPaint.color = Color.argb((alpha * 0.75f).toInt(), r, g, b)
                canvas.drawCircle(pt.x, pt.y, pt.size * (0.5f + (1f - ratio) * 0.4f), fillPaint)
            }
            ParticleShape.SPLASH -> {
                fillPaint.color = Color.argb(alpha, r, g, b)
                canvas.drawOval(RectF(pt.x - pt.size * 0.4f, pt.y - pt.size * 0.65f, pt.x + pt.size * 0.4f, pt.y + pt.size * 0.65f), fillPaint)
            }
            else -> {
                fillPaint.color = Color.argb(alpha, r, g, b)
                canvas.drawCircle(pt.x, pt.y, pt.size * 0.5f, fillPaint)
            }
        }
    }

    private fun drawWarehouseBackground(canvas: Canvas, cameraX: Float, time: Float) {
        fillPaint.shader = LinearGradient(
            0f, 0f, 0f, GameConfig.VIEW_HEIGHT,
            Color.parseColor("#C4E8FF"), Color.parseColor("#EFF9FF"),
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, virtualWidth, GameConfig.VIEW_HEIGHT, fillPaint)
        fillPaint.shader = null

        // Distant automated upper mezzanine conveyor belt (parallax 0.16)
        fillPaint.color = Color.parseColor("#A7D2F0")
        canvas.drawRect(0f, 252f, virtualWidth, 262f, fillPaint)
        val mezStep = 145f
        val mezShift = -((cameraX * 0.16f - time * 55f) % mezStep + mezStep) % mezStep
        var mx = mezShift - mezStep
        var mIdx = 0
        while (mx < virtualWidth + mezStep) {
            val bw = if (mIdx % 2 == 0) 36f else 28f
            val bh = if (mIdx % 3 == 0) 28f else 22f
            fillPaint.color = Color.argb(135, 0, 144, 227)
            canvas.drawRoundRect(RectF(mx, 252f - bh, mx + bw, 252f), 4f, 4f, fillPaint)
            fillPaint.color = Color.argb(145, 255, 102, 0)
            canvas.drawRect(mx + 6f, 252f - 8f, mx + bw - 6f, 252f - 5f, fillPaint)
            mx += mezStep
            mIdx++
        }

        // Ceiling steel beam + diagonal trusses + hanging warehouse lamps
        fillPaint.color = Color.parseColor("#7EB8E2")
        canvas.drawRect(0f, 0f, virtualWidth, 34f, fillPaint)
        strokePaint.color = Color.argb(80, 92, 143, 181)
        strokePaint.strokeWidth = 3f
        var trussX = -(cameraX * 0.2f) % 90f
        while (trussX < virtualWidth + 90f) {
            canvas.drawLine(trussX, 4f, trussX + 45f, 32f, strokePaint)
            canvas.drawLine(trussX + 45f, 32f, trussX + 90f, 4f, strokePaint)
            trussX += 90f
        }

        val lampSpacing = 320f
        val lampShift = -(cameraX * 0.25f) % lampSpacing
        var lx = lampShift - lampSpacing
        while (lx < virtualWidth + lampSpacing) {
            fillPaint.color = Color.parseColor("#5C8FB5")
            canvas.drawRect(lx - 3f, 34f, lx + 3f, 88f, fillPaint)
            fillPaint.color = coolblueNavy
            rect.set(lx - 36f, 84f, lx + 36f, 103f)
            canvas.drawRoundRect(rect, 8f, 8f, fillPaint)
            fillPaint.color = Color.parseColor("#FFF9C4")
            canvas.drawRoundRect(RectF(lx - 26f, 97f, lx + 26f, 103f), 3f, 3f, fillPaint)
            // Volumetric light cone
            fillPaint.color = Color.argb(42, 255, 244, 190)
            path.reset()
            path.moveTo(lx - 28f, 103f)
            path.lineTo(lx + 28f, 103f)
            path.lineTo(lx + 102f, 320f)
            path.lineTo(lx - 102f, 320f)
            path.close()
            canvas.drawPath(path, fillPaint)
            lx += lampSpacing
        }

        // Giant Coolblue Warehouse Wall Sign (parallax 0.22)
        val bannerCycle = 1180f
        val bannerShift = -(cameraX * 0.22f) % bannerCycle
        var bx = bannerShift + 130f
        while (bx < virtualWidth + 620f) {
            rect.set(bx, 126f, bx + 544f, 230f)
            fillPaint.color = Color.argb(230, 0, 144, 227)
            canvas.drawRoundRect(rect, 18f, 18f, fillPaint)
            strokePaint.color = Color.argb(150, 255, 255, 255)
            strokePaint.strokeWidth = 2.5f
            canvas.drawRoundRect(rect, 18f, 18f, strokePaint)
            textPaint.textAlign = Paint.Align.CENTER
            textPaint.color = white
            textPaint.textSize = 36f
            canvas.drawText("coolblue magazijn", rect.centerX() - 12f, rect.top + 52f, textPaint)
            fillPaint.color = coolblueOrange
            canvas.drawCircle(rect.right - 96f, rect.top + 42f, 10f, fillPaint)
            textPaint.color = Color.parseColor("#D8F1FF")
            textPaint.textSize = 16.5f
            canvas.drawText("Alles voor een glimlach • Vandaag besteld, morgen in huis!", rect.centerX(), rect.bottom - 18f, textPaint)
            bx += bannerCycle
        }

        // Background pallet shelving racks (parallax 0.38)
        val rackSpacing = 280f
        val rackShift = -(cameraX * 0.38f) % rackSpacing
        var rx = rackShift - rackSpacing
        var rackIdx = 0
        while (rx < virtualWidth + rackSpacing) {
            fillPaint.color = Color.parseColor("#8BB9DC")
            canvas.drawRect(rx, 286f, rx + 12f, GameConfig.GROUND_Y, fillPaint)
            canvas.drawRect(rx + 210f, 286f, rx + 222f, GameConfig.GROUND_Y, fillPaint)
            for (tier in 0..2) {
                val sy = 340f + tier * 88f
                fillPaint.color = Color.parseColor("#F58E3C")
                canvas.drawRect(rx, sy, rx + 222f, sy + 10f, fillPaint)
                for (boxCol in 0..2) {
                    if ((rackIdx + tier + boxCol) % 4 == 0) continue
                    val boxX = rx + 22f + boxCol * 60f
                    val boxH = if ((tier + boxCol) % 2 == 0) 46f else 38f
                    fillPaint.color = Color.argb(155, 0, 144, 227)
                    canvas.drawRoundRect(RectF(boxX, sy - boxH, boxX + 48f, sy), 5f, 5f, fillPaint)
                    fillPaint.color = Color.argb(165, 255, 255, 255)
                    canvas.drawRect(boxX + 19f, sy - boxH, boxX + 29f, sy - boxH + 9f, fillPaint)
                    fillPaint.color = Color.argb(155, 255, 102, 0)
                    canvas.drawRect(boxX + 12f, sy - 14f, boxX + 36f, sy - 9f, fillPaint)
                }
            }
            rx += rackSpacing
            rackIdx++
        }
    }

    private fun drawCanalsBackground(canvas: Canvas, cameraX: Float, time: Float) {
        fillPaint.shader = LinearGradient(
            0f, 0f, 0f, GameConfig.VIEW_HEIGHT,
            Color.parseColor("#38B6F6"), Color.parseColor("#E1F5FE"),
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, virtualWidth, GameConfig.VIEW_HEIGHT, fillPaint)
        fillPaint.shader = null

        // Warm Dutch sun with pulsing halo
        val sunX = virtualWidth - 155f
        val haloPulse = sin(time * 2.2f) * 6f
        fillPaint.color = Color.argb(45, 255, 230, 128)
        canvas.drawCircle(sunX, 114f, 84f + haloPulse, fillPaint)
        fillPaint.color = Color.argb(75, 255, 224, 102)
        canvas.drawCircle(sunX, 114f, 62f, fillPaint)
        fillPaint.color = Color.parseColor("#FFD54F")
        canvas.drawCircle(sunX, 114f, 42f, fillPaint)

        // Gentle flying birds in distance
        strokePaint.color = Color.argb(120, 0, 58, 108)
        strokePaint.strokeWidth = 2.2f
        for (b in 0..3) {
            val bx = ((b * 310f - cameraX * 0.08f + time * 24f) % (virtualWidth + 120f) + (virtualWidth + 120f)) % (virtualWidth + 120f) - 60f
            val by = 82f + (b % 2) * 32f + sin(time * 4f + b) * 5f
            val wing = sin(time * 7f + b * 1.3f) * 5f
            canvas.drawLine(bx - 8f, by - wing, bx, by, strokePaint)
            canvas.drawLine(bx, by, bx + 8f, by - wing, strokePaint)
        }

        // Fluffy clouds (slow parallax)
        val cloudSpan = 410f
        val cloudShift = -((cameraX * 0.14f + time * 12f) % cloudSpan)
        var cx = cloudShift
        var ci = 0
        while (cx < virtualWidth + 300f) {
            val cy = 104f + (ci % 3) * 42f
            fillPaint.color = Color.argb(230, 255, 255, 255)
            canvas.drawCircle(cx, cy, 30f, fillPaint)
            canvas.drawCircle(cx + 28f, cy - 12f, 36f, fillPaint)
            canvas.drawCircle(cx + 62f, cy, 28f, fillPaint)
            cx += cloudSpan
            ci++
        }

        // Classic Dutch Windmill (Molen) in midground parallax
        val millCycle = 1280f
        val millX = 680f - ((cameraX * 0.22f) % millCycle)
        for (offset in listOf(-millCycle, 0f, millCycle)) {
            val mx = millX + offset
            if (mx > -180f && mx < virtualWidth + 180f) {
                drawDutchWindmill(canvas, mx, 505f, time)
            }
        }

        // Iconic Dutch Canal Houses (Grachtenpanden with varied gables & tulips!)
        val housePalette = intArrayOf(
            Color.parseColor("#C85A3F"),
            Color.parseColor("#2F6D80"),
            Color.parseColor("#D9A13B"),
            Color.parseColor("#4B5F73"),
            Color.parseColor("#B8474B"),
            Color.parseColor("#3E7B67")
        )
        val houseW = 112f
        val startHouse = ((cameraX * 0.36f) / houseW).toInt() - 2
        val endHouse = startHouse + (virtualWidth / houseW).toInt() + 5
        for (i in startHouse..endHouse) {
            val hx = i * houseW - cameraX * 0.36f
            val h = 228f + ((abs(i * 37) % 4) * 34f)
            val topY = GameConfig.GROUND_Y - h
            val baseCol = housePalette[abs(i) % housePalette.size]
            fillPaint.color = baseCol
            canvas.drawRect(hx + 2f, topY, hx + houseW - 2f, GameConfig.GROUND_Y, fillPaint)

            // Varied Dutch gable styles: Trapgevel, Halsgevel, Klokgevel
            when (abs(i) % 3) {
                0 -> {
                    canvas.drawRect(hx + 16f, topY - 22f, hx + houseW - 16f, topY, fillPaint)
                    canvas.drawRect(hx + 32f, topY - 42f, hx + houseW - 32f, topY - 22f, fillPaint)
                    fillPaint.color = white
                    canvas.drawRect(hx + 14f, topY - 25f, hx + houseW - 14f, topY - 20f, fillPaint)
                    canvas.drawRect(hx + 30f, topY - 45f, hx + houseW - 30f, topY - 40f, fillPaint)
                }
                1 -> {
                    canvas.drawCircle(hx + houseW / 2f, topY - 12f, 34f, fillPaint)
                    strokePaint.color = white
                    strokePaint.strokeWidth = 3.5f
                    canvas.drawArc(RectF(hx + houseW / 2f - 34f, topY - 46f, hx + houseW / 2f + 34f, topY + 22f), 185f, 170f, false, strokePaint)
                }
                else -> {
                    canvas.drawRect(hx + 28f, topY - 38f, hx + houseW - 28f, topY, fillPaint)
                    fillPaint.color = white
                    canvas.drawRect(hx + 24f, topY - 43f, hx + houseW - 24f, topY - 37f, fillPaint)
                }
            }

            // Authentic Amsterdam hoisting beam (hijsbalk) at gable peak
            strokePaint.color = coolblueNavy
            strokePaint.strokeWidth = 3f
            canvas.drawLine(hx + houseW / 2f, topY - 32f, hx + houseW / 2f + 14f, topY - 32f, strokePaint)

            // Crisp Dutch windows with mullions & flower boxes
            for (row in 0..2) {
                val wy = topY + 28f + row * 60f
                if (wy + 36f < GameConfig.GROUND_Y - 14f) {
                    for (wx in listOf(hx + 20f, hx + 64f)) {
                        fillPaint.color = white
                        canvas.drawRect(wx - 2f, wy - 2f, wx + 28f, wy + 36f, fillPaint)
                        fillPaint.color = Color.parseColor("#2A4B63")
                        canvas.drawRect(wx + 1f, wy + 1f, wx + 25f, wy + 33f, fillPaint)
                        fillPaint.color = Color.argb(95, 255, 240, 170)
                        canvas.drawRect(wx + 1f, wy + 1f, wx + 12f, wy + 16f, fillPaint)
                        // Tulip flower box on middle row
                        if (row == 1) {
                            fillPaint.color = Color.parseColor("#6D4C41")
                            canvas.drawRect(wx - 1f, wy + 32f, wx + 27f, wy + 38f, fillPaint)
                            fillPaint.color = coolblueOrange
                            canvas.drawCircle(wx + 6f, wy + 30f, 3.2f, fillPaint)
                            fillPaint.color = smileGold
                            canvas.drawCircle(wx + 14f, wy + 29f, 3.2f, fillPaint)
                            fillPaint.color = Color.parseColor("#FF5252")
                            canvas.drawCircle(wx + 21f, wy + 30f, 3.2f, fillPaint)
                        }
                    }
                }
            }

            // Coolblue storefront awning every few houses
            if (abs(i) % 5 == 2) {
                fillPaint.color = coolblueBlue
                canvas.drawRoundRect(RectF(hx + 10f, GameConfig.GROUND_Y - 48f, hx + houseW - 10f, GameConfig.GROUND_Y - 26f), 6f, 6f, fillPaint)
                fillPaint.color = coolblueOrange
                canvas.drawRect(hx + 10f, GameConfig.GROUND_Y - 26f, hx + houseW - 10f, GameConfig.GROUND_Y - 20f, fillPaint)
            }
        }
    }

    private fun drawDutchWindmill(canvas: Canvas, cx: Float, baseY: Float, time: Float) {
        fillPaint.color = Color.parseColor("#7A5C43")
        path.reset()
        path.moveTo(cx - 38f, baseY)
        path.lineTo(cx + 38f, baseY)
        path.lineTo(cx + 22f, baseY - 138f)
        path.lineTo(cx - 22f, baseY - 138f)
        path.close()
        canvas.drawPath(path, fillPaint)

        // Wooden balcony ring
        fillPaint.color = Color.parseColor("#5D4037")
        canvas.drawRect(cx - 36f, baseY - 68f, cx + 36f, baseY - 60f, fillPaint)

        // Cap
        fillPaint.color = coolblueNavy
        canvas.drawArc(RectF(cx - 25f, baseY - 165f, cx + 25f, baseY - 115f), 180f, 180f, true, fillPaint)

        // Spinning lattice sails
        canvas.save()
        canvas.translate(cx, baseY - 132f)
        canvas.rotate(time * 28f)
        for (b in 0..3) {
            canvas.rotate(90f)
            fillPaint.color = Color.parseColor("#5A3E28")
            canvas.drawRect(-3f, 0f, 3f, 94f, fillPaint)
            fillPaint.color = Color.argb(220, 255, 255, 255)
            canvas.drawRect(3f, 16f, 22f, 88f, fillPaint)
            strokePaint.color = Color.argb(120, 0, 144, 227)
            strokePaint.strokeWidth = 1.6f
            canvas.drawLine(3f, 36f, 22f, 36f, strokePaint)
            canvas.drawLine(3f, 58f, 22f, 58f, strokePaint)
        }
        canvas.restore()
    }

    private fun drawCanalWater(canvas: Canvas, cameraX: Float, time: Float) {
        val left = cameraX - 60f
        val right = cameraX + virtualWidth + 60f
        fillPaint.shader = LinearGradient(
            0f, GameConfig.GROUND_Y + 22f, 0f, GameConfig.VIEW_HEIGHT + 40f,
            Color.parseColor("#0288D1"), Color.parseColor("#01579B"),
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(left, GameConfig.GROUND_Y + 22f, right, GameConfig.VIEW_HEIGHT + 40f, fillPaint)
        fillPaint.shader = null

        // Shimmering wave crests
        val waveStep = 60f
        val start = ((left / waveStep).toInt() - 1) * waveStep
        var wx = start
        while (wx < right) {
            val wy = GameConfig.GROUND_Y + 34f + sin(time * 3.5f + wx * 0.04f) * 4f
            fillPaint.color = Color.parseColor("#4FC3F7")
            canvas.drawRoundRect(RectF(wx, wy, wx + 36f, wy + 5.5f), 3f, 3f, fillPaint)
            fillPaint.color = Color.argb(120, 255, 255, 255)
            canvas.drawRoundRect(RectF(wx + 14f, wy + 18f, wx + 38f, wy + 22f), 2f, 2f, fillPaint)
            wx += waveStep
        }

        // Cute bobbing yellow canal duck in first canal gap!
        val duckX = 1120f + sin(time * 1.2f) * 35f
        val duckY = GameConfig.GROUND_Y + 22f + sin(time * 4.2f) * 3.5f
        if (duckX > left && duckX < right) {
            fillPaint.color = smileGold
            canvas.drawOval(RectF(duckX - 12f, duckY - 8f, duckX + 12f, duckY + 6f), fillPaint)
            canvas.drawCircle(duckX + 8f, duckY - 11f, 7f, fillPaint)
            fillPaint.color = coolblueOrange
            canvas.drawOval(RectF(duckX + 13f, duckY - 12f, duckX + 20f, duckY - 8f), fillPaint)
            fillPaint.color = coolblueBlue
            canvas.drawArc(RectF(duckX + 2f, duckY - 19f, duckX + 14f, duckY - 11f), 180f, 180f, true, fillPaint)
        }
    }

    private fun drawPlatform(canvas: Canvas, p: Platform, time: Float, theme: LevelTheme) {
        when (p.type) {
            PlatformType.GROUND -> {
                fillPaint.color = if (theme == LevelTheme.CANALS) Color.parseColor("#455A64") else Color.parseColor("#37474F")
                canvas.drawRect(p.x, p.y, p.x + p.width, p.y + p.height, fillPaint)
                fillPaint.color = coolblueBlue
                canvas.drawRect(p.x, p.y, p.x + p.width, p.y + 18f, fillPaint)
                fillPaint.color = Color.argb(90, 255, 255, 255)
                canvas.drawRect(p.x, p.y, p.x + p.width, p.y + 4f, fillPaint)
                fillPaint.color = coolblueOrange
                canvas.drawRect(p.x, p.y + 18f, p.x + p.width, p.y + 24f, fillPaint)

                // Safety edge stripes near pit drops in warehouse
                if (theme == LevelTheme.WAREHOUSE) {
                    fillPaint.color = smileGold
                    canvas.drawRect(p.x + p.width - 28f, p.y, p.x + p.width, p.y + 18f, fillPaint)
                    fillPaint.color = coolblueNavy
                    canvas.drawRect(p.x + p.width - 20f, p.y, p.x + p.width - 12f, p.y + 18f, fillPaint)
                }

                fillPaint.color = Color.parseColor("#546E7A")
                var tx = p.x + 14f
                while (tx < p.x + p.width - 14f) {
                    canvas.drawRoundRect(
                        RectF(tx, p.y + 38f, (tx + 54f).coerceAtMost(p.x + p.width - 10f), p.y + 52f),
                        4f, 4f, fillPaint
                    )
                    tx += 76f
                }
            }
            PlatformType.PALLET -> {
                fillPaint.color = Color.parseColor("#DEB887")
                canvas.drawRoundRect(RectF(p.x, p.y, p.x + p.width, p.y + p.height), 6f, 6f, fillPaint)
                fillPaint.color = Color.parseColor("#A46D2A")
                canvas.drawRect(p.x + 8f, p.y + p.height - 9f, p.x + p.width - 8f, p.y + p.height, fillPaint)
                // Pallet spacer blocks
                fillPaint.color = Color.parseColor("#7D4E16")
                canvas.drawRect(p.x + 10f, p.y + 9f, p.x + 28f, p.y + p.height - 4f, fillPaint)
                canvas.drawRect(p.x + p.width / 2f - 9f, p.y + 9f, p.x + p.width / 2f + 9f, p.y + p.height - 4f, fillPaint)
                canvas.drawRect(p.x + p.width - 28f, p.y + 9f, p.x + p.width - 10f, p.y + p.height - 4f, fillPaint)
                strokePaint.color = Color.parseColor("#6D4312")
                strokePaint.strokeWidth = 2.5f
                canvas.drawRoundRect(RectF(p.x, p.y, p.x + p.width, p.y + p.height), 6f, 6f, strokePaint)
            }
            PlatformType.PARCEL_STACK -> {
                val boxH = 56f
                var y = p.y
                while (y < p.y + p.height - 2f) {
                    val h = min(boxH, p.y + p.height - y)
                    rect.set(p.x + 1f, y + 1f, p.x + p.width - 1f, y + h - 1f)
                    fillPaint.color = coolblueBlue
                    canvas.drawRoundRect(rect, 6f, 6f, fillPaint)
                    // Top bevel highlight
                    fillPaint.color = Color.argb(65, 255, 255, 255)
                    canvas.drawRoundRect(RectF(rect.left + 2f, rect.top + 2f, rect.right - 2f, rect.top + 8f), 4f, 4f, fillPaint)
                    strokePaint.color = coolblueDark
                    strokePaint.strokeWidth = 2.5f
                    canvas.drawRoundRect(rect, 6f, 6f, strokePaint)
                    fillPaint.color = Color.argb(225, 255, 255, 255)
                    canvas.drawRect(rect.centerX() - 10f, rect.top + 2f, rect.centerX() + 10f, rect.top + 12f, fillPaint)
                    if (h >= 40f) {
                        strokePaint.color = coolblueOrange
                        strokePaint.strokeWidth = 3.5f
                        canvas.drawArc(
                            RectF(rect.centerX() - 15f, rect.centerY() - 6f, rect.centerX() + 15f, rect.centerY() + 16f),
                            18f, 144f, false, strokePaint
                        )
                    }
                    y += boxH
                }
            }
            PlatformType.CONVEYOR_RIGHT, PlatformType.CONVEYOR_LEFT -> {
                fillPaint.color = Color.parseColor("#263238")
                canvas.drawRoundRect(RectF(p.x, p.y, p.x + p.width, p.y + p.height), 10f, 10f, fillPaint)
                val dir = if (p.type == PlatformType.CONVEYOR_RIGHT) 1f else -1f
                val step = 28f
                val animShift = ((time * 135f * dir) % step + step) % step
                fillPaint.color = coolblueOrange
                var sx = p.x + 14f + animShift
                while (sx < p.x + p.width - 14f) {
                    path.reset()
                    val cy = p.y + p.height / 2f
                    path.moveTo(sx - 5f * dir, cy - 6f)
                    path.lineTo(sx + 5f * dir, cy)
                    path.lineTo(sx - 5f * dir, cy + 6f)
                    path.lineTo(sx - 1f * dir, cy)
                    path.close()
                    canvas.drawPath(path, fillPaint)
                    sx += step
                }
                // Rotating end rollers
                for (rx in listOf(p.x + 10f, p.x + p.width - 10f)) {
                    fillPaint.color = Color.parseColor("#CFD8DC")
                    canvas.drawCircle(rx, p.y + p.height / 2f, 6.5f, fillPaint)
                    strokePaint.color = coolblueNavy
                    strokePaint.strokeWidth = 2f
                    val ang = time * 8f * dir
                    canvas.drawLine(rx - cos(ang) * 4f, p.y + p.height / 2f - sin(ang) * 4f, rx + cos(ang) * 4f, p.y + p.height / 2f + sin(ang) * 4f, strokePaint)
                }
                strokePaint.color = Color.parseColor("#90A4AE")
                strokePaint.strokeWidth = 2.5f
                canvas.drawRoundRect(RectF(p.x, p.y, p.x + p.width, p.y + p.height), 10f, 10f, strokePaint)
            }
            PlatformType.BRICK_BRIDGE -> {
                fillPaint.color = Color.parseColor("#A64B38")
                canvas.drawRoundRect(RectF(p.x, p.y, p.x + p.width, p.y + p.height), 8f, 8f, fillPaint)
                // Masonry arch shadow underneath
                fillPaint.color = Color.parseColor("#6D2C1E")
                canvas.drawArc(
                    RectF(p.x + p.width * 0.18f, p.y + 10f, p.x + p.width * 0.82f, p.y + p.height + 24f),
                    180f, 180f, true, fillPaint
                )
                fillPaint.color = Color.parseColor("#E6D5B8")
                canvas.drawRect(p.x, p.y, p.x + p.width, p.y + 8f, fillPaint)
                // Wrought-iron canal railing
                strokePaint.color = Color.parseColor("#263238")
                strokePaint.strokeWidth = 2.2f
                canvas.drawLine(p.x + 8f, p.y - 12f, p.x + p.width - 8f, p.y - 12f, strokePaint)
                var postX = p.x + 8f
                while (postX <= p.x + p.width - 8f) {
                    canvas.drawLine(postX, p.y - 13f, postX, p.y, strokePaint)
                    postX += 38f
                }
            }
            PlatformType.ROOFTOP -> {
                fillPaint.color = Color.parseColor("#D85A2A")
                canvas.drawRoundRect(RectF(p.x, p.y, p.x + p.width, p.y + p.height), 8f, 8f, fillPaint)
                fillPaint.color = Color.parseColor("#FF8A50")
                var rx = p.x + 8f
                while (rx < p.x + p.width - 14f) {
                    canvas.drawArc(RectF(rx, p.y + 2f, rx + 22f, p.y + 16f), 180f, 180f, true, fillPaint)
                    rx += 24f
                }
                strokePaint.color = Color.parseColor("#8C2D12")
                strokePaint.strokeWidth = 2.5f
                canvas.drawRoundRect(RectF(p.x, p.y, p.x + p.width, p.y + p.height), 8f, 8f, strokePaint)
            }
            PlatformType.SPRING_PAD -> {
                val pop = if (p.springAnimTimer > 0f) sin(p.springAnimTimer * 28f) * 11f else 0f
                fillPaint.color = coolblueNavy
                canvas.drawRoundRect(RectF(p.x + 4f, p.y + p.height - 7f, p.x + p.width - 4f, p.y + p.height), 3f, 3f, fillPaint)
                strokePaint.color = Color.parseColor("#CFD8DC")
                strokePaint.strokeWidth = 4f
                canvas.drawLine(p.x + 18f, p.y + p.height - 6f, p.x + p.width - 18f, p.y + 8f - pop, strokePaint)
                canvas.drawLine(p.x + p.width - 18f, p.y + p.height - 6f, p.x + 18f, p.y + 8f - pop, strokePaint)
                fillPaint.color = coolblueOrange
                canvas.drawRoundRect(RectF(p.x, p.y - pop, p.x + p.width, p.y + 12f - pop), 6f, 6f, fillPaint)
                fillPaint.color = white
                canvas.drawRect(p.x + 14f, p.y + 3f - pop, p.x + p.width - 14f, p.y + 7f - pop, fillPaint)
            }
        }
    }

    private fun drawQuestionBox(canvas: Canvas, box: QuestionBox, time: Float) {
        val bob = if (!box.isHit) sin(time * 4.5f + box.id) * 2.5f else 0f
        val y = box.y - box.bumpOffset + bob
        rect.set(box.x, y, box.x + box.size, y + box.size)

        // Drop shadow
        fillPaint.color = Color.argb(45, 0, 22, 48)
        canvas.drawRoundRect(RectF(rect.left + 3f, rect.top + 5f, rect.right + 3f, rect.bottom + 5f), 10f, 10f, fillPaint)

        if (!box.isHit) {
            val pulse = (sin(time * 5.5f + box.id) * 0.5f + 0.5f)
            fillPaint.color = if (box.reward == BoxReward.BAKFIETS_SHIELD) coolblueOrange else coolblueBlue
            canvas.drawRoundRect(rect, 10f, 10f, fillPaint)

            // 3D top bevel highlight
            fillPaint.color = Color.argb(65, 255, 255, 255)
            canvas.drawRoundRect(RectF(rect.left + 4f, rect.top + 4f, rect.right - 4f, rect.top + 14f), 6f, 6f, fillPaint)

            strokePaint.color = white
            strokePaint.strokeWidth = 3f + pulse * 1.4f
            canvas.drawRoundRect(rect, 10f, 10f, strokePaint)

            fillPaint.color = Color.argb(195, 255, 255, 255)
            canvas.drawCircle(rect.left + 8f, rect.top + 8f, 2.5f, fillPaint)
            canvas.drawCircle(rect.right - 8f, rect.top + 8f, 2.5f, fillPaint)
            canvas.drawCircle(rect.left + 8f, rect.bottom - 8f, 2.5f, fillPaint)
            canvas.drawCircle(rect.right - 8f, rect.bottom - 8f, 2.5f, fillPaint)

            textPaint.textAlign = Paint.Align.CENTER
            textPaint.color = Color.argb(80, 0, 20, 45)
            textPaint.textSize = 34f
            canvas.drawText("?", rect.centerX() + 1.5f, rect.centerY() + 13.5f, textPaint)
            textPaint.color = white
            canvas.drawText("?", rect.centerX(), rect.centerY() + 12f, textPaint)
        } else {
            fillPaint.color = Color.parseColor("#78909C")
            canvas.drawRoundRect(rect, 8f, 8f, fillPaint)
            strokePaint.color = Color.parseColor("#455A64")
            strokePaint.strokeWidth = 2.5f
            canvas.drawRoundRect(rect, 8f, 8f, strokePaint)
            strokePaint.color = coolblueOrange
            strokePaint.strokeWidth = 3f
            canvas.drawArc(
                RectF(rect.centerX() - 12f, rect.centerY() - 8f, rect.centerX() + 12f, rect.centerY() + 12f),
                20f, 140f, false, strokePaint
            )
        }
    }

    private fun drawSmileCoin(canvas: Canvas, cx: Float, cy: Float, r: Float, phase: Float) {
        val spinWidth = (abs(cos(phase * 2.6f)).coerceAtLeast(0.30f)) * r
        // Subtle 3D coin rim behind
        fillPaint.color = Color.parseColor("#E68900")
        canvas.drawOval(RectF(cx - spinWidth - 2f, cy - r, cx + spinWidth + 2f, cy + r), fillPaint)

        fillPaint.color = smileGold
        canvas.drawOval(RectF(cx - spinWidth, cy - r, cx + spinWidth, cy + r), fillPaint)
        strokePaint.color = coolblueOrange
        strokePaint.strokeWidth = 2.8f
        canvas.drawOval(RectF(cx - spinWidth, cy - r, cx + spinWidth, cy + r), strokePaint)

        // Specular shine spot
        fillPaint.color = Color.argb(150, 255, 255, 255)
        canvas.drawOval(RectF(cx - spinWidth * 0.55f, cy - r * 0.65f, cx - spinWidth * 0.12f, cy - r * 0.25f), fillPaint)

        if (spinWidth > r * 0.52f) {
            fillPaint.color = coolblueNavy
            canvas.drawCircle(cx - spinWidth * 0.34f, cy - 4f, 2.3f, fillPaint)
            canvas.drawCircle(cx + spinWidth * 0.34f, cy - 4f, 2.3f, fillPaint)
            strokePaint.color = coolblueNavy
            strokePaint.strokeWidth = 2.6f
            canvas.drawArc(
                RectF(cx - spinWidth * 0.52f, cy - 4f, cx + spinWidth * 0.52f, cy + 10f),
                15f, 150f, false, strokePaint
            )
        }
    }

    private fun drawMiniSmileIcon(canvas: Canvas, cx: Float, cy: Float, r: Float, alpha: Int) {
        fillPaint.color = Color.argb(alpha, 255, 196, 0)
        canvas.drawCircle(cx, cy, r, fillPaint)
        strokePaint.color = Color.argb(alpha, 0, 43, 84)
        strokePaint.strokeWidth = 2f
        canvas.drawArc(RectF(cx - r * 0.55f, cy - r * 0.3f, cx + r * 0.55f, cy + r * 0.55f), 20f, 140f, false, strokePaint)
    }

    private fun drawBakfietsPowerUp(canvas: Canvas, x: Float, y: Float, w: Float, h: Float, time: Float) {
        val glow = 5f + sin(time * 8f) * 3.5f
        fillPaint.color = Color.argb(85, 0, 144, 227)
        canvas.drawCircle(x + w / 2f, y + h / 2f, w * 0.68f + glow, fillPaint)

        fillPaint.color = coolblueNavy
        canvas.drawCircle(x + 11f, y + h - 8f, 8f, fillPaint)
        canvas.drawCircle(x + w - 11f, y + h - 8f, 8f, fillPaint)
        fillPaint.color = white
        canvas.drawCircle(x + 11f, y + h - 8f, 3.5f, fillPaint)
        canvas.drawCircle(x + w - 11f, y + h - 8f, 3.5f, fillPaint)

        fillPaint.color = coolblueBlue
        canvas.drawRoundRect(RectF(x + 20f, y + 6f, x + w - 4f, y + h - 10f), 5f, 5f, fillPaint)
        strokePaint.color = coolblueOrange
        strokePaint.strokeWidth = 3.5f
        canvas.drawLine(x + 11f, y + h - 8f, x + 22f, y + 12f, strokePaint)
        canvas.drawLine(x + 15f, y + 8f, x + 24f, y + 8f, strokePaint)
        strokePaint.color = white
        strokePaint.strokeWidth = 2.4f
        canvas.drawArc(RectF(x + 25f, y + 12f, x + w - 9f, y + 24f), 20f, 140f, false, strokePaint)
    }

    private fun drawEnemy(canvas: Canvas, enemy: Enemy, level: LevelData) {
        if (!enemy.alive) {
            if (enemy.type == EnemyType.ONE_STAR_CLOUD) {
                // Transformed into a happy sunny 5★ cloud!
                val cx = enemy.x + enemy.width / 2f
                val cy = enemy.y + enemy.height / 2f
                fillPaint.color = Color.argb(235, 255, 255, 255)
                canvas.drawCircle(cx - 14f, cy + 2f, 14f, fillPaint)
                canvas.drawCircle(cx, cy - 5f, 18f, fillPaint)
                canvas.drawCircle(cx + 14f, cy + 2f, 14f, fillPaint)
                textPaint.textAlign = Paint.Align.CENTER
                textPaint.color = smileGold
                textPaint.textSize = 18f
                canvas.drawText("5★", cx, cy + 6f, textPaint)
            } else {
                drawMiniSmileIcon(canvas, enemy.x + enemy.width / 2f, enemy.y + enemy.height - 14f, 18f, 235)
            }
            return
        }

        when (enemy.type) {
            EnemyType.SLOW_SNAIL -> {
                drawProjectedShadow(canvas, enemy.x + enemy.width / 2f, enemy.y + enemy.height, 22f, level)
                val facingLeft = enemy.vx < 0f
                val cx = enemy.x + enemy.width / 2f
                val bottom = enemy.y + enemy.height
                val squish = sin(enemy.animTime * 7f) * 2f

                // Snail body
                fillPaint.color = Color.parseColor("#90A4AE")
                canvas.drawRoundRect(
                    RectF(enemy.x + 2f - squish, bottom - 15f, enemy.x + enemy.width - 2f + squish, bottom),
                    8f, 8f, fillPaint
                )

                // Red "Delayed Parcel" shell with spiral
                val shellCx = if (facingLeft) cx + 6f else cx - 6f
                fillPaint.color = Color.parseColor("#D32F2F")
                canvas.drawCircle(shellCx, bottom - 22f, 17f, fillPaint)
                strokePaint.color = Color.parseColor("#FFCDD2")
                strokePaint.strokeWidth = 2.8f
                canvas.drawCircle(shellCx, bottom - 22f, 9.5f, strokePaint)
                canvas.drawCircle(shellCx, bottom - 22f, 3.5f, strokePaint)

                // Eyestalks & grumpy brow
                val headX = if (facingLeft) enemy.x + 11f else enemy.x + enemy.width - 11f
                strokePaint.color = Color.parseColor("#78909C")
                strokePaint.strokeWidth = 2.5f
                canvas.drawLine(headX, bottom - 12f, headX, bottom - 22f, strokePaint)
                fillPaint.color = white
                canvas.drawCircle(headX, bottom - 22f, 6.2f, fillPaint)
                fillPaint.color = Color.BLACK
                canvas.drawCircle(headX + (if (facingLeft) -2f else 2f), bottom - 22f, 2.8f, fillPaint)
                strokePaint.color = Color.parseColor("#B71C1C")
                strokePaint.strokeWidth = 2.8f
                canvas.drawLine(headX - 6f, bottom - 30f, headX + 6f, bottom - 26f, strokePaint)
            }
            EnemyType.ONE_STAR_CLOUD -> {
                val cx = enemy.x + enemy.width / 2f
                val cy = enemy.y + enemy.height / 2f
                // Occasional tiny lightning flicker underneath
                if (sin(enemy.animTime * 9f) > 0.65f) {
                    strokePaint.color = smileGold
                    strokePaint.strokeWidth = 2.5f
                    canvas.drawLine(cx - 2f, cy + 18f, cx + 4f, cy + 26f, strokePaint)
                    canvas.drawLine(cx + 4f, cy + 26f, cx - 3f, cy + 33f, strokePaint)
                }
                fillPaint.color = Color.parseColor("#37474F")
                canvas.drawCircle(cx - 16f, cy + 4f, 16f, fillPaint)
                canvas.drawCircle(cx, cy - 6f, 21f, fillPaint)
                canvas.drawCircle(cx + 16f, cy + 4f, 16f, fillPaint)
                canvas.drawRect(cx - 18f, cy + 2f, cx + 18f, cy + 20f, fillPaint)

                textPaint.textAlign = Paint.Align.CENTER
                textPaint.color = Color.parseColor("#FF5252")
                textPaint.textSize = 18f
                canvas.drawText("1★", cx, cy + 9f, textPaint)
            }
        }
    }

    private fun drawFinishArea(canvas: Canvas, engine: GameEngine) {
        val fx = engine.level.flagX
        val gy = engine.level.flagGroundY

        // Coolblue Flagpole
        fillPaint.color = Color.parseColor("#CFD8DC")
        canvas.drawRect(fx - 5f, gy - 310f, fx + 5f, gy, fillPaint)
        fillPaint.color = white
        canvas.drawRect(fx - 2f, gy - 310f, fx + 1f, gy, fillPaint)
        fillPaint.color = coolblueOrange
        canvas.drawCircle(fx, gy - 316f, 14f, fillPaint)
        fillPaint.color = coolblueNavy
        canvas.drawRoundRect(RectF(fx - 24f, gy - 26f, fx + 24f, gy), 5f, 5f, fillPaint)

        // Coolblue Flag
        val flagY = gy - 295f + engine.flagSlideProgress * 190f
        fillPaint.color = coolblueBlue
        path.reset()
        path.moveTo(fx + 5f, flagY)
        path.lineTo(fx + 108f, flagY + 26f)
        path.lineTo(fx + 5f, flagY + 54f)
        path.close()
        canvas.drawPath(path, fillPaint)
        strokePaint.color = coolblueOrange
        strokePaint.strokeWidth = 4f
        canvas.drawArc(RectF(fx + 20f, flagY + 14f, fx + 62f, flagY + 40f), 15f, 145f, false, strokePaint)

        // Destination landmark (shifted slightly right to give room for both cheering teammates!)
        if (engine.level.theme == LevelTheme.WAREHOUSE) {
            drawCoolblueVan(canvas, fx + 195f, gy)
        } else {
            drawDutchCustomerHouse(canvas, fx + 200f, gy)
        }

        // BOTH Teammates cheering at the finish line!
        val mates = engine.selectedCharacter.teammates()
        var bubbleAnchorY = gy - 80f
        mates.forEachIndexed { idx, mate ->
            val cheerBounce = abs(sin(engine.worldTime * 6.4f + idx * 1.35f)) * 16f
            val mateX = fx + 68f + idx * 58f
            val mateY = gy - 62f - cheerBounce
            if (idx == 0) bubbleAnchorY = mateY
            drawProjectedShadow(canvas, mateX + 22f, mateY + 62f, 18f, engine.level)
            drawHeroSprite(
                canvas = canvas,
                x = mateX,
                y = mateY,
                width = 44f,
                height = 62f,
                character = mate,
                facingRight = false,
                runningAnim = engine.worldTime * 4.2f + idx,
                inAir = cheerBounce > 6f,
                hasShield = false,
                worldTime = engine.worldTime,
                showNameTag = true,
                squashX = 1f,
                stretchY = 1f,
                isHurt = false
            )
        }

        // Shared celebratory speech bubble cheering the active hero by name!
        val bubbleText = engine.selectedCharacter.cheerForHero
        val bubbleCenterX = fx + 118f
        rect.set(bubbleCenterX - 118f, bubbleAnchorY - 82f, bubbleCenterX + 118f, bubbleAnchorY - 40f)
        fillPaint.color = white
        canvas.drawRoundRect(rect, 14f, 14f, fillPaint)
        strokePaint.color = coolblueOrange
        strokePaint.strokeWidth = 3f
        canvas.drawRoundRect(rect, 14f, 14f, strokePaint)
        textPaint.textAlign = Paint.Align.CENTER
        textPaint.color = coolblueNavy
        textPaint.textSize = 17.5f
        canvas.drawText(bubbleText, rect.centerX(), rect.centerY() + 6f, textPaint)
    }

    private fun drawCoolblueVan(canvas: Canvas, x: Float, groundY: Float) {
        fillPaint.color = Color.argb(65, 0, 22, 48)
        canvas.drawOval(RectF(x + 12f, groundY - 8f, x + 218f, groundY + 6f), fillPaint)

        fillPaint.color = coolblueBlue
        canvas.drawRoundRect(RectF(x, groundY - 122f, x + 224f, groundY - 22f), 16f, 16f, fillPaint)
        // Orange lower trim stripe
        fillPaint.color = coolblueOrange
        canvas.drawRect(x + 8f, groundY - 34f, x + 216f, groundY - 26f, fillPaint)

        // Cab windshield
        fillPaint.color = Color.parseColor("#B3E5FC")
        canvas.drawRoundRect(RectF(x + 156f, groundY - 110f, x + 212f, groundY - 64f), 8f, 8f, fillPaint)
        // Headlight
        fillPaint.color = Color.parseColor("#FFF59D")
        canvas.drawRoundRect(RectF(x + 214f, groundY - 52f, x + 225f, groundY - 38f), 4f, 4f, fillPaint)

        // Coolblue branding + big orange smile arc
        textPaint.textAlign = Paint.Align.CENTER
        textPaint.color = white
        textPaint.textSize = 26f
        canvas.drawText("coolblue", x + 78f, groundY - 72f, textPaint)
        fillPaint.color = coolblueOrange
        canvas.drawCircle(x + 138f, groundY - 80f, 6f, fillPaint)
        strokePaint.color = white
        strokePaint.strokeWidth = 3f
        canvas.drawArc(RectF(x + 48f, groundY - 68f, x + 108f, groundY - 46f), 18f, 144f, false, strokePaint)

        // Wheels
        fillPaint.color = coolblueNavy
        canvas.drawCircle(x + 52f, groundY - 20f, 20f, fillPaint)
        canvas.drawCircle(x + 174f, groundY - 20f, 20f, fillPaint)
        fillPaint.color = Color.LTGRAY
        canvas.drawCircle(x + 52f, groundY - 20f, 9f, fillPaint)
        canvas.drawCircle(x + 174f, groundY - 20f, 9f, fillPaint)
    }

    private fun drawDutchCustomerHouse(canvas: Canvas, x: Float, groundY: Float) {
        fillPaint.color = Color.parseColor("#C45B3E")
        canvas.drawRect(x, groundY - 205f, x + 210f, groundY, fillPaint)
        canvas.drawRect(x + 25f, groundY - 235f, x + 185f, groundY - 205f, fillPaint)
        canvas.drawRect(x + 55f, groundY - 262f, x + 155f, groundY - 235f, fillPaint)
        fillPaint.color = white
        canvas.drawRect(x + 22f, groundY - 238f, x + 188f, groundY - 233f, fillPaint)
        canvas.drawRect(x + 52f, groundY - 265f, x + 158f, groundY - 260f, fillPaint)

        // Glowing warm windows
        fillPaint.color = Color.parseColor("#FFE082")
        canvas.drawRoundRect(RectF(x + 26f, groundY - 175f, x + 84f, groundY - 122f), 6f, 6f, fillPaint)
        canvas.drawRoundRect(RectF(x + 126f, groundY - 175f, x + 184f, groundY - 122f), 6f, 6f, fillPaint)

        // Welcoming Coolblue-orange front door
        fillPaint.color = coolblueOrange
        canvas.drawRoundRect(RectF(x + 74f, groundY - 94f, x + 136f, groundY), 8f, 8f, fillPaint)
        fillPaint.color = smileGold
        canvas.drawCircle(x + 124f, groundY - 46f, 4.5f, fillPaint)

        rect.set(x + 52f, groundY - 118f, x + 158f, groundY - 94f)
        fillPaint.color = coolblueBlue
        canvas.drawRoundRect(rect, 8f, 8f, fillPaint)
        textPaint.textAlign = Paint.Align.CENTER
        textPaint.color = white
        textPaint.textSize = 15.5f
        canvas.drawText("BLIJE KLANT", rect.centerX(), rect.centerY() + 5.5f, textPaint)
    }

    // =========================================================================
    // HERO SPRITES: MAX, REMI & VERO
    // =========================================================================

    private fun drawHeroSprite(
        canvas: Canvas,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        character: CharacterType,
        facingRight: Boolean,
        runningAnim: Float,
        inAir: Boolean,
        hasShield: Boolean,
        worldTime: Float,
        showNameTag: Boolean,
        squashX: Float = 1f,
        stretchY: Float = 1f,
        isHurt: Boolean = false
    ) {
        val cx = x + width / 2f
        val cy = y + height / 2f

        // Bakfiets Turbo Shield aura + orbiting stars!
        if (hasShield) {
            val pulse = 4f + sin(worldTime * 10f) * 3f
            fillPaint.color = Color.argb(55, 0, 144, 227)
            canvas.drawCircle(cx, cy, 42f + pulse, fillPaint)
            strokePaint.color = coolblueOrange
            strokePaint.strokeWidth = 3f
            canvas.drawCircle(cx, cy, 38f + pulse * 0.5f, strokePaint)
            fillPaint.color = smileGold
            for (s in 0..2) {
                val ang = worldTime * 5.2f + s * (Math.PI * 2.0 / 3.0).toFloat()
                canvas.drawCircle(cx + cos(ang) * 39f, cy + sin(ang) * 39f, 4f, fillPaint)
            }
        }

        canvas.save()
        // Anchor squash & stretch cleanly at the hero's feet!
        canvas.translate(cx, y + height)
        canvas.scale(if (facingRight) squashX else -squashX, stretchY)
        canvas.translate(0f, -height)

        val stride = if (inAir) 9.5f else sin(runningAnim * 2.4f) * 10.5f

        // VERO's bouncy chestnut-brown ponytail (drawn behind shoulder/head so it trails naturally!)
        if (character == CharacterType.VERO) {
            val ponyBounce = if (inAir) -6f else abs(cos(runningAnim * 2.4f)) * 4.5f
            canvas.save()
            canvas.translate(-11f, 7f + ponyBounce * 0.4f)
            canvas.rotate(if (inAir) -24f else -10f - stride * 0.9f)
            // Hair tie (Coolblue Orange!)
            fillPaint.color = coolblueOrange
            canvas.drawCircle(0f, 0f, 3.8f, fillPaint)
            // Flowing brown ponytail + warm highlight
            fillPaint.color = Color.parseColor("#5D3A1A")
            canvas.drawOval(RectF(-15f, -3f, 1f, 11f), fillPaint)
            canvas.drawCircle(-13f, 7f, 4.5f, fillPaint)
            fillPaint.color = Color.parseColor("#8B5A2B")
            canvas.drawOval(RectF(-12f, -1f, -2f, 5f), fillPaint)
            canvas.restore()
        }

        // Back arm swinging opposite to front arm
        fillPaint.color = coolblueDark
        val backArmY = if (inAir) 16f else 26f + stride * 0.35f
        canvas.drawRoundRect(RectF(-16f, 22f, -8f, backArmY + 7f), 4f, 4f, fillPaint)
        fillPaint.color = Color.parseColor("#F5C293")
        canvas.drawCircle(-13f, backArmY + 6f, 4.5f, fillPaint)

        // Legs & Coolblue sneakers with white soles
        fillPaint.color = coolblueNavy
        canvas.drawRoundRect(RectF(-12f + stride * 0.45f, 40f, -2f + stride * 0.45f, 56f), 4f, 4f, fillPaint)
        canvas.drawRoundRect(RectF(2f - stride * 0.45f, 40f, 12f - stride * 0.45f, 56f), 4f, 4f, fillPaint)

        for (legShift in listOf(-13f + stride * 0.45f, 1f - stride * 0.45f)) {
            fillPaint.color = coolblueOrange
            canvas.drawRoundRect(RectF(legShift, 53f, legShift + 15f, 61f), 4f, 4f, fillPaint)
            fillPaint.color = white
            canvas.drawRect(legShift, 59f, legShift + 15f, 62f, fillPaint)
        }

        // Torso: Coolblue Uniform Shirt
        fillPaint.color = coolblueBlue
        canvas.drawRoundRect(RectF(-15f, 20f, 15f, 44f), 8f, 8f, fillPaint)

        // Distinct Coolblue uniform accents per hero
        when (character) {
            CharacterType.MAX -> {
                fillPaint.color = coolblueOrange
                canvas.drawRect(-15f, 35f, 15f, 40f, fillPaint)
                // Mini ID badge on chest
                fillPaint.color = white
                canvas.drawRoundRect(RectF(-12f, 23f, -6f, 28f), 2f, 2f, fillPaint)
            }
            CharacterType.REMI -> {
                fillPaint.color = coolblueOrange
                canvas.drawRoundRect(RectF(-15f, 20f, 15f, 27.5f), 6f, 6f, fillPaint)
            }
            CharacterType.VERO -> {
                // Stylish teal & orange V-collar trim
                fillPaint.color = veroTeal
                canvas.drawRect(-15f, 36f, 15f, 41f, fillPaint)
                fillPaint.color = coolblueOrange
                path.reset()
                path.moveTo(-8f, 20f)
                path.lineTo(8f, 20f)
                path.lineTo(1f, 27f)
                path.close()
                canvas.drawPath(path, fillPaint)
            }
        }

        // Crisp white Coolblue smile on chest
        strokePaint.color = white
        strokePaint.strokeWidth = 2.5f
        canvas.drawArc(RectF(-6f, 25f, 9f, 36f), 20f, 140f, false, strokePaint)

        // Front sleeve & hand (heroic Mario fist-pump in air!)
        val armY = if (inAir) 9f else 28f - stride * 0.38f
        fillPaint.color = coolblueBlue
        canvas.drawRoundRect(RectF(7f, min(armY, 22f), 16f, 29f), 4f, 4f, fillPaint)
        fillPaint.color = Color.parseColor("#FFD1A4")
        canvas.drawCircle(14.5f, armY, 5.2f, fillPaint)

        // Head
        fillPaint.color = Color.parseColor("#FFD1A4")
        canvas.drawCircle(0f, 11f, 13f, fillPaint)
        // Warm rosy cheek
        fillPaint.color = Color.argb(65, 255, 105, 97)
        canvas.drawCircle(7.5f, 13.5f, 3.2f, fillPaint)

        // Expressive eyes (periodic blink + hurt/happy mouth!)
        val isBlinking = ((worldTime + character.ordinal * 1.17f) % 3.7f) < 0.11f
        if (isBlinking && !isHurt) {
            strokePaint.color = coolblueNavy
            strokePaint.strokeWidth = 2.2f
            canvas.drawLine(2.5f, 9.2f, 8f, 9.2f, strokePaint)
        } else {
            fillPaint.color = coolblueNavy
            canvas.drawCircle(5.2f, 9f, 2.6f, fillPaint)
            fillPaint.color = white
            canvas.drawCircle(6f, 8.1f, 0.9f, fillPaint)
        }

        if (isHurt) {
            fillPaint.color = Color.parseColor("#8D4E2A")
            canvas.drawCircle(5.5f, 15.5f, 2.5f, fillPaint)
        } else {
            strokePaint.color = Color.parseColor("#8D4E2A")
            strokePaint.strokeWidth = 2.1f
            canvas.drawArc(RectF(0f, 11f, 10.5f, 18.5f), 10f, 145f, false, strokePaint)
        }

        // Distinct Headgear & Hair for MAX, REMI & VERO!
        when (character) {
            CharacterType.MAX -> {
                // Neat brown hair peek + Iconic Coolblue Cap with Orange Visor
                fillPaint.color = Color.parseColor("#4E342E")
                canvas.drawRect(-12f, 4f, -6f, 12f, fillPaint)
                fillPaint.color = coolblueBlue
                canvas.drawArc(RectF(-14f, -5.5f, 14f, 15f), 180f, 180f, true, fillPaint)
                strokePaint.color = white
                strokePaint.strokeWidth = 1.8f
                canvas.drawArc(RectF(-3f, -2f, 6f, 4f), 20f, 140f, false, strokePaint)
                fillPaint.color = coolblueOrange
                canvas.drawRoundRect(RectF(2f, 2f, 19.5f, 7f), 3f, 3f, fillPaint)
            }
            CharacterType.REMI -> {
                // Stylish wavy espresso hair + sleek modern glasses with lens glint
                fillPaint.color = Color.parseColor("#3E2723")
                canvas.drawArc(RectF(-14f, -6f, 14f, 14f), 175f, 190f, true, fillPaint)
                canvas.drawCircle(-7f, 0f, 6f, fillPaint)
                canvas.drawCircle(2f, -2.5f, 6.2f, fillPaint)
                canvas.drawCircle(9f, 0f, 4.5f, fillPaint)
                fillPaint.color = Color.parseColor("#5D4037")
                canvas.drawCircle(1f, -3f, 3f, fillPaint)

                fillPaint.color = Color.argb(75, 129, 212, 250)
                canvas.drawRect(1.5f, 6.5f, 10f, 12.5f, fillPaint)
                strokePaint.color = coolblueNavy
                strokePaint.strokeWidth = 2.3f
                canvas.drawRoundRect(RectF(1f, 6f, 10.5f, 13f), 2f, 2f, strokePaint)
                canvas.drawLine(-4f, 9f, 1f, 9f, strokePaint)
            }
            CharacterType.VERO -> {
                // Warm chestnut-brown hair + Coolblue Orange Headband + Klantgeluk Headset!
                fillPaint.color = Color.parseColor("#5D3A1A")
                canvas.drawArc(RectF(-14.5f, -6f, 14f, 15f), 170f, 200f, true, fillPaint)
                // Side hair lock framing cheek
                canvas.drawRoundRect(RectF(-13.5f, 2f, -7.5f, 17f), 4f, 4f, fillPaint)
                // Swept bangs with warm highlight
                canvas.drawOval(RectF(-4f, -4f, 12f, 5f), fillPaint)
                fillPaint.color = Color.parseColor("#8B5A2B")
                canvas.drawOval(RectF(-1f, -3.5f, 9f, 2f), fillPaint)

                // Coolblue Orange Headband
                strokePaint.color = coolblueOrange
                strokePaint.strokeWidth = 3.6f
                canvas.drawArc(RectF(-12f, -5f, 11f, 13f), 200f, 125f, false, strokePaint)

                // Sleek Klantgeluk Headset (earcup + mic boom)
                fillPaint.color = coolblueNavy
                canvas.drawCircle(-3f, 10.5f, 3.6f, fillPaint)
                fillPaint.color = veroTeal
                canvas.drawCircle(-3f, 10.5f, 1.7f, fillPaint)
                strokePaint.color = coolblueNavy
                strokePaint.strokeWidth = 1.8f
                canvas.drawLine(-2f, 12f, 5.5f, 16.5f, strokePaint)
                fillPaint.color = coolblueOrange
                canvas.drawCircle(6f, 16.5f, 1.8f, fillPaint)
            }
        }

        canvas.restore()

        // Floating crisp name tag ("MAX" / "REMI" / "VERO")
        if (showNameTag) {
            rect.set(cx - 29f, y - 26f, cx + 29f, y - 6f)
            fillPaint.color = character.accentColorArgb
            canvas.drawRoundRect(rect, 9f, 9f, fillPaint)
            textPaint.textAlign = Paint.Align.CENTER
            textPaint.color = white
            textPaint.textSize = 13.5f
            canvas.drawText(character.displayName.uppercase(), cx, y - 11.5f, textPaint)
        }
    }

    // =========================================================================
    // LANDSCAPE HUD & TOUCH CONTROLS (SAFE-AREA AWARE)
    // =========================================================================

    private fun drawHudLandscape(canvas: Canvas, engine: GameEngine) {
        val barH = max(62f, safeTop + 52f)
        fillPaint.color = Color.argb(218, 0, 36, 74)
        canvas.drawRect(0f, 0f, virtualWidth, barH, fillPaint)
        fillPaint.color = coolblueOrange
        canvas.drawRect(0f, barH - 3f, virtualWidth, barH, fillPaint)

        val cy = barH - 24f
        val leftPad = max(18f, safeLeft + 10f)
        val rightEdge = virtualWidth - max(14f, safeRight + 8f)

        // Hero badge + Hearts + Shield
        fillPaint.color = engine.selectedCharacter.accentColorArgb
        rect.set(leftPad, cy - 19f, leftPad + 76f, cy + 12f)
        canvas.drawRoundRect(rect, 10f, 10f, fillPaint)
        textPaint.textAlign = Paint.Align.CENTER
        textPaint.color = white
        textPaint.textSize = 17f
        canvas.drawText(engine.selectedCharacter.displayName.uppercase(), rect.centerX(), cy + 3f, textPaint)

        val heartsText = buildString {
            for (i in 1..GameConfig.STARTING_HEARTS) {
                append(if (i <= engine.hearts) "♥" else "♡")
            }
        }
        textPaint.textAlign = Paint.Align.LEFT
        textPaint.color = Color.parseColor("#FF5252")
        textPaint.textSize = 24f
        canvas.drawText(heartsText, leftPad + 86f, cy + 5f, textPaint)

        var nextLeft = leftPad + 166f
        if (engine.player.hasBakfietsShield) {
            rect.set(nextLeft, cy - 19f, nextLeft + 104f, cy + 12f)
            fillPaint.color = coolblueBlue
            canvas.drawRoundRect(rect, 12f, 12f, fillPaint)
            textPaint.textAlign = Paint.Align.CENTER
            textPaint.color = white
            textPaint.textSize = 14f
            canvas.drawText("🚲 SCHILD", rect.centerX(), cy + 2f, textPaint)
            nextLeft += 114f
        }

        // Right-anchored quick controls so ultrawide & 4:3 tablets never clip!
        var rx = rightEdge
        fun placeRightBtn(w: Float, label: String, bg: Int, action: UiAction, fontSize: Float = 16f) {
            drawPillButton(canvas, RectF(rx - w, cy - 20f, rx, cy + 15f), label, bg, white, action, fontSize)
            rx -= (w + 6f)
        }

        placeRightBtn(38f, "⌂", Color.parseColor("#1D4366"), UiAction.RETURN_TITLE, 19f)
        placeRightBtn(38f, "↺", Color.parseColor("#1D4366"), UiAction.RESTART, 19f)
        placeRightBtn(40f, if (engine.isMuted) "🔇" else "🔊", Color.parseColor("#1D4366"), UiAction.TOGGLE_MUTE, 17f)
        placeRightBtn(
            40f,
            "🎵",
            if (engine.isMusicEnabled) coolblueBlue else Color.parseColor("#1D4366"),
            UiAction.TOGGLE_MUSIC,
            16f
        )
        placeRightBtn(
            40f,
            "🎮",
            if (engine.touchControlsVisible) coolblueBlue else Color.parseColor("#1D4366"),
            UiAction.TOGGLE_TOUCH,
            17f
        )
        placeRightBtn(
            40f,
            "L2",
            if (engine.currentLevelNumber == 2) coolblueBlue else Color.parseColor("#1D4366"),
            UiAction.START_LEVEL_2,
            15f
        )
        placeRightBtn(
            40f,
            "L1",
            if (engine.currentLevelNumber == 1) coolblueBlue else Color.parseColor("#1D4366"),
            UiAction.START_LEVEL_1,
            15f
        )
        placeRightBtn(
            98f,
            if (engine.demoModeEnabled) "⚡ Demo" else "⚡ Norm",
            if (engine.demoModeEnabled) coolblueOrange else coolblueDark,
            UiAction.TOGGLE_DEMO,
            15.5f
        )

        // Center stats & level progress in remaining space between nextLeft and rx
        val statsRight = rx - 14f
        textPaint.textAlign = Paint.Align.RIGHT
        textPaint.textSize = 20f
        textPaint.color = if (engine.timeRemaining < 20f) Color.parseColor("#FF6E6E") else Color.parseColor("#B8E2FF")
        canvas.drawText("⏱ ${engine.timeRemaining.toInt()}s", statsRight, cy + 4f, textPaint)
        textPaint.color = white
        canvas.drawText("${engine.score} pt", statsRight - 84f, cy + 4f, textPaint)
        textPaint.color = smileGold
        canvas.drawText("😄 ${engine.smilesCollected}", statsRight - 196f, cy + 4f, textPaint)

        val progAvail = (statsRight - 285f) - nextLeft
        if (progAvail >= 120f) {
            textPaint.textAlign = Paint.Align.LEFT
            textPaint.color = Color.parseColor("#8CE0FF")
            textPaint.textSize = 16.5f
            val titleLabel = if (progAvail < 185f) engine.level.code else "${engine.level.code} ${engine.level.title}"
            canvas.drawText(titleLabel, nextLeft, cy - 5f, textPaint)
            val prog = (engine.player.x / engine.level.flagX).coerceIn(0f, 1f)
            val barW = min(215f, progAvail - 8f)
            rect.set(nextLeft, cy + 3f, nextLeft + barW, cy + 13f)
            fillPaint.color = Color.argb(115, 255, 255, 255)
            canvas.drawRoundRect(rect, 5f, 5f, fillPaint)
            fillPaint.color = coolblueOrange
            canvas.drawRoundRect(RectF(nextLeft, cy + 3f, nextLeft + barW * prog, cy + 13f), 5f, 5f, fillPaint)
        }

        // Dynamic Level Banner Toast
        val msg = engine.bannerMessage
        if (msg != null && engine.screen == GameScreen.PLAYING) {
            val alpha = (min(1f, engine.bannerTimer * 1.4f) * 235).toInt()
            val bw = min(620f, virtualWidth - 120f)
            rect.set(virtualWidth / 2f - bw / 2f, barH + 14f, virtualWidth / 2f + bw / 2f, barH + 56f)
            fillPaint.color = Color.argb(alpha, 0, 144, 227)
            canvas.drawRoundRect(rect, 21f, 21f, fillPaint)
            textPaint.textAlign = Paint.Align.CENTER
            textPaint.color = Color.argb(alpha, 255, 255, 255)
            textPaint.textSize = 20f
            canvas.drawText(msg, rect.centerX(), rect.centerY() + 7f, textPaint)
        }
    }

    private fun drawTouchControlsLandscape(canvas: Canvas, pressedActions: Set<UiAction>) {
        val leftX = max(28f, safeLeft + 18f)
        val rightX = virtualWidth - max(28f, safeRight + 18f)
        val botY = virtualHeight - max(26f, safeBottom + 14f)

        drawTouchPad(
            canvas,
            RectF(leftX, botY - 108f, leftX + 108f, botY),
            "◀",
            UiAction.TOUCH_LEFT,
            isOrange = false,
            isPressed = pressedActions.contains(UiAction.TOUCH_LEFT)
        )
        drawTouchPad(
            canvas,
            RectF(leftX + 124f, botY - 108f, leftX + 232f, botY),
            "▶",
            UiAction.TOUCH_RIGHT,
            isOrange = false,
            isPressed = pressedActions.contains(UiAction.TOUCH_RIGHT)
        )
        drawTouchPad(
            canvas,
            RectF(rightX - 128f, botY - 124f, rightX, botY),
            "SPRING",
            UiAction.TOUCH_JUMP,
            isOrange = true,
            isPressed = pressedActions.contains(UiAction.TOUCH_JUMP)
        )
    }

    private fun drawTouchPad(
        canvas: Canvas,
        padRect: RectF,
        label: String,
        action: UiAction,
        isOrange: Boolean = false,
        isPressed: Boolean = false
    ) {
        activeButtons.add(HitButton(action, RectF(padRect)))
        val inset = if (isPressed) 4f else 0f
        val drawR = RectF(
            padRect.left + inset,
            padRect.top + inset,
            padRect.right - inset,
            padRect.bottom - inset
        )

        fillPaint.color = when {
            isOrange && isPressed -> Color.argb(235, 255, 136, 34)
            isOrange -> Color.argb(175, 255, 102, 0)
            isPressed -> Color.argb(225, 0, 144, 227)
            else -> Color.argb(150, 0, 43, 84)
        }
        canvas.drawRoundRect(drawR, 28f, 28f, fillPaint)
        strokePaint.color = if (isPressed) white else Color.argb(215, 255, 255, 255)
        strokePaint.strokeWidth = if (isPressed) 4.5f else 3f
        canvas.drawRoundRect(drawR, 28f, 28f, strokePaint)
        textPaint.textAlign = Paint.Align.CENTER
        textPaint.color = white
        textPaint.textSize = if (label.length > 2) 23f else 36f
        canvas.drawText(label, drawR.centerX(), drawR.centerY() + 11f, textPaint)
    }

    // =========================================================================
    // OVERLAYS: LEVEL CLEAR, VICTORY, GAME OVER (ADAPTIVE TO PORTRAIT & LANDSCAPE)
    // =========================================================================

    private fun drawLevelClearOverlay(canvas: Canvas, engine: GameEngine) {
        fillPaint.color = Color.argb(150, 0, 25, 55)
        canvas.drawRect(0f, 0f, virtualWidth, virtualHeight, fillPaint)

        val cardW = min(650f, virtualWidth - 52f)
        val cardH = 435f
        val card = RectF(
            virtualWidth / 2f - cardW / 2f,
            virtualHeight / 2f - cardH / 2f,
            virtualWidth / 2f + cardW / 2f,
            virtualHeight / 2f + cardH / 2f
        )

        fillPaint.color = white
        canvas.drawRoundRect(card, 28f, 28f, fillPaint)
        strokePaint.color = coolblueBlue
        strokePaint.strokeWidth = 6f
        canvas.drawRoundRect(card, 28f, 28f, strokePaint)

        textPaint.textAlign = Paint.Align.CENTER
        textPaint.color = coolblueBlue
        textPaint.textSize = 40f
        canvas.drawText("PAKKET BEZORGD! 😄", card.centerX(), card.top + 74f, textPaint)

        val mates = engine.selectedCharacter.teammates()
        textPaint.color = coolblueOrange
        textPaint.textSize = 22f
        canvas.drawText(
            "${mates[0].displayName} & ${mates[1].displayName}: \"${engine.selectedCharacter.cheerForHero}\"",
            card.centerX(),
            card.top + 120f,
            textPaint
        )

        textPaint.color = coolblueNavy
        textPaint.textSize = 25f
        canvas.drawText("Score: ${engine.score} punten", card.centerX(), card.top + 190f, textPaint)
        canvas.drawText("Glimlachen verzameld: 😄 ${engine.smilesCollected}", card.centerX(), card.top + 234f, textPaint)
        canvas.drawText("Klanten blij gemaakt: ⭐ ${engine.enemiesSmiled}", card.centerX(), card.top + 278f, textPaint)

        drawPillButton(
            canvas,
            RectF(card.left + 48f, card.bottom - 102f, card.right - 48f, card.bottom - 30f),
            "DOOR NAAR 1-2 GRACHTENGORDEL ▶",
            coolblueOrange,
            white,
            UiAction.NEXT_LEVEL,
            22f
        )
    }

    private fun drawVictoryOverlay(canvas: Canvas, engine: GameEngine) {
        fillPaint.color = Color.argb(170, 0, 25, 55)
        canvas.drawRect(0f, 0f, virtualWidth, virtualHeight, fillPaint)

        val mates = engine.selectedCharacter.teammates()
        if (isPortraitLayout) {
            val cardW = virtualWidth - 48f
            val cardH = min(880f, virtualHeight - safeTop - safeBottom - 60f)
            val card = RectF(
                virtualWidth / 2f - cardW / 2f,
                virtualHeight / 2f - cardH / 2f,
                virtualWidth / 2f + cardW / 2f,
                virtualHeight / 2f + cardH / 2f
            )
            fillPaint.color = white
            canvas.drawRoundRect(card, 28f, 28f, fillPaint)
            strokePaint.color = coolblueOrange
            strokePaint.strokeWidth = 6.5f
            canvas.drawRoundRect(card, 28f, 28f, strokePaint)

            textPaint.textAlign = Paint.Align.CENTER
            textPaint.color = coolblueBlue
            textPaint.textSize = 36f
            canvas.drawText("ALLES VOOR EEN GLIMLACH! 🎉", card.centerX(), card.top + 62f, textPaint)

            textPaint.color = coolblueOrange
            textPaint.textSize = 20f
            canvas.drawText(
                "${engine.selectedCharacter.displayName}, ${mates[0].displayName} & ${mates[1].displayName} hebben alles bezorgd!",
                card.centerX(),
                card.top + 100f,
                textPaint
            )

            // Stats card
            rect.set(card.left + 28f, card.top + 124f, card.right - 28f, card.top + 318f)
            fillPaint.color = Color.parseColor("#EBF7FF")
            canvas.drawRoundRect(rect, 18f, 18f, fillPaint)
            textPaint.textAlign = Paint.Align.LEFT
            textPaint.color = coolblueNavy
            textPaint.textSize = 22f
            canvas.drawText("Bezorger: ${engine.selectedCharacter.displayName}", rect.left + 22f, rect.top + 42f, textPaint)
            canvas.drawText("Totaalscore: ${engine.score} pt", rect.left + 22f, rect.top + 80f, textPaint)
            canvas.drawText("Glimlachen: 😄 ${engine.smilesCollected}   •   ⭐ ${engine.enemiesSmiled}", rect.left + 22f, rect.top + 118f, textPaint)
            canvas.drawText("Tijd: ${engine.totalElapsedSeconds.toInt()} sec", rect.left + 22f, rect.top + 156f, textPaint)

            // Leaderboard card
            drawLeaderboardCard(
                canvas,
                RectF(card.left + 28f, card.top + 334f, card.right - 28f, card.bottom - 180f),
                engine
            )

            drawPillButton(
                canvas,
                RectF(card.left + 28f, card.bottom - 160f, card.right - 28f, card.bottom - 94f),
                "↺  NOG EEN KEER SPELEN",
                coolblueOrange,
                white,
                UiAction.START_GAME,
                22f
            )
            drawPillButton(
                canvas,
                RectF(card.left + 28f, card.bottom - 82f, card.right - 28f, card.bottom - 18f),
                "⌂  KIES MAX / REMI / VERO",
                coolblueBlue,
                white,
                UiAction.RETURN_TITLE,
                22f
            )
        } else {
            val cardW = min(860f, virtualWidth - 64f)
            val card = RectF(virtualWidth / 2f - cardW / 2f, 84f, virtualWidth / 2f + cardW / 2f, 646f)
            fillPaint.color = white
            canvas.drawRoundRect(card, 30f, 30f, fillPaint)
            strokePaint.color = coolblueOrange
            strokePaint.strokeWidth = 7f
            canvas.drawRoundRect(card, 30f, 30f, strokePaint)

            textPaint.textAlign = Paint.Align.CENTER
            textPaint.color = coolblueBlue
            textPaint.textSize = 44f
            canvas.drawText("ALLES VOOR EEN GLIMLACH! 🎉", card.centerX(), card.top + 70f, textPaint)

            textPaint.color = coolblueOrange
            textPaint.textSize = 22f
            canvas.drawText(
                "${engine.selectedCharacter.displayName}, ${mates[0].displayName} & ${mates[1].displayName} hebben alle pakketten bezorgd!",
                card.centerX(),
                card.top + 112f,
                textPaint
            )

            rect.set(card.left + 34f, card.top + 142f, card.centerX() - 14f, card.bottom - 120f)
            fillPaint.color = Color.parseColor("#EBF7FF")
            canvas.drawRoundRect(rect, 18f, 18f, fillPaint)
            textPaint.textAlign = Paint.Align.LEFT
            textPaint.color = coolblueNavy
            textPaint.textSize = 23f
            canvas.drawText("Bezorger: ${engine.selectedCharacter.displayName}", rect.left + 22f, rect.top + 52f, textPaint)
            canvas.drawText("Totaalscore: ${engine.score} pt", rect.left + 22f, rect.top + 98f, textPaint)
            canvas.drawText("Glimlachen: 😄 ${engine.smilesCollected}", rect.left + 22f, rect.top + 144f, textPaint)
            canvas.drawText("Vijanden blij: ⭐ ${engine.enemiesSmiled}", rect.left + 22f, rect.top + 190f, textPaint)
            canvas.drawText("Tijd: ${engine.totalElapsedSeconds.toInt()} sec", rect.left + 22f, rect.top + 236f, textPaint)

            drawLeaderboardCard(
                canvas,
                RectF(card.centerX() + 14f, card.top + 142f, card.right - 34f, card.bottom - 120f),
                engine
            )

            drawPillButton(
                canvas,
                RectF(card.left + 38f, card.bottom - 94f, card.centerX() - 12f, card.bottom - 26f),
                "↺  NOG EEN KEER SPELEN",
                coolblueOrange,
                white,
                UiAction.START_GAME,
                21.5f
            )
            drawPillButton(
                canvas,
                RectF(card.centerX() + 12f, card.bottom - 94f, card.right - 38f, card.bottom - 26f),
                "⌂  WISSEL MAX / REMI / VERO",
                coolblueBlue,
                white,
                UiAction.RETURN_TITLE,
                20.5f
            )
        }
    }

    private fun drawGameOverOverlay(canvas: Canvas, engine: GameEngine) {
        fillPaint.color = Color.argb(168, 0, 20, 45)
        canvas.drawRect(0f, 0f, virtualWidth, virtualHeight, fillPaint)

        val cardW = min(590f, virtualWidth - 48f)
        val cardH = 370f
        val card = RectF(
            virtualWidth / 2f - cardW / 2f,
            virtualHeight / 2f - cardH / 2f,
            virtualWidth / 2f + cardW / 2f,
            virtualHeight / 2f + cardH / 2f
        )
        fillPaint.color = white
        canvas.drawRoundRect(card, 28f, 28f, fillPaint)
        strokePaint.color = Color.parseColor("#E53935")
        strokePaint.strokeWidth = 6f
        canvas.drawRoundRect(card, 28f, 28f, strokePaint)

        textPaint.textAlign = Paint.Align.CENTER
        textPaint.color = Color.parseColor("#D32F2F")
        textPaint.textSize = 40f
        canvas.drawText("OEPS, VERTRAGING!", card.centerX(), card.top + 76f, textPaint)

        textPaint.color = coolblueNavy
        textPaint.textSize = 22f
        canvas.drawText(
            "Geen zorgen ${engine.selectedCharacter.displayName} — probeer het direct opnieuw!",
            card.centerX(),
            card.top + 130f,
            textPaint
        )
        textPaint.color = coolblueBlue
        textPaint.textSize = 24f
        canvas.drawText(
            "Score: ${engine.score} pt  •  😄 ${engine.smilesCollected}",
            card.centerX(),
            card.top + 182f,
            textPaint
        )

        drawPillButton(
            canvas,
            RectF(card.left + 36f, card.bottom - 102f, card.centerX() - 10f, card.bottom - 32f),
            "↺  OPNIEUW (R)",
            coolblueOrange,
            white,
            UiAction.RESTART,
            21f
        )
        drawPillButton(
            canvas,
            RectF(card.centerX() + 10f, card.bottom - 102f, card.right - 36f, card.bottom - 32f),
            "⌂  STARTMENU",
            coolblueBlue,
            white,
            UiAction.RETURN_TITLE,
            21f
        )
    }

    private fun drawPillButton(
        canvas: Canvas,
        btnRect: RectF,
        label: String,
        bgColor: Int,
        fgColor: Int,
        action: UiAction,
        textSizeSp: Float
    ) {
        activeButtons.add(HitButton(action, RectF(btnRect)))
        fillPaint.color = Color.argb(45, 0, 15, 36)
        canvas.drawRoundRect(
            RectF(btnRect.left + 2f, btnRect.top + 3f, btnRect.right + 2f, btnRect.bottom + 3f),
            16f, 16f, fillPaint
        )
        fillPaint.color = bgColor
        canvas.drawRoundRect(btnRect, 16f, 16f, fillPaint)
        strokePaint.color = Color.argb(95, 255, 255, 255)
        strokePaint.strokeWidth = 2f
        canvas.drawRoundRect(btnRect, 16f, 16f, strokePaint)

        textPaint.textAlign = Paint.Align.CENTER
        textPaint.color = fgColor
        textPaint.textSize = textSizeSp
        canvas.drawText(label, btnRect.centerX(), btnRect.centerY() + textSizeSp * 0.34f, textPaint)
    }
}
