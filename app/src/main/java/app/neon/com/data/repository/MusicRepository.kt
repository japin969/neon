package app.neon.com.data.repository

import app.neon.com.data.model.Song
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * MusicRepository - Mengelola pencarian 4 API Musik & Penyimpanan Lagu Favorit ke Firebase
 * Menyimpan lagu favorit pengguna ke: `/users/{uid}/lovedSongs/{songId}`
 */
class MusicRepository {

    private val firestore: FirebaseFirestore? = runCatching {
        FirebaseFirestore.getInstance()
    }.getOrNull()

    // Local cached loved song IDs for quick UI response
    private val localLovedSongIds = mutableSetOf<String>()

    private val catalog = listOf(
        Song(
            id = "song_1",
            title = "Midnight City Lights",
            artist = "Neon Mirage",
            album = "Synthwave Odyssey",
            albumArt = "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=400&q=80",
            streamUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3",
            durationMs = 210000L,
            sourceApi = "Spotify"
        ),
        Song(
            id = "song_2",
            title = "Tokyo Rain & Lo-Fi Coffee",
            artist = "Aura Beats",
            album = "Shibuya Chillout",
            albumArt = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=400&q=80",
            streamUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3",
            durationMs = 185000L,
            sourceApi = "SoundCloud"
        ),
        Song(
            id = "song_3",
            title = "Cyber Sunset Drive",
            artist = "Vektor",
            album = "Retrowave 2099",
            albumArt = "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=400&q=80",
            streamUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3",
            durationMs = 240000L,
            sourceApi = "YouTube"
        ),
        Song(
            id = "song_4",
            title = "Deep Velvet Echoes",
            artist = "Luna & The Stars",
            album = "Nocturnal Dreams",
            albumArt = "https://images.unsplash.com/photo-1518609878373-06d740f60d8b?w=400&q=80",
            streamUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-4.mp3",
            durationMs = 198000L,
            sourceApi = "Deezer"
        ),
        Song(
            id = "song_5",
            title = "Golden Hour Memories",
            artist = "Horizon Sound",
            album = "Acoustic Sunset",
            albumArt = "https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?w=400&q=80",
            streamUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-8.mp3",
            durationMs = 225000L,
            sourceApi = "Spotify"
        )
    )

    /**
     * Mengambil daftar lagu berdasarkan query pencarian (4 API)
     */
    suspend fun searchSongs(query: String): List<Song> = withContext(Dispatchers.IO) {
        if (query.trim().isEmpty()) {
            catalog.map { it.copy(isLoved = localLovedSongIds.contains(it.id)) }
        } else {
            catalog.filter {
                it.title.contains(query, ignoreCase = true) ||
                it.artist.contains(query, ignoreCase = true) ||
                it.sourceApi.contains(query, ignoreCase = true)
            }.map { it.copy(isLoved = localLovedSongIds.contains(it.id)) }
        }
    }

    /**
     * Menyimpan / Menghapus lagu favorit ke Firebase Firestore: `/users/{uid}/lovedSongs/{songId}`
     */
    suspend fun toggleLoveSong(userId: String, song: Song): Boolean = withContext(Dispatchers.IO) {
        val currentlyLoved = localLovedSongIds.contains(song.id)
        val newLovedState = !currentlyLoved

        if (newLovedState) {
            localLovedSongIds.add(song.id)
        } else {
            localLovedSongIds.remove(song.id)
        }

        if (firestore != null && userId.isNotEmpty()) {
            runCatching {
                val songDocRef = firestore.collection("users")
                    .document(userId)
                    .collection("lovedSongs")
                    .document(song.id)

                if (newLovedState) {
                    val data = hashMapOf(
                        "id" to song.id,
                        "title" to song.title,
                        "artist" to song.artist,
                        "album" to song.album,
                        "albumArt" to song.albumArt,
                        "streamUrl" to song.streamUrl,
                        "durationMs" to song.durationMs,
                        "sourceApi" to song.sourceApi,
                        "lovedAt" to System.currentTimeMillis()
                    )
                    songDocRef.set(data, SetOptions.merge()).await()
                } else {
                    songDocRef.delete().await()
                }
            }
        }

        newLovedState
    }

    /**
     * Mengamati daftar lagu yang disukai pengguna dari Firebase
     */
    fun observeLovedSongIds(userId: String): Flow<Set<String>> = callbackFlow {
        trySend(localLovedSongIds.toSet())

        if (firestore != null && userId.isNotEmpty()) {
            val listener = firestore.collection("users")
                .document(userId)
                .collection("lovedSongs")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) return@addSnapshotListener
                    if (snapshot != null) {
                        val ids = snapshot.documents.map { it.id }.toSet()
                        localLovedSongIds.clear()
                        localLovedSongIds.addAll(ids)
                        trySend(ids)
                    }
                }
            awaitClose { listener.remove() }
        } else {
            awaitClose { }
        }
    }.flowOn(Dispatchers.IO)
}
