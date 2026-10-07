package com.codebuzz.app.bookcric.online

import com.codebuzz.app.bookcric.game.BallResult
import com.codebuzz.app.bookcric.game.MatchConfig
import com.codebuzz.app.bookcric.game.Player
import org.junit.Assert.*
import org.junit.Test

class NetMessageTest {

    private fun roundTrip(message: NetMessage) = NetMessage.decode(NetMessage.encode(message))

    @Test
    fun everyMessageSurvivesTheWire() {
        val config = MatchConfig("Host", "Guest", Player.TWO, oversLimit = null, localPlayer = Player.ONE)
        listOf(
            NetMessage.Start(config),
            NetMessage.Ball(BallResult(246, 6, false)),
            NetMessage.StartChase,
            NetMessage.Rematch
        ).forEach { assertEquals(it, roundTrip(it)) }
    }

    @Test
    fun garbageDecodesToNull() {
        assertNull(NetMessage.decode("not json".encodeToByteArray()))
        assertNull(NetMessage.decode("""{"type":"future-thing"}""".encodeToByteArray()))
    }
}
