/**
 * Copyright (c) 2019 Mesibo
 * https://mesibo.com
 * All rights reserved.
 *
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the terms and condition mentioned on https://mesibo.com
 * as well as following conditions are met:
 *
 *
 * Redistributions of source code must retain the above copyright notice, this list
 * of conditions, the following disclaimer and links to documentation and source code
 * repository.
 *
 *
 * Redistributions in binary form must reproduce the above copyright notice, this
 * list of conditions and the following disclaimer in the documentation and/or other
 * materials provided with the distribution.
 *
 *
 * Neither the name of Mesibo nor the names of its contributors may be used to endorse
 * or promote products derived from this software without specific prior written
 * permission.
 *
 *
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
 *
 * Documentation
 * https://mesibo.com/documentation/
 *
 *
 * Source Code Repository
 * https://github.com/mesibo/messengerKotlin-app-android
 */

package org.mesibo.messenger

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Chronometer
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView

import com.mesibo.api.Mesibo
import com.mesibo.api.MesiboUtils
import com.mesibo.calls.MesiboVideoCallFragment
import com.mesibo.messaging.MessagingActivity

import org.webrtc.RendererCommon.ScalingType

/**
 * Fragment for call control.
 */
class VideoCallFragmentLite : MesiboVideoCallFragment(), Mesibo.CallListener, View.OnTouchListener {

    private var cameraSwitchButton: ImageButton? = null
    private var videoScalingButton: ImageButton? = null
    private var toggleCameraButton: ImageButton? = null
    private var toggleMuteButton: ImageButton? = null
    private var toggleSpeakerButton: ImageButton? = null
    internal lateinit var imageView: ImageView
    internal lateinit var mDeclineViewButton: ImageView
    internal lateinit var mAcceptViewButton: ImageView
    internal lateinit var mDefaultMessageButton: ImageView
//    var callEvents: MesiboVideoCallFragment.OnCallEvents? = null
    var callEvents = null
    private var scalingType: ScalingType? = null
    internal lateinit var mProfile: Mesibo.UserProfile
    private var mIncomingView: View? = null
    private var mInProgressView: View? = null


    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val controlView = inflater.inflate(R.layout.fragment_videocall_new_lite, container, false)

        val contactView = controlView.findViewById<TextView>(R.id.call_name)
        imageView = controlView.findViewById(R.id.photo_image)
        val disconnectButton = controlView.findViewById<ImageButton>(R.id.button_call_disconnect)
        cameraSwitchButton = controlView.findViewById(R.id.button_call_switch_camera)
        videoScalingButton = controlView.findViewById(R.id.button_call_scaling_mode)
        toggleSpeakerButton = controlView.findViewById(R.id.button_call_toggle_speaker)
        toggleCameraButton = controlView.findViewById(R.id.button_call_toggle_camera)
        toggleMuteButton = controlView.findViewById(R.id.button_call_toggle_mic)
        mIncomingView = controlView.findViewById(R.id.incoming_call_container)
        mInProgressView = controlView.findViewById(R.id.outgoing_call_container)
        mDeclineViewButton = controlView.findViewById(R.id.declineButton)
        mAcceptViewButton = controlView.findViewById(R.id.accept_button)
        mDefaultMessageButton = controlView.findViewById(R.id.custom_message_button)

        // Add buttons click events.
        disconnectButton.setOnClickListener { hangup() }

        cameraSwitchButton!!.setOnClickListener { switchCamera() }

        videoScalingButton!!.setOnClickListener {
            if (scalingType == ScalingType.SCALE_ASPECT_FILL) {
                videoScalingButton!!.setBackgroundResource(R.drawable.ic_fullscreen_white_48dp)
                scalingType = ScalingType.SCALE_ASPECT_FIT
            } else {
                videoScalingButton!!.setBackgroundResource(R.drawable.ic_fullscreen_exit_white_48dp)
                scalingType = ScalingType.SCALE_ASPECT_FILL
            }
            ///callEvents.onVideoScalingSwitch(scalingType);
            scaleVideo(true)
        }
        scalingType = ScalingType.SCALE_ASPECT_FILL

        toggleSpeakerButton!!.setOnClickListener {
            toggleSpeaker()
            val enabled = callEvents?.onToggleSpeaker()
            toggleSpeakerButton!!.alpha = if (enabled.toString().toBoolean()) 1.0f else 0.3f
            callEvents!!.onToggleSpeaker()
        }

        toggleMuteButton!!.setOnClickListener {
            toggleMic()
            val enabled = callEvents!!.onToggleMic()
            toggleMuteButton!!.alpha = if (enabled.toString().toBoolean()) 1.0f else 0.3f
            callEvents!!.onToggleMic()
        }

        toggleCameraButton!!.setOnClickListener {
            toggleCamera()
            val enabled = callEvents!!.onToggleCamera()
            //setButton(toggleCameraButton, enabled);
            toggleCameraButton!!.alpha = if (enabled.toString().toBoolean()) 1.0f else 0.3f
            callEvents!!.onToggleCamera()
        }

        mDeclineViewButton.setOnClickListener { hangup() }

        mAcceptViewButton.setOnClickListener {
            answer(true)
            setDisplayMode()
        }

        mDefaultMessageButton.setOnClickListener {
            // setup the alert builder
            val builder = AlertDialog.Builder(activity)

            val DefaultTextMessages = arrayOf("Can't talk now. What's up?", "I'll call you right back.", "I'll call you later.", "Can't talk now. Call me later.", "Custom message...")
            builder.setItems(DefaultTextMessages) { dialog, which ->
                val peer = mProfile.address

                when (which) {
                    0 //
                    -> sendCustomeMessage(peer, "Can't talk now. What's up?")
                    1 //
                    -> sendCustomeMessage(peer, "I'll call you right back.")
                    2 //
                    -> sendCustomeMessage(peer, "I'll call you later.")
                    3 //
                    -> sendCustomeMessage(peer, "Can't talk now. Call me later.")
                    4 //Custom message
                    -> {
                        val i = Intent(activity, MessagingActivity::class.java)
                        i.putExtra("peer", peer)
                        startActivity(i)
                        hangup()
                    }
                }
            }

            val dialog_cust = builder.create()
            dialog_cust.show()
        }


        contactView.text = mProfile.name
        setUserPicture()
        val statusView = controlView.findViewById<View>(R.id.call_status) as Chronometer
        setStatusView(statusView)
        setDisplayMode()

        return controlView
    }


    private fun setDisplayMode() {

        val incoming = isIncoming && !isAnswered
        mIncomingView!!.visibility = if (incoming) View.VISIBLE else View.GONE
        mInProgressView!!.visibility = if (incoming) View.GONE else View.VISIBLE

    }


    fun setProfile(profile: Mesibo.UserProfile) {

        mProfile = profile

    }

    internal fun setUserPicture() {
        val filePath = Mesibo.getUserProfilePicturePath(mProfile, Mesibo.FileInfo.TYPE_AUTO)

        val b: Bitmap?
        if (Mesibo.fileExists(filePath)) {
            b = BitmapFactory.decodeFile(filePath)
            if (null != b) {
                imageView.setImageDrawable(MesiboUtils.getRoundImageDrawable(b))
            }
        } else {
            //TBD, getActivity.getresource crashes sometime if activity is closing
            imageView.setImageDrawable(MesiboUtils.getRoundImageDrawable(BitmapFactory.decodeResource(MainApplication.appContext?.getResources(), R.drawable.default_user_image)))
        }
    }


    private fun sendCustomeMessage(peer: String, message: String) {

        val messageParams = Mesibo.MessageParams()
        messageParams.setPeer(peer)
        Mesibo.sendMessage(messageParams, Mesibo.random(), message)
        hangup()

    }


    override fun onStart() {
        super.onStart()

        val captureSliderEnabled = false
        val args = arguments
        if (args != null) {
        }
        val videoCallEnabled = true
        if (!videoCallEnabled) {
            cameraSwitchButton!!.visibility = View.INVISIBLE
        }
    }

    override fun onResume() {
        super.onResume()
        Mesibo.addListener(this)
    }

    override fun onPause() {
        super.onPause()
        Mesibo.removeListener(this)
    }

    // TODO(sakal): Replace with onAttach(Context) once we only support API level 23+.
    override fun onAttach(activity: Activity?) {
        super.onAttach(activity)
//        callEvents = activity as MesiboVideoCallFragment.OnCallEvents?
    }

    override fun Mesibo_onCall(peerid: Long, callid: Long, userProfile: Mesibo.UserProfile, i: Int): Boolean {
        return false
    }

    override fun Mesibo_onCallStatus(peerid: Long, callid: Long, status: Int, flags: Int, desc: String?): Boolean {

        return false
    }

    override fun Mesibo_onCallServer(type: Int, url: String, username: String, credential: String) {

    }

    override fun onTouch(view: View, motionEvent: MotionEvent): Boolean {
        return false
    }
}

private fun Nothing.onToggleCamera() {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
}

private fun Nothing.onToggleMic() {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
}

private fun Nothing.onToggleSpeaker() {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
}
