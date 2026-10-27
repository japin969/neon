package app.neon.com.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.neon.com.ui.theme.*
import app.neon.com.ui.auth.RegisterViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    viewModel: RegisterViewModel,
    onNavigateToLogin: () -> Unit,
    onRegisterSuccess: (email: String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current

    LaunchedEffect(uiState.registeredUser) {
        uiState.registeredUser?.email?.let { email ->
            onRegisterSuccess(email)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(GhBackground)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 420.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Logo
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .background(GhSurface, shape = RoundedCornerShape(12.dp))
                    .border(1.dp, GhBorder, shape = RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "",
                    fontSize = 24.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Create your account",
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
                color = GhTextPrimary
            )

            Text(
                text = "Pendaftaran dengan Standar Keamanan Tinggi",
                fontSize = 13.sp,
                color = GhTextSecondary,
                modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
            )

            // Card Container
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = GhSurface),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(GhBorder),
                    width = 1.dp
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    // General Error Banner
                    if (uiState.generalError != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(GhErrorBackground, shape = RoundedCornerShape(6.dp))
                                .border(1.dp, GhErrorRed, shape = RoundedCornerShape(6.dp))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = uiState.generalError ?: "",
                                color = GhErrorRed,
                                fontSize = 13.sp,
                                lineHeight = 18.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Field 1: Email
                    Text(
                        text = "Email address",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = GhTextPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = uiState.email,
                        onValueChange = { viewModel.onEmailChanged(it) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        isError = uiState.emailError != null,
                        placeholder = {
                            Text(text = "nama@domain.com", color = GhTextSecondary)
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        ),
                        shape = RoundedCornerShape(6.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = GhBackground,
                            unfocusedContainerColor = GhBackground,
                            disabledContainerColor = GhBackground,
                            focusedBorderColor = GhBorderFocus,
                            unfocusedBorderColor = GhBorder,
                            errorBorderColor = GhErrorRed,
                            focusedTextColor = GhTextPrimary,
                            unfocusedTextColor = GhTextPrimary
                        )
                    )
                    if (uiState.emailError != null) {
                        Text(
                            text = uiState.emailError ?: "",
                            color = GhErrorRed,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 4.dp, start = 2.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Field 2: Password
                    Text(
                        text = "Password (min. 8 karakter, A-Z, a-z, 0-9)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = GhTextPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = uiState.password,
                        onValueChange = { viewModel.onPasswordChanged(it) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        isError = uiState.passwordError != null,
                        placeholder = {
                            Text(text = "Minimal 8 karakter kombinasi", color = GhTextSecondary)
                        },
                        visualTransformation = if (uiState.isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { viewModel.togglePasswordVisibility() }) {
                                Icon(
                                    imageVector = if (uiState.isPasswordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                    contentDescription = if (uiState.isPasswordVisible) "Sembunyikan password" else "Tampilkan password",
                                    tint = GhTextSecondary
                                )
                            }
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        ),
                        shape = RoundedCornerShape(6.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = GhBackground,
                            unfocusedContainerColor = GhBackground,
                            disabledContainerColor = GhBackground,
                            focusedBorderColor = GhBorderFocus,
                            unfocusedBorderColor = GhBorder,
                            errorBorderColor = GhErrorRed,
                            focusedTextColor = GhTextPrimary,
                            unfocusedTextColor = GhTextPrimary
                        )
                    )
                    if (uiState.passwordError != null) {
                        Text(
                            text = uiState.passwordError ?: "",
                            color = GhErrorRed,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 4.dp, start = 2.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Field 3: Konfirmasi Password
                    Text(
                        text = "Konfirmasi Password",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = GhTextPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = uiState.confirmPassword,
                        onValueChange = { viewModel.onConfirmPasswordChanged(it) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        isError = uiState.confirmPasswordError != null,
                        placeholder = {
                            Text(text = "Ulangi password di atas", color = GhTextSecondary)
                        },
                        visualTransformation = if (uiState.isConfirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { viewModel.toggleConfirmPasswordVisibility() }) {
                                Icon(
                                    imageVector = if (uiState.isConfirmPasswordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                    contentDescription = if (uiState.isConfirmPasswordVisible) "Sembunyikan password" else "Tampilkan password",
                                    tint = GhTextSecondary
                                )
                            }
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                focusManager.clearFocus()
                                viewModel.register()
                            }
                        ),
                        shape = RoundedCornerShape(6.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = GhBackground,
                            unfocusedContainerColor = GhBackground,
                            disabledContainerColor = GhBackground,
                            focusedBorderColor = GhBorderFocus,
                            unfocusedBorderColor = GhBorder,
                            errorBorderColor = GhErrorRed,
                            focusedTextColor = GhTextPrimary,
                            unfocusedTextColor = GhTextPrimary
                        )
                    )
                    if (uiState.confirmPasswordError != null) {
                        Text(
                            text = uiState.confirmPasswordError ?: "",
                            color = GhErrorRed,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 4.dp, start = 2.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Tombol Register (#1F6FEB, rounded 6dp)
                    Button(
                        onClick = {
                            focusManager.clearFocus()
                            viewModel.register()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        enabled = !uiState.isLoading,
                        shape = RoundedCornerShape(6.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GhButtonPrimary,
                            contentColor = GhTextPrimary,
                            disabledContainerColor = GhButtonPrimary.copy(alpha = 0.5f)
                        )
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = GhTextPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = "Create account",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Box Navigasi ke Halaman Login
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(GhSurface, shape = RoundedCornerShape(8.dp))
                    .border(1.dp, GhBorder, shape = RoundedCornerShape(8.dp))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Sudah memiliki akun? ",
                        fontSize = 13.sp,
                        color = GhTextSecondary
                    )
                    Text(
                        text = "Masuk di sini",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = GhTextLink,
                        modifier = Modifier.clickable { onNavigateToLogin() }
                    )
                }
            }
        }
    }
}
