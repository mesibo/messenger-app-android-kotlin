package org.mesibo.messenger

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.view.MenuItem
import com.mesibo.api.MesiboProfile
import com.mesibo.calls.api.MesiboCall
import com.mesibo.calls.ui.MesiboCallUi
import com.mesibo.messaging.MesiboRecycleViewHolder
import com.mesibo.messaging.MesiboUI
import com.mesibo.messaging.MesiboUI.MesiboMessageScreen
import com.mesibo.messaging.MesiboUI.MesiboRow
import com.mesibo.messaging.MesiboUI.MesiboScreen
import com.mesibo.messaging.MesiboUI.MesiboUserListScreen
import com.mesibo.messaging.MesiboUIListener

class UIListener : MesiboUIListener {
    override fun MesiboUI_onInitScreen(screen: MesiboScreen): Boolean {
        if (MesiboUI.SCREEN_USERLIST == screen.type) {
            initUserListScreen(screen as MesiboUserListScreen)
        } else if (MesiboUI.SCREEN_MESSAGES == screen.type) {
            initMessageListScreen(screen as MesiboMessageScreen)
        }
        return false
    }

    // a call started or ended (MesiboCall.CallMonitorListener -> MesiboUI.updateScreens)
    override fun MesiboUI_onUpdateScreen(screen: MesiboScreen) {
        updateCallButtons(screen)
    }

    /** during a call: the call in progress button instead of the voice / video call buttons  */
    fun updateCallButtons(screen: MesiboScreen) {
        if (null == screen.menu) return
        val inCall = MesiboCall.getInstance().isAnyCallInProgress
        val progress = screen.menu.findItem(R.id.action_call_in_progress)
        progress?.setVisible(inCall)

        if (MesiboUI.SCREEN_MESSAGES == screen.type) {
            val profile = (screen as MesiboMessageScreen).profile
            val canCall = !inCall && !(null != profile && profile.isGroup && !profile.isActive)
            val call = screen.menu.findItem(R.id.action_call)
            val video = screen.menu.findItem(R.id.action_videocall)
            call?.setVisible(canCall)
            video?.setVisible(canCall)
        }
    }

    private fun setCallInProgressHandler(screen: MesiboScreen) {
        val progress = screen.menu.findItem(R.id.action_call_in_progress)
        progress?.setOnMenuItemClickListener { item: MenuItem? ->
            MesiboCall.getInstance().showCallInProgress(screen.parent)
            true
        }
        updateCallButtons(screen)
    }

    override fun MesiboUI_onGetCustomRow(
        screen: MesiboScreen,
        row: MesiboRow
    ): MesiboRecycleViewHolder? {
        return null
    }

    override fun MesiboUI_onUpdateRow(
        screen: MesiboScreen,
        row: MesiboRow,
        last: Boolean
    ): Boolean {
        return false
    }

    override fun MesiboUI_onShowLocation(context: Context, profile: MesiboProfile): Boolean {
        return false
    }

    override fun MesiboUI_onClickedRow(screen: MesiboScreen, row: MesiboRow): Boolean {
        return false
    }

    fun userListScreenMenuHandler(context: Context, item: Int) {
        if (item == R.id.action_settings) {
            UIManager.launchUserSettings(context)
        } else if (item == R.id.action_conf) {
            MesiboCall.getInstance().groupCallJoinRoomUi(context, "Mesibo Conferencing Demo")
        } else if (item == R.id.action_calllogs) {
            MesiboCallUi.getInstance().launchCallLogs(context, 0)
        } else if (item == R.id.action_backup) {
            if (context is Activity) BackupRestore.show(context)
        } else if (item == R.id.action_menu_e2ee) {
            MesiboUI.showEndToEndEncryptionInfoForSelf(context)
        } else if (item == R.id.mesibo_share) {
            val sharingIntent = Intent(Intent.ACTION_SEND)
            sharingIntent.setType("text/plain")
            val invite = AppConfig.config.invite // from the server; may be missing
            sharingIntent.putExtra(Intent.EXTRA_SUBJECT, invite?.subject)
            sharingIntent.putExtra(Intent.EXTRA_TEXT, invite?.text)
            context.startActivity(Intent.createChooser(sharingIntent, invite?.title))
        }
    }

    fun initUserListScreen(screen: MesiboUserListScreen) {
        (screen.parent as Activity).menuInflater.inflate(
            R.menu.messaging_activity_menu,
            screen.menu
        )

        val menuhandler = MenuItem.OnMenuItemClickListener { item ->
            userListScreenMenuHandler(screen.parent, item.itemId)
            true
        }

        screen.menu.findItem(R.id.action_settings).setOnMenuItemClickListener(menuhandler)
        screen.menu.findItem(R.id.action_conf).setOnMenuItemClickListener(menuhandler)
        screen.menu.findItem(R.id.action_calllogs).setOnMenuItemClickListener(menuhandler)
        screen.menu.findItem(R.id.action_backup).setOnMenuItemClickListener(menuhandler)
        screen.menu.findItem(R.id.action_menu_e2ee).setOnMenuItemClickListener(menuhandler)
        screen.menu.findItem(R.id.mesibo_share).setOnMenuItemClickListener(menuhandler)
        setCallInProgressHandler(screen)
    }

    fun messageListScreenMenuHandler(context: Context?, item: Int, profile: MesiboProfile?) {
        if (null == profile) {
            return
        }

        // during a call, callUi brings back the call in progress instead
        if (R.id.action_call == item) {
            MesiboCall.getInstance().callUi(context, profile, false)
        } else if (R.id.action_videocall == item) {
            MesiboCall.getInstance().callUi(context, profile, true)
        } else if (R.id.action_e2e == item) {
            MesiboUI.showEndToEndEncryptionInfo(context, profile.getAddress(), profile.groupid)
        }
    }

    fun initMessageListScreen(screen: MesiboMessageScreen) {
        (screen.parent as Activity).menuInflater.inflate(R.menu.menu_messaging, screen.menu)

        // set group call icons
        if (null != screen.profile) {
            var menuItem = screen.menu.findItem(R.id.action_call)
            if (screen.profile.isGroup && !screen.profile.isActive) menuItem.setVisible(false)
            menuItem.setIcon(if (screen.profile.isGroup) MesiboCall.MESIBO_DEFAULTICON_GROUPAUDIOCALL else MesiboCall.MESIBO_DEFAULTICON_AUDIOCALL)

            // MenuItemCompat.setShowAsAction(menuItem, MenuItemCompat.SHOW_AS_ACTION_NEVER);
            menuItem = screen.menu.findItem(R.id.action_videocall)
            menuItem.setIcon(if (screen.profile.isGroup) MesiboCall.MESIBO_DEFAULTICON_GROUPVIDEOCALL else MesiboCall.MESIBO_DEFAULTICON_VIDEOCALL)
            if (screen.profile.isGroup && !screen.profile.isActive) menuItem.setVisible(false)
            //MenuItemCompat.setShowAsAction(menuItem, MenuItemCompat.SHOW_AS_ACTION_NEVER);
        }

        val menuhandler = MenuItem.OnMenuItemClickListener { item ->
            messageListScreenMenuHandler(screen.parent, item.itemId, screen.profile)
            true
        }

        screen.menu.findItem(R.id.action_call).setOnMenuItemClickListener(menuhandler)
        screen.menu.findItem(R.id.action_videocall).setOnMenuItemClickListener(menuhandler)
        screen.menu.findItem(R.id.action_e2e).setOnMenuItemClickListener(menuhandler)
        setCallInProgressHandler(screen)

        screen.titleArea.setOnClickListener { MesiboUI.showProfile(screen.parent, screen.profile) }
    }
}
