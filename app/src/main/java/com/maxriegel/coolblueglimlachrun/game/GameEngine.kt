package com.maxriegel.coolblueglimlachrun.game

import com.maxriegel.coolblueglimlachrun.audio.MusicScene
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

    var viewportWidth: Float = GameConfig.VIEW_WIDTH

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
    val trailGhosts: MutableList<TrailGhost> = mutableListOf()
    val floatingTexts: MutableList<FloatingText> = mutableListOf()
    val confetti: MutableList<ConfettiPiece> = mutableListOf()

    private var lastJumpHeld: Boolean = false
    private var trailSpawnTimer: Float = 0f
    private var coinStreak: Int = 0
    private var coinStreakTimer: Float = 0f

    val isMuted: Boolean
        get() = soundPlayer.isMuted

    val isMusicEnabled: Boolean
        get() = soundPlayer.isMusicEnabled

    init {
        syncMusicScene()
    }

    fun getLeaderboard(limit: Int = 5): List<ScoreEntry> = scoreRepository.getTopScores(limit)

    fun selectCharacter(character: CharacterType) {
        selectedCharacter = character
        soundPlayer.play(SoundEffect.SELECT)
        soundPlayer.playCharacterMotif(character)
    }

    fun cycleCharacter(delta: Int) {
        selectCharacter(selectedCharacter.next(delta))
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
        val muted = soundPlayer.toggleMute()
        if (!muted) {
            soundPlayer.play(SoundEffect.SELECT)
        }
        return muted
    }

    fun toggleMusic(): Boolean {
        val enabled = soundPlayer.toggleMusic()
        showBanner(if (enabled) "🎵 Chiptune-muziek AAN" else "🎵 Chiptune-muziek UIT")
        soundPlayer.play(SoundEffect.SELECT)
        return enabled
    }

    fun returnToTitle() {
        screen = GameScreen.TITLE
        confetti.clear()
        particles.clear()
        trailGhosts.clear()
        floatingTexts.clear()
        syncMusicScene()
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
        syncMusicScene()
        soundPlayer.play(SoundEffect.SELECT)
    }

    fun jumpToLevel(levelNumber: Int) {
        val target = levelNumber.coerceIn(1, GameConfig.TOTAL_LEVELS)
        if (screen == GameScreen.TITLE || screen == GameScreen.GAME_OVER || screen == GameScreen.VICTORY) {
            startNewGame(target)
        } else {
            loadLevel(target, preserveShield = player.hasBakfietsShield)
            screen = GameScreen.PLAYING
            syncMusicScene()
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
            syncMusicScene()
            soundPlayer.play(SoundEffect.SELECT)
        }
    }

    fun advanceFromLevelClear() {
        if (screen != GameScreen.LEVEL_CLEAR) return
        if (currentLevelNumber < GameConfig.TOTAL_LEVELS) {
            loadLevel(currentLevelNumber + 1, preserveShield = player.hasBakfietsShield)
            screen = GameScreen.PLAYING
            syncMusicScene()
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
        coinStreak = 0
        coinStreakTimer = 0f
        trailSpawnTimer = 0f
        powerUps.clear()
        particles.clear()
        trailGhosts.clear()
        floatingTexts.clear()
        confetti.clear()
        lastJumpHeld = false
        showBanner("${level.code} ${level.title} — ${level.subtitle}")
    }

    private fun syncMusicScene() {
        val scene = when (screen) {
            GameScreen.TITLE -> MusicScene.TITLE
            GameScreen.PLAYING -> when (level.theme) {
                LevelTheme.WAREHOUSE -> MusicScene.WAREHOUSE
                LevelTheme.CANALS -> MusicScene.CANALS
            }
            GameScreen.LEVEL_CLEAR, GameScreen.VICTORY, GameScreen.GAME_OVER -> MusicScene.NONE
        }
        soundPlayer.setMusicScene(scene)
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
        if (coinStreakTimer > 0f) {
            coinStreakTimer = max(0f, coinStreakTimer - dt)
            if (coinStreakTimer == 0f) coinStreak = 0
        }

        updateParticles(dt)
        updateTrailGhosts(dt)
        updateFloatingTexts(dt)
        updateConfetti(dt)

        when (screen) {
            GameScreen.TITLE -> {
                // Idle animations driven by worldTime
            }
            GameScreen.PLAYING -> updatePlaying(dt, input)
            GameScreen.LEVEL_CLEAR -> {
                flagSlideProgress = min(1f, flagSlideProgress + dt * 1.4f)
                levelClearDelayTimer += dt
                if (confetti.size < 95 && levelClearDelayTimer < 2.2f) {
                    spawnConfettiBurst(level.flagX, 240f, 4)
                }
            }
            GameScreen.VICTORY -> {
                flagSlideProgress = 1f
                if (confetti.size < 120) {
                    spawnConfettiBurst(cameraX + Random.nextFloat() * viewportWidth, -10f, 2)
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

        // Smooth squash & stretch spring-back
        player.squashX += (1f - player.squashX) * min(1f, dt * 12f)
        player.stretchY += (1f - player.stretchY) * min(1f, dt * 12f)

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
            player.squashX = 0.78f
            player.stretchY = 1.25f
            soundPlayer.play(SoundEffect.JUMP)
            spawnDustBurst(player.x + player.width / 2f, player.y + player.height, 7)
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
        val wasOnGround = player.onGround
        val preFallVy = player.vy
        val prevBottom = player.y + player.height
        val prevTop = player.y
        player.y += player.vy * dt
        player.onGround = false
        resolveVerticalCollisions(prevTop, prevBottom)

        // Landing impact juice!
        if (!wasOnGround && player.onGround) {
            player.airCombo = 0
            if (preFallVy > 250f) {
                val intensity = ((preFallVy - 250f) / 700f).coerceIn(0.15f, 1f)
                player.squashX = 1f + 0.24f * intensity
                player.stretchY = 1f - 0.20f * intensity
                spawnDustBurst(player.x + player.width / 2f, player.y + player.height, (4 + intensity * 5).toInt())
                if (preFallVy > 420f) {
                    soundPlayer.play(SoundEffect.LAND)
                }
            }
        }

        // Walking animation & sprint dust
        if (player.onGround && abs(player.vx) > 20f) {
            player.animTime += dt * (abs(player.vx) / 88f)
            if (abs(player.vx) > 290f) {
                player.runDustTimer -= dt
                if (player.runDustTimer <= 0f) {
                    player.runDustTimer = 0.11f
                    val footX = if (player.facingRight) player.x + 8f else player.x + player.width - 8f
                    particles.add(
                        Particle(
                            x = footX,
                            y = player.y + player.height - 3f,
                            vx = -player.vx * 0.22f + (Random.nextFloat() - 0.5f) * 30f,
                            vy = -25f - Random.nextFloat() * 35f,
                            life = 0.26f,
                            maxLife = 0.26f,
                            size = 7f + Random.nextFloat() * 4f,
                            colorArgb = 0xAAFFFFFF.toInt(),
                            shape = ParticleShape.DUST
                        )
                    )
                }
            }
        } else if (!player.onGround) {
            player.animTime += dt * 3f
        }

        // Bakfiets Turbo speed afterimage trail
        if ((player.hasBakfietsShield || demoModeEnabled) && (abs(player.vx) > 310f || abs(player.vy) > 520f)) {
            trailSpawnTimer -= dt
            if (trailSpawnTimer <= 0f) {
                trailSpawnTimer = 0.045f
                trailGhosts.add(
                    TrailGhost(
                        x = player.x,
                        y = player.y,
                        facingRight = player.facingRight,
                        animTime = player.animTime,
                        life = 0.18f,
                        maxLife = 0.18f
                    )
                )
            }
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

        // Smooth Mario-style camera follow adapted to active viewportWidth
        val lookAheadRatio = if (viewportWidth < 980f) 0.32f else 0.36f
        val targetCamX = (player.x - viewportWidth * lookAheadRatio)
            .coerceIn(0f, max(0f, level.worldWidth - viewportWidth))
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
        for (p in level.platforms) {
            val horizOverlap = player.x + player.width > p.x + 4f && player.x < p.x + p.width - 4f
            if (!horizOverlap) continue

            if (player.vy >= 0f && prevBottom <= p.y + 18f && player.y + player.height >= p.y) {
                player.y = p.y - player.height
                if (p.type == PlatformType.SPRING_PAD) {
                    player.vy = GameConfig.SPRING_VELOCITY
                    player.onGround = false
                    player.squashX = 0.70f
                    player.stretchY = 1.35f
                    p.springAnimTimer = 0.38f
                    soundPlayer.play(SoundEffect.SPRING)
                    spawnSmileSparkles(p.x + p.width / 2f, p.y, 12, 0xFFFF6600.toInt(), includeStars = true)
                    floatingTexts.add(FloatingText(p.x + p.width / 2f, p.y - 18f, "BOING!", 0xFFFF6600.toInt(), 0.75f, 0.75f, 1.1f))
                } else {
                    player.vy = 0f
                    player.onGround = true
                    if (p.type == PlatformType.GROUND || p.type == PlatformType.BRICK_BRIDGE || p.type == PlatformType.PALLET) {
                        player.lastSafeX = player.x.coerceIn(p.x + 12f, max(p.x + 12f, p.x + p.width - player.width - 12f))
                        player.lastSafeY = p.y - player.height - 4f
                    }
                }
            } else if (isFullSolid(p.type) && player.vy < 0f && prevTop >= p.y + p.height - 18f && player.y <= p.y + p.height) {
                player.y = p.y + p.height
                player.vy = 0f
            }
        }

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
                player.squashX = 1.16f
                player.stretchY = 0.86f
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
            soundPlayer.play(SoundEffect.BOX_EMPTY)
            return
        }
        box.isHit = true
        box.bumpOffset = 20f
        soundPlayer.play(SoundEffect.BOX_HIT)

        when (box.reward) {
            BoxReward.SMILE_BURST -> {
                smilesCollected += 3
                score += 300
                soundPlayer.playCoinStreak(2)
                spawnSmileSparkles(box.x + box.size / 2f, box.y - 10f, 14, 0xFFFF9900.toInt(), includeSmiles = true, includeStars = true)
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
                spawnSmileSparkles(box.x + box.size / 2f, box.y - 14f, 12, 0xFF0090E3.toInt(), includeStars = true)
                floatingTexts.add(FloatingText(box.x + box.size / 2f, box.y - 24f, "Coolblue Bakfiets!", 0xFF0090E3.toInt(), 1.05f, 1.05f, 1.1f))
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
                soundPlayer.playCoinStreak(coinStreak)
                coinStreak = (coinStreak + 1).coerceAtMost(6)
                coinStreakTimer = 0.85f
                spawnSmileSparkles(coin.x, coin.y, 8, 0xFFFFAA00.toInt(), includeSmiles = true, includeStars = true)
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
                player.squashX = 0.85f
                player.stretchY = 1.18f
                score += 500
                soundPlayer.play(SoundEffect.POWERUP)
                spawnSmileSparkles(player.x + player.width / 2f, player.y + 20f, 18, 0xFF0090E3.toInt(), includeSmiles = true, includeStars = true)
                floatingTexts.add(
                    FloatingText(
                        player.x + player.width / 2f,
                        player.y - 26f,
                        "BAKFIETS TURBO!",
                        0xFF0090E3.toInt(),
                        1.15f,
                        1.15f,
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
        enemy.squashedTimer = 0.65f
        enemiesSmiled += 1
        player.airCombo += 1
        val basePts = if (enemy.type == EnemyType.ONE_STAR_CLOUD) 300 else 200
        val comboMult = player.airCombo.coerceAtMost(4)
        val pts = basePts * comboMult
        score += pts
        player.vy = if (jumpHeld) GameConfig.STOMP_HIGH_BOUNCE_VELOCITY else GameConfig.STOMP_BOUNCE_VELOCITY
        player.onGround = false
        player.squashX = 1.22f
        player.stretchY = 0.80f
        soundPlayer.play(SoundEffect.STOMP)
        spawnSmileSparkles(enemy.x + enemy.width / 2f, enemy.y + enemy.height / 2f, 16, 0xFFFF6600.toInt(), includeSmiles = true, includeStars = true)
        val prefix = if (comboMult >= 2) "${comboMult}x COMBO! " else ""
        val msg = if (enemy.type == EnemyType.ONE_STAR_CLOUD) "${prefix}5 STERREN! +$pts" else "${prefix}BLIJ! +$pts"
        floatingTexts.add(FloatingText(enemy.x + enemy.width / 2f, enemy.y - 16f, msg, 0xFF0090E3.toInt(), 1.05f, 1.05f, if (comboMult >= 2) 1.15f else 1.0f))
    }

    private fun damagePlayer() {
        if (player.invulnTimer > 0f || demoModeEnabled) return
        screenShakeTimer = 0.25f

        if (player.hasBakfietsShield) {
            player.hasBakfietsShield = false
            player.invulnTimer = 1.6f
            player.vy = -420f
            player.squashX = 1.2f
            player.stretchY = 0.82f
            soundPlayer.play(SoundEffect.SHIELD_SAVE)
            spawnSmileSparkles(player.x + player.width / 2f, player.y + 24f, 14, 0xFF0090E3.toInt(), includeStars = true)
            floatingTexts.add(FloatingText(player.x + player.width / 2f, player.y - 20f, "Schild gered!", 0xFF0090E3.toInt(), 0.95f))
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
            player.squashX = 0.82f
            player.stretchY = 1.20f
            floatingTexts.add(FloatingText(player.x + player.width / 2f, player.y - 20f, "Oeps!", 0xFFE53935.toInt(), 0.85f))
        }
    }

    private fun handlePitFall() {
        if (level.theme == LevelTheme.CANALS) {
            spawnWaterSplash(player.x + player.width / 2f, GameConfig.GROUND_Y + 26f, 14)
            soundPlayer.play(SoundEffect.SPLASH)
        }

        if (demoModeEnabled) {
            player.x = player.lastSafeX
            player.y = player.lastSafeY - 30f
            player.vx = 0f
            player.vy = -520f
            player.squashX = 0.78f
            player.stretchY = 1.26f
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
        trailGhosts.clear()
        spawnConfettiBurst(level.flagX, 260f, 65)
        floatingTexts.add(
            FloatingText(level.flagX, 220f, "PAKKET BEZORGD! +${1000 + timeBonus + heightBonus}", 0xFFFF6600.toInt(), 1.6f, 1.6f, 1.15f)
        )

        if (currentLevelNumber >= GameConfig.TOTAL_LEVELS) {
            soundPlayer.play(SoundEffect.VICTORY)
            enterVictory()
        } else {
            soundPlayer.play(SoundEffect.LEVEL_CLEAR)
            screen = GameScreen.LEVEL_CLEAR
            levelClearDelayTimer = 0f
            syncMusicScene()
        }
    }

    private fun enterVictory() {
        screen = GameScreen.VICTORY
        syncMusicScene()
        saveRunScoreIfNeeded(levelReached = GameConfig.TOTAL_LEVELS)
        spawnConfettiBurst(level.flagX, 180f, 90)
    }

    private fun triggerGameOver() {
        screen = GameScreen.GAME_OVER
        syncMusicScene()
        soundPlayer.play(SoundEffect.GAME_OVER)
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
            val angle = -0.25f - Random.nextFloat() * 2.6f
            val speed = 55f + Random.nextFloat() * 115f
            particles.add(
                Particle(
                    x = x,
                    y = y,
                    vx = cos(angle) * speed,
                    vy = sin(angle) * speed,
                    life = 0.36f,
                    maxLife = 0.36f,
                    size = 7f + Random.nextFloat() * 5f,
                    colorArgb = 0xCCFFFFFF.toInt(),
                    shape = ParticleShape.DUST
                )
            )
        }
    }

    private fun spawnWaterSplash(x: Float, y: Float, count: Int) {
        for (i in 0 until count) {
            val angle = -0.35f - Random.nextFloat() * 2.4f
            val speed = 130f + Random.nextFloat() * 220f
            val col = if (i % 3 == 0) 0xFFFFFFFF.toInt() else 0xFF4FC3F7.toInt()
            particles.add(
                Particle(
                    x = x + (Random.nextFloat() - 0.5f) * 28f,
                    y = y,
                    vx = cos(angle) * speed,
                    vy = sin(angle) * speed - 60f,
                    life = 0.55f,
                    maxLife = 0.55f,
                    size = 7f + Random.nextFloat() * 6f,
                    colorArgb = col,
                    shape = ParticleShape.SPLASH
                )
            )
        }
    }

    private fun spawnSmileSparkles(
        x: Float,
        y: Float,
        count: Int,
        primaryColor: Int,
        includeSmiles: Boolean = false,
        includeStars: Boolean = false
    ) {
        for (i in 0 until count) {
            val angle = Random.nextFloat() * (Math.PI * 2.0).toFloat()
            val speed = 90f + Random.nextFloat() * 220f
            val color = when {
                i % 4 == 0 -> 0xFFFFFFFF.toInt()
                i % 3 == 0 -> 0xFF0090E3.toInt()
                else -> primaryColor
            }
            val isSmilePiece = includeSmiles && i < 2
            val shape = when {
                isSmilePiece -> ParticleShape.SMILE
                includeStars && i % 2 == 0 -> ParticleShape.STAR
                else -> ParticleShape.SPARK
            }
            particles.add(
                Particle(
                    x = x,
                    y = y,
                    vx = cos(angle) * speed,
                    vy = sin(angle) * speed - 95f,
                    life = 0.68f,
                    maxLife = 0.68f,
                    size = if (isSmilePiece) 18f else 8f + Random.nextFloat() * 6f,
                    colorArgb = color,
                    isSmile = isSmilePiece,
                    shape = shape,
                    rotation = Random.nextFloat() * 360f,
                    vRot = (Random.nextFloat() - 0.5f) * 320f
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
            0xFF00A896.toInt(),
            0xFF29B6F6.toInt()
        )
        for (i in 0 until count) {
            confetti.add(
                ConfettiPiece(
                    x = originX + (Random.nextFloat() - 0.5f) * 280f,
                    y = originY + (Random.nextFloat() - 0.5f) * 120f,
                    vx = (Random.nextFloat() - 0.5f) * 330f,
                    vy = -130f - Random.nextFloat() * 250f,
                    rotation = Random.nextFloat() * 360f,
                    vRot = (Random.nextFloat() - 0.5f) * 440f,
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
                val grav = if (p.shape == ParticleShape.DUST) 120f else 480f
                p.vy += grav * dt
                p.x += p.vx * dt
                p.y += p.vy * dt
                p.rotation += p.vRot * dt
            }
        }
    }

    private fun updateTrailGhosts(dt: Float) {
        val it = trailGhosts.iterator()
        while (it.hasNext()) {
            val g = it.next()
            g.life -= dt
            if (g.life <= 0f) {
                it.remove()
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
