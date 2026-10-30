package app.neon.com.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.neon.com.ui.theme.InterFontFamily
import app.neon.com.ui.theme.bouncyClickable

private val BackgroundColor = Color(0xFF1C1C1E)
private val SurfaceColor = Color(0xFF2C2C2E)
private val PrimaryColor = Color(0xFF0A84FF)
private val SecondaryTextColor = Color(0xFF8E8E93)
private val ErrorColor = Color(0xFFFF453A)
private val UsernameRegex = Regex("^[a-z0-9_]+$")

@Composable
fun CreateUsernameScreen(
    authViewModel: AuthViewModel,
    onUsernameCreated: () -> Unit = {}
) {
    var username by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = Color.White,
        unfocusedTextColor = Color.White,
        focusedBorderColor = PrimaryColor,
        unfocusedBorderColor = SecondaryTextColor,
        errorBorderColor = ErrorColor,
        cursorColor = PrimaryColor,
        focusedLabelColor = PrimaryColor,
        unfocusedLabelColor = SecondaryTextColor,
        errorLabelColor = ErrorColor,
        focusedContainerColor = BackgroundColor,
        unfocusedContainerColor = BackgroundColor,
        errorContainerColor = BackgroundColor
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundColor)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceColor, RoundedCornerShape(20.dp))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Pilih Username",
                color = Color.White,
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Username ini akan terlihat oleh pengguna lain.",
                color = SecondaryTextColor,
                fontFamily = InterFontFamily,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = username,
                onValueChange = {
                    username = it
                    errorMessage = null
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Username (contoh: userneon1000)", fontFamily = InterFontFamily) },
                singleLine = true,
                isError = errorMessage != null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                shape = RoundedCornerShape(12.dp),
                colors = fieldColors
            )

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = errorMessage.orEmpty(),
                    color = ErrorColor,
                    fontFamily = InterFontFamily,
                    fontSize = 13.sp,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .alpha(if (isLoading) 0.6f else 1f)
                    .background(PrimaryColor, RoundedCornerShape(12.dp))
                    .bouncyClickable {
                        if (isLoading) return@bouncyClickable
                        val cleanUsername = username.trim()
                        when {
                            cleanUsername.length < 3 -> {
                                errorMessage = "Username minimal 3 karakter."
                            }
                            !UsernameRegex.matches(cleanUsername) -> {
                                errorMessage = "Username hanya boleh huruf kecil, angka, dan underscore."
                            }
                            else -> {
                                errorMessage = null
                                isLoading = true
                                authViewModel.saveUsername(
                                    username = cleanUsername,
                                    onSuccess = {
                                        isLoading = false
                                        onUsernameCreated()
                                    },
                                    onError = { message ->
                                        isLoading = false
                                        errorMessage = message
                                    }
                                )
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "Lanjutkan",
                        color = Color.White,
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}
