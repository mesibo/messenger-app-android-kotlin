/**
 * Copyright 2015 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.mesibo.messenger.fcm

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import org.mesibo.messenger.MainApplication

class MesiboGcmListenerService : FirebaseMessagingService() {
    // [START receive_message]
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.e(TAG, "From: " + remoteMessage.from)
        if (remoteMessage == null) return
        val data = Bundle()
        data.putString("body", "newPushNotification")
        MesiboRegistrationIntentService.sendMessageToListener(false)
        val intent = Intent("com.mesibo.someintent")
        intent.putExtras(data)
        MesiboJobIntentService.enqueueWork(MainApplication.getAppContext(), intent)
    }

    override fun onNewToken(s: String) {
        super.onNewToken(s)
        Log.e("newToken", s)
        getSharedPreferences("_", MODE_PRIVATE).edit().putString("fb", s).apply()
    }

    companion object {
        private const val TAG = "FcmListenerService"
        fun getToken(context: Context): String {
            return context.getSharedPreferences("_", MODE_PRIVATE).getString("fb", "empty")
        }
    }
}