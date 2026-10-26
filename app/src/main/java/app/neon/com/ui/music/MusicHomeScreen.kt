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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.neon.com.data.model.Song
import coil.compose.AsyncImage

@Composable
fun MusicHomeScreen(
    viewModel: MusicViewModel,
    onOpenMusicPlayer: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    val darkBg = Color(0xFF0D0E15)
    val darkCard = Color(0xFF161824)
    val neonGreen = Color(0xFF00FF66)
    val neonCyan = Color(0xFF00E5FF)
    val neonPink = Color(0xFFFF007F)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(darkBg)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header: Greeting & Profile
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = state.greeting,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                    Text(
                        text = "Temukan musik favoritmu di Neont",
                        fontSize = 12.sp,
                        color = Color(0xFF888EA8)
                    )
                }

                // Avatar Icon
                val avatarColor = try {
                    Color(android.graphics.Color.parseColor(state.currentUser?.avatarColorHex ?: "#00FF66"))
                } catch (_: Exception) {
                    neonGreen
                }

                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(avatarColor),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = state.currentUser?.getInitials() ?: "NE",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.Black
                    )
                }
            }

            // Search Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp)
            ) {
                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = { viewModel.onSearchQueryChanged(it) },
                    placeholder = { Text("Cari lagu, artis, atau album...", color = Color(0xFF6B7280)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = neonGreen
                        )
                    },
                    trailingIcon = {
                        if (state.searchQuery.isNotBlank()) {
                            IconButton(onClick = { viewModel.clearSearch() }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear",
                                    tint = Color.White
                                )
                            }
                        }
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = neonGreen,
                        unfocusedBorderColor = Color(0xFF262A3E),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = neonGreen
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                )
            }

            // Content: Search Results vs Home Showcase
            if (state.isSearchActive) {
                // Search Results View
                if (state.isSearching) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = neonGreen)
                    }
                } else if (state.searchResults.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "Lagu tidak ditemukan untuk '${state.searchQuery}'",
                            color = Color(0xFF888EA8),
                            fontSize = 14.sp
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(state.searchResults, key = { "search_${it.id}" }) { song ->
                            SongRowItem(
                                song = song,
                                isPlaying = state.currentSong?.id == song.id && state.isPlaying,
                                isLoved = state.lovedSongIds.contains(song.id),
                                onPlay = {
                                    viewModel.playSong(song, state.searchResults)
                                    onOpenMusicPlayer()
                                },
                                onToggleLove = { viewModel.toggleLoveSong(song) }
                            )
                        }
                    }
                }
            } else {
                // Standard Spotify Home Layout
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    // 1. Section: Trending Now (LazyRow Cards)
                    item {
                        Text(
                            text = "TRENDING NOW",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = neonCyan,
                            modifier = Modifier.padding(start = 20.dp, top = 16.dp, bottom = 12.dp)
                        )

                        if (state.isLoadingTrending) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = neonCyan)
                            }
                        } else {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 20.dp),
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                items(state.trendingSongs, key = { "trending_${it.id}" }) { song ->
                                    TrendingSongCard(
                                        song = song,
                                        isPlaying = state.currentSong?.id == song.id && state.isPlaying,
                                        onClick = {
                                            viewModel.playSong(song, state.trendingSongs)
                                            onOpenMusicPlayer()
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // 2. Section: Rekomendasi Untukmu (LazyColumn)
                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = "REKOMENDASI UNTUKMU",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = neonGreen,
                            modifier = Modifier.padding(start = 20.dp, bottom = 12.dp)
                        )
                    }

                    items(state.recommendedSongs, key = { "rec_${it.id}" }) { song ->
                        Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
                            SongRowItem(
                                song = song,
                                isPlaying = state.currentSong?.id == song.id && state.isPlaying,
                                isLoved = state.lovedSongIds.contains(song.id),
                                onPlay = {
                                    viewModel.playSong(song, state.recommendedSongs)
                                    onOpenMusicPlayer()
                                },
                                onToggleLove = { viewModel.toggleLoveSong(song) }
                            )
                        }
                    }
                }
            }

            // Mini Player Bar (Persistent at bottom when playing)
            AnimatedVisibility(
                visible = state.currentSong != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                state.currentSong?.let { song ->
                    MiniPlayerBar(
                        song = song,
                        isPlaying = state.isPlaying,
                        isLoved = state.isCurrentSongLoved,
                        onBarClick = onOpenMusicPlayer,
                        onTogglePlayPause = { viewModel.togglePlayPause() },
                        onToggleLove = { viewModel.toggleLoveCurrentSong() }
                    )
                }
            }
        }
    }
}

@Composable
private fun TrendingSongCard(
    song: Song,
    isPlaying: Boolean,
    onClick: () -> Unit
) {
    val darkCard = Color(0xFF161824)
    val neonGreen = Color(0xFF00FF66)

    Column(
        modifier = Modifier
            .width(150.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(darkCard)
            .border(
                1.dp,
                if (isPlaying) neonGreen else Color(0xFF262A3E),
                RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(126.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF222638)),
            contentAlignment = Alignment.Center
        ) {
            if (song.coverUrl.isNotBlank()) {
                AsyncImage(
                    model = song.coverUrl,
                    contentDescription = song.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = neonGreen,
                    modifier = Modifier.size(48.dp)
                )
            }

            if (isPlaying) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.45f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Pause,
                        contentDescription = null,
                        tint = neonGreen,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = song.title,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = if (isPlaying) neonGreen else Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Text(
            text = song.artist,
            fontSize = 11.sp,
            color = Color(0xFF888EA8),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

@Composable
private fun SongRowItem(
    song: Song,
    isPlaying: Boolean,
    isLoved: Boolean,
    onPlay: () -> Unit,
    onToggleLove: () -> Unit
) {
    val darkCard = Color(0xFF161824)
    val neonGreen = Color(0xFF00FF66)
    val neonPink = Color(0xFFFF007F)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(darkCard)
            .border(
                1.dp,
                if (isPlaying) neonGreen else Color(0xFF262A3E),
                RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onPlay)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF222638)),
            contentAlignment = Alignment.Center
        ) {
            if (song.coverUrl.isNotBlank()) {
                AsyncImage(
                    model = song.coverUrl,
                    contentDescription = song.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = neonGreen,
                    modifier = Modifier.size(24.dp)
                )
            }

            if (isPlaying) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.4f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Pause,
                        contentDescription = null,
                        tint = neonGreen,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = if (isPlaying) neonGreen else Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${song.artist} • ${song.album}",
                fontSize = 12.sp,
                color = Color(0xFF888EA8),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        IconButton(onClick = onToggleLove) {
            Icon(
                imageVector = if (isLoved) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                contentDescription = "Love",
                tint = if (isLoved) neonPink else Color(0xFF7E849B)
            )
        }
    }
}

@Composable
private fun MiniPlayerBar(
    song: Song,
    isPlaying: Boolean,
    isLoved: Boolean,
    onBarClick: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onToggleLove: () -> Unit
) {
    val neonGreen = Color(0xFF00FF66)
    val neonPink = Color(0xFFFF007F)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(Color(0xFF1A2136), Color(0xFF141726))
                )
            )
            .border(1.dp, neonGreen.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            .clickable(onClick = onBarClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF282D42)),
            contentAlignment = Alignment.Center
        ) {
            if (song.coverUrl.isNotBlank()) {
                AsyncImage(
                    model = song.coverUrl,
                    contentDescription = song.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(imageVector = Icons.Default.MusicNote, contentDescription = null, tint = neonGreen)
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = song.artist,
                fontSize = 11.sp,
                color = Color(0xFF888EA8),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        IconButton(onClick = onToggleLove, modifier = Modifier.size(36.dp)) {
            Icon(
                imageVector = if (isLoved) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                contentDescription = null,
                tint = if (isLoved) neonPink else Color(0xFF888EA8),
                modifier = Modifier.size(20.dp)
            )
        }

        IconButton(
            onClick = onTogglePlayPause,
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(neonGreen)
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = null,
                tint = Color.Black,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}
