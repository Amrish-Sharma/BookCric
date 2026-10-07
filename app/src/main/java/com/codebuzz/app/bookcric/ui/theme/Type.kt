package com.codebuzz.app.bookcric.ui.theme

import android.graphics.Typeface
import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** Tall, tight numerals like a ground's scoreboard. A system face, so no font files ship. */
val ScoreboardFont = FontFamily(Typeface.create("sans-serif-condensed", Typeface.BOLD))

/** Fixed-width digits, so a score doesn't jiggle sideways as it counts up. */
private const val TABULAR_NUMS = "tnum"

private val Base = Typography()

val Typography = Typography(
    displayLarge = Base.displayLarge.copy(
        fontFamily = ScoreboardFont,
        fontWeight = FontWeight.Black,
        fontFeatureSettings = TABULAR_NUMS
    ),
    displayMedium = Base.displayMedium.copy(
        fontFamily = ScoreboardFont,
        fontWeight = FontWeight.Black,
        fontFeatureSettings = TABULAR_NUMS
    ),
    displaySmall = Base.displaySmall.copy(
        fontFamily = ScoreboardFont,
        fontWeight = FontWeight.Black,
        fontFeatureSettings = TABULAR_NUMS
    ),
    headlineLarge = Base.headlineLarge.copy(fontFamily = ScoreboardFont, fontWeight = FontWeight.ExtraBold),
    headlineMedium = Base.headlineMedium.copy(fontFamily = ScoreboardFont, fontWeight = FontWeight.ExtraBold),
    headlineSmall = Base.headlineSmall.copy(fontFamily = ScoreboardFont, fontWeight = FontWeight.ExtraBold),
    titleLarge = Base.titleLarge.copy(fontWeight = FontWeight.Bold),
    titleMedium = Base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
    labelLarge = Base.labelLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
    labelMedium = Base.labelMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp),
    labelSmall = Base.labelSmall.copy(letterSpacing = 1.sp)
)
