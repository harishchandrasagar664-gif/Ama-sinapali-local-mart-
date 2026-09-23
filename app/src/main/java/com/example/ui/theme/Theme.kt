package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val LocalTealPrimary = Color(0xFF00796B)
val LocalTealLight = Color(0xFF48A999)
val LocalTealDark = Color(0xFF004C40)
val LocalAmberSecondary = Color(0xFFFFB300)
val LocalAmberLight = Color(0xFFFFE54C)
val LocalAmberDark = Color(0xFFC68400)
val LocalIndigoTertiary = Color(0xFF3F51B5)
val LocalIndigoLight = Color(0xFF757DE8)
val LocalBackgroundLight = Color(0xFFF7F9FA)
val LocalSurfaceLight = Color(0xFFFFFFFF)
val LocalSurfaceVariantLight = Color(0xFFEDEFEF)
val LocalTextPrimary = Color(0xFF1E293B)
val LocalTextSecondary = Color(0xFF5E6E82)
val LocalTextMuted = Color(0xFF8F9BB3)
val LocalSuccess = Color(0xFF2E7D32)
val LocalWarning = Color(0xFFF57F17)
val LocalError = Color(0xFFD32F2F)
val LocalInfo = Color(0xFF0288D1)
val LocalDivider = Color(0xFFE2E8F0)
val LocalCardBorder = Color(0xFFDBE2EA)
val LocalBadgeBg = Color(0xFFE0F2F1)
val LocalBadgeText = Color(0xFF004D40)

val Typography = Typography(
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    )
)

private val DarkColorScheme = darkColorScheme(
    primary = LocalTealLight,
    onPrimary = Color.White,
    primaryContainer = LocalTealDark,
    secondary = LocalAmberLight,
    onSecondary = Color.Black,
    secondaryContainer = LocalAmberDark,
    tertiary = LocalIndigoLight
)

private val LightColorScheme = lightColorScheme(
    primary = LocalTealPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0F2F1),
    secondary = LocalAmberSecondary,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFF8E1),
    tertiary = LocalIndigoTertiary,
    onTertiary = Color.White,
    background = LocalBackgroundLight,
    onBackground = LocalTextPrimary,
    surface = LocalSurfaceLight,
    onSurface = LocalTextPrimary,
    surfaceVariant = LocalSurfaceVariantLight,
    onSurfaceVariant = LocalTextSecondary,
    outline = LocalDivider
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
