package com.codebuzz.app.bookcric.game

import org.junit.Assert.*
import org.junit.Test
import kotlin.random.Random

class GameLogicTest {

    /** Scripted RNG: nextInt(until) returns queued values (ignoring `until`); empty -> 1 (=> even page 2). */
    private class QueueRandom(values: List<Int>) : Random() {
        private val q = ArrayDeque(values)
        override fun nextBits(bitCount: Int): Int = 0
        override fun nextInt(until: Int): Int = if (q.isEmpty()) 1 else q.removeFirst()
    }

    // Queue value (P - 1) to land on page P, since drawEvenPage does nextInt() + 1.
    private val OUT = 9                          // -> page 10
    private fun runVal(runs: Int) = runs - 1     // page == runs for even, non-out pages
    private val cfg = MatchConfig("A", "B", Player.ONE, oversLimit = null)

    @Test
    fun lastDigitZeroIsOut() {
        assertTrue(GameLogic.isOut(10))
        assertTrue(GameLogic.isOut(400))
        assertFalse(GameLogic.isOut(24))
    }

    @Test
    fun runsAreTheLastDigit() {
        assertEquals(4, GameLogic.runsForPage(24))
        assertEquals(8, GameLogic.runsForPage(8))
    }

    @Test
    fun flipReflipsOddPages() {
        // page 5 (odd -> reflip), then page 4 -> 4 runs
        val b = GameLogic.flip(QueueRandom(listOf(4, runVal(4))), 400)
        assertEquals(4, b.page)
        assertEquals(4, b.runs)
        assertFalse(b.isOut)
    }

    @Test
    fun flipResolvesOut() {
        val b = GameLogic.flip(QueueRandom(listOf(OUT)), 400)
        assertEquals(10, b.page)
        assertEquals(0, b.runs)
        assertTrue(b.isOut)
    }

    @Test
    fun startMatchSetsBattingOrder() {
        val s = GameLogic.startMatch(cfg)
        assertEquals(Player.ONE, s.innings1.batter)
        assertEquals(Player.TWO, s.innings2.batter)
        assertEquals(Phase.INNINGS_1, s.phase)

        val swapped = GameLogic.startMatch(cfg.copy(battingFirst = Player.TWO))
        assertEquals(Player.TWO, swapped.innings1.batter)
        assertEquals(Player.ONE, swapped.innings2.batter)
    }

    @Test
    fun outEndsInningsAndSetsTarget() {
        var s = GameLogic.startMatch(cfg)
        s = GameLogic.playBall(s, QueueRandom(listOf(runVal(4))))
        assertEquals(Phase.INNINGS_1, s.phase)
        assertEquals(4, s.innings1.runs)

        s = GameLogic.playBall(s, QueueRandom(listOf(OUT)))
        assertEquals(Phase.INNINGS_BREAK, s.phase)
        assertEquals(5, s.target)
    }

    @Test
    fun oversCapEndsInningsWithoutOut() {
        var s = GameLogic.startMatch(cfg.copy(oversLimit = 1)) // 6 balls
        repeat(6) { s = GameLogic.playBall(s, QueueRandom(listOf(runVal(2)))) }
        assertEquals(Phase.INNINGS_BREAK, s.phase)
        assertFalse(s.innings1.isOut)
        assertEquals(12, s.innings1.runs)
        assertEquals(13, s.target)
    }

    @Test
    fun singleWicketAppliesInOversMode() {
        var s = GameLogic.startMatch(cfg.copy(oversLimit = 5))
        s = GameLogic.playBall(s, QueueRandom(listOf(OUT)))
        assertEquals(Phase.INNINGS_BREAK, s.phase)
        assertEquals(1, s.target)
    }

    @Test
    fun chaserWinsOnReachingTarget() {
        var s = firstInningsOf4()
        s = GameLogic.startSecondInnings(s)
        s = GameLogic.playBall(s, QueueRandom(listOf(runVal(6)))) // 6 >= target 5
        assertEquals(Phase.RESULT, s.phase)
        assertEquals(MatchOutcome.Win(Player.TWO), GameLogic.outcome(s))
    }

    @Test
    fun firstBatterWinsWhenChaserOutShort() {
        var s = firstInningsOf4()
        s = GameLogic.startSecondInnings(s)
        s = GameLogic.playBall(s, QueueRandom(listOf(OUT)))       // out at 0
        assertEquals(Phase.RESULT, s.phase)
        assertEquals(MatchOutcome.Win(Player.ONE), GameLogic.outcome(s))
    }

    @Test
    fun levelChaseIsATie() {
        var s = firstInningsOf4()
        s = GameLogic.startSecondInnings(s)
        s = GameLogic.playBall(s, QueueRandom(listOf(runVal(4)))) // 4, short of target 5
        s = GameLogic.playBall(s, QueueRandom(listOf(OUT)))       // out, level at 4
        assertEquals(Phase.RESULT, s.phase)
        assertEquals(MatchOutcome.Tie, GameLogic.outcome(s))
    }

    @Test
    fun outcomesAreEvenAndOutIsOneInFive() {
        val r = Random(42)
        val counts = IntArray(10)
        val n = 200_000
        repeat(n) { counts[GameLogic.flip(r, 400).page % 10]++ }
        assertEquals(0, intArrayOf(1, 3, 5, 7, 9).sumOf { counts[it] })
        val pOut = counts[0].toDouble() / n
        assertTrue("P(out)=$pOut", pOut in 0.19..0.21)
    }

    @Test
    fun startSecondInningsClearsLastBall() {
        var s = firstInningsOf4()
        assertNotNull(s.lastBall)
        s = GameLogic.startSecondInnings(s)
        assertNull(s.lastBall)
    }

    private fun firstInningsOf4(): GameState {
        var s = GameLogic.startMatch(cfg)
        s = GameLogic.playBall(s, QueueRandom(listOf(runVal(4)))) // 4
        s = GameLogic.playBall(s, QueueRandom(listOf(OUT)))       // out -> target 5
        return s
    }
}
