package app.neon.com.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.neon.com.ui.chat.bouncyClickable
import app.neon.com.ui.theme.GhBackground
import app.neon.com.ui.theme.GhBorder
import app.neon.com.ui.theme.GhButtonPrimary
import app.neon.com.ui.theme.GhSurface
import app.neon.com.ui.theme.GhTextPrimary
import app.neon.com.ui.theme.GhTextSecondary
import app.neon.com.ui.theme.InterFontFamily
import app.neon.com.ui.viewmodel.AuthViewModel

/**
 * LoginScreen - Halaman Masuk iOS WhatsApp / Neon Style
 * Menghubungkan tombol ke AuthViewModel
 */
@Composable
fun LoginScreen(
    authViewModel: AuthViewModel,
    onLoginSuccess: () -> Unit = {}
) {
    val currentUser by authViewModel.currentUser.collectAsState()
    val errorMessage by authViewModel.errorMessage.collectAsState()
    val isLoading by authViewModel.isLoading.collectAsState()

    var email by remember { mutableStateOf("user@neon.com") }
    var password by remember { mutableStateOf("password123") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GhBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Logo / Icon
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(GhSurface)
                .border(1.dp, GhBorder, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Shield,
                contentDescription = "Security",
                tint = GhButtonPrimary,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Selamat Datang di Neon",
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
            color = GhTextPrimary
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Enkripsi End-to-End & Audio Eksklusif",
            fontFamily = InterFontFamily,
            fontSize = 14.sp,
            color = GhTextSecondary
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Email Field
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email", fontFamily = InterFontFamily) },
            leadingIcon = {
                Icon(Icons.Default.Mail, contentDescription = null, tint = GhTextSecondary)
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = GhSurface,
                unfocusedContainerColor = GhSurface,
                focusedBorderColor = GhButtonPrimary,
                unfocusedBorderColor = GhBorder,
                focusedTextColor = GhTextPrimary,
                unfocusedTextColor = GhTextPrimary
            )
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Password Field
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Kata Sandi", fontFamily = InterFontFamily) },
            leadingIcon = {
                Icon(Icons.Default.Lock, contentDescription = null, tint = GhTextSecondary)
            },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = GhSurface,
                unfocusedContainerColor = GhSurface,
                focusedBorderColor = GhButtonPrimary,
                unfocusedBorderColor = GhBorder,
                focusedTextColor = GhTextPrimary,
                unfocusedTextColor = GhTextPrimary
            )
        )

        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = errorMessage ?: "",
                fontFamily = InterFontFamily,
                fontSize = 13.sp,
                color = Color(0xFFFF453A)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Tombol Masuk
        Button(
            onClick = {
                authViewModel.signInWithEmail(email, password)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .bouncyClickable { authViewModel.signInWithEmail(email, password) },
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = GhButtonPrimary,
                contentColor = Color.White
            ),
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text = "Masuk",
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Tombol Masuk Demo / Anonymous
        TextButton(
            onClick = { authViewModel.signInAnonymously() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Masuk Cepat Sebagai Tamu (Demo)",
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                color = GhButtonPrimary
            )
        }
    }
}
