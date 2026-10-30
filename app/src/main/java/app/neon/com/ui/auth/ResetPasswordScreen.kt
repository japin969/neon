package app.neon.com.ui.auth

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
fun ResetPasswordScreen(
    authViewModel: AuthViewModel,
    onNavigateBack: () -> Unit = {}
) {
    val uiState by authViewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current

    var email by rememberSaveable { mutableStateOf("") }

    val canSubmit by remember {
        derivedStateOf { email.isNotBlank() && !uiState.isLoading }
    }
    val status = uiState.resetPasswordStatus
    val isSuccess = status == AuthViewModel.RESET_SUCCESS_MESSAGE

    val submit: () -> Unit = {
        focusManager.clearFocus()
        authViewModel.resetPassword(email)
    }

    AuthScreenContainer {
        Text(
            text = "Reset Password",
            color = Color.White,
            style = MaterialTheme.typography.displaySmall,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Masukkan email akun Anda. Kami akan mengirim link untuk mengatur ulang password.",
            color = AuthTextSecondary,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))

        AuthTextField(
            value = email,
            onValueChange = { email = it; if (status != null) authViewModel.clearError() },
            placeholder = "Email",
            keyboardType = KeyboardType.Email,
            imeAction = ImeAction.Done,
            isError = status != null && !isSuccess,
            keyboardActions = KeyboardActions(onDone = { if (canSubmit) submit() })
        )

        status?.let { message ->
            Spacer(modifier = Modifier.height(12.dp))
            AuthMessageText(
                text = message,
                color = if (isSuccess) AuthSuccess else AuthError
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
        AuthPrimaryButton(
            text = "Kirim Link Reset",
            enabled = canSubmit,
            isLoading = uiState.isLoading,
            onClick = submit
        )
        Spacer(modifier = Modifier.height(8.dp))

        TextButton(onClick = onNavigateBack, enabled = !uiState.isLoading) {
            Text(
                text = "Kembali ke Masuk",
                color = AuthPrimary,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
