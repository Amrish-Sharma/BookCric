package com.codebuzz.app.bookcric.ui.screens

import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codebuzz.app.bookcric.game.BallResult
import com.codebuzz.app.bookcric.game.GameState
import com.codebuzz.app.bookcric.game.InningsState
import com.codebuzz.app.bookcric.game.MatchConfig
import com.codebuzz.app.bookcric.game.Phase
import com.codebuzz.app.bookcric.game.Player
import com.codebuzz.app.bookcric.ui.components.BallChip
import com.codebuzz.app.bookcric.ui.components.StadiumBackground
import com.codebuzz.app.bookcric.ui.theme.BookCricketTheme
import com.codebuzz.app.bookcric.ui.theme.Floodlight
import com.codebuzz.app.bookcric.ui.theme.Pavilion
import kotlinx.coroutines.delay

/** How long the first-innings score takes to count up from zero. */
private const val COUNT_UP_MILLIS = 900

/** The recap shows at most this many balls — the last ones of a long innings. */
private const val RECAP_BALLS = 24

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun InningsBreakScreen(
    state: GameState,
    onStartChase: () -> Unit,
    modifier: Modifier = Modifier
) {
    val innings = state.innings1
    val batter1Name = state.config.nameOf(innings.batter)
    val batter2Name = state.config.nameOf(state.innings2.batter)

    // Count the score up, then bring in what the chaser needs.
    var revealed by remember { mutableStateOf(false) }
    var showTarget by remember { mutableStateOf(false) }
    val runs by animateIntAsState(
        targetValue = if (revealed) innings.runs else 0,
        animationSpec = tween(durationMillis = COUNT_UP_MILLIS),
        label = "runs"
    )
    LaunchedEffect(Unit) {
        revealed = true
        delay(COUNT_UP_MILLIS.toLong())
        showTarget = true
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "INNINGS BREAK",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.secondary,
            letterSpacing = 3.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = batter1Name,
            style = MaterialTheme.typography.titleLarge
        )
        Text(
            text = runs.toString(),
            style = MaterialTheme.typography.displayLarge.copy(fontSize = 88.sp, lineHeight = 92.sp),
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = buildString {
                append("${innings.ballsBowled} ball${if (innings.ballsBowled == 1) "" else "s"}")
                append(if (innings.isOut) " · out" else " · not out")
            },
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (innings.balls.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                innings.balls.takeLast(RECAP_BALLS).forEach { BallChip(it, size = 28.dp) }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Reserve the card's space from the start, so its arrival doesn't shove the score up.
        Box(Modifier.fillMaxWidth().heightIn(min = 132.dp), contentAlignment = Alignment.Center) {
            // Fully qualified: the bare name resolves to the Column overload, which a Box can't call.
            androidx.compose.animation.AnimatedVisibility(
                visible = showTarget,
                enter = fadeIn() + slideInVertically { it / 3 }
            ) {
                TargetCard(state = state, chaserName = batter2Name)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onStartChase,
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text(
                text = "START CHASE",
                style = MaterialTheme.typography.titleMedium,
                letterSpacing = 1.5.sp
            )
        }
    }
}

@Composable
private fun TargetCard(state: GameState, chaserName: String) {
    val balls = state.config.oversLimit?.let { it * 6 }
    Surface(
        color = Pavilion.copy(alpha = 0.9f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, Floodlight.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "TARGET",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "${state.target}",
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "$chaserName needs ${state.target} to win" + (balls?.let { " from $it balls" } ?: ""),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Preview(device = "spec:width=411dp,height=891dp")
@Composable
fun InningsBreakScreenPreview() {
    val balls = listOf(4, 6, 2, 8, 4, 0).map { BallResult(page = 100 + it, runs = it, isOut = it == 0) }
    BookCricketTheme {
        StadiumBackground {
            InningsBreakScreen(
                state = GameState(
                    config = MatchConfig("Player 1", "Player 2", Player.ONE, 2),
                    phase = Phase.INNINGS_BREAK,
                    innings1 = InningsState(Player.ONE, 24, 6, true, balls),
                    innings2 = InningsState(Player.TWO),
                    target = 25
                ),
                onStartChase = {}
            )
        }
    }
}
