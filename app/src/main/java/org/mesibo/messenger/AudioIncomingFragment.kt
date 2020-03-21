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
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.app.KeyguardManager
import android.content.Context
import android.os.Bundle
import android.view.GestureDetector
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView

import com.mesibo.api.Mesibo
import com.mesibo.api.MesiboUtils
import com.mesibo.calls.ImageTouchSlider


import com.mesibo.calls.MesiboIncomingAudioCallFragment
import com.mesibo.messaging.MessagingActivity

import com.mesibo.api.Mesibo.CALLSTATUS_COMPLETE

/**
 * Created by mesibo on 6/5/17.
 */

class AudioIncomingFragment : MesiboIncomingAudioCallFragment(), View.OnClickListener, Mesibo.CallListener, View.OnKeyListener {

    private var mUpArrowDecline: LinearLayout? = null
    private var mUpArrowAccept: LinearLayout? = null
    private var mUpArrowMessage: LinearLayout? = null
    private val contactView: TextView? = null
    private var mSwipeTextDecline: TextView? = null
    private var mSwipeTextAccept: TextView? = null
    private var mSwipeTextMessage: TextView? = null
    internal lateinit var shake: Animation
    internal var dX: Float = 0.toFloat()
    internal var dY: Float = 0.toFloat()
    internal var m_screen_width: Float = 0.toFloat()
    internal var m_screen_height: Float = 0.toFloat()
    internal var m_VcentreX: Float = 0.toFloat()
    internal var m_VcentreY: Float = 0.toFloat()
    internal var m_ViewStartingY: Float = 0.toFloat()
    internal lateinit var photoImage: ImageView
    internal lateinit var photoImageDown: ImageView
    internal var firstTouch = false
    internal lateinit var mProfile: Mesibo.UserProfile
    internal lateinit var mName: TextView
    internal lateinit var mLocation: TextView
    internal var mContext: Context? = null
    internal lateinit var slider: ImageTouchSlider
    internal lateinit var myGestDetector: GestureDetector
    internal lateinit var mLocked: LinearLayout
    internal lateinit var mUnLocked: LinearLayout

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mContext = activity
        val view = inflater.inflate(com.mesibo.calls.R.layout.incoming_fragment_new, container, false)
        initializeView(view)
        return view
    }

    private fun initializeView(view: View) {
        mName = view.findViewById(com.mesibo.calls.R.id.incoming_name)
        mLocation = view.findViewById(com.mesibo.calls.R.id.incoming_location)
        slider = view.findViewById(com.mesibo.calls.R.id.slider)
        mUnLocked = view.findViewById(com.mesibo.calls.R.id.phone_unlocked)
        mLocked = view.findViewById(com.mesibo.calls.R.id.phone_locked)
        photoImage = view.findViewById(com.mesibo.calls.R.id.photo_image)
        photoImageDown = view.findViewById(com.mesibo.calls.R.id.photo_image_down)

        val mDeclineViewButton = view.findViewById<RelativeLayout>(R.id.decline_view_layout)
        val mAcceptViewButton = view.findViewById<RelativeLayout>(R.id.accept_view_layout)
        val mDefaultMessageButton = view.findViewById<RelativeLayout>(R.id.custom_message_view_layout)

        mSwipeTextDecline = view.findViewById(R.id.decline_swipe_tv)
        mSwipeTextAccept = view.findViewById(R.id.accept_swipe_tv)
        mSwipeTextMessage = view.findViewById(R.id.message_swipe_tv)

        mUpArrowDecline = view.findViewById(R.id.decline_up_arrow)
        mUpArrowAccept = view.findViewById(R.id.accept_up_arrow)
        mUpArrowMessage = view.findViewById(R.id.message_up_arrow)


        ///to know the size of screen
        val mRootLayout = view.findViewById<LinearLayout>(R.id.incoming_fragment_view)

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

        mName.text = mProfile.name
        setUserPicture()
        setValues()

    }

    private fun setValues() {
        myGestDetector = GestureDetector(activity, object : GestureDetector.SimpleOnGestureListener() {
            internal var swipePerformed = true

            override fun onScroll(e1: MotionEvent, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
                return if (!swipePerformed) {
                    true
                } else false
            }

            override fun onDown(e1: MotionEvent): Boolean {
                swipePerformed = false
                return true
            }

            override fun onFling(e1: MotionEvent, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
                return false
            }
        })
        val myKeyManager = activity!!.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager

        //Yusuf, TBD fix lock screen
        if (false && myKeyManager.inKeyguardRestrictedInputMode()) {

            //screen is locked
            mLocked.visibility = View.VISIBLE
            mUnLocked.visibility = View.GONE

        } else {
            mLocked.visibility = View.GONE
            mUnLocked.visibility = View.VISIBLE
            //screen is not locked

        }

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
                photoImage.setImageDrawable(MesiboUtils.getRoundImageDrawable(b))
                photoImageDown.setImageBitmap(b)
            }
        } else {
            //TBD, getActivity.getresource crashes sometime if activity is closing
            photoImage.setImageDrawable(MesiboUtils.getRoundImageDrawable(BitmapFactory.decodeResource(MainApplication.appContext?.resources, R.drawable.default_user_image)))
            photoImageDown.setImageDrawable(MesiboUtils.getRoundImageDrawable(BitmapFactory.decodeResource(MainApplication.appContext?.resources, R.drawable.default_user_image)))
        }
    }


    private fun sendCustomeMessage(peer: String, message: String) {

        val messageParams = Mesibo.MessageParams()
        messageParams.setPeer(peer)
        Mesibo.sendMessage(messageParams, Mesibo.random(), message)
        callHangup()


    }

    private fun MoveUpAnimation(v: View?) {
        val animation = ObjectAnimator.ofFloat(v, "translationY", 100f)
        animation.duration = 1000
        animation.repeatCount = ValueAnimator.INFINITE
        animation.repeatMode = ValueAnimator.REVERSE
        animation.start()
    }

    override fun onKey(v: View, keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            //Do something
            // Toast.makeText(mContext, "vol", Toast.LENGTH_SHORT).show();
        }
        return true
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

                                sendCustomMessageOnCall()

                            } else if (ItemClicked == 2) {//Accept Button

                                callAnswer()

                            } else {// Decline Button

                                callHangup()

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


    fun callHangup() {
        hangup()
        activity!!.finish()
    }


    fun callAnswer() {

        answer()


    }

    fun sendCustomMessageOnCall() {
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
                }
            }
            callHangup()
        }


        val dialog = builder.create()
        dialog.show()
    }


    override fun onClick(v: View) {}

    override fun onResume() {
        super.onResume()
        Mesibo.addListener(this)
    }

    override fun onPause() {
        super.onPause()
        Mesibo.removeListener(this)
        stopIncomingNotification()

    }


    override fun Mesibo_onCall(peerid: Long, callid: Long, profile: Mesibo.UserProfile, flags: Int): Boolean {
        return true
    }

    override fun Mesibo_onCallStatus(peerid: Long, callid: Long, status: Int, flags: Int, desc: String): Boolean {
        if (status and CALLSTATUS_COMPLETE > 0) {
            activity!!.finish()
        }
        return true
    }

    override fun Mesibo_onCallServer(type: Int, url: String, username: String, credential: String) {

    }

    companion object {
        val TAG = "AudioIncomingFragment"
    }
}
