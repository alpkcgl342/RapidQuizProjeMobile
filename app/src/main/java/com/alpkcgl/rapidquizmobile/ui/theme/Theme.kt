package com.alpkcgl.rapidquizmobile.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

/** Web ile aynı: yalnızca açık tema, dinamik renk kapalı (marka renkleri korunur). */
private val LightColors = lightColorScheme(
    primary = RqColors.Primary,
    onPrimary = RqColors.TextOnColor,
    primaryContainer = RqColors.PrimarySoft,
    onPrimaryContainer = RqColors.PrimaryStrong,
    secondary = RqColors.Accent,
    onSecondary = RqColors.TextOnColor,
    secondaryContainer = RqColors.AccentSoft,
    onSecondaryContainer = RqColors.Text,
    background = RqColors.Background,
    onBackground = RqColors.Text,
    surface = RqColors.Surface,
    onSurface = RqColors.Text,
    surfaceVariant = RqColors.SurfaceAlt,
    onSurfaceVariant = RqColors.TextMuted,
    surfaceContainerLowest = RqColors.Surface,
    surfaceContainerLow = RqColors.Surface,
    surfaceContainer = RqColors.Surface,
    surfaceContainerHigh = RqColors.SurfaceAlt,
    surfaceContainerHighest = RqColors.SurfaceAlt,
    outline = RqColors.Border,
    outlineVariant = RqColors.Border,
    error = RqColors.Danger,
    onError = RqColors.TextOnColor,
    errorContainer = RqColors.DangerSoft,
)

/** Köşe yarıçapları: 10 / 16 / 24 / 28 dp. */
object RqShapes {
    val Small = RoundedCornerShape(10.dp)
    val Medium = RoundedCornerShape(16.dp)
    val Large = RoundedCornerShape(24.dp)
    val ExtraLarge = RoundedCornerShape(28.dp)
    val Pill = RoundedCornerShape(999.dp)
}

@Composable
fun RapidQuizTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        typography = Typography,
        shapes = Shapes(
            small = RqShapes.Small,
            medium = RqShapes.Medium,
            large = RqShapes.Large,
            extraLarge = RqShapes.ExtraLarge,
        ),
        content = content,
    )
}
