package game

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color

enum class ItemType {
    CHEESE,
    POWER_UP
}

data class Item(
    val type: ItemType,
    var position: Offset,
    val size: Float = 25f,
    val color: Color = Color.Yellow // Default color, can be changed by type
)
