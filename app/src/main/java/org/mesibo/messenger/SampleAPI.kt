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

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Environment
import android.os.Handler

import android.text.TextUtils
import android.util.Base64
import android.util.Log

import com.google.gson.Gson
import com.mesibo.api.Mesibo
import com.mesibo.contactutils.ContactUtils
import com.mesibo.mediapicker.MediaPicker
import org.mesibo.messenger.fcm.MesiboRegistrationIntentService
import com.mesibo.calls.MesiboCall


import java.util.ArrayList


/**
 * Created for Mesibo Sample App
 */

object SampleAPI {
    private val TAG = "SampleAPI"
    private var mNotifyUser: NotifyUser? = null
    private var mSyncPending = true
    private var mContactSyncOver = false
    private var mContext: Context? = null
    private var mResetSyncedContacts = false
    private var mAkClientToken: String? = null
    private var mAkAppId: String? = null

    val KEY_SYNCEDCONTACTS = "syncedContacts"
    val KEY_SYNCEDDEVICECONTACTSTIME = "syncedPhoneContactTs"
    val KEY_SYNCEDCONTACTSTIME = "syncedTs"
    val KEY_AUTODOWNLOAD = "autodownload"
    val KEY_GCMTOKEN = "gcmtoken"

    val VISIBILITY_HIDE = 0
    val VISIBILITY_VISIBLE = 1
    val VISIBILITY_UNCHANGED = 2

    private val DEFAULT_FILE_URL = "https://media.mesibo.com/files/"

    private val mGson = Gson()
    private val mApiUrl = "https://app.mesibo.com/api.php"
    private var mContactTs: Long = 0

    //MUST not happen
    val phone: String?
        get() {

            if (!TextUtils.isEmpty(AppConfig.config.phone)) {
                return AppConfig.config.phone
            }


            val u = Mesibo.getSelfProfile()
            if (null == u) {
                forceLogout()
                return null
            }
            AppConfig.config.phone = u.address
            AppConfig.save()
            return AppConfig.config.phone
        }


    val token: String
        get() = AppConfig.config.token
    val uploadUrl: String
        get() = AppConfig.config.uploadurl?:""
    val downloadUrl: String
        get() = AppConfig.config.downloadurl?:""

    private var mPendingHiddenContacts: ArrayList<Mesibo.UserProfile>? = null

    private var mGCMToken: String? = null
    private var mGCMTokenSent = false

    private var mAutoDownload = true

    var mediaAutoDownload: Boolean
        get() = mAutoDownload
        set(autoDownload) {
            mAutoDownload = autoDownload
            Mesibo.setKey(KEY_AUTODOWNLOAD, if (mAutoDownload) "" else "0")
        }


    abstract class ResponseHandler : Mesibo.HttpListener {
        private var http: Mesibo.Http? = null
        private var mRequest: Bundle? = null
        private var mBlocking = false
        private var mOnUiThread = false
        public var result = true
        var context: Context? = null

        override fun Mesibo_onHttpProgress(http: Mesibo.Http, state: Int, percent: Int): Boolean {
            if (percent < 0) {
                HandleAPIResponse(null)
                return true
            }

            if (100 == percent && Mesibo.Http.STATE_DOWNLOAD == state) {
                val strResponse = http.dataString
                var response: Response? = null

                if (null != strResponse) {
                    try {
                        response = mGson.fromJson(strResponse, Response::class.java)
                    } catch (e: Exception) {
                        Log.d("MesiboKotlin", "Exception in HTTP response"+ e.toString())
                        this.result = false
                    }

                }

                if (null == response)
                    this.result = false

                val context = if (null == this.context) SampleAPI.mContext else this.context

                if (!mOnUiThread) {
                    parseResponse(response, mRequest, context, false)
                    HandleAPIResponse(response)
                } else {
                    val r = response

                    if (null == context)
                        return true

                    val uiHandler = Handler(context.mainLooper)

                    val myRunnable = Runnable {
                        parseResponse(r, mRequest, context, true)
                        HandleAPIResponse(r)
                    }
                    uiHandler.post(myRunnable)
                }
            }
            return true
        }

        fun setBlocking(blocking: Boolean) {
            mBlocking = blocking
        }

        fun setOnUiThread(onUiThread: Boolean) {
            mOnUiThread = onUiThread
        }

        fun sendRequest(postBunlde: Bundle, filePath: String?, formFieldName: String?): Boolean {

            postBunlde.putString("dt", Mesibo.getDeviceType().toString())
            val nwtype = Mesibo.getNetworkConnectivity()
            if (nwtype == 0xFF) {

            }

            mRequest = postBunlde
            http = Mesibo.Http()
            http!!.url = mApiUrl
            http!!.postBundle = postBunlde
            http!!.uploadFile = filePath
            http!!.uploadFileField = formFieldName
            http!!.notifyOnCompleteOnly = true
            http!!.concatData = true
            http!!.listener = this
            return if (mBlocking) http!!.executeAndWait() else http!!.execute()
        }

        abstract fun HandleAPIResponse(response: Response?)
//
//        companion object {
//            var result = true
//        }
    }

    interface ApiResponseHandler {
        fun onApiResponse(result: Boolean, respString: String?)
    }

    class Urls {
        var upload = ""
        var download = ""
    }

    class Invite {
        var text = ""
        var subject = ""
        var title = ""
    }

    class Contacts {
        var name = ""
        var phone = ""
        var gid: Long = 0
        var ts: Long = 0
        var status = ""
        var photo = ""
        var tn = ""
        var members = ""
    }

    class Response internal constructor() {
        var result: String? = null
        var op: String? = null
        var error: String? = null
        var token: String? = null
        var contacts: Array<Contacts>? = null
        var name: String? = null
        var status: String? = null
        var members: String? = null
        var photo: String? = null
        var phone: String? = null
        var cc: String? = null

        var urls: Urls? = null
        var share: Invite? = null

        var gid: Long = 0
        var type: Int = 0
        var profile: Int = 0
        var ts: Long = 0
        var tn: String? = null

        init {
            result = null
            op = null
            error = null
            token = null
            contacts = null
            gid = 0
            type = 0
            profile = 0
            urls = null
        }
    }

    private fun invokeApi(context: Context?, postBunlde: Bundle, filePath: String?, formFieldName: String?, uiThread: Boolean): Boolean {
        val http = object : ResponseHandler() {
            override fun HandleAPIResponse(response: Response?) {

            }
        }

        http.context = context
        http.setOnUiThread(uiThread)
        return http.sendRequest(postBunlde, filePath, formFieldName)
    }

    fun phoneBookLookup(phone: String?): String? {
        return if (TextUtils.isEmpty(phone)) null else ContactUtils.reverseLookup(phone)

    }

    fun updateDeletedGroup(gid: Long) {
        if (0L == gid) return
        val u = Mesibo.getUserProfile(gid) ?: return
        u.flag = u.flag or Mesibo.UserProfile.FLAG_DELETED.toLong()
        u.status = "Not a group member" // can be better handle dynamically
        Mesibo.setUserProfile(u, false)
    }

    fun createContact(name: String?, phone: String?, groupid: Long, status: String?, members: String?, photo: String?, tnBasee64: String?, ts: Long, `when`: Long, selfProfile: Boolean, refresh: Boolean, visibility: Int) {
        val u = Mesibo.UserProfile()
        u.address = phone
        u.groupid = groupid

        if (!selfProfile && 0L == u.groupid)
            u.name = phoneBookLookup(phone)

        if (TextUtils.isEmpty(u.name))
            u.name = name

        if (TextUtils.isEmpty(u.name)) {
            u.name = phone
            if (TextUtils.isEmpty(u.name))
                u.name = "Group-$groupid"
        }

        if (groupid == 0L && !TextUtils.isEmpty(phone) && phone!!.equals("0", ignoreCase = true)) {
            u.name = "hello"
            return
        }

        u.status = status // Base64.decode(c[i].status, Base64.DEFAULT).toString();

        if (groupid > 0) {
            u.groupMembers = members
            val p = phone ?: return
//if members empty or doesn't contain myself, it means I am not a member or group deleted
            if (!members!!.contains(phone)) {
                updateDeletedGroup(groupid)
                return
            }
            u.status = groupStatusFromMembers(members)
        }

        if (null == u.status) {
            u.status = ""
        }

        u.picturePath = photo
        u.timestamp = ts
        if (ts > 0 && u.timestamp > mContactTs)
            mContactTs = u.timestamp

        if (`when` >= 0) {
            u.lastActiveTime = Mesibo.getTimestamp() - `when` * 1000
        }

        if (!TextUtils.isEmpty(tnBasee64)) {
            var tn: ByteArray? = null
            try {
                tn = Base64.decode(tnBasee64, Base64.DEFAULT)

                if (Mesibo.createFile(Mesibo.getFilePath(Mesibo.FileInfo.TYPE_PROFILETHUMBNAIL), photo, tn, true)) {
                    //u.tnPath = photo;
                }
            } catch (e: Exception) {
            }

        }

        if (visibility == VISIBILITY_HIDE)
            u.flag = u.flag or Mesibo.UserProfile.FLAG_HIDDEN.toLong()
        else if (visibility == VISIBILITY_UNCHANGED) {
            val tp = Mesibo.getUserProfile(phone, groupid)
            if (null != tp && tp.flag and Mesibo.UserProfile.FLAG_HIDDEN.toLong() > 0)
                u.flag = u.flag or Mesibo.UserProfile.FLAG_HIDDEN.toLong()
        }

        if (selfProfile) {
            AppConfig.config.phone = u.address
            Mesibo.setSelfProfile(u)
        } else
            Mesibo.setUserProfile(u, refresh)
    }

    private fun parseResponse(response: Response?, request: Bundle?, context: Context?, uiThread: Boolean): Boolean {
        Log.d("MesiboKotlin", "response "+ response.toString()+" request: "+request.toString())
        if (null == response) {
            if (request!!.getString("op")!!.equals("getcontacts", ignoreCase = true)) {
                mSyncPending = true
            }

            if (uiThread && null != context) {
                showConnectionError(context)
            }
            return false
        }

        if (!response.result!!.equals("OK", ignoreCase = true)) {
            if (response.error!!.equals("AUTHFAIL", ignoreCase = true)) {
                forceLogout()
            }
            return false
        }

        var save = false
        if (null != response.urls) {
            AppConfig.config.uploadurl = response.urls!!.upload
            AppConfig.config.downloadurl = response.urls!!.download
            save = true
        }

        if (null != response.share) {
            AppConfig.config.invite = response.share
            save = true
        }

        if (response.op == "login" && !TextUtils.isEmpty(response.token)) {
            AppConfig.config.token = response.token?:"" //TBD, save into preference
            AppConfig.config.phone =  response.phone?:""
            AppConfig.config.cc = response.cc?:""
            mContactTs = 0
            mResetSyncedContacts = true
            mSyncPending = true

            save = true

            Mesibo.reset()
            startMesibo(true)

            createContact(response.name, response.phone, 0, response.status, "", response.photo, response.tn, response.ts, 0, true, false, VISIBILITY_VISIBLE)

            // we need to get permission
            startSync()
        } else if (response.op == "getcontacts") {
            val c = response.contacts

            val h = request!!.getString("hidden")
            var visibility = VISIBILITY_VISIBLE
            if (null != h && h.equals("1", ignoreCase = true)) {
                visibility = VISIBILITY_HIDE
            }

            if (null != c) {
                val count = c.size

                for (i in 0 until count) {
                    createContact(c[i].name, c[i].phone, c[i].gid, c[i].status, c[i].members, c[i].photo, c[i].tn, c[i].ts, response.ts - c[i].ts, false, true, visibility)
                }

                // update only if count > 0
                saveSyncedTimestamp(mContactTs)
            }

            mResetSyncedContacts = false
            if (TextUtils.isEmpty(request.getString("phones"))) {

                // update table with group messages if any
                if (null != c && c.size > 0) {
                    if (uiThread)
                        Mesibo.setUserProfile(null, true)
                    else {

                        val uiHandler = Handler(mContext!!.mainLooper)

                        val myRunnable = Runnable { Mesibo.setUserProfile(null, true) }
                        uiHandler.post(myRunnable)
                    }
                }

                if (VISIBILITY_VISIBLE == visibility)
                    startContactsSync()
            }
        } else if (response.op == "getgroup" || response.op == "setgroup") {
            if (!TextUtils.isEmpty(response.phone) || response.gid > 0) {
                createContact(response.name, "", response.gid, response.status, response.members, response.photo, response.tn, response.ts, 0, false, true, VISIBILITY_VISIBLE)
            }
        } else if (response.op == "editmembers" || response.op == "setadmin") {
            var u: Mesibo.UserProfile? = null
            if (response.gid > 0)
                u = Mesibo.getUserProfile(response.gid)
            if (null != u) {
                u.groupMembers = response.members
                u.status = groupStatusFromMembers(response.members)
                Mesibo.setUserProfile(u, false)
            }
        } else if (response.op == "delgroup") {
            updateDeletedGroup(response.gid)
        } else if (response.op == "upload") {
            if (response.profile > 0)
                createContact(response.name, response.phone, response.gid, response.status, response.members, response.photo, response.tn, response.ts, 0, true, true, VISIBILITY_VISIBLE)
        } else if (response.op == "logout") {
            forceLogout()
            AppConfig.reset()
        }

        if (save)
            AppConfig.save()

        return true
    }


    fun showConnectionError(context: Context) {
        val title = "No Internet Connection"
        val message = "Your phone is not connected to the internet. Please check your internet connection and try again later."
        UIManager.showAlert(context, title, message)
    }

    fun groupStatusFromMembers(members: String?): String? {
        if (TextUtils.isEmpty(members))
            return null

        val s = members!!.split("\\:".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        if (null == s || s.size < 2)
            return null

        val users = s[1].split("\\,".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                ?: return ""

        var status = ""
        for (i in users.indices) {
            if (!TextUtils.isEmpty(status))
                status += ", "

            if (phone!!.equals(users[i], ignoreCase = true)) {
                status += "You"
            } else {
                val u = Mesibo.getUserProfile(users[i], 0)

                //TBD, use only the first name
                if (u != null)
                    status += u.name
                else
                    status += users[i]
            }

            if (status.length > 32)
                break
        }
        return status
    }

    fun getGroupMembers(members: String): ArrayList<Mesibo.UserProfile>? {
        if (TextUtils.isEmpty(members))
            return null

        val s = members.split("\\:".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        if (null == s || s.size < 2)
            return null

        val users = s[1].split("\\,".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                ?: return null

        val profiles = ArrayList<Mesibo.UserProfile>()

        val status = ""
        for (i in users.indices) {

            //TBD, check about self profile
            var u: Mesibo.UserProfile? = Mesibo.getUserProfile(users[i], 0)
            if (null == u) {
                u = Mesibo.createUserProfile(users[i], 0, users[i])
            }

            profiles.add(u!!)
        }
        return profiles
    }


    fun saveLocalSyncedContacts(contacts: String, timestamp: Long) {
        Mesibo.setKey(SampleAPI.KEY_SYNCEDCONTACTS, contacts)
        Mesibo.setKey(SampleAPI.KEY_SYNCEDDEVICECONTACTSTIME, timestamp.toString())
    }

    fun saveSyncedTimestamp(timestamp: Long) {
        Mesibo.setKey(SampleAPI.KEY_SYNCEDCONTACTSTIME, timestamp.toString())
    }

    fun init(context: Context) {
        mContext = context

        val api = Mesibo.getInstance()
        api.init(context)

        Mesibo.initCrashHandler(MesiboListeners.instance)
        Mesibo.uploadCrashLogs()
        Mesibo.setSecureConnection(true)


        var ai: ApplicationInfo? = null
        try {
            ai = context.packageManager.getApplicationInfo(context.packageName, PackageManager.GET_META_DATA)
            mAkClientToken = ai!!.metaData.getString("com.facebook.accountkit.ClientToken")
            mAkAppId = ai.metaData.getString("com.facebook.sdk.ApplicationId")
        } catch (e: Exception) {
        }


        val token = AppConfig.config.token
        if (!TextUtils.isEmpty(token)) {
            startMesibo(false)
            startSync()
        }
    }

    fun startOnlineAction() {
        sendGCMToken()
        startSync()
    }


    private fun startSync() {

        synchronized(SampleAPI::class.java) {
            if (!mSyncPending)
                return
            mSyncPending = false
        }

        SampleAPI.getContacts(null, false, true)
    }

    // this is called to indicate first round of sync is done
    //TBD, this may trigger getcontact with hidden=1 to reach server before last contact sysc getconatct request
    fun syncDone() {
        synchronized(SampleAPI::class.java) {
            mContactSyncOver = true
        }

        autoAddContact(null)
    }

    fun startContactsSync() {
        val synced = Mesibo.readKey(KEY_SYNCEDCONTACTS)
        val syncedts = Mesibo.readKey(KEY_SYNCEDDEVICECONTACTSTIME)
        var ts: Long = 0
        if (!TextUtils.isEmpty(syncedts)) {
            try {
                ts = java.lang.Long.parseLong(syncedts)
            } catch (e: Exception) {
            }

        }
        ContactUtils.sync(synced, ts, false, MesiboListeners.instance)
    }

    fun startMesibo(resetContacts: Boolean): Boolean {

        //        MesiboRegistrationIntentService.startRegistration(mContext, "946969055788", MesiboListeners.getInstance());
        MesiboRegistrationIntentService.startRegistration(mContext!!, "470359684336", MesiboListeners.instance)

        // set path for storing DB and messaging files
        Mesibo.setPath(Environment.getExternalStorageDirectory().absolutePath)

        val path = Mesibo.getBasePath()
        MediaPicker.setPath(path)

        // add lister
        Mesibo.addListener(MesiboListeners.instance)
        MesiboCall.getInstance().setListener(MesiboListeners.instance)

        // add file transfer handler
        val fileTransferHelper = MesiboFileTransferHelper()
        Mesibo.addListener(fileTransferHelper)

        // this will also register listener from the constructor
        mNotifyUser = NotifyUser(MainApplication.appContext!!)

        // set access token
        if (0 != Mesibo.setAccessToken(AppConfig.config.token)) {
            return false
        }

        //Mesibo.sendTv(null, 0, 0, 0, null);

        // set database after setting access token so that it's associated with user
        Mesibo.setDatabase("mesibo.db", if (resetContacts) Mesibo.DBTABLE_PROFILES else 0)

        // do this after setting token and db
        if (resetContacts) {
            SampleAPI.saveLocalSyncedContacts("", 0)
            SampleAPI.saveSyncedTimestamp(0)
        }

        initAutoDownload()

        // Now start mesibo
        if (0 != Mesibo.start()) {
            return false
        }

        val ts = Mesibo.readKey(KEY_SYNCEDCONTACTSTIME)
        if (!TextUtils.isEmpty(ts))
            mContactTs = java.lang.Long.parseLong(ts)

        ContactUtils.init(mContext)
        ContactUtils.setCountryCode(AppConfig.config.cc)

        if (resetContacts)
            ContactUtils.syncReset()

        val restartIntent = Intent(MainApplication.restartIntent)

        Mesibo.runInBackground(MainApplication.appContext!!, null, restartIntent)

        return true
    }

    private fun createPostBundle(op: String): Bundle? {
        if (TextUtils.isEmpty(AppConfig.config.token))
            return null

        val b = Bundle()
        b.putString("op", op)
        b.putString("token", AppConfig.config.token)
        return b
    }

    fun startLogout(): Boolean {
        val b = createPostBundle("logout") ?: return false
        invokeApi(null, b, null, null, false)
        return true
    }


    fun forceLogout() {
        Log.d("MesiboKotlin", "===Force Logout===")
        mGCMTokenSent = false
        Mesibo.setKey(KEY_GCMTOKEN, "")
        SampleAPI.saveLocalSyncedContacts("", 0)
        SampleAPI.saveSyncedTimestamp(0)
        Mesibo.stop(true)
        AppConfig.config.reset()
        mNotifyUser!!.clearNotification()
        Mesibo.reset()
        //Mesibo.resetDB();
        ContactUtils.syncReset()

        UIManager.launchStartupActivity(mContext!!, true)
    }

    fun login(phoneNumber: String, verificationCode: String, handler: ResponseHandler) {
        //Mesibo.resetDB();

        val b = Bundle()
        b.putString("op", "login")
        b.putString("appid", mContext!!.packageName)
        b.putString("phone", phoneNumber)
        if (!TextUtils.isEmpty(verificationCode))
            b.putString("code", verificationCode)

        handler.setOnUiThread(true)
        handler.sendRequest(b, null, null)
    }

    fun loginAccountKit(accessToken: String, handler: ResponseHandler) {
        //Mesibo.resetDB();

        val b = Bundle()
        b.putString("op", "login")
        b.putString("appid", mContext!!.packageName)
        b.putString("aktoken", accessToken)
        b.putString("akct", mAkClientToken)
        b.putString("akaid", mAkAppId)
        handler.setOnUiThread(true)
        handler.sendRequest(b, null, null)
    }

    fun setProfile(name: String, status: String, groupid: Long, handler: ResponseHandler): Boolean {
        val b = createPostBundle("profile") ?: return false

        b.putString("name", name)
        b.putString("status", status)
        b.putLong("gid", groupid)

        handler.setOnUiThread(true)
        handler.sendRequest(b, null, null)

        return true
    }

    fun setProfilePicture(filePath: String, groupid: Long, handler: ResponseHandler): Boolean {
        val b = createPostBundle("upload") ?: return false
        b.putLong("mid", 0)
        b.putInt("profile", 1)
        b.putLong("gid", groupid)

        handler.setOnUiThread(true)

        if (TextUtils.isEmpty(filePath)) {
            b.putInt("delete", 1)
            handler.sendRequest(b, null, null)
            return true
        }

        handler.sendRequest(b, filePath, "photo")
        return true
    }

    fun getContacts(contacts: ArrayList<String>?, hidden: Boolean, async: Boolean): Boolean {
        if (hidden && (null == contacts || contacts.size == 0))
            return false

        val b = createPostBundle("getcontacts") ?: return false

        b.putString("hidden", if (hidden) "1" else "0")

        if (!hidden && mResetSyncedContacts) {
            mContactTs = 0
            mResetSyncedContacts = false // we are doing it here because if old messages are stored,
            // it will start querying with hidden flag before response and then every request
            // will have reset
            b.putString("reset", "1")
        }

        b.putLong("ts", mContactTs)
        if (null != contacts && contacts.size > 0) {
            val c = contacts.toTypedArray()
            b.putString("phones", array2String(c))
            //b.putStringArray("phones", c);
        }


        val http = object : ResponseHandler() {
            override fun HandleAPIResponse(response: Response?) {}
        }

        http.setBlocking(!async)
        http.sendRequest(b, null, null)
        return http.result
    }

    fun deleteContacts(contacts: ArrayList<String>?): Boolean {
        if (null == contacts || 0 == contacts.size)
            return false

        val b = createPostBundle("delcontacts") ?: return false

        //if((System.currentTimeMillis() - mContactFetchTs) < 5000)
        //  return true;

        val c = contacts.toTypedArray()
        b.putString("phones", array2String(c))

        val http = object : ResponseHandler() {
            override fun HandleAPIResponse(response: Response?) {}
        }

        http.setBlocking(true)
        http.sendRequest(b, null, null)
        return http.result
    }

    // groupid is 0 for new group else pass actual value to add/remove members
    fun setGroup(groupid: Long, name: String, status: String, photoPath: String, members: Array<String>?, handler: ResponseHandler): Boolean {
        val b = createPostBundle("setgroup") ?: return false

        b.putString("name", name)
        b.putLong("gid", groupid)
        b.putString("status", status)
        if (null != members)
            b.putString("m", array2String(members))

        handler.setOnUiThread(true)
        handler.sendRequest(b, photoPath, "photo")
        return true
    }

    fun deleteGroup(groupid: Long, handler: ResponseHandler): Boolean {
        val b = createPostBundle("delgroup") ?: return false

        b.putLong("gid", groupid)
        handler.setOnUiThread(true)
        handler.sendRequest(b, null, null)

        return true
    }

    fun getGroup(groupid: Long, handler: ResponseHandler): Boolean {
        if (0L == groupid)
            return false

        val b = createPostBundle("getgroup") ?: return false

        b.putLong("gid", groupid)
        handler.setOnUiThread(true)
        handler.sendRequest(b, null, null)
        return true
    }

    fun editMembers(groupid: Long, members: Array<String>, remove: Boolean, handler: ResponseHandler): Boolean {
        if (0L == groupid || null == members)
            return false

        val b = createPostBundle("editmembers") ?: return false

        b.putLong("gid", groupid)
        b.putString("m", array2String(members))
        b.putInt("delete", if (remove) 1 else 0)

        handler.setOnUiThread(true)
        handler.sendRequest(b, null, null)
        return true
    }

    fun setAdmin(groupid: Long, member: String, admin: Boolean, handler: ResponseHandler): Boolean {
        if (0L == groupid || TextUtils.isEmpty(member))
            return false

        val b = createPostBundle("setadmin") ?: return false

        b.putLong("gid", groupid)
        b.putString("m", member)
        b.putInt("admin", if (admin) 1 else 0)

        handler.setOnUiThread(true)
        handler.sendRequest(b, null, null)
        return true
    }

    fun array2String(a: Array<String>): String {
        var str = ""
        for (i in a.indices) {
            if (i > 0)
                str += ","
            str += a[i]
        }

        return str
    }


    fun getLinkPreview(q: String, handler: ApiResponseHandler) {

        val http = Mesibo.Http()

        http.url = "http://api.linkpreview.net/?key=5d04a893457c8c32e57398a4a3d95cb29ce12ae30e18f&q=$q"

        http.onMainThread = true

        http.listener = Mesibo.HttpListener { config, state, percent ->
            if (100 == percent && Mesibo.Http.STATE_DOWNLOAD == state) {

                //parse response
                val respString = config.dataString



                if (null == respString || respString.equals("", ignoreCase = true)) {
                    handler.onApiResponse(false, null)
                    return@HttpListener true
                }

                handler.onApiResponse(true, respString)
                return@HttpListener true
            }

            if (percent < 0)
                handler.onApiResponse(false, null)

            true
        }

        http.execute()
    }


    fun notify(channelid: String, id: Int, title: String, message: String) {
        mNotifyUser!!.sendNotification(channelid, id, title, message)
    }

    fun notify(params: Mesibo.MessageParams, message: String) {
        var message = message
        // if call is in progress, we must give notification even if reading because user is in call
        // screen
        if (!MesiboCall.getInstance().isCallInProgress && Mesibo.isReading(params))
            return

        if (Mesibo.ORIGIN_REALTIME != params.origin || Mesibo.MSGSTATUS_OUTBOX == params.status)
            return

        //MUST not happen for realtime message
        if (params.groupid > 0 && null == params.groupProfile)
            return

        val profile = Mesibo.getUserProfile(params)

        // this will also mute message from user in group
        if (null != profile && profile.isMuted)
            return

        var name = params.peer
        if (null != profile) {
            name = profile.name
        }

        if (params.groupid > 0) {
            val gp = Mesibo.getUserProfile(params.groupid) ?: return
// must not happen

            if (gp.isMuted)
                return

            name += " @ " + gp.name
        }

        if (params.isMissedCall) {
            val subject = "Mesibo Missed Call"
            message = "You missed a mesibo " + (if (params.isVideoCall) "video " else "") + "call from " + profile!!.name
            SampleAPI.notify(NotifyUser.NOTIFYCALL_CHANNEL_ID, 2, subject, message)
            return
        }

        // outgoing or incoming call
        if (params.isCall) return

        mNotifyUser!!.sendNotificationInList(name, message)
    }

    fun addContacts(profiles: ArrayList<Mesibo.UserProfile>, hidden: Boolean) {

        val c = ArrayList<String>()

        for (i in profiles.indices) {
            val profile = profiles[i]

            if (profile.flag and Mesibo.UserProfile.FLAG_TEMPORARY.toLong() > 0 && profile.flag and Mesibo.UserProfile.FLAG_PROFILEREQUESTED.toLong() == 0L && null != profile.address) {
                profile.flag = profile.flag or Mesibo.UserProfile.FLAG_PROFILEREQUESTED.toLong()
                c.add(profile.address)
            }
        }

        if (c.size == 0)
            return

        getContacts(c, hidden, true)
    }

    @Synchronized
    fun autoAddContact(params: Mesibo.MessageParams?) {
        if (null == params) {
            if (null != mPendingHiddenContacts) {
                addContacts(mPendingHiddenContacts!!, true)
                mPendingHiddenContacts = null
            }
            return
        }

        // the logic is if user replies, we will see contact details, else not */
        if (/*Mesibo.ORIGIN_REALTIME != params.origin || */Mesibo.MSGSTATUS_OUTBOX == params.status)
            return

        if (params.profile.flag and Mesibo.UserProfile.FLAG_TEMPORARY.toLong() == 0L || params.profile.flag and Mesibo.UserProfile.FLAG_PROFILEREQUESTED.toLong() > 0)
            return

        if (null == mPendingHiddenContacts)
            mPendingHiddenContacts = ArrayList()

        mPendingHiddenContacts!!.add(params.profile)

        if (!mContactSyncOver) {
            return
        }

        addContacts(mPendingHiddenContacts!!, true)
        mPendingHiddenContacts = null
    }

    fun setGCMToken(token: String) {
        mGCMToken = token
        sendGCMToken()
    }

    private fun sendGCMToken() {
        if (null == mGCMToken || mGCMTokenSent) {
            return
        }

        Mesibo.setPushToken(mGCMToken)

        synchronized(SampleAPI::class.java) {
            if (mGCMTokenSent)
                return
            mGCMTokenSent = true
        }

        val gcmtoken = Mesibo.readKey(KEY_GCMTOKEN)
        if (!TextUtils.isEmpty(gcmtoken) && gcmtoken.equals(mGCMToken!!, ignoreCase = true)) {
            mGCMTokenSent = true
            return
        }


        val b = createPostBundle("setnotify") ?: return

        b.putString("notifytoken", mGCMToken)
        b.putInt("fcm", 1)

        val http = object : ResponseHandler() {
            override fun HandleAPIResponse(response: Response?) {
                if (null != response && response.result!!.equals("OK", ignoreCase = true)) {
                    Mesibo.setKey(KEY_GCMTOKEN, mGCMToken)
                } else
                    mGCMTokenSent = false
            }
        }

        http.sendRequest(b, null, null)
    }

    /* if it is called from service, it's okay to block, we should wait till
       we are online. As soon as we return, service will be destroyed
     */
    fun onGCMMessage(inService: Boolean) {
        Mesibo.setAppInForeground(null, -1, true)

        while (inService) {
            if (Mesibo.STATUS_ONLINE == Mesibo.getConnectionStatus())
                break

            try {
                Thread.sleep(1000)
            } catch (e: InterruptedException) {
            }

        }

        // wait for messages to receive etc
        try {
            Thread.sleep(1000)
        } catch (e: InterruptedException) {
        }

    }

    fun notifyClear() {
        mNotifyUser!!.clearNotification()
    }

    private fun initAutoDownload() {
        val autodownload = Mesibo.readKey(KEY_AUTODOWNLOAD)
        mAutoDownload = TextUtils.isEmpty(autodownload)
    }


}
