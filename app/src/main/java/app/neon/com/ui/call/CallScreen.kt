package app.neon.com.ui.call

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CallScreen(
    targetUserId: String,
    targetUserName: String,
    isVideo: Boolean,
    viewModel: CallViewModel,
    onCallEnded: () -> Unit
) {
    LaunchedEffect(targetUserId) {
        viewModel.initCall(targetUserId, targetUserName, isVideo)
    }

    val state by viewModel.uiState.collectAsState()

    val darkBg = Color(0xFF090A0F)
    val neonGreen = Color(0xFF00FF66)
    val neonCyan = Color(0xFF00E5FF)
    val redEndCall = Color(0xFFFF0055)

    // Pulsing circle animation
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF0E1220),
                        darkBg,
                        Color(0xFF151829)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 48.dp, horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header Info
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = if (state.isVideo) "PANGGILAN VIDEO NEONT" else "PANGGILAN SUARA NEONT",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                    color = neonGreen
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = state.targetUserName.ifBlank { "Pengguna Neont" },
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(8.dp))

                val statusText = when (state.callState) {
                    CallState.CONNECTING -> "Menghubungkan..."
                    CallState.RINGING -> "Berdering..."
                    CallState.CONNECTED -> "Terhubung • ${state.formattedDuration}"
                    CallState.ENDED -> "Panggilan Berakhir"
                }

                Text(
                    text = statusText,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (state.callState == CallState.CONNECTED) neonCyan else Color(0xFF888EA8)
                )
            }

            // Center Visual (Pulsing Avatar or Video Sim)
            Box(
                modifier = Modifier.size(240.dp),
                contentAlignment = Alignment.Center
            ) {
                // Outer Pulse Ring
                Box(
                    modifier = Modifier
                        .size(220.dp)
                        .scale(pulseScale)
                        .clip(CircleShape)
                        .background(Color(0x1A00FF66))
                        .border(1.dp, Color(0x3300FF66), CircleShape)
                )

                // Main Avatar Circle
                Box(
                    modifier = Modifier
                        .size(150.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(neonGreen, neonCyan)
                            )
                        )
                        .border(3.dp, Color.White.copy(alpha = 0.6f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = state.targetUserName.take(2).uppercase(),
                        fontSize = 54.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.Black
                    )
                }

                if (state.isVideo && state.isCameraOff) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .clip(CircleShape)
                            .background(Color(0xCC000000))
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Kamera Nonaktif",
                            fontSize = 11.sp,
                            color = Color(0xFFFF6B6B)
                        )
                    }
                }
            }

            // Bottom Action Controls Bar
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Tombol Mute Mic
                    CallActionButton(
                        icon = if (state.isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                        label = if (state.isMuted) "Unmute" else "Mute",
                        isActive = state.isMuted,
                        onClick = { viewModel.toggleMute() }
                    )

                    // Tombol Speakerphone
                    CallActionButton(
                        icon = if (state.isSpeakerOn) Icons.Default.VolumeUp else Icons.Default.VolumeDown,
                        label = if (state.isSpeakerOn) "Speaker" else "Earpiece",
                        isActive = state.isSpeakerOn,
                        onClick = { viewModel.toggleSpeaker() }
                    )

                    // Tombol Kamera (Jika Video Call)
                    if (state.isVideo) {
                        CallActionButton(
                            icon = if (state.isCameraOff) Icons.Default.VideocamOff else Icons.Default.Videocam,
                            label = if (state.isCameraOff) "Buka Cam" else "Tutup Cam",
                            isActive = state.isCameraOff,
                            onClick = { viewModel.toggleCamera() }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(36.dp))

                // End Call Button (Merah Besar)
                IconButton(
                    onClick = { viewModel.endCall(onCallEnded) },
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(redEndCall)
                        .border(2.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.CallEnd,
                        contentDescription = "Akhiri Panggilan",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Akhiri",
                    fontSize = 12.sp,
                    color = Color(0xFFFF85A1),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun CallActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    val neonGreen = Color(0xFF00FF66)
    val buttonBg = if (isActive) Color(0xFF2A2E44) else Color(0xFF1A1D2B)
    val iconColor = if (isActive) neonGreen else Color.White

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(buttonBg)
                .border(
                    1.dp,
                    if (isActive) neonGreen.copy(alpha = 0.8f) else Color(0xFF333850),
                    CircleShape
                )
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = iconColor,
                modifier = Modifier.size(26.dp)
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = label,
            fontSize = 11.sp,
            color = Color(0xFF888EA8)
        )
    }
}
