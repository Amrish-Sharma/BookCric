package com.codebuzz.app.bookcric.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codebuzz.app.bookcric.data.Stats
import com.codebuzz.app.bookcric.game.MatchConfig
import com.codebuzz.app.bookcric.game.Player
import com.codebuzz.app.bookcric.ui.components.StadiumBackground
import com.codebuzz.app.bookcric.ui.theme.*
import kotlin.random.Random

@Composable
fun SetupScreen(
    stats: Stats,
    onStartMatch: (MatchConfig) -> Unit,
    onPlayOnline: (name: String) -> Unit,
    onToggleSound: () -> Unit,
    modifier: Modifier = Modifier
) {
    var player1Name by remember { mutableStateOf("") }
    var player2Name by remember { mutableStateOf("") }
    var battingFirst by remember { mutableStateOf(Player.ONE) }
    var oversLimitOption by remember { mutableStateOf<Int?>(1) }
    var mode by remember { mutableStateOf(SetupMode.FRIEND) }
    val vsComputer = mode == SetupMode.COMPUTER
    val online = mode == SetupMode.ONLINE
    // Online the opponent sees this name, so the default must mean something to them.
    val defaultOnlineName = remember { "Player ${Random.nextInt(100, 1000)}" }

    val defaultPlayerOneName = when (mode) {
        SetupMode.FRIEND -> "Player 1"
        SetupMode.COMPUTER -> "You"
        SetupMode.ONLINE -> defaultOnlineName
    }
    val playerOneLabel = player1Name.ifBlank { defaultPlayerOneName }
    val playerTwoLabel = if (vsComputer) MatchConfig.COMPUTER_NAME else player2Name.ifBlank { "Player 2" }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(Modifier.fillMaxWidth()) {
                Wordmark(Modifier.align(Alignment.Center))
                IconButton(onClick = onToggleSound, modifier = Modifier.align(Alignment.TopEnd)) {
                    Icon(
                        imageVector = if (stats.soundEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                        contentDescription = if (stats.soundEnabled) "Turn sound off" else "Turn sound on",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            RecordsStrip(stats)

            Row(
                Modifier.fillMaxWidth().selectableGroup(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SetupMode.entries.forEach { option ->
                    SelectCard(
                        selected = mode == option,
                        onClick = { mode = option },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(option.icon, contentDescription = null, modifier = Modifier.size(28.dp))
                        Text(option.label, style = MaterialTheme.typography.labelLarge, maxLines = 1)
                    }
                }
            }

            OutlinedTextField(
                value = player1Name,
                onValueChange = { player1Name = it },
                label = { Text(if (mode == SetupMode.FRIEND) "Player 1 Name" else "Your Name") },
                placeholder = { Text(defaultPlayerOneName) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            if (mode == SetupMode.FRIEND) {
                OutlinedTextField(
                    value = player2Name,
                    onValueChange = { player2Name = it },
                    label = { Text("Player 2 Name") },
                    placeholder = { Text("Player 2") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            if (online) {
                Text(
                    text = "Play against a friend on their own phone. Both phones need to be close " +
                        "together with Bluetooth and Wi-Fi turned on — no internet needed.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                MatchOptions(
                    playerOneLabel = playerOneLabel,
                    playerTwoLabel = playerTwoLabel,
                    battingFirst = battingFirst,
                    onBattingFirstChange = { battingFirst = it },
                    oversLimit = oversLimitOption,
                    onOversLimitChange = { oversLimitOption = it }
                )
            }
        }

        Button(
            onClick = {
                if (online) {
                    onPlayOnline(playerOneLabel.trim())
                } else {
                    onStartMatch(
                        MatchConfig(
                            playerOneName = playerOneLabel,
                            playerTwoName = playerTwoLabel,
                            battingFirst = battingFirst,
                            oversLimit = oversLimitOption,
                            vsComputer = vsComputer
                        )
                    )
                }
            },
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text(
                text = if (online) "FIND A FRIEND NEARBY" else "START MATCH",
                style = MaterialTheme.typography.titleMedium,
                letterSpacing = 1.5.sp
            )
        }
    }
}

private enum class SetupMode(val label: String, val icon: ImageVector) {
    FRIEND("Friend", Icons.Filled.Group),
    COMPUTER("Computer", Icons.Filled.SmartToy),
    ONLINE("Online", Icons.Filled.Wifi)
}

@Composable
private fun Wordmark(modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "BOOK",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
            letterSpacing = 10.sp
        )
        Text(
            text = "CRICKET",
            style = MaterialTheme.typography.displayMedium,
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = 3.sp
        )
        Text(
            text = "Flip a page. The last digit is your runs.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** This phone's records. Hidden until there is something to show. */
@Composable
private fun RecordsStrip(stats: Stats) {
    val played = stats.wins + stats.losses + stats.ties
    if (stats.highScore == 0 && played == 0) return

    Row(
        Modifier.fillMaxWidth().height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        StatTile(
            title = "HIGH SCORE",
            value = stats.highScore.toString(),
            caption = stats.highScoreBy,
            modifier = Modifier.weight(1f)
        )
        if (played > 0) {
            StatTile(
                title = "WON · LOST",
                value = "${stats.wins} · ${stats.losses}",
                caption = if (stats.ties > 0) "${stats.ties} tied" else null,
                modifier = Modifier.weight(1f)
            )
            StatTile(
                title = "STREAK",
                value = stats.streak.toString(),
                caption = "best ${stats.bestStreak}",
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun StatTile(title: String, value: String, caption: String?, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxHeight(),
        shape = RoundedCornerShape(16.dp),
        color = Pavilion.copy(alpha = 0.8f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(1.dp, BoundaryRopeDim)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )
            if (caption != null) {
                Text(
                    text = caption,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/** Who bats first and the overs cap. Shared by the setup screen and the online host's lobby. */
@Composable
fun MatchOptions(
    playerOneLabel: String,
    playerTwoLabel: String,
    battingFirst: Player,
    onBattingFirstChange: (Player) -> Unit,
    oversLimit: Int?,
    onOversLimitChange: (Int?) -> Unit
) {
    SectionLabel("WHO BATS FIRST?")

    Row(
        Modifier.fillMaxWidth().selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        listOf(Player.ONE to playerOneLabel, Player.TWO to playerTwoLabel).forEach { (player, label) ->
            SelectCard(
                selected = battingFirst == player,
                onClick = { onBattingFirstChange(player) },
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }

    SectionLabel("OVERS")

    val options = listOf(1, 2, 5, null)
    Row(
        Modifier.fillMaxWidth().selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        options.forEach { option ->
            SelectCard(
                selected = oversLimit == option,
                onClick = { onOversLimitChange(option) },
                // "Unlimited" needs more room than a digit.
                modifier = Modifier.weight(if (option == null) 2f else 1f)
            ) {
                Text(
                    text = option?.toString() ?: "Unlimited",
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Start,
        modifier = Modifier.fillMaxWidth()
    )
}

/** A tappable card for picking one of a few options; the chosen one lights up gold. */
@Composable
private fun SelectCard(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val borderColor by animateColorAsState(if (selected) Floodlight else BoundaryRopeDim, label = "border")
    val containerColor by animateColorAsState(
        if (selected) FloodlightContainer.copy(alpha = 0.6f) else Pavilion.copy(alpha = 0.8f),
        label = "container"
    )
    Surface(
        selected = selected,
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = containerColor,
        contentColor = if (selected) Floodlight else Chalk,
        border = BorderStroke(if (selected) 2.dp else 1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
            content = content
        )
    }
}

@Preview(device = "spec:width=411dp,height=891dp")
@Composable
fun SetupScreenPreview() {
    BookCricketTheme {
        StadiumBackground {
            SetupScreen(
                stats = Stats(highScore = 86, highScoreBy = "Riya", wins = 5, losses = 2, streak = 3, bestStreak = 4),
                onStartMatch = {},
                onPlayOnline = {},
                onToggleSound = {}
            )
        }
    }
}
