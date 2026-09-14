package com.codebuzz.app.bookcric.game

import android.os.Parcelable
import kotlin.random.Random
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

/** Which player is at the crease / owns an innings. */
@Serializable
enum class Player { ONE, TWO }

/** High-level match phase — drives which screen the UI shows. */
@Serializable
enum class Phase { SETUP, INNINGS_1, INNINGS_BREAK, INNINGS_2, RESULT }

/**
 * Match settings chosen on the setup screen.
 *
 * @param oversLimit null = classic unlimited (bat till out); otherwise an innings ALSO ends
 *   once [oversLimit] * 6 balls have been bowled. A 0 always ends the innings (single wicket).
 * @param battingFirst set by the players after their own real-world coin toss.
 * @param bookPages length of the "book". Kept even (default 400) so the even-page outcomes
 *   {0, 2, 4, 6, 8} come up exactly uniformly.
 */
@Serializable
@Parcelize
data class MatchConfig(
    val playerOneName: String,
    val playerTwoName: String,
    val battingFirst: Player,
    val oversLimit: Int?,          // null == unlimited
    val bookPages: Int = 400
) : Parcelable

/** Outcome of one flip. [runs] is 0 when [isOut]; [page] is always even (odd pages are reflipped). */
@Serializable
@Parcelize
data class BallResult(val page: Int, val runs: Int, val isOut: Boolean) : Parcelable

/** State of a single innings. */
@Serializable
@Parcelize
data class InningsState(
    val batter: Player,
    val runs: Int = 0,
    val ballsBowled: Int = 0,
    val isOut: Boolean = false
) : Parcelable

/** Whole-match state. Immutable — each ball returns a new copy. */
@Serializable
@Parcelize
data class GameState(
    val config: MatchConfig,
    val phase: Phase,
    val innings1: InningsState,
    val innings2: InningsState,
    val lastBall: BallResult? = null,
    val target: Int? = null        // runs the chaser needs to WIN (score1 + 1); set at the break
) : Parcelable

/** Final result of the match. */
sealed interface MatchOutcome {
    data class Win(val winner: Player) : MatchOutcome
    data object Tie : MatchOutcome
    data object InProgress : MatchOutcome
}

object GameLogic {

    /** A page's last digit of 0 means the batter is out. */
    fun isOut(page: Int): Boolean = page % 10 == 0

    /** Runs for a page = its last digit (always even here); 0 when out. */
    fun runsForPage(page: Int): Int = page % 10

    /** Draw a page in 1..[bookPages], reflipping until it is even. */
    fun drawEvenPage(rng: Random, bookPages: Int): Int {
        var page: Int
        do {
            page = rng.nextInt(bookPages) + 1
        } while (page % 2 != 0)
        return page
    }

    /** Resolve one flip into a [BallResult]. */
    fun flip(rng: Random, bookPages: Int): BallResult {
        val page = drawEvenPage(rng, bookPages)
        val out = isOut(page)
        return BallResult(page = page, runs = if (out) 0 else runsForPage(page), isOut = out)
    }

    /** Build the initial state from setup config; the batting-first player owns innings 1. */
    fun startMatch(config: MatchConfig): GameState {
        val first = config.battingFirst
        val second = if (first == Player.ONE) Player.TWO else Player.ONE
        return GameState(
            config = config,
            phase = Phase.INNINGS_1,
            innings1 = InningsState(batter = first),
            innings2 = InningsState(batter = second)
        )
    }

    /** True when an innings is over: out (single wicket) OR the overs cap is reached. */
    private fun inningsEnded(innings: InningsState, oversLimit: Int?): Boolean {
        if (innings.isOut) return true
        if (oversLimit != null && innings.ballsBowled >= oversLimit * 6) return true
        return false
    }

    /**
     * Play one ball. Only valid in [Phase.INNINGS_1] or [Phase.INNINGS_2]; a no-op otherwise.
     * Handles run/out resolution, the single-wicket rule, the overs cap, and the chase win-on-target.
     */
    fun playBall(state: GameState, rng: Random): GameState {
        val ball = flip(rng, state.config.bookPages)
        return when (state.phase) {
            Phase.INNINGS_1 -> {
                val updated = state.innings1.copy(
                    runs = state.innings1.runs + ball.runs,
                    ballsBowled = state.innings1.ballsBowled + 1,
                    isOut = ball.isOut
                )
                val ended = inningsEnded(updated, state.config.oversLimit)
                state.copy(
                    innings1 = updated,
                    lastBall = ball,
                    phase = if (ended) Phase.INNINGS_BREAK else Phase.INNINGS_1,
                    target = if (ended) updated.runs + 1 else state.target
                )
            }
            Phase.INNINGS_2 -> {
                val updated = state.innings2.copy(
                    runs = state.innings2.runs + ball.runs,
                    ballsBowled = state.innings2.ballsBowled + 1,
                    isOut = ball.isOut
                )
                val target = state.target ?: (state.innings1.runs + 1)
                val chased = updated.runs >= target
                val ended = chased || inningsEnded(updated, state.config.oversLimit)
                state.copy(
                    innings2 = updated,
                    lastBall = ball,
                    phase = if (ended) Phase.RESULT else Phase.INNINGS_2
                )
            }
            else -> state
        }
    }

    /** Move from the innings break into the chase. */
    fun startSecondInnings(state: GameState): GameState =
        if (state.phase == Phase.INNINGS_BREAK) {
            state.copy(phase = Phase.INNINGS_2, lastBall = null)
        } else {
            state
        }

    /** Current match outcome. */
    fun outcome(state: GameState): MatchOutcome {
        if (state.phase != Phase.RESULT) return MatchOutcome.InProgress
        val s1 = state.innings1.runs
        val s2 = state.innings2.runs
        return when {
            s2 > s1 -> MatchOutcome.Win(state.innings2.batter)
            s1 > s2 -> MatchOutcome.Win(state.innings1.batter)
            else -> MatchOutcome.Tie
        }
    }
}
