package app.neon.com.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun LoginScreen(
    authViewModel: AuthViewModel,
    onLoginSuccess: () -> Unit = {},
    onNavigateToRegister: () -> Unit = {},
    onNavigateToResetPassword: () -> Unit = {}
) {
    val currentUser by authViewModel.currentUser.collectAsState()
    val errorMessage by authViewModel.errorMessage.collectAsState()
    val isLoading by authViewModel.isLoading.collectAsState()
    val focusManager = LocalFocusManager.current

    var email by rememberSaveable { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    val canSubmit by remember {
        derivedStateOf { email.isNotBlank() && password.isNotEmpty() && !isLoading }
    }
    val hasError = errorMessage != null

    LaunchedEffect(currentUser) {
        if (currentUser != null) onLoginSuccess()
    }

    val submit: () -> Unit = {
        focusManager.clearFocus()
        authViewModel.signInWithEmail(email, password)
    }

    AuthScreenContainer {
        AuthLogo()
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Selamat Datang",
            color = Color.White,
            style = MaterialTheme.typography.displaySmall,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Masuk untuk melanjutkan ke Neont.",
            color = AuthTextSecondary,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))

        AuthTextField(
            value = email,
            onValueChange = { email = it; if (hasError) authViewModel.clearError() },
            placeholder = "Email",
            keyboardType = KeyboardType.Email,
            imeAction = ImeAction.Next,
            isError = hasError
        )
        Spacer(modifier = Modifier.height(12.dp))

        AuthPasswordField(
            value = password,
            onValueChange = { password = it; if (hasError) authViewModel.clearError() },
            placeholder = "Password",
            imeAction = ImeAction.Done,
            isError = hasError,
            keyboardActions = KeyboardActions(onDone = { if (canSubmit) submit() })
        )

        errorMessage?.let { message ->
            Spacer(modifier = Modifier.height(12.dp))
            AuthMessageText(text = message, color = AuthError)
        }

        Spacer(modifier = Modifier.height(24.dp))
        AuthPrimaryButton(
            text = "Masuk",
            enabled = canSubmit,
            isLoading = isLoading,
            onClick = submit
        )
        Spacer(modifier = Modifier.height(8.dp))

        TextButton(onClick = onNavigateToResetPassword, enabled = !isLoading) {
            Text(
                text = "Lupa Password?",
                color = AuthPrimary,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        Spacer(modifier = Modifier.height(8.dp))

        AuthLinkRow(
            prefix = "Belum memiliki akun?",
            action = "Daftar",
            enabled = !isLoading,
            onClick = onNavigateToRegister
        )
    }
}

@Composable
private fun AuthLogo() {
    Box(
        modifier = Modifier
            .size(96.dp)
            .background(
                Brush.verticalGradient(listOf(AuthPrimaryLight, AuthPrimary)),
                CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "NEON",
            color = Color.White,
            style = MaterialTheme.typography.titleLarge.copy(fontSize = 18.sp, letterSpacing = 0.sp)
        )
    }
}
