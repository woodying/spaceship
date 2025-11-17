package game

import androidx.compose.ui.geometry.Offset

data class EnemySpawn(
    val initialPosition: Offset
    // We can add enemy type, movement pattern etc. later
)

data class Wave(
    val enemies: List<EnemySpawn>
)

data class Stage(
    val waves: List<Wave>
)

val stage1 = Stage(
    waves = listOf(
        Wave(
            enemies = listOf(
                EnemySpawn(initialPosition = Offset(200f, 100f)),
                EnemySpawn(initialPosition = Offset(400f, 100f))
            )
        ),
        Wave(
            enemies = listOf(
                EnemySpawn(initialPosition = Offset(100f, 100f)),
                EnemySpawn(initialPosition = Offset(300f, 100f)),
                EnemySpawn(initialPosition = Offset(500f, 100f))
            )
        ),
        Wave(
            enemies = listOf(
                EnemySpawn(initialPosition = Offset(200f, 50f)),
                EnemySpawn(initialPosition = Offset(400f, 50f)),
                EnemySpawn(initialPosition = Offset(200f, 150f)),
                EnemySpawn(initialPosition = Offset(400f, 150f))
            )
        )
    )
)
