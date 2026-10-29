package app.neon.com.ui.music

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.neon.com.ui.viewmodel.MusicViewModel
import coil.compose.AsyncImage
import java.util.Locale

@Composable
fun MusicPlayerScreen(
    viewModel: MusicViewModel,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val song = state.currentSong

    val darkBg = Color(0xFF090A10)
    val neonGreen = Color(0xFF00FF66)
    val neonPink = Color(0xFFFF007F)

    val infiniteTransition = rememberInfiniteTransition(label = "musicPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (state.isPlaying) 1.05f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "albumScale"
    )

    var isSeeking by remember { mutableStateOf(false) }
    var seekPositionMs by remember { mutableFloatStateOf(0f) }

    val currentPosition = if (isSeeking) seekPositionMs.toLong() else state.currentPositionMs
    val totalDuration = state.durationMs.coerceAtLeast(1L)

    fun formatDuration(ms: Long): String {
        val totalSecs = (ms / 1000).toInt()
        val minutes = totalSecs / 60
        val seconds = totalSecs % 60
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF161E34), darkBg, Color(0xFF0D0F18))
                )
            )
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Kembali", tint = Color.White)
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("MEMUTAR DARI PLAYLIST", fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp, color = Color(0xFF888EA8))
                    Text(song?.album ?: "Neont Music", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                }

                IconButton(onClick = { viewModel.toggleLoveCurrentSong() }) {
                    Icon(
                        if (state.isCurrentSongLoved) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        "Love",
                        tint = if (state.isCurrentSongLoved) neonPink else Color.White
                    )
                }
            }

            if (song != null) {
                Box(
                    modifier = Modifier.size(280.dp).scale(pulseScale).clip(RoundedCornerShape(24.dp))
                        .background(Color(0xFF1E2438))
                        .border(2.dp, if (state.isPlaying) neonGreen.copy(alpha = 0.6f) else Color(0xFF282E44), RoundedCornerShape(24.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (song.albumArt.isNotBlank()) {
                        AsyncImage(
                            model = song.albumArt,
                            contentDescription = song.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(Icons.Default.MusicNote, null, tint = neonGreen, modifier = Modifier.size(80.dp))
                    }
                }
            } else {
                Box(
                    modifier = Modifier.size(260.dp).clip(RoundedCornerShape(24.dp)).background(Color(0xFF161824)),
                    contentAlignment = Alignment.Center
                ) { Text("Pilih lagu untuk diputar", color = Color(0xFF888EA8)) }
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    song?.title ?: "Tidak Ada Lagu",
                    fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Color.White,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    song?.artist ?: "Pilih dari Beranda Musik",
                    fontSize = 14.sp, color = Color(0xFF9094A6),
                    maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(24.dp))

                Slider(
                    value = currentPosition.toFloat(),
                    onValueChange = { isSeeking = true; seekPositionMs = it },
                    onValueChangeFinished = { isSeeking = false; viewModel.seekTo(seekPositionMs.toLong()) },
                    valueRange = 0f..totalDuration.toFloat(),
                    colors = SliderDefaults.colors(
                        thumbColor = neonGreen,
                        activeTrackColor = neonGreen,
                        inactiveTrackColor = Color(0xFF282D42)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(formatDuration(currentPosition), fontSize = 11.sp, color = Color(0xFF888EA8))
                    Text(formatDuration(totalDuration), fontSize = 11.sp, color = Color(0xFF888EA8))
                }

                Spacer(Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { viewModel.previous() }, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Default.SkipPrevious, "Sebelumnya", tint = Color.White, modifier = Modifier.size(34.dp))
                    }

                    IconButton(
                        onClick = { viewModel.togglePlayPause() },
                        modifier = Modifier.size(70.dp).clip(CircleShape).background(neonGreen).border(2.dp, Color.White.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(
                            if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            if (state.isPlaying) "Pause" else "Play",
                            tint = Color.Black, modifier = Modifier.size(40.dp)
                        )
                    }

                    IconButton(onClick = { viewModel.next() }, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Default.SkipNext, "Selanjutnya", tint = Color.White, modifier = Modifier.size(34.dp))
                    }
                }

                Spacer(Modifier.height(24.dp))
            }
        }
    }
}
