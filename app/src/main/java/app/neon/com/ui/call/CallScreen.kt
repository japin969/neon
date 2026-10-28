package app.neon.com.ui.call

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.FlipCameraIos
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.neon.com.data.webrtc.CallConnectionState
import app.neon.com.ui.theme.GhBorder
import app.neon.com.ui.theme.GhCallEndRed
import app.neon.com.ui.theme.GhSurface
import app.neon.com.ui.theme.GhTextPrimary
import app.neon.com.ui.theme.GhTextSecondary
import app.neon.com.ui.theme.InterFontFamily
import app.neon.com.ui.theme.bouncyClickable
import app.neon.com.ui.viewmodel.CallViewModel
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest

@Composable
fun CallScreen(
    callViewModel: CallViewModel,
    chatId: String = "chat_aura",
    contactName: String = "Aura Cantika",
    contactAvatarUrl: String = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=400&q=80",
    isVideoCall: Boolean = false,
    onEndCallClick: () -> Unit = {}
) {
    val context = LocalContext.current

    val isAudioMuted by callViewModel.isAudioMuted.collectAsState()
    val isVideoEnabled by callViewModel.isVideoEnabled.collectAsState()
    val isSpeakerOn by callViewModel.isSpeakerOn.collectAsState()
    val isFrontCamera by callViewModel.isFrontCamera.collectAsState()
    val durationFormatted by callViewModel.durationFormatted.collectAsState()
    val connectionState by callViewModel.connectionState.collectAsState()

    LaunchedEffect(chatId, isVideoCall) {
        callViewModel.initCall(chatId, isVideoCall)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D1117))
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(contactAvatarUrl)
                .crossfade(true)
                .memoryCachePolicy(CachePolicy.ENABLED)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .blur(radius = 35.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0D1117).copy(alpha = 0.78f))
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(40.dp))

            Box(
                modifier = Modifier
                    .size(120.dp)
                    .shadow(16.dp, CircleShape)
                    .clip(CircleShape)
                    .border(2.dp, GhBorder, CircleShape)
                    .background(GhSurface)
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(contactAvatarUrl)
                        .crossfade(true)
                        .memoryCachePolicy(CachePolicy.ENABLED)
                        .build(),
                    contentDescription = contactName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = contactName,
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                color = GhTextPrimary
            )

            Spacer(modifier = Modifier.height(6.dp))

            val statusText = when (connectionState) {
                CallConnectionState.CONNECTING -> "Menghubungkan..."
                CallConnectionState.CONNECTED -> {
                    if (isVideoEnabled) "WhatsApp Video Call • $durationFormatted" else "WhatsApp Voice Call • $durationFormatted"
                }
                CallConnectionState.DISCONNECTED -> "Panggilan Berakhir"
                CallConnectionState.IDLE -> "Memulai Panggilan..."
            }

            Text(
                text = statusText,
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 16.sp,
                color = GhTextSecondary
            )

            Spacer(modifier = Modifier.weight(1f))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(if (isAudioMuted) Color.White else GhSurface.copy(alpha = 0.85f))
                        .border(1.dp, GhBorder, CircleShape)
                        .bouncyClickable { callViewModel.toggleAudio() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isAudioMuted) Icons.Default.MicOff else Icons.Default.Mic,
                        contentDescription = "Mute Toggle",
                        tint = if (isAudioMuted) Color.Black else GhTextPrimary,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(if (!isVideoEnabled) Color.White else GhSurface.copy(alpha = 0.85f))
                        .border(1.dp, GhBorder, CircleShape)
                        .bouncyClickable { callViewModel.toggleVideo() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isVideoEnabled) Icons.Default.Videocam else Icons.Default.VideocamOff,
                        contentDescription = "Video Toggle",
                        tint = if (!isVideoEnabled) Color.Black else GhTextPrimary,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(if (isSpeakerOn) Color.White else GhSurface.copy(alpha = 0.85f))
                        .border(1.dp, GhBorder, CircleShape)
                        .bouncyClickable { callViewModel.toggleSpeaker() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isSpeakerOn) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                        contentDescription = "Speaker Toggle",
                        tint = if (isSpeakerOn) Color.Black else GhTextPrimary,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .shadow(12.dp, CircleShape)
                        .clip(CircleShape)
                        .background(GhCallEndRed)
                        .bouncyClickable {
                            callViewModel.endCall(onEndCallClick)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CallEnd,
                        contentDescription = "End Call",
                        tint = Color.White,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = isVideoEnabled,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(top = 16.dp, end = 16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(width = 100.dp, height = 145.dp)
                    .shadow(12.dp, RoundedCornerShape(12.dp))
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.5.dp, GhBorder, RoundedCornerShape(12.dp))
                    .background(GhSurface)
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(if (isFrontCamera) "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=200&q=80" else "https://images.unsplash.com/photo-1517841905240-472988babdf9?w=200&q=80")
                        .crossfade(true)
                        .memoryCachePolicy(CachePolicy.ENABLED)
                        .build(),
                    contentDescription = "Self Video PiP",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.65f))
                        .clickable { callViewModel.switchCamera() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.FlipCameraIos,
                        contentDescription = "Flip Camera",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
