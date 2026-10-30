package app.neon.com.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import app.neon.com.R

val InterFontFamily = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_bold, FontWeight.Bold)
)

private fun interStyle(weight: FontWeight, size: Int, lineHeight: Int) = TextStyle(
    fontFamily = InterFontFamily,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp
)

val IosTypography = Typography(
    displayLarge = interStyle(FontWeight.Bold, 34, 41),
    displayMedium = interStyle(FontWeight.Bold, 28, 34),
    displaySmall = interStyle(FontWeight.Bold, 24, 30),
    headlineLarge = interStyle(FontWeight.Bold, 22, 28),
    headlineMedium = interStyle(FontWeight.Bold, 20, 26),
    headlineSmall = interStyle(FontWeight.Bold, 18, 24),
    titleLarge = interStyle(FontWeight.Bold, 17, 22),
    titleMedium = interStyle(FontWeight.Bold, 16, 22),
    titleSmall = interStyle(FontWeight.Bold, 14, 20),
    bodyLarge = interStyle(FontWeight.Normal, 16, 22),
    bodyMedium = interStyle(FontWeight.Normal, 14, 20),
    bodySmall = interStyle(FontWeight.Normal, 12, 16),
    labelLarge = interStyle(FontWeight.Bold, 16, 22),
    labelMedium = interStyle(FontWeight.Bold, 12, 16),
    labelSmall = interStyle(FontWeight.Normal, 11, 14)
)

val LocalIosTypography = staticCompositionLocalOf { IosTypography }

@Composable
fun IosSpecificTypography(): Typography = IosTypography
