package app.neon.com.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.neon.com.data.model.Song
import app.neon.com.data.player.MusicPlayerManager
import app.neon.com.data.repository.MusicRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * MusicViewModel - Menghubungkan MusicHomeScreen ke ExoPlayer & Firebase
 * - playSong(song): Memutar streaming audio via ExoPlayer (Media3)
 * - toggleLoveSong(song): Menyimpan/menghapus lagu ke Firebase `/users/{uid}/lovedSongs`
 */
class MusicViewModel(application: Application) : AndroidViewModel(application) {

    private val auth = runCatching { FirebaseAuth.getInstance() }.getOrNull()
    private val repository = MusicRepository()
    private val playerManager = MusicPlayerManager(application.applicationContext)

    private val _songs = MutableStateFlow<List<Song>>(emptyList())
    val songs: StateFlow<List<Song>> = _songs.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val currentSong: StateFlow<Song?> = playerManager.currentSong
    val isPlaying: StateFlow<Boolean> = playerManager.isPlaying
    val currentPositionMs: StateFlow<Long> = playerManager.currentPositionMs
    val durationMs: StateFlow<Long> = playerManager.durationMs

    private val _lovedSongIds = MutableStateFlow<Set<String>>(emptySet())
    val lovedSongIds: StateFlow<Set<String>> = _lovedSongIds.asStateFlow()

    private val currentUserId: String
        get() = auth?.currentUser?.uid ?: "default_user_1"

    init {
        loadSongs("")
        observeLovedSongs()
    }

    private fun observeLovedSongs() {
        viewModelScope.launch {
            repository.observeLovedSongIds(currentUserId).collectLatest { lovedSet ->
                _lovedSongIds.value = lovedSet
                // Perbarui status isLoved pada list lagu
                _songs.value = _songs.value.map { song ->
                    song.copy(isLoved = lovedSet.contains(song.id))
                }
            }
        }
    }

    fun loadSongs(query: String) {
        _searchQuery.value = query
        viewModelScope.launch {
            val list = repository.searchSongs(query)
            val lovedSet = _lovedSongIds.value
            _songs.value = list.map { it.copy(isLoved = lovedSet.contains(it.id)) }
        }
    }

    /**
     * Tombol Play di MusicHomeScreen: Memutar URL lagu via ExoPlayer
     */
    fun playSong(song: Song) {
        playerManager.playSong(song)
    }

    /**
     * Play/Pause Toggle
     */
    fun togglePlayPause() {
        playerManager.togglePlayPause()
    }

    fun seekTo(positionMs: Long) {
        playerManager.seekTo(positionMs)
    }

    /**
     * Tombol Love/Save: Menyimpan lagu ke Firebase `/users/{uid}/lovedSongs`
     */
    fun toggleLoveSong(song: Song) {
        viewModelScope.launch {
            val newLovedState = repository.toggleLoveSong(currentUserId, song)
            val currentSet = _lovedSongIds.value.toMutableSet()
            if (newLovedState) {
                currentSet.add(song.id)
            } else {
                currentSet.remove(song.id)
            }
            _lovedSongIds.value = currentSet

            _songs.value = _songs.value.map {
                if (it.id == song.id) it.copy(isLoved = newLovedState) else it
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        playerManager.release()
    }
}
