package com.codebuzz.app.bookcric.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.codebuzz.app.bookcric.game.*
import com.codebuzz.app.bookcric.ui.components.FlipBook
import com.codebuzz.app.bookcric.ui.components.PageFace
import com.codebuzz.app.bookcric.ui.components.rememberBookState
import com.codebuzz.app.bookcric.ui.theme.BookCricketTheme
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

/** Pause before the computer flips, so its turn reads as a deliberate action. */
private const val COMPUTER_THINK_MILLIS = 900L

/** How long the landing page stays on screen before an innings-ending ball moves the match on. */
private const val INNINGS_END_HOLD_MILLIS = 1400L

@Composable
fun GameScreen(
    state: GameState,
    onDrawBall: () -> BallResult?,
    onCommitBall: (BallResult) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentInnings = if (state.phase == Phase.INNINGS_1) state.innings1 else state.innings2
    val batterName = if (currentInnings.batter == Player.ONE) state.config.playerOneName else state.config.playerTwoName
    val computerBatting = GameLogic.isComputerTurn(state)

    val overs = currentInnings.ballsBowled / 6
    val balls = currentInnings.ballsBowled % 6
    val oversText = "$overs.$balls overs"

    // Animation States
    var isAnimating by remember { mutableStateOf(false) }
    var shownBall by remember { mutableStateOf(state.lastBall) }
    val latestState by rememberUpdatedState(state)
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    val resultScale = remember { Animatable(1f) }
    val resultShake = remember { Animatable(0f) }
    val book = rememberBookState {
        state.lastBall?.let { PageFace(it.page, highlight = true, isOut = it.isOut) }
            ?: PageFace(GameLogic.drawEvenPage(Random, state.config.bookPages))
    }

    LaunchedEffect(shownBall) {
        shownBall?.let { ball ->
            // Trigger animations when the result settles
            if (ball.isOut) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                resultShake.animateTo(
                    targetValue = 10f,
                    animationSpec = repeatable(
                        iterations = 4,
                        animation = tween(durationMillis = 50, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    )
                )
                resultShake.animateTo(0f)
            } else if (ball.runs > 0) {
                resultScale.snapTo(0.8f)
                resultScale.animateTo(
                    targetValue = 1.2f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                )
                resultScale.animateTo(1f)
            }
        }
    }

    // Riffle through the book, land on the drawn page, then commit the ball.
    suspend fun playTurn() {
        if (isAnimating) return
        val ball = onDrawBall() ?: return
        isAnimating = true
        try {
            // Thumb forward through a few pages. Sheets overlap in flight, and each one is let
            // go a little later and falls a little slower, like a stack losing momentum.
            val decoyPages = List(6) { GameLogic.drawEvenPage(Random, latestState.config.bookPages) }.sorted()
            coroutineScope {
                decoyPages.forEachIndexed { i, page ->
                    launch { book.turnTo(PageFace(page), durationMillis = 380 + i * 45) }
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    delay(70L + i * 20L)
                }
                delay(90)
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                book.turnTo(PageFace(ball.page, highlight = true, isOut = ball.isOut), durationMillis = 720)
            }
            shownBall = ball

            // Let an innings-ending ball sink in before the screen moves on.
            val current = latestState
            if (GameLogic.applyBall(current, ball).phase != current.phase) {
                delay(INNINGS_END_HOLD_MILLIS)
            }
            onCommitBall(ball)
        } finally {
            isAnimating = false
        }
    }

    // The computer flips on its own, once per ball.
    LaunchedEffect(computerBatting, currentInnings.ballsBowled) {
        if (computerBatting) {
            delay(COMPUTER_THINK_MILLIS)
            playTurn()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Scoreboard
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = batterName,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "${currentInnings.runs} / ${if (currentInnings.isOut) 1 else 0}",
                    style = MaterialTheme.typography.displayMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = oversText,
                    style = MaterialTheme.typography.bodyLarge
                )

                if (state.phase == Phase.INNINGS_2 && state.target != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Target: ${state.target}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }

        // The book, and the result of the page it landed on.
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            FlipBook(state = book, modifier = Modifier.fillMaxWidth())

            val ball = shownBall
            Surface(
                shape = RoundedCornerShape(50),
                color = when {
                    ball == null -> MaterialTheme.colorScheme.surfaceVariant
                    ball.isOut -> MaterialTheme.colorScheme.errorContainer
                    else -> MaterialTheme.colorScheme.secondaryContainer
                },
                modifier = Modifier.graphicsLayer {
                    scaleX = resultScale.value
                    scaleY = resultScale.value
                    translationX = resultShake.value
                }
            ) {
                Text(
                    text = when {
                        ball == null && computerBatting -> "$batterName is about to bat"
                        ball == null -> "Flip the book to bat"
                        ball.isOut -> "OUT! · Page ${ball.page}"
                        else -> "${ball.runs} run${if (ball.runs == 1) "" else "s"} · Page ${ball.page}"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        ball == null -> MaterialTheme.colorScheme.onSurfaceVariant
                        ball.isOut -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.onSecondaryContainer
                    },
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
                )
            }
        }

        // Flip Button — disabled while the computer bats; it flips on its own.
        Button(
            onClick = { scope.launch { playTurn() } },
            enabled = !isAnimating && !computerBatting,
            modifier = Modifier
                .size(128.dp)
                .clip(CircleShape),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Text(
                text = when {
                    computerBatting -> "CPU"
                    isAnimating -> "..."
                    else -> "FLIP"
                },
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Preview(showBackground = true, device = "spec:width=411dp,height=891dp")
@Composable
fun GameScreenPreview() {
    BookCricketTheme {
        GameScreen(
            state = GameState(
                config = MatchConfig("Player 1", "Player 2", Player.ONE, 1),
                phase = Phase.INNINGS_1,
                innings1 = InningsState(Player.ONE, 12, 4),
                innings2 = InningsState(Player.TWO),
                lastBall = BallResult(124, 4, false)
            ),
            onDrawBall = { null },
            onCommitBall = {}
        )
    }
}
