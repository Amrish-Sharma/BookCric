package com.codebuzz.app.bookcric.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codebuzz.app.bookcric.game.BallResult
import com.codebuzz.app.bookcric.ui.theme.BoundaryRope
import com.codebuzz.app.bookcric.ui.theme.NightSky
import com.codebuzz.app.bookcric.ui.theme.RunEight
import com.codebuzz.app.bookcric.ui.theme.RunFour
import com.codebuzz.app.bookcric.ui.theme.RunSix
import com.codebuzz.app.bookcric.ui.theme.RunTwo
import com.codebuzz.app.bookcric.ui.theme.ScoreboardFont
import com.codebuzz.app.bookcric.ui.theme.Wicket

/** The colour a ball is shown in everywhere: chips, the landing stamp, recaps. */
fun ballColor(ball: BallResult): Color = when {
    ball.isOut -> Wicket
    ball.runs >= 8 -> RunEight
    ball.runs >= 6 -> RunSix
    ball.runs >= 4 -> RunFour
    else -> RunTwo
}

/** One ball of an over: its runs, "W" for a wicket, or an empty ring for a ball still to come. */
@Composable
fun BallChip(ball: BallResult?, modifier: Modifier = Modifier, size: Dp = 34.dp) {
    if (ball == null) {
        Box(modifier.size(size).border(1.5.dp, BoundaryRope, CircleShape))
    } else {
        Box(
            modifier.size(size).background(ballColor(ball), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (ball.isOut) "W" else ball.runs.toString(),
                color = NightSky,
                fontFamily = ScoreboardFont,
                fontSize = (size.value * 0.5f).sp
            )
        }
    }
}
