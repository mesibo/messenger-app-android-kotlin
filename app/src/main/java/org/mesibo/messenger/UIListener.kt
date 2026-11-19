package org.mesibo.messenger

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.view.MenuItem
import com.mesibo.api.Mesibo
import com.mesibo.api.MesiboProfile
import com.mesibo.calls.api.MesiboCall
import com.mesibo.messaging.MesiboRecycleViewHolder
import com.mesibo.messaging.MesiboUI
import com.mesibo.messaging.MesiboUiConfig
import com.mesibo.messaging.MesiboUI.MesiboMessageScreen
import com.mesibo.messaging.MesiboUI.MesiboRow
import com.mesibo.messaging.MesiboUI.MesiboScreen
import com.mesibo.messaging.MesiboUI.MesiboUserListScreen
import com.mesibo.messaging.MesiboUIListener

class UIListener : MesiboUIListener {

    companion object {
        // ids of the call items of the profile screen and popup
        private const val MENU_PROFILE_VOICE = 9101
        private const val MENU_PROFILE_VIDEO = 9102
    }

    /** each new configuration of the messaging UI (a theme applied): the app's own settings */
    override fun MesiboUI_onConfig(config: MesiboUiConfig, dark: Boolean) {
        config.userList.emptyMessage = "No messages! Click on the message icon above to start messaging!"
        config.profileViewer.showAddress = true
    }

    override fun MesiboUI_onInitScreen(screen: MesiboScreen): Boolean {
        if (MesiboUI.SCREEN_USERLIST == screen.type) {
            initUserListScreen(screen as MesiboUserListScreen)
        } else if (MesiboUI.SCREEN_MESSAGES == screen.type) {
            initMessageListScreen(screen as MesiboMessageScreen)
        } else if (MesiboUI.SCREEN_PROFILE == screen.type) {
            // the buttons under the name
            addCallItems((screen as MesiboUI.MesiboProfileScreen).actions, screen)
        } else if (MesiboUI.SCREEN_PROFILE_POPUP == screen.type) {
            // the popup's buttons, between Message and Info
            addCallItems(screen.menu, screen)
        }
        return false
    }

    /**
     * Voice and video call buttons, for the profile screen and the profile popup (none for a group
     * the user left). During a call, they bring it back.
     */
    private fun addCallItems(menu: android.view.Menu?, screen: MesiboScreen) {
        val profile = screen.profile ?: return
        if (null == menu || (profile.isGroup && !profile.isActive))
            return

        for (video in listOf(false, true)) {
            menu.add(android.view.Menu.NONE, if (video) MENU_PROFILE_VIDEO else MENU_PROFILE_VOICE, if (video) 51 else 50,
                    if (video) "Video" else "Voice")
                .setIcon(if (video) R.drawable.ic_videocam_white_24dp else R.drawable.ic_call_white_24dp)
                .setOnMenuItemClickListener {
                    // during a call: back to it
                    if (MesiboCall.getInstance().isAnyCallInProgress)
                        MesiboCall.getInstance().showCallInProgress(screen.parent)
                    else
                        MesiboCall.getInstance().callUi(screen.parent, profile, video)
                    true
                }
        }
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
        return true // true: the built-in location picker; false when the app shows its own
    }

    override fun MesiboUI_onClickedRow(screen: MesiboScreen, row: MesiboRow): Boolean {
        return true // true: the row opens as usual; false when the app handled the tap
    }

    fun userListScreenMenuHandler(context: Context, item: Int) {
        if (item == R.id.action_settings) {
            UIManager.launchUserSettings(context)
        } else if (item == R.id.action_customize) {
            UIManager.launchCustomize(context)
        } else if (item == R.id.action_conf) {
            MesiboCall.getInstance().groupCallJoinRoomUi(context, "Mesibo Conferencing Demo")
        } else if (item == R.id.action_calllogs) {
            MesiboCall.getInstance().launchCallLogs(context)
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
        screen.menu.findItem(R.id.action_customize).setOnMenuItemClickListener(menuhandler)
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
        } else if (R.id.action_disappearing == item) {
            MesiboUI.launchDisappearingMessages(context, profile)
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

        // end-to-end encryption of this chat only while it is on (when off, the screen would
        // only say the chat isn't end-to-end encrypted), and never for groups
        screen.menu.findItem(R.id.action_e2e).isVisible =
            Mesibo.e2ee().isEnabled && screen.profile?.isGroup != true
        screen.menu.findItem(R.id.action_disappearing).setOnMenuItemClickListener(menuhandler)
        setCallInProgressHandler(screen)

        screen.titleArea.setOnClickListener { MesiboUI.showProfile(screen.parent, screen.profile) }
    }
}
