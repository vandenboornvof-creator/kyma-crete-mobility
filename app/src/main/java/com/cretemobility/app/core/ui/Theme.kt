package com.cretemobility.app.core.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// ─── Crete Mobility Brand Colors ─────────────────────────────────────────────
// Inspired by Cretan sea blues, warm Mediterranean sunset tones

object CreteMobilityColors {
    // Primary: deep Aegean blue
    val AegeanBlue = Color(0xFF1565C0)
    val AegeanBlueLight = Color(0xFF1E88E5)
    val AegeanBlueDark = Color(0xFF0D47A1)

    // Secondary: warm terracotta / Minoan ochre
    val Terracotta = Color(0xFFE65100)
    val TerracottaLight = Color(0xFFFF8F00)

    // Surface: warm off-white (like Cretan limestone)
    val Limestone = Color(0xFFFAF8F5)
    val LimestoneDark = Color(0xFF1C1B1F)

    // Transit mode colors
    val BusBlue = Color(0xFF1565C0)
    val FerryTeal = Color(0xFF00695C)
    val WalkGreen = Color(0xFF2E7D32)
    val TaxiYellow = Color(0xFFF9A825)

    // Status colors
    val OnTime = Color(0xFF2E7D32)
    val Delayed = Color(0xFFF57C00)
    val Cancelled = Color(0xFFC62828)
    val NearlyDue = Color(0xFFE65100)

    // Map colors
    val RoutePolyline = Color(0xFF1565C0)
    val WalkPolyline = Color(0xFF2E7D32)
    val StopMarker = Color(0xFFFFFFFF)
    val StopMarkerBorder = Color(0xFF1565C0)
}

// ─── Light color scheme ───────────────────────────────────────────────────────

val CreteLightColorScheme = lightColorScheme(
    primary = CreteMobilityColors.AegeanBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6E4FF),
    onPrimaryContainer = Color(0xFF001A43),
    secondary = CreteMobilityColors.Terracotta,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFDCC8),
    onSecondaryContainer = Color(0xFF341200),
    tertiary = Color(0xFF006A67),
    onTertiary = Color.White,
    background = CreteMobilityColors.Limestone,
    onBackground = Color(0xFF1C1B1F),
    surface = Color.White,
    onSurface = Color(0xFF1C1B1F),
    surfaceVariant = Color(0xFFE7E0EC),
    onSurfaceVariant = Color(0xFF49454F),
    outline = Color(0xFF79747E),
    error = Color(0xFFB3261E),
    onError = Color.White,
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B)
)

// ─── Dark color scheme ────────────────────────────────────────────────────────

val CreteDarkColorScheme = darkColorScheme(
    primary = Color(0xFFADC6FF),
    onPrimary = Color(0xFF002E69),
    primaryContainer = Color(0xFF004494),
    onPrimaryContainer = Color(0xFFD6E4FF),
    secondary = Color(0xFFFFB58B),
    onSecondary = Color(0xFF552200),
    background = Color(0xFF1C1B1F),
    onBackground = Color(0xFFE6E1E5),
    surface = Color(0xFF1C1B1F),
    onSurface = Color(0xFFE6E1E5),
    error = Color(0xFFF2B8B5),
    onError = Color(0xFF601410)
)

// ─── Typography ───────────────────────────────────────────────────────────────

val CreteMobilityTypography = Typography(
    displayLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 57.sp,
        lineHeight = 64.sp
    ),
    displayMedium = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 45.sp,
        lineHeight = 52.sp
    ),
    headlineLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 40.sp
    ),
    headlineMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 36.sp
    ),
    headlineSmall = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 32.sp
    ),
    titleLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp
    ),
    titleSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    bodySmall = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp
    ),
    labelLarge = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    ),
    labelSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
)

// ─── Theme composable ─────────────────────────────────────────────────────────

@Composable
fun CreteMobilityTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) CreteDarkColorScheme else CreteLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = CreteMobilityTypography,
        content = content
    )
}
