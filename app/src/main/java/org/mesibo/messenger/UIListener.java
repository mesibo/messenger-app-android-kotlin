package org.mesibo.messenger;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.MenuItem;
import android.view.View;

import com.mesibo.api.Mesibo;
import com.mesibo.api.MesiboProfile;
import com.mesibo.calls.api.MesiboCall;
import com.mesibo.calls.ui.MesiboCallUi;
import com.mesibo.messaging.MesiboRecycleViewHolder;
import com.mesibo.messaging.MesiboUI;
import com.mesibo.messaging.MesiboUIListener;

public class UIListener implements MesiboUIListener {
    private static Context mLastUserListContext = null;
    private static Context mLastMessagingContext = null;
    @Override
    public boolean MesiboUI_onInitScreen(MesiboUI.MesiboScreen screen) {
        if(screen.userList) {
            mLastUserListContext = screen.parent;
            initUserListScreen((MesiboUI.MesiboUserListScreen) screen);
        } else {
            mLastMessagingContext = screen.parent;
            initMessageListScreen((MesiboUI.MesiboMessageScreen) screen);
        }
        return false;
    }

    public static Context getLastUserListContext() {
        return mLastUserListContext;
    }

    public static Context getLastMessagingContext() {
        return mLastMessagingContext;
    }

    @Override
    public MesiboRecycleViewHolder MesiboUI_onGetCustomRow(MesiboUI.MesiboScreen screen, MesiboUI.MesiboRow row) {
        return null;
    }

    @Override
    public boolean MesiboUI_onUpdateRow(MesiboUI.MesiboScreen screen, MesiboUI.MesiboRow row, boolean last) {
        return false;
    }

    @Override
    public boolean MesiboUI_onShowLocation(Context context, MesiboProfile profile) {
        return false;
    }

    void userListScreenMenuHandler(Context context, int item) {
        if (item == R.id.action_settings) {
            UIManager.launchUserSettings(context);
        } else if(item == R.id.action_conf) {
            MesiboCall.getInstance().groupCallJoinRoomUi(context, "Mesibo Conferencing Demo");
        } else if(item == R.id.action_calllogs) {
            MesiboCallUi.getInstance().launchCallLogs(context, 0);
        } else if(item == R.id.action_menu_e2ee) {
            MesiboUI.showEndToEndEncryptionInfoForSelf(context);
        } else if(item == R.id.mesibo_share) {
            Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
            sharingIntent.setType("text/plain");
            sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, AppConfig.getConfig().invite.subject);
            sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, AppConfig.getConfig().invite.text);
            context.startActivity(Intent.createChooser(sharingIntent, AppConfig.getConfig().invite.title));
        }
    }

    void initUserListScreen(MesiboUI.MesiboUserListScreen screen) {
        ((Activity)screen.parent).getMenuInflater().inflate(R.menu.messaging_activity_menu, screen.menu);

        MenuItem.OnMenuItemClickListener menuhandler = new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                userListScreenMenuHandler(screen.parent, item.getItemId());
                return true;
            }
        };

        screen.menu.findItem(R.id.action_settings).setOnMenuItemClickListener(menuhandler);
        screen.menu.findItem(R.id.action_conf).setOnMenuItemClickListener(menuhandler);
        screen.menu.findItem(R.id.action_calllogs).setOnMenuItemClickListener(menuhandler);
        screen.menu.findItem(R.id.action_menu_e2ee).setOnMenuItemClickListener(menuhandler);
        screen.menu.findItem(R.id.mesibo_share).setOnMenuItemClickListener(menuhandler);

    }

    void messageListScreenMenuHandler(Context context, int item, MesiboProfile profile) {
        if(null == profile) {
            return;
        }

        if(R.id.action_call == item) {
            if(!MesiboCall.getInstance().callUi(context, profile, false))
                MesiboCall.getInstance().callUiForExistingCall(context);
        }
        else if(R.id.action_videocall == item) {
            if(!MesiboCall.getInstance().callUi(context, profile, true))
                MesiboCall.getInstance().callUiForExistingCall(context);
        }
        else if(R.id.action_e2e == item) {
            MesiboUI.showEndToEndEncryptionInfo(context, profile.getAddress(), profile.groupid);
        }
    }

    void initMessageListScreen(MesiboUI.MesiboMessageScreen screen) {
        ((Activity)screen.parent).getMenuInflater().inflate(R.menu.menu_messaging, screen.menu);

        MenuItem.OnMenuItemClickListener menuhandler = new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                messageListScreenMenuHandler(screen.parent, item.getItemId(), screen.profile);
                return true;
            }
        };

        screen.menu.findItem(R.id.action_call).setOnMenuItemClickListener(menuhandler);
        screen.menu.findItem(R.id.action_videocall).setOnMenuItemClickListener(menuhandler);
        screen.menu.findItem(R.id.action_e2e).setOnMenuItemClickListener(menuhandler);

        screen.titleArea.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                UIManager.launchUserProfile(screen.parent, screen.profile.groupid, screen.profile.address);
            }
        });
    }
}
