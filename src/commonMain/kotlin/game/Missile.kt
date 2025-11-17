package game

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color

data class Missile(
    var position: Offset,
    val size: Float = 15f,
    val color: Color = Color.Yellow
)
