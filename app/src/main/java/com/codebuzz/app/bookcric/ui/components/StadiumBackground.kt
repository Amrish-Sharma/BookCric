package com.codebuzz.app.bookcric.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.codebuzz.app.bookcric.ui.theme.Chalk
import com.codebuzz.app.bookcric.ui.theme.Floodlight
import com.codebuzz.app.bookcric.ui.theme.NightSky
import com.codebuzz.app.bookcric.ui.theme.Outfield

/** Night sky falling to a lit outfield, with a floodlight tower glowing in each top corner. */
@Composable
fun StadiumBackground(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    // Nothing here is a Material surface, so say what colour text on the ground defaults to.
    CompositionLocalProvider(LocalContentColor provides Chalk) {
        StadiumGround(modifier, content)
    }
}

@Composable
private fun StadiumGround(modifier: Modifier, content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier
            .fillMaxSize()
            .drawBehind {
                drawRect(Brush.verticalGradient(0f to NightSky, 0.55f to NightSky, 1f to Outfield))
                val glow = size.width * 0.75f
                listOf(Offset(0f, 0f), Offset(size.width, 0f)).forEach { tower ->
                    drawCircle(
                        brush = Brush.radialGradient(
                            0f to Floodlight.copy(alpha = 0.16f),
                            1f to Color.Transparent,
                            center = tower,
                            radius = glow
                        ),
                        radius = glow,
                        center = tower
                    )
                }
            },
        content = content
    )
}
