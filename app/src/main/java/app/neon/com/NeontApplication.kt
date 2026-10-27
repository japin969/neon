package app.neon.com

import android.app.Application
import com.vanniktech.emoji.EmojiManager
import com.vanniktech.emoji.ios.IosEmojiProvider

/**
 * NeontApplication - Inisialisasi tingkat aplikasi
 * Mengonfigurasi Emoji iOS (Vanniktech IosEmojiProvider) agar seluruh emoji di aplikasi
 * menggunakan grafis otentik Apple iPhone.
 */
class NeontApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Inisialisasi Vanniktech iOS Emoji Provider
        // Mengganti semua font emoji Android default menjadi Apple iOS Style
        EmojiManager.install(IosEmojiProvider())
    }
}
