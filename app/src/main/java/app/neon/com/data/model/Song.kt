package app.neon.com.data.model

import com.google.gson.annotations.SerializedName

data class Song(
    val id: String = "",
    val title: String = "",
    val artist: String = "",
    val album: String = "",
    val coverUrl: String = "",
    val audioUrl: String = "",
    val durationSeconds: Int = 0,
    val isLoved: Boolean = false
) {
    fun getFormattedDuration(): String {
        val minutes = durationSeconds / 60
        val seconds = durationSeconds % 60
        return String.format(java.util.Locale.getDefault(), "%02d:%02d", minutes, seconds)
    }
}

// Retrofit DTO Models for https://neont-music.vercel.app/api
data class MusicSearchApiResponse(
    @SerializedName("status") val status: String? = null,
    @SerializedName("message") val message: String? = null,
    @SerializedName("data") val data: MusicSearchData? = null
)

data class MusicSearchData(
    @SerializedName("total") val total: Int? = null,
    @SerializedName("start") val start: Int? = null,
    @SerializedName("results") val results: List<SongDto>? = null
)

data class ModulesApiResponse(
    @SerializedName("status") val status: String? = null,
    @SerializedName("data") val data: ModulesData? = null
)

data class ModulesData(
    @SerializedName("trending") val trending: TrendingBlock? = null
)

data class TrendingBlock(
    @SerializedName("songs") val songs: List<SongDto>? = null
)

data class SongDto(
    @SerializedName("id") val id: String? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("title") val title: String? = null,
    @SerializedName("type") val type: String? = null,
    @SerializedName("year") val year: String? = null,
    @SerializedName("releaseDate") val releaseDate: String? = null,
    @SerializedName("duration") val duration: Any? = null, // can be String or Int
    @SerializedName("label") val label: String? = null,
    @SerializedName("primaryArtists") val primaryArtists: Any? = null,
    @SerializedName("artists") val artists: ArtistsDto? = null,
    @SerializedName("album") val album: AlbumDto? = null,
    @SerializedName("image") val image: Any? = null, // can be List<ImageDto> or String
    @SerializedName("downloadUrl") val downloadUrl: Any? = null, // can be List<DownloadUrlDto> or String
    @SerializedName("url") val url: String? = null
) {
    fun toSong(): Song {
        val resolvedTitle = name ?: title ?: "Unknown Track"

        val resolvedArtist = when {
            artists?.primary?.isNotEmpty() == true -> artists.primary.joinToString(", ") { it.name ?: "" }
            primaryArtists is String -> primaryArtists
            else -> "Neont Artist"
        }

        val resolvedAlbum = album?.name ?: "Single"

        val durationSec = when (duration) {
            is Number -> duration.toInt()
            is String -> duration.toIntOrNull() ?: 180
            else -> 180
        }

        val resolvedCover = when (image) {
            is List<*> -> {
                // Find highest quality (e.g., 500x500)
                val lastItem = image.lastOrNull()
                if (lastItem is Map<*, *>) {
                    lastItem["url"] as? String ?: (lastItem["link"] as? String ?: "")
                } else {
                    lastItem?.toString() ?: ""
                }
            }
            is String -> image
            else -> ""
        }

        val resolvedAudio = when (downloadUrl) {
            is List<*> -> {
                // Find 320kbps or highest quality link
                val best = downloadUrl.lastOrNull()
                if (best is Map<*, *>) {
                    best["url"] as? String ?: (best["link"] as? String ?: "")
                } else {
                    best?.toString() ?: ""
                }
            }
            is String -> downloadUrl
            else -> url ?: ""
        }

        return Song(
            id = id ?: java.util.UUID.randomUUID().toString(),
            title = resolvedTitle,
            artist = resolvedArtist,
            album = resolvedAlbum,
            coverUrl = resolvedCover,
            audioUrl = resolvedAudio,
            durationSeconds = durationSec,
            isLoved = false
        )
    }
}

data class ArtistsDto(
    @SerializedName("primary") val primary: List<ArtistItemDto>? = null
)

data class ArtistItemDto(
    @SerializedName("id") val id: String? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("role") val role: String? = null
)

data class AlbumDto(
    @SerializedName("id") val id: String? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("url") val url: String? = null
)

data class ImageDto(
    @SerializedName("quality") val quality: String? = null,
    @SerializedName("url") val url: String? = null
)

data class DownloadUrlDto(
    @SerializedName("quality") val quality: String? = null,
    @SerializedName("url") val url: String? = null
)
