package com.codebuzz.app.bookcric.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.codebuzz.app.bookcric.game.BallResult
import com.codebuzz.app.bookcric.game.GameLogic
import com.codebuzz.app.bookcric.game.GameState
import com.codebuzz.app.bookcric.game.MatchConfig
import kotlinx.coroutines.flow.StateFlow
import kotlin.random.Random

class GameViewModel(private val savedStateHandle: SavedStateHandle) : ViewModel() {

    private val rng = Random.Default

    /**
     * The current game state, persisted across process death.
     * Initialized to null (Setup phase).
     */
    val uiState: StateFlow<GameState?> = savedStateHandle.getStateFlow(KEY_GAME_STATE, null)

    /** Start a new match with the given configuration. */
    fun startMatch(config: MatchConfig) {
        savedStateHandle[KEY_GAME_STATE] = GameLogic.startMatch(config)
    }

    /** Flip the "book" to play one ball in the current innings. Returns the resulting ball. */
    fun flip(): BallResult? {
        val currentState = uiState.value ?: return null
        val updated = GameLogic.playBall(currentState, rng)
        savedStateHandle[KEY_GAME_STATE] = updated
        return updated.lastBall
    }

    /** Transition from the break into the second innings. */
    fun startSecondInnings() {
        val currentState = uiState.value ?: return
        savedStateHandle[KEY_GAME_STATE] = GameLogic.startSecondInnings(currentState)
    }

    /** Reset the game to the setup screen. */
    fun playAgain() {
        savedStateHandle[KEY_GAME_STATE] = null
    }

    companion object {
        private const val KEY_GAME_STATE = "game_state"
    }
}
