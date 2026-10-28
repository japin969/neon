package app.neon.com.data.model

import androidx.compose.runtime.Immutable

@Immutable
data class Song(
    val id: String = "",
    val title: String = "",
    val artist: String = "",
    val album: String = "",
    val albumArt: String = "",
    val streamUrl: String = "",
    val durationMs: Long = 0L,
    val isLoved: Boolean = false,
    val sourceApi: String = "Spotify"
)
