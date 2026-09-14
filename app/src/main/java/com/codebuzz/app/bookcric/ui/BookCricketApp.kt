package com.codebuzz.app.bookcric.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.codebuzz.app.bookcric.game.Phase
import com.codebuzz.app.bookcric.ui.screens.GameScreen
import com.codebuzz.app.bookcric.ui.screens.InningsBreakScreen
import com.codebuzz.app.bookcric.ui.screens.ResultScreen
import com.codebuzz.app.bookcric.ui.screens.SetupScreen
import com.codebuzz.app.bookcric.ui.theme.BookCricketTheme

@Composable
fun BookCricketApp(
    viewModel: GameViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    BookCricketTheme {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            val modifier = Modifier.padding(innerPadding)
            
            val state = uiState
            if (state == null) {
                SetupScreen(
                    onStartMatch = { viewModel.startMatch(it) },
                    modifier = modifier
                )
            } else {
                when (state.phase) {
                    Phase.SETUP -> { // Should not happen based on ViewModel logic but good to handle
                        SetupScreen(
                            onStartMatch = { viewModel.startMatch(it) },
                            modifier = modifier
                        )
                    }
                    Phase.INNINGS_1, Phase.INNINGS_2 -> {
                        GameScreen(
                            state = state,
                            onFlip = { viewModel.flip() },
                            modifier = modifier
                        )
                    }
                    Phase.INNINGS_BREAK -> {
                        InningsBreakScreen(
                            state = state,
                            onStartChase = { viewModel.startSecondInnings() },
                            modifier = modifier
                        )
                    }
                    Phase.RESULT -> {
                        ResultScreen(
                            state = state,
                            onPlayAgain = { viewModel.playAgain() },
                            modifier = modifier
                        )
                    }
                }
            }
        }
    }
}
