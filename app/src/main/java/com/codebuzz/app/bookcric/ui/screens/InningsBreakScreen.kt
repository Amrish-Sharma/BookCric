package com.codebuzz.app.bookcric.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.codebuzz.app.bookcric.game.GameState
import com.codebuzz.app.bookcric.game.InningsState
import com.codebuzz.app.bookcric.game.MatchConfig
import com.codebuzz.app.bookcric.game.Phase
import com.codebuzz.app.bookcric.game.Player
import com.codebuzz.app.bookcric.ui.theme.BookCricketTheme

@Composable
fun InningsBreakScreen(
    state: GameState,
    onStartChase: () -> Unit,
    modifier: Modifier = Modifier
) {
    val batter1Name = if (state.innings1.batter == Player.ONE) state.config.playerOneName else state.config.playerTwoName
    val batter2Name = if (state.innings2.batter == Player.ONE) state.config.playerOneName else state.config.playerTwoName

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Innings Over!",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            text = "$batter1Name scored",
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = "${state.innings1.runs} runs",
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.secondary
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Surface(
            color = MaterialTheme.colorScheme.tertiaryContainer,
            shape = MaterialTheme.shapes.large,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "$batter2Name needs ${state.target} to win",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(24.dp),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
        }
        
        Spacer(modifier = Modifier.height(64.dp))
        
        Button(
            onClick = onStartChase,
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text("Start Chase")
        }
    }
}

@Preview(showBackground = true, device = "spec:width=411dp,height=891dp")
@Composable
fun InningsBreakScreenPreview() {
    BookCricketTheme {
        InningsBreakScreen(
            state = GameState(
                config = MatchConfig("Player 1", "Player 2", Player.ONE, 2),
                phase = Phase.INNINGS_BREAK,
                innings1 = InningsState(Player.ONE, 24, 12, true),
                innings2 = InningsState(Player.TWO),
                target = 25
            ),
            onStartChase = {}
        )
    }
}
