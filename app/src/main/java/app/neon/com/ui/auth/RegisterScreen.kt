package app.neon.com.ui.auth

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun RegisterScreen(
    authViewModel: AuthViewModel,
    onRegisterSuccess: () -> Unit = {},
    onNavigateToLogin: () -> Unit = {}
) {
    val errorMessage by authViewModel.errorMessage.collectAsState()
    val isLoading by authViewModel.isLoading.collectAsState()
    val focusManager = LocalFocusManager.current

    var email by rememberSaveable { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    val canSubmit by remember {
        derivedStateOf {
            email.isNotBlank() &&
                password.isNotEmpty() &&
                confirmPassword.isNotEmpty() &&
                !isLoading
        }
    }
    val hasError = errorMessage != null

    LaunchedEffect(Unit) { authViewModel.clearError() }

    val submit: () -> Unit = {
        focusManager.clearFocus()
        authViewModel.signUpWithEmail(
            email = email,
            password = password,
            confirmPassword = confirmPassword,
            onSuccess = onRegisterSuccess
        )
    }

    AuthScreenContainer {
        Text(
            text = "Buat Akun",
            color = Color.White,
            style = MaterialTheme.typography.displaySmall,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Daftar untuk mulai menggunakan Neont.",
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
            imeAction = ImeAction.Next,
            isError = hasError
        )
        Spacer(modifier = Modifier.height(12.dp))

        AuthPasswordField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it; if (hasError) authViewModel.clearError() },
            placeholder = "Konfirmasi Password",
            imeAction = ImeAction.Done,
            isError = hasError,
            keyboardActions = KeyboardActions(onDone = { if (canSubmit) submit() })
        )

        Spacer(modifier = Modifier.height(8.dp))
        AuthMessageText(
            text = "Minimal 8 karakter, mengandung huruf besar, huruf kecil, dan angka.",
            color = AuthTextSecondary
        )

        errorMessage?.let { message ->
            Spacer(modifier = Modifier.height(12.dp))
            AuthMessageText(text = message, color = AuthError)
        }

        Spacer(modifier = Modifier.height(24.dp))
        AuthPrimaryButton(
            text = "Daftar",
            enabled = canSubmit,
            isLoading = isLoading,
            onClick = submit
        )
        Spacer(modifier = Modifier.height(16.dp))

        AuthLinkRow(
            prefix = "Sudah memiliki akun?",
            action = "Masuk",
            enabled = !isLoading,
            onClick = onNavigateToLogin
        )
    }
}
