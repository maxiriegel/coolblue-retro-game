package com.maxriegel.coolblueglimlachrun.game

import com.maxriegel.coolblueglimlachrun.audio.SoundEffect
import com.maxriegel.coolblueglimlachrun.audio.SoundPlayer
import com.maxriegel.coolblueglimlachrun.data.ScoreRepository
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

class GameEngine(
    private val soundPlayer: SoundPlayer,
    private val scoreRepository: ScoreRepository,
    private val clockMs: () -> Long = { System.currentTimeMillis() }
) {
    var screen: GameScreen = GameScreen.TITLE
        private set

    var selectedCharacter: CharacterType = CharacterType.MAX
        private set

    var currentLevelNumber: Int = 1
        private set

    var level: LevelData = LevelFactory.createLevel(1)
        private set

    var player: PlayerState = PlayerState()
        private set

    var hearts: Int = GameConfig.STARTING_HEARTS
        private set

    var score: Int = 0
        private set

    var smilesCollected: Int = 0
        private set

    var enemiesSmiled: Int = 0
        private set

    var timeRemaining: Float = GameConfig.LEVEL_TIME_LIMIT
        private set

    var totalElapsedSeconds: Float = 0f
        private set

    var cameraX: Float = 0f
        private set

    var demoModeEnabled: Boolean = false
        private set

    var touchControlsVisible: Boolean = true
        private set

    var worldTime: Float = 0f
        private set

    var screenShakeTimer: Float = 0f
        private set

    var bannerMessage: String? = null
        private set

    var bannerTimer: Float = 0f
        private set

    var flagSlideProgress: Float = 0f
        private set

    var levelClearDelayTimer: Float = 0f
        private set

    var runScoreSaved: Boolean = false
        private set

    val powerUps: MutableList<PowerUpItem> = mutableListOf()
    val particles: MutableList<Particle> = mutableListOf()
    val floatingTexts: MutableList<FloatingText> = mutableListOf()
    val confetti: MutableList<ConfettiPiece> = mutableListOf()

    private var lastJumpHeld: Boolean = false

    val isMuted: Boolean
        get() = soundPlayer.isMuted

    fun getLeaderboard(limit: Int = 5): List<ScoreEntry> = scoreRepository.getTopScores(limit)

    fun selectCharacter(character: CharacterType) {
        selectedCharacter = character
        soundPlayer.play(SoundEffect.SELECT)
    }

    fun toggleDemoMode(): Boolean {
        demoModeEnabled = !demoModeEnabled
        showBanner(if (demoModeEnabled) "Demo-modus AAN (Onkwetsbaar!)" else "Demo-modus UIT")
        soundPlayer.play(SoundEffect.SELECT)
        return demoModeEnabled
    }

    fun toggleTouchControls(): Boolean {
        touchControlsVisible = !touchControlsVisible
        soundPlayer.play(SoundEffect.SELECT)
        return touchControlsVisible
    }

    fun toggleMute(): Boolean {
        return soundPlayer.toggleMute()
    }

    fun returnToTitle() {
        screen = GameScreen.TITLE
        confetti.clear()
        particles.clear()
        floatingTexts.clear()
        soundPlayer.play(SoundEffect.SELECT)
    }

    fun startNewGame(startLevel: Int = 1) {
        score = 0
        smilesCollected = 0
        enemiesSmiled = 0
        hearts = GameConfig.STARTING_HEARTS
        totalElapsedSeconds = 0f
        runScoreSaved = false
        loadLevel(startLevel.coerceIn(1, GameConfig.TOTAL_LEVELS), preserveShield = false)
        screen = GameScreen.PLAYING
        soundPlayer.play(SoundEffect.SELECT)
    }

    fun jumpToLevel(levelNumber: Int) {
        val target = levelNumber.coerceIn(1, GameConfig.TOTAL_LEVELS)
        if (screen == GameScreen.TITLE || screen == GameScreen.GAME_OVER || screen == GameScreen.VICTORY) {
            startNewGame(target)
        } else {
            loadLevel(target, preserveShield = player.hasBakfietsShield)
            screen = GameScreen.PLAYING
            soundPlayer.play(SoundEffect.SELECT)
        }
    }

    fun restartCurrentLevel() {
        if (screen == GameScreen.GAME_OVER || screen == GameScreen.VICTORY) {
            startNewGame(if (screen == GameScreen.VICTORY) 1 else currentLevelNumber)
        } else {
            hearts = GameConfig.STARTING_HEARTS
            loadLevel(currentLevelNumber, preserveShield = false)
            screen = GameScreen.PLAYING
            soundPlayer.play(SoundEffect.SELECT)
        }
    }

    fun advanceFromLevelClear() {
        if (screen != GameScreen.LEVEL_CLEAR) return
        if (currentLevelNumber < GameConfig.TOTAL_LEVELS) {
            loadLevel(currentLevelNumber + 1, preserveShield = player.hasBakfietsShield)
            screen = GameScreen.PLAYING
            soundPlayer.play(SoundEffect.SELECT)
        } else {
            enterVictory()
        }
    }

    private fun loadLevel(levelNumber: Int, preserveShield: Boolean) {
        currentLevelNumber = levelNumber
        level = LevelFactory.createLevel(levelNumber)
        player = PlayerState(
            x = level.spawnX,
            y = level.spawnY,
            hasBakfietsShield = preserveShield,
            lastSafeX = level.spawnX,
            lastSafeY = level.spawnY
        )
        timeRemaining = GameConfig.LEVEL_TIME_LIMIT
        cameraX = 0f
        flagSlideProgress = 0f
        levelClearDelayTimer = 0f
        powerUps.clear()
        particles.clear()
        floatingTexts.clear()
        confetti.clear()
        lastJumpHeld = false
        showBanner("${level.code} ${level.title} — ${level.subtitle}")
    }

    fun update(rawDt: Float, input: InputState) {
        val dt = rawDt.coerceIn(0f, 0.05f)
        worldTime += dt

        if (bannerTimer > 0f) {
            bannerTimer = max(0f, bannerTimer - dt)
            if (bannerTimer == 0f) bannerMessage = null
        }
        if (screenShakeTimer > 0f) {
            screenShakeTimer = max(0f, screenShakeTimer - dt)
        }

        updateParticles(dt)
        updateFloatingTexts(dt)
        updateConfetti(dt)

        when (screen) {
            GameScreen.TITLE -> {
                // Idle animations handled via worldTime
            }
            GameScreen.PLAYING -> updatePlaying(dt, input)
            GameScreen.LEVEL_CLEAR -> {
                flagSlideProgress = min(1f, flagSlideProgress + dt * 1.4f)
                levelClearDelayTimer += dt
                if (confetti.size < 90 && levelClearDelayTimer < 2.0f) {
                    spawnConfettiBurst(level.flagX, 240f, 4)
                }
            }
            GameScreen.VICTORY -> {
                flagSlideProgress = 1f
                if (confetti.size < 110) {
                    spawnConfettiBurst(cameraX + Random.nextFloat() * GameConfig.VIEW_WIDTH, -10f, 2)
                }
            }
            GameScreen.GAME_OVER -> {
                // Waiting for restart input
            }
        }
    }

    private fun updatePlaying(dt: Float, input: InputState) {
        totalElapsedSeconds += dt
        timeRemaining = max(0f, timeRemaining - dt)
        if (timeRemaining <= 0f && !demoModeEnabled) {
            triggerGameOver()
            return
        }

        // Animate platforms & boxes
        for (platform in level.platforms) {
            if (platform.springAnimTimer > 0f) {
                platform.springAnimTimer = max(0f, platform.springAnimTimer - dt)
            }
        }
        for (box in level.boxes) {
            if (box.bumpOffset > 0f) {
                box.bumpOffset = max(0f, box.bumpOffset - dt * 95f)
            }
        }

        // Player timers
        if (player.invulnTimer > 0f) {
            player.invulnTimer = max(0f, player.invulnTimer - dt)
        }
        if (input.jumpJustPressed) {
            player.jumpBufferTimer = GameConfig.JUMP_BUFFER_TIME
        } else if (player.jumpBufferTimer > 0f) {
            player.jumpBufferTimer = max(0f, player.jumpBufferTimer - dt)
        }

        if (player.onGround) {
            player.coyoteTimer = GameConfig.COYOTE_TIME
        } else if (player.coyoteTimer > 0f) {
            player.coyoteTimer = max(0f, player.coyoteTimer - dt)
        }

        // Horizontal movement
        val maxSpeed = if (player.hasBakfietsShield) GameConfig.SHIELD_RUN_SPEED else GameConfig.BASE_RUN_SPEED
        val accel = if (player.onGround) GameConfig.GROUND_ACCEL else GameConfig.AIR_ACCEL
        val friction = if (player.onGround) GameConfig.GROUND_FRICTION else GameConfig.AIR_FRICTION

        val moveDir = when {
            input.left && !input.right -> -1f
            input.right && !input.left -> 1f
            else -> 0f
        }

        if (moveDir != 0f) {
            player.facingRight = moveDir > 0f
            player.vx += moveDir * accel * dt
            player.vx = player.vx.coerceIn(-maxSpeed, maxSpeed)
        } else {
            val decel = friction * dt
            if (abs(player.vx) <= decel) {
                player.vx = 0f
            } else {
                player.vx -= kotlin.math.sign(player.vx) * decel
            }
        }

        // Jump execution (coyote time + jump buffer)
        if (player.jumpBufferTimer > 0f && player.coyoteTimer > 0f) {
            player.vy = GameConfig.JUMP_VELOCITY
            player.onGround = false
            player.coyoteTimer = 0f
            player.jumpBufferTimer = 0f
            soundPlayer.play(SoundEffect.JUMP)
            spawnDustBurst(player.x + player.width / 2f, player.y + player.height, 6)
        }

        // Variable jump height (short hop when releasing jump early)
        if (!input.jumpHeld && lastJumpHeld && player.vy < -280f) {
            player.vy *= 0.56f
        }
        lastJumpHeld = input.jumpHeld

        // Gravity
        player.vy = min(GameConfig.MAX_FALL_SPEED, player.vy + GameConfig.GRAVITY * dt)

        // Move X & resolve horizontal collisions
        val conveyorPush = computeConveyorVelocity()
        val prevX = player.x
        player.x += (player.vx + conveyorPush) * dt
        player.x = player.x.coerceIn(0f, level.worldWidth - player.width)
        resolveHorizontalCollisions(prevX)

        // Move Y & resolve vertical collisions
        val prevBottom = player.y + player.height
        val prevTop = player.y
        player.y += player.vy * dt
        player.onGround = false
        resolveVerticalCollisions(prevTop, prevBottom)

        // Walking animation
        if (player.onGround && abs(player.vx) > 20f) {
            player.animTime += dt * (abs(player.vx) / 90f)
        } else if (!player.onGround) {
            player.animTime += dt * 3f
        }

        // Pit fall handling (canal water / warehouse gap)
        if (player.y > GameConfig.VIEW_HEIGHT + 70f) {
            handlePitFall()
            if (screen != GameScreen.PLAYING) return
        }

        // Collect Smile coins
        updateCoins()

        // Update & collect Power-ups
        updatePowerUps(dt)

        // Update Enemies & combat
        updateEnemies(dt, input.jumpHeld)
        if (screen != GameScreen.PLAYING) return

        // Check Coolblue Flagpole finish!
        if (player.x + player.width >= level.flagX - 10f) {
            triggerLevelClear()
            return
        }

        // Smooth Mario-style camera follow
        val targetCamX = (player.x - GameConfig.VIEW_WIDTH * 0.36f)
            .coerceIn(0f, max(0f, level.worldWidth - GameConfig.VIEW_WIDTH))
        cameraX += (targetCamX - cameraX) * min(1f, dt * 8.5f)
    }

    private fun computeConveyorVelocity(): Float {
        if (!player.onGround) return 0f
        val feetY = player.y + player.height
        for (p in level.platforms) {
            if (p.type != PlatformType.CONVEYOR_RIGHT && p.type != PlatformType.CONVEYOR_LEFT) continue
            val horizontallyOver = player.x + player.width > p.x + 2f && player.x < p.x + p.width - 2f
            val standingOn = abs(feetY - p.y) <= 6f
            if (horizontallyOver && standingOn) {
                return if (p.type == PlatformType.CONVEYOR_RIGHT) {
                    GameConfig.CONVEYOR_PUSH_SPEED
                } else {
                    -GameConfig.CONVEYOR_PUSH_SPEED
                }
            }
        }
        return 0f
    }

    private fun resolveHorizontalCollisions(prevX: Float) {
        // Collide against solid pillars/ground and QuestionBoxes
        for (p in level.platforms) {
            if (!isFullSolid(p.type)) continue
            if (rectsOverlap(player.x, player.y + 4f, player.width, player.height - 8f, p.x, p.y, p.width, p.height)) {
                if (prevX + player.width <= p.x + 6f) {
                    player.x = p.x - player.width
                    player.vx = 0f
                } else if (prevX >= p.x + p.width - 6f) {
                    player.x = p.x + p.width
                    player.vx = 0f
                }
            }
        }
        for (box in level.boxes) {
            if (rectsOverlap(player.x, player.y + 4f, player.width, player.height - 8f, box.x, box.y, box.size, box.size)) {
                if (prevX + player.width <= box.x + 6f) {
                    player.x = box.x - player.width
                    player.vx = 0f
                } else if (prevX >= box.x + box.size - 6f) {
                    player.x = box.x + box.size
                    player.vx = 0f
                }
            }
        }
    }

    private fun resolveVerticalCollisions(prevTop: Float, prevBottom: Float) {
        // Check platforms
        for (p in level.platforms) {
            val horizOverlap = player.x + player.width > p.x + 4f && player.x < p.x + p.width - 4f
            if (!horizOverlap) continue

            // Landing on top
            if (player.vy >= 0f && prevBottom <= p.y + 18f && player.y + player.height >= p.y) {
                player.y = p.y - player.height
                if (p.type == PlatformType.SPRING_PAD) {
                    player.vy = GameConfig.SPRING_VELOCITY
                    player.onGround = false
                    p.springAnimTimer = 0.35f
                    soundPlayer.play(SoundEffect.SPRING)
                    spawnSmileSparkles(p.x + p.width / 2f, p.y, 10, 0xFFFF6600.toInt())
                    floatingTexts.add(FloatingText(p.x + p.width / 2f, p.y - 18f, "BOING!", 0xFFFF6600.toInt(), 0.7f))
                } else {
                    player.vy = 0f
                    player.onGround = true
                    if (p.type == PlatformType.GROUND || p.type == PlatformType.BRICK_BRIDGE || p.type == PlatformType.PALLET) {
                        player.lastSafeX = player.x.coerceIn(p.x + 12f, max(p.x + 12f, p.x + p.width - player.width - 12f))
                        player.lastSafeY = p.y - player.height - 4f
                    }
                }
            } else if (isFullSolid(p.type) && player.vy < 0f && prevTop >= p.y + p.height - 18f && player.y <= p.y + p.height) {
                // Hitting ceiling of solid blocks
                player.y = p.y + p.height
                player.vy = 0f
            }
        }

        // Check QuestionBoxes (solid top + head-bonk from below!)
        for (box in level.boxes) {
            val horizOverlap = player.x + player.width > box.x + 4f && player.x < box.x + box.size - 4f
            if (!horizOverlap) continue

            if (player.vy >= 0f && prevBottom <= box.y + 18f && player.y + player.height >= box.y) {
                player.y = box.y - player.height
                player.vy = 0f
                player.onGround = true
            } else if (player.vy < 0f && prevTop >= box.y + box.size - 22f && player.y <= box.y + box.size) {
                player.y = box.y + box.size
                player.vy = 60f
                triggerQuestionBox(box)
            }
        }
    }

    private fun isFullSolid(type: PlatformType): Boolean {
        return type == PlatformType.GROUND || type == PlatformType.PARCEL_STACK
    }

    private fun triggerQuestionBox(box: QuestionBox) {
        if (box.isHit) {
            box.bumpOffset = 6f
            soundPlayer.play(SoundEffect.BOX_HIT)
            return
        }
        box.isHit = true
        box.bumpOffset = 18f
        soundPlayer.play(SoundEffect.BOX_HIT)

        when (box.reward) {
            BoxReward.SMILE_BURST -> {
                smilesCollected += 3
                score += 300
                soundPlayer.play(SoundEffect.SMILE_COIN)
                spawnSmileSparkles(box.x + box.size / 2f, box.y - 10f, 12, 0xFFFF9900.toInt(), includeSmiles = true)
                floatingTexts.add(FloatingText(box.x + box.size / 2f, box.y - 24f, "+3 Glimlachen!", 0xFFFF6600.toInt(), 1.0f))
            }
            BoxReward.BAKFIETS_SHIELD -> {
                powerUps.add(
                    PowerUpItem(
                        x = box.x + 4f,
                        y = box.y - 42f,
                        vx = 135f,
                        vy = -260f
                    )
                )
                spawnSmileSparkles(box.x + box.size / 2f, box.y - 14f, 10, 0xFF0090E3.toInt())
                floatingTexts.add(FloatingText(box.x + box.size / 2f, box.y - 24f, "Coolblue Bakfiets!", 0xFF0090E3.toInt(), 1.05f))
            }
        }
    }

    private fun updateCoins() {
        val pCx = player.x + player.width / 2f
        val pCy = player.y + player.height / 2f
        for (coin in level.coins) {
            if (coin.collected) continue
            val dx = pCx - coin.x
            val dy = pCy - coin.y
            val hitDist = coin.radius + 26f
            if (dx * dx + dy * dy <= hitDist * hitDist) {
                coin.collected = true
                smilesCollected += 1
                score += 100
                soundPlayer.play(SoundEffect.SMILE_COIN)
                spawnSmileSparkles(coin.x, coin.y, 7, 0xFFFFAA00.toInt(), includeSmiles = true)
                floatingTexts.add(FloatingText(coin.x, coin.y - 14f, "+100", 0xFFFF6600.toInt(), 0.7f))
            }
        }
    }

    private fun updatePowerUps(dt: Float) {
        val iterator = powerUps.iterator()
        while (iterator.hasNext()) {
            val item = iterator.next()
            if (item.collected) {
                iterator.remove()
                continue
            }
            item.vy = min(GameConfig.MAX_FALL_SPEED, item.vy + GameConfig.GRAVITY * 0.7f * dt)
            val prevBottom = item.y + item.height
            item.x += item.vx * dt
            item.y += item.vy * dt

            // Bounce off walls & stay on platforms
            for (p in level.platforms) {
                val horiz = item.x + item.width > p.x && item.x < p.x + p.width
                if (horiz && item.vy >= 0f && prevBottom <= p.y + 18f && item.y + item.height >= p.y) {
                    item.y = p.y - item.height
                    item.vy = 0f
                }
            }
            if (item.x <= 0f || item.x + item.width >= level.worldWidth) {
                item.vx = -item.vx
            }

            if (rectsOverlap(player.x, player.y, player.width, player.height, item.x, item.y, item.width, item.height)) {
                item.collected = true
                player.hasBakfietsShield = true
                score += 500
                soundPlayer.play(SoundEffect.POWERUP)
                spawnSmileSparkles(player.x + player.width / 2f, player.y + 20f, 16, 0xFF0090E3.toInt(), includeSmiles = true)
                floatingTexts.add(
                    FloatingText(
                        player.x + player.width / 2f,
                        player.y - 26f,
                        "BAKFIETS TURBO!",
                        0xFF0090E3.toInt(),
                        1.15f
                    )
                )
                iterator.remove()
            } else if (item.y > GameConfig.VIEW_HEIGHT + 120f) {
                iterator.remove()
            }
        }
    }

    private fun updateEnemies(dt: Float, jumpHeld: Boolean) {
        for (enemy in level.enemies) {
            enemy.animTime += dt
            if (!enemy.alive) {
                if (enemy.squashedTimer > 0f) {
                    enemy.squashedTimer = max(0f, enemy.squashedTimer - dt)
                }
                continue
            }

            enemy.x += enemy.vx * dt
            if (enemy.x <= enemy.minX) {
                enemy.x = enemy.minX
                enemy.vx = abs(enemy.vx)
            } else if (enemy.x + enemy.width >= enemy.maxX) {
                enemy.x = enemy.maxX - enemy.width
                enemy.vx = -abs(enemy.vx)
            }
            if (enemy.type == EnemyType.ONE_STAR_CLOUD) {
                enemy.y = enemy.baseY + sin(enemy.animTime * 3.2f) * 18f
            }

            // Player vs Enemy collision (slightly forgiving hitbox)
            if (rectsOverlap(
                    player.x + 4f, player.y + 4f, player.width - 8f, player.height - 4f,
                    enemy.x + 3f, enemy.y + 3f, enemy.width - 6f, enemy.height - 6f
                )
            ) {
                val playerFeet = player.y + player.height
                val stompZone = enemy.y + enemy.height * 0.62f
                val isStomp = player.vy > 0f && playerFeet <= stompZone + 14f

                if (isStomp || demoModeEnabled) {
                    defeatEnemyWithSmile(enemy, jumpHeld)
                } else {
                    damagePlayer()
                    if (screen != GameScreen.PLAYING) return
                }
            }
        }
    }

    private fun defeatEnemyWithSmile(enemy: Enemy, jumpHeld: Boolean) {
        enemy.alive = false
        enemy.squashedTimer = 0.55f
        enemiesSmiled += 1
        val pts = if (enemy.type == EnemyType.ONE_STAR_CLOUD) 300 else 200
        score += pts
        player.vy = if (jumpHeld) GameConfig.STOMP_HIGH_BOUNCE_VELOCITY else GameConfig.STOMP_BOUNCE_VELOCITY
        player.onGround = false
        soundPlayer.play(SoundEffect.STOMP)
        spawnSmileSparkles(enemy.x + enemy.width / 2f, enemy.y + enemy.height / 2f, 14, 0xFFFF6600.toInt(), includeSmiles = true)
        val msg = if (enemy.type == EnemyType.ONE_STAR_CLOUD) "5 STERREN! +$pts" else "BLIJ! +$pts"
        floatingTexts.add(FloatingText(enemy.x + enemy.width / 2f, enemy.y - 16f, msg, 0xFF0090E3.toInt(), 1.0f))
    }

    private fun damagePlayer() {
        if (player.invulnTimer > 0f || demoModeEnabled) return
        screenShakeTimer = 0.25f

        if (player.hasBakfietsShield) {
            player.hasBakfietsShield = false
            player.invulnTimer = 1.6f
            player.vy = -420f
            soundPlayer.play(SoundEffect.HURT)
            spawnSmileSparkles(player.x + player.width / 2f, player.y + 24f, 12, 0xFF0090E3.toInt())
            floatingTexts.add(FloatingText(player.x + player.width / 2f, player.y - 20f, "Schild gered!", 0xFF0090E3.toInt(), 0.9f))
            return
        }

        hearts -= 1
        soundPlayer.play(SoundEffect.HURT)
        if (hearts <= 0) {
            triggerGameOver()
        } else {
            player.invulnTimer = 1.8f
            player.vy = -460f
            player.vx = if (player.facingRight) -220f else 220f
            floatingTexts.add(FloatingText(player.x + player.width / 2f, player.y - 20f, "Oeps!", 0xFFE53935.toInt(), 0.85f))
        }
    }

    private fun handlePitFall() {
        if (demoModeEnabled) {
            // Friendly superhero save for live demos!
            player.x = player.lastSafeX
            player.y = player.lastSafeY - 30f
            player.vx = 0f
            player.vy = -520f
            soundPlayer.play(SoundEffect.SPRING)
            floatingTexts.add(FloatingText(player.x + player.width / 2f, player.y - 20f, "Demo Reddingsactie!", 0xFF0090E3.toInt(), 0.95f))
            return
        }

        hearts -= 1
        soundPlayer.play(SoundEffect.HURT)
        screenShakeTimer = 0.3f
        if (hearts <= 0) {
            triggerGameOver()
        } else {
            player.x = player.lastSafeX
            player.y = player.lastSafeY
            player.vx = 0f
            player.vy = -220f
            player.invulnTimer = 1.8f
            floatingTexts.add(FloatingText(player.x + player.width / 2f, player.y - 18f, "Plons! -1 ♥", 0xFFE53935.toInt(), 0.95f))
        }
    }

    private fun triggerLevelClear() {
        val timeBonus = (timeRemaining.toInt() * 10).coerceAtLeast(0)
        val heightBonus = ((level.flagGroundY - player.y).toInt().coerceIn(0, 320) / 2) * 5
        score += 1000 + timeBonus + heightBonus
        player.x = level.flagX - 18f
        player.vx = 0f
        player.vy = 0f
        soundPlayer.play(SoundEffect.LEVEL_CLEAR)
        spawnConfettiBurst(level.flagX, 260f, 55)
        floatingTexts.add(
            FloatingText(level.flagX, 220f, "PAKKET BEZORGD! +${1000 + timeBonus + heightBonus}", 0xFFFF6600.toInt(), 1.6f, 1.6f)
        )

        if (currentLevelNumber >= GameConfig.TOTAL_LEVELS) {
            enterVictory()
        } else {
            screen = GameScreen.LEVEL_CLEAR
            levelClearDelayTimer = 0f
        }
    }

    private fun enterVictory() {
        screen = GameScreen.VICTORY
        saveRunScoreIfNeeded(levelReached = GameConfig.TOTAL_LEVELS)
        spawnConfettiBurst(level.flagX, 180f, 80)
    }

    private fun triggerGameOver() {
        screen = GameScreen.GAME_OVER
        saveRunScoreIfNeeded(levelReached = currentLevelNumber)
    }

    private fun saveRunScoreIfNeeded(levelReached: Int) {
        if (runScoreSaved || score <= 0) return
        runScoreSaved = true
        scoreRepository.saveScore(
            ScoreEntry(
                character = selectedCharacter,
                score = score,
                smiles = smilesCollected,
                levelReached = levelReached,
                timeSeconds = totalElapsedSeconds.toInt().coerceAtLeast(1),
                timestampMs = clockMs()
            )
        )
    }

    private fun showBanner(message: String) {
        bannerMessage = message
        bannerTimer = 3.2f
    }

    private fun spawnDustBurst(x: Float, y: Float, count: Int) {
        for (i in 0 until count) {
            val angle = -0.3f - Random.nextFloat() * 2.5f
            val speed = 60f + Random.nextFloat() * 110f
            particles.add(
                Particle(
                    x = x,
                    y = y,
                    vx = cos(angle) * speed,
                    vy = sin(angle) * speed,
                    life = 0.35f,
                    maxLife = 0.35f,
                    size = 6f + Random.nextFloat() * 5f,
                    colorArgb = 0xCCFFFFFF.toInt()
                )
            )
        }
    }

    private fun spawnSmileSparkles(
        x: Float,
        y: Float,
        count: Int,
        primaryColor: Int,
        includeSmiles: Boolean = false
    ) {
        for (i in 0 until count) {
            val angle = Random.nextFloat() * (Math.PI * 2.0).toFloat()
            val speed = 90f + Random.nextFloat() * 210f
            val color = if (i % 3 == 0) 0xFF0090E3.toInt() else primaryColor
            particles.add(
                Particle(
                    x = x,
                    y = y,
                    vx = cos(angle) * speed,
                    vy = sin(angle) * speed - 90f,
                    life = 0.65f,
                    maxLife = 0.65f,
                    size = if (includeSmiles && i == 0) 18f else 7f + Random.nextFloat() * 6f,
                    colorArgb = color,
                    isSmile = includeSmiles && i < 2
                )
            )
        }
    }

    private fun spawnConfettiBurst(originX: Float, originY: Float, count: Int) {
        val palette = intArrayOf(
            0xFF0090E3.toInt(),
            0xFFFF6600.toInt(),
            0xFFFFFFFF.toInt(),
            0xFFFFD54F.toInt(),
            0xFF29B6F6.toInt()
        )
        for (i in 0 until count) {
            confetti.add(
                ConfettiPiece(
                    x = originX + (Random.nextFloat() - 0.5f) * 260f,
                    y = originY + (Random.nextFloat() - 0.5f) * 120f,
                    vx = (Random.nextFloat() - 0.5f) * 320f,
                    vy = -120f - Random.nextFloat() * 240f,
                    rotation = Random.nextFloat() * 360f,
                    vRot = (Random.nextFloat() - 0.5f) * 420f,
                    width = 10f + Random.nextFloat() * 8f,
                    height = 6f + Random.nextFloat() * 6f,
                    colorArgb = palette[i % palette.size]
                )
            )
        }
    }

    private fun updateParticles(dt: Float) {
        val it = particles.iterator()
        while (it.hasNext()) {
            val p = it.next()
            p.life -= dt
            if (p.life <= 0f) {
                it.remove()
            } else {
                p.vy += 480f * dt
                p.x += p.vx * dt
                p.y += p.vy * dt
            }
        }
    }

    private fun updateFloatingTexts(dt: Float) {
        val it = floatingTexts.iterator()
        while (it.hasNext()) {
            val f = it.next()
            f.life -= dt
            if (f.life <= 0f) {
                it.remove()
            } else {
                f.y -= 58f * dt
            }
        }
    }

    private fun updateConfetti(dt: Float) {
        val it = confetti.iterator()
        while (it.hasNext()) {
            val c = it.next()
            c.vy += 380f * dt
            c.x += c.vx * dt
            c.y += c.vy * dt
            c.rotation += c.vRot * dt
            if (c.y > GameConfig.VIEW_HEIGHT + 60f) {
                it.remove()
            }
        }
    }

    private fun rectsOverlap(
        ax: Float, ay: Float, aw: Float, ah: Float,
        bx: Float, by: Float, bw: Float, bh: Float
    ): Boolean {
        return ax < bx + bw && ax + aw > bx && ay < by + bh && ay + ah > by
    }
}
