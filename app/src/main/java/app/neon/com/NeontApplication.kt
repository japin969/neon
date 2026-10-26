package app.neon.com

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class NeontApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Inisialisasi Firebase Core
        FirebaseApp.initializeApp(this)

        // Firebase App Check (Play Integrity & Debug provider untuk pengujian)
        val firebaseAppCheck = FirebaseAppCheck.getInstance()
        if (BuildConfig.DEBUG) {
            // Mode Debug: menggunakan token debug lokal untuk testing di emulator / studio
            firebaseAppCheck.installAppCheckProviderFactory(
                DebugAppCheckProviderFactory.getInstance()
            )
        } else {
            // Mode Produksi: verifikasi integritas perangkat lewat Google Play Integrity API
            firebaseAppCheck.installAppCheckProviderFactory(
                PlayIntegrityAppCheckProviderFactory.getInstance()
            )
        }
    }
}
