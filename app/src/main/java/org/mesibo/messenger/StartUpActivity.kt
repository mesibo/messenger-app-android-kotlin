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
 * https://github.com/mesibo/messenger-app-android
 *
 */
package org.mesibo.messenger

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.text.TextUtils
import android.util.Log
import com.mesibo.contactutils.ContactUtils
import com.mesibo.uihelper.MesiboUiHelperConfig
import org.mesibo.messenger.MesiboListeners.Companion.instance
import org.mesibo.messenger.SampleAPI.token

class StartUpActivity : AppCompatActivity() {
    private var mRunInBackground = false
    private val mPermissionAlert = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (intent.getBooleanExtra(INTENTEXIT, false)) {
            Log.d(TAG, "onCreate closing")
            finish()
            return
        }
        mRunInBackground = intent.getBooleanExtra(STARTINBACKGROUND, false)
        if (mRunInBackground) {
            Log.e(TAG, "Moving app to background")
            moveTaskToBack(true)
        } else {
            Log.e(TAG, "Not Moving app to background")
        }
        setContentView(R.layout.activity_blank_launcher)
        startNextActivity()
    }

    fun startNextActivity() {
        if (TextUtils.isEmpty(token)) {
            MesiboUiHelperConfig.mDefaultCountry = ContactUtils.getCountryCode()
            MesiboUiHelperConfig.mPhoneVerificationBottomText = "Note, Mesibo may call instead of sending an SMS if SMS delivery to your phone fails."
            if (null == MesiboUiHelperConfig.mDefaultCountry) {
                MesiboUiHelperConfig.mDefaultCountry = "91"
            }
            if (intent.getBooleanExtra(SKIPTOUR, false)) {
                UIManager.launchLogin(this, instance)
            } else {
                UIManager.launchWelcomeactivity(this, true, instance, instance)
            }
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        } else {
            UIManager.launchMesibo(this, 0, mRunInBackground, true)
        }
        finish()
    }

    // since this activity is singleTask, intent will be delivered here if it's running
    override fun onNewIntent(intent: Intent) {
        Log.d(TAG, "onNewIntent")
        if (intent.getBooleanExtra(INTENTEXIT, false)) {
            finish()
        }
        super.onNewIntent(intent)
    }

    companion object {
        private const val TAG = "MesiboStartupActivity"
        const val INTENTEXIT = "exit"
        const val SKIPTOUR = "skipTour"
        const val STARTINBACKGROUND = "startinbackground"
        fun newInstance(context: Context, startInBackground: Boolean) {
            val i = Intent(context, StartUpActivity::class.java) //MyActivity can be anything which you want to start on bootup...
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            //i.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            i.putExtra(STARTINBACKGROUND, startInBackground)
            context.startActivity(i)
        }
    }
}