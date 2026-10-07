package com.codebuzz.app.bookcric.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import com.codebuzz.app.bookcric.ui.theme.Chalk
import com.codebuzz.app.bookcric.ui.theme.Floodlight
import com.codebuzz.app.bookcric.ui.theme.RunEight
import com.codebuzz.app.bookcric.ui.theme.Turf
import com.codebuzz.app.bookcric.ui.theme.Twilight
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private const val PIECES = 80
private const val BURST_MILLIS = 1800
private val ConfettiColors = listOf(Floodlight, Turf, Chalk, RunEight, Twilight)

/** Fires bursts of confetti on a [Confetti] canvas. A new burst replaces one still in the air. */
@Stable
class ConfettiState {
    internal val progress = Animatable(1f)
    internal var seed by mutableIntStateOf(0)

    suspend fun fire() {
        seed++
        progress.snapTo(0f)
        progress.animateTo(1f, tween(BURST_MILLIS, easing = LinearEasing))
    }
}

/** Draws [state]'s current burst, thrown out from just above the centre. Empty between bursts. */
@Composable
fun Confetti(state: ConfettiState, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val t = state.progress.value
        if (t >= 1f) return@Canvas
        // Seeded per burst, so every frame rebuilds the same pieces on the same paths.
        val rng = Random(state.seed)
        val origin = Offset(size.width / 2f, size.height * 0.45f)
        val reach = size.width * 0.9f
        val fall = size.height * 0.6f
        val piece = 7.dp.toPx()
        // Fast out of the burst, then drag slows the spread while gravity takes over.
        val spread = 1f - (1f - t) * (1f - t)
        val alpha = 1f - t * t * t
        repeat(PIECES) { i ->
            val angle = rng.nextFloat() * 2f * PI.toFloat()
            val speed = 0.25f + rng.nextFloat() * 0.75f
            val spin = (rng.nextFloat() - 0.5f) * 1440f
            val aspect = 0.4f + rng.nextFloat() * 0.6f
            val x = origin.x + cos(angle) * speed * reach * spread
            val y = origin.y + sin(angle) * speed * reach * 0.7f * spread + fall * t * t
            rotate(spin * t, pivot = Offset(x, y)) {
                drawRect(
                    color = ConfettiColors[i % ConfettiColors.size].copy(alpha = alpha),
                    topLeft = Offset(x - piece / 2f, y - piece * aspect / 2f),
                    size = Size(piece, piece * aspect)
                )
            }
        }
    }
}
