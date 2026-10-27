package app.neon.com.utils

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class NeontMessagingService : FirebaseMessagingService() {
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        
        val title = remoteMessage.notification?.title ?: "Neont"
        val body = remoteMessage.notification?.body ?: "Pesan baru"
        
        NotificationHelper.showNotification(applicationContext, title, body)
    }
}
