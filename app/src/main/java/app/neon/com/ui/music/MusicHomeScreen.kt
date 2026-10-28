package app.neon.com.ui.music

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import app.neon.com.ui.chat.bouncyClickable
import app.neon.com.ui.theme.GhBackground
import app.neon.com.ui.theme.GhBorder
import app.neon.com.ui.theme.GhButtonPrimary
import app.neon.com.ui.theme.GhSurface
import app.neon.com.ui.theme.GhTextPrimary
import app.neon.com.ui.theme.GhTextSecondary
import app.neon.com.ui.theme.InterFontFamily
import app.neon.com.ui.viewmodel.MusicViewModel
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest

/**
 * MusicHomeScreen - Menghubungkan UI Music ke ExoPlayer & Firebase
 * - Tombol Play: Memutar streaming URL lagu via ExoPlayer (Media3)
 * - Tombol Love/Save: Menyimpan lagu ke Firebase `/users/{uid}/lovedSongs`
 */
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
        modifier = Modifier
            .fillMaxSize()
            .background(GhBackground)
            .statusBarsPadding()
    ) {
        // ==========================================
        // 🍎 Header Music (Inter Bold 34sp)
        // ==========================================
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Music",
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 34.sp,
                    color = GhTextPrimary
                )
                Text(
                    text = "Pencarian 4 API (Spotify, SoundCloud, YouTube, Deezer)",
                    fontFamily = InterFontFamily,
                    fontSize = 12.sp,
                    color = GhTextSecondary
                )
            }
        }

        // Search Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            TextField(
                value = searchQuery,
                onValueChange = {
                    searchQuery = it
                    musicViewModel.loadSongs(it)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(0.5.dp, GhBorder, RoundedCornerShape(12.dp)),
                placeholder = {
                    Text(
                        text = "Cari lagu, artis, atau genre...",
                        fontFamily = InterFontFamily,
                        fontSize = 15.sp,
                        color = GhTextSecondary
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = GhTextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                },
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = GhSurface,
                    unfocusedContainerColor = GhSurface,
                    disabledContainerColor = GhSurface,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = GhTextPrimary,
                    unfocusedTextColor = GhTextPrimary
                )
            )
        }

        // ==========================================
        // 🎵 Daftar Lagu (LazyColumn dengan Key Unik)
        // ==========================================
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        ) {
            items(
                items = songs,
                key = { it.id } // WAJIB KEY UNIK
            ) { song ->
                val isThisSongPlaying = (currentSong?.id == song.id && isPlaying)

                SongCardItem(
                    song = song,
                    isCurrentlyPlaying = isThisSongPlaying,
                    onPlayClick = {
                        if (currentSong?.id == song.id) {
                            musicViewModel.togglePlayPause()
                        } else {
                            musicViewModel.playSong(song)
                        }
                    },
                    onLoveClick = {
                        musicViewModel.toggleLoveSong(song)
                    }
                )
                Spacer(modifier = Modifier.height(10.dp))
            }
        }

        // ==========================================
        // 📻 Mini Player Melayang di Bawah (ExoPlayer)
        // ==========================================
        AnimatedVisibility(
            visible = currentSong != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            currentSong?.let { song ->
                val progress = if (durationMs > 0) (currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, GhBorder, RoundedCornerShape(16.dp))
                        .shadow(8.dp, RoundedCornerShape(16.dp)),
                    color = GhSurface,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp),
                            color = GhButtonPrimary,
                            trackColor = GhBorder
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(song.albumArt)
                                    .crossfade(true)
                                    .memoryCachePolicy(CachePolicy.ENABLED)
                                    .build(),
                                contentDescription = song.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(0.5.dp, GhBorder, RoundedCornerShape(8.dp))
                            )

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = song.title,
                                    fontFamily = InterFontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 15.sp,
                                    color = GhTextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${song.artist} • ${song.sourceApi}",
                                    fontFamily = InterFontFamily,
                                    fontSize = 12.sp,
                                    color = GhTextSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            // Tombol Love di MiniPlayer
                            IconButton(onClick = { musicViewModel.toggleLoveSong(song) }) {
                                Icon(
                                    imageVector = if (song.isLoved) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = "Love",
                                    tint = if (song.isLoved) Color(0xFFFF2D55) else GhTextSecondary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            // Tombol Play/Pause di MiniPlayer
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(GhButtonPrimary)
                                    .bouncyClickable { musicViewModel.togglePlayPause() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = "Play/Pause",
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Kartu Lagu dengan tombol Play (ExoPlayer) dan tombol Love (Firebase)
 */
@Composable
fun SongCardItem(
    song: Song,
    isCurrentlyPlaying: Boolean,
    onPlayClick: () -> Unit,
    onLoveClick: () -> Unit
) {
    val context = LocalContext.current

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(0.5.dp, if (isCurrentlyPlaying) GhButtonPrimary else GhBorder, RoundedCornerShape(14.dp)),
        color = GhSurface,
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Album Art
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(song.albumArt)
                    .crossfade(true)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .build(),
                contentDescription = song.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .border(0.5.dp, GhBorder, RoundedCornerShape(10.dp))
                    .background(GhBackground)
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    color = if (isCurrentlyPlaying) GhButtonPrimary else GhTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = song.artist,
                        fontFamily = InterFontFamily,
                        fontSize = 13.sp,
                        color = GhTextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(GhBackground)
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = song.sourceApi,
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 10.sp,
                            color = GhButtonPrimary
                        )
                    }
                }
            }

            // Tombol Love/Save: Menyimpan ke Firebase /users/{uid}/lovedSongs
            IconButton(onClick = onLoveClick) {
                Icon(
                    imageVector = if (song.isLoved) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Save to Firebase",
                    tint = if (song.isLoved) Color(0xFFFF2D55) else GhTextSecondary,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Tombol Play: Memutar URL lagu di ExoPlayer
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(if (isCurrentlyPlaying) GhButtonPrimary else GhSurface)
                    .border(1.dp, GhBorder, CircleShape)
                    .bouncyClickable(onClick = onPlayClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isCurrentlyPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = "Play Music",
                    tint = if (isCurrentlyPlaying) Color.White else GhTextPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
