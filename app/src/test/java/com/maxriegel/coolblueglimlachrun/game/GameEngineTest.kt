package com.maxriegel.coolblueglimlachrun.game

import com.maxriegel.coolblueglimlachrun.audio.NoOpSoundPlayer
import com.maxriegel.coolblueglimlachrun.audio.SoundEffect
import com.maxriegel.coolblueglimlachrun.data.InMemoryScoreRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GameEngineTest {

    private lateinit var soundPlayer: NoOpSoundPlayer
    private lateinit var repository: InMemoryScoreRepository
    private lateinit var engine: GameEngine

    @Before
    fun setUp() {
        soundPlayer = NoOpSoundPlayer()
        repository = InMemoryScoreRepository()
        engine = GameEngine(soundPlayer, repository, clockMs = { 1700000000000L })
    }

    @Test
    fun selectAndCycleCharacter_supportsMaxRemiAndVeroWithCorrectTeammates() {
        assertEquals(CharacterType.MAX, engine.selectedCharacter)
        assertEquals(listOf(CharacterType.REMI, CharacterType.VERO), engine.selectedCharacter.teammates())

        engine.cycleCharacter(1)
        assertEquals(CharacterType.REMI, engine.selectedCharacter)
        assertEquals(listOf(CharacterType.MAX, CharacterType.VERO), engine.selectedCharacter.teammates())

        engine.cycleCharacter(1)
        assertEquals(CharacterType.VERO, engine.selectedCharacter)
        assertEquals(listOf(CharacterType.MAX, CharacterType.REMI), engine.selectedCharacter.teammates())

        engine.cycleCharacter(1)
        assertEquals(CharacterType.MAX, engine.selectedCharacter)
        assertTrue(soundPlayer.playedEffects.contains(SoundEffect.SELECT))
    }

    @Test
    fun startNewGame_initializesLevelOneAndPlayingState() {
        engine.startNewGame(1)
        assertEquals(GameScreen.PLAYING, engine.screen)
        assertEquals(1, engine.currentLevelNumber)
        assertEquals("Coolblue Magazijn", engine.level.title)
        assertEquals(GameConfig.STARTING_HEARTS, engine.hearts)
        assertEquals(0, engine.score)
    }

    @Test
    fun movementAndJump_advancesPlayerAppliesSquashStretchAndPlaysJumpSfx() {
        engine.startNewGame(1)
        repeat(20) { engine.update(0.016f, InputState()) }
        assertTrue(engine.player.onGround)
        val startX = engine.player.x
        val startY = engine.player.y

        engine.update(0.016f, InputState(right = true, jumpHeld = true, jumpJustPressed = true))
        assertTrue(engine.player.stretchY > 1.05f)
        repeat(8) { engine.update(0.016f, InputState(right = true, jumpHeld = true)) }

        assertTrue(engine.player.x > startX + 20f)
        assertTrue(engine.player.y < startY - 30f)
        assertTrue(soundPlayer.playedEffects.contains(SoundEffect.JUMP))
    }

    @Test
    fun collectingSmileCoin_incrementsSmilesScoreAndPlaysCoinSound() {
        engine.startNewGame(1)
        val coin = engine.level.coins.first()
        engine.player.x = coin.x - engine.player.width / 2f
        engine.player.y = coin.y - engine.player.height / 2f

        engine.update(0.016f, InputState())

        assertTrue(coin.collected)
        assertEquals(1, engine.smilesCollected)
        assertEquals(100, engine.score)
        assertTrue(soundPlayer.playedEffects.contains(SoundEffect.SMILE_COIN))
    }

    @Test
    fun hittingQuestionBoxFromBelow_grantsSmileBurstOrBakfietsPowerUp() {
        engine.startNewGame(1)
        val shieldBox = engine.level.boxes.first { it.reward == BoxReward.BAKFIETS_SHIELD }
        engine.player.x = shieldBox.x + 4f
        engine.player.y = shieldBox.y + shieldBox.size + 4f
        engine.player.vy = -650f

        engine.update(0.016f, InputState(jumpHeld = true))

        assertTrue(shieldBox.isHit)
        assertEquals(1, engine.powerUps.size)
        assertTrue(soundPlayer.playedEffects.contains(SoundEffect.BOX_HIT))
    }

    @Test
    fun stompingEnemyFromAbove_defeatsEnemyBouncesPlayerAndAwardsPoints() {
        engine.startNewGame(1)
        val enemy = engine.level.enemies.first { it.type == EnemyType.SLOW_SNAIL }
        engine.player.x = enemy.x + 4f
        engine.player.y = enemy.y - engine.player.height + 6f
        engine.player.vy = 280f

        engine.update(0.016f, InputState())

        assertFalse(enemy.alive)
        assertEquals(1, engine.enemiesSmiled)
        assertTrue(engine.score >= 200)
        assertTrue(engine.player.vy < 0f)
        assertTrue(soundPlayer.playedEffects.contains(SoundEffect.STOMP))
    }

    @Test
    fun bakfietsShield_absorbsSideHitWithoutLosingHeart() {
        engine.startNewGame(1)
        engine.player.hasBakfietsShield = true
        val enemy = engine.level.enemies.first { it.type == EnemyType.SLOW_SNAIL }
        engine.player.x = enemy.x - 10f
        engine.player.y = enemy.y - 18f
        engine.player.vy = 0f

        engine.update(0.016f, InputState())

        assertFalse(engine.player.hasBakfietsShield)
        assertEquals(GameConfig.STARTING_HEARTS, engine.hearts)
        assertTrue(engine.player.invulnTimer > 0f)
        assertTrue(soundPlayer.playedEffects.contains(SoundEffect.SHIELD_SAVE))
    }

    @Test
    fun demoMode_protectsAgainstEnemyAndPitFalls() {
        engine.startNewGame(1)
        engine.toggleDemoMode()
        assertTrue(engine.demoModeEnabled)

        engine.player.y = GameConfig.VIEW_HEIGHT + 100f
        engine.update(0.016f, InputState())

        assertEquals(GameConfig.STARTING_HEARTS, engine.hearts)
        assertEquals(GameScreen.PLAYING, engine.screen)
        assertTrue(engine.player.y < GameConfig.VIEW_HEIGHT)
    }

    @Test
    fun completingLevelOneAndTwoAsVero_reachesVictoryAndSavesScore() {
        engine.selectCharacter(CharacterType.VERO)
        engine.startNewGame(1)

        // Reach Level 1 flagpole
        engine.player.x = engine.level.flagX - 5f
        engine.update(0.016f, InputState())
        assertEquals(GameScreen.LEVEL_CLEAR, engine.screen)
        assertTrue(engine.score >= 1000)

        // Advance to Level 2 (Grachtengordel Express)
        engine.advanceFromLevelClear()
        assertEquals(GameScreen.PLAYING, engine.screen)
        assertEquals(2, engine.currentLevelNumber)
        assertEquals("Grachtengordel Express", engine.level.title)

        // Reach Level 2 flagpole -> VICTORY & score saved for VERO
        engine.player.x = engine.level.flagX - 5f
        engine.update(0.016f, InputState())
        assertEquals(GameScreen.VICTORY, engine.screen)

        val saved = repository.getTopScores(5)
        assertEquals(1, saved.size)
        assertEquals(CharacterType.VERO, saved.first().character)
        assertEquals(2, saved.first().levelReached)
        assertEquals(engine.score, saved.first().score)
    }
}
