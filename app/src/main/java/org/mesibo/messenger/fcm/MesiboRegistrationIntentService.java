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

package org.mesibo.messenger.fcm;

import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.core.app.JobIntentService;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.firebase.FirebaseApp;
import com.google.firebase.iid.FirebaseInstanceId;


import java.io.IOException;


public class MesiboRegistrationIntentService extends JobIntentService {

    private static final String TAG = "RegIntentService";
    private static final String[] TOPICS = {"global"};
    private static String SENDER_ID= "";
    private static GCMListener mListener = null;

    public MesiboRegistrationIntentService() {
        super();
    }

    public interface GCMListener {
        void Mesibo_onGCMToken(String token);
        void Mesibo_onGCMMessage( boolean inService);
    }

    @Override
    protected void onHandleWork(@NonNull Intent intent) {
      onHandleIntent(intent);
    }

    //@Override
    protected void onHandleIntent(Intent intent) {
        String token = null;
        try {

            FirebaseInstanceId instanceID = (FirebaseInstanceId) FirebaseInstanceId.getInstance(FirebaseApp.initializeApp(this));
            if(null == instanceID) {
                Log.d(TAG, "FCM getInstance Failed");
                return;
            }
          token = instanceID.getToken();
            Log.d(TAG, "FCM Registration Token: " + token);


        } catch (Exception e) {
            Log.d(TAG, "Failed to complete token refresh", e);
        }

        if(null != mListener) {
            mListener.Mesibo_onGCMToken(token);
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
    private void sendRegistrationToServer(String token) {
        // Add custom implementation, as needed.
        Log.d("Token", token);
    }

    /**
     * Subscribe to any GCM topics of interest, as defined by the TOPICS constant.
     *

     * @throws IOException if unable to reach the GCM PubSub service
     */
    // [START subscribe_topics]
    private void subscribeTopics(String token) throws IOException {
//        GcmPubSub pubSub = GcmPubSub.getInstance(this);
//        for (String topic : TOPICS) {
//            pubSub.subscribe(token, "/topics/" + topic, null);
//        }
    }
    // [END subscribe_topics]

    private boolean checkPlayServices() {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = apiAvailability.isGooglePlayServicesAvailable(this);
        return (resultCode == ConnectionResult.SUCCESS);
    }

    public static final int JOB_ID = 1;
    public static void enqueueWork(Context context, Intent work) {
        enqueueWork(context, MesiboRegistrationIntentService.class, JOB_ID, work);
    }

    public static void startRegistration(Context context, String senderId, GCMListener listener) {
        if(!TextUtils.isEmpty(senderId))
            SENDER_ID = senderId;

        if(listener != null)
            mListener = listener;

        try {
            Intent intent = new Intent(context, MesiboRegistrationIntentService.class);
            //context.startService(intent);
            enqueueWork(context, intent);
        } catch (Exception e) {

        }
    }

    public static void sendMessageToListener( boolean inService) {
        if(null != mListener) {
            mListener.Mesibo_onGCMMessage( inService);
        }
    }
}
