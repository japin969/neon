package app.neon.com.data.repository

import app.neon.com.data.model.Song
import app.neon.com.data.model.SongSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/**
 * MusicRepository - Agregator 4 API Musik Paralel + Integrasi Firebase
 */
class MusicRepository(
    private val vercelBaseUrl: String = "https://neont-music-api.vercel.app"
) {

    private val connectTimeoutMs = 12000
    private val readTimeoutMs = 15000

    /**
     * 1. PENCARIAN 4 API SECARA PARALEL (1 REQUEST ASYNC)
     * Tetap memanggil 4 API di belakang layar dan menghapus duplikat.
     */
    suspend fun searchSongsUnified(query: String): List<Song> = withContext(Dispatchers.IO) {
        val cleanQuery = query.trim()
        if (cleanQuery.isEmpty()) return@withContext emptyList()

        val encodedQuery = URLEncoder.encode(cleanQuery, StandardCharsets.UTF_8.name())

        coroutineScope {
            val vercelDeferred = async { fetchVercelSongs(encodedQuery) }
            val nexaSpotifyDeferred = async { fetchNexaSpotifyPlay(encodedQuery) }
            val nexaYtDeferred = async { fetchNexaYtPlay(encodedQuery) }
            val nexaSpotifyUrlDeferred = async { fetchNexaSpotifyUrl(cleanQuery) }

            val vercelResults = runCatching { vercelDeferred.await() }.getOrDefault(emptyList())
            val nexaSpotifyResults = runCatching { nexaSpotifyDeferred.await() }.getOrDefault(emptyList())
            val nexaYtResults = runCatching { nexaYtDeferred.await() }.getOrDefault(emptyList())
            val nexaSpotifyUrlResults = runCatching { nexaSpotifyUrlDeferred.await() }.getOrDefault(emptyList())

            val combined = mutableListOf<Song>().apply {
                addAll(vercelResults)
                addAll(nexaSpotifyResults)
                addAll(nexaYtResults)
                addAll(nexaSpotifyUrlResults)
            }

            deduplicateSongs(combined)
        }
    }

    /**
     * 2. LOGIKA "POPULER": Mengambil lagu trending/populer dari API Vercel
     */
    suspend fun getPopularSongs(): List<Song> = withContext(Dispatchers.IO) {
        val popularList = mutableListOf<Song>()
        try {
            // Mencoba endpoint /trending atau /modules dari API Vercel
            val urlString = "$vercelBaseUrl/trending"
            val jsonString = performHttpGet(urlString)
            if (jsonString != null) {
                val root = JSONObject(jsonString)
                val items = when {
                    root.has("data") -> root.getJSONArray("data")
                    root.has("songs") -> root.getJSONArray("songs")
                    root.has("results") -> root.getJSONArray("results")
                    else -> null
                }
                if (items != null) {
                    for (i in 0 until items.length()) {
                        val item = items.getJSONObject(i)
                        popularList.add(
                            Song(
                                id = "pop_${item.optString("id", i.toString())}",
                                title = item.optString("title", item.optString("name", "Populer")),
                                artist = item.optString("artist", item.optString("author", "Artis Populer")),
                                duration = formatDuration(item.optLong("duration", 210000L)),
                                albumCoverUrl = item.optString("cover", item.optString("image", item.optString("thumbnail", ""))),
                                audioUrl = item.optString("downloadUrl", item.optString("audioUrl", item.optString("url", ""))),
                                source = SongSource.VERCEL,
                                album = item.optString("album", "Hits Terpopuler")
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Jika endpoint /trending kosong, lakukan fallback query populer dari 4 API
        if (popularList.isEmpty()) {
            return@withContext searchSongsUnified("Top Indonesia Global 2026")
        }

        deduplicateSongs(popularList)
    }

    /**
     * 3. LOGIKA "SERING DIPUTAR" (FIREBASE ASLI)
     * Dipanggil setiap kali ExoPlayer memutar lagu:
     * - Tambah playCount di /songs/{songId}
     * - Simpan histori ke /users/{uid}/recentlyPlayed/{songId} dengan timestamp
     */
    suspend fun recordSongPlay(song: Song, userId: String) = withContext(Dispatchers.IO) {
        if (userId.isEmpty() || song.id.isEmpty()) return@withContext

        try {
            // Menggunakan Firebase Firestore Android SDK
            val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()

            // 1. Tambah playCount di /songs/{songId}
            val songRef = firestore.collection("songs").document(song.id)
            val songData = hashMapOf(
                "title" to song.title,
                "artist" to song.artist,
                "albumCoverUrl" to song.albumCoverUrl,
                "duration" to song.duration,
                "audioUrl" to song.audioUrl,
                "playCount" to com.google.firebase.firestore.FieldValue.increment(1),
                "lastPlayedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
            )
            songRef.set(songData, com.google.firebase.firestore.SetOptions.merge()).await()

            // 2. Simpan ke riwayat user /users/{uid}/recentlyPlayed/{songId}
            val userRecentRef = firestore.collection("users")
                .document(userId)
                .collection("recentlyPlayed")
                .document(song.id)

            val recentPayload = hashMapOf(
                "id" to song.id,
                "title" to song.title,
                "artist" to song.artist,
                "albumCoverUrl" to song.albumCoverUrl,
                "duration" to song.duration,
                "audioUrl" to song.audioUrl,
                "timestamp" to System.currentTimeMillis()
            )
            userRecentRef.set(recentPayload, com.google.firebase.firestore.SetOptions.merge()).await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 4. BACA DATA "SERING DIPUTAR" DARI FIREBASE
     * Mengambil lagu riwayat user yang diurutkan berdasarkan timestamp terbaru
     */
    suspend fun getRecentlyPlayed(userId: String): List<Song> = withContext(Dispatchers.IO) {
        if (userId.isEmpty()) return@withContext emptyList()

        val list = mutableListOf<Song>()
        try {
            val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            val querySnapshot = firestore.collection("users")
                .document(userId)
                .collection("recentlyPlayed")
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(25)
                .get()
                .await()

            for (doc in querySnapshot.documents) {
                val id = doc.getString("id") ?: doc.id
                val title = doc.getString("title") ?: "Unknown Title"
                val artist = doc.getString("artist") ?: "Unknown Artist"
                val cover = doc.getString("albumCoverUrl") ?: ""
                val duration = doc.getString("duration") ?: "3:30"
                val audioUrl = doc.getString("audioUrl") ?: ""

                list.add(
                    Song(
                        id = id,
                        title = title,
                        artist = artist,
                        duration = duration,
                        albumCoverUrl = cover,
                        audioUrl = audioUrl,
                        source = SongSource.VERCEL,
                        album = "Sering Diputar"
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext list
    }

    // ==========================================
    // Fetcher 4 API Private Methods
    // ==========================================
    private fun fetchVercelSongs(encodedQuery: String): List<Song> {
        val list = mutableListOf<Song>()
        try {
            val res = performHttpGet("$vercelBaseUrl/search/songs?query=$encodedQuery") ?: return emptyList()
            val root = JSONObject(res)
            val arr = when {
                root.has("data") -> root.optJSONArray("data")
                root.has("songs") -> root.optJSONArray("songs")
                root.has("results") -> root.optJSONArray("results")
                else -> null
            }
            if (arr != null) {
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    list.add(
                        Song(
                            id = "vc_${o.optString("id", i.toString())}",
                            title = o.optString("title", o.optString("name", "Judul")),
                            artist = o.optString("artist", o.optString("author", "Artis")),
                            duration = formatDuration(o.optLong("duration", 0L)),
                            albumCoverUrl = o.optString("cover", o.optString("image", o.optString("thumbnail", ""))),
                            audioUrl = o.optString("downloadUrl", o.optString("audioUrl", o.optString("url", ""))),
                            source = SongSource.VERCEL
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    private fun fetchNexaSpotifyPlay(encodedQuery: String): List<Song> {
        val list = mutableListOf<Song>()
        try {
            val res = performHttpGet("https://api.nexadev.my.id/api/spotifyplay?q=$encodedQuery") ?: return emptyList()
            val root = JSONObject(res)
            if (root.optInt("status", 200) in 200..299 && root.has("result")) {
                val r = root.optJSONObject("result")
                if (r != null) {
                    val title = r.optString("title", "Lagu")
                    val artist = r.optString("artists", r.optString("artist", "Artis"))
                    list.add(
                        Song(
                            id = "sp_p_${title.hashCode()}_${artist.hashCode()}",
                            title = title,
                            artist = artist,
                            duration = r.optString("duration", "3:40"),
                            albumCoverUrl = r.optString("image", r.optString("cover", "")),
                            audioUrl = r.optString("download_url", r.optString("url", "")),
                            source = SongSource.SPOTIFY_PLAY
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    private fun fetchNexaYtPlay(encodedQuery: String): List<Song> {
        val list = mutableListOf<Song>()
        try {
            val res = performHttpGet("https://api.nexadev.my.id/api/ytplay?q=$encodedQuery") ?: return emptyList()
            val root = JSONObject(res)
            if (root.optInt("status", 200) in 200..299 && root.has("result")) {
                val r = root.optJSONObject("result")
                if (r != null) {
                    val title = cleanTitle(r.optString("title", "Lagu"))
                    val artist = r.optString("channel", r.optString("author", "Artis"))
                    list.add(
                        Song(
                            id = "yt_p_${title.hashCode()}",
                            title = title,
                            artist = artist,
                            duration = r.optString("duration", "4:00"),
                            albumCoverUrl = r.optString("thumbnail", r.optString("image", "")),
                            audioUrl = r.optString("download_url", r.optString("url", "")),
                            source = SongSource.YOUTUBE_PLAY
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    private fun fetchNexaSpotifyUrl(rawQuery: String): List<Song> {
        if (!rawQuery.contains("open.spotify.com/track/")) return emptyList()
        val list = mutableListOf<Song>()
        try {
            val enc = URLEncoder.encode(rawQuery, StandardCharsets.UTF_8.name())
            val res = performHttpGet("https://api.nexadev.my.id/api/spotify?url=$enc") ?: return emptyList()
            val root = JSONObject(res)
            if (root.has("result")) {
                val r = root.getJSONObject("result")
                list.add(
                    Song(
                        id = "sp_u_${r.optString("title").hashCode()}",
                        title = r.optString("title", "Spotify Track"),
                        artist = r.optString("artists", r.optString("artist", "Artis")),
                        duration = r.optString("duration", "3:30"),
                        albumCoverUrl = r.optString("image", r.optString("cover", "")),
                        audioUrl = r.optString("download_url", ""),
                        source = SongSource.SPOTIFY_URL
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    private fun performHttpGet(urlString: String): String? {
        var conn: HttpURLConnection? = null
        return try {
            conn = (URL(urlString).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = connectTimeoutMs
                readTimeout = readTimeoutMs
                setRequestProperty("User-Agent", "NeontMusic-Android/2.0")
                setRequestProperty("Accept", "application/json")
            }
            if (conn.responseCode in 200..299) {
                BufferedReader(InputStreamReader(conn.inputStream, StandardCharsets.UTF_8)).use { it.readText() }
            } else null
        } catch (e: Exception) {
            null
        } finally {
            conn?.disconnect()
        }
    }

    private fun deduplicateSongs(songs: List<Song>): List<Song> {
        val seen = mutableSetOf<String>()
        val result = mutableListOf<Song>()
        for (song in songs) {
            val key = "${song.title.lowercase().replace(Regex("[^a-z0-9]"), "")}_${song.artist.lowercase().replace(Regex("[^a-z0-9]"), "")}"
            if (seen.add(key)) {
                result.add(song)
            }
        }
        return result
    }

    private fun cleanTitle(title: String): String {
        return title
            .replace(Regex("(?i)\\[official.*?\\]|\\(official.*?\\)"), "")
            .replace(Regex("(?i)\\[mv\\]|\\(mv\\)"), "")
            .replace(Regex("(?i)\\[audio\\]|\\(audio\\)"), "")
            .trim()
    }

    private fun formatDuration(ms: Long): String {
        if (ms <= 0) return "3:30"
        val totalSec = ms / 1000
        val m = totalSec / 60
        val s = totalSec % 60
        return "%d:%02d".format(m, s)
    }
}
