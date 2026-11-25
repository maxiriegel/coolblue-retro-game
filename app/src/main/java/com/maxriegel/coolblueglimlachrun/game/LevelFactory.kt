package com.maxriegel.coolblueglimlachrun.game

object LevelFactory {

    fun createLevel(levelNumber: Int): LevelData {
        return when (levelNumber) {
            1 -> createWarehouseLevel()
            else -> createCanalsLevel()
        }
    }

    private fun createWarehouseLevel(): LevelData {
        val g = GameConfig.GROUND_Y
        var boxId = 1
        var coinId = 1
        var enemyId = 1

        val platforms = mutableListOf(
            // Opening warehouse floor
            Platform(0f, g, 1150f, 140f, PlatformType.GROUND),
            // Parcel pillar obstacles
            Platform(760f, g - 68f, 82f, 68f, PlatformType.PARCEL_STACK),
            // Zone 2 floor + turbo conveyor + elevated pallet
            Platform(1275f, g, 725f, 140f, PlatformType.GROUND),
            Platform(1330f, g - 20f, 250f, 20f, PlatformType.CONVEYOR_RIGHT),
            Platform(1610f, 466f, 255f, 28f, PlatformType.PALLET),
            // Zone 3 floor + orange spring pad + high bonus shelf
            Platform(2125f, g, 645f, 140f, PlatformType.GROUND),
            Platform(2185f, g - 22f, 84f, 22f, PlatformType.SPRING_PAD),
            Platform(2320f, 355f, 345f, 28f, PlatformType.PALLET),
            Platform(2595f, g - 64f, 76f, 64f, PlatformType.PARCEL_STACK),
            // Floating bridge pallet over gap
            Platform(2790f, 518f, 130f, 26f, PlatformType.PALLET),
            // Zone 3b floor + conveyor + shelves
            Platform(2945f, g, 710f, 140f, PlatformType.GROUND),
            Platform(3005f, g - 20f, 235f, 20f, PlatformType.CONVEYOR_RIGHT),
            Platform(3360f, 472f, 210f, 28f, PlatformType.PALLET),
            // Zone 4 final floor & Mario staircase of Coolblue boxes
            Platform(3720f, g, 1220f, 140f, PlatformType.GROUND),
            Platform(3830f, g - 56f, 68f, 56f, PlatformType.PARCEL_STACK),
            Platform(3898f, g - 112f, 68f, 112f, PlatformType.PARCEL_STACK),
            Platform(3966f, g - 168f, 68f, 168f, PlatformType.PARCEL_STACK),
            Platform(4034f, g - 224f, 92f, 224f, PlatformType.PARCEL_STACK)
        )

        val boxes = mutableListOf(
            QuestionBox(boxId++, 440f, 442f, reward = BoxReward.SMILE_BURST),
            QuestionBox(boxId++, 565f, 442f, reward = BoxReward.BAKFIETS_SHIELD),
            QuestionBox(boxId++, 1710f, 326f, reward = BoxReward.SMILE_BURST),
            QuestionBox(boxId++, 2465f, 218f, reward = BoxReward.SMILE_BURST),
            QuestionBox(boxId++, 3265f, 442f, reward = BoxReward.BAKFIETS_SHIELD),
            QuestionBox(boxId++, 3440f, 336f, reward = BoxReward.SMILE_BURST)
        )

        val coins = mutableListOf<SmileCoin>()
        fun addCoinArc(startX: Float, count: Int, spacing: Float, baseY: Float, arcHeight: Float) {
            for (i in 0 until count) {
                val frac = if (count <= 1) 0.5f else i.toFloat() / (count - 1)
                val curve = 4f * frac * (1f - frac)
                coins.add(
                    SmileCoin(
                        id = coinId++,
                        x = startX + i * spacing,
                        y = baseY - curve * arcHeight,
                        bobPhase = i * 0.65f
                    )
                )
            }
        }

        // Opening welcome arc
        addCoinArc(250f, 4, 46f, 520f, 55f)
        // Jump over parcel pillar
        addCoinArc(735f, 3, 52f, 465f, 45f)
        // Jump over first pit
        addCoinArc(1155f, 3, 52f, 495f, 55f)
        // Conveyor sprint coins
        addCoinArc(1365f, 4, 52f, 520f, 20f)
        // Pallet high road coins
        addCoinArc(1640f, 4, 55f, 415f, 15f)
        // Spring pad bonus skyway
        addCoinArc(2355f, 6, 52f, 305f, 25f)
        // Floating pallet bridge coins
        addCoinArc(2815f, 3, 44f, 462f, 22f)
        // Second conveyor coins
        addCoinArc(3035f, 4, 52f, 520f, 18f)
        // Final jump toward Coolblue flagpole
        addCoinArc(4165f, 4, 52f, 345f, 55f)

        val enemies = mutableListOf(
            Enemy(
                id = enemyId++,
                x = 920f, y = g - 40f, width = 52f, height = 40f,
                type = EnemyType.SLOW_SNAIL,
                minX = 860f, maxX = 1100f, vx = -72f
            ),
            Enemy(
                id = enemyId++,
                x = 1720f, y = g - 40f, width = 52f, height = 40f,
                type = EnemyType.SLOW_SNAIL,
                minX = 1610f, maxX = 1930f, vx = -82f
            ),
            Enemy(
                id = enemyId++,
                x = 1790f, y = 380f, width = 58f, height = 42f,
                type = EnemyType.ONE_STAR_CLOUD,
                minX = 1620f, maxX = 1950f, vx = 95f, baseY = 380f
            ),
            Enemy(
                id = enemyId++,
                x = 2420f, y = g - 40f, width = 52f, height = 40f,
                type = EnemyType.SLOW_SNAIL,
                minX = 2295f, maxX = 2560f, vx = -78f
            ),
            Enemy(
                id = enemyId++,
                x = 3300f, y = 415f, width = 58f, height = 42f,
                type = EnemyType.ONE_STAR_CLOUD,
                minX = 3120f, maxX = 3520f, vx = -100f, baseY = 415f
            ),
            Enemy(
                id = enemyId++,
                x = 3450f, y = g - 40f, width = 52f, height = 40f,
                type = EnemyType.SLOW_SNAIL,
                minX = 3290f, maxX = 3600f, vx = -85f
            )
        )

        return LevelData(
            levelNumber = 1,
            code = "1-1",
            title = "Coolblue Magazijn",
            subtitle = "Vandaag besteld, morgen in huis!",
            theme = LevelTheme.WAREHOUSE,
            worldWidth = 4940f,
            spawnX = 95f,
            spawnY = g - 70f,
            flagX = 4420f,
            flagGroundY = g,
            platforms = platforms,
            boxes = boxes,
            coins = coins,
            enemies = enemies
        )
    }

    private fun createCanalsLevel(): LevelData {
        val g = GameConfig.GROUND_Y
        var boxId = 100
        var coinId = 100
        var enemyId = 100

        val platforms = mutableListOf(
            // Opening Amsterdam/Rotterdam quay
            Platform(0f, g, 880f, 140f, PlatformType.GROUND),
            // First arched canal bridge over water
            Platform(955f, 536f, 325f, 32f, PlatformType.BRICK_BRIDGE),
            // Second quay + orange canal awning spring pad + rooftop shortcut
            Platform(1355f, g, 670f, 140f, PlatformType.GROUND),
            Platform(1450f, g - 22f, 84f, 22f, PlatformType.SPRING_PAD),
            Platform(1595f, 362f, 330f, 30f, PlatformType.ROOFTOP),
            // Canal stepping bridges
            Platform(2115f, 542f, 195f, 32f, PlatformType.BRICK_BRIDGE),
            Platform(2405f, 492f, 195f, 32f, PlatformType.BRICK_BRIDGE),
            // Express bike-lane quay
            Platform(2690f, g, 810f, 140f, PlatformType.GROUND),
            Platform(2755f, g - 20f, 285f, 20f, PlatformType.CONVEYOR_RIGHT),
            Platform(3365f, g - 22f, 84f, 22f, PlatformType.SPRING_PAD),
            // Mid-air canal rooftop over wide gracht
            Platform(3570f, 418f, 275f, 30f, PlatformType.ROOFTOP),
            // Lower safety pier under rooftop so nobody gets stuck
            Platform(3620f, 548f, 180f, 30f, PlatformType.BRICK_BRIDGE),
            // Final Customer Street + Coolblue Box Staircase
            Platform(3935f, g, 1420f, 140f, PlatformType.GROUND),
            Platform(4220f, g - 56f, 68f, 56f, PlatformType.PARCEL_STACK),
            Platform(4288f, g - 112f, 68f, 112f, PlatformType.PARCEL_STACK),
            Platform(4356f, g - 168f, 68f, 168f, PlatformType.PARCEL_STACK),
            Platform(4424f, g - 224f, 92f, 224f, PlatformType.PARCEL_STACK)
        )

        val boxes = mutableListOf(
            QuestionBox(boxId++, 410f, 442f, reward = BoxReward.SMILE_BURST),
            QuestionBox(boxId++, 525f, 442f, reward = BoxReward.BAKFIETS_SHIELD),
            QuestionBox(boxId++, 1730f, 222f, reward = BoxReward.SMILE_BURST),
            QuestionBox(boxId++, 2930f, 438f, reward = BoxReward.BAKFIETS_SHIELD),
            QuestionBox(boxId++, 3685f, 276f, reward = BoxReward.SMILE_BURST)
        )

        val coins = mutableListOf<SmileCoin>()
        fun addCoinArc(startX: Float, count: Int, spacing: Float, baseY: Float, arcHeight: Float) {
            for (i in 0 until count) {
                val frac = if (count <= 1) 0.5f else i.toFloat() / (count - 1)
                val curve = 4f * frac * (1f - frac)
                coins.add(
                    SmileCoin(
                        id = coinId++,
                        x = startX + i * spacing,
                        y = baseY - curve * arcHeight,
                        bobPhase = i * 0.7f
                    )
                )
            }
        }

        // Quay opener
        addCoinArc(230f, 3, 48f, 520f, 42f)
        // First canal bridge arc
        addCoinArc(995f, 5, 58f, 475f, 55f)
        // Rooftop reward line
        addCoinArc(1625f, 5, 58f, 310f, 22f)
        // Stepping bridge hops
        addCoinArc(2155f, 3, 52f, 482f, 32f)
        addCoinArc(2445f, 3, 52f, 432f, 32f)
        // Express bike lane sprint
        addCoinArc(2785f, 5, 52f, 520f, 18f)
        // Sky jump over wide canal
        addCoinArc(3595f, 5, 52f, 362f, 28f)
        // Final leap to customer house flagpole
        addCoinArc(4560f, 4, 54f, 345f, 58f)

        val enemies = mutableListOf(
            Enemy(
                id = enemyId++,
                x = 1120f, y = 536f - 40f, width = 52f, height = 40f,
                type = EnemyType.SLOW_SNAIL,
                minX = 970f, maxX = 1230f, vx = -76f
            ),
            Enemy(
                id = enemyId++,
                x = 1780f, y = g - 40f, width = 52f, height = 40f,
                type = EnemyType.SLOW_SNAIL,
                minX = 1600f, maxX = 1960f, vx = -84f
            ),
            Enemy(
                id = enemyId++,
                x = 2290f, y = 415f, width = 58f, height = 42f,
                type = EnemyType.ONE_STAR_CLOUD,
                minX = 2120f, maxX = 2520f, vx = 98f, baseY = 415f
            ),
            Enemy(
                id = enemyId++,
                x = 3150f, y = g - 40f, width = 52f, height = 40f,
                type = EnemyType.SLOW_SNAIL,
                minX = 3060f, maxX = 3320f, vx = -88f
            ),
            Enemy(
                id = enemyId++,
                x = 3220f, y = 390f, width = 58f, height = 42f,
                type = EnemyType.ONE_STAR_CLOUD,
                minX = 3040f, maxX = 3440f, vx = -108f, baseY = 390f
            ),
            Enemy(
                id = enemyId++,
                x = 4070f, y = 468f, width = 58f, height = 42f,
                type = EnemyType.ONE_STAR_CLOUD,
                minX = 3960f, maxX = 4200f, vx = 92f, baseY = 468f
            )
        )

        return LevelData(
            levelNumber = 2,
            code = "1-2",
            title = "Grachtengordel Express",
            subtitle = "Alles voor een glimlach!",
            theme = LevelTheme.CANALS,
            worldWidth = 5355f,
            spawnX = 95f,
            spawnY = g - 70f,
            flagX = 4810f,
            flagGroundY = g,
            platforms = platforms,
            boxes = boxes,
            coins = coins,
            enemies = enemies
        )
    }
}
