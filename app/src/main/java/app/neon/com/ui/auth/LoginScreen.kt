package app.neon.com.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
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

@Composable
fun LoginScreen(
    authViewModel: AuthViewModel,
    onLoginSuccess: () -> Unit = {},
    onNavigateToRegister: () -> Unit = {}
) {
    val currentUser by authViewModel.currentUser.collectAsState()
    val errorMessage by authViewModel.errorMessage.collectAsState()
    val isLoading by authViewModel.isLoading.collectAsState()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    LaunchedEffect(currentUser) {
        if (currentUser != null) onLoginSuccess()
    }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = Color.White,
        unfocusedTextColor = Color.White,
        focusedBorderColor = PrimaryColor,
        unfocusedBorderColor = SecondaryTextColor,
        cursorColor = PrimaryColor,
        focusedLabelColor = PrimaryColor,
        unfocusedLabelColor = SecondaryTextColor,
        focusedContainerColor = BackgroundColor,
        unfocusedContainerColor = BackgroundColor
    )

    Box(
        modifier = Modifier.fillMaxSize().background(BackgroundColor).padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().background(SurfaceColor, RoundedCornerShape(20.dp)).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.size(72.dp).background(PrimaryColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "N", color = Color.White, fontFamily = InterFontFamily, fontWeight = FontWeight.Bold, fontSize = 32.sp)
            }

            Spacer(modifier = Modifier.height(20.dp))
            Text(text = "Selamat Datang", color = Color.White, fontFamily = InterFontFamily, fontWeight = FontWeight.Bold, fontSize = 24.sp, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = "Masuk untuk melanjutkan ke Neont.", color = SecondaryTextColor, fontFamily = InterFontFamily, fontSize = 14.sp, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = email,
                onValueChange = {
                    email = it
                    if (errorMessage != null) authViewModel.clearError()
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Email", fontFamily = InterFontFamily) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                shape = RoundedCornerShape(12.dp),
                colors = fieldColors
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                    if (errorMessage != null) authViewModel.clearError()
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Password", fontFamily = InterFontFamily) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    Text(
                        text = if (passwordVisible) "Sembunyikan" else "Tampilkan",
                        color = PrimaryColor,
                        fontFamily = InterFontFamily,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(end = 12.dp).bouncyClickable { passwordVisible = !passwordVisible }
                    )
                },
                shape = RoundedCornerShape(12.dp),
                colors = fieldColors
            )

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(12.dp))
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
                        if (!isLoading) authViewModel.signInWithEmail(email, password)
                    },
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Text(text = "Masuk", color = Color.White, fontFamily = InterFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                Text(text = "Belum memiliki akun? ", color = SecondaryTextColor, fontFamily = InterFontFamily, fontSize = 14.sp)
                Text(
                    text = "Daftar",
                    color = PrimaryColor,
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    modifier = Modifier.bouncyClickable { onNavigateToRegister() }
                )
            }
        }
    }
}
