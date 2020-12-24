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

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.support.v4.view.MenuItemCompat
import android.text.TextUtils
import android.util.Log
import android.view.Menu
import android.view.View
import com.google.gson.Gson
import com.mesibo.api.Mesibo.*
import com.mesibo.calls.api.MesiboCall.CallProperties
import com.mesibo.calls.ui.MesiboCallUi
import com.mesibo.contactutils.ContactUtils
import com.mesibo.contactutils.ContactUtils.ContactsListener
import com.mesibo.uihelper.ILoginInterface
import com.mesibo.uihelper.ILoginResultsInterface
import com.mesibo.uihelper.IProductTourListener
import com.mesibo.uihelper.WelcomeScreen
import org.mesibo.messenger.SampleAPI.autoAddContact
import org.mesibo.messenger.SampleAPI.createContact
import org.mesibo.messenger.SampleAPI.deleteContacts
import org.mesibo.messenger.SampleAPI.forceLogout
import org.mesibo.messenger.SampleAPI.getContacts
import org.mesibo.messenger.SampleAPI.getGroup
import org.mesibo.messenger.SampleAPI.getGroupMembers
import org.mesibo.messenger.SampleAPI.groupStatusFromMembers
import org.mesibo.messenger.SampleAPI.login
import org.mesibo.messenger.SampleAPI.loginAccountKit
import org.mesibo.messenger.SampleAPI.notify
import org.mesibo.messenger.SampleAPI.notifyClear
import org.mesibo.messenger.SampleAPI.onGCMMessage
import org.mesibo.messenger.SampleAPI.phone
import org.mesibo.messenger.SampleAPI.saveLocalSyncedContacts
import org.mesibo.messenger.SampleAPI.setGCMToken
import org.mesibo.messenger.SampleAPI.setGroup
import org.mesibo.messenger.SampleAPI.setProfilePicture
import org.mesibo.messenger.SampleAPI.startOnlineAction
import org.mesibo.messenger.SampleAPI.syncDone
import org.mesibo.messenger.SampleAPI.token
import org.mesibo.messenger.SampleAPI.updateDeletedGroup
import org.mesibo.messenger.UIManager.launchLogin
import org.mesibo.messenger.UIManager.launchMesibo
import org.mesibo.messenger.UIManager.launchUserProfile
import org.mesibo.messenger.UIManager.launchUserRegistration
import org.mesibo.messenger.UIManager.launchUserSettings
import org.mesibo.messenger.fcm.MesiboRegistrationIntentService.GCMListener
import org.webrtc.ContextUtils.getApplicationContext
import java.util.*

class MesiboListeners : ConnectionListener, ILoginInterface, IProductTourListener, MessageListener, UIHelperListner, UserProfileLookupListener, ContactsListener, MessageFilter, CrashListener, GCMListener, MesiboCallUi.Listener {
    class MesiboNotification internal constructor() {
        var subject: String? = null
        var msg: String? = null
        var type: String? = null
        var action: String? = null
        var name: String? = null
        var gid: Long = 0
        var phone: String? = null
        var status: String? = null
        var members: String? = null
        var photo: String? = null
        var ts: Long = 0
        var tn: String? = null
    }

    var mILoginResultsInterface: ILoginResultsInterface? = null
    var mGroupHandler: Handler? = null
    var mCode: String? = null
    var mPhone: String? = null
    private val mHandler: SampleAPI.ResponseHandler = object : SampleAPI.ResponseHandler() {
        override fun HandleAPIResponse(response: SampleAPI.Response?) {
            Log.d(TAG, "Respose: $response")
            if (null == response) return
            if (response.op == "login") {
                if (!TextUtils.isEmpty(token)) {
                    val u = getSelfProfile()
                    if (TextUtils.isEmpty(u.name)) {
                        launchUserRegistration(mLoginContext!!, 0)
                    } else {
                        launchMesibo(mLoginContext, 0, false, true)
                    }
                }
                if (null != mILoginResultsInterface) mILoginResultsInterface!!.onLoginResult(response.result == "OK", -1)
            } else if (response.op == "setgroup") {
                if (null != mGroupHandler) {
                    val msg = Message()
                    val bundle = Bundle()
                    bundle.putLong("groupid", response.gid)
                    bundle.putString("result", response.result)
                    msg.data = bundle
                    mGroupHandler!!.handleMessage(msg)
                }
            } else if (response.op == "getgroup") {
                if (null != mGroupHandler) {
                    val msg = Message()
                    val bundle = Bundle()
                    bundle.putString("result", response.result)
                    msg.data = bundle
                    mGroupHandler!!.handleMessage(msg)
                }
            }
            //handleAPIResponse(response);
        }
    }

    override fun Mesibo_onConnectionStatus(status: Int) {
        Log.d(TAG, "on Mesibo Connection: $status")
        if (STATUS_SIGNOUT == status) {
            //TBD, Prompt
            forceLogout()
        } else if (STATUS_AUTHFAIL == status) {
            forceLogout()
        }
        if (STATUS_ONLINE == status) {
            startOnlineAction()
        }
    }

    override fun Mesibo_onMessage(params: MessageParams, data: ByteArray): Boolean {
        autoAddContact(params)
        if (isReading(params)) return true
        var message = ""
        try {
            message = data.toString(Charsets.UTF_8);
        } catch (e: Exception) {
            return false
        }
        notify(params, message)
        return true
    }

    override fun Mesibo_onMessageStatus(params: MessageParams) {}
    override fun Mesibo_onActivity(params: MessageParams, i: Int) {
        autoAddContact(params) // we start fetching contact when user started typing
    }

    override fun Mesibo_onLocation(params: MessageParams, location: Location) {
        autoAddContact(params)
        notify(params, "Location")
    }

    override fun Mesibo_onFile(params: MessageParams, fileInfo: FileInfo) {
        autoAddContact(params)
        notify(params, "Attachment")
    }

    override fun Mesibo_onUpdateUserProfiles(profile: UserProfile?): Boolean {
        if (null == profile) {
            //This is a sub-optimal approach, use only if backend does not implement contact update support
            //SampleAPI.getContacts(null, null, true);
            return false
        }
        if (profile.flag and UserProfile.FLAG_DELETED.toLong() > 0) {
            if (profile.groupid > 0) {
                profile.lookedup = true //else getProfile will be recursive call
                updateDeletedGroup(profile.groupid)
                return true
            }
        }
        if (profile.groupid > 0) {
            profile.status = groupStatusFromMembers(profile.groupMembers)
            return true
        }
        if (!TextUtils.isEmpty(profile.address)) {
            val name = ContactUtils.reverseLookup(profile.address) ?: return false
            if (profile.name != null && profile.name.equals(name, ignoreCase = true)) return false
            profile.name = name
            return true
        }
        return false //group
    }

    override fun Mesibo_onShowProfile(context: Context, userProfile: UserProfile) {
        launchUserProfile(context, userProfile.groupid, userProfile.address)
    }

    override fun Mesibo_onDeleteProfile(c: Context, u: UserProfile, handler: Handler) {}
    override fun Mesibo_onGetMenuResourceId(context: Context, type: Int, params: MessageParams?, menu: Menu): Int {
        var id = 0
        id = if (type == 0) // Setting menu in userlist
            R.menu.messaging_activity_menu else  // from User chatbox
            R.menu.menu_messaging
        (context as Activity).menuInflater.inflate(id, menu)
        if (1 == type && null != params && params.groupid > 0) {
            var menuItem = menu.findItem(R.id.action_call)
            menuItem.isVisible = false
            MenuItemCompat.setShowAsAction(menuItem, MenuItemCompat.SHOW_AS_ACTION_NEVER)
            menuItem = menu.findItem(R.id.action_videocall)
            menuItem.isVisible = false
            MenuItemCompat.setShowAsAction(menuItem, MenuItemCompat.SHOW_AS_ACTION_NEVER)
        }
        return 0
    }

    override fun Mesibo_onMenuItemSelected(context: Context, type: Int, params: MessageParams?, item: Int): Boolean {
        if (type == 0) { // from userlist
            if (item == R.id.action_settings) {
                launchUserSettings(context)
            } else if (item == R.id.action_calllogs) {
                MesiboCallUi.getInstance().launchCallLogs(context, 0)
            } else if (item == R.id.mesibo_share) {
                val sharingIntent = Intent(Intent.ACTION_SEND)
                sharingIntent.type = "text/plain"
                sharingIntent.putExtra(Intent.EXTRA_SUBJECT, AppConfig.config?.invite?.subject)
                sharingIntent.putExtra(Intent.EXTRA_TEXT, AppConfig.config?.invite?.text)
                context.startActivity(Intent.createChooser(sharingIntent, AppConfig.config?.invite?.title))
            }
        } else { // from messaging box
            if (params != null) {
                if (R.id.action_call === item && 0L == params.groupid) {
                    //UIManager.launchCallActivity(MainApplication.getAppContext(), params.peer, true);
                    if (!MesiboCallUi.callUi(context, params.profile.address, false)) MesiboCallUi.callUiForExistingCall(context)
                } else if (R.id.action_videocall === item && 0L == params.groupid) {
                    //UIManager.launchCallActivity(MainApplication.getAppContext(), params.peer, true);
                    if (!MesiboCallUi.callUi(context, params.profile.address, true)) MesiboCallUi.callUiForExistingCall(context)
                }
            }
        }
        return false
    }

    override fun Mesibo_onSetGroup(context: Context, groupid: Long, name: String, type: Int, status: String, photoPath: String, members: Array<String>, handler: Handler) {
        mGroupHandler = handler
        if (null == name && null == status && null == photoPath) {
            mHandler.context = context
            setProfilePicture(null, groupid, mHandler)
            return
        }
        setGroup(groupid, name, status, photoPath, members, mHandler)
    }

    override fun Mesibo_onGetGroup(context: Context, groupid: Long, handler: Handler) {
        mGroupHandler = handler
        mHandler.context = context
        getGroup(groupid, mHandler)
    }

    override fun Mesibo_onGetGroupMembers(context: Context, groupid: Long): ArrayList<UserProfile?>? {
        val profile = getUserProfile(groupid) ?: return null
        return getGroupMembers(profile.groupMembers)
    }

    //Note this is not in UI thread
    override fun Mesibo_onMessageFilter(messageParams: MessageParams, i: Int, data: ByteArray): Boolean {

        // using it for notifications
        if (1 != messageParams.type || messageParams.isCall) return true
        var message = ""
        try {
            message = data.toString( Charsets.UTF_8)
        } catch (e: Exception) {
            return false
        }
        if (TextUtils.isEmpty(message)) return false
        var n: MesiboNotification? = null
        n = try {
            mGson.fromJson(message, MesiboNotification::class.java)
        } catch (e: Exception) {
            return false
        }
        if (null == n) return false
        var name = n.name
        if (!TextUtils.isEmpty(n.phone)) {
            name = ContactUtils.reverseLookup(n.phone)
            if (TextUtils.isEmpty(name)) name = n.name
        }
        if (!TextUtils.isEmpty(n.subject)) {
            n.subject = n.subject!!.replace("%NAME%", name!!)
            n.msg = n.msg!!.replace("%NAME%", name)
            notify(NotifyUser.NOTIFYMESSAGE_CHANNEL_ID, NotifyUser.TYPE_OTHER, n.subject, n.msg)
        }
        if (!TextUtils.isEmpty(n.phone) || n.gid > 0) {
            createContact(n.name, n.phone, n.gid, n.status, n.members, n.photo, n.tn, n.ts, (getTimestamp() - messageParams.ts) / 1000, false, true, SampleAPI.VISIBILITY_UNCHANGED)
        }
        return false
    }

    override fun MesiboCallUi_OnConfig(callProperties: CallProperties): CallProperties {
        return callProperties
    }

    override fun MesiboCallUi_OnError(callProperties: CallProperties, i: Int): Boolean {
        return false
    }

    override fun MesiboCallUi_onNotify(type: Int, profile: UserProfile, video: Boolean): Boolean {
        return false
    }

    override fun Mesibo_onForeground(context: Context, screenId: Int, foreground: Boolean) {

        //userlist is in foreground
        if (foreground && 0 == screenId) {
            //notify count clear
            notifyClear()
        }
    }

    override fun Mesibo_onCrash(crashLogs: String) {
        Log.e(TAG, "Mesibo_onCrash: " + (crashLogs ?: ""))
        //restart application
        val i = Intent(MainApplication.getAppContext(), StartUpActivity::class.java) //MyActivity can be anything which you want to start on bootup...
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        i.putExtra(StartUpActivity.STARTINBACKGROUND, !isAppInForeground()) ////Maintain the state of the application

        //Replacing MainApplication.getAppContext
        MainApplication.getAppContext()?.startActivity(i)
    }

    override fun onProductTourViewLoaded(v: View, index: Int, screen: WelcomeScreen) {}
    override fun onProductTourCompleted(context: Context) {
        launchLogin(context as Activity, instance)
    }

    override fun onLogin(context: Context, phone: String, code: String?, iLoginResultsInterface: ILoginResultsInterface): Boolean {
        mLoginContext = context
        mILoginResultsInterface = iLoginResultsInterface
        mCode = code
        mPhone = phone
        mHandler.context = context
        login(phone, code, mHandler)
        return false
    }

    override fun onAccountKitLogin(context: Context, accesstoken: String, iLoginResultsInterface: ILoginResultsInterface): Boolean {
        if (null == accesstoken) return true //return true to relaunch accountkit
        mLoginContext = context
        mILoginResultsInterface = iLoginResultsInterface
        mHandler.context = context
        loginAccountKit(accesstoken, mHandler)
        return false
    }

    private var mSyncTs: Long = 0

    //This callback is not in UI thread
    override fun ContactUtils_onContact(type: Int, name: String?, phoneNumber: String?, ts: Long): Boolean {
        if (ContactsListener.TYPE_SYNCDELETED == type) {
            if (phoneNumber != null) {
                mDeletedContacts.add(phoneNumber)
            }
            val profile = getUserProfile(phoneNumber, 0)
            if (null != profile) {
                // we don't send refresh as this usually would have happened from native
                // contact screen and when screen resumes, it will anyway refresh
                deleteUserProfile(profile, false, false)
            }
            return true
        }
        if (ContactsListener.TYPE_SYNC == type) {
            if (null != phoneNumber) {
                val selfPhone = phone
                if (!TextUtils.isEmpty(selfPhone) && selfPhone.equals(phoneNumber, ignoreCase = true)) {
                    ContactUtils.synced(phoneNumber, ts, ContactsListener.TYPE_SYNC)
                    return true
                }
                mContactsToSync.add(phoneNumber)
                if (mSyncTs < ts) mSyncTs = ts
            }

            // if sync completed, sync unsent numbers
            if (null == phoneNumber || mContactsToSync.size >= 100) {
                if (mContactsToSync.size > 0) {
                    val rv = getContacts(mContactsToSync, false, false)
                    if (!rv) return false
                    val c = mContactsToSync.toTypedArray()
                    ContactUtils.synced(c, ts, ContactsListener.TYPE_SYNC)
                    mContactsToSync.clear()
                    mSyncTs = 0
                }
            }
            if (null == phoneNumber && mDeletedContacts.size > 0) {
                val rv = deleteContacts(mDeletedContacts)
                if (rv) {
                    val c = mDeletedContacts.toTypedArray()
                    ContactUtils.synced(c, ts, ContactsListener.TYPE_SYNCDELETED)
                    mDeletedContacts.clear()
                }
            }
            if (null == phoneNumber) syncDone()
        }
        return true
    }

    override fun ContactUtils_onSave(contacts: String, timestamp: Long): Boolean {
        var contacts: String? = contacts
        if (null == contacts) contacts = ""
        saveLocalSyncedContacts(contacts, timestamp)
        return true
    }

    override fun Mesibo_onGCMToken(token: String?) {
        setGCMToken(token)
    }

    override fun Mesibo_onGCMMessage( /*Bundle data,*/
            inService: Boolean) {
        onGCMMessage(inService)
    }

    companion object {
        const val TAG = "MesiboListeners"
        var mLoginContext: Context? = null
        private val mGson = Gson()
        private var _instance: MesiboListeners? = null
        val instance: MesiboListeners?
            get() {
                if (null == _instance) synchronized(MesiboListeners::class.java) {
                    if (null == _instance) {
                        _instance = MesiboListeners()
                    }
                }
                return _instance
            }
        private val mContactsToSync = ArrayList<String>()
        private val mDeletedContacts = ArrayList<String>()
    }
}