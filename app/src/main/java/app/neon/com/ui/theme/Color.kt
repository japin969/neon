package app.neon.com.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// ==========================================
// 🎨 PALET WARNA RESMI iOS (APPLE HIG)
// ==========================================

// Primary & Secondary iOS
val IosBlue = Color(0xFF007AFF)           // Biru iOS Light
val IosBlueDark = Color(0xFF0A84FF)       // Biru iOS Dark
val IosGray = Color(0xFF8E8E93)           // Abu-abu Sistem iOS (Secondary Text)

// Background (True Black OLED di Dark Mode, Pure White di Light Mode)
val IosBackgroundDark = Color(0xFF000000)
val IosBackgroundLight = Color(0xFFFFFFFF)

// Surface & Card
val IosSurfaceDark = Color(0xFF1C1C1E)    // Surface Primer Dark (iOS System Gray 6)
val IosSurfaceLight = Color(0xFFF2F2F7)   // Surface Primer Light
val IosCardDark = Color(0xFF2C2C2E)       // Card Elevation Dark
val IosCardLight = Color(0xFFFFFFFF)      // Card Elevation Light

// Kontras Teks Bersih Standar iOS
val IosTextPrimaryDark = Color(0xFFFFFFFF)
val IosTextPrimaryLight = Color(0xFF000000)
val IosTextSecondaryDark = Color(0xFF8E8E93)
val IosTextSecondaryLight = Color(0xFF6C6C70)
val IosTextTertiaryDark = Color(0xFF48484A)
val IosTextTertiaryLight = Color(0xFFC7C7CC)

// Separators, Borders & Fills
val IosSeparatorDark = Color(0xFF38383A)
val IosSeparatorLight = Color(0xFFE5E5EA)
val IosFillDark = Color(0xFF767680).copy(alpha = 0.24f)
val IosFillLight = Color(0xFF767680).copy(alpha = 0.12f)
val IosChatBubbleReceivedDark = Color(0xFF262628)
val IosChatBubbleReceivedLight = Color(0xFFE9E9EB)

// System Accents
val IosGreen = Color(0xFF34C759)
val IosRed = Color(0xFFFF3B30)
val IosOrange = Color(0xFFFF9500)
val IosPink = Color(0xFFFF2D55)

@Immutable
data class IosCustomColors(
    val primary: Color,
    val secondary: Color,
    val background: Color,
    val surface: Color,
    val card: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val separator: Color,
    val searchBarFill: Color,
    val chatBubbleSent: Color,
    val chatBubbleReceived: Color,
    val accentGreen: Color,
    val accentRed: Color
)

val LocalIosColors = staticCompositionLocalOf {
    IosCustomColors(
        primary = IosBlue,
        secondary = IosGray,
        background = IosBackgroundDark,
        surface = IosSurfaceDark,
        card = IosCardDark,
        textPrimary = IosTextPrimaryDark,
        textSecondary = IosTextSecondaryDark,
        separator = IosSeparatorDark,
        searchBarFill = IosFillDark,
        chatBubbleSent = IosBlueDark,
        chatBubbleReceived = IosChatBubbleReceivedDark,
        accentGreen = IosGreen,
        accentRed = IosRed
    )
}
