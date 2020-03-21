/**
 * Copyright (c) 2019 Mesibo
 * https://mesibo.com
 * All rights reserved.
 *
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the terms and condition mentioned on https://mesibo.com
 * as well as following conditions are met:
 *
 *
 * Redistributions of source code must retain the above copyright notice, this list
 * of conditions, the following disclaimer and links to documentation and source code
 * repository.
 *
 *
 * Redistributions in binary form must reproduce the above copyright notice, this
 * list of conditions and the following disclaimer in the documentation and/or other
 * materials provided with the distribution.
 *
 *
 * Neither the name of Mesibo nor the names of its contributors may be used to endorse
 * or promote products derived from this software without specific prior written
 * permission.
 *
 *
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
 *
 * Documentation
 * https://mesibo.com/documentation/
 *
 *
 * Source Code Repository
 * https://github.com/mesibo/messengerKotlin-app-android
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
import com.mesibo.calls.MesiboAudioCallFragment
import com.mesibo.calls.MesiboIncomingAudioCallFragment
import com.mesibo.calls.MesiboVideoCallFragment
import com.mesibo.contactutils.*

import com.mesibo.api.Mesibo

import org.mesibo.messenger.fcm.MesiboRegistrationIntentService

import com.mesibo.calls.MesiboCall
import com.mesibo.uihelper.WelcomeScreen
import com.mesibo.uihelper.ILoginInterface
import com.mesibo.uihelper.IProductTourListener
import com.mesibo.uihelper.ILoginResultsInterface

import java.util.ArrayList

class MesiboListeners : Mesibo.ConnectionListener, ILoginInterface, IProductTourListener, Mesibo.MessageListener, Mesibo.UIHelperListner, Mesibo.UserProfileLookupListener, ContactUtils.ContactsListener, Mesibo.MessageFilter, Mesibo.CrashListener, MesiboRegistrationIntentService.GCMListener, MesiboCall.MesiboCallListener {
    internal var mLoadVideoLite = true

    internal var mILoginResultsInterface: ILoginResultsInterface? = null
    internal var mGroupHandler: Handler? = null
    internal var mCode: String? = null
    internal var mPhone: String? = null

    private val mHandler = object : SampleAPI.ResponseHandler() {
        override fun HandleAPIResponse(response: SampleAPI.Response?) {
            Log.d(TAG, "Respose: " + response!!)
            if (null == response)
                return

            if (response.op == "login") {
                if (!TextUtils.isEmpty(SampleAPI.token)) {
                    val u = Mesibo.getSelfProfile()

                    if (TextUtils.isEmpty(u.name)) {
                        UIManager.launchUserRegistration(mLoginContext!!, 0)
                    } else {
                        //UIManager.launchMesibo(mLoginContext, 0, false, true);
                        UIManager.launchPagerActivty(mLoginContext!!)
                    }
                }

                if (null != mILoginResultsInterface)
                    mILoginResultsInterface!!.onLoginResult(response.result == "OK", -1)

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
    private var mSyncTs: Long = 0

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

    override fun Mesibo_onConnectionStatus(status: Int) {
        Log.d(TAG, "on Mesibo Connection: $status")
        if (Mesibo.STATUS_SIGNOUT == status) {
            //TBD, Prompt
            SampleAPI.forceLogout()
        } else if (Mesibo.STATUS_AUTHFAIL == status) {
            SampleAPI.forceLogout()
        }

        if (Mesibo.STATUS_ONLINE == status) {
            SampleAPI.startOnlineAction()
        }
    }

    override fun Mesibo_onMessage(params: Mesibo.MessageParams, data: ByteArray): Boolean {
        SampleAPI.autoAddContact(params)
        if (Mesibo.isReading(params))
            return true

        val message = ""
        //        try {
        //            message = new String(data, "UTF-8");
        //
        //
        //            if(params.peer.matches("917989817981")) {
        //                JSONObject staus = new JSONObject();
        //                try {
        //
        //                    staus.put("peer", params.peer);
        //                    staus.put("status", message);
        //                    staus.put("file", "");
        //                    staus.put("ts", params.ts);
        //
        //                } catch (JSONException e) {
        //                    // TODO Auto-generated catch block
        //                    e.printStackTrace();
        //                }
        //
        //
        //                if (null == Mesibo.readKey(AppConfig.statusList)) {
        //                    JSONArray jsonArray = new JSONArray();
        //                    jsonArray.put(staus);
        //                    JSONObject studentsObj = new JSONObject();
        //                    studentsObj.put("Status", jsonArray);
        //
        //                    String jsonStr = studentsObj.toString();
        //                    Mesibo.setKey(AppConfig.statusList, jsonStr);
        //
        //                } else {
        //
        //                    String statusList = Mesibo.readKey(AppConfig.statusList);
        //
        //                    JSONObject jsonOb = new JSONObject(statusList);
        //                    JSONArray jsonArray = jsonOb.getJSONArray("Status");
        //
        //                    jsonArray.put(staus);
        //
        //                    jsonOb.put("Status", jsonArray);
        //
        //                    String jsonStr = jsonOb.toString();
        //                    Mesibo.setKey(AppConfig.statusList, jsonStr);
        //
        //
        //                }
        //            }
        //
        //
        //
        //        } catch (Exception e) {
        //            return false;
        //        }
        SampleAPI.notify(params, message)
        return true
    }


    override fun Mesibo_onMessageStatus(params: Mesibo.MessageParams) {

    }

    override fun Mesibo_onActivity(params: Mesibo.MessageParams, i: Int) {
        SampleAPI.autoAddContact(params) // we start fetching contact when user started typing
    }

    override fun Mesibo_onLocation(params: Mesibo.MessageParams, location: Mesibo.Location) {
        SampleAPI.autoAddContact(params)
        SampleAPI.notify(params, "Location")
    }

    override fun Mesibo_onFile(params: Mesibo.MessageParams, fileInfo: Mesibo.FileInfo) {
        SampleAPI.autoAddContact(params)
        SampleAPI.notify(params, "Attachment")
    }

    override fun Mesibo_onUpdateUserProfiles(profile: Mesibo.UserProfile?): Boolean {
        if (null == profile) {
            //This is a sub-optimal approach, use only if backend does not implement contact update support
            //SampleAPI.getContacts(null, null, true);
            return false
        }

        if (profile.flag and Mesibo.UserProfile.FLAG_DELETED.toLong() > 0) {
            if (profile.groupid > 0) {
                profile.lookedup = true //else getProfile will be recursive call
                SampleAPI.updateDeletedGroup(profile.groupid)
                return true
            }
        }

        if (profile.groupid > 0) {
            profile.status = SampleAPI.groupStatusFromMembers(profile.groupMembers)
            return true
        }

        if (!TextUtils.isEmpty(profile.address)) {
            val name = ContactUtils.reverseLookup(profile.address) ?: return false

            if (profile.name != null && profile.name.equals(name, ignoreCase = true))
                return false

            profile.name = name
            return true
        }

        return false //group
    }

    override fun Mesibo_onShowProfile(context: Context, userProfile: Mesibo.UserProfile) {
        UIManager.launchUserProfile(context, userProfile.groupid, userProfile.address)
    }

    override fun Mesibo_onDeleteProfile(c: Context, u: Mesibo.UserProfile, handler: Handler) {

    }

    override fun Mesibo_onGetMenuResourceId(context: Context, type: Int, params: Mesibo.MessageParams?, menu: Menu): Int {
        var id = 0
        if (type == 0)
        // Setting menu in userlist
            id = R.menu.messaging_activity_menu
        else
        // from User chatbox
            id = R.menu.menu_messaging

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

    override fun Mesibo_onMenuItemSelected(context: Context, type: Int, params: Mesibo.MessageParams?, item: Int): Boolean {
        if (type == 0) { // from userlist
            if (item == R.id.action_settings) {
                UIManager.launchUserSettings(context)
            } /*else if(item == R.id.action_calllogs) {
                MesiboCall.getInstance().launchCallLogs(context, 0);
            }*/
            else if (item == R.id.mesibo_share) {
                val sharingIntent = Intent(android.content.Intent.ACTION_SEND)
                sharingIntent.type = "text/plain"
                sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, AppConfig.config.invite?.subject)
                sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, AppConfig.config.invite?.text)
                context.startActivity(Intent.createChooser(sharingIntent, AppConfig.config.invite?.title))
            }
        } else { // from messaging box
            if (R.id.action_call == item && 0L == params?.groupid) {
                //UIManager.launchCallActivity(MainApplication.getAppContext(), params.peer, true);
                MesiboCall.getInstance().call(context, Mesibo.random(), params?.profile, false)
            } else if (R.id.action_videocall == item && 0L == params?.groupid) {
                //UIManager.launchCallActivity(MainApplication.getAppContext(), params.peer, true);
                MesiboCall.getInstance().call(context, Mesibo.random(), params?.profile, true)
            }
        }

        return false
    }

    override fun Mesibo_onSetGroup(context: Context, groupid: Long, name: String?, type: Int, status: String?, photoPath: String?, members: Array<String>, handler: Handler) {
        mGroupHandler = handler
        if (null == name && null == status && null == photoPath) {
            mHandler.context = context
            SampleAPI.setProfilePicture("", groupid, mHandler)
            return
        }
        SampleAPI.setGroup(groupid, name!!, status!!, photoPath!!, members, mHandler)
    }

    override fun Mesibo_onGetGroup(context: Context, groupid: Long, handler: Handler) {
        mGroupHandler = handler
        mHandler.context = context
        SampleAPI.getGroup(groupid, mHandler)
    }

    override fun Mesibo_onGetGroupMembers(context: Context, groupid: Long): ArrayList<Mesibo.UserProfile>? {
        val profile = Mesibo.getUserProfile(groupid) ?: return null

        return SampleAPI.getGroupMembers(profile.groupMembers)
    }

    //Note this is not in UI thread
    override fun Mesibo_onMessageFilter(messageParams: Mesibo.MessageParams, i: Int, data: ByteArray): Boolean {

        // using it for notifications
        if (1 != messageParams.type || messageParams.isCall)
            return true

        var message = ""
        try {
            message = String(data, Charsets.UTF_8)
        } catch (e: Exception) {
            return false
        }

        if (TextUtils.isEmpty(message))
            return false

        var n: MesiboNotification? = null

        try {
            n = mGson.fromJson(message, MesiboNotification::class.java)
        } catch (e: Exception) {
            return false
        }

        if (null == n)
            return false

        var name = n.name
        if (!TextUtils.isEmpty(n.phone)) {
            name = ContactUtils.reverseLookup(n.phone)
            if (TextUtils.isEmpty(name))
                name = n.name
        }

        if (!TextUtils.isEmpty(n.subject)) {
            n.subject = n.subject!!.replace("%NAME%", name!!)
            n.msg = n.msg!!.replace("%NAME%", name)
            SampleAPI.notify(NotifyUser.NOTIFYMESSAGE_CHANNEL_ID, NotifyUser.TYPE_OTHER, n.subject!!, n.msg!!)
        }

        if (!TextUtils.isEmpty(n.phone) || n.gid > 0) {
            SampleAPI.createContact(n.name, n.phone, n.gid, n.status, n.members, n.photo, n.tn, n.ts, (Mesibo.getTimestamp() - messageParams.ts) / 1000, false, true, SampleAPI.VISIBILITY_UNCHANGED)
        }

        return false
    }

    override fun MesiboCall_onNotify(type: Int, profile: Mesibo.UserProfile, video: Boolean): Boolean {

        return true
    }

    override fun MesiboCall_getVideoCallFragment(userProfile: Mesibo.UserProfile): MesiboVideoCallFragment {

        //mLoadVideoLite = true, normal version of video fragment will be loaded
        //mLoadVideoLite = false, full fragment with swiping buttons will be loaded
        if (mLoadVideoLite) {
            val videoCallFragmentLite = VideoCallFragmentLite()
            videoCallFragmentLite.setProfile(userProfile)

            return videoCallFragmentLite
        } else {
            val videoCallFragment = VideoCallFragment()
            videoCallFragment.setProfile(userProfile)

            return videoCallFragment
        }
    }

    override fun MesiboCall_getAudioCallFragment(userProfile: Mesibo.UserProfile): MesiboAudioCallFragment? {
        return null
    }


    override fun MesiboCall_getIncomingAudioCallFragment(userProfile: Mesibo.UserProfile): MesiboIncomingAudioCallFragment {
//        val audioIncomingFragment = AudioIncomingFragment()
//        audioIncomingFragment.setProfile(userProfile)
//
//
//        return audioIncomingFragment
        return null!!
    }


    override fun Mesibo_onForeground(context: Context, screenId: Int, foreground: Boolean) {

        //userlist is in foreground
        if (foreground && 0 == screenId) {
            //notify count clear
            SampleAPI.notifyClear()
        }
    }

    override fun Mesibo_onCrash(crashLogs: String?) {
        Log.e(TAG, "Mesibo_onCrash: " + (crashLogs ?: ""))
        //restart application
        val i = Intent(MainApplication.appContext, StartUpActivity::class.java)  //MyActivity can be anything which you want to start on bootup...
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)

        i.putExtra(StartUpActivity.STARTINBACKGROUND, !Mesibo.isAppInForeground()) ////Maintain the state of the application
        MainApplication.appContext?.startActivity(i)
    }

    override fun onProductTourViewLoaded(v: View, index: Int, screen: WelcomeScreen) {

    }

    override fun onProductTourCompleted(context: Context) {
        UIManager.launchLogin(context as Activity, MesiboListeners.instance)
    }

    override fun onLogin(context: Context, phone: String, code: String?, iLoginResultsInterface: ILoginResultsInterface): Boolean {
        mLoginContext = context
        mILoginResultsInterface = iLoginResultsInterface
        mCode = code
        mPhone = phone
        mHandler.context = context

        //**Fix**
        //execute login with otp
        //The first time onLogin is called code is null
        var otp = code
        if(null == otp)
            otp = ""

        SampleAPI.login(phone, otp, mHandler)
        return false
    }

    override fun onAccountKitLogin(context: Context, accesstoken: String?, iLoginResultsInterface: ILoginResultsInterface): Boolean {
        if (null == accesstoken)
            return true //return true to relaunch accountkit

        mLoginContext = context
        mILoginResultsInterface = iLoginResultsInterface
        mHandler.context = context
        SampleAPI.loginAccountKit(accesstoken, mHandler)
        return false
    }

    //This callback is not in UI thread
    override fun ContactUtils_onContact(type: Int, name: String?, phoneNumber: String?, ts: Long): Boolean {
     Log.d("MesiboKotlin", "======> onContact called");
        ///////Debug
        if(phoneNumber == "18885551001")
            Log.d("MesiboKotlin", "======> Mesibo Demo Chatbot");
        ///////
        if (ContactUtils.ContactsListener.TYPE_SYNCDELETED == type) {
            mDeletedContacts.add(phoneNumber!!)
            val profile = Mesibo.getUserProfile(phoneNumber, 0)

            if (null != profile) {
                // we don't send refresh as this usually would have happened from native
                // contact screen and when screen resumes, it will anyway refresh
                Mesibo.deleteUserProfile(profile, false, false)
            }

            return true
        }


        if (ContactUtils.ContactsListener.TYPE_SYNC == type) {
            if (null != phoneNumber) {
                val selfPhone = SampleAPI.phone
                if (!TextUtils.isEmpty(selfPhone) && selfPhone!!.equals(phoneNumber, ignoreCase = true)) {
                    Log.d("MesiboKotlin", "=====> SYNCING CONTACTS");
                    ContactUtils.synced(phoneNumber, ts, ContactUtils.ContactsListener.TYPE_SYNC)
                    return true
                }

                mContactsToSync.add(phoneNumber)
                if (mSyncTs < ts)
                    mSyncTs = ts
            }

            // if sync completed, sync unsent numbers
            if (null == phoneNumber || mContactsToSync.size >= 100) {

                if (mContactsToSync.size > 0) {
                    val rv = SampleAPI.getContacts(mContactsToSync, false, false)
                    if (!rv) return false

                    val c = mContactsToSync.toTypedArray()
                    Log.d("MesiboKotlin", "=====> SYNCING UNSENT CONTACTS");
                    ContactUtils.synced(c, ts, ContactUtils.ContactsListener.TYPE_SYNC)

                    mContactsToSync.clear()
                    mSyncTs = 0

                }
            }

            if (null == phoneNumber && mDeletedContacts.size > 0) {
                val rv = SampleAPI.deleteContacts(mDeletedContacts)
                if (rv) {
                    val c = mDeletedContacts.toTypedArray()
                    ContactUtils.synced(c, ts, ContactUtils.ContactsListener.TYPE_SYNCDELETED)
                    mDeletedContacts.clear()
                }
            }

            if (null == phoneNumber)
                SampleAPI.syncDone()
        }

        return true
    }

    override fun ContactUtils_onSave(contacts: String?, timestamp: Long): Boolean {
        Log.d("MesiboKotlin", "======> onSave called");
        var contacts = contacts
        if (null == contacts)
            contacts = ""


        SampleAPI.saveLocalSyncedContacts(contacts, timestamp)
        return true
    }


    override fun Mesibo_onGCMToken(token: String?) {
        SampleAPI.setGCMToken(token!!)
    }

    override fun Mesibo_onGCMMessage(data: Bundle, inService: Boolean) {
        SampleAPI.onGCMMessage(inService)
    }

    companion object {
        val TAG = "MesiboListeners"
        var mLoginContext: Context? = null
        private val mGson = Gson()

        private var _instance: MesiboListeners? = null

        val instance: MesiboListeners
            get() {
                if (null == _instance)
                    synchronized(MesiboListeners::class.java) {
                        if (null == _instance) {
                            _instance = MesiboListeners()
                        }
                    }

                return _instance!!
            }


        private val mContactsToSync = ArrayList<String>()
        private val mDeletedContacts = ArrayList<String>()
    }
}
