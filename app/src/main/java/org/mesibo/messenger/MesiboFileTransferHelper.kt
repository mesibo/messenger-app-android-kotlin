package org.mesibo.messenger

import android.os.Bundle
import android.text.TextUtils
import com.mesibo.api.Mesibo
import com.mesibo.api.MesiboFileTransfer
import com.mesibo.api.MesiboHttp
import org.json.JSONObject
import java.util.Locale

class MesiboFileTransferHelper internal constructor() : Mesibo.FileTransferHandler {
    class MesiboUrl internal constructor() {
        var op: String? = null
        var file: String? = null
        var result: String? = null
        var xxx: String? = null
    }

    fun Mesibo_onStartUpload(file: MesiboFileTransfer): Boolean {
        // we don't need to check origin the way we do in download

        if (mUploadCounter >= 5 && file.priority == 0) return false

        val mid = file.mid

        val b = Bundle()
        b.putString("op", "upload")
        b.putString("token", SampleAPI.token)
        b.putLong("mid", mid)
        b.putInt("profile", 0)

        updateUploadCounter(1)
        val http = MesiboHttp()

        http.url = SampleAPI.uploadUrl
        http.postBundle = b
        http.uploadFile = file.path
        http.uploadFileField = "file"
        http.other = file
        file.fileTransferContext = http

        http.listener = MesiboHttp.Listener { config, state, percent ->
            val f = config.other as MesiboFileTransfer
            if (100 == percent && MesiboHttp.STATE_DOWNLOAD == state) {
                val response = config.dataString
                var url: String? = null
                var result = false

                try {
                    val jo = JSONObject(response)
                    if (null != jo) {
                        url = jo.getString("url")
                        result = jo.getBoolean("result")
                    }
                } catch (e: Exception) {
                }

                f.setResult(result, url)
                return@Listener true
            }

            if (percent < 0) {
                f.setResult(false, null)
                return@Listener true
            }

            f.setProgress(percent)
            true
        }

        if (null != mQueue) mQueue.queue(http)
        else if (http.execute()) {
        }

        return true
    }

    fun Mesibo_onStartDownload(file: MesiboFileTransfer): Boolean {
        //TBD, check file type and size to decide automatic download

        if (!SampleAPI.mediaAutoDownload && Mesibo.getNetworkConnectivity() != Mesibo.CONNECTIVITY_WIFI && 0 == file.priority) return false

        // only realtime messages to be downloaded in automatic mode.
        if (Mesibo.ORIGIN_REALTIME != file.origin && Mesibo.ORIGIN_DBPENDING != file.origin && file.priority == 0) return false


        updateDownloadCounter(1)

        val mid = file.mid

        var url = file.url
        if (TextUtils.isEmpty(url)) return false

        if (!url.lowercase(Locale.getDefault())
                .startsWith("http://") && !url.lowercase(Locale.getDefault()).startsWith("https://")
        ) {
            url = SampleAPI.downloadUrl + url
        }

        val http = MesiboHttp()

        http.url = url
        http.downloadFile = file.path
        http.resume = true
        http.maxRetries = 10
        http.other = file
        file.fileTransferContext = http

        http.listener = MesiboHttp.Listener { config, state, percent ->
            val f = config.other as MesiboFileTransfer
            if (100 == percent && MesiboHttp.STATE_DOWNLOAD == state) {
                f.setResult(true, null)
                return@Listener true
            }

            if (percent < 0) {
                f.setResult(false, null)
                return@Listener true
            }

            f.setProgress(percent)
            true
        }

        if (null != mQueue) mQueue.queue(http)
        else if (http.execute()) {
        }

        return true
    }

    override fun Mesibo_onStartFileTransfer(file: MesiboFileTransfer): Boolean {
        if (!file.upload) return Mesibo_onStartDownload(file)

        return Mesibo_onStartUpload(file)
    }

    override fun Mesibo_onStopFileTransfer(file: MesiboFileTransfer): Boolean {
        val http = file.fileTransferContext as MesiboHttp
        http?.cancel()

        return true
    }

    private var mDownloadCounter = 0
    private var mUploadCounter = 0
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
        private val mQueue = MesiboHttp.Queue(4, 0)
    }
}


