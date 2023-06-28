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

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.google.gson.Gson;

import com.mesibo.api.MesiboGroupProfile;
import com.mesibo.api.MesiboMessage;
import com.mesibo.api.MesiboProfile;
import com.mesibo.calls.api.MesiboCall;
import com.mesibo.calls.ui.MesiboCallUi;
import com.mesibo.contactutils.*;

import com.mesibo.api.Mesibo;
import org.mesibo.messenger.fcm.MesiboRegistrationIntentService;

import com.mesibo.messaging.MesiboUI;
import com.mesibo.uihelper.WelcomeScreen;
import com.mesibo.uihelper.ILoginInterface;
import com.mesibo.uihelper.IProductTourListener;
import com.mesibo.uihelper.ILoginResultsInterface;

import java.util.ArrayList;
import static org.webrtc.ContextUtils.getApplicationContext;

public class MesiboListeners implements Mesibo.ConnectionListener, ILoginInterface, IProductTourListener, Mesibo.MessageListener, ContactUtils.ContactsListener, Mesibo.MessageFilter, Mesibo.ProfileListener, Mesibo.CrashListener, MesiboRegistrationIntentService.GCMListener, MesiboCall.IncomingListener, Mesibo.GroupListener, Mesibo.AppStateListener, Mesibo.EndToEndEncryptionListener {
    public static final String TAG = "MesiboListeners";
    public static Context mLoginContext = null;
    private static Gson mGson = new Gson();
    public static class MesiboNotification {
        public String subject;
        public String msg;
        public String type;
        public String action;
        public String name;
        public long gid;
        public String phone;
        public String status;
        public String members;
        public String photo;
        public long ts;
        public String tn;

        MesiboNotification() {
        }
    }

    ILoginResultsInterface mILoginResultsInterface = null;
    Handler mGroupHandler = null;
    String mCode = null;
    String mPhone = null;
    boolean mSyncDone = false;
    Context mUserListContext = null;
    Context mMessageContext = null;
    Context mLastContext = null;

    @SuppressWarnings("all")
    private SampleAPI.ResponseHandler mHandler = new SampleAPI.ResponseHandler() {
        @Override
        public void HandleAPIResponse(SampleAPI.Response response) {
            Log.d(TAG, "Respose: " + response);
            if (null == response)
                return;

            if (response.op.equals("login")) {
                if (!TextUtils.isEmpty(SampleAPI.getToken())) {
                    MesiboProfile u = Mesibo.getSelfProfile();

                    if (TextUtils.isEmpty(u.getName())) {
                        UIManager.launchEditProfile(mLoginContext, 0, 0, true);
                    } else {
                        MesiboUI.MesiboUserListScreenOptions opts = new MesiboUI.MesiboUserListScreenOptions();
                        opts.keepRunning = true;
                        UIManager.launchMesibo(mLoginContext, opts);
                    }
                }

                if(null != mILoginResultsInterface && null == response.errmsg)
                    mILoginResultsInterface.onLoginResult(response.result.equals("OK"), -1);

            } else if (response.op.equals("setgroup")) {

                if(null != mGroupHandler) {
                    Message msg = new Message();
                    Bundle bundle = new Bundle();
                    bundle.putLong("groupid", response.gid);
                    bundle.putString("result", response.result);
                    msg.setData(bundle);
                    mGroupHandler.handleMessage(msg);
                }
            } else if (response.op.equals("getgroup")) {

                if(null != mGroupHandler) {
                    Message msg = new Message();
                    Bundle bundle = new Bundle();
                    bundle.putString("result", response.result);
                    msg.setData(bundle);
                    mGroupHandler.handleMessage(msg);
                }
            }
            //handleAPIResponse(response);
        }
    };

    @Override
    public void Mesibo_onConnectionStatus(int status) {
        Log.d(TAG, "Mesibo_onConnectionStatus: " + status);
        if (Mesibo.STATUS_SIGNOUT == status) {
            UIManager.showAlert(mUserListContext, "Signed Out", "You have signed-in from other device and hence signed out here");
            SampleAPI.forceLogout();
        } else if (Mesibo.STATUS_AUTHFAIL == status) {
            UIManager.showAlert(mUserListContext, "Signed Out", "Login Expired. Login again to continue.");
            SampleAPI.forceLogout();
        }

        if(Mesibo.STATUS_ONLINE == status) {
            SampleAPI.startOnlineAction();
        }
    }

    @Override
    public void Mesibo_onEndToEndEncryption(MesiboProfile profile, int status) {
        Log.d(TAG, "Mesibo_onEndToEndEncryption: " + status);
    }

    @Override
    public void Mesibo_onMessage(MesiboMessage msg) {
        if(!msg.isRealtimeMessage() || Mesibo.MSGSTATUS_OUTBOX == msg.getStatus())
            return;

        if(msg.isEndToEndEncryptionStatus())
            return;

        // if(Mesibo.isAppInForeground()) return true;

        if(Mesibo.isReading(msg))
            return;

        String message = msg.message;
        if(TextUtils.isEmpty(message))
            message = msg.title;
        if(TextUtils.isEmpty(message) && msg.hasImage())
            message = "Picture";
        if(TextUtils.isEmpty(message) && msg.hasVideo())
            message = "Video";
        if(TextUtils.isEmpty(message) && msg.hasAudio())
            message = "Audio";
        if(TextUtils.isEmpty(message) && msg.hasDocument())
            message = "Attachment";
        if(TextUtils.isEmpty(message) && msg.hasLocation())
            message = "Location";

        SampleAPI.notify(msg, message);
        return;
    }

    @Override
    public void Mesibo_onMessageStatus(MesiboMessage params) {
    }

    @Override
    public void Mesibo_onMessageUpdate(MesiboMessage mesiboMessage) {

    }

    @Override
    public void Mesibo_onProfileUpdated(MesiboProfile userProfile) {

    }

    @Override
    public boolean Mesibo_onGetProfile(MesiboProfile profile) {
        if(null == profile) {
            return false;
        }

        if(!profile.isActive()) {
            if(profile.groupid > 0) {
                profile.lookedup = true; //else getProfile will be recursive call
                //SampleAPI.updateDeletedGroup(profile.groupid);
                return true;
            }
        }

        if(profile.groupid > 0) {
            return true;
        }

        if(!TextUtils.isEmpty(profile.address)) {
            long ts = Mesibo.getTimestamp();
            String name = ContactUtils.reverseLookup(profile.address);
            if(null == name) {
                return mSyncDone;
            }

            profile.setOverrideName(name);
            return true;
        }

        return false;
    }

    //Note this is not in UI thread
    @Override
    public boolean Mesibo_onMessageFilter(MesiboMessage msg) {

        // using it for notifications
        if(1 != msg.type || msg.isCall())
            return true;

        return false;
    }

    @Override
    public MesiboCall.CallProperties MesiboCall_OnIncoming(MesiboProfile userProfile, boolean video) {
        MesiboCall.CallProperties cc = MesiboCall.getInstance().createCallProperties(video);
        cc.parent = getApplicationContext();
        cc.user = userProfile;
        return cc;
    }

    @Override
    public boolean MesiboCall_OnShowUserInterface(MesiboCall.Call call, MesiboCall.CallProperties callProperties) {
        return false;
    }

    @Override
    public void MesiboCall_OnError(MesiboCall.CallProperties callProperties, int error) {
    }

    @Override
    public boolean MesiboCall_onNotify(int type, MesiboProfile profile, boolean video) {
        String subject = null, message = null;

        if(true)
            return false;

        if(MesiboCall.MESIBOCALL_NOTIFY_INCOMING == type) {

        } else if(MesiboCall.MESIBOCALL_NOTIFY_MISSED == type) {
            subject = "Mesibo Missed Call";
            message = "You missed a mesibo " + (video?"video ":"") + "call from " + profile.getNameOrAddress("+");

        }


        return true;
    }

    @Override
    public void Mesibo_onGroupCreated(MesiboProfile groupProfile) {
        Log.d(TAG, "New group " + groupProfile.groupid);
    }

    @Override
    public void Mesibo_onGroupJoined(MesiboProfile groupProfile) {
        SampleAPI.notify(3, "Joined a group", "You have been added to the group " + groupProfile.getName());
    }

    @Override
    public void Mesibo_onGroupLeft(MesiboProfile groupProfile) {
        SampleAPI.notify(3, "Left a group", "You left the group " + groupProfile.getName());
    }

    @Override
    public void Mesibo_onGroupMembers(MesiboProfile groupProfile, MesiboGroupProfile.Member[] members) {

    }

    @Override
    public void Mesibo_onGroupMembersJoined(MesiboProfile groupProfile, MesiboGroupProfile.Member[] members) {

    }

    @Override
    public void Mesibo_onGroupMembersRemoved(MesiboProfile groupProfile, MesiboGroupProfile.Member[] members) {

    }

    @Override
    public void Mesibo_onGroupSettings(MesiboProfile mesiboProfile, MesiboGroupProfile.GroupSettings groupSettings, MesiboGroupProfile.MemberPermissions memberPermissions, MesiboGroupProfile.GroupPin[] groupPins) {

    }

    @Override
    public void Mesibo_onGroupError(MesiboProfile mesiboProfile, long l) {

    }

    @Override
    public void Mesibo_onForeground(boolean foreground) {
        if(foreground && MesiboCall.getInstance().isCallInProgress() && null != mLastContext) {
            MesiboCall.getInstance().callUiForExistingCall(mLastContext);
        }
    }

    @Override
    public void Mesibo_onForeground(Context context, int screenId, boolean foreground) {


        //userlist is in foreground
        if(foreground && 0 == screenId) {
            //notify count clear
            SampleAPI.notifyClear();
        }

        // if app restarted
        if(foreground && MesiboCall.getInstance().isCallInProgress()  && null == mLastContext) {
            MesiboCall.getInstance().callUiForExistingCall(context);
        }

        if(foreground) mLastContext= context;
    }

    @Override
    public void Mesibo_onCrash(String crashLogs) {
        Log.e(TAG, "Mesibo_onCrash: " + ((null != crashLogs)?crashLogs:""));
        //restart application
        Intent i = new Intent(MainApplication.getAppContext(), StartUpActivity.class);  //MyActivity can be anything which you want to start on bootup...
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        i.putExtra(StartUpActivity.STARTINBACKGROUND, !Mesibo.isAppInForeground()); ////Maintain the state of the application
        MainApplication.getAppContext().startActivity(i);
    }

    @Override
    public void onProductTourViewLoaded(View v, int index, WelcomeScreen screen) {

    }

    @Override
    public void onProductTourCompleted(Context context) {
        UIManager.launchLogin((Activity)context, MesiboListeners.getInstance());
    }

    @Override
    public boolean onLogin(Context context, String phone, String code, ILoginResultsInterface iLoginResultsInterface) {
        mLoginContext = context;
        mILoginResultsInterface = iLoginResultsInterface;
        mCode = code;
        mPhone = phone;
        mHandler.setContext(context);
        SampleAPI.login(phone, code, mHandler);
        return false;
    }

    private static MesiboListeners _instance = null;
    public static MesiboListeners getInstance() {
        if(null==_instance)
            synchronized(MesiboListeners.class) {
                if(null == _instance) {
                    _instance = new MesiboListeners();
                }
            }

        return _instance;
    }


    private static ArrayList<String> mContactsToSync = new ArrayList<String>();
    private static ArrayList<String> mDeletedContacts = new ArrayList<String>();
    private long mSyncTs = 0;
    @Override
    public boolean ContactUtils_onContact(String[] phoneNumbers, boolean deleted, String contacts, long ts) {
        mSyncDone = true;
        Mesibo.updateLookups();
        if(null == phoneNumbers) return true;

        int maxcount = 100; // we are limiting count to conserve memory
        String[] phones = new String[maxcount];
        for(int i=0; i < phoneNumbers.length; ) {
            if((phoneNumbers.length - i) < maxcount) {
                maxcount = (phoneNumbers.length - i);
                phones = new String[maxcount];
            }

            for(int j=0; j < maxcount; j++)
                phones[j] = phoneNumbers[i++];


            Mesibo.syncContacts(phones, !deleted, true, 0, false);
        }

        if(phoneNumbers.length > 0)  Mesibo.syncContacts();

        if(!deleted)  SampleAPI.saveLocalSyncedContacts(contacts, ts);
        return true;
    }


    @Override
    public void Mesibo_onGCMToken(String token) {
        SampleAPI.setGCMToken(token);
    }

    @Override
    public void Mesibo_onGCMMessage(/*Bundle data, */boolean inService) {
        SampleAPI.onGCMMessage(inService);
    }
}
