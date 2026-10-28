package app.neon.com.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * AuthViewModel - Mengelola status autentikasi Firebase
 * Mendukung Login Email/Password, Anonymous login, dan Logout
 */
class AuthViewModel : ViewModel() {

    private val auth: FirebaseAuth? = runCatching { FirebaseAuth.getInstance() }.getOrNull()

    private val _currentUser = MutableStateFlow<FirebaseUser?>(auth?.currentUser)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private var authListener: FirebaseAuth.AuthStateListener? = null

    init {
        auth?.let { firebaseAuth ->
            authListener = FirebaseAuth.AuthStateListener { fa ->
                _currentUser.value = fa.currentUser
            }
            firebaseAuth.addAuthStateListener(authListener!!)
        }
    }

    fun signInWithEmail(email: String, pass: String) {
        if (email.isBlank() || pass.isBlank()) {
            _errorMessage.value = "Email dan password tidak boleh kosong"
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            runCatching {
                auth?.signInWithEmailAndPassword(email.trim(), pass)?.await()
            }.onFailure { error ->
                _errorMessage.value = error.localizedMessage ?: "Login gagal. Coba lagi."
            }
            _isLoading.value = false
        }
    }

    fun signInAnonymously() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            runCatching {
                auth?.signInAnonymously()?.await()
            }.onFailure { error ->
                _errorMessage.value = error.localizedMessage ?: "Gagal masuk sebagai tamu."
            }
            _isLoading.value = false
        }
    }

    fun signOut() {
        auth?.signOut()
        _currentUser.value = null
    }

    override fun onCleared() {
        super.onCleared()
        authListener?.let { auth?.removeAuthStateListener(it) }
    }
}
