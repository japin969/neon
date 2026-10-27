package app.neon.com.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import app.neon.com.data.model.Song
import app.neon.com.data.repository.MusicRepository
import app.neon.com.ui.theme.IosBlue
import app.neon.com.ui.theme.LocalIosColors
import app.neon.com.ui.theme.LocalIosTypography
import kotlinx.coroutines.launch

/**
 * Modifier Tombol Bouncy iOS Ringan (Zero-Lag)
 */
@Composable
fun Modifier.iosBouncyClickable(
    enabled: Boolean = true,
    onClick: () -> Unit
): Modifier {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "iOSBouncy"
    )

    return this
        .scale(scale)
        .pointerInput(enabled) {
            if (enabled) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        tryAwaitRelease()
                        isPressed = false
                        onClick()
                    }
                )
            }
        }
}

@Composable
fun MusicHomeScreen(
    repository: MusicRepository = remember { MusicRepository() },
    currentUserId: String = "user_neont_guest"
) {
    val colors = LocalIosColors.current
    val typography = LocalIosTypography.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<Song>>(emptyList()) }
    var popularSongs by remember { mutableStateOf<List<Song>>(emptyList()) }
    var recentlyPlayedSongs by remember { mutableStateOf<List<Song>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    var currentPlayingSong by remember { mutableStateOf<Song?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var playbackProgress by remember { mutableFloatStateOf(0.40f) }

    // OPTIMASI: derivedStateOf untuk tombol Scroll to Top agar tidak rekomposisi berlebihan
    val showScrollToTop by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 3 }
    }

    // Fungsi putar lagu + Catat ke Firebase playCount & recentlyPlayed
    val playSongAction: (Song) -> Unit = remember {
        { song ->
            currentPlayingSong = song
            isPlaying = true
            scope.launch {
                // Catat ke Firebase di background (ExoPlayer mulai)
                repository.recordSongPlay(song, currentUserId)
                // Refresh daftar sering diputar
                recentlyPlayedSongs = repository.getRecentlyPlayed(currentUserId)
            }
        }
    }

    // Load data Populer dan Sering Diputar saat layar pertama kali dibuka
    LaunchedEffect(Unit) {
        scope.launch {
            popularSongs = repository.getPopularSongs()
        }
        scope.launch {
            recentlyPlayedSongs = repository.getRecentlyPlayed(currentUserId)
        }
    }

    // Pencarian 4 API Paralel (di belakang layar tanpa teks sumber)
    fun performSearch(query: String) {
        if (query.trim().isEmpty()) {
            searchResults = emptyList()
            return
        }
        isSearching = true
        scope.launch {
            val results = repository.searchSongsUnified(query)
            searchResults = results
            isSearching = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // ==========================================
            // 🍎 1. Header Resmi: "Neont Music" (20sp Title)
            // ==========================================
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Neont Music",
                    style = typography.title3,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(colors.surface)
                        .iosBouncyClickable { },
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "🎵", fontSize = 18.sp)
                }
            }

            // ==========================================
            // 🔍 2. Search Bar: "cari lagu anda"
            // ==========================================
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                TextField(
                    value = searchQuery,
                    onValueChange = {
                        searchQuery = it
                        if (it.length >= 2) {
                            performSearch(it)
                        } else if (it.isEmpty()) {
                            searchResults = emptyList()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .shadow(elevation = 2.dp, shape = RoundedCornerShape(12.dp))
                        .clip(RoundedCornerShape(12.dp)),
                    placeholder = {
                        Text(
                            text = "cari lagu anda",
                            style = typography.callout,
                            fontSize = 16.sp,
                            color = colors.textSecondary
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Cari",
                            tint = colors.textSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = {
                                searchQuery = ""
                                searchResults = emptyList()
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Hapus",
                                    tint = colors.textSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = colors.surface,
                        unfocusedContainerColor = colors.surface,
                        disabledContainerColor = colors.surface,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary
                    )
                )
            }

            // Indikator loading pencarian
            if (isSearching) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        color = IosBlue,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Memuat lagu...",
                        style = typography.footnote,
                        color = colors.textSecondary
                    )
                }
            }

            // ==========================================
            // 🎶 3. Konten Utama: LazyColumn dengan unique key (Anti-Lag)
            // ==========================================
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(bottom = 120.dp)
            ) {
                // KONDISI A: Menampilkan Hasil Pencarian
                if (searchQuery.isNotEmpty() && searchResults.isNotEmpty()) {
                    item(key = "header_search_results") {
                        Text(
                            text = "Hasil Pencarian",
                            style = typography.title3,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                        )
                    }

                    items(
                        items = searchResults,
                        key = { it.id } // OPTIMASI: Key Unik
                    ) { song ->
                        val isCurrent = currentPlayingSong?.id == song.id
                        CleanSongListItem(
                            song = song,
                            isCurrent = isCurrent,
                            isPlaying = isCurrent && isPlaying,
                            onSongClick = { playSongAction(song) }
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                } else {
                    // KONDISI B: Menampilkan "Sering Diputar" & "Populer"

                    // SECTION 1: SERING DIPUTAR (DATA ASLI FIREBASE)
                    if (recentlyPlayedSongs.isNotEmpty()) {
                        item(key = "section_recent_header") {
                            Column(modifier = Modifier.padding(top = 10.dp)) {
                                Text(
                                    text = "Sering Diputar",
                                    style = typography.title3,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textPrimary,
                                    modifier = Modifier.padding(horizontal = 20.dp)
                                )
                                Text(
                                    text = "Baru-baru ini diputar",
                                    style = typography.footnote,
                                    color = colors.textSecondary,
                                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 2.dp)
                                )
                            }
                        }

                        item(key = "section_recent_horizontal_list") {
                            LazyRow(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 10.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                items(
                                    items = recentlyPlayedSongs,
                                    key = { "recent_${it.id}" } // OPTIMASI: Key Unik
                                ) { song ->
                                    CleanSongCard(
                                        song = song,
                                        onSongClick = { playSongAction(song) }
                                    )
                                }
                            }
                        }
                    }

                    // SECTION 2: POPULER (TERPOPULER BELAKANGAN INI)
                    item(key = "section_popular_header") {
                        Column(modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)) {
                            Text(
                                text = "Populer",
                                style = typography.title3,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary,
                                modifier = Modifier.padding(horizontal = 20.dp)
                            )
                            Text(
                                text = "Lagu terpopuler belakangan ini",
                                style = typography.footnote,
                                color = colors.textSecondary,
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 2.dp)
                            )
                        }
                    }

                    items(
                        items = popularSongs,
                        key = { "pop_${it.id}" } // OPTIMASI: Key Unik
                    ) { song ->
                        val isCurrent = currentPlayingSong?.id == song.id
                        CleanSongListItem(
                            song = song,
                            isCurrent = isCurrent,
                            isPlaying = isCurrent && isPlaying,
                            onSongClick = { playSongAction(song) }
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
            }
        }

        // ==========================================
        // ⬆️ Tombol Scroll to Top (Smooth & Ringan)
        // ==========================================
        AnimatedVisibility(
            visible = showScrollToTop,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 90.dp, end = 20.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .shadow(4.dp, CircleShape)
                    .clip(CircleShape)
                    .background(colors.surface)
                    .iosBouncyClickable {
                        scope.launch { listState.animateScrollToItem(0) }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowUp,
                    contentDescription = "Scroll to Top",
                    tint = colors.textPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        // ==========================================
        // 🪟 Mini Player iOS Bersih di Bawah
        // ==========================================
        if (currentPlayingSong != null) {
            currentPlayingSong?.let { song ->
                CleanIosMiniPlayer(
                    song = song,
                    isPlaying = isPlaying,
                    progress = playbackProgress,
                    onPlayPauseToggle = { isPlaying = !isPlaying },
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }
    }
}

// ==========================================
// 🎨 KOMPONEN ITEM LIST BERSIH TANPA BADGE API
// ==========================================

@Composable
fun CleanSongListItem(
    song: Song,
    isCurrent: Boolean,
    isPlaying: Boolean,
    onSongClick: () -> Unit
) {
    val colors = LocalIosColors.current
    val typography = LocalIosTypography.current
    val context = LocalContext.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .shadow(elevation = 1.dp, shape = RoundedCornerShape(14.dp))
            .clip(RoundedCornerShape(14.dp))
            .background(if (isCurrent) colors.card else colors.surface)
            .clickable(onClick = onSongClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Coil Image Loader dengan Cache Memory & Crossfade
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(song.albumCoverUrl)
                .crossfade(true)
                .memoryCachePolicy(CachePolicy.ENABLED)
                .build(),
            contentDescription = song.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(colors.card)
        )

        Spacer(modifier = Modifier.width(14.dp))

        // Hanya Judul & Artis (Tanpa Teks Sumber API)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                style = typography.body,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isCurrent) IosBlue else colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = song.artist,
                style = typography.subheadline,
                fontSize = 14.sp,
                color = colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        // Durasi & Tombol Play
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = song.duration,
                style = typography.caption1,
                color = colors.textSecondary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(if (isCurrent) IosBlue else colors.surface)
                    .iosBouncyClickable(onClick = onSongClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isCurrent && isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = "Play",
                    tint = if (isCurrent) Color.White else colors.textPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun CleanSongCard(
    song: Song,
    onSongClick: () -> Unit
) {
    val colors = LocalIosColors.current
    val typography = LocalIosTypography.current
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .width(135.dp)
            .shadow(elevation = 2.dp, shape = RoundedCornerShape(14.dp))
            .clip(RoundedCornerShape(14.dp))
            .background(colors.surface)
            .clickable(onClick = onSongClick)
            .padding(8.dp)
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(song.albumCoverUrl)
                .crossfade(true)
                .memoryCachePolicy(CachePolicy.ENABLED)
                .build(),
            contentDescription = song.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(119.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(colors.card)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = song.title,
            style = typography.body,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = colors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = song.artist,
            style = typography.footnote,
            fontSize = 12.sp,
            color = colors.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun CleanIosMiniPlayer(
    song: Song,
    isPlaying: Boolean,
    progress: Float,
    onPlayPauseToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalIosColors.current
    val typography = LocalIosTypography.current
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .shadow(12.dp, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(colors.card)
    ) {
        // Garis Progress Mini Player
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(colors.separator)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .height(2.dp)
                    .background(IosBlue)
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(song.albumCoverUrl)
                    .crossfade(true)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .build(),
                contentDescription = song.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(colors.surface)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    style = typography.body,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = song.artist,
                    style = typography.caption1,
                    color = colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(IosBlue)
                    .iosBouncyClickable(onClick = onPlayPauseToggle),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = "Toggle",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            IconButton(
                onClick = { },
                modifier = Modifier.size(34.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.SkipNext,
                    contentDescription = "Berikutnya",
                    tint = colors.textPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}
