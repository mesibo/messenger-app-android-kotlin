/** Copyright (c) 2019 Mesibo
 * https://mesibo.com
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the terms and condition mentioned on https://mesibo.com
 * as well as following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice, this list
 * of conditions, the following disclaimer and links to documentation and source code
 * repository.
 *
 * Redistributions in binary form must reproduce the above copyright notice, this
 * list of conditions and the following disclaimer in the documentation and/or other
 * materials provided with the distribution.
 *
 * Neither the name of Mesibo nor the names of its contributors may be used to endorse
 * or promote products derived from this software without specific prior written
 * permission.
 *
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA,
 * OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 * Documentation
 * https://mesibo.com/documentation/
 *
 * Source Code Repository
 * https://github.com/mesibo/messengerKotlin-app-android
 *
 */

package org.mesibo.messenger


import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import android.support.v7.app.AppCompatActivity

import com.mesibo.messaging.MesiboUI

class LaunchContactsActivity : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Get intent, action and MIME type
        val intent = intent
        val action = intent.action
        val type = intent.type

        if (Intent.ACTION_SEND == action && type != null) {
            if ("text/plain" == type) {
                handleSendText(intent) // Handle text being sent
            } else if (type.startsWith("image/")) {
                handleSendImage(intent) // Handle single image being sent
            }
        } else if (Intent.ACTION_SEND_MULTIPLE == action && type != null) {
            if (type.startsWith("image/")) {
                handleSendMultipleImages(intent) // Handle multiple images being sent
            }
        } else {
            // Handle other intents, such as being started from the home screen
        }

    }

    internal fun handleSendText(intent: Intent) {
        val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
        if (sharedText != null) {
            //Toast.makeText(this, ""+sharedText, Toast.LENGTH_SHORT).show();
            // Update UI to reflect text being shared

            // String text = "Hello Dummy";
            MesiboUI.launchForwardActivity(this, sharedText, true)
            finish()

        }
    }

    internal fun handleSendImage(intent: Intent) {
        val imageUri = intent.getParcelableExtra<Parcelable>(Intent.EXTRA_STREAM) as Uri
        if (imageUri != null) {
            // Update UI to reflect image being shared
        }
    }

    internal fun handleSendMultipleImages(intent: Intent) {
        val imageUris = intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)
        if (imageUris != null) {
            // Update UI to reflect multiple images being shared
        }
    }


    override fun onResume() {
        super.onResume()


        //

    }
}
