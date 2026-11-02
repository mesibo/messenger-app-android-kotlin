package org.mesibo.messenger

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.os.Build
import android.provider.Settings
import android.text.TextUtils
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat

class NotifyUser(private val mContxt: Context) {
    class NotificationContent {
        var title: String? = null
        var content: String? = null
        var subContent: String? = null
        var v: RemoteViews? = null
        var style: NotificationCompat.Style? = null
    }

    private val mNotificationContentList: MutableList<NotificationContent> = ArrayList()

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < 26) return

        val channel = NotificationChannel(
            NOTIFYMESSAGE_CHANNEL_ID,
            NOTIFYMESSAGE_CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH
        )
        channel.description = "Notifications for new messages"
        channel.enableVibration(true)
        channel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        channel.setShowBadge(true)

        val audioAttributes = AudioAttributes.Builder()
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .build()
        channel.setSound(Settings.System.DEFAULT_NOTIFICATION_URI, audioAttributes)

        val nm = mContxt.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.deleteNotificationChannel(OLD_CHANNEL_ID)
        nm.createNotificationChannel(channel)
    }

    fun sendNotification(id: Int, intent: PendingIntent?, n: NotificationContent) {
        // Use NotificationCompat.Builder to set up our notification.

        val builder = NotificationCompat.Builder(mContxt, NOTIFYMESSAGE_CHANNEL_ID)

        builder.setPriority(NotificationCompat.PRIORITY_HIGH)
        builder.setCategory(NotificationCompat.CATEGORY_MESSAGE) //TBD, configurable

        if (null != n.title) builder.setContentTitle(n.title)
        if (null != n.content) builder.setContentText(n.content)

        // The subtext, which appears under the text on newer devices.
        // This will show-up in the devices with Android 4.2 and above only
        if (!TextUtils.isEmpty(n.subContent)) builder.setSubText(n.subContent)

        builder.setWhen(System.currentTimeMillis())

        // message notifications must be dismissible (was ongoing/sticky)
        builder.setOngoing(false)

        //icon appears in device notification bar and right hand corner of notification
        //https://clevertap.com/blog/fixing-notification-icon-for-android-lollipop-and-above/
        builder.setSmallIcon(R.drawable.ic_message_white_24dp)

        // Set the intent that will fire when the user taps the notification.
        builder.setContentIntent(intent)

        //builder.setFullScreenIntent(intent, true);

        // Large icon appears on the left of the notification
        //builder.setLargeIcon(BitmapFactory.decodeResource(mContxt.getResources(), R.drawable.notify_large));
        if (null != n.v) {
            builder.setCustomContentView(n.v)
        }

        if (null != n.style) builder.setStyle(n.style)


        // clears on click
        builder.setAutoCancel(true)
        // sound/vibration on Android 6-7; on 8+ the channel decides
        builder.setDefaults(Notification.DEFAULT_ALL)

        val notification = builder.build()

        val notificationManager =
            mContxt.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Will display the notification in the notification bar
        notificationManager.notify(id, notification)
    }

    @Synchronized
    fun clearNotification() {
        val notificationManager = MainApplication.appContext.getSystemService(
            Context.NOTIFICATION_SERVICE
        ) as NotificationManager

        mNotificationContentList.clear()
        // Will display the notification in the notification bar
        notificationManager.cancel(TYPE_MESSAGE)
        mCount = 0
        mNotificationContentList.clear()
        //notificationManager.cancel(TYPE_OTHER);
    }

    private fun getDefaultIntent(code: Int): PendingIntent {
        // This intent is fired when notification is clicked
        val intent = Intent(mContxt, StartUpActivity::class.java)
        val immutable = if (Build.VERSION.SDK_INT >= 23) PendingIntent.FLAG_IMMUTABLE else 0
        return PendingIntent.getActivity(
            mContxt,
            code,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or immutable
        )
    }


    fun sendNotification(id: Int, title: String?, content: String?) {
        val n = NotificationContent()
        n.title = title
        n.content = content

        sendNotification(id, getDefaultIntent(0), n)
    }

    @Synchronized
    private fun notifyMessages() {
        if (0 == mNotificationContentList.size) return

        val notify = mNotificationContentList[mNotificationContentList.size - 1]

        val title = "New Message from " + notify.title
        var inboxStyle: NotificationCompat.InboxStyle? = null
        if (mNotificationContentList.size > 1) {
            inboxStyle = NotificationCompat.InboxStyle()
            for (n in mNotificationContentList) inboxStyle.addLine(n.title + " : " + n.content)

            inboxStyle.setBigContentTitle(title)
            inboxStyle.setSummaryText(mCount.toString() + " new messages")
            notify.style = inboxStyle
            sendNotification(TYPE_MESSAGE, getDefaultIntent(0), notify)
            return
        }

        // Don't use notify object else we it will overwrite title
        sendNotification(TYPE_MESSAGE, title, notify.content)
    }

    @Synchronized
    fun sendNotificationInList(title: String?, message: String?) {
        // it is also possible to limit only latest notification from a user by adding params and checking for duplicate

        val notify = NotificationContent()
        notify.title = title
        notify.content = message

        // inboxStyle can only have max 5 messages
        if (mNotificationContentList.size >= 5) mNotificationContentList.removeAt(0)

        mNotificationContentList.add(notify)
        mCount++

        notifyMessages()
        return
    }

    companion object {
        const val ID_GROUP_JOINED: Int = 3
        const val ID_GROUP_LEFT: Int = 4
        const val TYPE_MESSAGE: Int = 5
        const val TYPE_OTHER: Int = 10
        var mCount: Int = 0

        // Channel settings are immutable once created; the id was changed so that the corrected
        // sound attributes apply to existing installs as well. The old channel is deleted.
        const val NOTIFYMESSAGE_CHANNEL_ID: String = "MesiboMessages"
        private const val OLD_CHANNEL_ID = "MesiboAppNotificationChannel"
        private const val NOTIFYMESSAGE_CHANNEL_NAME = "New Messages"
    }
}
