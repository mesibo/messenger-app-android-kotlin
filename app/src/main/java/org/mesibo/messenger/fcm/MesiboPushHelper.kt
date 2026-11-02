package org.mesibo.messenger.fcm

import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import com.mesibo.api.Mesibo

/**
 * All push (FCM) handling of the app:
 *
 * - fetches the FCM token and gives it to mesibo (Mesibo.setPushToken), again whenever FCM
 *   rotates it (MesiboFcmService.onNewToken)
 * - a push only wakes the app so that mesibo connects and receives pending messages; the
 *   payload is not used (MesiboFcmService.onMessageReceived)
 *
 * The sender id comes from google-services.json.
 */
object MesiboPushHelper {
    private const val TAG = "MesiboPushHelper"

    // FCM allows only a short execution window per message
    private const val PUSH_WAIT_ONLINE_MS: Long = 15000

    private var mToken: String? = null
    private var mTokenSent = false

    /** fetches the token; call when mesibo starts */
    @JvmStatic
    fun start() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.d(TAG, "FCM token fetch failed", task.exception)
                return@addOnCompleteListener
            }
            setToken(task.result)
        }
    }

    /** sends the token to mesibo if not sent yet, e.g. once online */
    @JvmStatic
    @Synchronized
    fun sendToken() {
        val token = mToken
        if (null == token || mTokenSent) return
        mTokenSent = true
        Mesibo.setPushToken(token)
    }

    /** logout: the token is to be sent again for the next user */
    @JvmStatic
    @Synchronized
    fun reset() {
        mTokenSent = false
    }

    @Synchronized
    internal fun setToken(token: String?) {
        if (null == token) return
        // FCM may rotate the token; a new token must be sent again
        if (token != mToken) mTokenSent = false
        mToken = token
        sendToken()
    }

    /**
     * a push arrived, on the FCM service thread: wake mesibo, wait (bounded) until it is online and
     * give it a moment to receive the messages
     */
    internal fun onMessage() {
        Mesibo.setForegroundContext(null, -1, true)

        val deadline = System.currentTimeMillis() + PUSH_WAIT_ONLINE_MS
        try {
            while (Mesibo.STATUS_ONLINE != Mesibo.getConnectionStatus()
                && System.currentTimeMillis() < deadline
            ) {
                Thread.sleep(500)
            }
            Thread.sleep(1000)
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
        }
    }
}
