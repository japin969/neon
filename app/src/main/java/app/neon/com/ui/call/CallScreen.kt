package app.neon.com.ui.call

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import org.webrtc.SurfaceViewRenderer

@Composable
fun CallScreen(
    targetUserId: String,
    targetUserName: String,
    isVideo: Boolean,
    viewModel: CallViewModel,
    onCallEnded: () -> Unit
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsState()

    // 1. Cek & Kelola Izin Runtime Kamera & Audio
    val requiredPermissions = remember(isVideo) {
        if (isVideo) {
            arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA)
        } else {
            arrayOf(Manifest.permission.RECORD_AUDIO)
        }
    }

    var hasPermissions by remember {
        mutableStateOf(
            requiredPermissions.all {
                ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
            }
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        hasPermissions = results.values.all { it }
    }

    // Inisialisasi SurfaceViewRenderer satu kali
    val localRenderer = remember {
        if (isVideo) SurfaceViewRenderer(context) else null
    }
    val remoteRenderer = remember {
        if (isVideo) SurfaceViewRenderer(context) else null
    }

    LaunchedEffect(hasPermissions) {
        if (hasPermissions) {
            viewModel.initCall(
                targetUserId = targetUserId,
                targetUserName = targetUserName,
                isVideo = isVideo,
                localRenderer = localRenderer,
                remoteRenderer = remoteRenderer
            )
        } else {
            permissionLauncher.launch(requiredPermissions)
        }
    }

    // Bersihkan resource saat composable dihancurkan
    DisposableEffect(Unit) {
        onDispose {
            localRenderer?.release()
            remoteRenderer?.release()
        }
    }

    val darkBg = Color(0xFF090A0F)
    val neonGreen = Color(0xFF00FF66)
    val neonCyan = Color(0xFF00E5FF)
    val redEndCall = Color(0xFFFF0055)

    // Animasi Pulse Lingkaran Cyberpunk
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
            .background(darkBg)
    ) {
        if (!hasPermissions) {
            // Tampilan Dialog Permintaan Izin
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(darkBg)
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(Color(0x2200FF66)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = "Izin",
                            tint = neonGreen,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "Izin Akses Diperlukan",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Neont memerlukan izin mikrofon dan kamera untuk menghubungkan panggilan WebRTC terenkripsi.",
                        fontSize = 14.sp,
                        color = Color(0xFF888EA8),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(28.dp))

                    Button(
                        onClick = { permissionLauncher.launch(requiredPermissions) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = neonGreen,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "Izinkan Akses",
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = onCallEnded,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent,
                            contentColor = Color(0xFF888EA8)
                        )
                    ) {
                        Text(text = "Batal")
                    }
                }
            }
        } else {
            // 2. VIDEO STREAM AREA (REMOTE & LOCAL)
            if (isVideo && remoteRenderer != null && localRenderer != null) {
                // Remote Full Screen Video
                AndroidView(
                    factory = { remoteRenderer },
                    modifier = Modifier.fillMaxSize()
                )

                // Jika remote video belum terhubung, tampilkan background gradient gelap
                AnimatedVisibility(
                    visible = !state.isRemoteConnected,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
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
                    )
                }

                // Local PiP Video (Pojok Kanan Atas)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 48.dp, end = 20.dp),
                    contentAlignment = Alignment.TopEnd
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 110.dp, height = 150.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .border(2.dp, if (state.isCameraOff) Color(0xFFFF0055) else neonCyan, RoundedCornerShape(16.dp))
                            .background(Color.Black)
                    ) {
                        if (!state.isCameraOff) {
                            AndroidView(
                                factory = { localRenderer },
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Kamera Mati",
                                    fontSize = 11.sp,
                                    color = Color(0xFFFF6B6B),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            } else {
                // Audio Only Background
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
                )
            }

            // 3. OVERLAY UI (HEADER & CONTROLS)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 48.dp, horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Header Info
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (state.isVideo) "PANGGILAN VIDEO WEBRTC" else "PANGGILAN SUARA WEBRTC",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp,
                        color = neonGreen
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = state.targetUserName.ifBlank { "Pengguna Neont" },
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    val statusText = when (state.callState) {
                        CallState.CONNECTING -> "Menghubungkan Peer..."
                        CallState.RINGING -> "Memanggil Lawan..."
                        CallState.CONNECTED -> "Terhubung • ${state.formattedDuration}"
                        CallState.ENDED -> "Panggilan Berakhir"
                    }

                    Text(
                        text = statusText,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (state.callState == CallState.CONNECTED) neonCyan else Color(0xFF888EA8)
                    )
                }

                // Center Visual for Audio Call or when Video is waiting
                if (!isVideo || !state.isRemoteConnected) {
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
                                .size(140.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        listOf(neonGreen, neonCyan)
                                    )
                                )
                                .border(3.dp, Color.White.copy(alpha = 0.7f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = state.targetUserName.take(2).uppercase().ifBlank { "NE" },
                                fontSize = 48.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.Black
                            )
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.height(40.dp))
                }

                // Bottom Controls Bar
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 1. Mute Mic Toggle
                        CallActionButton(
                            icon = if (state.isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                            label = if (state.isMuted) "Bisu" else "Mic",
                            isActive = state.isMuted,
                            activeColor = Color(0xFFFF5252),
                            inactiveColor = Color(0x33FFFFFF),
                            onClick = { viewModel.toggleMute() }
                        )

                        // 2. Speaker Toggle
                        CallActionButton(
                            icon = if (state.isSpeakerOn) Icons.Default.VolumeUp else Icons.Default.VolumeDown,
                            label = if (state.isSpeakerOn) "Speaker" else "Earpiece",
                            isActive = state.isSpeakerOn,
                            activeColor = neonCyan,
                            inactiveColor = Color(0x33FFFFFF),
                            onClick = { viewModel.toggleSpeaker() }
                        )

                        // 3. Switch Camera Toggle (Jika Video Call)
                        if (isVideo) {
                            CallActionButton(
                                icon = Icons.Default.Cameraswitch,
                                label = "Putar",
                                isActive = false,
                                activeColor = neonGreen,
                                inactiveColor = Color(0x33FFFFFF),
                                onClick = { viewModel.switchCamera() }
                            )

                            // 4. Video Off Toggle
                            CallActionButton(
                                icon = if (state.isCameraOff) Icons.Default.VideocamOff else Icons.Default.Videocam,
                                label = if (state.isCameraOff) "Mati" else "Kamera",
                                isActive = state.isCameraOff,
                                activeColor = Color(0xFFFF5252),
                                inactiveColor = Color(0x33FFFFFF),
                                onClick = { viewModel.toggleCamera() }
                            )
                        }

                        // 5. End Call Button
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(redEndCall)
                                .clickable {
                                    viewModel.endCall {
                                        onCallEnded()
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CallEnd,
                                contentDescription = "Akhiri Panggilan",
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CallActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isActive: Boolean,
    activeColor: Color,
    inactiveColor: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 4.dp)
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(if (isActive) activeColor else inactiveColor)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isActive) Color.Black else Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            color = Color(0xFFC0C4D6),
            fontWeight = FontWeight.Medium
        )
    }
}
