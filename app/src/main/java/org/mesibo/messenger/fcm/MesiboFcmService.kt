package org.mesibo.messenger.fcm

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/** FCM events; handled in MesiboPushHelper */
class MesiboFcmService : FirebaseMessagingService() {

    // background thread; the push only wakes the app, the payload is not used
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        MesiboPushHelper.onMessage()
    }

    override fun onNewToken(token: String) {
        MesiboPushHelper.setToken(token)
    }
}
