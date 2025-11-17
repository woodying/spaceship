package game

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset

class GameState {
    val player = Player(position = Offset(300f, 500f))
    val missiles = mutableStateListOf<Missile>()
    val enemies = mutableStateListOf<Enemy>()

    val stages = listOf(stage1) // Add more stages later
    var currentStageIndex by mutableStateOf(0)
    var currentWaveIndex by mutableStateOf(-1) // -1 means stage has not started

    var gameOver by mutableStateOf(false)
    var stageClear by mutableStateOf(false)
}
