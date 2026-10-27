package app.neon.com.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import app.neon.com.R

// ==========================================
// 🔤 GOOGLE FONTS: UNDUH FONT "INTER" SECARA LIVE
// ==========================================

val googleFontProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

val interGoogleFont = GoogleFont("Inter")

// FontFamily Inter Resmi dengan Variasi Ketebalan Lengkap
val InterFontFamily = FontFamily(
    Font(googleFont = interGoogleFont, fontProvider = googleFontProvider, weight = FontWeight.Light),
    Font(googleFont = interGoogleFont, fontProvider = googleFontProvider, weight = FontWeight.Normal),
    Font(googleFont = interGoogleFont, fontProvider = googleFontProvider, weight = FontWeight.Medium),
    Font(googleFont = interGoogleFont, fontProvider = googleFontProvider, weight = FontWeight.SemiBold),
    Font(googleFont = interGoogleFont, fontProvider = googleFontProvider, weight = FontWeight.Bold),
    Font(googleFont = interGoogleFont, fontProvider = googleFontProvider, weight = FontWeight.ExtraBold)
)

// ==========================================
// 🍎 TYPOGRAPHY iOS DENGAN FONT INTER UTAMA
// ==========================================

val IosTypography = Typography(
    headlineLarge = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 34.sp,
        lineHeight = 41.sp,
        letterSpacing = 0.37.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        letterSpacing = 0.36.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.35.sp
    ),
    titleLarge = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 25.sp,
        letterSpacing = 0.38.sp
    ),
    titleMedium = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
        lineHeight = 22.sp,
        letterSpacing = (-0.41).sp
    ),
    titleSmall = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        letterSpacing = (-0.24).sp
    ),
    bodyLarge = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = (-0.32).sp
    ),
    bodyMedium = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        letterSpacing = (-0.24).sp
    ),
    bodySmall = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = (-0.08).sp
    ),
    labelLarge = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        letterSpacing = (-0.24).sp
    ),
    labelMedium = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.sp
    ),
    labelSmall = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp,
        lineHeight = 13.sp,
        letterSpacing = 0.07.sp
    )
)

@Immutable
data class IosSpecificTypography(
    val largeTitle: TextStyle = IosTypography.headlineLarge,
    val title1: TextStyle = IosTypography.headlineMedium,
    val title2: TextStyle = IosTypography.headlineSmall,
    val title3: TextStyle = IosTypography.titleLarge,
    val headline: TextStyle = IosTypography.titleMedium,
    val subheadline: TextStyle = IosTypography.titleSmall,
    val body: TextStyle = IosTypography.bodyLarge,
    val callout: TextStyle = IosTypography.bodyMedium,
    val footnote: TextStyle = IosTypography.bodySmall,
    val caption1: TextStyle = IosTypography.labelMedium,
    val caption2: TextStyle = IosTypography.labelSmall
)

val LocalIosTypography = staticCompositionLocalOf { IosSpecificTypography() }
