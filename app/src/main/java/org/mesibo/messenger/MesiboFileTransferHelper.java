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

package org.mesibo.messenger;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import com.mesibo.api.Mesibo;
import com.mesibo.api.MesiboFileTransfer;
import com.mesibo.api.MesiboHttp;

import org.json.JSONObject;
import java.util.LinkedHashMap;
import java.util.Map;

// Refer to http://mesibo.com/documentation/get-started/file-transfer/#how-to-send-and-receive-files
public class MesiboFileTransferHelper implements Mesibo.FileTransferHandler {

    private static MesiboHttp.Queue mQueue = new MesiboHttp.Queue(4, 0);
    public static class MesiboUrl {
        public String op;
        public String file;
        public String result;
        public String xxx;

        MesiboUrl() {
            result = null;
            op = null;
            xxx = null;
            file = null;
        }
    }

    MesiboFileTransferHelper() {
        	//Mesibo.addListener(this);
    }
    
    public boolean Mesibo_onStartUpload(final MesiboFileTransfer file) {

        // we don't need to check origin the way we do in download
        if(mUploadCounter >= 5 && file.priority == 0)
            return false;

        final long mid = file.mid;

        Bundle b = new Bundle();
        b.putString("op", "upload");
        b.putString("token", SampleAPI.getToken());
        b.putLong("mid", mid);
        b.putInt("profile", 0);

        updateUploadCounter(1);
        MesiboHttp http = new MesiboHttp();

        http.url = SampleAPI.getUploadUrl();
        http.postBundle = b;
        http.uploadFile = file.getPath();
        http.uploadFileField = "photo";
        http.other = file;
        file.setFileTransferContext(http);

        http.listener = new MesiboHttp.Listener() {
            @Override
            public boolean Mesibo_onHttpProgress(MesiboHttp config, int state, int percent) {
                MesiboFileTransfer f = (MesiboFileTransfer)config.other;

                if(100 == percent && MesiboHttp.STATE_DOWNLOAD == state) {
                    String response = config.getDataString();
                    String url = null;
                    boolean result = false;

                    try {
                        JSONObject jo = new JSONObject(response);
                        if(null != jo) {
                            url = jo.getString("url");
                            result = jo.getBoolean("result");
                        }

                    } catch (Exception e) {}

                    f.setResult(result, url);
                    return true;
                }

                if(percent < 0) {
                    f.setResult(false, null);
                    return true;
                }

                f.setProgress(percent);
                return true;
            }
        };

        if(null != mQueue)
            mQueue.queue(http);

        else if(http.execute()) {

        }

        return true;

    }

    public boolean Mesibo_onStartDownload(final MesiboFileTransfer file) {

        //TBD, check file type and size to decide automatic download
        if(!SampleAPI.getMediaAutoDownload() && Mesibo.getNetworkConnectivity() != Mesibo.CONNECTIVITY_WIFI && 0 == file.priority)
            return false;

        // only realtime messages to be downloaded in automatic mode.
        if(Mesibo.ORIGIN_REALTIME != file.origin && Mesibo.ORIGIN_DBPENDING != file.origin && file.priority == 0)
            return false;


        updateDownloadCounter(1);

        final long mid = file.mid;

        String url = file.getUrl();
        if(TextUtils.isEmpty(url))
            return false;

        if(!url.toLowerCase().startsWith("http://") && !url.toLowerCase().startsWith("https://")) {
            url = SampleAPI.getDownloadUrl() + url;
        }

        MesiboHttp http = new MesiboHttp();

        http.url = url;
        http.downloadFile = file.getPath();
        http.resume = true;
        http.maxRetries = 10;
        http.other = file;
        file.setFileTransferContext(http);

        http.listener = new MesiboHttp.Listener() {
            @Override
            public boolean Mesibo_onHttpProgress(MesiboHttp config, int state, int percent) {
                MesiboFileTransfer f = (MesiboFileTransfer)config.other;

                if(100 == percent && MesiboHttp.STATE_DOWNLOAD == state) {
                    f.setResult(true, null);
                    return true;
                }

                if(percent < 0) {
                    f.setResult(false, null);
                    return true;
                }

                f.setProgress(percent);
                return true;
            }
        };

        if(null != mQueue)
            mQueue.queue(http);
        else if(http.execute()) {

        }

        return true;
    }

    @Override
    public boolean Mesibo_onStartFileTransfer(MesiboFileTransfer file) {
        if(!file.upload)
            return Mesibo_onStartDownload(file);

        return Mesibo_onStartUpload(file);
    }

    @Override
    public boolean Mesibo_onStopFileTransfer(MesiboFileTransfer file) {
        MesiboHttp http = (MesiboHttp) file.getFileTransferContext();
        if(null != http)
            http.cancel();

        return true;
    }

    private int mDownloadCounter = 0, mUploadCounter = 0;
    public synchronized int updateDownloadCounter(int increment) {
        mDownloadCounter += increment;
        return mDownloadCounter;
    }

    public synchronized int updateUploadCounter(int increment) {
        mUploadCounter += increment;
        return mUploadCounter;
    }
}


