package game

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color

data class BossMissile(
    var position: Offset,
    val size: Float = 20f,
    val color: Color = Color.Yellow
)
