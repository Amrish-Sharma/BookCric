package com.codebuzz.app.bookcric.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

// A physical book keeps its paper colors regardless of the app theme.
private val Paper = Color(0xFFFBF6E9)
private val PageBlock = Color(0xFFE6DCC4)
private val Ink = Color(0xFF3B3326)
private val Cover = Color(0xFF6B2E2E)
private val RunInk = Color(0xFF1E6B3A)
private val OutInk = Color(0xFFB3261E)

/** Quick lift off the page, then a long, slowing fall as the sheet settles flat. */
private val PageTurnEasing = CubicBezierEasing(0.45f, 0.05f, 0.2f, 1f)

/** One printed side of a sheet. [highlight] marks the page a ball landed on. */
@Immutable
data class PageFace(val number: Int, val highlight: Boolean = false, val isOut: Boolean = false)

/** A sheet mid-turn: [angle] runs 0 (lying on the right) to 180 (lying on the left). */
@Stable
internal class TurningSheet(val id: Int, val front: PageFace, val back: PageFace) {
    val angle = Animatable(0f)
}

/**
 * State of an open book. Several sheets can be in flight at once, which is what makes a riffle
 * look like a thumb releasing a stack of pages rather than one card spinning.
 */
@Stable
class BookState(initialLeft: PageFace) {
    var left by mutableStateOf(initialLeft)
        private set
    var right by mutableStateOf(PageFace(initialLeft.number + 1))
        private set

    internal val sheets = mutableStateListOf<TurningSheet>()
    private var nextId = 0

    /**
     * Turn the current right-hand sheet over so that [target] (an even, left-hand page) ends up
     * face-up on the left. Suspends until the sheet lies flat.
     */
    suspend fun turnTo(target: PageFace, durationMillis: Int) {
        val sheet = TurningSheet(nextId++, front = right, back = target)
        // Lifting the sheet reveals the page underneath it straight away.
        right = PageFace(target.number + 1)
        sheets += sheet
        try {
            sheet.angle.animateTo(180f, tween(durationMillis, easing = PageTurnEasing))
            left = target
        } finally {
            sheets -= sheet
        }
    }
}

@Composable
fun rememberBookState(initialLeft: () -> PageFace): BookState = remember { BookState(initialLeft()) }

/** An open book whose pages turn in 3D around the spine. Drive it with [BookState.turnTo]. */
@Composable
fun FlipBook(state: BookState, modifier: Modifier = Modifier) {
    Box(
        modifier
            .aspectRatio(1.5f)
            .clip(RoundedCornerShape(10.dp))
            .background(Cover)
            .padding(start = 6.dp, end = 6.dp, top = 6.dp, bottom = 10.dp)
    ) {
        // Edge of the page block peeking out below the spread gives the book some thickness.
        Box(
            Modifier
                .matchParentSize()
                .offset(y = 3.dp)
                .background(PageBlock, RoundedCornerShape(3.dp))
        )

        Row(Modifier.fillMaxSize()) {
            PageSide(state.left, isLeft = true, Modifier.weight(1f).fillMaxHeight())
            PageSide(state.right, isLeft = false, Modifier.weight(1f).fillMaxHeight())
        }

        // Shadows the lifted sheets cast on the pages beneath them.
        Canvas(Modifier.matchParentSize().zIndex(1f)) {
            val half = size.width / 2f
            state.sheets.forEach { sheet ->
                val rad = sheet.angle.value * (PI.toFloat() / 180f)
                val lift = sin(rad)
                if (lift < 0.01f) return@forEach
                // Footprint of the sheet plus a soft penumbra that widens as it rises.
                val reach = (half * (abs(cos(rad)) + 0.35f * lift)).coerceAtMost(half)
                val shadow = Color.Black.copy(alpha = 0.3f * lift)
                val onRight = sheet.angle.value < 90f
                val endX = if (onRight) half + reach else half - reach
                drawRect(
                    brush = Brush.horizontalGradient(
                        0f to shadow,
                        1f to Color.Transparent,
                        startX = half,
                        endX = endX
                    ),
                    topLeft = Offset(if (onRight) half else endX, 0f),
                    size = Size(reach, size.height)
                )
            }
        }

        state.sheets.forEachIndexed { index, sheet ->
            key(sheet.id) {
                TurningSheetView(sheet, index, Modifier.align(Alignment.CenterEnd))
            }
        }
    }
}

@Composable
private fun TurningSheetView(sheet: TurningSheet, index: Int, modifier: Modifier) {
    val showBack by remember(sheet) { derivedStateOf { sheet.angle.value > 90f } }

    Box(
        modifier
            // Still on the right, earlier sheets sit on top of later ones; once over on the
            // left the order flips, just like a real stack being thumbed through.
            .zIndex(if (showBack) 200f + index else 100f - index)
            .fillMaxHeight()
            .fillMaxWidth(0.5f)
            .graphicsLayer {
                transformOrigin = TransformOrigin(0f, 0.5f)
                // Negative so the free edge swings up toward the reader, not into the table.
                rotationY = -sheet.angle.value
                cameraDistance = 12f * density
            }
            .clip(RoundedCornerShape(topEnd = 3.dp, bottomEnd = 3.dp))
            .drawWithContent {
                drawContent()
                val lift = sin(sheet.angle.value * (PI.toFloat() / 180f))
                // Fake the paper's curl: shade at the spine, a sheen across the belly of the
                // bend, and a slight falloff toward the free edge. x = 0 is always the spine.
                drawRect(
                    Brush.horizontalGradient(
                        0f to Color.Black.copy(alpha = 0.3f * lift),
                        0.3f to Color.Transparent,
                        0.7f to Color.White.copy(alpha = 0.25f * lift),
                        1f to Color.Black.copy(alpha = 0.12f * lift)
                    )
                )
                // A sheet standing upright catches less light than one lying flat.
                drawRect(Color.Black.copy(alpha = 0.12f * lift))
            }
    ) {
        if (showBack) {
            // The layer is rotated past 90°, so un-mirror the back face.
            PageSide(sheet.back, isLeft = true, Modifier.fillMaxSize().graphicsLayer { scaleX = -1f })
        } else {
            PageSide(sheet.front, isLeft = false, Modifier.fillMaxSize())
        }
    }
}

@Composable
private fun PageSide(face: PageFace, isLeft: Boolean, modifier: Modifier) {
    val shape = if (isLeft) {
        RoundedCornerShape(topStart = 3.dp, bottomStart = 3.dp)
    } else {
        RoundedCornerShape(topEnd = 3.dp, bottomEnd = 3.dp)
    }
    Box(
        modifier
            .clip(shape)
            .background(Paper)
            .drawBehind {
                // Placeholder "text", seeded by page number so a page always reads the same.
                val rng = Random(face.number)
                val marginX = 12.dp.toPx()
                val bottom = size.height - 12.dp.toPx()
                val gap = 7.dp.toPx()
                val thickness = 2.dp.toPx()
                val textWidth = size.width - 2 * marginX
                var y = 40.dp.toPx()
                while (y < bottom) {
                    val paragraphEnd = rng.nextInt(6) == 0
                    val width = textWidth * if (paragraphEnd) 0.3f + rng.nextFloat() * 0.4f else 0.9f + rng.nextFloat() * 0.1f
                    drawRoundRect(
                        color = Ink.copy(alpha = 0.13f),
                        topLeft = Offset(marginX, y),
                        size = Size(width, thickness),
                        cornerRadius = CornerRadius(thickness / 2)
                    )
                    y += if (paragraphEnd) gap * 1.6f else gap
                }
                // Gutter: paper curving down into the spine.
                val gutter = Color.Black.copy(alpha = 0.2f)
                drawRect(
                    if (isLeft) {
                        Brush.horizontalGradient(0.82f to Color.Transparent, 1f to gutter)
                    } else {
                        Brush.horizontalGradient(0f to gutter, 0.18f to Color.Transparent)
                    }
                )
            }
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        val digits = face.number.toString()
        val lastDigitColor = when {
            !face.highlight -> Ink
            face.isOut -> OutInk
            else -> RunInk
        }
        Text(
            text = buildAnnotatedString {
                append(digits.dropLast(1))
                withStyle(SpanStyle(color = lastDigitColor)) { append(digits.last()) }
            },
            color = Ink,
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold,
            fontSize = if (face.highlight) 24.sp else 15.sp,
            modifier = Modifier.align(if (isLeft) Alignment.TopStart else Alignment.TopEnd)
        )
    }
}
