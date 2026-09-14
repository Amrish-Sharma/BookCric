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

@Composable
fun SetupScreen(
    onStartMatch: (MatchConfig) -> Unit,
    modifier: Modifier = Modifier
) {
    var player1Name by remember { mutableStateOf("") }
    var player2Name by remember { mutableStateOf("") }
    var battingFirst by remember { mutableStateOf(Player.ONE) }
    var oversLimitOption by remember { mutableStateOf<Int?>(1) }

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

        OutlinedTextField(
            value = player1Name,
            onValueChange = { player1Name = it },
            label = { Text("Player 1 Name") },
            placeholder = { Text("Player 1") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = player2Name,
            onValueChange = { player2Name = it },
            label = { Text("Player 2 Name") },
            placeholder = { Text("Player 2") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Text(
            text = "Who bats first?",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.align(Alignment.Start)
        )

        Row(
            Modifier.fillMaxWidth().selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            PlayerRadioOption(
                label = player1Name.ifBlank { "Player 1" },
                selected = battingFirst == Player.ONE,
                onClick = { battingFirst = Player.ONE }
            )
            PlayerRadioOption(
                label = player2Name.ifBlank { "Player 2" },
                selected = battingFirst == Player.TWO,
                onClick = { battingFirst = Player.TWO }
            )
        }

        Text(
            text = "Overs limit",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.align(Alignment.Start)
        )

        val options = listOf(1, 2, 5, null)
        Row(
            Modifier.fillMaxWidth().selectableGroup(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            options.forEach { option ->
                OversOption(
                    label = option?.toString() ?: "Unlimited",
                    selected = oversLimitOption == option,
                    onClick = { oversLimitOption = option }
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = {
                onStartMatch(
                    MatchConfig(
                        playerOneName = player1Name.ifBlank { "Player 1" },
                        playerTwoName = player2Name.ifBlank { "Player 2" },
                        battingFirst = battingFirst,
                        oversLimit = oversLimitOption
                    )
                )
            },
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text("Start Match")
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
        SetupScreen(onStartMatch = {})
    }
}
