package app.neon.com.ui.music

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.neon.com.data.model.Song
import app.neon.com.ui.theme.*
import app.neon.com.ui.viewmodel.MusicViewModel
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest

@Composable
fun MusicHomeScreen(
    musicViewModel: MusicViewModel,
    onBackClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val songs by musicViewModel.songs.collectAsState()
    val currentSong by musicViewModel.currentSong.collectAsState()
    val isPlaying by musicViewModel.isPlaying.collectAsState()
    val currentPositionMs by musicViewModel.currentPositionMs.collectAsState()
    val durationMs by musicViewModel.durationMs.collectAsState()

    var searchQuery by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().background(GhBackground).statusBarsPadding()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Music", fontFamily = InterFontFamily, fontWeight = FontWeight.Bold, fontSize = 34.sp, color = GhTextPrimary)
                Text("Pencarian 4 API (Spotify, SoundCloud, YouTube, Deezer)", fontFamily = InterFontFamily, fontSize = 12.sp, color = GhTextSecondary)
            }
        }

        Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
            TextField(
                value = searchQuery,
                onValueChange = { searchQuery = it; musicViewModel.loadSongs(it) },
                modifier = Modifier.fillMaxWidth().height(46.dp).clip(RoundedCornerShape(12.dp)).border(0.5.dp, GhBorder, RoundedCornerShape(12.dp)),
                placeholder = { Text("Cari lagu, artis, atau genre...", fontFamily = InterFontFamily, fontSize = 15.sp, color = GhTextSecondary) },
                leadingIcon = { Icon(Icons.Default.Search, "Search", tint = GhTextSecondary, modifier = Modifier.size(18.dp)) },
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = GhSurface, unfocusedContainerColor = GhSurface, disabledContainerColor = GhSurface,
                    focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = GhTextPrimary, unfocusedTextColor = GhTextPrimary
                )
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        ) {
            items(songs, key = { it.id }) { song ->
                val isThisSongPlaying = (currentSong?.id == song.id && isPlaying)
                SongCardItem(
                    song = song,
                    isCurrentlyPlaying = isThisSongPlaying,
                    onPlayClick = {
                        if (currentSong?.id == song.id) musicViewModel.togglePlayPause() else musicViewModel.playSong(song)
                    },
                    onLoveClick = { musicViewModel.toggleLoveSong(song) }
                )
                Spacer(modifier = Modifier.height(10.dp))
            }
        }

        AnimatedVisibility(visible = currentSong != null, enter = fadeIn(), exit = fadeOut()) {
            currentSong?.let { song ->
                val progress = if (durationMs > 0L) (currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f
                Surface(
                    modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 12.dp, vertical = 8.dp)
                        .clip(RoundedCornerShape(16.dp)).border(1.dp, GhBorder, RoundedCornerShape(16.dp))
                        .shadow(8.dp, RoundedCornerShape(16.dp)),
                    color = GhSurface, shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().height(3.dp), color = GhButtonPrimary, trackColor = GhBorder)
                        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                            AsyncImage(
                                model = ImageRequest.Builder(context).data(song.albumArt).crossfade(true).memoryCachePolicy(CachePolicy.ENABLED).build(),
                                contentDescription = song.title, contentScale = ContentScale.Crop,
                                modifier = Modifier.size(46.dp).clip(RoundedCornerShape(8.dp)).border(0.5.dp, GhBorder, RoundedCornerShape(8.dp))
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(song.title, fontFamily = InterFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = GhTextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("${song.artist} • ${song.sourceApi}", fontFamily = InterFontFamily, fontSize = 12.sp, color = GhTextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            IconButton(onClick = { musicViewModel.toggleLoveSong(song) }) {
                                Icon(if (song.isLoved) Icons.Default.Favorite else Icons.Default.FavoriteBorder, "Love", tint = if (song.isLoved) Color(0xFFFF2D55) else GhTextSecondary, modifier = Modifier.size(22.dp))
                            }
                            Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(GhButtonPrimary).bouncyClickable { musicViewModel.togglePlayPause() }, contentAlignment = Alignment.Center) {
                                Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, "Play/Pause", tint = Color.White, modifier = Modifier.size(22.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SongCardItem(song: Song, isCurrentlyPlaying: Boolean, onPlayClick: () -> Unit, onLoveClick: () -> Unit) {
    val context = LocalContext.current
    Surface(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).border(0.5.dp, if (isCurrentlyPlaying) GhButtonPrimary else GhBorder, RoundedCornerShape(14.dp)),
        color = GhSurface, shape = RoundedCornerShape(14.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = ImageRequest.Builder(context).data(song.albumArt).crossfade(true).memoryCachePolicy(CachePolicy.ENABLED).build(),
                contentDescription = song.title, contentScale = ContentScale.Crop,
                modifier = Modifier.size(54.dp).clip(RoundedCornerShape(10.dp)).border(0.5.dp, GhBorder, RoundedCornerShape(10.dp)).background(GhBackground)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(song.title, fontFamily = InterFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = if (isCurrentlyPlaying) GhButtonPrimary else GhTextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(song.artist, fontFamily = InterFontFamily, fontSize = 13.sp, color = GhTextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(GhBackground).padding(horizontal = 5.dp, vertical = 2.dp)) {
                        Text(song.sourceApi, fontFamily = InterFontFamily, fontWeight = FontWeight.Medium, fontSize = 10.sp, color = GhButtonPrimary)
                    }
                }
            }
            IconButton(onClick = onLoveClick) {
                Icon(if (song.isLoved) Icons.Default.Favorite else Icons.Default.FavoriteBorder, "Save to Firebase", tint = if (song.isLoved) Color(0xFFFF2D55) else GhTextSecondary, modifier = Modifier.size(24.dp))
            }
            Box(modifier = Modifier.size(38.dp).clip(CircleShape).background(if (isCurrentlyPlaying) GhButtonPrimary else GhSurface).border(1.dp, GhBorder, CircleShape).bouncyClickable(onClick = onPlayClick), contentAlignment = Alignment.Center) {
                Icon(if (isCurrentlyPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, "Play Music", tint = if (isCurrentlyPlaying) Color.White else GhTextPrimary, modifier = Modifier.size(20.dp))
            }
        }
    }
}
