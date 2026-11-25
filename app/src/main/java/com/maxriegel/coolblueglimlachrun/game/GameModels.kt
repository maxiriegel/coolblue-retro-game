package com.maxriegel.coolblueglimlachrun.game

enum class CharacterType(
    val displayName: String,
    val badgeTitle: String,
    val partnerCheerText: String
) {
    MAX(
        displayName = "Max",
        badgeTitle = "Express Bezorger",
        partnerCheerText = "Lekker bezig, Remi!"
    ),
    REMI(
        displayName = "Remi",
        badgeTitle = "Glimlach Kampioen",
        partnerCheerText = "Top gedaan, Max!"
    );

    fun partner(): CharacterType = if (this == MAX) REMI else MAX
}

enum class GameScreen {
    TITLE,
    PLAYING,
    LEVEL_CLEAR,
    GAME_OVER,
    VICTORY
}

enum class LevelTheme {
    WAREHOUSE,
    CANALS
}

enum class PlatformType {
    GROUND,
    PALLET,
    PARCEL_STACK,
    CONVEYOR_RIGHT,
    CONVEYOR_LEFT,
    BRICK_BRIDGE,
    SPRING_PAD,
    ROOFTOP
}

data class Platform(
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val type: PlatformType,
    var springAnimTimer: Float = 0f
)

enum class BoxReward {
    SMILE_BURST,
    BAKFIETS_SHIELD
}

data class QuestionBox(
    val id: Int,
    val x: Float,
    val y: Float,
    val size: Float = 54f,
    val reward: BoxReward = BoxReward.SMILE_BURST,
    var isHit: Boolean = false,
    var bumpOffset: Float = 0f
)

data class SmileCoin(
    val id: Int,
    val x: Float,
    val y: Float,
    val radius: Float = 18f,
    var collected: Boolean = false,
    val bobPhase: Float = 0f
)

enum class EnemyType {
    SLOW_SNAIL,
    ONE_STAR_CLOUD
}

data class Enemy(
    val id: Int,
    var x: Float,
    var y: Float,
    val width: Float,
    val height: Float,
    val type: EnemyType,
    val minX: Float,
    val maxX: Float,
    var vx: Float,
    val baseY: Float = y,
    var alive: Boolean = true,
    var squashedTimer: Float = 0f,
    var animTime: Float = 0f
)

data class PowerUpItem(
    var x: Float,
    var y: Float,
    val width: Float = 48f,
    val height: Float = 38f,
    var vx: Float = 145f,
    var vy: Float = -280f,
    var collected: Boolean = false
)

data class Particle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var life: Float,
    val maxLife: Float,
    val size: Float,
    val colorArgb: Int,
    val isSmile: Boolean = false
)

data class FloatingText(
    var x: Float,
    var y: Float,
    val text: String,
    val colorArgb: Int,
    var life: Float,
    val maxLife: Float = 1.0f
)

data class ConfettiPiece(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var rotation: Float,
    val vRot: Float,
    val width: Float,
    val height: Float,
    val colorArgb: Int
)

data class LevelData(
    val levelNumber: Int,
    val code: String,
    val title: String,
    val subtitle: String,
    val theme: LevelTheme,
    val worldWidth: Float,
    val spawnX: Float,
    val spawnY: Float,
    val flagX: Float,
    val flagGroundY: Float,
    val platforms: MutableList<Platform>,
    val boxes: MutableList<QuestionBox>,
    val coins: MutableList<SmileCoin>,
    val enemies: MutableList<Enemy>
)

data class PlayerState(
    var x: Float = 100f,
    var y: Float = 500f,
    val width: Float = 44f,
    val height: Float = 62f,
    var vx: Float = 0f,
    var vy: Float = 0f,
    var facingRight: Boolean = true,
    var onGround: Boolean = false,
    var coyoteTimer: Float = 0f,
    var jumpBufferTimer: Float = 0f,
    var hasBakfietsShield: Boolean = false,
    var invulnTimer: Float = 0f,
    var animTime: Float = 0f,
    var lastSafeX: Float = 100f,
    var lastSafeY: Float = 500f
)

data class InputState(
    val left: Boolean = false,
    val right: Boolean = false,
    val jumpHeld: Boolean = false,
    val jumpJustPressed: Boolean = false
)

data class ScoreEntry(
    val character: CharacterType,
    val score: Int,
    val smiles: Int,
    val levelReached: Int,
    val timeSeconds: Int,
    val timestampMs: Long
)

object GameConfig {
    const val VIEW_WIDTH = 1280f
    const val VIEW_HEIGHT = 720f
    const val GROUND_Y = 604f
    const val GRAVITY = 2150f
    const val MAX_FALL_SPEED = 1100f
    const val BASE_RUN_SPEED = 415f
    const val SHIELD_RUN_SPEED = 495f
    const val GROUND_ACCEL = 2800f
    const val AIR_ACCEL = 1900f
    const val GROUND_FRICTION = 2600f
    const val AIR_FRICTION = 550f
    const val JUMP_VELOCITY = -790f
    const val SPRING_VELOCITY = -1140f
    const val STOMP_BOUNCE_VELOCITY = -620f
    const val STOMP_HIGH_BOUNCE_VELOCITY = -820f
    const val CONVEYOR_PUSH_SPEED = 195f
    const val COYOTE_TIME = 0.12f
    const val JUMP_BUFFER_TIME = 0.13f
    const val STARTING_HEARTS = 3
    const val LEVEL_TIME_LIMIT = 120f
    const val TOTAL_LEVELS = 2
}
