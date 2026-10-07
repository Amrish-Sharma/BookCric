package com.codebuzz.app.bookcric.online

import com.codebuzz.app.bookcric.game.BallResult
import com.codebuzz.app.bookcric.game.MatchConfig
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Messages exchanged between the two phones in an online match.
 *
 * Both phones run [com.codebuzz.app.bookcric.game.GameLogic] locally; only the inputs travel.
 * Whoever is batting draws the ball on their own phone and sends it, so both sides apply the
 * exact same [BallResult] in the same order.
 */
@Serializable
sealed interface NetMessage {

    /** Host → guest: the match settings. The guest takes [com.codebuzz.app.bookcric.game.Player.TWO]. */
    @Serializable
    @SerialName("start")
    data class Start(val config: MatchConfig) : NetMessage

    /** Batter → opponent: the page the batter just flipped to. */
    @Serializable
    @SerialName("ball")
    data class Ball(val ball: BallResult) : NetMessage

    /** Either side: the chase has begun. */
    @Serializable
    @SerialName("chase")
    data object StartChase : NetMessage

    /** Either side: back to the lobby for another match, staying connected. */
    @Serializable
    @SerialName("rematch")
    data object Rematch : NetMessage

    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        fun encode(message: NetMessage): ByteArray = json.encodeToString(serializer(), message).encodeToByteArray()

        /** Returns null for bytes that aren't a message we understand. */
        fun decode(bytes: ByteArray): NetMessage? =
            runCatching { json.decodeFromString(serializer(), bytes.decodeToString()) }.getOrNull()
    }
}
