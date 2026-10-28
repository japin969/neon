package app.neon.com.ui.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.neon.com.data.model.Song
import app.neon.com.data.player.MusicPlayerManager
import app.neon.com.data.repository.MusicRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MusicViewModel @Inject constructor(
    @ApplicationContext private val application: Application,
    private val repository: MusicRepository
) : ViewModel() {

    private val playerManager = MusicPlayerManager(application.applicationContext)

    private val _songs = MutableStateFlow<List<Song>>(emptyList())
    val songs: StateFlow<List<Song>> = _songs.asStateFlow()

    init {
        loadSongs()
    }

    private fun loadSongs() {
        viewModelScope.launch {
            try {
                val list = repository.getPopularSongs()
                _songs.value = list
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
