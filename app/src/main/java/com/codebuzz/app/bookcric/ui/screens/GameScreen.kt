package com.codebuzz.app.bookcric.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codebuzz.app.bookcric.game.*
import com.codebuzz.app.bookcric.ui.theme.BookCricketTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

@Composable
fun GameScreen(
    state: GameState,
    onFlip: () -> Unit,
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
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            
            val riffleDuration = 450L
            val startTime = System.currentTimeMillis()
            while (System.currentTimeMillis() - startTime < riffleDuration) {
                riffleBall = GameLogic.flip(Random, state.config.bookPages)
                delay(60)
            }
            
            onFlip()
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
                    .size(120.dp)
                    .graphicsLayer {
                        scaleX = resultScale.value
                        scaleY = resultScale.value
                        translationX = resultShake.value
                    }
                    .clip(CircleShape)
                    .background(
                        if (displayedBall?.isOut == true) MaterialTheme.colorScheme.errorContainer
                        else MaterialTheme.colorScheme.secondaryContainer
                    ),
                contentAlignment = Alignment.Center
            ) {
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
            onFlip = {}
        )
    }
}
