package app.neon.com

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class NeontApplication : Application() {
    override fun onCreate() {
        super.onCreate()
    }
}
