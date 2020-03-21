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
package org.mesibo.messenger.AppSettings


import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout

import com.mesibo.api.Mesibo
import com.mesibo.api.MesiboUtils
import com.mesibo.emojiview.EmojiconTextView
import org.mesibo.messenger.BlockedSettingsFragment
import org.mesibo.messenger.EditProfileFragment
import org.mesibo.messenger.SampleAPI

import org.mesibo.messenger.R


class BasicSettingsFragment : Fragment() {


    private var mUserName: EmojiconTextView? = null
    private var mUserStatus: EmojiconTextView? = null
    private var mUserImage: ImageView? = null
    private var mUser: Mesibo.UserProfile? = Mesibo.getSelfProfile()


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val v = inflater.inflate(R.layout.fragment_basic_settings, container, false)
        val ab = (activity as AppCompatActivity).supportActionBar
        ab!!.setDisplayHomeAsUpEnabled(true)
        ab.title = "Settings"

        if (null == mUser) {
            activity!!.finish()
            //TBD show alert
            return v
        }

        mUserName = v.findViewById<View>(R.id.set_self_user_name) as EmojiconTextView
        mUserStatus = v.findViewById<View>(R.id.set_self_status) as EmojiconTextView
        mUserImage = v.findViewById<View>(R.id.set_user_image) as ImageView


        val profileLayout = v.findViewById<View>(R.id.set_picture_name_status_layout) as LinearLayout
        profileLayout.setOnClickListener {
            val RegFragment = EditProfileFragment()
            (activity as SettingsActivity).setRequestingFragment(RegFragment)
            RegFragment.activateInSettingsMode()
            val fm = (activity as AppCompatActivity).supportFragmentManager
            val ft = fm.beginTransaction()
            ft.replace(R.id.settings_fragment_place, RegFragment, "null")
            ft.addToBackStack("profile")
            ft.commit()
        }

        val DataUsageLayout = v.findViewById<View>(R.id.set_data_layout) as LinearLayout
        DataUsageLayout.setOnClickListener {
            val dataFragment = DataUsageFragment()
            val fm = (activity as AppCompatActivity).supportFragmentManager
            val ft = fm.beginTransaction()
            ft.replace(R.id.settings_fragment_place, dataFragment, "null")
            ft.addToBackStack("datausage")
            ft.commit()
        }


        val BlockedUserLayout = v.findViewById<View>(R.id.set_blocked_layout) as LinearLayout
        BlockedUserLayout.setOnClickListener {
            val dataFragment = BlockedSettingsFragment()
            val fm = (activity as AppCompatActivity).supportFragmentManager
            val ft = fm.beginTransaction()
            ft.replace(R.id.settings_fragment_place, dataFragment, "null")
            ft.addToBackStack("blocked")
            ft.commit()
        }

        val aboutLayout = v.findViewById<View>(R.id.set_about_layout) as LinearLayout
        aboutLayout.setOnClickListener {
            val aboutFragment = AboutFragment()
            val fm = (activity as AppCompatActivity).supportFragmentManager
            val ft = fm.beginTransaction()
            ft.replace(R.id.settings_fragment_place, aboutFragment, "null")
            ft.addToBackStack("about")
            ft.commit()
        }

        val privacyPolicy = v.findViewById<View>(R.id.privacy_policy_layout) as LinearLayout
        privacyPolicy.setOnClickListener {
            val url = "https://mesibo.com/terms-of-use/"
            val i = Intent(Intent.ACTION_VIEW)
            i.data = Uri.parse(url)
            startActivity(i)
        }

        val logoutLayout = v.findViewById<View>(R.id.set_logout_layout) as LinearLayout
        logoutLayout.setOnClickListener {
            SampleAPI.startLogout()
            activity!!.finish()
        }


        return v
    }

    /**
     * Called when the fragment is visible to the user and actively running.
     * This is generally
     * tied to [Activity.onResume] of the containing
     * Activity's lifecycle.
     */
    override fun onResume() {
        super.onResume()
        mUser = Mesibo.getSelfProfile()
        val imagePath = Mesibo.getUserProfilePicturePath(mUser, Mesibo.FileInfo.TYPE_AUTO)
        if (null != imagePath) {
            val b = BitmapFactory.decodeFile(imagePath)
            if (null != b)
                mUserImage!!.setImageDrawable(MesiboUtils.getRoundImageDrawable(b))
        }

        if (!TextUtils.isEmpty(mUser!!.name)) {
            mUserName!!.text = mUser!!.name
        } else {
            mUserName!!.text = ""
        }

        if (!TextUtils.isEmpty(mUser!!.status)) {
            mUserStatus!!.text = mUser!!.status
        } else {
            mUserStatus!!.text = ""
        }
    }
}// Required empty public constructor
