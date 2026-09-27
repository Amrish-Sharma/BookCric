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

    /**
     * Draw the next ball without applying it, so the UI can animate the page turn first.
     * Pass the result to [commitBall] once the page has landed.
     */
    fun drawBall(): BallResult? {
        val currentState = uiState.value ?: return null
        return GameLogic.flip(rng, currentState.config.bookPages)
    }

    /** Apply a ball previously returned by [drawBall] to the current innings. */
    fun commitBall(ball: BallResult) {
        val currentState = uiState.value ?: return
        savedStateHandle[KEY_GAME_STATE] = GameLogic.applyBall(currentState, ball)
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
