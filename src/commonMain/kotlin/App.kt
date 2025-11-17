import androidx.compose.foundation.Canvas
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import game.Missile
import game.Player
import kotlinx.coroutines.delay

// Game state holder
class GameState {
    val player = Player(position = Offset(300f, 500f))
    val missiles = mutableStateListOf<Missile>()
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun App() {
    val gameState by remember { mutableStateOf(GameState()) }
    val focusRequester = remember { FocusRequester() }
    val keysPressed = remember { mutableStateOf(emptySet<Key>()) }

    // Game loop
    LaunchedEffect(Unit) {
        var fireCooldown = 0
        val fireRate = 10 // Fire every 10 frames

        while (true) {
            // Update player position
            val moveDelta = 10f
            if (Key.DirectionLeft in keysPressed.value) {
                gameState.player.position = gameState.player.position.copy(x = gameState.player.position.x - moveDelta)
            }
            if (Key.DirectionRight in keysPressed.value) {
                gameState.player.position = gameState.player.position.copy(x = gameState.player.position.x + moveDelta)
            }
            if (Key.DirectionUp in keysPressed.value) {
                gameState.player.position = gameState.player.position.copy(y = gameState.player.position.y - moveDelta)
            }
            if (Key.DirectionDown in keysPressed.value) {
                gameState.player.position = gameState.player.position.copy(y = gameState.player.position.y + moveDelta)
            }

            // Update missiles
            gameState.missiles.forEach { it.position = it.position.copy(y = it.position.y - 15f) }
            gameState.missiles.removeAll { it.position.y < 0 }

            // Fire missiles
            if (fireCooldown <= 0) {
                val missilePosition = gameState.player.position.copy(
                    x = gameState.player.position.x + gameState.player.size / 2 - 7.5f, // Center the missile
                    y = gameState.player.position.y
                )
                gameState.missiles.add(Missile(position = missilePosition))
                fireCooldown = fireRate
            } else {
                fireCooldown--
            }

            delay(16) // ~60 FPS
        }
    }

    Canvas(modifier = Modifier
        .fillMaxSize()
        .focusRequester(focusRequester)
        .focusable()
        .onKeyEvent {
            if (it.type == KeyEventType.KeyDown) {
                keysPressed.value += it.key
            } else if (it.type == KeyEventType.KeyUp) {
                keysPressed.value -= it.key
            }
            true
        }
    ) {
        // Draw player
        drawRect(
            color = gameState.player.color,
            topLeft = gameState.player.position,
            size = androidx.compose.ui.geometry.Size(gameState.player.size, gameState.player.size)
        )

        // Draw missiles
        gameState.missiles.forEach { missile ->
            drawRect(
                color = missile.color,
                topLeft = missile.position,
                size = androidx.compose.ui.geometry.Size(missile.size, missile.size)
            )
        }
    }

    // Request focus to receive key events
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}
