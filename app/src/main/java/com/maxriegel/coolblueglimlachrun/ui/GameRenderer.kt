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
import com.maxriegel.coolblueglimlachrun.game.LevelTheme
import com.maxriegel.coolblueglimlachrun.game.Platform
import com.maxriegel.coolblueglimlachrun.game.PlatformType
import com.maxriegel.coolblueglimlachrun.game.QuestionBox
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

enum class UiAction {
    SELECT_MAX,
    SELECT_REMI,
    START_GAME,
    START_LEVEL_1,
    START_LEVEL_2,
    TOGGLE_DEMO,
    TOGGLE_TOUCH,
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
    private val coolblueOrange = Color.parseColor("#FF6600")
    private val coolblueOrangeBright = Color.parseColor("#FF8822")
    private val smileGold = Color.parseColor("#FFC400")
    private val white = Color.WHITE

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
    }
    private val rect = RectF()
    private val path = Path()

    // Mapping parameters for screen <-> virtual 1280x720 coordinate conversion
    var scaleFactor: Float = 1f
        private set
    var offsetX: Float = 0f
        private set
    var offsetY: Float = 0f
        private set

    fun screenToVirtual(sx: Float, sy: Float): Pair<Float, Float> {
        val vx = (sx - offsetX) / scaleFactor.coerceAtLeast(0.001f)
        val vy = (sy - offsetY) / scaleFactor.coerceAtLeast(0.001f)
        return vx to vy
    }

    fun render(canvas: Canvas, viewWidth: Int, viewHeight: Int, engine: GameEngine) {
        activeButtons.clear()

        // Fill letterbox background with Coolblue Navy
        canvas.drawColor(coolblueNavy)

        val scaleX = viewWidth / GameConfig.VIEW_WIDTH
        val scaleY = viewHeight / GameConfig.VIEW_HEIGHT
        scaleFactor = min(scaleX, scaleY)
        val drawnW = GameConfig.VIEW_WIDTH * scaleFactor
        val drawnH = GameConfig.VIEW_HEIGHT * scaleFactor
        offsetX = (viewWidth - drawnW) / 2f
        offsetY = (viewHeight - drawnH) / 2f

        canvas.save()
        canvas.translate(offsetX, offsetY)
        canvas.scale(scaleFactor, scaleFactor)
        canvas.clipRect(0f, 0f, GameConfig.VIEW_WIDTH, GameConfig.VIEW_HEIGHT)

        if (engine.screen == GameScreen.TITLE) {
            drawTitleScreen(canvas, engine)
        } else {
            drawGameplayWorld(canvas, engine)
            drawHud(canvas, engine)
            if (engine.screen == GameScreen.PLAYING && engine.touchControlsVisible) {
                drawTouchControls(canvas)
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

    private fun drawTitleScreen(canvas: Canvas, engine: GameEngine) {
        // Vibrant Coolblue sky gradient
        fillPaint.shader = LinearGradient(
            0f, 0f, 0f, GameConfig.VIEW_HEIGHT,
            Color.parseColor("#0090E3"), Color.parseColor("#005596"),
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, GameConfig.VIEW_WIDTH, GameConfig.VIEW_HEIGHT, fillPaint)
        fillPaint.shader = null

        // Subtle Dutch canal skyline silhouette at the bottom of the title screen
        drawDecorativeCanalSkyline(canvas, engine.worldTime)

        // Floating animated decorative smiles in background
        for (i in 0..7) {
            val sx = ((i * 185f + engine.worldTime * 28f) % (GameConfig.VIEW_WIDTH + 80f)) - 40f
            val sy = 95f + sin(engine.worldTime * 2f + i) * 24f + (i % 3) * 165f
            fillPaint.color = Color.argb(40, 255, 255, 255)
            canvas.drawCircle(sx, sy, 22f, fillPaint)
        }

        // Top right quick demo/mute toggles on title screen
        drawPillButton(
            canvas,
            RectF(955f, 22f, 1135f, 64f),
            if (engine.demoModeEnabled) "⚡ Demo: AAN" else "⚡ Demo: UIT",
            if (engine.demoModeEnabled) coolblueOrange else Color.argb(170, 0, 43, 84),
            white,
            UiAction.TOGGLE_DEMO,
            19f
        )
        drawPillButton(
            canvas,
            RectF(1148f, 22f, 1255f, 64f),
            if (engine.isMuted) "🔇 Uit" else "🔊 Aan",
            Color.argb(170, 0, 43, 84),
            white,
            UiAction.TOGGLE_MUTE,
            19f
        )

        // Coolblue Hero Logo Header
        textPaint.textAlign = Paint.Align.CENTER
        textPaint.color = Color.argb(90, 0, 25, 55)
        textPaint.textSize = 62f
        canvas.drawText("COOLBLUE GLIMLACH RUN", GameConfig.VIEW_WIDTH / 2f + 4f, 92f, textPaint)
        textPaint.color = white
        canvas.drawText("COOLBLUE GLIMLACH RUN", GameConfig.VIEW_WIDTH / 2f, 88f, textPaint)

        // Orange accent badge with Dutch Coolblue slogan
        rect.set(420f, 104f, 860f, 146f)
        fillPaint.color = coolblueOrange
        canvas.drawRoundRect(rect, 21f, 21f, fillPaint)
        textPaint.color = white
        textPaint.textSize = 24f
        canvas.drawText("Alles voor een glimlach!  •  Max & Remi Demo", GameConfig.VIEW_WIDTH / 2f, 133f, textPaint)

        // Character Selection Title
        textPaint.color = white
        textPaint.textSize = 26f
        textPaint.textAlign = Paint.Align.LEFT
        canvas.drawText("Kies je bezorger:", 76f, 198f, textPaint)

        // Character Card: MAX
        drawCharacterSelectCard(
            canvas = canvas,
            cardRect = RectF(72f, 214f, 404f, 492f),
            character = CharacterType.MAX,
            selected = engine.selectedCharacter == CharacterType.MAX,
            time = engine.worldTime,
            action = UiAction.SELECT_MAX
        )

        // Character Card: REMI
        drawCharacterSelectCard(
            canvas = canvas,
            cardRect = RectF(428f, 214f, 760f, 492f),
            character = CharacterType.REMI,
            selected = engine.selectedCharacter == CharacterType.REMI,
            time = engine.worldTime,
            action = UiAction.SELECT_REMI
        )

        // Leaderboard Panel on the right (real persisted scores!)
        drawLeaderboardCard(canvas, RectF(800f, 214f, 1212f, 492f), engine)

        // Big Start Button
        drawPillButton(
            canvas,
            RectF(72f, 520f, 515f, 608f),
            "▶  START ALS ${engine.selectedCharacter.displayName.uppercase()}!",
            coolblueOrange,
            white,
            UiAction.START_GAME,
            30f
        )

        // Quick Level Select Buttons for the live demo
        drawPillButton(
            canvas,
            RectF(538f, 520f, 760f, 608f),
            "1-1 Magazijn",
            Color.parseColor("#004E89"),
            white,
            UiAction.START_LEVEL_1,
            23f
        )
        drawPillButton(
            canvas,
            RectF(782f, 520f, 1020f, 608f),
            "1-2 Grachten",
            Color.parseColor("#004E89"),
            white,
            UiAction.START_LEVEL_2,
            23f
        )
        drawPillButton(
            canvas,
            RectF(1042f, 520f, 1212f, 608f),
            if (engine.touchControlsVisible) "🎮 Touch: AAN" else "🎮 Touch: UIT",
            Color.argb(185, 0, 43, 84),
            white,
            UiAction.TOGGLE_TOUCH,
            19f
        )

        // Controls footer hint
        rect.set(72f, 634f, 1212f, 686f)
        fillPaint.color = Color.argb(150, 0, 29, 61)
        canvas.drawRoundRect(rect, 16f, 16f, fillPaint)
        textPaint.textAlign = Paint.Align.CENTER
        textPaint.color = Color.parseColor("#D6F0FF")
        textPaint.textSize = 20f
        canvas.drawText(
            "Besturing: ← → / A D = Rennen   •   Spatie / ↑ / W = Springen   •   1 / 2 = Level   •   G = Demo-modus   •   M = Geluid",
            GameConfig.VIEW_WIDTH / 2f,
            667f,
            textPaint
        )
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

        fillPaint.color = if (selected) white else Color.argb(225, 232, 246, 255)
        canvas.drawRoundRect(cardRect, 24f, 24f, fillPaint)

        strokePaint.color = if (selected) coolblueOrange else Color.argb(120, 255, 255, 255)
        strokePaint.strokeWidth = if (selected) 7f else 3f
        canvas.drawRoundRect(cardRect, 24f, 24f, strokePaint)

        // Selected badge
        if (selected) {
            rect.set(cardRect.right - 132f, cardRect.top + 14f, cardRect.right - 14f, cardRect.top + 48f)
            fillPaint.color = coolblueOrange
            canvas.drawRoundRect(rect, 14f, 14f, fillPaint)
            textPaint.textAlign = Paint.Align.CENTER
            textPaint.color = white
            textPaint.textSize = 18f
            canvas.drawText("GEKOZEN ✓", rect.centerX(), rect.centerY() + 6f, textPaint)
        }

        // Animated character showcase inside card
        val bounceY = if (selected) abs(sin(time * 5.2f)) * 14f else 0f
        canvas.save()
        val cx = cardRect.centerX()
        val cy = cardRect.top + 115f - bounceY
        canvas.translate(cx, cy)
        canvas.scale(1.65f, 1.65f)
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
            showNameTag = false
        )
        canvas.restore()

        // Name & subtitle
        textPaint.textAlign = Paint.Align.CENTER
        textPaint.color = coolblueNavy
        textPaint.textSize = 36f
        canvas.drawText(character.displayName.uppercase(), cardRect.centerX(), cardRect.bottom - 58f, textPaint)

        textPaint.color = coolblueBlue
        textPaint.textSize = 20f
        canvas.drawText(character.badgeTitle, cardRect.centerX(), cardRect.bottom - 26f, textPaint)
    }

    private fun drawLeaderboardCard(canvas: Canvas, cardRect: RectF, engine: GameEngine) {
        fillPaint.color = Color.argb(215, 0, 36, 74)
        canvas.drawRoundRect(cardRect, 24f, 24f, fillPaint)
        strokePaint.color = Color.argb(120, 255, 255, 255)
        strokePaint.strokeWidth = 3f
        canvas.drawRoundRect(cardRect, 24f, 24f, strokePaint)

        textPaint.textAlign = Paint.Align.CENTER
        textPaint.color = smileGold
        textPaint.textSize = 25f
        canvas.drawText("🏆 TOP BEZORGERS", cardRect.centerX(), cardRect.top + 42f, textPaint)

        val entries = engine.getLeaderboard(5)
        if (entries.isEmpty()) {
            textPaint.color = Color.parseColor("#B8E2FF")
            textPaint.textSize = 20f
            canvas.drawText("Nog geen ritten opgeslagen.", cardRect.centerX(), cardRect.centerY() - 10f, textPaint)
            textPaint.color = white
            textPaint.textSize = 19f
            canvas.drawText("Wie haalt de hoogste score:", cardRect.centerX(), cardRect.centerY() + 26f, textPaint)
            textPaint.color = coolblueOrangeBright
            textPaint.textSize = 22f
            canvas.drawText("Max of Remi?", cardRect.centerX(), cardRect.centerY() + 58f, textPaint)
        } else {
            var y = cardRect.top + 86f
            entries.forEachIndexed { index, entry ->
                rect.set(cardRect.left + 18f, y - 26f, cardRect.right - 18f, y + 12f)
                fillPaint.color = if (index == 0) Color.argb(75, 255, 196, 0) else Color.argb(50, 255, 255, 255)
                canvas.drawRoundRect(rect, 10f, 10f, fillPaint)

                textPaint.textAlign = Paint.Align.LEFT
                textPaint.color = if (index == 0) smileGold else white
                textPaint.textSize = 20f
                canvas.drawText("${index + 1}. ${entry.character.displayName}", rect.left + 12f, y, textPaint)

                textPaint.textAlign = Paint.Align.RIGHT
                textPaint.color = Color.parseColor("#7CE0FF")
                canvas.drawText("${entry.score} pt  •  😄${entry.smiles}", rect.right - 12f, y, textPaint)
                y += 42f
            }
        }
    }

    private fun drawDecorativeCanalSkyline(canvas: Canvas, time: Float) {
        val baseY = GameConfig.VIEW_HEIGHT
        val colors = intArrayOf(
            Color.argb(65, 0, 32, 66),
            Color.argb(80, 0, 42, 82),
            Color.argb(55, 0, 25, 55)
        )
        var x = 0f
        var idx = 0
        while (x < GameConfig.VIEW_WIDTH + 90f) {
            val w = 78f + (idx % 3) * 18f
            val h = 115f + (idx % 4) * 28f
            fillPaint.color = colors[idx % colors.size]
            canvas.drawRect(x, baseY - h, x + w - 4f, baseY, fillPaint)
            // Stepped Dutch gable top
            canvas.drawRect(x + 12f, baseY - h - 18f, x + w - 16f, baseY - h, fillPaint)
            canvas.drawRect(x + 24f, baseY - h - 32f, x + w - 28f, baseY - h - 18f, fillPaint)
            x += w
            idx++
        }
        // Subtle animated canal shimmer strip
        fillPaint.color = Color.argb(45, 255, 255, 255)
        val wave = sin(time * 2.4f) * 12f
        canvas.drawRect(0f, baseY - 22f + wave * 0.2f, GameConfig.VIEW_WIDTH, baseY, fillPaint)
    }

    private fun drawGameplayWorld(canvas: Canvas, engine: GameEngine) {
        val shakeX = if (engine.screenShakeTimer > 0f) sin(engine.worldTime * 80f) * 7f else 0f
        val shakeY = if (engine.screenShakeTimer > 0f) cos(engine.worldTime * 75f) * 5f else 0f

        // Sky & Parallax background
        when (engine.level.theme) {
            LevelTheme.WAREHOUSE -> drawWarehouseBackground(canvas, engine.cameraX, engine.worldTime)
            LevelTheme.CANALS -> drawCanalsBackground(canvas, engine.cameraX, engine.worldTime)
        }

        // World space drawing
        canvas.save()
        canvas.translate(-engine.cameraX + shakeX, shakeY)

        // Canal water under gaps in Level 2
        if (engine.level.theme == LevelTheme.CANALS) {
            drawCanalWater(canvas, engine.cameraX, engine.worldTime)
        }

        // Platforms
        for (platform in engine.level.platforms) {
            if (platform.x + platform.width < engine.cameraX - 120f || platform.x > engine.cameraX + GameConfig.VIEW_WIDTH + 120f) {
                continue
            }
            drawPlatform(canvas, platform, engine.worldTime)
        }

        // Coolblue ?-Boxes
        for (box in engine.level.boxes) {
            if (box.x + box.size < engine.cameraX - 80f || box.x > engine.cameraX + GameConfig.VIEW_WIDTH + 80f) {
                continue
            }
            drawQuestionBox(canvas, box, engine.worldTime)
        }

        // Smile Coins
        for (coin in engine.level.coins) {
            if (coin.collected) continue
            if (coin.x < engine.cameraX - 50f || coin.x > engine.cameraX + GameConfig.VIEW_WIDTH + 50f) continue
            drawSmileCoin(canvas, coin.x, coin.y + sin(engine.worldTime * 4.2f + coin.bobPhase) * 6f, coin.radius, engine.worldTime + coin.bobPhase)
        }

        // Power-Up Items (Coolblue Bakfiets)
        for (item in engine.powerUps) {
            drawBakfietsPowerUp(canvas, item.x, item.y, item.width, item.height, engine.worldTime)
        }

        // Finish Area: Coolblue Flagpole + Delivery Van / Dutch Customer House + Cheering Partner!
        drawFinishArea(canvas, engine)

        // Enemies
        for (enemy in engine.level.enemies) {
            if (!enemy.alive && enemy.squashedTimer <= 0f) continue
            if (enemy.x + enemy.width < engine.cameraX - 80f || enemy.x > engine.cameraX + GameConfig.VIEW_WIDTH + 80f) continue
            drawEnemy(canvas, enemy)
        }

        // Player Hero (Max or Remi)
        val p = engine.player
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
                showNameTag = true
            )
        }

        // Particles
        for (pt in engine.particles) {
            val alpha = ((pt.life / pt.maxLife).coerceIn(0f, 1f) * 255).toInt()
            if (pt.isSmile) {
                drawMiniSmileIcon(canvas, pt.x, pt.y, pt.size * 0.65f, alpha)
            } else {
                fillPaint.color = Color.argb(alpha, Color.red(pt.colorArgb), Color.green(pt.colorArgb), Color.blue(pt.colorArgb))
                canvas.drawCircle(pt.x, pt.y, pt.size * 0.5f, fillPaint)
            }
        }

        // Confetti
        for (c in engine.confetti) {
            canvas.save()
            canvas.translate(c.x, c.y)
            canvas.rotate(c.rotation)
            fillPaint.color = c.colorArgb
            canvas.drawRect(-c.width / 2f, -c.height / 2f, c.width / 2f, c.height / 2f, fillPaint)
            canvas.restore()
        }

        // Floating Score Popups
        textPaint.textAlign = Paint.Align.CENTER
        for (f in engine.floatingTexts) {
            val alpha = ((f.life / f.maxLife).coerceIn(0f, 1f) * 255).toInt()
            textPaint.textSize = 24f
            textPaint.color = Color.argb(alpha, 0, 30, 60)
            canvas.drawText(f.text, f.x + 2f, f.y + 2f, textPaint)
            textPaint.color = Color.argb(alpha, Color.red(f.colorArgb), Color.green(f.colorArgb), Color.blue(f.colorArgb))
            canvas.drawText(f.text, f.x, f.y, textPaint)
        }

        canvas.restore()
    }

    private fun drawWarehouseBackground(canvas: Canvas, cameraX: Float, time: Float) {
        // Bright modern warehouse interior wall gradient
        fillPaint.shader = LinearGradient(
            0f, 0f, 0f, GameConfig.VIEW_HEIGHT,
            Color.parseColor("#CBEBFF"), Color.parseColor("#EAF7FF"),
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, GameConfig.VIEW_WIDTH, GameConfig.VIEW_HEIGHT, fillPaint)
        fillPaint.shader = null

        // Ceiling steel beam + hanging warehouse lamps (slow parallax)
        fillPaint.color = Color.parseColor("#8CC3E8")
        canvas.drawRect(0f, 0f, GameConfig.VIEW_WIDTH, 34f, fillPaint)
        val lampSpacing = 320f
        val lampShift = -(cameraX * 0.25f) % lampSpacing
        var lx = lampShift - lampSpacing
        while (lx < GameConfig.VIEW_WIDTH + lampSpacing) {
            fillPaint.color = Color.parseColor("#5C8FB5")
            canvas.drawRect(lx - 3f, 34f, lx + 3f, 88f, fillPaint)
            fillPaint.color = coolblueNavy
            rect.set(lx - 34f, 84f, lx + 34f, 102f)
            canvas.drawRoundRect(rect, 8f, 8f, fillPaint)
            fillPaint.color = Color.argb(45, 255, 240, 180)
            path.reset()
            path.moveTo(lx - 28f, 102f)
            path.lineTo(lx + 28f, 102f)
            path.lineTo(lx + 95f, 310f)
            path.lineTo(lx - 95f, 310f)
            path.close()
            canvas.drawPath(path, fillPaint)
            lx += lampSpacing
        }

        // Giant Coolblue Warehouse Wall Sign (parallax 0.2)
        val bannerCycle = 1150f
        val bannerShift = -(cameraX * 0.22f) % bannerCycle
        var bx = bannerShift + 160f
        while (bx < GameConfig.VIEW_WIDTH + 600f) {
            rect.set(bx, 130f, bx + 470f, 232f)
            fillPaint.color = Color.argb(220, 0, 144, 227)
            canvas.drawRoundRect(rect, 18f, 18f, fillPaint)
            textPaint.textAlign = Paint.Align.CENTER
            textPaint.color = white
            textPaint.textSize = 36f
            canvas.drawText("coolblue magazijn", rect.centerX() - 12f, rect.top + 52f, textPaint)
            fillPaint.color = coolblueOrange
            canvas.drawCircle(rect.right - 64f, rect.top + 42f, 10f, fillPaint)
            textPaint.color = Color.parseColor("#D8F1FF")
            textPaint.textSize = 18f
            canvas.drawText("Alles voor een glimlach • Vandaag besteld, morgen in huis!", rect.centerX(), rect.bottom - 18f, textPaint)
            bx += bannerCycle
        }

        // Background pallet shelving racks (parallax 0.4)
        val rackSpacing = 280f
        val rackShift = -(cameraX * 0.38f) % rackSpacing
        var rx = rackShift - rackSpacing
        var rackIdx = 0
        while (rx < GameConfig.VIEW_WIDTH + rackSpacing) {
            // Vertical rack uprights
            fillPaint.color = Color.parseColor("#9BC4E2")
            canvas.drawRect(rx, 290f, rx + 12f, GameConfig.GROUND_Y, fillPaint)
            canvas.drawRect(rx + 210f, 290f, rx + 222f, GameConfig.GROUND_Y, fillPaint)
            // Horizontal shelf beams & decorative blue Coolblue boxes
            for (tier in 0..2) {
                val sy = 340f + tier * 88f
                fillPaint.color = Color.parseColor("#F59A55")
                canvas.drawRect(rx, sy, rx + 222f, sy + 10f, fillPaint)
                for (boxCol in 0..2) {
                    if ((rackIdx + tier + boxCol) % 4 == 0) continue
                    val boxX = rx + 22f + boxCol * 60f
                    fillPaint.color = Color.argb(145, 0, 144, 227)
                    canvas.drawRoundRect(RectF(boxX, sy - 44f, boxX + 48f, sy), 5f, 5f, fillPaint)
                    fillPaint.color = Color.argb(140, 255, 102, 0)
                    canvas.drawRect(boxX + 12f, sy - 16f, boxX + 36f, sy - 11f, fillPaint)
                }
            }
            rx += rackSpacing
            rackIdx++
        }
    }

    private fun drawCanalsBackground(canvas: Canvas, cameraX: Float, time: Float) {
        // Sunny Dutch blue sky
        fillPaint.shader = LinearGradient(
            0f, 0f, 0f, GameConfig.VIEW_HEIGHT,
            Color.parseColor("#4FC3F7"), Color.parseColor("#E1F5FE"),
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, GameConfig.VIEW_WIDTH, GameConfig.VIEW_HEIGHT, fillPaint)
        fillPaint.shader = null

        // Warm Dutch sun
        fillPaint.color = Color.argb(65, 255, 224, 102)
        canvas.drawCircle(1120f, 118f, 74f, fillPaint)
        fillPaint.color = Color.parseColor("#FFD54F")
        canvas.drawCircle(1120f, 118f, 44f, fillPaint)

        // Fluffy clouds (slow parallax)
        val cloudSpan = 420f
        val cloudShift = -((cameraX * 0.14f + time * 12f) % cloudSpan)
        var cx = cloudShift
        var ci = 0
        while (cx < GameConfig.VIEW_WIDTH + 300f) {
            val cy = 105f + (ci % 3) * 44f
            fillPaint.color = Color.argb(225, 255, 255, 255)
            canvas.drawCircle(cx, cy, 30f, fillPaint)
            canvas.drawCircle(cx + 28f, cy - 12f, 36f, fillPaint)
            canvas.drawCircle(cx + 62f, cy, 28f, fillPaint)
            cx += cloudSpan
            ci++
        }

        // Classic Dutch Windmill (Molen) in midground parallax
        val millCycle = 1280f
        val millX = 680f - ((cameraX * 0.22f) % millCycle)
        for (offset in listOf(0f, millCycle)) {
            val mx = millX + offset
            if (mx > -180f && mx < GameConfig.VIEW_WIDTH + 180f) {
                drawDutchWindmill(canvas, mx, 505f, time)
            }
        }

        // Iconic Dutch Canal Houses (Grachtenpanden, parallax 0.36)
        val housePalette = intArrayOf(
            Color.parseColor("#C85A3F"),
            Color.parseColor("#2F6D80"),
            Color.parseColor("#D9A13B"),
            Color.parseColor("#4B5F73"),
            Color.parseColor("#B8474B"),
            Color.parseColor("#3E7B67")
        )
        val houseW = 112f
        val startHouse = ((cameraX * 0.36f) / houseW).toInt() - 1
        for (i in startHouse..(startHouse + 15)) {
            val hx = i * houseW - cameraX * 0.36f
            val h = 230f + ((abs(i * 37) % 4) * 34f)
            val topY = GameConfig.GROUND_Y - h
            fillPaint.color = housePalette[abs(i) % housePalette.size]
            canvas.drawRect(hx + 2f, topY, hx + houseW - 2f, GameConfig.GROUND_Y, fillPaint)

            // Stepped gable roof (Trapgevel)
            canvas.drawRect(hx + 16f, topY - 22f, hx + houseW - 16f, topY, fillPaint)
            canvas.drawRect(hx + 32f, topY - 40f, hx + houseW - 32f, topY - 22f, fillPaint)
            // White gable trim
            fillPaint.color = white
            canvas.drawRect(hx + 14f, topY - 25f, hx + houseW - 14f, topY - 20f, fillPaint)
            canvas.drawRect(hx + 30f, topY - 43f, hx + houseW - 30f, topY - 38f, fillPaint)

            // Crisp Dutch windows
            fillPaint.color = Color.parseColor("#FFF8E1")
            for (row in 0..2) {
                val wy = topY + 28f + row * 60f
                if (wy + 34f < GameConfig.GROUND_Y - 12f) {
                    canvas.drawRect(hx + 20f, wy, hx + 46f, wy + 34f, fillPaint)
                    canvas.drawRect(hx + 64f, wy, hx + 90f, wy + 34f, fillPaint)
                }
            }
            // Occasional Coolblue storefront awning!
            if (abs(i) % 5 == 2) {
                fillPaint.color = coolblueBlue
                canvas.drawRect(hx + 10f, GameConfig.GROUND_Y - 46f, hx + houseW - 10f, GameConfig.GROUND_Y - 26f, fillPaint)
                fillPaint.color = coolblueOrange
                canvas.drawRect(hx + 10f, GameConfig.GROUND_Y - 26f, hx + houseW - 10f, GameConfig.GROUND_Y - 20f, fillPaint)
            }
        }
    }

    private fun drawDutchWindmill(canvas: Canvas, cx: Float, baseY: Float, time: Float) {
        // Tower body
        fillPaint.color = Color.parseColor("#7A5C43")
        path.reset()
        path.moveTo(cx - 38f, baseY)
        path.lineTo(cx + 38f, baseY)
        path.lineTo(cx + 22f, baseY - 138f)
        path.lineTo(cx - 22f, baseY - 138f)
        path.close()
        canvas.drawPath(path, fillPaint)

        // Cap
        fillPaint.color = coolblueNavy
        canvas.drawArc(RectF(cx - 25f, baseY - 165f, cx + 25f, baseY - 115f), 180f, 180f, true, fillPaint)

        // Spinning blades
        canvas.save()
        canvas.translate(cx, baseY - 132f)
        canvas.rotate(time * 28f)
        for (b in 0..3) {
            canvas.rotate(90f)
            fillPaint.color = Color.parseColor("#5A3E28")
            canvas.drawRect(-3f, 0f, 3f, 92f, fillPaint)
            fillPaint.color = Color.argb(215, 255, 255, 255)
            canvas.drawRect(3f, 16f, 22f, 86f, fillPaint)
        }
        canvas.restore()
    }

    private fun drawCanalWater(canvas: Canvas, cameraX: Float, time: Float) {
        val left = cameraX - 40f
        val right = cameraX + GameConfig.VIEW_WIDTH + 40f
        fillPaint.color = Color.parseColor("#0288D1")
        canvas.drawRect(left, GameConfig.GROUND_Y + 26f, right, GameConfig.VIEW_HEIGHT + 40f, fillPaint)
        fillPaint.color = Color.parseColor("#4FC3F7")
        val waveStep = 64f
        val start = ((left / waveStep).toInt() - 1) * waveStep
        var wx = start
        while (wx < right) {
            val wy = GameConfig.GROUND_Y + 38f + sin(time * 3.5f + wx * 0.04f) * 4f
            canvas.drawRoundRect(RectF(wx, wy, wx + 36f, wy + 6f), 3f, 3f, fillPaint)
            wx += waveStep
        }
    }

    private fun drawPlatform(canvas: Canvas, p: Platform, time: Float) {
        when (p.type) {
            PlatformType.GROUND -> {
                // Top vibrant turf/quay trim + Coolblue industrial base
                fillPaint.color = Color.parseColor("#37474F")
                canvas.drawRect(p.x, p.y, p.x + p.width, p.y + p.height, fillPaint)
                fillPaint.color = coolblueBlue
                canvas.drawRect(p.x, p.y, p.x + p.width, p.y + 18f, fillPaint)
                fillPaint.color = coolblueOrange
                canvas.drawRect(p.x, p.y + 18f, p.x + p.width, p.y + 24f, fillPaint)
                // Clean floor tile joints
                fillPaint.color = Color.parseColor("#455A64")
                var tx = p.x + 12f
                while (tx < p.x + p.width - 12f) {
                    canvas.drawRect(tx, p.y + 38f, (tx + 54f).coerceAtMost(p.x + p.width - 8f), p.y + 52f, fillPaint)
                    tx += 76f
                }
            }
            PlatformType.PALLET -> {
                // Wooden warehouse pallet
                fillPaint.color = Color.parseColor("#D49B54")
                canvas.drawRoundRect(RectF(p.x, p.y, p.x + p.width, p.y + p.height), 6f, 6f, fillPaint)
                fillPaint.color = Color.parseColor("#A46D2A")
                canvas.drawRect(p.x + 8f, p.y + p.height - 9f, p.x + p.width - 8f, p.y + p.height, fillPaint)
                strokePaint.color = Color.parseColor("#7D4E16")
                strokePaint.strokeWidth = 2.5f
                canvas.drawRoundRect(RectF(p.x, p.y, p.x + p.width, p.y + p.height), 6f, 6f, strokePaint)
            }
            PlatformType.PARCEL_STACK -> {
                // Stacked Coolblue cardboard boxes!
                val boxH = 56f
                var y = p.y
                while (y < p.y + p.height - 2f) {
                    val h = min(boxH, p.y + p.height - y)
                    rect.set(p.x + 1f, y + 1f, p.x + p.width - 1f, y + h - 1f)
                    fillPaint.color = coolblueBlue
                    canvas.drawRoundRect(rect, 6f, 6f, fillPaint)
                    strokePaint.color = coolblueDark
                    strokePaint.strokeWidth = 2.5f
                    canvas.drawRoundRect(rect, 6f, 6f, strokePaint)
                    // White shipping tape + tiny orange Coolblue smile on each box
                    fillPaint.color = Color.argb(215, 255, 255, 255)
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
                // Animated industrial conveyor belt!
                fillPaint.color = Color.parseColor("#263238")
                canvas.drawRoundRect(RectF(p.x, p.y, p.x + p.width, p.y + p.height), 10f, 10f, fillPaint)
                val dir = if (p.type == PlatformType.CONVEYOR_RIGHT) 1f else -1f
                val step = 28f
                val animShift = ((time * 130f * dir) % step + step) % step
                fillPaint.color = coolblueOrange
                var sx = p.x + 6f + animShift
                while (sx < p.x + p.width - 12f) {
                    // Chevron arrow indicating boost direction
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
                strokePaint.color = Color.parseColor("#90A4AE")
                strokePaint.strokeWidth = 2.5f
                canvas.drawRoundRect(RectF(p.x, p.y, p.x + p.width, p.y + p.height), 10f, 10f, strokePaint)
            }
            PlatformType.BRICK_BRIDGE -> {
                // Dutch brick canal bridge with subtle arch underneath
                fillPaint.color = Color.parseColor("#A64B38")
                canvas.drawRoundRect(RectF(p.x, p.y, p.x + p.width, p.y + p.height), 8f, 8f, fillPaint)
                fillPaint.color = Color.parseColor("#D8C3A5")
                canvas.drawRect(p.x, p.y, p.x + p.width, p.y + 8f, fillPaint)
                // White canal railing posts on top corners
                fillPaint.color = white
                canvas.drawRect(p.x + 6f, p.y - 10f, p.x + 12f, p.y, fillPaint)
                canvas.drawRect(p.x + p.width - 12f, p.y - 10f, p.x + p.width - 6f, p.y, fillPaint)
            }
            PlatformType.ROOFTOP -> {
                // Dutch orange roof tiles!
                fillPaint.color = Color.parseColor("#D85A2A")
                canvas.drawRoundRect(RectF(p.x, p.y, p.x + p.width, p.y + p.height), 8f, 8f, fillPaint)
                fillPaint.color = Color.parseColor("#FF8A50")
                canvas.drawRect(p.x + 4f, p.y + 3f, p.x + p.width - 4f, p.y + 10f, fillPaint)
                strokePaint.color = Color.parseColor("#8C2D12")
                strokePaint.strokeWidth = 2.5f
                canvas.drawRoundRect(RectF(p.x, p.y, p.x + p.width, p.y + p.height), 8f, 8f, strokePaint)
            }
            PlatformType.SPRING_PAD -> {
                // Bouncy Coolblue Orange Spring Pad!
                val pop = if (p.springAnimTimer > 0f) sin(p.springAnimTimer * 28f) * 10f else 0f
                // Base metal plate
                fillPaint.color = coolblueNavy
                canvas.drawRect(p.x + 6f, p.y + p.height - 7f, p.x + p.width - 6f, p.y + p.height, fillPaint)
                // Coiled spring
                strokePaint.color = Color.parseColor("#CFD8DC")
                strokePaint.strokeWidth = 4f
                canvas.drawLine(p.x + 20f, p.y + p.height - 6f, p.x + p.width - 20f, p.y + 8f - pop, strokePaint)
                canvas.drawLine(p.x + p.width - 20f, p.y + p.height - 6f, p.x + 20f, p.y + 8f - pop, strokePaint)
                // Top bouncy orange pad
                fillPaint.color = coolblueOrange
                canvas.drawRoundRect(RectF(p.x, p.y - pop, p.x + p.width, p.y + 12f - pop), 6f, 6f, fillPaint)
                fillPaint.color = white
                canvas.drawRect(p.x + 14f, p.y + 3f - pop, p.x + p.width - 14f, p.y + 7f - pop, fillPaint)
            }
        }
    }

    private fun drawQuestionBox(canvas: Canvas, box: QuestionBox, time: Float) {
        val y = box.y - box.bumpOffset
        rect.set(box.x, y, box.x + box.size, y + box.size)

        if (!box.isHit) {
            val pulse = (sin(time * 5.5f + box.id) * 0.5f + 0.5f)
            fillPaint.color = if (box.reward == BoxReward.BAKFIETS_SHIELD) {
                coolblueOrange
            } else {
                coolblueBlue
            }
            canvas.drawRoundRect(rect, 10f, 10f, fillPaint)

            strokePaint.color = white
            strokePaint.strokeWidth = 3f + pulse * 1.5f
            canvas.drawRoundRect(rect, 10f, 10f, strokePaint)

            // Corner bolts like classic Mario ?-block
            fillPaint.color = Color.argb(180, 255, 255, 255)
            canvas.drawCircle(rect.left + 8f, rect.top + 8f, 2.5f, fillPaint)
            canvas.drawCircle(rect.right - 8f, rect.top + 8f, 2.5f, fillPaint)
            canvas.drawCircle(rect.left + 8f, rect.bottom - 8f, 2.5f, fillPaint)
            canvas.drawCircle(rect.right - 8f, rect.bottom - 8f, 2.5f, fillPaint)

            textPaint.textAlign = Paint.Align.CENTER
            textPaint.color = white
            textPaint.textSize = 34f
            canvas.drawText("?", rect.centerX(), rect.centerY() + 12f, textPaint)
        } else {
            // Delivered taped box
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
        val spinWidth = (abs(cos(phase * 2.6f)).coerceAtLeast(0.32f)) * r
        fillPaint.color = smileGold
        canvas.drawOval(RectF(cx - spinWidth, cy - r, cx + spinWidth, cy + r), fillPaint)
        strokePaint.color = coolblueOrange
        strokePaint.strokeWidth = 3f
        canvas.drawOval(RectF(cx - spinWidth, cy - r, cx + spinWidth, cy + r), strokePaint)

        // Iconic Coolblue smile on the coin face
        if (spinWidth > r * 0.55f) {
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
        // Glowing Coolblue Bakfiets power-up badge!
        val glow = 4f + sin(time * 8f) * 3f
        fillPaint.color = Color.argb(75, 0, 144, 227)
        canvas.drawCircle(x + w / 2f, y + h / 2f, w * 0.65f + glow, fillPaint)

        // Wheels
        fillPaint.color = coolblueNavy
        canvas.drawCircle(x + 11f, y + h - 8f, 8f, fillPaint)
        canvas.drawCircle(x + w - 11f, y + h - 8f, 8f, fillPaint)
        fillPaint.color = white
        canvas.drawCircle(x + 11f, y + h - 8f, 3.5f, fillPaint)
        canvas.drawCircle(x + w - 11f, y + h - 8f, 3.5f, fillPaint)

        // Coolblue Cargo Box in front
        fillPaint.color = coolblueBlue
        canvas.drawRoundRect(RectF(x + 20f, y + 6f, x + w - 4f, y + h - 10f), 5f, 5f, fillPaint)
        // Orange handlebar & frame
        strokePaint.color = coolblueOrange
        strokePaint.strokeWidth = 3.5f
        canvas.drawLine(x + 11f, y + h - 8f, x + 22f, y + 12f, strokePaint)
        canvas.drawLine(x + 15f, y + 8f, x + 24f, y + 8f, strokePaint)
        // Tiny smile on cargo box
        strokePaint.color = white
        strokePaint.strokeWidth = 2.4f
        canvas.drawArc(RectF(x + 25f, y + 12f, x + w - 9f, y + 24f), 20f, 140f, false, strokePaint)
    }

    private fun drawEnemy(canvas: Canvas, enemy: Enemy) {
        if (!enemy.alive) {
            // Squashed into a happy glowing smile!
            drawMiniSmileIcon(canvas, enemy.x + enemy.width / 2f, enemy.y + enemy.height - 14f, 18f, 230)
            return
        }

        when (enemy.type) {
            EnemyType.SLOW_SNAIL -> {
                // "Trage Bezorging" Grumpy Snail
                val facingLeft = enemy.vx < 0f
                val cx = enemy.x + enemy.width / 2f
                val bottom = enemy.y + enemy.height

                // Snail body
                fillPaint.color = Color.parseColor("#90A4AE")
                canvas.drawRoundRect(RectF(enemy.x + 2f, bottom - 16f, enemy.x + enemy.width - 2f, bottom), 8f, 8f, fillPaint)

                // Red "Delayed Parcel" shell
                val shellCx = if (facingLeft) cx + 6f else cx - 6f
                fillPaint.color = Color.parseColor("#D32F2F")
                canvas.drawCircle(shellCx, bottom - 22f, 17f, fillPaint)
                strokePaint.color = Color.parseColor("#FFCDD2")
                strokePaint.strokeWidth = 3f
                canvas.drawCircle(shellCx, bottom - 22f, 9f, strokePaint)

                // Grumpy head + furrowed brow
                val headX = if (facingLeft) enemy.x + 11f else enemy.x + enemy.width - 11f
                fillPaint.color = white
                canvas.drawCircle(headX, bottom - 20f, 6f, fillPaint)
                fillPaint.color = Color.BLACK
                canvas.drawCircle(headX + (if (facingLeft) -2f else 2f), bottom - 20f, 2.8f, fillPaint)
                strokePaint.color = Color.parseColor("#B71C1C")
                strokePaint.strokeWidth = 2.8f
                canvas.drawLine(headX - 6f, bottom - 28f, headX + 6f, bottom - 24f, strokePaint)
            }
            EnemyType.ONE_STAR_CLOUD -> {
                // Grumpy "1-Ster" Review Storm Cloud
                val cx = enemy.x + enemy.width / 2f
                val cy = enemy.y + enemy.height / 2f
                fillPaint.color = Color.parseColor("#455A64")
                canvas.drawCircle(cx - 16f, cy + 4f, 16f, fillPaint)
                canvas.drawCircle(cx, cy - 6f, 20f, fillPaint)
                canvas.drawCircle(cx + 16f, cy + 4f, 16f, fillPaint)
                canvas.drawRect(cx - 18f, cy + 2f, cx + 18f, cy + 20f, fillPaint)

                // Red "1★" label right on the grumpy cloud!
                textPaint.textAlign = Paint.Align.CENTER
                textPaint.color = Color.parseColor("#FF5252")
                textPaint.textSize = 18f
                canvas.drawText("1★", cx, cy + 8f, textPaint)
            }
        }
    }

    private fun drawFinishArea(canvas: Canvas, engine: GameEngine) {
        val fx = engine.level.flagX
        val gy = engine.level.flagGroundY

        // Coolblue Flagpole
        fillPaint.color = Color.parseColor("#CFD8DC")
        canvas.drawRect(fx - 5f, gy - 310f, fx + 5f, gy, fillPaint)
        // Orange top orb
        fillPaint.color = coolblueOrange
        canvas.drawCircle(fx, gy - 316f, 14f, fillPaint)
        // Pedestal block
        fillPaint.color = coolblueNavy
        canvas.drawRoundRect(RectF(fx - 24f, gy - 26f, fx + 24f, gy), 5f, 5f, fillPaint)

        // Coolblue Flag (slides down on clear!)
        val flagY = gy - 295f + engine.flagSlideProgress * 190f
        fillPaint.color = coolblueBlue
        path.reset()
        path.moveTo(fx + 5f, flagY)
        path.lineTo(fx + 108f, flagY + 26f)
        path.lineTo(fx + 5f, flagY + 54f)
        path.close()
        canvas.drawPath(path, fillPaint)
        // Orange smile on the flag
        strokePaint.color = coolblueOrange
        strokePaint.strokeWidth = 4f
        canvas.drawArc(RectF(fx + 20f, flagY + 14f, fx + 62f, flagY + 40f), 15f, 145f, false, strokePaint)

        // Destination landmark: Delivery Van (Level 1) or Dutch Customer House (Level 2)
        if (engine.level.theme == LevelTheme.WAREHOUSE) {
            drawCoolblueVan(canvas, fx + 150f, gy)
        } else {
            drawDutchCustomerHouse(canvas, fx + 155f, gy)
        }

        // Cheering Partner (Remi if playing Max, Max if playing Remi!)
        val partner = engine.selectedCharacter.partner()
        val cheerBounce = abs(sin(engine.worldTime * 6.5f)) * 16f
        val partnerX = fx + 92f
        val partnerY = gy - 62f - cheerBounce
        drawHeroSprite(
            canvas = canvas,
            x = partnerX,
            y = partnerY,
            width = 44f,
            height = 62f,
            character = partner,
            facingRight = false,
            runningAnim = engine.worldTime * 4f,
            inAir = cheerBounce > 6f,
            hasShield = false,
            worldTime = engine.worldTime,
            showNameTag = true
        )

        // Speech bubble from cheering Max/Remi!
        val bubbleText = partner.partnerCheerText
        rect.set(partnerX - 75f, partnerY - 76f, partnerX + 128f, partnerY - 34f)
        fillPaint.color = white
        canvas.drawRoundRect(rect, 14f, 14f, fillPaint)
        strokePaint.color = coolblueOrange
        strokePaint.strokeWidth = 3f
        canvas.drawRoundRect(rect, 14f, 14f, strokePaint)
        textPaint.textAlign = Paint.Align.CENTER
        textPaint.color = coolblueNavy
        textPaint.textSize = 18f
        canvas.drawText(bubbleText, rect.centerX(), rect.centerY() + 6f, textPaint)
    }

    private fun drawCoolblueVan(canvas: Canvas, x: Float, groundY: Float) {
        // Iconic blue Coolblue delivery van!
        fillPaint.color = coolblueBlue
        canvas.drawRoundRect(RectF(x, groundY - 120f, x + 220f, groundY - 22f), 16f, 16f, fillPaint)
        // Cab windshield
        fillPaint.color = Color.parseColor("#B3E5FC")
        canvas.drawRoundRect(RectF(x + 155f, groundY - 108f, x + 208f, groundY - 64f), 8f, 8f, fillPaint)
        // Coolblue branding on van side
        textPaint.textAlign = Paint.Align.CENTER
        textPaint.color = white
        textPaint.textSize = 26f
        canvas.drawText("coolblue", x + 78f, groundY - 66f, textPaint)
        fillPaint.color = coolblueOrange
        canvas.drawCircle(x + 138f, groundY - 74f, 6f, fillPaint)
        // Wheels
        fillPaint.color = coolblueNavy
        canvas.drawCircle(x + 52f, groundY - 20f, 20f, fillPaint)
        canvas.drawCircle(x + 174f, groundY - 20f, 20f, fillPaint)
        fillPaint.color = Color.LTGRAY
        canvas.drawCircle(x + 52f, groundY - 20f, 9f, fillPaint)
        canvas.drawCircle(x + 174f, groundY - 20f, 9f, fillPaint)
    }

    private fun drawDutchCustomerHouse(canvas: Canvas, x: Float, groundY: Float) {
        // Warm Dutch stepped-gable customer house
        fillPaint.color = Color.parseColor("#C45B3E")
        canvas.drawRect(x, groundY - 205f, x + 210f, groundY, fillPaint)
        canvas.drawRect(x + 25f, groundY - 235f, x + 185f, groundY - 205f, fillPaint)
        canvas.drawRect(x + 55f, groundY - 260f, x + 155f, groundY - 235f, fillPaint)

        // Glowing warm windows
        fillPaint.color = Color.parseColor("#FFE082")
        canvas.drawRoundRect(RectF(x + 26f, groundY - 175f, x + 84f, groundY - 122f), 6f, 6f, fillPaint)
        canvas.drawRoundRect(RectF(x + 126f, groundY - 175f, x + 184f, groundY - 122f), 6f, 6f, fillPaint)

        // Welcoming Coolblue-orange front door
        fillPaint.color = coolblueOrange
        canvas.drawRoundRect(RectF(x + 74f, groundY - 94f, x + 136f, groundY), 8f, 8f, fillPaint)
        fillPaint.color = smileGold
        canvas.drawCircle(x + 124f, groundY - 46f, 4.5f, fillPaint)

        // "KLANT" sign above door
        rect.set(x + 54f, groundY - 118f, x + 156f, groundY - 94f)
        fillPaint.color = coolblueBlue
        canvas.drawRoundRect(rect, 8f, 8f, fillPaint)
        textPaint.textAlign = Paint.Align.CENTER
        textPaint.color = white
        textPaint.textSize = 16f
        canvas.drawText("BLIJE KLANT", rect.centerX(), rect.centerY() + 6f, textPaint)
    }

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
        showNameTag: Boolean
    ) {
        val cx = x + width / 2f
        val cy = y + height / 2f

        // Bakfiets Turbo Shield aura!
        if (hasShield) {
            val pulse = 4f + sin(worldTime * 10f) * 3f
            fillPaint.color = Color.argb(55, 0, 144, 227)
            canvas.drawCircle(cx, cy, 42f + pulse, fillPaint)
            strokePaint.color = coolblueOrange
            strokePaint.strokeWidth = 3.5f
            canvas.drawCircle(cx, cy, 38f + pulse * 0.6f, strokePaint)
        }

        canvas.save()
        canvas.translate(cx, y)
        if (!facingRight) {
            canvas.scale(-1f, 1f)
        }

        val stride = if (inAir) 9f else sin(runningAnim * 2.4f) * 10f

        // Legs & Coolblue sneakers
        fillPaint.color = coolblueNavy
        canvas.drawRoundRect(RectF(-12f + stride * 0.45f, 40f, -2f + stride * 0.45f, 56f), 4f, 4f, fillPaint)
        canvas.drawRoundRect(RectF(2f - stride * 0.45f, 40f, 12f - stride * 0.45f, 56f), 4f, 4f, fillPaint)
        fillPaint.color = coolblueOrange
        canvas.drawRoundRect(RectF(-13f + stride * 0.45f, 53f, 2f + stride * 0.45f, 62f), 4f, 4f, fillPaint)
        canvas.drawRoundRect(RectF(1f - stride * 0.45f, 53f, 16f - stride * 0.45f, 62f), 4f, 4f, fillPaint)

        // Torso: Coolblue Uniform Shirt
        fillPaint.color = coolblueBlue
        canvas.drawRoundRect(RectF(-15f, 20f, 15f, 44f), 8f, 8f, fillPaint)
        // Orange shoulder accent (distinguishes Remi slightly more orange collar!)
        fillPaint.color = coolblueOrange
        if (character == CharacterType.REMI) {
            canvas.drawRoundRect(RectF(-15f, 20f, 15f, 28f), 6f, 6f, fillPaint)
        } else {
            canvas.drawRect(-15f, 34f, 15f, 39f, fillPaint)
        }
        // White Coolblue smile on chest
        strokePaint.color = white
        strokePaint.strokeWidth = 2.6f
        canvas.drawArc(RectF(-7f, 25f, 9f, 36f), 20f, 140f, false, strokePaint)

        // Back arm & front arm (raised heroically in air like Mario!)
        fillPaint.color = Color.parseColor("#FFCC99")
        val armY = if (inAir) 10f else 28f - stride * 0.35f
        canvas.drawCircle(15f, armY, 5.5f, fillPaint)

        // Head
        fillPaint.color = Color.parseColor("#FFD1A4")
        canvas.drawCircle(0f, 11f, 13f, fillPaint)

        // Friendly eye & big smile
        fillPaint.color = coolblueNavy
        canvas.drawCircle(5f, 9f, 2.5f, fillPaint)
        strokePaint.color = Color.parseColor("#8D4E2A")
        strokePaint.strokeWidth = 2f
        canvas.drawArc(RectF(0f, 11f, 10f, 18f), 10f, 140f, false, strokePaint)

        // Distinct Headgear for MAX vs REMI!
        if (character == CharacterType.MAX) {
            // MAX: Iconic Coolblue Blue Cap with Orange Visor!
            fillPaint.color = coolblueBlue
            canvas.drawArc(RectF(-14f, -5f, 14f, 15f), 180f, 180f, true, fillPaint)
            fillPaint.color = coolblueOrange
            canvas.drawRoundRect(RectF(2f, 2f, 19f, 7f), 3f, 3f, fillPaint)
        } else {
            // REMI: Stylish wavy dark hair + cool modern glasses + Coolblue visor accent!
            fillPaint.color = Color.parseColor("#3E2723")
            canvas.drawArc(RectF(-14f, -6f, 14f, 14f), 175f, 190f, true, fillPaint)
            canvas.drawCircle(-7f, 0f, 6f, fillPaint)
            canvas.drawCircle(3f, -2f, 6f, fillPaint)
            // Sleek glasses
            strokePaint.color = coolblueNavy
            strokePaint.strokeWidth = 2.4f
            canvas.drawRect(1f, 6f, 10f, 13f, strokePaint)
            canvas.drawLine(-4f, 9f, 1f, 9f, strokePaint)
        }

        canvas.restore()

        // Floating crisp name tag ("MAX" / "REMI")
        if (showNameTag) {
            rect.set(cx - 28f, y - 26f, cx + 28f, y - 6f)
            fillPaint.color = if (character == CharacterType.MAX) coolblueBlue else coolblueOrange
            canvas.drawRoundRect(rect, 9f, 9f, fillPaint)
            textPaint.textAlign = Paint.Align.CENTER
            textPaint.color = white
            textPaint.textSize = 14f
            canvas.drawText(character.displayName.uppercase(), cx, y - 11f, textPaint)
        }
    }

    private fun drawHud(canvas: Canvas, engine: GameEngine) {
        // Sleek translucent top HUD strip
        fillPaint.color = Color.argb(215, 0, 38, 77)
        canvas.drawRect(0f, 0f, GameConfig.VIEW_WIDTH, 62f, fillPaint)
        fillPaint.color = coolblueOrange
        canvas.drawRect(0f, 60f, GameConfig.VIEW_WIDTH, 63f, fillPaint)

        // Left: Hero name, Hearts, Bakfiets shield
        textPaint.textAlign = Paint.Align.LEFT
        textPaint.color = white
        textPaint.textSize = 22f
        canvas.drawText(engine.selectedCharacter.displayName.uppercase(), 18f, 38f, textPaint)

        // Hearts
        val heartsText = buildString {
            for (i in 1..GameConfig.STARTING_HEARTS) {
                append(if (i <= engine.hearts) "♥" else "♡")
            }
        }
        textPaint.color = Color.parseColor("#FF5252")
        textPaint.textSize = 25f
        canvas.drawText(heartsText, 92f, 39f, textPaint)

        if (engine.player.hasBakfietsShield) {
            rect.set(174f, 14f, 294f, 48f)
            fillPaint.color = coolblueBlue
            canvas.drawRoundRect(rect, 14f, 14f, fillPaint)
            textPaint.textAlign = Paint.Align.CENTER
            textPaint.color = white
            textPaint.textSize = 15f
            canvas.drawText("🚲 SCHILD", rect.centerX(), 36f, textPaint)
        }

        // Center-Left: Level Title & mini progress bar to flagpole
        textPaint.textAlign = Paint.Align.LEFT
        textPaint.color = Color.parseColor("#8CE0FF")
        textPaint.textSize = 18f
        canvas.drawText("${engine.level.code} ${engine.level.title}", 312f, 27f, textPaint)
        val prog = (engine.player.x / engine.level.flagX).coerceIn(0f, 1f)
        rect.set(312f, 36f, 530f, 47f)
        fillPaint.color = Color.argb(120, 255, 255, 255)
        canvas.drawRoundRect(rect, 5f, 5f, fillPaint)
        fillPaint.color = coolblueOrange
        canvas.drawRoundRect(RectF(312f, 36f, 312f + 218f * prog, 47f), 5f, 5f, fillPaint)

        // Score, Smiles, Timer
        textPaint.textAlign = Paint.Align.LEFT
        textPaint.color = smileGold
        textPaint.textSize = 22f
        canvas.drawText("😄 ${engine.smilesCollected}", 558f, 39f, textPaint)
        textPaint.color = white
        canvas.drawText("${engine.score} pt", 648f, 39f, textPaint)
        textPaint.color = if (engine.timeRemaining < 20f) Color.parseColor("#FF6E6E") else Color.parseColor("#B8E2FF")
        canvas.drawText("⏱ ${engine.timeRemaining.toInt()}s", 765f, 39f, textPaint)

        // Right: Live-Demo Quick Controls (clickable!)
        drawPillButton(
            canvas,
            RectF(862f, 11f, 976f, 50f),
            if (engine.demoModeEnabled) "⚡ Demo" else "⚡ Norm",
            if (engine.demoModeEnabled) coolblueOrange else Color.parseColor("#005F9E"),
            white,
            UiAction.TOGGLE_DEMO,
            16f
        )
        drawPillButton(
            canvas,
            RectF(984f, 11f, 1028f, 50f),
            "L1",
            if (engine.currentLevelNumber == 1) coolblueBlue else Color.parseColor("#1D4366"),
            white,
            UiAction.START_LEVEL_1,
            16f
        )
        drawPillButton(
            canvas,
            RectF(1034f, 11f, 1078f, 50f),
            "L2",
            if (engine.currentLevelNumber == 2) coolblueBlue else Color.parseColor("#1D4366"),
            white,
            UiAction.START_LEVEL_2,
            16f
        )
        drawPillButton(
            canvas,
            RectF(1084f, 11f, 1128f, 50f),
            "🎮",
            if (engine.touchControlsVisible) coolblueBlue else Color.parseColor("#1D4366"),
            white,
            UiAction.TOGGLE_TOUCH,
            18f
        )
        drawPillButton(
            canvas,
            RectF(1134f, 11f, 1178f, 50f),
            if (engine.isMuted) "🔇" else "🔊",
            Color.parseColor("#1D4366"),
            white,
            UiAction.TOGGLE_MUTE,
            18f
        )
        drawPillButton(
            canvas,
            RectF(1184f, 11f, 1226f, 50f),
            "↺",
            Color.parseColor("#1D4366"),
            white,
            UiAction.RESTART,
            20f
        )
        drawPillButton(
            canvas,
            RectF(1232f, 11f, 1272f, 50f),
            "⌂",
            Color.parseColor("#1D4366"),
            white,
            UiAction.RETURN_TITLE,
            20f
        )

        // Dynamic Level Banner Toast
        val msg = engine.bannerMessage
        if (msg != null && engine.screen == GameScreen.PLAYING) {
            val alpha = (min(1f, engine.bannerTimer * 1.4f) * 235).toInt()
            rect.set(340f, 78f, 940f, 122f)
            fillPaint.color = Color.argb(alpha, 0, 144, 227)
            canvas.drawRoundRect(rect, 22f, 22f, fillPaint)
            textPaint.textAlign = Paint.Align.CENTER
            textPaint.color = Color.argb(alpha, 255, 255, 255)
            textPaint.textSize = 21f
            canvas.drawText(msg, rect.centerX(), rect.centerY() + 7f, textPaint)
        }
    }

    private fun drawTouchControls(canvas: Canvas) {
        drawTouchPad(canvas, RectF(34f, 588f, 134f, 688f), "◀", UiAction.TOUCH_LEFT)
        drawTouchPad(canvas, RectF(154f, 588f, 254f, 688f), "▶", UiAction.TOUCH_RIGHT)
        drawTouchPad(canvas, RectF(1126f, 574f, 1246f, 694f), "SPRING", UiAction.TOUCH_JUMP, isOrange = true)
    }

    private fun drawTouchPad(canvas: Canvas, padRect: RectF, label: String, action: UiAction, isOrange: Boolean = false) {
        activeButtons.add(HitButton(action, RectF(padRect)))
        fillPaint.color = if (isOrange) Color.argb(150, 255, 102, 0) else Color.argb(135, 0, 43, 84)
        canvas.drawRoundRect(padRect, 28f, 28f, fillPaint)
        strokePaint.color = Color.argb(210, 255, 255, 255)
        strokePaint.strokeWidth = 3f
        canvas.drawRoundRect(padRect, 28f, 28f, strokePaint)
        textPaint.textAlign = Paint.Align.CENTER
        textPaint.color = white
        textPaint.textSize = if (label.length > 2) 22f else 34f
        canvas.drawText(label, padRect.centerX(), padRect.centerY() + 10f, textPaint)
    }

    private fun drawLevelClearOverlay(canvas: Canvas, engine: GameEngine) {
        fillPaint.color = Color.argb(145, 0, 25, 55)
        canvas.drawRect(0f, 0f, GameConfig.VIEW_WIDTH, GameConfig.VIEW_HEIGHT, fillPaint)

        val card = RectF(320f, 145f, 960f, 575f)
        fillPaint.color = white
        canvas.drawRoundRect(card, 28f, 28f, fillPaint)
        strokePaint.color = coolblueBlue
        strokePaint.strokeWidth = 6f
        canvas.drawRoundRect(card, 28f, 28f, strokePaint)

        textPaint.textAlign = Paint.Align.CENTER
        textPaint.color = coolblueBlue
        textPaint.textSize = 44f
        canvas.drawText("PAKKET BEZORGD! 😄", card.centerX(), card.top + 78f, textPaint)

        val partner = engine.selectedCharacter.partner()
        textPaint.color = coolblueOrange
        textPaint.textSize = 24f
        canvas.drawText("${partner.displayName}: \"${partner.partnerCheerText}\"", card.centerX(), card.top + 124f, textPaint)

        textPaint.color = coolblueNavy
        textPaint.textSize = 26f
        canvas.drawText("Score: ${engine.score} punten", card.centerX(), card.top + 195f, textPaint)
        canvas.drawText("Glimlachen verzameld: 😄 ${engine.smilesCollected}", card.centerX(), card.top + 238f, textPaint)
        canvas.drawText("Klanten blij gemaakt: ⭐ ${engine.enemiesSmiled}", card.centerX(), card.top + 281f, textPaint)

        drawPillButton(
            canvas,
            RectF(card.left + 80f, card.bottom - 102f, card.right - 80f, card.bottom - 30f),
            "DOOR NAAR 1-2 GRACHTENGORDEL ▶",
            coolblueOrange,
            white,
            UiAction.NEXT_LEVEL,
            24f
        )
    }

    private fun drawVictoryOverlay(canvas: Canvas, engine: GameEngine) {
        fillPaint.color = Color.argb(165, 0, 25, 55)
        canvas.drawRect(0f, 0f, GameConfig.VIEW_WIDTH, GameConfig.VIEW_HEIGHT, fillPaint)

        val card = RectF(210f, 88f, 1070f, 646f)
        fillPaint.color = white
        canvas.drawRoundRect(card, 30f, 30f, fillPaint)
        strokePaint.color = coolblueOrange
        strokePaint.strokeWidth = 7f
        canvas.drawRoundRect(card, 30f, 30f, strokePaint)

        textPaint.textAlign = Paint.Align.CENTER
        textPaint.color = coolblueBlue
        textPaint.textSize = 46f
        canvas.drawText("ALLES VOOR EEN GLIMLACH! 🎉", card.centerX(), card.top + 72f, textPaint)

        textPaint.color = coolblueOrange
        textPaint.textSize = 24f
        canvas.drawText(
            "${engine.selectedCharacter.displayName} & ${engine.selectedCharacter.partner().displayName} hebben alle pakketten bezorgd!",
            card.centerX(),
            card.top + 114f,
            textPaint
        )

        // Left stats box
        rect.set(card.left + 38f, card.top + 144f, card.centerX() - 18f, card.bottom - 122f)
        fillPaint.color = Color.parseColor("#EBF7FF")
        canvas.drawRoundRect(rect, 18f, 18f, fillPaint)
        textPaint.textAlign = Paint.Align.LEFT
        textPaint.color = coolblueNavy
        textPaint.textSize = 24f
        canvas.drawText("Bezorger: ${engine.selectedCharacter.displayName}", rect.left + 24f, rect.top + 52f, textPaint)
        canvas.drawText("Totaalscore: ${engine.score} pt", rect.left + 24f, rect.top + 98f, textPaint)
        canvas.drawText("Glimlachen: 😄 ${engine.smilesCollected}", rect.left + 24f, rect.top + 144f, textPaint)
        canvas.drawText("Vijanden blij: ⭐ ${engine.enemiesSmiled}", rect.left + 24f, rect.top + 190f, textPaint)
        canvas.drawText("Tijd: ${engine.totalElapsedSeconds.toInt()} sec", rect.left + 24f, rect.top + 236f, textPaint)

        // Right leaderboard box
        drawLeaderboardCard(
            canvas,
            RectF(card.centerX() + 18f, card.top + 144f, card.right - 38f, card.bottom - 122f),
            engine
        )

        drawPillButton(
            canvas,
            RectF(card.left + 44f, card.bottom - 94f, card.centerX() - 16f, card.bottom - 26f),
            "↺  NOG EEN KEER SPELEN",
            coolblueOrange,
            white,
            UiAction.START_GAME,
            23f
        )
        drawPillButton(
            canvas,
            RectF(card.centerX() + 16f, card.bottom - 94f, card.right - 44f, card.bottom - 26f),
            "⌂  WISSEL MAX / REMI",
            coolblueBlue,
            white,
            UiAction.RETURN_TITLE,
            23f
        )
    }

    private fun drawGameOverOverlay(canvas: Canvas, engine: GameEngine) {
        fillPaint.color = Color.argb(165, 0, 20, 45)
        canvas.drawRect(0f, 0f, GameConfig.VIEW_WIDTH, GameConfig.VIEW_HEIGHT, fillPaint)

        val card = RectF(345f, 175f, 935f, 545f)
        fillPaint.color = white
        canvas.drawRoundRect(card, 28f, 28f, fillPaint)
        strokePaint.color = Color.parseColor("#E53935")
        strokePaint.strokeWidth = 6f
        canvas.drawRoundRect(card, 28f, 28f, strokePaint)

        textPaint.textAlign = Paint.Align.CENTER
        textPaint.color = Color.parseColor("#D32F2F")
        textPaint.textSize = 42f
        canvas.drawText("OEPS, VERTRAGING!", card.centerX(), card.top + 78f, textPaint)

        textPaint.color = coolblueNavy
        textPaint.textSize = 24f
        canvas.drawText(
            "Geen zorgen ${engine.selectedCharacter.displayName} — probeer het direct opnieuw!",
            card.centerX(),
            card.top + 132f,
            textPaint
        )
        textPaint.color = coolblueBlue
        textPaint.textSize = 25f
        canvas.drawText(
            "Score: ${engine.score} pt  •  😄 ${engine.smilesCollected}",
            card.centerX(),
            card.top + 184f,
            textPaint
        )

        drawPillButton(
            canvas,
            RectF(card.left + 44f, card.bottom - 104f, card.centerX() - 12f, card.bottom - 34f),
            "↺  OPNIEUW (R)",
            coolblueOrange,
            white,
            UiAction.RESTART,
            22f
        )
        drawPillButton(
            canvas,
            RectF(card.centerX() + 12f, card.bottom - 104f, card.right - 44f, card.bottom - 34f),
            "⌂  STARTMENU",
            coolblueBlue,
            white,
            UiAction.RETURN_TITLE,
            22f
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
        fillPaint.color = bgColor
        canvas.drawRoundRect(btnRect, 16f, 16f, fillPaint)
        strokePaint.color = Color.argb(90, 255, 255, 255)
        strokePaint.strokeWidth = 2f
        canvas.drawRoundRect(btnRect, 16f, 16f, strokePaint)

        textPaint.textAlign = Paint.Align.CENTER
        textPaint.color = fgColor
        textPaint.textSize = textSizeSp
        canvas.drawText(label, btnRect.centerX(), btnRect.centerY() + textSizeSp * 0.34f, textPaint)
    }
}
