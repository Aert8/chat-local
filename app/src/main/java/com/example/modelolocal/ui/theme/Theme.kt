package com.example.modelolocal.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Paleta oscura — grises neutros con acento ámbar dorado
val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFE2B350),
    onPrimary = Color(0xFF1C1B18),
    primaryContainer = Color(0xFF3D3526),
    onPrimaryContainer = Color(0xFFF5E6B8),
    secondary = Color(0xFF8AB4F8),
    onSecondary = Color(0xFF1A1A1A),
    secondaryContainer = Color(0xFF293042),
    onSecondaryContainer = Color(0xFFD0E4FF),
    tertiary = Color(0xFF81C995),
    surface = Color(0xFF1E1E1E),
    onSurface = Color(0xFFE0E0E0),
    surfaceVariant = Color(0xFF2A2A2A),
    onSurfaceVariant = Color(0xFFBDBDBD),
    background = Color(0xFF121212),
    onBackground = Color(0xFFE8E8E8),
    error = Color(0xFFEF5350),
    onError = Color(0xFF1A1A1A),
    outline = Color(0xFF3A3A3A),
    outlineVariant = Color(0xFF2C2C2C),
    inverseSurface = Color(0xFFE8E8E8),
    inverseOnSurface = Color(0xFF1E1E1E),
)

// Colores semánticos para toda la UI
object ChatColors {
    // Mensajes
    val UserMessageBackground = Color(0xFF2A2A2A)
    val UserMessageText = Color(0xFFE8E8E8)
    val AssistantMessageText = Color(0xFFD4D4D4)

    // Input
    val InputBackground = Color(0xFF1E1E1E)
    val InputBorder = Color(0xFF3A3A3A)

    // Drawer
    val DrawerBackground = Color(0xFF161616)
    val DrawerItemSelected = Color(0xFF2A2A2A)

    // Acentos
    val Accent = Color(0xFFE2B350)
    val AccentSecondary = Color(0xFF8AB4F8)

    // Texto secundario
    val MutedText = Color(0xFF808080)

    // Markdown — bloques de código
    val CodeBlockBackground = Color(0xFF1A1A1A)
    val CodeBlockBorder = Color(0xFF333333)
    val CodeText = Color(0xFF81C995)
    val InlineCodeBackground = Color(0xFF252525)
    val InlineCodeText = Color(0xFFE2B350)

    // Markdown — otros elementos
    val BlockquoteBorder = Color(0xFFE2B350)
    val BlockquoteText = Color(0xFFB0B0B0)
    val LinkColor = Color(0xFF8AB4F8)
    val DividerColor = Color(0xFF2A2A2A)

    // Labels de rol
    val RoleLabelUser = Color(0xFFE2B350)
    val RoleLabelAssistant = Color(0xFF808080)

    // Estado
    val StatusReady = Color(0xFF81C995)
    val StatusLoading = Color(0xFFE2B350)
}

val AppTypography = Typography(
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
)

@Composable
fun ModeloLocalTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = AppTypography,
        content = content
    )
}
