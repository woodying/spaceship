import androidx.compose.foundation.Canvas
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import game.Enemy
import game.Missile
import game.Player
import kotlinx.coroutines.delay

// Game state holder
class GameState {
    val player = Player(position = Offset(300f, 500f))
    val missiles = mutableStateListOf<Missile>()
    val enemies = mutableStateListOf<Enemy>()
    var gameOver by mutableStateOf(false)
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun App() {
    val gameState by remember { mutableStateOf(GameState()) }
    val focusRequester = remember { FocusRequester() }
    val keysPressed = remember { mutableStateOf(emptySet<Key>()) }

    // Game loop
    LaunchedEffect(gameState.gameOver) {
        if (gameState.gameOver) return@LaunchedEffect

        var fireCooldown = 0
        val fireRate = 10 // Fire every 10 frames

        // Spawn a test enemy
        if (gameState.enemies.isEmpty()) {
            gameState.enemies.add(Enemy(position = Offset(300f, 100f)))
        }

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

            // Check for collisions
            checkCollisions(gameState)

            if (gameState.player.lives < 0) {
                gameState.gameOver = true
                break
            }

            delay(16) // ~60 FPS
        }
    }

    if (gameState.gameOver) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Game Over", fontSize = 50.sp)
        }
    } else {
        Box(modifier = Modifier.fillMaxSize()) {
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

                // Draw enemies
                gameState.enemies.forEach { enemy ->
                    drawRect(
                        color = enemy.color,
                        topLeft = enemy.position,
                        size = androidx.compose.ui.geometry.Size(enemy.size, enemy.size)
                    )
                }
            }
            // HUD
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                Text("Lives: ${gameState.player.lives}", color = Color.White, fontSize = 20.sp)
                Text("Health: ${gameState.player.health}", color = Color.White, fontSize = 20.sp)
            }
        }
    }


    // Request focus to receive key events
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}

fun checkCollisions(gameState: GameState) {
    val missilesToRemove = mutableListOf<Missile>()
    val enemiesToRemove = mutableListOf<Enemy>()

    // Missile-Enemy collision
    for (missile in gameState.missiles) {
        val missileRect = Rect(missile.position, androidx.compose.ui.geometry.Size(missile.size, missile.size))
        for (enemy in gameState.enemies) {
            val enemyRect = Rect(enemy.position, androidx.compose.ui.geometry.Size(enemy.size, enemy.size))
            if (missileRect.overlaps(enemyRect)) {
                missilesToRemove.add(missile)
                enemiesToRemove.add(enemy)
            }
        }
    }

    // Player-Enemy collision
    val playerRect = Rect(gameState.player.position, androidx.compose.ui.geometry.Size(gameState.player.size, gameState.player.size))
    for (enemy in gameState.enemies) {
        val enemyRect = Rect(enemy.position, androidx.compose.ui.geometry.Size(enemy.size, enemy.size))
        if (playerRect.overlaps(enemyRect)) {
            enemiesToRemove.add(enemy)
            gameState.player.health -= 25 // Player takes damage
        }
    }

    if (gameState.player.health <= 0) {
        gameState.player.lives--
        if (gameState.player.lives >= 0) {
            gameState.player.health = 100
            gameState.player.position = Offset(300f, 500f) // Reset position
        }
    }

    gameState.missiles.removeAll(missilesToRemove)
    gameState.enemies.removeAll(enemiesToRemove)
}
