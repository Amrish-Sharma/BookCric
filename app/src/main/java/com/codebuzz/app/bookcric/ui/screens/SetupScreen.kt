package com.codebuzz.app.bookcric.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.codebuzz.app.bookcric.game.MatchConfig
import com.codebuzz.app.bookcric.game.Player
import com.codebuzz.app.bookcric.ui.theme.BookCricketTheme
import kotlin.random.Random

@Composable
fun SetupScreen(
    onStartMatch: (MatchConfig) -> Unit,
    onPlayOnline: (name: String) -> Unit,
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
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Match Setup",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )

        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            SetupMode.entries.forEachIndexed { index, option ->
                SegmentedButton(
                    selected = mode == option,
                    onClick = { mode = option },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = SetupMode.entries.size)
                ) {
                    Text(option.label)
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

        Spacer(modifier = Modifier.weight(1f))

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
            Text(if (online) "Find a Friend Nearby" else "Start Match")
        }
    }
}

private enum class SetupMode(val label: String) {
    FRIEND("vs Friend"),
    COMPUTER("vs Computer"),
    ONLINE("Online")
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
    Text(
        text = "Who bats first?",
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.fillMaxWidth()
    )

    Row(
        Modifier.fillMaxWidth().selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        PlayerRadioOption(
            label = playerOneLabel,
            selected = battingFirst == Player.ONE,
            onClick = { onBattingFirstChange(Player.ONE) }
        )
        PlayerRadioOption(
            label = playerTwoLabel,
            selected = battingFirst == Player.TWO,
            onClick = { onBattingFirstChange(Player.TWO) }
        )
    }

    Text(
        text = "Overs limit",
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.fillMaxWidth()
    )

    val options = listOf(1, 2, 5, null)
    Row(
        Modifier.fillMaxWidth().selectableGroup(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        options.forEach { option ->
            OversOption(
                label = option?.toString() ?: "Unlimited",
                selected = oversLimit == option,
                onClick = { onOversLimitChange(option) }
            )
        }
    }
}

@Composable
fun PlayerRadioOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.RadioButton
            )
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = null)
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

@Composable
fun OversOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        modifier = modifier
    )
}

@Preview(showBackground = true, device = "spec:width=411dp,height=891dp")
@Composable
fun SetupScreenPreview() {
    BookCricketTheme {
        SetupScreen(onStartMatch = {}, onPlayOnline = {})
    }
}
