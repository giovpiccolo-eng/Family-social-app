package com.familynest.app.notifications

import com.familynest.app.di.Graph
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Receives FCM pushes and registers this device's token against the signed-in user. */
class FamilyMessagingService : FirebaseMessagingService() {

    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onNewToken(token: String) {
        // Persist the token so the Cloud Function can target this device.
        Graph.auth.currentUid?.let {
            scope.launch { Graph.auth.saveFcmToken(token) }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val title = message.notification?.title ?: message.data["title"] ?: "FamilyNest"
        val body = message.notification?.body ?: message.data["body"] ?: "Something new was shared"
        val postId = message.data["postId"]
        Notifications.show(applicationContext, title, body, postId)
    }
}
