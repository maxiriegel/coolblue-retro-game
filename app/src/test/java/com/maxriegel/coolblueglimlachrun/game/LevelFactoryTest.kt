package com.maxriegel.coolblueglimlachrun.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LevelFactoryTest {

    @Test
    fun levelOneWarehouse_hasExpectedThemeElementsAndFlag() {
        val level = LevelFactory.createLevel(1)
        assertEquals(LevelTheme.WAREHOUSE, level.theme)
        assertEquals("1-1", level.code)
        assertTrue(level.platforms.any { it.type == PlatformType.CONVEYOR_RIGHT })
        assertTrue(level.platforms.any { it.type == PlatformType.SPRING_PAD })
        assertTrue(level.boxes.any { it.reward == BoxReward.BAKFIETS_SHIELD })
        assertTrue(level.coins.size >= 20)
        assertTrue(level.enemies.any { it.type == EnemyType.SLOW_SNAIL })
        assertTrue(level.enemies.any { it.type == EnemyType.ONE_STAR_CLOUD })
        assertTrue(level.flagX > 3500f && level.flagX < level.worldWidth)
    }

    @Test
    fun levelTwoCanals_hasExpectedThemeElementsAndFlag() {
        val level = LevelFactory.createLevel(2)
        assertEquals(LevelTheme.CANALS, level.theme)
        assertEquals("1-2", level.code)
        assertTrue(level.platforms.any { it.type == PlatformType.BRICK_BRIDGE })
        assertTrue(level.platforms.any { it.type == PlatformType.ROOFTOP })
        assertTrue(level.platforms.any { it.type == PlatformType.SPRING_PAD })
        assertTrue(level.boxes.any { it.reward == BoxReward.BAKFIETS_SHIELD })
        assertTrue(level.coins.size >= 20)
        assertTrue(level.flagX > 4000f && level.flagX < level.worldWidth)
    }
}
