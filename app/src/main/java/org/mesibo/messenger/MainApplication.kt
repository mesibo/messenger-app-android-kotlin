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

import android.app.Application
import android.content.Context
import android.util.Log

import com.mesibo.api.Mesibo

import com.mesibo.mediapicker.MediaPicker
import com.mesibo.calls.MesiboCall
import com.mesibo.messaging.MesiboUI

/**
 * Created by Mesibo on 29/09/17.
 */

class MainApplication : Application(), Mesibo.RestartListener {


    override fun onCreate() {
        super.onCreate()
        appContext = applicationContext
        Mesibo.setRestartListener(this)
        mConfig = AppConfig(this)
        SampleAPI.init(applicationContext)

        mCall = MesiboCall.getInstance()
        mCall!!.init(this)


        val mesiboCallConfig = mCall!!.config
        mesiboCallConfig.backgroundColor = -0xff7975


        val opt = MesiboUI.getConfig()
        opt.mToolbarColor = -0xff7975
        opt.emptyUserListMessage = "Ask your family and friends to download so that you can try out Mesibo functionalities"
        MediaPicker.setToolbarColor(opt.mToolbarColor)


    }

    override fun Mesibo_onRestart() {
        Log.d(TAG, "OnRestart")
        StartUpActivity.newInstance(this, true)
    }

    companion object {
        val TAG = "MesiboSampleApplication"
        var appContext: Context? = null
            private set
        private var mCall: MesiboCall? = null
        private var mConfig: AppConfig? = null

        val restartIntent: String
            get() = "com.mesibo.sampleapp.restart"
    }

}

