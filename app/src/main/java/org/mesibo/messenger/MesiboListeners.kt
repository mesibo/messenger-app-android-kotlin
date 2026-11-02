package org.mesibo.messenger

import android.content.Context
import android.graphics.Bitmap
import android.text.TextUtils
import android.util.Log
import com.mesibo.api.Mesibo
import com.mesibo.api.Mesibo.AppStateListener
import com.mesibo.api.Mesibo.ConnectionListener
import com.mesibo.api.Mesibo.CrashListener
import com.mesibo.api.Mesibo.EndToEndEncryptionListener
import com.mesibo.api.Mesibo.GroupListener
import com.mesibo.api.Mesibo.MessageFilter
import com.mesibo.api.Mesibo.MessageListener
import com.mesibo.api.Mesibo.ProfileCustomizationListener
import com.mesibo.api.Mesibo.ProfileListener
import com.mesibo.api.MesiboGroupProfile
import com.mesibo.api.MesiboGroupProfile.GroupPin
import com.mesibo.api.MesiboGroupProfile.GroupSettings
import com.mesibo.api.MesiboGroupProfile.MemberPermissions
import com.mesibo.api.MesiboMessage
import com.mesibo.api.MesiboProfile
import com.mesibo.messaging.MesiboDialogs
import com.mesibo.uihelper.MesiboLoginUiHelperListener
import com.mesibo.uihelper.MesiboLoginUiHelperResultCallback
import org.mesibo.messenger.SampleAPI.ResponseHandler

class MesiboListeners : ConnectionListener, MesiboLoginUiHelperListener, MessageListener,
    MessageFilter, ProfileListener, ProfileCustomizationListener, CrashListener,
    GroupListener, AppStateListener, EndToEndEncryptionListener {
    var mMesiboLoginUiHelperResultCallback: MesiboLoginUiHelperResultCallback? = null
    var mLastContext: Context? = null

    private val mHandler: ResponseHandler = object : ResponseHandler() {
        override fun HandleAPIResponse(response: SampleAPI.Response?) {
            Log.d(TAG, "Respose: $response")
            if (null == response) return

            if ("login" == response.op) {
                if (!TextUtils.isEmpty(SampleAPI.token)) {
                    // the login screen, else the app (both screens start in a new task if needed)
                    val context = mLoginContext ?: MainApplication.appContext
                    if (response.isNewUser()) {
                        UIManager.launchEditProfile(context, true)
                    } else {
                        // via startup: requests permissions, then opens the user list
                        UIManager.launchStartupActivity(context, true)
                    }
                }

                if (null != mMesiboLoginUiHelperResultCallback && null == response.errmsg) mMesiboLoginUiHelperResultCallback!!.onLoginResult(
                    "OK" == response.result
                )
            }
        }
    }

    override fun Mesibo_onConnectionStatus(status: Int) {
        Log.d(TAG, "Mesibo_onConnectionStatus: $status")
        if (Mesibo.STATUS_SIGNOUT == status) {
            MesiboDialogs.alert(
                mLastContext,
                "Signed Out",
                "You have signed-in from other device and hence signed out here"
            )
            SampleAPI.forceLogout()
        } else if (Mesibo.STATUS_AUTHFAIL == status) {
            MesiboDialogs.alert(
                mLastContext,
                "Signed Out",
                "Login Expired. Login again to continue."
            )
            SampleAPI.forceLogout()
        }

        if (Mesibo.STATUS_ONLINE == status) {
            SampleAPI.startOnlineAction()
        }
    }

    override fun Mesibo_onEndToEndEncryption(profile: MesiboProfile, status: Int) {
        //Log.d(TAG, "Mesibo_onEndToEndEncryption: " + status);
    }

    override fun Mesibo_onMessage(msg: MesiboMessage) {
        if (!msg.isRealtimeMessage || Mesibo.MSGSTATUS_OUTBOX == msg.status) return

        msg.getProfile().removeLocalProfile()

        if (msg.isEndToEndEncryptionStatus) return

        if (Mesibo.isReading(msg)) return

        if (Mesibo.isAppInForeground()) return

        var message = msg.message
        if (TextUtils.isEmpty(message)) message = msg.title
        if (TextUtils.isEmpty(message) && msg.hasImage()) message = "Picture"
        if (TextUtils.isEmpty(message) && msg.hasVideo()) message = "Video"
        if (TextUtils.isEmpty(message) && msg.hasAudio()) message = "Audio"
        if (TextUtils.isEmpty(message) && msg.hasDocument()) message = "Attachment"
        if (TextUtils.isEmpty(message) && msg.hasLocation()) message = "Location"

        SampleAPI.notify(msg, message)
        return
    }

    override fun Mesibo_onMessageStatus(params: MesiboMessage) {
    }

    override fun Mesibo_onMessageUpdate(mesiboMessage: MesiboMessage) {
    }

    override fun Mesibo_onProfileUpdated(userProfile: MesiboProfile) {
        Log.d(TAG, "Mesibo_onProfileUpdated: ")
    }

    override fun Mesibo_onCustomizeProfile(profile: MesiboProfile): Boolean {
        return true
    }

    // null: the default (no custom name)
    override fun Mesibo_onGetProfileName(profile: MesiboProfile): String? {
        if (profile.isGroup) {
            return null
        }
        val c = Mesibo.getPhoneContactsManager().getPhoneNumberInfo(profile.getAddress(), true)
        return c?.formattedPhoneNumber
    }

    // null: the default (no custom picture)
    override fun Mesibo_onGetProfileImage(profile: MesiboProfile): Bitmap? {
        return null
    }

    override fun Mesibo_onMessageFilter(msg: MesiboMessage): Boolean {
        // using it for notifications

        if (1 != msg.type || msg.isCall) return true

        return false
    }

    override fun Mesibo_onGroupCreated(groupProfile: MesiboProfile) {
        Log.d(TAG, "New group " + groupProfile.groupid)
    }

    override fun Mesibo_onGroupJoined(groupProfile: MesiboProfile) {
        SampleAPI.notify(
            NotifyUser.ID_GROUP_JOINED,
            "Joined a group",
            "You have been added to the group " + groupProfile.name
        )
    }

    override fun Mesibo_onGroupLeft(groupProfile: MesiboProfile) {
        SampleAPI.notify(
            NotifyUser.ID_GROUP_LEFT,
            "Left a group",
            "You left the group " + groupProfile.name
        )
    }

    override fun Mesibo_onGroupMembers(
        groupProfile: MesiboProfile,
        members: Array<MesiboGroupProfile.Member>
    ) {
    }

    override fun Mesibo_onGroupMembersJoined(
        groupProfile: MesiboProfile,
        members: Array<MesiboGroupProfile.Member>
    ) {
    }

    override fun Mesibo_onGroupMembersRemoved(
        groupProfile: MesiboProfile,
        members: Array<MesiboGroupProfile.Member>
    ) {
    }

    override fun Mesibo_onGroupSettings(
        mesiboProfile: MesiboProfile,
        groupSettings: GroupSettings,
        memberPermissions: MemberPermissions,
        groupPins: Array<out GroupPin?>? // null: no pins
    ) {
    }

    override fun Mesibo_onGroupError(mesiboProfile: MesiboProfile, l: Long) {
    }

    // The call screen is no longer forced open when the app comes back during a call, so the
    // user can use the app meanwhile; the ongoing call notification (status bar) and the call
    // buttons bring the call back.
    override fun Mesibo_onForeground(foreground: Boolean) {
    }

    override fun Mesibo_onForeground(context: Context, screenId: Int, foreground: Boolean) {
        //userlist is in foreground


        if (foreground && 0 == screenId) {
            //notify count clear
            SampleAPI.notifyClear()
        }

        if (foreground) mLastContext = context
    }

    override fun Mesibo_onCrash(crashLogs: String) {
        Log.e(TAG, "Mesibo_onCrash: " + (if ((null != crashLogs)) crashLogs else ""))
        //restart application
        StartUpActivity.newInstance(
            MainApplication.appContext,
            !Mesibo.isAppInForeground()
        )
    }

    override fun MesiboLoginUiHelper_onLogin(
        context: Context,
        phone: String,
        code: String,
        MesiboLoginUiHelperResultCallback: MesiboLoginUiHelperResultCallback
    ): Boolean {
        mLoginContext = context
        mMesiboLoginUiHelperResultCallback = MesiboLoginUiHelperResultCallback
        mHandler.context = context
        SampleAPI.login(phone, code, mHandler)
        return false
    }

    override fun MesiboLoginUiHelper_getCountryCode(context: Context): Int {
        return 0
    }

    override fun MesiboLoginUiHelper_isPhoneValid(context: Context, s: String): Boolean {
        return true
    }

    companion object {
        const val TAG: String = "MesiboListeners"
        var mLoginContext: Context? = null

        // created on first use; lazy is thread-safe
        val instance: MesiboListeners by lazy { MesiboListeners() }
    }
}
