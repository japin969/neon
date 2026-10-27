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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.neon.com.data.model.User

@Composable
fun CreateUsernameScreen(
    uid: String,
    email: String,
    viewModel: AuthViewModel,
    onProfileCreated: (User) -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()

    val neonGreen = Color(0xFF00FF66)
    val neonCyan = Color(0xFF00E5FF)
    val darkCardBg = Color(0xFF161824)
    val focusRequester = remember { FocusRequester() }

    // Dynamic initial 2-letter avatar generator
    val initials = remember(state.username) {
        val clean = state.username.trim()
        when {
            clean.length >= 2 -> clean.take(2).uppercase()
            clean.length == 1 -> clean.take(1).uppercase()
            else -> "NE"
        }
    }

    // Dynamic color picker preview based on username hash
    val previewColor = remember(state.username) {
        val colors = listOf(
            Color(0xFF00FF66),
            Color(0xFF00E5FF),
            Color(0xFFFF007F),
            Color(0xFF7928CA),
            Color(0xFFFF9100),
            Color(0xFF00F5D4),
            Color(0xFFF72585)
        )
        val index = kotlin.math.abs(state.username.hashCode()) % colors.size
        colors[index]
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0E15))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp, vertical = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Pilih Identitas Neont",
                fontSize = 26.sp,
                fontWeight = FontWeight.Black,
                color = Color.White
            )

            Text(
                text = "Buat @username unik untuk mulai terhubung dan menikmati musik",
                fontSize = 14.sp,
                color = Color(0xFF9094A6),
                modifier = Modifier.padding(top = 6.dp, bottom = 32.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            // Live Initial Avatar Preview Generator

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = neonCyan
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Error Banner
            if (state.errorMessage != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0x33FF0055))
                        .border(1.dp, Color(0xFFFF0055), RoundedCornerShape(12.dp))
                        .padding(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Peringatan",
                            tint = Color(0xFFFF4D6D)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = state.errorMessage ?: "",
                            color = Color(0xFFFF85A1),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Card Input Username
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(darkCardBg)
                    .border(1.dp, Color(0xFF262A3E), RoundedCornerShape(20.dp))
                    .padding(20.dp)
            ) {
                OutlinedTextField(
                    value = state.username,
                    onValueChange = { viewModel.onUsernameChange(it) },
                    label = { Text("Username (@username)", color = Color(0xFF888EA8)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.AlternateEmail,
                            contentDescription = "Username Icon",
                            tint = neonGreen
                        )
                    },
                    trailingIcon = {
                        when {
                            state.isCheckingUsername -> {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = neonGreen
                                )
                            }
                            state.isUsernameAvailable == true -> {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Tersedia",
                                    tint = neonGreen
                                )
                            }
                            state.isUsernameAvailable == false && state.username.length >= 3 -> {
                                Icon(
                                    imageVector = Icons.Default.Error,
                                    contentDescription = "Sudah Terpakai",
                                    tint = Color(0xFFFF0055)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            focusManager.clearFocus()
                            if (state.isUsernameAvailable == true) {
                                viewModel.createUsername(uid, email, onProfileCreated)
                            }
                        }
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = if (state.isUsernameAvailable == true) neonGreen else Color(0xFF2E334D),
                        unfocusedBorderColor = Color(0xFF2E334D),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = neonGreen
                    ),
                    modifier = Modifier.fillMaxWidth().focusRequester(focusRequester)
                )

                // Feedback status label
                if (state.usernameStatusMessage != null) {
                    val statusColor = if (state.isUsernameAvailable == true) neonGreen else Color(0xFFFF6B6B)
                    Text(
                        text = state.usernameStatusMessage ?: "",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = statusColor,
                        modifier = Modifier.padding(top = 8.dp, start = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Tombol Konfirmasi Username
                val isButtonEnabled = state.username.length >= 3 && !state.isLoading

                Button(
                    onClick = {
                        focusManager.clearFocus()
                        viewModel.createUsername(uid, email, onProfileCreated)
                    },
                    enabled = isButtonEnabled,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = neonGreen,
                        disabledContainerColor = Color(0xFF1E2F26)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (state.isLoading) {
                        CircularProgressIndicator(
                            color = Color.Black,
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.5.dp
                        )
                    } else {
                        Text(
                            text = "SELESAI & MASUK KE NEONT",
                            color = if (isButtonEnabled) Color.Black else Color(0xFF6B7280),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }
        }
    }
}
