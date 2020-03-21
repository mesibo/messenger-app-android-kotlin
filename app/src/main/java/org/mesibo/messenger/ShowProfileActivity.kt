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


import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.support.design.widget.AppBarLayout
import android.support.design.widget.CollapsingToolbarLayout
import android.support.design.widget.CoordinatorLayout
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.widget.Toolbar
import android.view.Menu
import android.view.View
import android.view.animation.AlphaAnimation
import android.widget.TextView

import com.mesibo.api.Mesibo

class ShowProfileActivity : AppCompatActivity(), ShowProfileFragment.OnFragmentInteractionListener, Mesibo.FileTransferListener, Mesibo.UserProfileUpdateListener {

    internal lateinit var mUsermageView: SquareImageView
    internal var mUserProfile: Mesibo.UserProfile? = null
    internal lateinit var mToolbar: Toolbar
    internal lateinit var mAppBarLayout: AppBarLayout
    internal lateinit var mCoordinatorLayout: CoordinatorLayout
    internal var mGroupId: Long = 0
    internal var mPeer: String? = null
    private var mProfilePicturePath: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_show_user_profile)
        mToolbar = findViewById<View>(R.id.up_toolbar) as Toolbar
        setSupportActionBar(mToolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        val args = intent.extras ?: return

        mPeer = args.getString("peer")
        mGroupId = args.getLong("groupid")

        mUserProfile = null

        if (mGroupId > 0) {
            mUserProfile = Mesibo.getUserProfile(mGroupId)
        } else {
            mUserProfile = Mesibo.getUserProfile(mPeer)
        }
        mUsermageView = findViewById<View>(R.id.up_image_profile) as SquareImageView

        Mesibo.addListener(this)

        mUsermageView.setOnClickListener { UIManager.launchImageViewer(this@ShowProfileActivity, mProfilePicturePath!!) }

        val userName = findViewById<View>(R.id.up_user_name) as TextView
        val userstatus = findViewById<View>(R.id.up_current_status) as TextView

        userName.text = mUserProfile!!.name
        var lastSeen = Mesibo.getTimestamp() - mUserProfile!!.lastActiveTime
        userstatus.visibility = View.VISIBLE
        if (lastSeen <= 60000) {
            userstatus.text = "Online"
        } else {
            var seenStatus = ""
            lastSeen = lastSeen / 60000 //miutes
            if (mUserProfile!!.groupid > 0 || 0L == mUserProfile!!.lastActiveTime) {
                userstatus.visibility = View.GONE
            } else if (lastSeen >= 2 * 24 * 60) {
                seenStatus = (lastSeen / (24 * 60)).toInt().toString() + " days ago"
            } else if (lastSeen >= 24 * 60) {
                seenStatus = "yesterday"
            } else if (lastSeen >= 120) {
                seenStatus = (lastSeen / 60).toInt().toString() + " hours ago"
            } else if (lastSeen >= 60) {
                seenStatus = "an hour ago"
            } else if (lastSeen >= 2) {
                seenStatus = "$lastSeen minutes ago"
            } else {
                seenStatus = "a few moments before"
            }

            userstatus.text = "Last seen $seenStatus"

            //userstatus.setVisibility(View.GONE);
        }

        val collapsingToolbar = findViewById<CollapsingToolbarLayout>(R.id.up_collapsing_toolbar)
        collapsingToolbar.title = "  "
        mCoordinatorLayout = findViewById(R.id.up_profile_root)
        mAppBarLayout = findViewById(R.id.up_appbar)
        mAppBarLayout.post { setAppBarOffset(-250) }

        mAppBarLayout.addOnOffsetChangedListener(AppBarLayout.OnOffsetChangedListener { appBarLayout, verticalOffset ->
            //measuring for alpha
            var Alpha = (appBarLayout.totalScrollRange - Math.abs(verticalOffset)).toFloat() / appBarLayout.totalScrollRange
            if (Alpha > 0.4)
                Alpha = 1f
            else {
                Alpha = Alpha + 0.6.toFloat()
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
                mUsermageView.alpha = Alpha
            else {
                val alpha = AlphaAnimation(Alpha, Alpha)
                alpha.duration = 0
                alpha.fillAfter = true
                mUsermageView.startAnimation(alpha)
            }
        })

        val fragmentManager = supportFragmentManager
        val fragmentTransaction = fragmentManager.beginTransaction()
        val showUserProfileDetailsFragment = ShowProfileFragment.newInstance(mUserProfile!!)
        fragmentTransaction.add(R.id.up_fragment, showUserProfileDetailsFragment, "up")
        fragmentTransaction.commit()
    }

    private fun setAppBarOffset(offsetPx: Int) {
        val params = mAppBarLayout.layoutParams as CoordinatorLayout.LayoutParams
        val behavior = params.behavior as AppBarLayout.Behavior?
        behavior!!.topAndBottomOffset = -60 - mAppBarLayout.totalScrollRange / 2
        //behavior.onNestedPreScroll(mCoordinatorLayout, mAppBarLayout, null, 0, offsetPx, new int[]{0, 0});
    }

    private fun setUserPicture() {
        mProfilePicturePath = Mesibo.getUserProfilePicturePath(mUserProfile, Mesibo.FileInfo.TYPE_AUTO)
        if (null == mProfilePicturePath || !Mesibo.fileExists(mProfilePicturePath))
            return

        val b = BitmapFactory.decodeFile(mProfilePicturePath)
        if (null != b) {
            mUsermageView.setImageBitmap(b)
        }
    }

    override fun Mesibo_onUserProfileUpdated(userProfile: Mesibo.UserProfile, i: Int, refresh: Boolean) {
        if (!refresh)
            return

        if (userProfile !== mUserProfile)
            return

        if (Mesibo.isUiThread()) {
            setUserPicture()
            Mesibo.startUserProfilePictureTransfer(mUserProfile, this)
            return
        }

        Handler(Looper.getMainLooper()).post {
            setUserPicture()
            Mesibo.startUserProfilePictureTransfer(mUserProfile, this@ShowProfileActivity)
        }


    }

    /**
     * @param file
     */
    override fun Mesibo_onFileTransferProgress(file: Mesibo.FileInfo): Boolean {

        if (100 == file.progress)
            setUserPicture()

        return true
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        //getMenuInflater().inflate(R.menu.user_profile_menu, menu);
        return true
    }

    override fun onFragmentInteraction(uri: Uri) {

    }

    public override fun onResume() {
        super.onResume()

        if (!Mesibo.setAppInForeground(this, 0x102, true)) {
            finish()
            return
        }

        if (mUserProfile!!.groupid > 0) {
            val userName = findViewById<View>(R.id.up_user_name) as TextView
            if (null != mUserProfile!!.name)
                userName.text = mUserProfile!!.name
        }

        setUserPicture()
        Mesibo.startUserProfilePictureTransfer(mUserProfile, this)

    }

    override fun onPause() {
        super.onPause()
        Mesibo.setAppInForeground(this, 0x102, false)
    }

    override fun onDestroy() {
        super.onDestroy()
        Mesibo.setAppInForeground(this, 0x102, false)
    }


    override fun finish() {
        super.finish()
        overridePendingTransition(0, 0)
    }


    public override fun onStart() {
        super.onStart()
    }

    public override fun onStop() {
        super.onStop()
    }
}
