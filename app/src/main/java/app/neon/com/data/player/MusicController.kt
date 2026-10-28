package app.neon.com.data.player

import android.content.Context
import app.neon.com.data.model.Song
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MusicController @Inject constructor(
    context: Context
) {
    private val playerManager = MusicPlayerManager(context)

    val currentSong: StateFlow<Song?> = playerManager.currentSong
    val isPlaying: StateFlow<Boolean> = playerManager.isPlaying
    val currentPositionMs: StateFlow<Long> = playerManager.currentPositionMs
    val durationMs: StateFlow<Long> = playerManager.durationMs

    private var currentPlaylist: List<Song> = emptyList()

    fun playSong(song: Song, playlist: List<Song> = emptyList()) {
        currentPlaylist = playlist
        playerManager.playSong(song)
    }

    fun togglePlayPause() {
        playerManager.togglePlayPause()
    }

    fun seekTo(positionMs: Long) {
        playerManager.seekTo(positionMs)
    }

    fun next() {
        val song = currentSong.value ?: return
        if (currentPlaylist.isEmpty()) return
        val currentIndex = currentPlaylist.indexOfFirst { it.id == song.id }
        if (currentIndex != -1 && currentIndex + 1 < currentPlaylist.size) {
            playSong(currentPlaylist[currentIndex + 1], currentPlaylist)
        }
    }

    fun previous() {
        val song = currentSong.value ?: return
        if (currentPlaylist.isEmpty()) return
        val currentIndex = currentPlaylist.indexOfFirst { it.id == song.id }
        if (currentIndex > 0) {
            playSong(currentPlaylist[currentIndex - 1], currentPlaylist)
        }
    }

    fun release() {
        playerManager.release()
    }
}
