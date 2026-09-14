package com.codebuzz.app.bookcric.ui.screens

import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.codebuzz.app.bookcric.game.*
import com.codebuzz.app.bookcric.ui.theme.BookCricketTheme
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

@Composable
fun ResultScreen(
    state: GameState,
    onPlayAgain: () -> Unit,
    modifier: Modifier = Modifier
) {
    val outcome = GameLogic.outcome(state)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val graphicsLayer = rememberGraphicsLayer()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        ResultPoster(
            state = state,
            outcome = outcome,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false)
                .aspectRatio(9f / 16f)
                .clip(RoundedCornerShape(28.dp))
                .drawWithGraphicsLayer(graphicsLayer)
        )

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
}

/** Records the composable's draws into [layer] every frame so it can be captured on demand. */
private fun Modifier.drawWithGraphicsLayer(
    layer: GraphicsLayer
): Modifier = this.then(
    Modifier.drawWithContent {
        layer.record { this@drawWithContent.drawContent() }
        drawLayer(layer)
    }
)

/** Short caption sent alongside the shared image. */
private fun shareCaption(state: GameState, outcome: MatchOutcome): String = when (outcome) {
    is MatchOutcome.Win -> {
        val winnerName = if (outcome.winner == Player.ONE) state.config.playerOneName else state.config.playerTwoName
        "$winnerName won our Book Cricket match! 🏏"
    }
    MatchOutcome.Tie -> "Our Book Cricket match ended in a tie! 🏏"
    else -> "Playing Book Cricket! 🏏"
}

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
    modifier: Modifier = Modifier
) {
    val resultText = when (outcome) {
        is MatchOutcome.Win -> {
            val winnerName = if (outcome.winner == Player.ONE) state.config.playerOneName else state.config.playerTwoName
            "$winnerName Wins!"
        }
        MatchOutcome.Tie -> "It's a Tie!"
        else -> "Match In Progress"
    }
    val margin = kotlin.math.abs(state.innings1.runs - state.innings2.runs)
    val marginText = if (outcome is MatchOutcome.Win && margin > 0) {
        "won by $margin run${if (margin == 1) "" else "s"}"
    } else null

    Box(
        modifier = modifier.background(
            Brush.linearGradient(
                colors = listOf(
                    Color(0xFF6750A4),
                    Color(0xFF4F378B),
                    Color(0xFF21005D)
                ),
                start = Offset(0f, 0f),
                end = Offset(1000f, 1000f)
            )
        )
    ) {
        // Decorative translucent blobs for depth.
        Box(
            modifier = Modifier
                .size(180.dp)
                .align(Alignment.TopEnd)
                .offset(x = 60.dp, y = (-60).dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.08f))
        )
        Box(
            modifier = Modifier
                .size(220.dp)
                .align(Alignment.BottomStart)
                .offset(x = (-80).dp, y = 60.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.06f))
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "🏏  BOOK CRICKET",
                color = Color.White.copy(alpha = 0.8f),
                style = MaterialTheme.typography.labelLarge,
                letterSpacing = 2.sp
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = resultText,
                    color = Color.White,
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center
                )
                marginText?.let {
                    Text(
                        text = it,
                        color = Color(0xFFEADDFF),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PosterScoreRow(
                    playerName = state.config.playerOneName,
                    runs = state.innings1.runs,
                    isBattingFirst = state.config.battingFirst == Player.ONE,
                    isWinner = (outcome as? MatchOutcome.Win)?.winner == Player.ONE
                )
                PosterScoreRow(
                    playerName = state.config.playerTwoName,
                    runs = state.innings2.runs,
                    isBattingFirst = state.config.battingFirst == Player.TWO,
                    isWinner = (outcome as? MatchOutcome.Win)?.winner == Player.TWO
                )
            }

            Text(
                text = "Played on Book Cricket",
                color = Color.White.copy(alpha = 0.55f),
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

@Composable
private fun PosterScoreRow(
    playerName: String,
    runs: Int,
    isBattingFirst: Boolean,
    isWinner: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = if (isWinner) 0.22f else 0.12f))
            .padding(horizontal = 18.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = playerName,
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = if (isBattingFirst) "Batted 1st" else "Batted 2nd",
                color = Color.White.copy(alpha = 0.65f),
                style = MaterialTheme.typography.labelSmall
            )
        }
        Text(
            text = "$runs runs",
            color = Color.White,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
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
                innings1 = InningsState(Player.ONE, 24, 12, true),
                innings2 = InningsState(Player.TWO, 25, 10, false),
                target = 25
            ),
            onPlayAgain = {}
        )
    }
}
