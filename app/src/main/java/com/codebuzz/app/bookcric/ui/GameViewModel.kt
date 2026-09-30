package com.codebuzz.app.bookcric.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.codebuzz.app.bookcric.game.BallResult
import com.codebuzz.app.bookcric.game.GameLogic
import com.codebuzz.app.bookcric.game.GameState
import com.codebuzz.app.bookcric.game.MatchConfig
import com.codebuzz.app.bookcric.game.Phase
import com.codebuzz.app.bookcric.game.Player
import com.codebuzz.app.bookcric.online.ConnectionStatus
import com.codebuzz.app.bookcric.online.NearbyEndpoint
import com.codebuzz.app.bookcric.online.NearbySession
import com.codebuzz.app.bookcric.online.NetMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.random.Random

/** The online lobby: shown while [playerName] is set and no match is running. */
data class OnlineLobby(val playerName: String, val status: ConnectionStatus)

class GameViewModel(
    application: Application,
    private val savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {

    private val rng = Random.Default

    private val session = NearbySession(application)

    /**
     * The current game state, persisted across process death.
     * Initialized to null (Setup phase).
     */
    val uiState: StateFlow<GameState?> = savedStateHandle.getStateFlow(KEY_GAME_STATE, null)

    /** Name used online; non-null while the player is in online mode. */
    private val onlineName = MutableStateFlow<String?>(null)

    val lobby: StateFlow<OnlineLobby?> = combine(onlineName, session.status) { name, status ->
        name?.let { OnlineLobby(it, status) }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _pendingBall = MutableStateFlow<BallResult?>(null)

    /**
     * The ball being flipped right now: drawn here or by the online opponent, but not yet
     * applied. The UI animates it and then hands it to [commitBall]. Held here rather than in
     * the screen so a flip interrupted by an activity rebuild is replayed, not lost — online,
     * the other phone has already counted it.
     */
    val pendingBall: StateFlow<BallResult?> = _pendingBall.asStateFlow()

    /** Opponent balls that arrived while [pendingBall] was still being animated. */
    private val queuedBalls = ArrayDeque<BallResult>()

    init {
        // A restored online match can't continue: the connection died with the old process.
        if (uiState.value?.config?.isOnline == true) {
            savedStateHandle[KEY_GAME_STATE] = null
        }
        viewModelScope.launch { session.messages.collect(::onMessage) }
        viewModelScope.launch {
            session.status.collect { status ->
                // Opponent gone mid-match: back to the lobby, which explains why. A finished
                // match stays up so its result can still be shared.
                val state = uiState.value
                if (status !is ConnectionStatus.Connected && state?.config?.isOnline == true && state.phase != Phase.RESULT) {
                    resetMatch()
                }
            }
        }
    }

    /** Start a new match with the given configuration. Online, only the host starts matches. */
    fun startMatch(config: MatchConfig) {
        val status = session.status.value
        if (onlineName.value != null && status is ConnectionStatus.Connected) {
            val online = config.copy(localPlayer = Player.ONE)
            session.send(NetMessage.Start(online))
            savedStateHandle[KEY_GAME_STATE] = GameLogic.startMatch(online)
        } else {
            savedStateHandle[KEY_GAME_STATE] = GameLogic.startMatch(config)
        }
    }

    /**
     * Draw the next ball into [pendingBall] without applying it, so the UI can animate the page
     * turn first. Online, the ball is sent straight away so the opponent's book turns at the
     * same time. Ignored while another ball is still in flight, so a double tap draws only once.
     */
    fun drawBall() {
        val currentState = uiState.value ?: return
        if (_pendingBall.value != null || GameLogic.isRemoteTurn(currentState)) return
        val ball = GameLogic.flip(rng, currentState.config.bookPages)
        if (currentState.config.isOnline) session.send(NetMessage.Ball(ball))
        _pendingBall.value = ball
    }

    /** Apply [pendingBall] to the current innings once its page has landed. */
    fun commitBall(ball: BallResult) {
        if (ball !== _pendingBall.value) return
        val currentState = uiState.value ?: return
        savedStateHandle[KEY_GAME_STATE] = GameLogic.applyBall(currentState, ball)
        _pendingBall.value = queuedBalls.removeFirstOrNull()
    }

    /** Transition from the break into the second innings. */
    fun startSecondInnings() {
        val currentState = uiState.value ?: return
        if (currentState.phase != Phase.INNINGS_BREAK) return
        if (currentState.config.isOnline) session.send(NetMessage.StartChase)
        savedStateHandle[KEY_GAME_STATE] = GameLogic.startSecondInnings(currentState)
    }

    /** Reset the game to the setup screen — or, online, back to the lobby with the same opponent. */
    fun playAgain() {
        if (uiState.value?.config?.isOnline == true) session.send(NetMessage.Rematch)
        resetMatch()
    }

    /** Enter online mode as [name]; the lobby screen takes over from setup. */
    fun openOnline(name: String) {
        onlineName.value = name
    }

    fun hostOnline() {
        onlineName.value?.let(session::host)
    }

    fun joinOnline() {
        onlineName.value?.let(session::join)
    }

    fun connectTo(endpoint: NearbyEndpoint) = session.connectTo(endpoint)

    /** Stop hosting/searching but stay in the lobby. */
    fun cancelOnline() = session.stop()

    /** Disconnect and go back to the setup screen. */
    fun leaveOnline() {
        session.stop()
        onlineName.value = null
        resetMatch()
    }

    private fun onMessage(message: NetMessage) {
        when (message) {
            is NetMessage.Start -> {
                resetMatch()
                savedStateHandle[KEY_GAME_STATE] =
                    GameLogic.startMatch(message.config.copy(localPlayer = Player.TWO))
            }
            is NetMessage.Ball ->
                if (_pendingBall.value == null) _pendingBall.value = message.ball
                else queuedBalls.addLast(message.ball)
            NetMessage.StartChase -> uiState.value?.let {
                savedStateHandle[KEY_GAME_STATE] = GameLogic.startSecondInnings(it)
            }
            NetMessage.Rematch -> resetMatch()
        }
    }

    private fun resetMatch() {
        queuedBalls.clear()
        _pendingBall.value = null
        savedStateHandle[KEY_GAME_STATE] = null
    }

    override fun onCleared() {
        session.stop()
    }

    companion object {
        private const val KEY_GAME_STATE = "game_state"
    }
}
