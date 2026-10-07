package com.codebuzz.app.bookcric.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.codebuzz.app.bookcric.game.GameLogic
import com.codebuzz.app.bookcric.game.GameState
import com.codebuzz.app.bookcric.game.MatchOutcome
import com.codebuzz.app.bookcric.game.Player
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.math.max

/**
 * Records kept on this phone. [highScore] is the best innings by anyone who has batted on it;
 * wins, losses and streaks count only matches with a clear "you" — vs Computer and online.
 */
data class Stats(
    val highScore: Int = 0,
    val highScoreBy: String? = null,
    val wins: Int = 0,
    val losses: Int = 0,
    val ties: Int = 0,
    val streak: Int = 0,
    val bestStreak: Int = 0,
    val soundEnabled: Boolean = true
)

private val Context.statsStore by preferencesDataStore(name = "stats")

class StatsRepository(private val context: Context) {

    val stats: Flow<Stats> = context.statsStore.data.map { prefs ->
        Stats(
            highScore = prefs[HIGH_SCORE] ?: 0,
            highScoreBy = prefs[HIGH_SCORE_BY],
            wins = prefs[WINS] ?: 0,
            losses = prefs[LOSSES] ?: 0,
            ties = prefs[TIES] ?: 0,
            streak = prefs[STREAK] ?: 0,
            bestStreak = prefs[BEST_STREAK] ?: 0,
            soundEnabled = prefs[SOUND_ENABLED] ?: true
        )
    }

    /** Record a finished match. Returns true when one of its innings set a new high score. */
    suspend fun recordMatch(state: GameState): Boolean {
        var newHighScore = false
        context.statsStore.edit { prefs ->
            val best = listOf(state.innings1, state.innings2).maxBy { it.runs }
            newHighScore = best.runs > (prefs[HIGH_SCORE] ?: 0)
            if (newHighScore) {
                prefs[HIGH_SCORE] = best.runs
                prefs[HIGH_SCORE_BY] = state.config.nameOf(best.batter)
            }

            // Pass-and-play has no "you" to keep a win-loss record for.
            val me = state.config.localPlayer ?: if (state.config.vsComputer) Player.ONE else return@edit
            when (val outcome = GameLogic.outcome(state)) {
                is MatchOutcome.Win -> if (outcome.winner == me) {
                    val streak = (prefs[STREAK] ?: 0) + 1
                    prefs[WINS] = (prefs[WINS] ?: 0) + 1
                    prefs[STREAK] = streak
                    prefs[BEST_STREAK] = max(streak, prefs[BEST_STREAK] ?: 0)
                } else {
                    prefs[LOSSES] = (prefs[LOSSES] ?: 0) + 1
                    prefs[STREAK] = 0
                }
                // A tie neither extends nor breaks a streak.
                MatchOutcome.Tie -> prefs[TIES] = (prefs[TIES] ?: 0) + 1
                MatchOutcome.InProgress -> Unit
            }
        }
        return newHighScore
    }

    suspend fun setSoundEnabled(enabled: Boolean) {
        context.statsStore.edit { it[SOUND_ENABLED] = enabled }
    }

    private companion object {
        val HIGH_SCORE = intPreferencesKey("high_score")
        val HIGH_SCORE_BY = stringPreferencesKey("high_score_by")
        val WINS = intPreferencesKey("wins")
        val LOSSES = intPreferencesKey("losses")
        val TIES = intPreferencesKey("ties")
        val STREAK = intPreferencesKey("streak")
        val BEST_STREAK = intPreferencesKey("best_streak")
        val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
    }
}
