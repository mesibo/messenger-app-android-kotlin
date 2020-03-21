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

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.support.design.widget.TabLayout
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import android.support.v4.view.ViewPager
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.util.Log
import android.view.View
import android.widget.RelativeLayout

import com.mesibo.api.Mesibo
import com.mesibo.calls.MesiboCall
import com.mesibo.mediapicker.MediaPicker
import com.mesibo.messaging.MesiboUserListFragment
import com.mesibo.messaging.MesiboMessagingFragment

import java.util.ArrayList

class MesiboMainActivity : AppCompatActivity(), MesiboUserListFragment.FragmentListener, MesiboMessagingFragment.FragmentListener, MediaPicker.ImageEditorListener, Mesibo.CallListener {


    internal lateinit var mUserListFragment: MesiboUserListFragment
    internal lateinit var mesiboMyCallLogsFragment: MesiboMyCallLogsFragment
    internal lateinit var mAdapter: ViewPagerAdapter
    internal lateinit var mReturnToCallFragment: RelativeLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d("MeisboKotlin", "==== onCreate called ====")
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_pager_layout)

        mReturnToCallFragment = findViewById(R.id.returnToCallLayout)
        val toolbar = findViewById<View>(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)

        val viewPager = findViewById<ViewPager>(R.id.viewpager)
        setupViewPager(viewPager)


        val tabLayout = findViewById<View>(R.id.tabs) as TabLayout
        tabLayout.setupWithViewPager(viewPager)

        mReturnToCallFragment.setOnClickListener { MesiboCall.getInstance().call(this@MesiboMainActivity, 0, null, false) }


        Mesibo.addListener(this)

    }


    private fun setupViewPager(viewPager: ViewPager) {
        mAdapter = ViewPagerAdapter(supportFragmentManager)

        val bundle = Bundle()
        val myMessage = "NewContactSelector"
        bundle.putString("message", myMessage)
        // Create a new Fragment to be placed in the activity layout
        mUserListFragment = MesiboUserListFragment()
        mUserListFragment.listener = this
        // In case this activity was started with special instructions from an
        // Intent, pass the Intent's extras to the fragment as arguments
        mUserListFragment.arguments = bundle

        mesiboMyCallLogsFragment = MesiboMyCallLogsFragment()
        mAdapter.addFragment(mUserListFragment, "Chats")
        mAdapter.addFragment(mesiboMyCallLogsFragment, "Call Logs")

        viewPager.adapter = mAdapter


    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        val filePath = MediaPicker.processOnActivityResult(this, requestCode, resultCode, data)
                ?: return


        UIManager.launchImageEditor(this, MediaPicker.TYPE_FILEIMAGE, -1, "", filePath, false, false, true, true, 600, this)

    }

    override fun onImageEdit(i: Int, s: String, filePath: String, bitmap: Bitmap, status1: Int) {

    }

    override fun Mesibo_onUpdateTitle(s: String?) {

    }

    override fun Mesibo_onUpdateSubTitle(s: String?) {

    }

    override fun Mesibo_onClickUser(s: String, l: Long, l1: Long): Boolean {

        val i = Intent(this, MessagingActivityNew::class.java)
        i.putExtra("peer", s)
        i.putExtra("groupid", l)
        startActivity(i)

        //return false to load default
        return true
    }

    override fun Mesibo_onUserListFilter(messageParams: Mesibo.MessageParams): Boolean {
        return false
    }

    override fun onPointerCaptureChanged(hasCapture: Boolean) {

    }

    override fun Mesibo_onUpdateUserPicture(userProfile: Mesibo.UserProfile, bitmap: Bitmap, s: String) {

    }

    override fun Mesibo_onUpdateUserOnlineStatus(userProfile: Mesibo.UserProfile, s: String) {

    }

    override fun Mesibo_onShowInContextUserInterface() {

    }

    override fun Mesibo_onHideInContextUserInterface() {

    }

    override fun Mesibo_onContextUserInterfaceCount(i: Int) {

    }

    override fun Mesibo_onError(i: Int, s: String, s1: String) {

    }


    override fun onResume() {
        super.onResume()

    }

    override fun Mesibo_onCall(l: Long, l1: Long, userProfile: Mesibo.UserProfile, i: Int): Boolean {
        return false
    }

    override fun Mesibo_onCallStatus(l: Long, l1: Long, i: Int, i1: Int, s: String?): Boolean {

        UIManager.showOnCallProgressGreenBar(mReturnToCallFragment)
        return false
    }

    override fun Mesibo_onCallServer(i: Int, s: String, s1: String, s2: String) {

    }

    internal inner class ViewPagerAdapter(manager: FragmentManager) : FragmentPagerAdapter(manager) {
        private val mFragmentList = ArrayList<Fragment>()
        private val mFragmentTitleList = ArrayList<String>()

        override fun getItem(position: Int): Fragment {
            return mFragmentList[position]
        }

        override fun getCount(): Int {
            return mFragmentList.size
        }

        fun addFragment(fragment: Fragment, title: String) {
            mFragmentList.add(fragment)
            mFragmentTitleList.add(title)
        }

        override fun getPageTitle(position: Int): CharSequence? {
            return mFragmentTitleList[position]
        }
    }

}
