package org.mesibo.messenger

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.text.TextUtils
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.mesibo.api.Mesibo
import com.mesibo.api.MesiboHttp
import com.mesibo.api.MesiboLocationConfig
import com.mesibo.api.MesiboMessage
import com.mesibo.api.MesiboProfile
import com.mesibo.calls.api.MesiboCall
import com.mesibo.messaging.MesiboDialogs
import com.mesibo.messaging.MesiboUI
import org.json.JSONObject
import org.mesibo.messenger.fcm.MesiboPushHelper
import java.nio.charset.StandardCharsets

object SampleAPI {
    private const val TAG = "SampleAPI"
    private var mNotifyUser: NotifyUser? = null
    private var mSyncPending = true
    // the app context, set in init()
    private lateinit var mContext: Context
    private var mResetSyncedContacts = false

    const val KEY_SYNCEDCONTACTS: String = "AppSyncedContacts"
    const val KEY_SYNCEDDEVICECONTACTSTIME: String = "AppSyncedPhoneContactTs"
    const val KEY_SYNCEDCONTACTSTIME: String = "AppSyncedTsNew"
    const val KEY_AUTODOWNLOAD: String = "autodownload"
    const val KEY_NOTIFY_MESSAGES: String = "notifymessages"
    const val KEY_NOTIFY_GROUPS: String = "notifygroups"

    private val mGson = Gson()
    private const val mApiUrl = "https://messenger.mesibo.com"

    private fun invokeApi(
        context: Context?,
        postBunlde: JSONObject,
        filePath: String?,
        formFieldName: String?,
        uiThread: Boolean
    ): Boolean {
        val http: ResponseHandler = object : ResponseHandler() {
            override fun HandleAPIResponse(response: Response?) {
            }
        }

        http.context = context
        http.setOnUiThread(uiThread)
        return http.sendRequest(postBunlde, filePath, formFieldName)
    }

    private fun parseResponse(response: Response?, context: Context?, uiThread: Boolean): Boolean {
        if (response?.result == null) {
            if (uiThread && null != context) {
                showConnectionError(context)
            }
            return false
        }

        if (!response.result.equals("OK", ignoreCase = true)) {
            if (null != response.error && response.error.equals("AUTHFAIL", ignoreCase = true)) {
                forceLogout()
                return false
            }

            if (null != response.errmsg) {
                if (null == response.errtitle) response.errtitle = "Failed"
                MesiboDialogs.alert(context, response.errtitle, response.errmsg)
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

        if (!TextUtils.isEmpty(response.message)) {
            MesiboDialogs.alert(context, response.title, response.message)
        }

        if ("login" == response.op && !TextUtils.isEmpty(response.token)) {
            AppConfig.config.token = response.token ?: "" //TBD, save into preference
            AppConfig.config.phone = response.phone ?: ""
            mResetSyncedContacts = true
            mSyncPending = true

            save = true

            Mesibo.reset()
            if (startMesibo(true)) {
                // need permission
                startSync()
            }
        } else if ("logout" == response.op) {
            forceLogout()
            AppConfig.reset()
        }

        if (save) AppConfig.save()
        return true
    }

    fun showConnectionError(context: Context?) {
        val title = "No Internet Connection"
        val message =
            "Your phone is not connected to the internet. Please check your internet connection and try again later."
        MesiboDialogs.alert(context, title, message)
    }

    fun saveLocalSyncedContacts(contacts: String?, timestamp: Long) {
        Mesibo.setKey(KEY_SYNCEDCONTACTS, contacts)
        Mesibo.setKey(KEY_SYNCEDDEVICECONTACTSTIME, timestamp.toString())
    }

    fun saveSyncedTimestamp(timestamp: Long) {
        Mesibo.setKey(KEY_SYNCEDCONTACTSTIME, timestamp.toString())
    }

    fun init(context: Context) {
        mContext = context

        val api = Mesibo.getInstance()
        api.init(context)

        Mesibo.initCrashHandler(MesiboListeners.instance)
        Mesibo.uploadCrashLogs()

        if (!TextUtils.isEmpty(AppConfig.config.token)) {
            if (startMesibo(false)) startSync()
        }
    }

    val phone: String?
        get() {
            if (!TextUtils.isEmpty(AppConfig.config.phone)) {
                return AppConfig.config.phone
            }


            val u: MesiboProfile? = Mesibo.getSelfProfile()

            //MUST not happen
            if (null == u) {
                forceLogout()
                return null
            }
            AppConfig.config.phone = u.address
            AppConfig.save()
            return AppConfig.config.phone
        }


    val token: String?
        get() = AppConfig.config.token
    val uploadUrl: String?
        get() = AppConfig.config.uploadurl
    val downloadUrl: String?
        get() = AppConfig.config.downloadurl

    fun startOnlineAction() {
        MesiboPushHelper.sendToken()
        startSync()
    }


    private fun startSync() {
        synchronized(SampleAPI::class.java) {
            if (!mSyncPending) return
            mSyncPending = false
        }

        startContactsSync()
    }

    fun startContactsSync() {
        val contacts = Mesibo.getPhoneContactsManager()
        contacts.overrideProfileName(true)
        contacts.start()
    }

    fun startMesibo(resetContacts: Boolean): Boolean {
        // sender id comes from google-services.json

        MesiboPushHelper.start()

        val path = Mesibo.getBasePath()
        MesiboUI.setMediaPath(path)

        // add lister
        Mesibo.addListener(MesiboListeners.instance)
        MesiboUI.setListener(UIListener())
        CallListeners.register()

        // add file transfer handler
        val fileTransferHelper = MesiboFileTransferHelper()

        //Mesibo.addListener(fileTransferHelper);

        // this will also register listener from the constructor
        if (null == mNotifyUser) mNotifyUser = NotifyUser(MainApplication.appContext)

        // set access token
        if (0 != Mesibo.setAccessToken(AppConfig.config.token)) {
            AppConfig.config.token = ""
            AppConfig.save()
            return false
        }

        // set database after setting access token so that it's associated with user
        Mesibo.setDatabase("mesibo.db")

        // do this after setting token and db
        if (resetContacts) {
            saveLocalSyncedContacts("", 0)
            saveSyncedTimestamp(0)
        }

        initAutoDownload()
        Mesibo.enableGalleryScanForMedia(true)
        // Now start mesibo
        if (0 != Mesibo.start()) {
            return false
        }

        val lc = MesiboLocationConfig()
        lc.backgroundRefresh = false

        Mesibo.getLocationManager().start(lc)

        val contactsManager = Mesibo.getPhoneContactsManager()
        contactsManager.overrideProfileName(true)

        if (resetContacts) {
            Mesibo.getPhoneContactsManager().reset()
        }

        // explicit package: implicit broadcasts are not delivered to manifest receivers (API 26+)
        val restartIntent: Intent = Intent(MainApplication.restartIntent)
        restartIntent.setPackage(MainApplication.appContext.getPackageName())

        Mesibo.runInBackground(MainApplication.appContext, null, restartIntent)

        return true
    }

    fun forceLogout() {
        MesiboPushHelper.reset()
        saveLocalSyncedContacts("", 0)
        saveSyncedTimestamp(0)
        Mesibo.stop(true)
        Mesibo.getPhoneContactsManager().reset()
        AppConfig.config.reset()
        notifyClear()
        Mesibo.setAccessToken(null)

        UIManager.launchStartupActivity(mContext, true)
    }

    fun startLogout(): Boolean {
        if (TextUtils.isEmpty(AppConfig.config.token)) return false
        val b = JSONObject()
        try {
            b.put("op", "logout")
            b.put("token", AppConfig.config.token)
        } catch (e: Exception) {
        }
        invokeApi(null, b, null, null, false)
        return true
    }

    fun login(phoneNumber: String?, verificationCode: String?, handler: ResponseHandler) {
        val b = JSONObject()
        try {
            b.put("op", "login")
            b.put("appid", mContext.packageName)
            b.put("phone", phoneNumber)
            if (!TextUtils.isEmpty(verificationCode)) {
                b.put("otp", verificationCode)
                if (!TextUtils.isEmpty(AppConfig.config.uniqueid)) b.put(
                    "device",
                    AppConfig.config.uniqueid
                )
            }
        } catch (e: Exception) {
        }

        handler.setOnUiThread(true)
        handler.sendRequest(b, null, null)
    }

    fun notify(id: Int, title: String?, message: String?) {
        if (null != mNotifyUser) mNotifyUser!!.sendNotification(id, title, message)
    }

    fun notify(params: MesiboMessage, message: String?) {
        // if call is in progress, we must give notification even if reading because user is in call
        // screen
        if (!MesiboCall.getInstance().isCallInProgress && Mesibo.isReading(params)) return

        // TBD, create read session for unread messages in database
        if (!params.isRealtimeMessage || params.isInOutbox || params.isOutgoing) return

        //MUST not happen for realtime message
        if (params.groupid > 0 && null == params.groupProfile) return

        // calls (incl. missed calls) are notified from MesiboCall_onNotify
        if (params.isCall || params.isMissedCall) return

        if (if (params.groupid > 0) !groupMessageNotification else !messageNotification) return

        val profile = Mesibo.getProfile(params)

        // this will also mute message from user in group
        if (null != profile && profile.isMuted) return

        var name = params.peer
        if (null != profile) {
            name = profile.name
        }

        if (params.groupid > 0) {
            val gp = Mesibo.getProfile(params.groupid) ?: return
            // must not happen


            if (gp.isMuted) return

            name += " @ " + gp.name
        }

        if (null != mNotifyUser) mNotifyUser!!.sendNotificationInList(name, message)
    }

    fun notifyClear() {
        if (null != mNotifyUser) mNotifyUser!!.clearNotification()
    }

    private var mAutoDownload = true

    private fun initAutoDownload() {
        val autodownload = Mesibo.readKey(KEY_AUTODOWNLOAD)
        mAutoDownload = (TextUtils.isEmpty(autodownload))
    }

    var mediaAutoDownload: Boolean
        get() = mAutoDownload
        set(autoDownload) {
            mAutoDownload = autoDownload
            Mesibo.setKey(KEY_AUTODOWNLOAD, if (mAutoDownload) "" else "0")
        }

    // notification settings: empty (default) = enabled, "0" = disabled
    private fun readFlag(key: String): Boolean {
        return "0" != Mesibo.readKey(key)
    }

    var messageNotification: Boolean
        get() = readFlag(KEY_NOTIFY_MESSAGES)
        set(enable) {
            Mesibo.setKey(KEY_NOTIFY_MESSAGES, if (enable) "" else "0")
        }
    var groupMessageNotification: Boolean
        get() = readFlag(KEY_NOTIFY_GROUPS)
        set(enable) {
            Mesibo.setKey(KEY_NOTIFY_GROUPS, if (enable) "" else "0")
        }


    abstract class ResponseHandler : MesiboHttp.Listener {
        private var http: MesiboHttp? = null
        private val mRequest: Bundle? = null
        private val mBlocking = false
        private var mOnUiThread = false
        var context: Context? = null

        override fun Mesibo_onHttpProgress(http: MesiboHttp, state: Int, percent: Int): Boolean {
            if (percent < 0) {
                HandleAPIResponse(null)
                return true
            }

            if (100 == percent && MesiboHttp.STATE_DOWNLOAD == state) {
                val strResponse = http.dataString
                var response: Response? = null

                if (null != strResponse) {
                    try {
                        response = mGson.fromJson(strResponse, Response::class.java)
                    } catch (e: Exception) {
                        result = false
                    }
                }

                if (null == response) result = false

                val context = if ((null == this.context)) mContext else this.context

                if (!mOnUiThread) {
                    parseResponse(response, context, false)
                    HandleAPIResponse(response)
                } else {
                    val r = response

                    if (null == context) return true

                    val uiHandler = Handler(context.mainLooper)

                    val myRunnable = Runnable {
                        parseResponse(r, context, true)
                        HandleAPIResponse(r)
                    }
                    uiHandler.post(myRunnable)
                }
            }
            return true
        }

        fun setOnUiThread(onUiThread: Boolean) {
            mOnUiThread = onUiThread
        }

        fun sendRequest(j: JSONObject, filePath: String?, formFieldName: String?): Boolean {
            try {
                j.put("dt", Mesibo.getDeviceType().toString())
            } catch (e: Exception) {
            }
            http = MesiboHttp()
            http!!.url = mApiUrl
            http!!.post = j.toString().toByteArray(StandardCharsets.UTF_8)

            http!!.contentType = "application/json"
            http!!.uploadFile = filePath
            http!!.uploadFileField = formFieldName
            http!!.notifyOnCompleteOnly = true
            http!!.concatData = true
            http!!.listener = this
            if (mBlocking) return http!!.executeAndWait()
            return http!!.execute()
        }

        abstract fun HandleAPIResponse(response: Response?)

        companion object {
            var result: Boolean = true
        }
    }

    class Urls {
        var upload: String = ""
        var download: String = ""
    }

    class Invite {
        var text: String = ""
        var subject: String = ""
        var title: String = ""
    }

    class Contacts {
        var name: String = ""
        var phone: String = ""
        var gid: Long = 0
        var ts: Long = 0
        var status: String = ""
        var photo: String = ""
        var tn: String = ""
        var members: String = ""
    }

    class Response internal constructor() {
        var result: String? = null
        var op: String? = null
        var error: String? = null
        var errmsg: String? = null
        var errtitle: String? = null
        var message: String? = null
        var title: String? = null
        var token: String? = null

        // set with the token for a user without a profile yet; Object since it may come as
        // true, 1 or "1"
        @SerializedName("new_user")
        var newUser: Any? = null
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

        fun isNewUser(): Boolean {
            if (newUser is Boolean) return newUser as Boolean
            if (newUser is Number) return 0 != (newUser as Number).toInt()
            if (newUser is String) return "1" == newUser || "true".equals(
                newUser as String?,
                ignoreCase = true
            )
            return false
        }
    }
}
