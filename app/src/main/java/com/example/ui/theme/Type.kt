package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

val OrbitronFontFamily = FontFamily(
    Font(R.font.orbitron, FontWeight.Normal),
    Font(R.font.orbitron, FontWeight.Medium),
    Font(R.font.orbitron, FontWeight.Bold)
)

val JakartaSansFontFamily = FontFamily(
    Font(R.font.plus_jakarta_sans, FontWeight.Normal),
    Font(R.font.plus_jakarta_sans, FontWeight.Medium),
    Font(R.font.plus_jakarta_sans, FontWeight.SemiBold),
    Font(R.font.plus_jakarta_sans, FontWeight.Bold)
)

val JetBrainsMonoFontFamily = FontFamily(
    Font(R.font.jetbrains_mono, FontWeight.Normal),
    Font(R.font.jetbrains_mono, FontWeight.Medium),
    Font(R.font.jetbrains_mono, FontWeight.Bold)
)

fun buildShinobiTypography(fontScale: Float = 1f): Typography {
    val s = fontScale.coerceIn(0.85f, 1.35f)
    return Typography(
        displaySmall = TextStyle(
            fontFamily = OrbitronFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = (28f * s).sp,
            lineHeight = (34f * s).sp,
            letterSpacing = 0.8.sp
        ),
        headlineMedium = TextStyle(
            fontFamily = OrbitronFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = (22f * s).sp,
            lineHeight = (28f * s).sp,
            letterSpacing = 0.6.sp
        ),
        headlineSmall = TextStyle(
            fontFamily = OrbitronFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = (18f * s).sp,
            lineHeight = (24f * s).sp,
            letterSpacing = 0.5.sp
        ),
        titleLarge = TextStyle(
            fontFamily = JakartaSansFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = (18f * s).sp,
            lineHeight = (24f * s).sp
        ),
        titleMedium = TextStyle(
            fontFamily = JakartaSansFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = (15f * s).sp,
            lineHeight = (21f * s).sp
        ),
        titleSmall = TextStyle(
            fontFamily = OrbitronFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = (12f * s).sp,
            lineHeight = (16f * s).sp,
            letterSpacing = 0.8.sp
        ),
        bodyLarge = TextStyle(
            fontFamily = JakartaSansFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = (15f * s).sp,
            lineHeight = (22f * s).sp
        ),
        bodyMedium = TextStyle(
            fontFamily = JakartaSansFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = (13.5f * s).sp,
            lineHeight = (19f * s).sp
        ),
        bodySmall = TextStyle(
            fontFamily = JakartaSansFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = (12f * s).sp,
            lineHeight = (16f * s).sp
        ),
        labelLarge = TextStyle(
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = (13f * s).sp,
            lineHeight = (18f * s).sp,
            letterSpacing = 0.4.sp
        ),
        labelMedium = TextStyle(
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = (11.5f * s).sp,
            lineHeight = (15f * s).sp,
            letterSpacing = 0.3.sp
        ),
        labelSmall = TextStyle(
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = (10.5f * s).sp,
            lineHeight = (14f * s).sp
        )
    )
}

val Typography = buildShinobiTypography(1f)
