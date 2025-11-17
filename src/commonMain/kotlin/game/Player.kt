package game

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color

data class Player(
    var position: Offset,
    val size: Float = 50f,
    val color: Color = Color.Blue // Placeholder color for the cat airplane
)
