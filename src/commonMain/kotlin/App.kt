import androidx.compose.foundation.Canvas
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
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
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.pointerInput // New import
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import game.*
import kotlinx.coroutines.delay
import kotlin.math.min

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun App() {
    val gameState by remember { mutableStateOf(GameState()) }
    val focusRequester = remember { FocusRequester() }
    val keysPressed = remember { mutableStateOf(emptySet<Key>()) }

    when (gameState.currentScreen) {
        Screen.MENU -> MenuScreen(gameState)
        Screen.GAME -> GameScreen(gameState, focusRequester, keysPressed)
        Screen.GAME_OVER -> GameOverScreen(gameState)
        Screen.STAGE_CLEAR -> StageClearScreen(gameState)
    }

    // Request focus to receive key events
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun GameScreen(gameState: GameState, focusRequester: FocusRequester, keysPressed: MutableState<Set<Key>>) {
    // TODO: Play game background music
    // Game loop
    LaunchedEffect(gameState.gameOver, gameState.stageClear) {
        if (gameState.gameOver) {
            gameState.currentScreen = Screen.GAME_OVER
            return@LaunchedEffect
        }
        if (gameState.stageClear) {
            gameState.currentScreen = Screen.STAGE_CLEAR
            return@LaunchedEffect
        }

        var fireCooldown = 0
        val fireRate = 10 // Fire every 10 frames

        // Start the first wave
        if (gameState.currentWaveIndex == -1) {
            gameState.currentWaveIndex = 0
            spawnWave(gameState)
        }

        while (true) {
            // --- Update game objects ---
            updatePlayerPosition(keysPressed.value, gameState.player)
            updateMissiles(gameState.missiles)
            updateBossMissiles(gameState.bossMissiles)
            gameState.boss?.let { updateBoss(it, gameState) }

            // --- Spawn new things ---
            fireCooldown = fireMissile(gameState, fireCooldown, fireRate)

            // --- Handle interactions ---
            checkCollisions(gameState)

            // --- Handle game state changes ---
            if (gameState.player.lives < 0) {
                gameState.gameOver = true
            }

            if (gameState.enemies.isEmpty() && !gameState.bossAppeared) {
                val currentStage = gameState.stages[gameState.currentStageIndex]
                if (gameState.currentWaveIndex < currentStage.waves.size - 1) {
                    gameState.currentWaveIndex++
                    spawnWave(gameState)
                } else {
                    currentStage.boss?.let { bossSpawn ->
                        gameState.boss = Boss(
                            position = bossSpawn.initialPosition,
                            health = bossSpawn.health
                        )
                        gameState.bossAppeared = true
                    } ?: run {
                        gameState.stageClear = true
                    }
                }
            }

            if (gameState.bossAppeared) {
                gameState.boss?.let { boss ->
                    if (boss.health <= 0) {
                        gameState.stageClear = true
                    }
                }
            }

            if (gameState.stageClear || gameState.gameOver) {
                break
            }

            delay(16) // ~60 FPS
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Canvas(modifier = Modifier
            .fillMaxSize()
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent {
                if (it.type == KeyEventType.KeyDown) {
                    keysPressed.value += it.key
                    // Handle special move activation
                    if (it.key == Key.Spacebar && gameState.player.specialGauge >= 100f) {
                        gameState.enemies.clear()
                        gameState.player.specialGauge = 0f
                    }
                } else if (it.type == KeyEventType.KeyUp) {
                    keysPressed.value -= it.key
                }
                true
            }
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    gameState.player.position = gameState.player.position.copy(
                        x = gameState.player.position.x + dragAmount.x,
                        y = gameState.player.position.y + dragAmount.y
                    )
                }
            }
        ) {
            drawPlayer(gameState.player)
            gameState.missiles.forEach { drawMissile(it) }
            gameState.enemies.forEach { drawEnemy(it) }
            gameState.items.forEach { drawItem(it) }
            gameState.boss?.let { drawBoss(it) }
            gameState.bossMissiles.forEach { drawBossMissile(it) }
        }
        // HUD
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Text("Stage: ${gameState.currentStageIndex + 1} - Wave: ${gameState.currentWaveIndex + 1}", color = Color.White, fontSize = 20.sp)
            Text("Lives: ${gameState.player.lives}", color = Color.White, fontSize = 20.sp)
            Text("Health: ${gameState.player.health}", color = Color.White, fontSize = 20.sp)
            Text("Special: ${gameState.player.specialGauge.toInt()}", color = Color.White, fontSize = 20.sp)
            if (gameState.bossAppeared) {
                gameState.boss?.let {
                    Text("Boss Health: ${it.health}", color = Color.White, fontSize = 20.sp)
                }
            }
        }
    }
}

@Composable
fun MenuScreen(gameState: GameState) {
    // TODO: Play menu background music
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("냥냥 슈터", fontSize = 50.sp, color = Color.White)
            Button(onClick = {
                resetGame(gameState)
                gameState.currentScreen = Screen.GAME
            }, modifier = Modifier.padding(top = 20.dp)) {
                Text("게임 시작", fontSize = 30.sp)
            }
        }
    }
}

@Composable
fun GameOverScreen(gameState: GameState) {
    // TODO: Play game over sound/music
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("게임 오버", fontSize = 50.sp, color = Color.White)
            Button(onClick = {
                resetGame(gameState)
                gameState.currentScreen = Screen.GAME
            }, modifier = Modifier.padding(top = 20.dp)) {
                Text("다시 시작", fontSize = 30.sp)
            }
        }
    }
}

@Composable
fun StageClearScreen(gameState: GameState) {
    // TODO: Play stage clear sound/music
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("스테이지 클리어!", fontSize = 50.sp, color = Color.White)
            Button(onClick = {
                // For now, just restart the current stage
                resetGame(gameState)
                gameState.currentScreen = Screen.GAME
            }, modifier = Modifier.padding(top = 20.dp)) {
                Text("다음 스테이지", fontSize = 30.sp)
            }
        }
    }
}

private fun resetGame(gameState: GameState) {
    // TODO: Stop all sounds and music, reset audio states
    gameState.player.lives = 3
    gameState.player.health = 100
    gameState.player.position = Offset(300f, 500f)
    gameState.player.powerLevel = 0
    gameState.player.specialGauge = 0f
    gameState.missiles.clear()
    gameState.enemies.clear()
    gameState.items.clear()
    gameState.bossMissiles.clear()
    gameState.boss = null
    gameState.currentStageIndex = 0
    gameState.currentWaveIndex = -1
    gameState.gameOver = false
    gameState.stageClear = false
    gameState.bossAppeared = false
}

private fun updatePlayerPosition(keysPressed: Set<Key>, player: Player) {
    val moveDelta = 10f
    if (Key.DirectionLeft in keysPressed) {
        player.position = player.position.copy(x = player.position.x - moveDelta)
    }
    if (Key.DirectionRight in keysPressed) {
        player.position = player.position.copy(x = player.position.x + moveDelta)
    }
    if (Key.DirectionUp in keysPressed) {
        player.position = player.position.copy(y = player.position.y - moveDelta)
    }
    if (Key.DirectionDown in keysPressed) {
        player.position = player.position.copy(y = player.position.y + moveDelta)
    }
}

private fun updateMissiles(missiles: MutableList<Missile>) {
    missiles.forEach { it.position = it.position.copy(y = it.position.y - 15f) }
    missiles.removeAll { it.position.y < 0 }
}

private fun updateBoss(boss: Boss, gameState: GameState) {
    // Movement
    val moveSpeed = 3f
    boss.position = boss.position.copy(x = boss.position.x + moveSpeed * boss.direction)

    // Assuming screen width is around 800 for now. A better approach would be to pass screen dimensions.
    if (boss.position.x <= 0 || boss.position.x >= 800 - boss.size) {
        boss.direction *= -1
    }

    // Firing
    if (boss.fireCooldown <= 0) {
        val missilePosition = boss.position.copy(x = boss.position.x + boss.size / 2 - 10f, y = boss.position.y + boss.size)
        gameState.bossMissiles.add(BossMissile(position = missilePosition))
        // TODO: Play boss missile sound effect
        boss.fireCooldown = 120 // Reset cooldown (e.g., every 2 seconds)
    } else {
        boss.fireCooldown--
    }
}

private fun updateBossMissiles(missiles: MutableList<BossMissile>) {
    missiles.forEach { it.position = it.position.copy(y = it.position.y + 8f) }
    missiles.removeAll { it.position.y > 800 } // Assuming screen height is 800
}

private fun fireMissile(gameState: GameState, currentCooldown: Int, fireRate: Int): Int {
    var newCooldown = currentCooldown
    if (newCooldown <= 0) {
        val player = gameState.player
        when (player.powerLevel) {
            0 -> { // Single missile
                val missilePosition = player.position.copy(x = player.position.x + player.size / 2 - 7.5f, y = player.position.y)
                gameState.missiles.add(Missile(position = missilePosition))
            }
            1 -> { // Double missile
                val missile1Pos = player.position.copy(x = player.position.x + player.size / 4 - 7.5f, y = player.position.y)
                val missile2Pos = player.position.copy(x = player.position.x + player.size * 3 / 4 - 7.5f, y = player.position.y)
                gameState.missiles.add(Missile(position = missile1Pos))
                gameState.missiles.add(Missile(position = missile2Pos))
            }
            else -> { // Triple missile (and max level)
                val missile1Pos = player.position.copy(x = player.position.x + player.size / 2 - 7.5f, y = player.position.y)
                val missile2Pos = player.position.copy(x = player.position.x, y = player.position.y)
                val missile3Pos = player.position.copy(x = player.position.x + player.size - 15f, y = player.position.y)
                gameState.missiles.add(Missile(position = missile1Pos))
                gameState.missiles.add(Missile(position = missile2Pos))
                gameState.missiles.add(Missile(position = missile3Pos))
            }
        }
        // TODO: Play player missile sound effect
        newCooldown = fireRate
    } else {
        newCooldown--
    }
    return newCooldown
}

private fun spawnWave(gameState: GameState) {
    val currentStageData = gameState.stages[gameState.currentStageIndex]
    val currentWaveData = currentStageData.waves[gameState.currentWaveIndex]
    currentWaveData.enemies.forEach { enemySpawn ->
        gameState.enemies.add(Enemy(position = enemySpawn.initialPosition))
    }
}

private fun checkCollisions(gameState: GameState) {
    val missilesToRemove = mutableListOf<Missile>()
    val enemiesToRemove = mutableListOf<Enemy>()
    val itemsToRemove = mutableListOf<Item>()
    val bossMissilesToRemove = mutableListOf<BossMissile>()

    // Missile-Enemy collision
    for (missile in gameState.missiles) {
        val missileRect = Rect(missile.position, androidx.compose.ui.geometry.Size(missile.size, missile.size))
        for (enemy in gameState.enemies) {
            val enemyRect = Rect(enemy.position, androidx.compose.ui.geometry.Size(enemy.size, enemy.size))
            if (missileRect.overlaps(enemyRect)) {
                missilesToRemove.add(missile)
                enemiesToRemove.add(enemy)
                // TODO: Play enemy hit sound effect
                // Charge special gauge
                gameState.player.specialGauge = min(100f, gameState.player.specialGauge + 5f)
                // Item spawn logic
                when (kotlin.random.Random.nextInt(0, 8)) { // Lower chance
                    0 -> gameState.items.add(Item(ItemType.CHEESE, enemy.position.copy()))
                    1 -> gameState.items.add(Item(ItemType.POWER_UP, enemy.position.copy(), color = Color.Cyan))
                }
            }
        }
    }

    // Missile-Boss collision
    gameState.boss?.let { boss ->
        val bossRect = Rect(boss.position, androidx.compose.ui.geometry.Size(boss.size, boss.size))
        for (missile in gameState.missiles) {
            if (missilesToRemove.contains(missile)) continue
            val missileRect = Rect(missile.position, androidx.compose.ui.geometry.Size(missile.size, missile.size))
            if (missileRect.overlaps(bossRect)) {
                missilesToRemove.add(missile)
                boss.health--
                // TODO: Play boss hit sound effect
            }
        }
    }

    val playerRect = Rect(gameState.player.position, androidx.compose.ui.geometry.Size(gameState.player.size, gameState.player.size))
    // Player-Enemy collision
    for (enemy in gameState.enemies) {
        if (enemiesToRemove.contains(enemy)) continue // Don't check against already defeated enemies
        val enemyRect = Rect(enemy.position, androidx.compose.ui.geometry.Size(enemy.size, enemy.size))
        if (playerRect.overlaps(enemyRect)) {
            enemiesToRemove.add(enemy)
            gameState.player.health -= 25 // Player takes damage
            // TODO: Play player hit sound effect
        }
    }

    // Player-BossMissile collision
    for (missile in gameState.bossMissiles) {
        val missileRect = Rect(missile.position, androidx.compose.ui.geometry.Size(missile.size, missile.size))
        if (playerRect.overlaps(missileRect)) {
            bossMissilesToRemove.add(missile)
            gameState.player.health -= 20 // Player takes damage from boss
            // TODO: Play player hit sound effect
        }
    }

    // Player-Item collision
    for (item in gameState.items) {
        val itemRect = Rect(item.position, androidx.compose.ui.geometry.Size(item.size, item.size))
        if (playerRect.overlaps(itemRect)) {
            itemsToRemove.add(item)
            // TODO: Play item pickup sound effect
            when (item.type) {
                ItemType.CHEESE -> gameState.player.lives++
                ItemType.POWER_UP -> if (gameState.player.powerLevel < 2) gameState.player.powerLevel++
            }
        }
    }

    if (gameState.player.health <= 0) {
        gameState.player.lives--
        if (gameState.player.lives >= 0) {
            gameState.player.health = 100
            gameState.player.position = Offset(300f, 500f) // Reset position
            gameState.player.powerLevel = 0 // Reset power level
            // TODO: Play player death sound effect
        }
    }

    gameState.missiles.removeAll(missilesToRemove)
    gameState.enemies.removeAll(enemiesToRemove)
    gameState.items.removeAll(itemsToRemove)
    gameState.bossMissiles.removeAll(bossMissilesToRemove)
}

private fun DrawScope.drawPlayer(player: Player) {
    // TODO: Replace with actual player image
    drawRect(
        color = Color.Blue, // Distinct color for player
        topLeft = player.position,
        size = androidx.compose.ui.geometry.Size(player.size, player.size)
    )
}

private fun DrawScope.drawMissile(missile: Missile) {
    // TODO: Replace with actual player missile image
    drawRect(
        color = Color.Cyan, // Distinct color for player missile
        topLeft = missile.position,
        size = androidx.compose.ui.geometry.Size(missile.size, missile.size)
    )
}

private fun DrawScope.drawEnemy(enemy: Enemy) {
    // TODO: Replace with actual enemy image
    drawRect(
        color = Color.Green, // Distinct color for enemy
        topLeft = enemy.position,
        size = androidx.compose.ui.geometry.Size(enemy.size, enemy.size)
    )
}

private fun DrawScope.drawBoss(boss: Boss) {
    // TODO: Replace with actual boss image
    drawRect(
        color = Color.Red, // Distinct color for boss
        topLeft = boss.position,
        size = androidx.compose.ui.geometry.Size(boss.size, boss.size)
    )
}

private fun DrawScope.drawBossMissile(missile: BossMissile) {
    // TODO: Replace with actual boss missile image
    drawRect(
        color = Color.Magenta, // Distinct color for boss missile
        topLeft = missile.position,
        size = androidx.compose.ui.geometry.Size(missile.size, missile.size)
    )
}

private fun DrawScope.drawItem(item: Item) {
    // TODO: Replace with actual item images
    val itemColor = when (item.type) {
        ItemType.CHEESE -> Color.Yellow
        ItemType.POWER_UP -> Color(0xFFFFA500) // Orange color
    }
    drawCircle(
        color = itemColor, // Distinct colors for items
        radius = item.size / 2,
        center = item.position.copy(x = item.position.x + item.size / 2, y = item.position.y + item.size / 2)
    )
}
