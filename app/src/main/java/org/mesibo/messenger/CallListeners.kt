package org.mesibo.messenger

import com.mesibo.api.Mesibo
import com.mesibo.api.MesiboDateTime
import com.mesibo.api.MesiboProfile
import com.mesibo.calls.api.MesiboCall
import com.mesibo.calls.api.MesiboCall.CallMonitorListener
import com.mesibo.calls.api.MesiboCall.CallProperties
import com.mesibo.calls.api.MesiboCall.IncomingListener
import com.mesibo.messaging.MesiboUI

/**
 * All call listeners of the app:
 *
 * IncomingListener: incoming calls (call properties), missed call notifications.
 * CallMonitorListener: calls starting and ending - updates the chat list and chats, which show
 * the call in progress button during a call (UIListener.updateCallButtons).
 */
class CallListeners : IncomingListener, CallMonitorListener {
    // -- IncomingListener --
    override fun MesiboCall_OnIncoming(
        userProfile: MesiboProfile,
        video: Boolean,
        waiting: Boolean
    ): CallProperties? {
        val cc = MesiboCall.getInstance().createCallProperties(video)
        cc.parent = MainApplication.appContext
        cc.user = userProfile
        return cc
    }

    override fun MesiboCall_OnShowUserInterface(
        call: MesiboCall.Call,
        callProperties: CallProperties
    ): Boolean {
        return false
    }

    override fun MesiboCall_OnError(callProperties: CallProperties, error: Int) {
    }

    override fun MesiboCall_onNotify(
        type: Int,
        profile: MesiboProfile,
        video: Boolean,
        ts: MesiboDateTime
    ): Boolean {
        if (MesiboCall.MESIBOCALL_NOTIFY_MISSED == type) {
            val subject = "Mesibo Missed Call"
            val message =
                "You missed a mesibo " + (if (video) "video " else "") + "call from " + profile.nameOrAddress
            SampleAPI.notify(Mesibo.getTimestamp().toInt(), subject, message)
        }
        return true
    }

    // -- CallMonitorListener --
    override fun MesiboCall_onCallStarted(
        profile: MesiboProfile,
        incoming: Boolean,
        video: Boolean
    ) {
        MesiboUI.updateScreens()
    }

    override fun MesiboCall_onCallEnded(profile: MesiboProfile, incoming: Boolean, video: Boolean) {
        MesiboUI.updateScreens()
    }

    companion object {
        private var sInstance: CallListeners? = null

        @get:Synchronized
        val instance: CallListeners?
            get() {
                if (null == sInstance) sInstance = CallListeners()
                return sInstance
            }

        /** registers all call listeners; once MesiboCall is initialized  */
        fun register() {
            MesiboCall.getInstance().setListener(instance)
            MesiboCall.getInstance().addCallMonitorListener(instance)
        }
    }
}
