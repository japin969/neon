package app.neon.com.util

import android.util.Patterns
import java.util.regex.Pattern

/**
 * Enterprise Security Utility
 * Berisi helper sanitasi input untuk mencegah XSS, SQLi, LDAPi injection,
 * serta validasi regex password dan email sebelum diteruskan ke Firebase.
 */
object Utils {

    /**
     * Regex untuk Password:
     * - Minimal 8 karakter
     * - Minimal 1 huruf besar (A-Z)
     * - Minimal 1 huruf kecil (a-z)
     * - Minimal 1 angka (0-9)
     */
    private val PASSWORD_PATTERN: Pattern = Pattern.compile(
        "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}$"
    )

    /**
     * SANITASI INPUT (Anti-Injection / XSS):
     * Menghapus karakter berbahaya seperti:
     * '<', '>', '"', '\'', ';', '--', dan token 'script' (case-insensitive).
     * Diterapkan pada parameter Email dan field input sebelum dikirim ke Firebase.
     */
    fun sanitizeInput(input: String): String {
        if (input.isBlank()) return ""

        var sanitized = input.trim()

        // 1. Hapus token "script" (case-insensitive)
        sanitized = sanitized.replace(Regex("(?i)script"), "")

        // 2. Hapus tag XML/HTML (< dan >)
        sanitized = sanitized.replace("<", "").replace(">", "")

        // 3. Hapus tanda kutip tunggal dan ganda (Anti-SQLi escape & string break)
        sanitized = sanitized.replace("'", "").replace("\"", "")

        // 4. Hapus semicolon (;) yang umum digunakan untuk chaining injection command
        sanitized = sanitized.replace(";", "")

        // 5. Hapus comment marker SQL (--)
        sanitized = sanitized.replace("--", "")

        return sanitized.trim()
    }

    /**
     * Validasi Email menggunakan Android standard Patterns.EMAIL_ADDRESS
     * Didahului oleh sanitasi input terlebih dahulu.
     */
    fun isValidEmail(email: String): Boolean {
        if (email.isBlank()) return false
        val sanitized = sanitizeInput(email)
        return Patterns.EMAIL_ADDRESS.matcher(sanitized).matches()
    }

    /**
     * Validasi Password Kuat:
     * Minimal 8 karakter, wajib kombinasi huruf besar, huruf kecil, dan angka.
     */
    fun isValidPassword(password: String): Boolean {
        if (password.length < 8) return false
        return PASSWORD_PATTERN.matcher(password).matches()
    }

    /**
     * Pesan validasi human-readable untuk feedback UI
     */
    fun getPasswordValidationErrorMessage(password: String): String? {
        if (password.isBlank()) return "Password tidak boleh kosong."
        if (password.length < 8) return "Password minimal 8 karakter."
        if (!password.any { it.isUpperCase() }) return "Password harus mengandung minimal satu huruf kapital (A-Z)."
        if (!password.any { it.isLowerCase() }) return "Password harus mengandung minimal satu huruf kecil (a-z)."
        if (!password.any { it.isDigit() }) return "Password harus mengandung minimal satu angka (0-9)."
        return null
    }
}
