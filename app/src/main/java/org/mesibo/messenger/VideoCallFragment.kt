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


import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.Chronometer
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView

import com.mesibo.api.Mesibo
import com.mesibo.api.MesiboUtils
import com.mesibo.calls.MesiboVideoCallFragment
import com.mesibo.messaging.MessagingActivity

import org.webrtc.RendererCommon.ScalingType

/**
 * Fragment for call control.
 */
class VideoCallFragment : MesiboVideoCallFragment(), Mesibo.CallListener, View.OnTouchListener {

    private var mUpArrowDecline: LinearLayout? = null
    private var mUpArrowAccept: LinearLayout? = null
    private var mUpArrowMessage: LinearLayout? = null
    private var cameraSwitchButton: ImageButton? = null
    private var videoScalingButton: ImageButton? = null
    private var toggleCameraButton: ImageButton? = null
    private var toggleMuteButton: ImageButton? = null
    private var toggleSpeakerButton: ImageButton? = null
    internal lateinit var imageView: ImageView
//    private var callEvents: MesiboVideoCallFragment.OnCallEvents? = null
    private var mSwipeTextDecline: TextView? = null
    private var mSwipeTextAccept: TextView? = null
    private var mSwipeTextMessage: TextView? = null
    internal lateinit var shake: Animation
    private var scalingType: ScalingType? = null
    internal var firstTouch = false
    internal var dX: Float = 0.toFloat()
    internal var dY: Float = 0.toFloat()
    internal var m_screen_width: Float = 0.toFloat()
    internal var m_screen_height: Float = 0.toFloat()
    internal var m_VcentreX: Float = 0.toFloat()
    internal var m_VcentreY: Float = 0.toFloat()
    internal var m_ViewStartingY: Float = 0.toFloat()
    internal lateinit var mProfile: Mesibo.UserProfile
    private var mIncomingView: View? = null
    private var mInProgressView: View? = null
    private val mIncomingAudioAcceptLayout: View? = null


    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val controlView = inflater.inflate(R.layout.fragment_videocall_new, container, false)

        // Create UI controls.

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
        val mDeclineViewButton = controlView.findViewById<RelativeLayout>(R.id.decline_view_layout)
        val mAcceptViewButton = controlView.findViewById<RelativeLayout>(R.id.accept_view_layout)
        val mDefaultMessageButton = controlView.findViewById<RelativeLayout>(R.id.custom_message_view_layout)
        mSwipeTextDecline = controlView.findViewById(R.id.decline_swipe_tv)
        mSwipeTextAccept = controlView.findViewById(R.id.accept_swipe_tv)
        mSwipeTextMessage = controlView.findViewById(R.id.message_swipe_tv)
        mUpArrowDecline = controlView.findViewById(R.id.decline_up_arrow)
        mUpArrowAccept = controlView.findViewById(R.id.accept_up_arrow)
        mUpArrowMessage = controlView.findViewById(R.id.message_up_arrow)

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
            val enabled = callEvents!!.onToggleSpeaker()
            toggleSpeakerButton!!.alpha = if (enabled) 1.0f else 0.3f
            callEvents!!.onToggleSpeaker()
        }

        toggleMuteButton!!.setOnClickListener {
            toggleMic()
            val enabled = callEvents!!.onToggleMic()
            toggleMuteButton!!.alpha = if (enabled) 1.0f else 0.3f
            callEvents!!.onToggleMic()
        }

        toggleCameraButton!!.setOnClickListener {
            toggleCamera()
            val enabled = callEvents!!.onToggleCamera()
            //setButton(toggleCameraButton, enabled);
            toggleCameraButton!!.alpha = if (enabled) 1.0f else 0.3f
            callEvents!!.onToggleCamera()
        }

        contactView.text = mProfile.name
        setUserPicture()

        //CallManager.CallUserInterface ui = mCall.ui.get();
        val statusView = controlView.findViewById<View>(R.id.call_status) as Chronometer
        setStatusView(statusView)
        setDisplayMode()

        ///to know the size of screen
        val mRootLayout = controlView.findViewById<RelativeLayout>(R.id.rootView)

        val vto = mRootLayout.viewTreeObserver
        vto.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
                    mRootLayout.viewTreeObserver.removeGlobalOnLayoutListener(this)
                } else {
                    mRootLayout.viewTreeObserver.removeOnGlobalLayoutListener(this)
                }
                m_screen_width = mRootLayout.measuredWidth.toFloat()
                m_screen_height = mRootLayout.measuredHeight.toFloat()

            }
        })

        mAcceptViewButton.setOnTouchListener(MyTouchListener())
        mDeclineViewButton.setOnTouchListener(MyTouchListener())
        mDefaultMessageButton.setOnTouchListener(MyTouchListener())

        //start shake animation
        shake = AnimationUtils.loadAnimation(activity, R.anim.shake_wibble)
        mAcceptViewButton.startAnimation(shake)

        //Start Animation
        MoveUpAnimation(mUpArrowAccept)
        MoveUpAnimation(mUpArrowDecline)
        MoveUpAnimation(mUpArrowMessage)
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

    private fun MoveUpAnimation(v: View?) {
        val animation = ObjectAnimator.ofFloat(v, "translationY", 100f)
        animation.duration = 1000
        animation.repeatCount = ValueAnimator.INFINITE
        animation.repeatMode = ValueAnimator.REVERSE
        animation.start()
    }


    private inner class MyTouchListener : View.OnTouchListener {
        override fun onTouch(view: View, motionEvent: MotionEvent): Boolean {

            var x_cord = 0f
            val y_cord: Float
            val triggerRangeLimit: Float
            val triggerRangeDownValue: Float
            var ItemClicked = 0


            if (isAdded && activity != null) {


                //initial coordinates of the selected view
                x_cord = view.x
                y_cord = view.y

                // coordinates of view keeps changing on motion, save it in starting and animate view to statrting position of tge view after release
                if (!firstTouch) {

                    m_VcentreX = view.x
                    m_VcentreY = view.y
                    firstTouch = true
                    m_ViewStartingY = motionEvent.rawY


                }


                triggerRangeLimit = m_ViewStartingY - m_screen_height / 5 //based on screen sizes , range of the button action should be decided, here we are taking 5th part of the screen height
                triggerRangeDownValue = (m_ViewStartingY - m_screen_height / 4.5).toFloat()// lower range value action


                //ItemClicked is to know which button(Decline/Accept/Message) is pressed
                if (view is RelativeLayout) {
                    if (view.getId() == R.id.decline_view_layout) {
                        ItemClicked = 1  //Decline

                    } else if (view.getId() == R.id.custom_message_view_layout) {

                        ItemClicked = 3  //Message

                    } else {
                        ItemClicked = 2  //Accept
                        view.clearAnimation()// stop shaky animation on Accept button when button is pressed
                    }
                }


                when (motionEvent.action) {

                    MotionEvent.ACTION_DOWN //when view is pressed
                    -> {

                        //                        dX = x_cord - motionEvent.getRawX();
                        dY = y_cord - motionEvent.rawY


                        if (ItemClicked == 1) {// Decline
                            mSwipeTextDecline!!.visibility = View.VISIBLE
                            mSwipeTextAccept!!.visibility = View.INVISIBLE
                            mSwipeTextMessage!!.visibility = View.INVISIBLE
                            mUpArrowDecline!!.visibility = View.VISIBLE
                            mUpArrowAccept!!.visibility = View.INVISIBLE
                            mUpArrowMessage!!.visibility = View.INVISIBLE
                        } else if (ItemClicked == 3) {//Message
                            mSwipeTextDecline!!.visibility = View.INVISIBLE
                            mSwipeTextAccept!!.visibility = View.INVISIBLE
                            mSwipeTextMessage!!.visibility = View.VISIBLE
                            mUpArrowDecline!!.visibility = View.INVISIBLE
                            mUpArrowAccept!!.visibility = View.INVISIBLE
                            mUpArrowMessage!!.visibility = View.VISIBLE
                        } else {//accept
                            mSwipeTextDecline!!.visibility = View.INVISIBLE
                            mSwipeTextAccept!!.visibility = View.VISIBLE
                            mSwipeTextMessage!!.visibility = View.INVISIBLE
                            mUpArrowDecline!!.visibility = View.INVISIBLE
                            mUpArrowAccept!!.visibility = View.VISIBLE
                            mUpArrowMessage!!.visibility = View.INVISIBLE
                        }
                    }

                    MotionEvent.ACTION_MOVE //view is moving
                    -> {


                        val movingY = motionEvent.rawY

                        // limit the moving range of view
                        if (movingY <= m_ViewStartingY && movingY > triggerRangeLimit) {
                            view.animate()
                                    .x(m_VcentreX)
                                    .y(motionEvent.rawY + dY)
                                    .setDuration(0)
                                    .start()
                        }
                    }


                    MotionEvent.ACTION_UP // view is released
                    -> {


                        val relesed_view_Y = motionEvent.rawY// Y axis of the view when released

                        //Check if the Y value of view when released lies under the range of action || or || it is beyond the limit, in both the cases we need to trigger action of the view
                        if (relesed_view_Y < triggerRangeLimit && relesed_view_Y > triggerRangeDownValue || relesed_view_Y < triggerRangeDownValue) {


                            if (ItemClicked == 3) {//Message button

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


                                val dialog = builder.create()
                                dialog.show()
                            } else if (ItemClicked == 2) {//Accept Button
                                answer(true)
                                setDisplayMode()
                            } else {// Decline Button
                                hangup()
                            }


                        }


                        view.animate()
                                .x(m_VcentreX)
                                .y(m_VcentreY)
                                .setDuration(0)
                                .start()

                        if (ItemClicked == 2) { // start shaky animation on accept button again when button is released
                            view.startAnimation(shake)
                        }


                        mSwipeTextDecline!!.visibility = View.INVISIBLE
                        mSwipeTextAccept!!.visibility = View.VISIBLE
                        mSwipeTextMessage!!.visibility = View.INVISIBLE
                        mUpArrowDecline!!.visibility = View.INVISIBLE
                        mUpArrowAccept!!.visibility = View.VISIBLE
                        mUpArrowMessage!!.visibility = View.INVISIBLE


                        firstTouch = false
                    }
                    else -> return false
                }


            }
            return true
        }
    }


    private fun setButton(v: ImageButton, enable: Boolean) {
        //v.setAlpha((float)(enable?mCallConf.buttonAlphaOn:mCallConf.buttonAlphaOff)/255.0f);

    }

    override fun onStart() {
        super.onStart()

        val captureSliderEnabled = false
        val args = arguments
        if (args != null) {
            //            contactView.setText(mCall.profile.name);
            //            videoCallEnabled = mCallConf.videoCallEnabled;
            //            captureSliderEnabled = videoCallEnabled && mCallConf.captureQualitySlider;
        }
        val videoCallEnabled = true
        if (!videoCallEnabled) {
            cameraSwitchButton!!.visibility = View.INVISIBLE
        }
        //        if (captureSliderEnabled) {
        //            captureFormatSlider.setOnSeekBarChangeListener(
        ////                    new CaptureQualityController(captureFormatText, callEvents));
        //       // } else {
        //            captureFormatText.setVisibility(View.GONE);
        //            captureFormatSlider.setVisibility(View.GONE);
        //        }
    }

    override fun onResume() {
        super.onResume()
        //        setButton(toggleMuteButton, mCall.audioMute);
        //        setButton(toggleCameraButton, mCall.videoMute);
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

    override fun Mesibo_onCallStatus(peerid: Long, callid: Long, status: Int, flags: Int, desc: String): Boolean {
        //Log.d(TAG, "Mesibo_onCallStatus: status: " + status + " flags: " + flags);

        //        if(null == mCall)
        //            return true;

        //        boolean video = ((flags & Mesibo.CALLFLAG_VIDEO) > 0);
        //        if (CALLSTATUS_ANSWER == status && !video) {
        //            setButton(toggleCameraButton, true);
        //        }
        return false
    }

    override fun Mesibo_onCallServer(type: Int, url: String, username: String, credential: String) {

    }

    override fun onTouch(view: View, motionEvent: MotionEvent): Boolean {
        return false
    }
}
