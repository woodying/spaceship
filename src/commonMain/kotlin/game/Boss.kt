package game

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color

data class Boss(
    var position: Offset,
    var health: Int,
    val size: Float = 150f,
    val color: Color = Color.Magenta, // Placeholder color for the boss
    var fireCooldown: Int = 100,
    var direction: Int = 1 // 1 for right, -1 for left
)
