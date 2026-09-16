package com.codebuzz.app.bookcric.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codebuzz.app.bookcric.game.*
import com.codebuzz.app.bookcric.ui.theme.BookCricketTheme
import kotlinx.coroutines.launch
import kotlin.random.Random

@Composable
fun GameScreen(
    state: GameState,
    onFlip: () -> BallResult?,
    modifier: Modifier = Modifier
) {
    val currentInnings = if (state.phase == Phase.INNINGS_1) state.innings1 else state.innings2
    val batterName = if (currentInnings.batter == Player.ONE) state.config.playerOneName else state.config.playerTwoName
    
    val overs = currentInnings.ballsBowled / 6
    val balls = currentInnings.ballsBowled % 6
    val oversText = "$overs.$balls overs"

    // Animation States
    var isAnimating by remember { mutableStateOf(false) }
    var riffleBall by remember { mutableStateOf<BallResult?>(null) }
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    
    val resultScale = remember { Animatable(1f) }
    val resultShake = remember { Animatable(0f) }
    val pageRotation = remember { Animatable(0f) }

    val displayedBall = if (isAnimating) riffleBall else state.lastBall

    LaunchedEffect(state.lastBall) {
        state.lastBall?.let { ball ->
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

    val handleFlip = {
        scope.launch {
            isAnimating = true

            // Riffle through a handful of decoy pages first. Each turn takes a
            // little longer than the last, like a real stack of pages losing
            // momentum as you let them go, before settling on the real result.
            val decoyTurns = 6
            repeat(decoyTurns) { i ->
                val turnDuration = (90 + i * 26).coerceAtMost(210)
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                pageRotation.animateTo(90f, tween(turnDuration, easing = FastOutSlowInEasing))
                riffleBall = GameLogic.flip(Random, state.config.bookPages)
                pageRotation.snapTo(-90f)
                pageRotation.animateTo(0f, tween(turnDuration, easing = FastOutSlowInEasing))
            }

            // Final, slowest turn lands on the actual outcome.
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            pageRotation.animateTo(90f, tween(280, easing = FastOutSlowInEasing))
            riffleBall = onFlip()
            pageRotation.snapTo(-90f)
            pageRotation.animateTo(0f, tween(320, easing = FastOutSlowInEasing))

            isAnimating = false
        }
        Unit
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
                modifier = Modifier.padding(16.dp),
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

        // Last Ball Result
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Last Ball",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.outline
            )
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .size(width = 140.dp, height = 168.dp)
                    .graphicsLayer {
                        scaleX = resultScale.value
                        scaleY = resultScale.value
                        translationX = resultShake.value
                        rotationY = pageRotation.value
                        cameraDistance = 12f * density
                        // Pivot near the spine (left edge) so the page turns like a real
                        // book page instead of spinning around its own center.
                        transformOrigin = TransformOrigin(0.08f, 0.5f)
                    }
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        if (displayedBall?.isOut == true) MaterialTheme.colorScheme.errorContainer
                        else MaterialTheme.colorScheme.secondaryContainer
                    ),
                contentAlignment = Alignment.Center
            ) {
                // Book spine, near the left edge, to sell the "page" shape.
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 12.dp)
                        .fillMaxHeight(0.8f)
                        .width(1.5.dp)
                        .background(
                            (if (displayedBall?.isOut == true) MaterialTheme.colorScheme.onErrorContainer
                            else MaterialTheme.colorScheme.onSecondaryContainer).copy(alpha = 0.2f)
                        )
                )
                displayedBall?.let { ball ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (ball.isOut) "OUT" else ball.runs.toString(),
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (ball.isOut) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = "Page ${ball.page}",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                } ?: Text("—", style = MaterialTheme.typography.displaySmall)
            }
        }

        // Flip Button
        Button(
            onClick = handleFlip,
            enabled = !isAnimating,
            modifier = Modifier
                .size(160.dp)
                .clip(CircleShape),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Text(
                text = if (isAnimating) "..." else "FLIP",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary
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
            onFlip = { null }
        )
    }
}
