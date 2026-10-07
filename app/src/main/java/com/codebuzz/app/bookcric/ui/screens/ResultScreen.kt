package com.codebuzz.app.bookcric.ui.screens

import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.codebuzz.app.bookcric.audio.Sfx
import com.codebuzz.app.bookcric.data.Stats
import com.codebuzz.app.bookcric.game.*
import com.codebuzz.app.bookcric.ui.components.BallChip
import com.codebuzz.app.bookcric.ui.components.Confetti
import com.codebuzz.app.bookcric.ui.components.ConfettiState
import com.codebuzz.app.bookcric.ui.theme.*
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

private const val PLAY_STORE_URL = "https://play.google.com/store/apps/details?id=com.codebuzz.app.bookcric"

/** The width the poster is designed at; it is scaled as a whole to whatever space it gets. */
private const val POSTER_WIDTH_DP = 360f

/** The poster shows at most this many balls per innings — the last ones of a long innings. */
private const val POSTER_BALLS = 12

@Composable
fun ResultScreen(
    state: GameState,
    onPlayAgain: () -> Unit,
    modifier: Modifier = Modifier,
    stats: Stats = Stats(),
    newHighScore: Boolean = false,
    onSound: (Sfx) -> Unit = {}
) {
    val outcome = GameLogic.outcome(state)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val graphicsLayer = rememberGraphicsLayer()

    // The player this phone belongs to, when there is one: vs Computer and online.
    val me = state.config.localPlayer ?: if (state.config.vsComputer) Player.ONE else null
    val winner = (outcome as? MatchOutcome.Win)?.winner
    // Pass-and-play always has a winner in the room; otherwise celebrate only our own win.
    val celebrate = winner != null && (me == null || winner == me)

    val confetti = remember { ConfettiState() }
    // Saved, so rotating the phone doesn't set the party off again.
    var celebrated by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (celebrate && !celebrated) {
            celebrated = true
            onSound(Sfx.FANFARE)
            confetti.fire()
        }
    }

    Box(modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .aspectRatio(9f / 16f)
            ) {
                ResultPoster(
                    state = state,
                    outcome = outcome,
                    newHighScore = newHighScore,
                    showStoreLink = false,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(28.dp))
                )
                // The copy that gets shared: the same poster plus where to get the game, which
                // someone already playing doesn't need to see. Recorded, never shown.
                ResultPoster(
                    state = state,
                    outcome = outcome,
                    newHighScore = newHighScore,
                    showStoreLink = true,
                    modifier = Modifier
                        .fillMaxSize()
                        .clearAndSetSemantics { }
                        .recordInto(graphicsLayer)
                )
            }

            if (me != null && winner == me && stats.streak >= 2) {
                Text(
                    text = "🔥 ${stats.streak} wins in a row",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Button(
                onClick = {
                    scope.launch {
                        val bitmap = graphicsLayer.toImageBitmap().asAndroidBitmap()
                        shareResultImage(context, bitmap, shareCaption(state, outcome))
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Icon(Icons.Filled.Share, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Share Result")
            }

            OutlinedButton(
                onClick = onPlayAgain,
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text("Play Again")
            }
        }

        // Outside the poster's layer, so confetti never ends up in the shared image.
        Confetti(confetti, Modifier.matchParentSize())
    }
}

/**
 * Records the composable's draws into [layer] so it can be captured on demand, without drawing
 * them to the screen.
 */
private fun Modifier.recordInto(
    layer: GraphicsLayer
): Modifier = this.then(
    Modifier.drawWithContent {
        layer.record { this@drawWithContent.drawContent() }
    }
)

/** Caption sent alongside the shared image; unlike the image, its Play Store link can be tapped. */
private fun shareCaption(state: GameState, outcome: MatchOutcome): String {
    val score = "${state.innings1.runs}–${state.innings2.runs}"
    val headline = when (outcome) {
        is MatchOutcome.Win -> "${state.config.nameOf(outcome.winner)} won our Book Cricket match $score! 🏏"
        MatchOutcome.Tie -> "Our Book Cricket match ended in a tie, $score! 🏏"
        else -> "Playing Book Cricket! 🏏"
    }
    return "$headline\n${challengeLine(state, outcome)} Play free: $PLAY_STORE_URL"
}

/** The dare to whoever sees the result. */
private fun challengeLine(state: GameState, outcome: MatchOutcome): String = when (outcome) {
    MatchOutcome.Tie -> "Can you break the tie?"
    else -> "Think you can beat ${maxOf(state.innings1.runs, state.innings2.runs)}?"
}

/** How the match was won, in cricket's terms: by runs batting first, by balls left when chasing. */
private fun marginLine(state: GameState, outcome: MatchOutcome): String? {
    if (outcome !is MatchOutcome.Win) return null
    if (outcome.winner == state.innings1.batter) {
        val margin = state.innings1.runs - state.innings2.runs
        return "won by $margin run${if (margin == 1) "" else "s"}"
    }
    val faced = state.innings2.ballsBowled
    val spare = state.config.oversLimit?.let { it * 6 - faced }
    return when {
        spare == null || spare == 0 -> "chased it down in $faced ball${if (faced == 1) "" else "s"}"
        else -> "chased it down with $spare ball${if (spare == 1) "" else "s"} to spare"
    }
}

/** "1-OVER MATCH · VS COMPUTER": what kind of game the result is from. */
private fun formatLine(config: MatchConfig): String {
    val overs = when (val limit = config.oversLimit) {
        null -> "UNLIMITED OVERS"
        else -> "$limit-OVER MATCH"
    }
    return when {
        config.vsComputer -> "$overs · VS COMPUTER"
        config.isOnline -> "$overs · ONLINE"
        else -> overs
    }
}

/** "2 fours · 1 six": the boundaries in an innings, or null if there were none. */
private fun boundariesLine(innings: InningsState): String? =
    listOf(4 to ("four" to "fours"), 6 to ("six" to "sixes"), 8 to ("eight" to "eights"))
        .mapNotNull { (runs, names) ->
            val count = innings.balls.count { it.runs == runs }
            if (count == 0) null else "$count ${if (count == 1) names.first else names.second}"
        }
        .joinToString(" · ")
        .ifEmpty { null }

/** Saves [bitmap] to the app cache and opens the system share sheet (WhatsApp, X, Facebook, ...). */
private fun shareResultImage(context: android.content.Context, bitmap: Bitmap, caption: String) {
    val imagesDir = File(context.cacheDir, "shared_images").apply { mkdirs() }
    val file = File(imagesDir, "bookcric_result_${System.currentTimeMillis()}.png")
    FileOutputStream(file).use { out -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, out) }

    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_TEXT, caption)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(sendIntent, "Share match result"))
}

@Composable
private fun ResultPoster(
    state: GameState,
    outcome: MatchOutcome,
    newHighScore: Boolean,
    showStoreLink: Boolean,
    modifier: Modifier = Modifier
) {
    val resultText = when (outcome) {
        // The vs-Computer default name is "You", which takes a different verb.
        is MatchOutcome.Win -> state.config.nameOf(outcome.winner).let { if (it == "You") "You Win!" else "$it Wins!" }
        MatchOutcome.Tie -> "It's a Tie!"
        else -> "Match In Progress"
    }
    val marginText = marginLine(state, outcome)

    // Lay the poster out as if it were always POSTER_WIDTH_DP wide, whatever the screen size or
    // font scale, so everything fits and the shared image looks the same from every phone.
    BoxWithConstraints(modifier) {
        val posterDensity = Density(density = constraints.maxWidth / POSTER_WIDTH_DP, fontScale = 1f)
        CompositionLocalProvider(LocalDensity provides posterDensity) {
            PosterContent(state, outcome, resultText, marginText, newHighScore, showStoreLink)
        }
    }
}

@Composable
private fun PosterContent(
    state: GameState,
    outcome: MatchOutcome,
    resultText: String,
    marginText: String?,
    newHighScore: Boolean,
    showStoreLink: Boolean
) {
    Box(
        modifier = Modifier.fillMaxSize().background(
            Brush.linearGradient(
                colors = listOf(
                    PavilionHigh,
                    Outfield,
                    NightSky
                ),
                start = Offset(0f, 0f),
                end = Offset(1000f, 1000f)
            )
        )
    ) {
        // Floodlight glow in one corner, a patch of lit turf in the other.
        Box(
            modifier = Modifier
                .size(180.dp)
                .align(Alignment.TopEnd)
                .offset(x = 60.dp, y = (-60).dp)
                .clip(CircleShape)
                .background(Floodlight.copy(alpha = 0.14f))
        )
        Box(
            modifier = Modifier
                .size(220.dp)
                .align(Alignment.BottomStart)
                .offset(x = (-80).dp, y = 60.dp)
                .clip(CircleShape)
                .background(Turf.copy(alpha = 0.08f))
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "🏏  BOOK CRICKET",
                    color = Color.White.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.labelLarge,
                    letterSpacing = 2.sp
                )
                Text(
                    text = formatLine(state.config),
                    color = Turf,
                    style = MaterialTheme.typography.labelSmall
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = if (outcome is MatchOutcome.Win) "🏆" else "🤝",
                    style = MaterialTheme.typography.displaySmall
                )
                Text(
                    text = resultText,
                    color = Floodlight,
                    style = MaterialTheme.typography.displaySmall,
                    textAlign = TextAlign.Center
                )
                marginText?.let {
                    Text(
                        text = it,
                        color = Chalk,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                if (newHighScore) {
                    Text(
                        text = "NEW HIGH SCORE",
                        color = OnFloodlight,
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .background(Floodlight, RoundedCornerShape(50))
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Rows follow batting order; name and runs both come from the innings itself.
                listOf(state.innings1, state.innings2).forEachIndexed { index, innings ->
                    PosterScoreRow(
                        playerName = state.config.nameOf(innings.batter),
                        innings = innings,
                        isBattingFirst = index == 0,
                        isWinner = (outcome as? MatchOutcome.Win)?.winner == innings.batter
                    )
                }
            }

            // The invitation: a dare, and where to get the game.
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = challengeLine(state, outcome),
                    color = Floodlight,
                    style = MaterialTheme.typography.titleMedium
                )
                if (showStoreLink) {
                    Text(
                        text = "PLAY FREE ON GOOGLE PLAY",
                        color = Color.White.copy(alpha = 0.8f),
                        style = MaterialTheme.typography.labelSmall
                    )
                    Text(
                        text = PLAY_STORE_URL.removePrefix("https://"),
                        color = Color.White.copy(alpha = 0.6f),
                        style = MaterialTheme.typography.labelSmall,
                        letterSpacing = 0.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PosterScoreRow(
    playerName: String,
    innings: InningsState,
    isBattingFirst: Boolean,
    isWinner: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = if (isWinner) 0.16f else 0.08f))
            .then(
                if (isWinner) Modifier.border(1.5.dp, Floodlight, RoundedCornerShape(16.dp)) else Modifier
            )
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = playerName,
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = buildString {
                        append(if (isBattingFirst) "Batted 1st" else "Batted 2nd")
                        append(" · ${innings.ballsBowled} ball${if (innings.ballsBowled == 1) "" else "s"}")
                        append(if (innings.isOut) " · out" else " · not out")
                    },
                    color = Color.White.copy(alpha = 0.65f),
                    style = MaterialTheme.typography.labelSmall
                )
            }
            Text(
                text = "${innings.runs} runs",
                color = if (isWinner) Floodlight else Color.White,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }

        if (innings.balls.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                innings.balls.takeLast(POSTER_BALLS).forEach { BallChip(it, size = 20.dp) }
            }
        }
        boundariesLine(innings)?.let {
            Text(
                text = it,
                color = Color.White.copy(alpha = 0.8f),
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

@Preview(showBackground = true, device = "spec:width=411dp,height=891dp")
@Composable
fun ResultScreenPreview() {
    BookCricketTheme {
        ResultScreen(
            state = GameState(
                config = MatchConfig("Alice", "Bob", Player.ONE, 2),
                phase = Phase.RESULT,
                innings1 = InningsState(
                    Player.ONE, 24, 5, true,
                    listOf(8, 6, 4, 6, 0).map { BallResult(100 + it, it, isOut = it == 0) }
                ),
                innings2 = InningsState(
                    Player.TWO, 26, 4, false,
                    listOf(6, 8, 4, 8).map { BallResult(200 + it, it, isOut = false) }
                ),
                target = 25
            ),
            onPlayAgain = {}
        )
    }
}
