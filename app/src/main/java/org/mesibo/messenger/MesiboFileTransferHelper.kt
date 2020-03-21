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

import android.os.Bundle

import com.google.gson.Gson
import com.mesibo.api.Mesibo

class MesiboFileTransferHelper internal constructor() : Mesibo.FileTransferHandler {

    private var mDownloadCounter = 0
    private var mUploadCounter = 0

    class MesiboUrl internal constructor() {
        var op: String? = null
        var file: String? = null
        var result: String? = null
        var xxx: String? = null

        init {
            result = null
            op = null
            xxx = null
            file = null
        }
    }

    init {
        Mesibo.addListener(this)
    }

    fun Mesibo_onStartUpload(params: Mesibo.MessageParams, file: Mesibo.FileInfo): Boolean {

        // we don't need to check origin the way we do in download
        if (Mesibo.getNetworkConnectivity() != Mesibo.CONNECTIVITY_WIFI && !file.userInteraction)
            return false

        // limit simultaneous upload or download
        if (mUploadCounter >= 3 && !file.userInteraction)
            return false

        val mid = file.mid

        val b = Bundle()
        b.putString("op", "upload")
        b.putString("token", SampleAPI.token)
        b.putLong("mid", mid)
        b.putInt("profile", 0)

        updateUploadCounter(1)
        val http = Mesibo.Http()

        http.url = SampleAPI.uploadUrl
        http.postBundle = b
        http.uploadFile = file.path
        http.uploadFileField = "photo"
        http.other = file
        file.fileTransferContext = http

        http.listener = Mesibo.HttpListener { config, state, percent ->
            val f = config.other as Mesibo.FileInfo

            if (100 == percent && Mesibo.Http.STATE_DOWNLOAD == state) {
                val response = config.dataString
                var mesibourl: MesiboUrl? = null
                try {
                    mesibourl = mGson.fromJson(response, MesiboUrl::class.java)
                } catch (e: Exception) {
                }

                if (null == mesibourl || null == mesibourl.file) {
                    Mesibo.updateFileTransferProgress(f, -1, Mesibo.FileInfo.STATUS_FAILED)
                    return@HttpListener false
                }

                //TBD, f.setPath if video is re-compressed
                f.url = mesibourl.file
            }

            var status = f.status
            if (100 == percent || status != Mesibo.FileInfo.STATUS_RETRYLATER) {
                status = Mesibo.FileInfo.STATUS_INPROGRESS
                if (percent < 0)
                    status = Mesibo.FileInfo.STATUS_RETRYLATER
            }

            if (percent < 100 || 100 == percent && Mesibo.Http.STATE_DOWNLOAD == state)
                Mesibo.updateFileTransferProgress(f, percent, status)

            if (100 == percent && Mesibo.Http.STATE_DOWNLOAD == state || status != Mesibo.FileInfo.STATUS_INPROGRESS)
                updateUploadCounter(-1)

            100 == percent && Mesibo.Http.STATE_DOWNLOAD == state || status != Mesibo.FileInfo.STATUS_RETRYLATER
        }

        mQueue?.queue(http) ?: if (http.execute()) {

        }

        return true

    }

    fun Mesibo_onStartDownload(params: Mesibo.MessageParams, file: Mesibo.FileInfo): Boolean {

        //TBD, check file type and size to decide automatic download
        if (!SampleAPI.mediaAutoDownload && Mesibo.getNetworkConnectivity() != Mesibo.CONNECTIVITY_WIFI && !file.userInteraction)
            return false

        // only realtime messages to be downloaded in automatic mode.
        if (Mesibo.ORIGIN_REALTIME != params.origin && !file.userInteraction)
            return false

        // limit simultaneous upload or download, 1st condition is redundant but for reference only
        if (Mesibo.getNetworkConnectivity() != Mesibo.CONNECTIVITY_WIFI && mDownloadCounter >= 3 && !file.userInteraction)
            return false

        updateDownloadCounter(1)

        val mid = file.mid

        var url = file.url
        if (!url.toLowerCase().startsWith("http://") && !url.toLowerCase().startsWith("https://")) {
            url = SampleAPI.downloadUrl + url
        }

        val http = Mesibo.Http()

        http.url = url
        http.downloadFile = file.path
        http.resume = true
        http.maxRetries = 10
        http.other = file
        file.fileTransferContext = http

        http.listener = Mesibo.HttpListener { http, state, percent ->
            val f = http.other as Mesibo.FileInfo

            var status = Mesibo.FileInfo.STATUS_INPROGRESS

            //TBD, we can simplify this now, don't need separate handling
            if (Mesibo.FileInfo.SOURCE_PROFILE == f.source) {
                if (100 == percent) {
                    Mesibo.updateFileTransferProgress(f, percent, Mesibo.FileInfo.STATUS_INPROGRESS)
                }
            } else {

                status = f.status
                if (100 == percent || status != Mesibo.FileInfo.STATUS_RETRYLATER) {
                    status = Mesibo.FileInfo.STATUS_INPROGRESS
                    if (percent < 0)
                        status = Mesibo.FileInfo.STATUS_RETRYLATER
                }

                Mesibo.updateFileTransferProgress(f, percent, status)

            }

            if (100 == percent || status != Mesibo.FileInfo.STATUS_INPROGRESS)
                updateDownloadCounter(-1)

            100 == percent || status != Mesibo.FileInfo.STATUS_RETRYLATER
        }

        mQueue?.queue(http) ?: if (http.execute()) {

        }

        return true
    }

    override fun Mesibo_onStartFileTransfer(file: Mesibo.FileInfo): Boolean {
        return if (Mesibo.FileInfo.MODE_DOWNLOAD == file.mode) Mesibo_onStartDownload(file.params, file) else Mesibo_onStartUpload(file.params, file)

    }

    override fun Mesibo_onStopFileTransfer(file: Mesibo.FileInfo): Boolean {
        val http = file.fileTransferContext as Mesibo.Http
        http?.cancel()

        return true
    }

    @Synchronized
    fun updateDownloadCounter(increment: Int): Int {
        mDownloadCounter += increment
        return mDownloadCounter
    }

    @Synchronized
    fun updateUploadCounter(increment: Int): Int {
        mUploadCounter += increment
        return mUploadCounter
    }

    companion object {

        private val mGson = Gson()
        private val mQueue = Mesibo.HttpQueue(4, 0)
    }
}


