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

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.support.v4.app.NotificationCompat
import android.text.TextUtils
import android.widget.RemoteViews

import com.mesibo.api.Mesibo

import java.util.ArrayList
import java.util.Timer
import java.util.TimerTask

class NotifyUser(context: Context) {

    private val mNotificationContentList = ArrayList<NotificationContent>()
    private var mContxt: Context? = null
    private var mPackageName: String? = null
    private var mTimerTask: TimerTask? = null
    private var mTimer: Timer? = null
    private val mUiHandler = Handler(MainApplication.appContext?.getMainLooper())

    private// This intent is fired when notification is clicked
    val defaultIntent: PendingIntent
        get() {
            val intent = Intent(mContxt, StartUpActivity::class.java)
            val bundle = Bundle()
            intent.putExtras(bundle)
            return PendingIntent.getActivity(mContxt, 0, intent, 0)
        }

    private val mNotifyRunnable = Runnable { notifyMessages() }

    class NotificationContent {
        var title: String? = null
        var content: String? = null
        var subContent: String? = null
        var v: RemoteViews? = null
        var style: NotificationCompat.Style? = null
    }

    init {
        mContxt = context
        mPackageName = context.packageName
        createNotificationChannels()
        Mesibo.addListener(this)
    }

    private fun createNotificationChannel(id: String, name: String, important: Int) {
        if (Build.VERSION.SDK_INT < 26) {
            return
        }

        val nchannel = NotificationChannel(id, name, important)

        // Sets whether notifications posted to this channel should display notification lights
        nchannel.enableLights(true)
        // Sets whether notification posted to this channel should vibrate.
        nchannel.enableVibration(true)
        // Sets the notification light color for notifications posted to this channel
        nchannel.lightColor = Color.GREEN
        // Sets whether notifications posted to this channel appear on the lockscreen or not
        nchannel.lockscreenVisibility = Notification.VISIBILITY_PRIVATE

        val notificationManager = mContxt!!.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        notificationManager.createNotificationChannel(nchannel)
    }

    private fun createNotificationChannels() {
        createNotificationChannel(NOTIFYMESSAGE_CHANNEL_ID, NOTIFYMESSAGE_CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT)
        createNotificationChannel(NOTIFYCALL_CHANNEL_ID, NOTIFYCALL_CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH)
    }

    fun sendNotification(channelId: String, id: Int, intent: PendingIntent, n: NotificationContent) {

        // Use NotificationCompat.Builder to set up our notification.
        val builder = NotificationCompat.Builder(mContxt!!, channelId)

        if (null != n.title)
            builder.setContentTitle(n.title)
        if (null != n.content)
            builder.setContentText(n.content)

        // The subtext, which appears under the text on newer devices.
        // This will show-up in the devices with Android 4.2 and above only
        if (!TextUtils.isEmpty(n.subContent))
            builder.setSubText(n.subContent)

        //icon appears in device notification bar and right hand corner of notification
        //https://clevertap.com/blog/fixing-notification-icon-for-android-lollipop-and-above/
        builder.setSmallIcon(R.drawable.notify_transparent) //R.drawable.ic_launcher

        // Set the intent that will fire when the user taps the notification.
        builder.setContentIntent(intent)

        // Large icon appears on the left of the notification
        builder.setLargeIcon(BitmapFactory.decodeResource(mContxt!!.resources, R.drawable.notify_large))

        if (null != n.v) {
            builder.setCustomContentView(n.v)
        }

        if (null == n.style && null != n.content)
            n.style = NotificationCompat.BigTextStyle().bigText(n.content)

        builder.setStyle(n.style)

        // clears on click
        builder.setAutoCancel(true)
        builder.setDefaults(Notification.DEFAULT_ALL)
        builder.priority = Notification.PRIORITY_MAX

        val notificationManager = mContxt!!.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Will display the notification in the notification bar
        notificationManager.notify(id, builder.build())
    }

    @Synchronized
    fun clearNotification() {

        val notificationManager = MainApplication.appContext?.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        mNotificationContentList.clear()
        // Will display the notification in the notification bar
        notificationManager.cancel(TYPE_MESSAGE)
        mCount = 0
        mNotificationContentList.clear()
        //notificationManager.cancel(TYPE_OTHER);
    }


    fun sendNotification(channelid: String, id: Int, title: String, content: String?) {
        val n = NotificationContent()
        n.title = title
        n.content = content

        sendNotification(channelid, id, defaultIntent, n)
    }

    @Synchronized
    private fun notifyMessages() {
        if (0 == mNotificationContentList.size)
            return

        val notify = mNotificationContentList[mNotificationContentList.size - 1]

        val title = "New Message from " + notify.title!!
        var inboxStyle: NotificationCompat.InboxStyle? = null
        if (mNotificationContentList.size > 1) {

            inboxStyle = NotificationCompat.InboxStyle()
            val iterator = mNotificationContentList.iterator()

            while (iterator.hasNext()) {
                val n = iterator.next()
                inboxStyle.addLine(n.title + " : " + n.content)
            }

            inboxStyle.setBigContentTitle(title)
            inboxStyle.setSummaryText("$mCount new messages")
            notify.style = inboxStyle
            sendNotification(NotifyUser.NOTIFYMESSAGE_CHANNEL_ID, TYPE_MESSAGE, defaultIntent, notify)
            return
        }

        // Don't use notify object else we it will overwrite title
        sendNotification(NotifyUser.NOTIFYMESSAGE_CHANNEL_ID, TYPE_MESSAGE, title, notify.content)
    }

    @Synchronized
    fun sendNotificationInList(title: String, message: String) {


        // it is also possible to limit only latest notification from a user by adding params and checking for duplicate
        val notify = NotificationContent()
        notify.title = title
        notify.content = message

        // inboxStyle can only have max 5 messages
        if (mNotificationContentList.size >= 5)
            mNotificationContentList.removeAt(0)

        mNotificationContentList.add(notify)
        mCount++

        // if more realtime messages in thread queue, just add into our list and return
        // TBD, this may have adverse effect if next realtime message is the one we reading

        //if(Mesibo.isMoreRealtimeMessages(params.ts))
        //  return;

        if (null != mTimer) {
            mTimer!!.cancel()
            mTimer = null
        }

        mTimer = Timer()
        mTimerTask = object : TimerTask() {
            override fun run() {
                mUiHandler.post(mNotifyRunnable)
            }
        }

        mTimer!!.schedule(mTimerTask, 1000)

        return
    }

    companion object {
        val TYPE_MESSAGE = 0
        val TYPE_OTHER = 0
        var mCount = 0

        val NOTIFYMESSAGE_CHANNEL_ID = "MESSAGE_CHANNEL"
        private val NOTIFYMESSAGE_CHANNEL_NAME = "Messages"

        val NOTIFYCALL_CHANNEL_ID = "CALL_CHANNEL"
        private val NOTIFYCALL_CHANNEL_NAME = "Calls"
    }
}
