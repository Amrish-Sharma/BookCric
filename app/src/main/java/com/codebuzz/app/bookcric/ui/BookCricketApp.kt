package com.codebuzz.app.bookcric.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import com.codebuzz.app.bookcric.game.GameState
import com.codebuzz.app.bookcric.game.Phase
import com.codebuzz.app.bookcric.ui.components.StadiumBackground
import com.codebuzz.app.bookcric.ui.screens.GameScreen
import com.codebuzz.app.bookcric.ui.screens.InningsBreakScreen
import com.codebuzz.app.bookcric.ui.screens.OnlineLobbyScreen
import com.codebuzz.app.bookcric.ui.screens.ResultScreen
import com.codebuzz.app.bookcric.ui.screens.SetupScreen
import com.codebuzz.app.bookcric.ui.theme.BookCricketTheme

/**
 * What is on screen, with the data it shows. A screen animating out keeps the last snapshot it
 * was given, so it never has to draw a match that has already moved on to another phase.
 */
private data class Screen(val state: GameState?, val lobby: OnlineLobby?) {
    /** Screens with the same key are the same screen updating; a new key animates across. */
    val key: String
        get() = when {
            state != null && state.phase != Phase.SETUP -> state.phase.name
            lobby != null -> "LOBBY"
            else -> "SETUP"
        }
}

@Composable
fun BookCricketApp(
    viewModel: GameViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val lobby by viewModel.lobby.collectAsState()
    val pendingBall by viewModel.pendingBall.collectAsState()
    val stats by viewModel.stats.collectAsState()
    val newHighScore by viewModel.newHighScore.collectAsState()

    BookCricketTheme {
        StadiumBackground {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.onBackground
            ) { innerPadding ->
                AnimatedContent(
                    targetState = Screen(uiState, lobby),
                    modifier = Modifier.padding(innerPadding),
                    transitionSpec = {
                        (fadeIn(tween(durationMillis = 300)) + slideInVertically(tween(durationMillis = 300)) { it / 12 })
                            .togetherWith(fadeOut(tween(durationMillis = 200)))
                    },
                    contentKey = { it.key },
                    label = "screen"
                ) { screen ->
                    val state = screen.state
                    val onlineLobby = screen.lobby
                    when {
                        state != null && state.phase != Phase.SETUP -> when (state.phase) {
                            Phase.INNINGS_BREAK -> InningsBreakScreen(
                                state = state,
                                onStartChase = { viewModel.startSecondInnings() }
                            )
                            Phase.RESULT -> ResultScreen(
                                state = state,
                                stats = stats,
                                newHighScore = newHighScore,
                                onPlayAgain = { viewModel.playAgain() },
                                onSound = viewModel::playSound
                            )
                            else -> GameScreen(
                                state = state,
                                pendingBall = pendingBall,
                                onDrawBall = { viewModel.drawBall() },
                                onCommitBall = { viewModel.commitBall(it) },
                                onSound = viewModel::playSound
                            )
                        }
                        onlineLobby != null -> OnlineLobbyScreen(
                            lobby = onlineLobby,
                            onHost = { viewModel.hostOnline() },
                            onJoin = { viewModel.joinOnline() },
                            onConnect = { viewModel.connectTo(it) },
                            onCancel = { viewModel.cancelOnline() },
                            onStartMatch = { viewModel.startMatch(it) },
                            onLeave = { viewModel.leaveOnline() }
                        )
                        else -> SetupScreen(
                            stats = stats,
                            onStartMatch = { viewModel.startMatch(it) },
                            onPlayOnline = { viewModel.openOnline(it) },
                            onToggleSound = { viewModel.toggleSound() }
                        )
                    }
                }
            }
        }
    }
}
