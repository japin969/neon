package app.neon.com.data.local

import android.content.Context
import app.neon.com.data.model.ChatItem
import app.neon.com.data.model.User
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import javax.inject.Inject
import javax.inject.Singleton

data class LocalDatabaseCache(
    var currentUser: User? = null,
    var cachedUsers: MutableMap<String, User> = mutableMapOf(),
    var cachedChats: MutableList<ChatItem> = mutableListOf(),
    var cachedFollowers: MutableList<String> = mutableListOf(),
    var lastSyncTimestamp: Long = 0L,
    var cachedPinHash: String? = null,
    var isPinEnabled: Boolean = false
)

@Singleton
class FileDatabaseManager @Inject constructor(
    private val context: Context
) {
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()
    private val cacheFileName = "neont_local_database.json"
    private val cacheFile: File by lazy {
        File(context.filesDir, cacheFileName)
    }

    private val _currentUserFlow = MutableStateFlow<User?>(null)
    val currentUserFlow: Flow<User?> = _currentUserFlow.asStateFlow()

    private var inMemoryCache: LocalDatabaseCache = LocalDatabaseCache()

    // Volatile session flag: tracks if the user unlocked PIN during current app process
    @Volatile
    private var isPinUnlockedInSession: Boolean = false

    init {
        loadFromDisk()
    }

    private fun loadFromDisk() {
        try {
            if (cacheFile.exists()) {
                FileReader(cacheFile).use { reader ->
                    val type = object : TypeToken<LocalDatabaseCache>() {}.type
                    val loaded: LocalDatabaseCache? = gson.fromJson(reader, type)
                    if (loaded != null) {
                        inMemoryCache = loaded
                        _currentUserFlow.value = loaded.currentUser
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            inMemoryCache = LocalDatabaseCache()
        }
    }

    private suspend fun persistToDisk() = withContext(Dispatchers.IO) {
        try {
            FileWriter(cacheFile).use { writer ->
                gson.toJson(inMemoryCache, writer)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun saveCurrentUser(user: User) {
        inMemoryCache.currentUser = user
        inMemoryCache.cachedUsers[user.uid] = user
        inMemoryCache.lastSyncTimestamp = System.currentTimeMillis()
        _currentUserFlow.value = user
        persistToDisk()
    }

    suspend fun saveCachedChats(chats: List<ChatItem>) {
        inMemoryCache.cachedChats.clear()
        inMemoryCache.cachedChats.addAll(chats)
        persistToDisk()
    }

    fun getCachedChats(): List<ChatItem> {
        return inMemoryCache.cachedChats.toList()
    }

    fun getCurrentUser(): User? {
        return inMemoryCache.currentUser
    }

    suspend fun savePinCache(pinHash: String?, isEnabled: Boolean) {
        inMemoryCache.cachedPinHash = pinHash
        inMemoryCache.isPinEnabled = isEnabled
        persistToDisk()
    }

    fun getCachedPinHash(): String? = inMemoryCache.cachedPinHash
    fun isPinEnabled(): Boolean = inMemoryCache.isPinEnabled

    fun setPinUnlockedInSession(unlocked: Boolean) {
        isPinUnlockedInSession = unlocked
    }

    fun isPinUnlockedInSession(): Boolean = isPinUnlockedInSession

    suspend fun clearSession() {
        inMemoryCache.currentUser = null
        inMemoryCache.cachedPinHash = null
        inMemoryCache.isPinEnabled = false
        isPinUnlockedInSession = false
        _currentUserFlow.value = null
        persistToDisk()
    }

    suspend fun clearAllCache() = withContext(Dispatchers.IO) {
        inMemoryCache = LocalDatabaseCache()
        isPinUnlockedInSession = false
        _currentUserFlow.value = null
        if (cacheFile.exists()) {
            cacheFile.delete()
        }
    }

    fun getCacheSizeInBytes(): Long {
        return if (cacheFile.exists()) cacheFile.length() else 0L
    }
}
