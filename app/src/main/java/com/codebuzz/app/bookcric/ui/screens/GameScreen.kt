package com.codebuzz.app.bookcric.ui.screens

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codebuzz.app.bookcric.audio.Sfx
import com.codebuzz.app.bookcric.game.*
import com.codebuzz.app.bookcric.ui.components.BallChip
import com.codebuzz.app.bookcric.ui.components.Confetti
import com.codebuzz.app.bookcric.ui.components.ConfettiState
import com.codebuzz.app.bookcric.ui.components.FlipBook
import com.codebuzz.app.bookcric.ui.components.PageFace
import com.codebuzz.app.bookcric.ui.components.StadiumBackground
import com.codebuzz.app.bookcric.ui.components.ballColor
import com.codebuzz.app.bookcric.ui.components.rememberBookState
import com.codebuzz.app.bookcric.ui.theme.*
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.random.Random

/** Pause before the computer flips, so its turn reads as a deliberate action. */
private const val COMPUTER_THINK_MILLIS = 900L

/** How long the landing page stays on screen before an innings-ending ball moves the match on. */
private const val INNINGS_END_HOLD_MILLIS = 1400L

/** How long a run stamp sits on the book before fading. A wicket's stamp stays. */
private const val STAMP_HOLD_MILLIS = 650L

/** Mean of the five equally likely outcomes 0, 2, 4, 6, 8: a chase needing more is falling behind. */
private const val AVERAGE_RUNS_PER_BALL = 4

/** Every this-many runs is a milestone worth a cheer. */
private const val MILESTONE_STEP = 50

/** A ball whose page has just landed. [serial] tells apart two identical balls in a row. */
private data class Landing(val serial: Int, val ball: BallResult, val milestone: String?)

@Composable
fun GameScreen(
    state: GameState,
    pendingBall: BallResult?,
    onDrawBall: () -> Unit,
    onCommitBall: (BallResult) -> Unit,
    modifier: Modifier = Modifier,
    onSound: (Sfx) -> Unit = {}
) {
    val currentInnings = if (state.phase == Phase.INNINGS_1) state.innings1 else state.innings2
    val batterName = state.config.nameOf(currentInnings.batter)
    val computerBatting = GameLogic.isComputerTurn(state)
    val remoteBatting = GameLogic.isRemoteTurn(state)

    // Animation States
    // A ball is in flight from the moment it's drawn until it's committed.
    val isAnimating = pendingBall != null
    var shownBall by remember { mutableStateOf(state.lastBall) }
    var landing by remember { mutableStateOf<Landing?>(null) }
    val latestState by rememberUpdatedState(state)
    val latestOnSound by rememberUpdatedState(onSound)
    val haptic = LocalHapticFeedback.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()

    val confetti = remember { ConfettiState() }
    val bookShake = remember { Animatable(0f) }
    val wicketFlash = remember { Animatable(0f) }
    val book = rememberBookState {
        state.lastBall?.let { PageFace(it.page, highlight = true, isOut = it.isOut) }
            ?: PageFace(GameLogic.drawEvenPage(Random, state.config.bookPages))
    }

    // React to the page landing: the crowd, the confetti, the wicket.
    LaunchedEffect(landing?.serial) {
        val landed = landing ?: return@LaunchedEffect
        if (landed.ball.isOut) {
            view.performHapticFeedback(HapticFeedbackConstants.REJECT)
            latestOnSound(Sfx.WICKET)
            launch {
                wicketFlash.snapTo(0.35f)
                wicketFlash.animateTo(0f, tween(durationMillis = 450))
            }
            bookShake.animateTo(
                targetValue = 10f,
                animationSpec = repeatable(
                    iterations = 4,
                    animation = tween(durationMillis = 50, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                )
            )
            bookShake.animateTo(0f)
        } else {
            latestOnSound(Sfx.BAT)
            if (landed.ball.runs >= 6 || landed.milestone != null) {
                view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                latestOnSound(Sfx.CHEER)
                // Outlives this effect, so the next ball landing doesn't freeze it mid-air.
                scope.launch { confetti.fire() }
            } else if (landed.ball.runs >= 4) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            }
        }
    }

    // Riffle through the book, land on [ball]'s page, then commit it.
    suspend fun playTurn(ball: BallResult) {
        // Thumb forward through a few pages. Sheets overlap in flight, and each one is let
        // go a little later and falls a little slower, like a stack losing momentum.
        val decoyPages = List(6) { GameLogic.drawEvenPage(Random, latestState.config.bookPages) }.sorted()
        coroutineScope {
            decoyPages.forEachIndexed { i, page ->
                launch { book.turnTo(PageFace(page), durationMillis = 380 + i * 45) }
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                latestOnSound(Sfx.PAGE)
                delay(70L + i * 20L)
            }
            delay(90)
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            latestOnSound(Sfx.PAGE)
            book.turnTo(PageFace(ball.page, highlight = true, isOut = ball.isOut), durationMillis = 720)
        }
        shownBall = ball

        val current = latestState
        val runsBefore = GameLogic.currentInnings(current)?.runs ?: 0
        landing = Landing(
            serial = (landing?.serial ?: 0) + 1,
            ball = ball,
            milestone = milestoneReached(runsBefore, runsBefore + ball.runs)
        )

        // Let an innings-ending ball sink in before the screen moves on.
        if (GameLogic.applyBall(current, ball).phase != current.phase) {
            delay(INNINGS_END_HOLD_MILLIS)
        }
        onCommitBall(ball)
    }

    // Animate whichever ball is pending: ours, the computer's or the online opponent's. Keyed on
    // the ball, so if the screen is rebuilt mid-flip the flip replays instead of being dropped.
    LaunchedEffect(pendingBall) {
        pendingBall?.let { playTurn(it) }
    }

    // The computer flips on its own, once per ball.
    LaunchedEffect(computerBatting, currentInnings.ballsBowled) {
        if (computerBatting) {
            delay(COMPUTER_THINK_MILLIS)
            onDrawBall()
        }
    }

    Box(modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Scoreboard(state = state, innings = currentInnings, batterName = batterName)

            OverTracker(innings = currentInnings)

            // The book, and the result of the page it landed on.
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    FlipBook(
                        state = book,
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer { translationX = bookShake.value }
                            .shadow(
                                elevation = 28.dp,
                                shape = RoundedCornerShape(10.dp),
                                ambientColor = Floodlight,
                                spotColor = Floodlight
                            )
                    )
                    ResultStamp(landing)
                }

                val ball = shownBall
                Text(
                    text = when {
                        ball == null && computerBatting -> "$batterName is about to bat"
                        ball == null && remoteBatting -> "Waiting for $batterName to flip"
                        ball == null -> "Flip the book to bat"
                        ball.isOut -> "Out on page ${ball.page}"
                        else -> "Page ${ball.page} · ${ball.runs} run${if (ball.runs == 1) "" else "s"}"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    color = when {
                        ball == null -> MaterialTheme.colorScheme.onSurfaceVariant
                        else -> ballColor(ball)
                    }
                )
            }

            FlipButton(
                label = when {
                    computerBatting -> "CPU"
                    remoteBatting -> "WAIT"
                    isAnimating -> "..."
                    else -> "FLIP"
                },
                // Disabled while the computer or the online opponent bats.
                enabled = !isAnimating && !computerBatting && !remoteBatting,
                onClick = onDrawBall
            )
        }

        Confetti(confetti, Modifier.matchParentSize())

        // A wicket flashes the whole ground red for a moment.
        Box(
            Modifier
                .matchParentSize()
                .drawBehind {
                    if (wicketFlash.value > 0f) drawRect(Wicket.copy(alpha = wicketFlash.value))
                }
        )
    }
}

/** "FIFTY!", "CENTURY!", "150 UP!"… when this ball carried the score across a multiple of 50. */
private fun milestoneReached(runsBefore: Int, runsAfter: Int): String? {
    val milestone = runsAfter / MILESTONE_STEP * MILESTONE_STEP
    if (milestone == 0 || runsBefore >= milestone) return null
    return when (milestone) {
        50 -> "FIFTY!"
        100 -> "CENTURY!"
        else -> "$milestone UP!"
    }
}

@Composable
private fun Scoreboard(state: GameState, innings: InningsState, batterName: String) {
    val runs by animateIntAsState(innings.runs, tween(durationMillis = 450), label = "runs")
    val overs = "${innings.ballsBowled / 6}.${innings.ballsBowled % 6}"

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = Pavilion.copy(alpha = 0.9f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(1.dp, Floodlight.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (state.phase == Phase.INNINGS_2) "THE CHASE" else "1ST INNINGS",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary
            )
            Text(
                text = batterName,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = runs.toString(),
                style = MaterialTheme.typography.displayLarge.copy(fontSize = 72.sp, lineHeight = 76.sp),
                color = MaterialTheme.colorScheme.primary
            )
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = state.config.oversLimit?.let { "Overs $overs / $it" } ?: "Overs $overs",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                GameLogic.runRate(innings)?.let { rate ->
                    Text(
                        text = "Run rate ${String.format(Locale.US, "%.1f", rate)}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (state.phase == Phase.INNINGS_2) ChaseStrip(state)
        }
    }
}

/** What the chaser still needs, and how far along the chase is. */
@Composable
private fun ChaseStrip(state: GameState) {
    val needed = GameLogic.runsNeeded(state) ?: return
    val target = state.target ?: return
    val ballsLeft = GameLogic.ballsRemaining(state)
    val fallingBehind = ballsLeft != null && needed > ballsLeft * AVERAGE_RUNS_PER_BALL
    val accent = if (fallingBehind) Wicket else Turf
    val progress by animateFloatAsState(
        targetValue = (state.innings2.runs.toFloat() / target).coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 450),
        label = "chase"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = when (ballsLeft) {
                    null -> "Need $needed to win"
                    else -> "Need $needed from $ballsLeft ball${if (ballsLeft == 1) "" else "s"}"
                },
                style = MaterialTheme.typography.titleMedium,
                color = accent
            )
            Text(
                text = "Target $target",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(CircleShape)
                .background(BoundaryRopeDim)
        ) {
            Box(
                Modifier
                    .fillMaxWidth(progress)
                    .fillMaxHeight()
                    .background(accent, CircleShape)
            )
        }
    }
}

/** The six balls of the current over, filling in as they are bowled. */
@Composable
private fun OverTracker(innings: InningsState) {
    val over = GameLogic.currentOver(innings)
    val overIndex = (innings.balls.size - 1).coerceAtLeast(0) / 6

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = "THIS OVER",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(6) { i ->
                val ball = over.getOrNull(i)
                // Keyed so a chip pops in once, when its ball is bowled.
                key(overIndex, i, ball != null) {
                    if (ball == null) {
                        BallChip(null)
                    } else {
                        val scale = remember { Animatable(0f) }
                        LaunchedEffect(Unit) {
                            scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
                        }
                        BallChip(
                            ball,
                            Modifier.graphicsLayer {
                                scaleX = scale.value
                                scaleY = scale.value
                            }
                        )
                    }
                }
            }
        }
    }
}

/** Stamps the result across the book as the page lands: "SIX!", "OUT!", and any milestone. */
@Composable
private fun ResultStamp(landing: Landing?) {
    val scale = remember { Animatable(0f) }
    val fade = remember { Animatable(0f) }

    LaunchedEffect(landing?.serial) {
        if (landing == null) return@LaunchedEffect
        fade.snapTo(1f)
        scale.snapTo(0.3f)
        scale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            )
        )
        if (!landing.ball.isOut) {
            delay(STAMP_HOLD_MILLIS)
            fade.animateTo(0f, tween(durationMillis = 250))
        }
    }

    if (landing == null) return
    val ball = landing.ball
    val color = ballColor(ball)
    // A two is routine; boundaries and wickets get the full-size stamp.
    val big = ball.isOut || ball.runs >= 4

    Column(
        modifier = Modifier
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
                alpha = fade.value
                rotationZ = -6f
            }
            .background(NightSky.copy(alpha = 0.88f), RoundedCornerShape(18.dp))
            .border(2.dp, color, RoundedCornerShape(18.dp))
            .padding(horizontal = 22.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = when {
                ball.isOut -> "OUT!"
                ball.runs == 8 -> "EIGHT!"
                ball.runs == 6 -> "SIX!"
                ball.runs == 4 -> "FOUR!"
                else -> "+${ball.runs}"
            },
            style = if (big) {
                MaterialTheme.typography.displayLarge
            } else {
                MaterialTheme.typography.headlineMedium
            },
            color = color
        )
        landing.milestone?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.titleLarge,
                color = Chalk
            )
        }
    }
}

/** The big round button that bats. It breathes while it is this phone's turn to flip. */
@Composable
private fun FlipButton(label: String, enabled: Boolean, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(if (pressed) 0.92f else 1f, label = "press")
    val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 1400, easing = LinearOutSlowInEasing)),
        label = "ring"
    )

    Box(
        modifier = Modifier
            .size(116.dp)
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .drawBehind {
                if (enabled) {
                    // A ring that swells out of the button and fades.
                    drawCircle(
                        color = Floodlight.copy(alpha = 0.6f * (1f - pulse)),
                        radius = size.minDimension / 2f * (1f + 0.3f * pulse),
                        style = Stroke(width = 3.dp.toPx())
                    )
                }
            }
            .clip(CircleShape)
            .background(
                if (enabled) Brush.verticalGradient(listOf(Floodlight, FloodlightDeep))
                else Brush.verticalGradient(listOf(PavilionHigh, PavilionHigh))
            )
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.headlineMedium,
            color = if (enabled) OnFloodlight else ChalkDim
        )
    }
}

@Preview(device = "spec:width=411dp,height=891dp")
@Composable
fun GameScreenPreview() {
    val balls = listOf(BallResult(124, 4, false), BallResult(36, 6, false), BallResult(212, 2, false))
    BookCricketTheme {
        StadiumBackground {
            GameScreen(
                state = GameState(
                    config = MatchConfig("Player 1", "Player 2", Player.ONE, 1),
                    phase = Phase.INNINGS_2,
                    innings1 = InningsState(Player.ONE, 24, 6),
                    innings2 = InningsState(Player.TWO, 12, 3, balls = balls),
                    lastBall = balls.last(),
                    target = 25
                ),
                pendingBall = null,
                onDrawBall = {},
                onCommitBall = {}
            )
        }
    }
}
