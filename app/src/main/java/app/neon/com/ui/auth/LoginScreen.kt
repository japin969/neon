package app.neon.com.ui.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.neon.com.ui.theme.InterFontFamily

private val BackgroundColor = Color(0xFF1C1C1E)
private val SurfaceColor = Color(0xFF2C2C2E)
private val PrimaryColor = Color(0xFF0A84FF)
private val PrimaryLightColor = Color(0xFF409CFF)
private val SecondaryTextColor = Color(0xFF8E8E93)
private val BorderColor = Color(0xFF3A3A3C)
private val ErrorColor = Color(0xFFFF453A)
private val CardShape = RoundedCornerShape(16.dp)
private val InputShape = RoundedCornerShape(12.dp)
private val ButtonShape = RoundedCornerShape(12.dp)

@Composable
fun LoginScreen(
    authViewModel: AuthViewModel,
    onLoginSuccess: () -> Unit = {},
    onNavigateToRegister: () -> Unit = {}
) {
    val currentUser by authViewModel.currentUser.collectAsState()
    val errorMessage by authViewModel.errorMessage.collectAsState()
    val isLoading by authViewModel.isLoading.collectAsState()
    val focusManager = LocalFocusManager.current

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    val cardVisible = remember { MutableTransitionState(false).apply { targetState = true } }
    val canSubmit = email.isNotBlank() && password.isNotEmpty() && !isLoading

    LaunchedEffect(currentUser) {
        if (currentUser != null) onLoginSuccess()
    }

    val textStyle = TextStyle(fontFamily = InterFontFamily, fontSize = 16.sp, color = Color.White)
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = Color.White, unfocusedTextColor = Color.White,
        focusedBorderColor = PrimaryColor, unfocusedBorderColor = BorderColor, errorBorderColor = ErrorColor,
        cursorColor = PrimaryColor,
        focusedPlaceholderColor = SecondaryTextColor, unfocusedPlaceholderColor = SecondaryTextColor,
        focusedContainerColor = BackgroundColor, unfocusedContainerColor = BackgroundColor, errorContainerColor = BackgroundColor
    )

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize().background(BackgroundColor)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { focusManager.clearFocus() }
            .imePadding()
    ) {
        val minContentHeight = maxHeight
        Column(
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                .heightIn(min = minContentHeight).padding(24.dp),
            verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AnimatedVisibility(visibleState = cardVisible, enter = fadeIn(tween(400)) + slideInVertically(tween(400)) { it / 12 }) {
                Column(
                    modifier = Modifier.fillMaxWidth().shadow(8.dp, CardShape).background(SurfaceColor, CardShape).padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier.size(96.dp).background(Brush.verticalGradient(listOf(PrimaryLightColor, PrimaryColor)), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("NEON", color = Color.White, fontFamily = InterFontFamily, fontWeight = FontWeight.Bold, fontSize = 24.sp, letterSpacing = 1.sp)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Selamat Datang", color = Color.White, fontFamily = InterFontFamily, fontWeight = FontWeight.Bold, fontSize = 24.sp, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Masuk untuk melanjutkan ke Neont.", color = SecondaryTextColor, fontFamily = InterFontFamily, fontSize = 14.sp, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(24.dp))

                    OutlinedTextField(
                        value = email, onValueChange = { email = it; if (errorMessage != null) authViewModel.clearError() },
                        modifier = Modifier.fillMaxWidth(), placeholder = { Text("Email", fontFamily = InterFontFamily) },
                        textStyle = textStyle, singleLine = true, isError = errorMessage != null,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                        shape = InputShape, colors = fieldColors
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = password, onValueChange = { password = it; if (errorMessage != null) authViewModel.clearError() },
                        modifier = Modifier.fillMaxWidth(), placeholder = { Text("Password", fontFamily = InterFontFamily) },
                        textStyle = textStyle, singleLine = true, isError = errorMessage != null,
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { if (canSubmit) { focusManager.clearFocus(); authViewModel.signInWithEmail(email, password) } }),
                        trailingIcon = {
                            TextButton(onClick = { passwordVisible = !passwordVisible }, contentPadding = PaddingValues(horizontal = 8.dp)) {
                                Text(if (passwordVisible) "Sembunyikan" else "Tampilkan", color = PrimaryColor, fontFamily = InterFontFamily, fontSize = 12.sp)
                            }
                        },
                        shape = InputShape, colors = fieldColors
                    )

                    errorMessage?.let { message ->
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(message, color = ErrorColor, fontFamily = InterFontFamily, fontSize = 13.sp, modifier = Modifier.fillMaxWidth())
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { focusManager.clearFocus(); authViewModel.signInWithEmail(email, password) },
                        enabled = canSubmit, modifier = Modifier.fillMaxWidth().height(50.dp), shape = ButtonShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryColor, contentColor = Color.White,
                            disabledContainerColor = PrimaryColor.copy(alpha = 0.4f), disabledContentColor = Color.White.copy(alpha = 0.6f)
                        )
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Text("Masuk", fontFamily = InterFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                        Text("Belum memiliki akun?", color = SecondaryTextColor, fontFamily = InterFontFamily, fontSize = 14.sp)
                        TextButton(onClick = onNavigateToRegister, enabled = !isLoading, contentPadding = PaddingValues(horizontal = 6.dp)) {
                            Text("Daftar", color = PrimaryColor, fontFamily = InterFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        }
                    }
                }
            }
        }
    }
}
