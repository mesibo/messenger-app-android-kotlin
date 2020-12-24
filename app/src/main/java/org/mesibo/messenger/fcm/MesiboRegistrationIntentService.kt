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
import android.support.v4.app.JobIntentService
import android.text.TextUtils
import android.util.Log
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.firebase.FirebaseApp
import com.google.firebase.iid.FirebaseInstanceId
import java.io.IOException

class MesiboRegistrationIntentService : JobIntentService() {
    interface GCMListener {
        fun Mesibo_onGCMToken(token: String?)
        fun Mesibo_onGCMMessage(inService: Boolean)
    }

    override fun onHandleWork(intent: Intent) {
        onHandleIntent(intent)
    }

    //@Override
    protected fun onHandleIntent(intent: Intent?) {
        var token: String? = null
        try {
            val instanceID = FirebaseInstanceId.getInstance(FirebaseApp.initializeApp(this)!!) as FirebaseInstanceId
            if (null == instanceID) {
                Log.d(TAG, "FCM getInstance Failed")
                return
            }
            token = instanceID.token
            Log.d(TAG, "FCM Registration Token: $token")
        } catch (e: Exception) {
            Log.d(TAG, "Failed to complete token refresh", e)
        }
        if (null != mListener) {
            mListener!!.Mesibo_onGCMToken(token)
        }
    }

    /**
     * Persist registration to third-party servers.
     *
     * Modify this method to associate the user's GCM registration token with any server-side account
     * maintained by your application.
     *
     * @param token The new token.
     */
    private fun sendRegistrationToServer(token: String) {
        // Add custom implementation, as needed.
        Log.d("Token", token)
    }

    /**
     * Subscribe to any GCM topics of interest, as defined by the TOPICS constant.
     *
     *
     * @throws IOException if unable to reach the GCM PubSub service
     */
    // [START subscribe_topics]
    @Throws(IOException::class)
    private fun subscribeTopics(token: String) {
//        GcmPubSub pubSub = GcmPubSub.getInstance(this);
//        for (String topic : TOPICS) {
//            pubSub.subscribe(token, "/topics/" + topic, null);
//        }
    }

    // [END subscribe_topics]
    private fun checkPlayServices(): Boolean {
        val apiAvailability = GoogleApiAvailability.getInstance()
        val resultCode = apiAvailability.isGooglePlayServicesAvailable(this)
        return resultCode == ConnectionResult.SUCCESS
    }

    companion object {
        private const val TAG = "RegIntentService"
        private val TOPICS = arrayOf("global")
        private var SENDER_ID = ""
        private var mListener: GCMListener? = null
        const val JOB_ID = 1
        fun enqueueWork(context: Context?, work: Intent?) {
            enqueueWork(context!!, MesiboRegistrationIntentService::class.java, JOB_ID, work!!)
        }

        fun startRegistration(context: Context?, senderId: String?, listener: GCMListener?) {
            if (!TextUtils.isEmpty(senderId)) SENDER_ID = senderId.toString()
            if (listener != null) mListener = listener
            try {
                val intent = Intent(context, MesiboRegistrationIntentService::class.java)
                //context.startService(intent);
                enqueueWork(context, intent)
            } catch (e: Exception) {
            }
        }

        fun sendMessageToListener(inService: Boolean) {
            if (null != mListener) {
                mListener!!.Mesibo_onGCMMessage(inService)
            }
        }
    }
}