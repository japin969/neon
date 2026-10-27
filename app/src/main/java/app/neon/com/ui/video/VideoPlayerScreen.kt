package app.neon.com.ui.video

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import app.neon.com.data.model.VideoPost

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun VideoPlayerScreen(
    videos: List<VideoPost>,
    initialIndex: Int = 0,
    onBackClick: () -> Unit = {}
) {
    if (videos.isEmpty()) return

    val pagerState = rememberPagerState(initialPage = initialIndex) { videos.size }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        VerticalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val video = videos[page]
            VideoPage(video = video, isActive = pagerState.currentPage == page)
        }

        // Tombol Back
        IconButton(
            onClick = onBackClick,
            modifier = Modifier.padding(16.dp).align(Alignment.TopStart)
        ) {
            Icon(Icons.Default.ArrowBack, "Back", tint = Color.White)
        }
    }
}

@Composable
fun VideoPage(video: VideoPost, isActive: Boolean) {
    val context = LocalContext.current
    
    // ExoPlayer
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(video.videoUrl))
            prepare()
        }
    }

    // Play/Pause berdasarkan halaman aktif
    LaunchedEffect(isActive) {
        if (isActive) exoPlayer.play() else exoPlayer.pause()
    }

    DisposableEffect(Unit) {
        onDispose { exoPlayer.release() }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Video Player
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Overlay Kanan (Like, Komen, Share, Simpan)
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 12.dp, bottom = 100.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Foto Profil
            Icon(
                imageVector = Icons.Default.AccountCircle,
                contentDescription = "Profile",
                tint = Color.White,
                modifier = Modifier.size(48.dp)
            )
            
            // Like
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Favorite, "Like", tint = Color.White, modifier = Modifier.size(32.dp))
                Text("${video.likesCount}", color = Color.White, fontSize = 12.sp)
            }
            
            // Komentar
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Comment, "Comment", tint = Color.White, modifier = Modifier.size(32.dp))
                Text("${video.commentsCount}", color = Color.White, fontSize = 12.sp)
            }
            
            // Share
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Share, "Share", tint = Color.White, modifier = Modifier.size(32.dp))
                Text("${video.sharesCount}", color = Color.White, fontSize = 12.sp)
            }
            
            // Simpan
            Icon(Icons.Default.Bookmark, "Save", tint = Color.White, modifier = Modifier.size(32.dp))
        }

        // Overlay Kiri Bawah (Info Video)
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 12.dp, bottom = 100.dp)
        ) {
            Text("@${video.username}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(video.caption, color = Color.White, fontSize = 14.sp, modifier = Modifier.padding(top = 4.dp))
        }
    }
}
