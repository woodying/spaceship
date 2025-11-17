package game

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color

data class Enemy(
    var position: Offset,
    val size: Float = 50f,
    val color: Color = Color.Red // Placeholder color for the mouse enemy
)
