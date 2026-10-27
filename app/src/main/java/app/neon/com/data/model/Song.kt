package app.neon.com.data.model

import androidx.compose.runtime.Immutable

@Immutable
data class Song(
    val id: String,
    val title: String,
    val artist: String,
    val duration: String,
    val albumCoverUrl: String,
    val audioUrl: String,
    val source: SongSource,
    val externalUrl: String? = null,
    val bitrate: String? = "320kbps",
    val album: String? = null
)

enum class SongSource(val displayName: String, val badgeColorHex: Long) {
    VERCEL("Vercel Music", 0xFF007AFF),
    SPOTIFY_PLAY("Spotify Play", 0xFF1DB954),
    YOUTUBE_PLAY("YouTube Music", 0xFFFF0000),
    SPOTIFY_URL("Spotify Direct", 0xFF1ED760)
}
