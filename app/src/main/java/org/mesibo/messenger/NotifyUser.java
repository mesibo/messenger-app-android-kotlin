/** Copyright (c) 2021 Mesibo
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

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import androidx.core.app.NotificationCompat;
import android.text.TextUtils;
import android.widget.RemoteViews;

import com.mesibo.api.Mesibo;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class NotifyUser {
    public static final int TYPE_MESSAGE=5;
    public static final int TYPE_OTHER=10;
    public static int mCount = 0;

    public static class NotificationContent {
        public String title = null;
        public String content = null;
        public String subContent = null;
        public RemoteViews v = null;
        public NotificationCompat.Style style = null;
    }

    private List<NotificationContent> mNotificationContentList = new ArrayList<NotificationContent>();
    private Context mContxt = null;
    private Uri mSoundUri = null;
    private String mPackageName = null;
    private TimerTask mTimerTask = null;
    private Timer mTimer = null;
    private Handler mUiHandler = new Handler(MainApplication.getAppContext().getMainLooper());
    private static NotificationChannel mChannel = null;


    public static String NOTIFYMESSAGE_CHANNEL_ID = "MesiboMessageNotificationChannel";
    private static final String NOTIFYMESSAGE_CHANNEL_NAME = "New Messages";

    public NotifyUser(Context context) {
        mContxt = context;
        mPackageName = context.getPackageName();
        createNotificationChannels();
        Mesibo.addListener(this);
    }

    private void createNotificationChannel(String id, String name, int importance) {
        if (Build.VERSION.SDK_INT < 26) {
            return;
        }

        NotificationChannel nchannel = new NotificationChannel(id, name, importance);

        nchannel.setDescription("None");
        nchannel.enableVibration(true);
        nchannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        nchannel.setShowBadge(true);

        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                .build();

        mSoundUri = RingtoneManager.getActualDefaultRingtoneUri(mContxt, RingtoneManager.TYPE_NOTIFICATION);
        nchannel.setSound(mSoundUri, audioAttributes);

        NotificationManager notificationManager =
                (NotificationManager) mContxt.getSystemService(Context.NOTIFICATION_SERVICE);

        notificationManager.createNotificationChannel(nchannel);
        mChannel = nchannel;
        NOTIFYMESSAGE_CHANNEL_ID = mChannel.getId();

    }

    private void createNotificationChannels() {
        createNotificationChannel(NOTIFYMESSAGE_CHANNEL_ID, NOTIFYMESSAGE_CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH);
    }

    public void sendNotification(int id, PendingIntent intent, NotificationContent n) {

        if(null == mChannel) createNotificationChannels();

        NotificationCompat.Builder builder = new NotificationCompat.Builder(mContxt, NOTIFYMESSAGE_CHANNEL_ID);

        builder.setPriority(NotificationCompat.PRIORITY_HIGH);
        builder.setCategory(NotificationCompat.CATEGORY_MESSAGE); //TBD, configurable

        if(null != n.title)
            builder.setContentTitle(n.title);
        if(null != n.content)
            builder.setContentText(n.content);

        if(!TextUtils.isEmpty(n.subContent))
            builder.setSubText(n.subContent);

        if(null != mSoundUri)
            builder.setSound(mSoundUri);

        builder.setWhen(System.currentTimeMillis());
        builder.setOngoing(true);

        builder.setSmallIcon(R.drawable.ic_message); //R.drawable.notify_transparent)

        builder.setContentIntent(intent);

        if(null != n.v) {
            builder.setCustomContentView(n.v);
        }

        if(null == n.style && null != n.content) {
        }

        if(null != n.style)
            builder.setStyle(n.style);


        builder.setAutoCancel(true);
        builder.setDefaults(Notification.DEFAULT_ALL);
        builder.setChannelId(NOTIFYMESSAGE_CHANNEL_ID);

        Notification notification = builder.build();

        NotificationManager notificationManager = (NotificationManager) mContxt.getSystemService(Context.NOTIFICATION_SERVICE);

        // Will display the notification in the notification bar
        notificationManager.notify(id, notification);
    }

    public synchronized void  clearNotification() {

        NotificationManager notificationManager = (NotificationManager) MainApplication.getAppContext().getSystemService(Context.NOTIFICATION_SERVICE);

        mNotificationContentList.clear();
        notificationManager.cancel(TYPE_MESSAGE);
        mCount = 0;
        mNotificationContentList.clear();
    }

    private PendingIntent getDefaultIntent(int code) {
        Intent intent = new Intent(mContxt, StartUpActivity.class);
        Bundle bundle = new Bundle();
        intent.putExtras(bundle);
        return PendingIntent.getActivity(mContxt, code, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }


    public void sendNotification(int id, String title, String content) {
        NotificationContent n = new NotificationContent();
        n.title = title;
        n.content = content;

        sendNotification(id, getDefaultIntent(0), n);
    }

    private synchronized void notifyMessages() {
        if(0 == mNotificationContentList.size())
            return;

        NotificationContent notify= mNotificationContentList.get(mNotificationContentList.size()-1);

        String title = "New Message from " + notify.title;
        NotificationCompat.InboxStyle inboxStyle = null;
        if(mNotificationContentList.size() > 1) {

            inboxStyle = new NotificationCompat.InboxStyle();
            Iterator iterator = mNotificationContentList.iterator();

            while(iterator.hasNext()) {
                NotificationContent n = (NotificationContent) iterator.next();
                inboxStyle.addLine(n.title + " : " + n.content);
            }

            inboxStyle.setBigContentTitle(title);
            inboxStyle.setSummaryText(mCount + " new messages");
            notify.style = inboxStyle;
            sendNotification(TYPE_MESSAGE, getDefaultIntent(0), notify);
            return;
        }

        sendNotification(TYPE_MESSAGE, title, notify.content);
    }

    public synchronized void sendNotificationInList(String title, String message) {

        NotificationContent notify = new NotificationContent();
        notify.title = title;
        notify.content = message;

        if(mNotificationContentList.size() >= 5)
            mNotificationContentList.remove(0);

        mNotificationContentList.add(notify);
        mCount++;

        notifyMessages();
        return;
    }
}
