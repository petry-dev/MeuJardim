package br.com.meujardim.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Paleta: verde principal; amarelo, coral e lilás como complementares; fundos claros.
object Garden {
    val Green = Color(0xFF2E7D4F)
    val GreenDark = Color(0xFF1B5E3A)
    val GreenLight = Color(0xFFDDF0DF)
    val Leaf = Color(0xFF43A047)
    val Yellow = Color(0xFFF6C343)
    val YellowLight = Color(0xFFFFF3CF)
    val Coral = Color(0xFFFF7F6B)
    val CoralDark = Color(0xFFD9503C)
    val CoralLight = Color(0xFFFFE2DC)
    val Lilac = Color(0xFFB39DDB)
    val LilacDark = Color(0xFF6E56A8)
    val LilacLight = Color(0xFFEDE6FA)
    val Sky = Color(0xFF6BB7E8)
    val SkyLight = Color(0xFFE1F1FB)
    val Background = Color(0xFFF6FAF3)
    val Surface = Color(0xFFFFFFFF)
    val Ink = Color(0xFF1E2B22)
    val InkSoft = Color(0xFF5B6B60)
    val Outline = Color(0xFFDCE6D9)
}

private val colors = lightColorScheme(
    primary = Garden.Green,
    onPrimary = Color.White,
    primaryContainer = Garden.GreenLight,
    onPrimaryContainer = Garden.GreenDark,
    secondary = Garden.Coral,
    onSecondary = Color.White,
    secondaryContainer = Garden.CoralLight,
    onSecondaryContainer = Garden.CoralDark,
    tertiary = Garden.LilacDark,
    onTertiary = Color.White,
    tertiaryContainer = Garden.LilacLight,
    onTertiaryContainer = Garden.LilacDark,
    background = Garden.Background,
    onBackground = Garden.Ink,
    surface = Garden.Surface,
    onSurface = Garden.Ink,
    surfaceVariant = Color(0xFFEEF4EC),
    onSurfaceVariant = Garden.InkSoft,
    surfaceContainer = Color(0xFFF0F6EE),
    surfaceContainerLow = Color(0xFFF3F8F1),
    surfaceContainerHigh = Color(0xFFEAF2E8),
    outline = Garden.Outline,
    outlineVariant = Garden.Outline,
    error = Garden.CoralDark
)

private val base = Typography()
private val typography = Typography(
    headlineLarge = base.headlineLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
    headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.3).sp),
    headlineSmall = base.headlineSmall.copy(fontWeight = FontWeight.Bold),
    titleLarge = base.titleLarge.copy(fontWeight = FontWeight.SemiBold),
    titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
    titleSmall = base.titleSmall.copy(fontWeight = FontWeight.SemiBold),
    labelLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
)

private val shapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun MeuJardimTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = colors, typography = typography, shapes = shapes, content = content)
}
