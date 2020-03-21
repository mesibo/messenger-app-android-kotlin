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
import android.support.v7.app.AppCompatActivity
import android.support.v7.view.ActionMode
import android.support.v7.widget.Toolbar
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.View.OnClickListener
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView

import com.mesibo.api.Mesibo
import com.mesibo.api.MesiboUtils
import com.mesibo.messaging.MesiboMessagingFragment
import com.mesibo.messaging.MesiboUI


class MessagingActivityNew : AppCompatActivity(), MesiboMessagingFragment.FragmentListener {


    private var mToolbar: Toolbar? = null


    internal var mFragment: MessagingUIFragmentNew? = null

    private var mMesiboUIHelperlistener: Mesibo.UIHelperListner? = null
    private var mMesiboUIOptions: MesiboUI.Config? = null

    private var mUser: Mesibo.UserProfile? = null
    private var mProfileImage: ImageView? = null
    private var mUserStatus: TextView? = null
    private var mTitle: TextView? = null
    private var mProfileImagePath: String? = null
    private var mProfileThumbnail: Bitmap? = null

    private var mActionMode: ActionMode? = null
    private val mActionModeCallback = ActionModeCallback()

    private var mParameter: Mesibo.MessageParams? = null
    internal var mIsMessagingLite = true

    override fun onCreate(savedInstanceState: Bundle?) {
        //getWindow().requestFeature(Window.FEATURE_ACTION_MODE_OVERLAY);

        super.onCreate(savedInstanceState)
        val args = intent.extras ?: return

        //TBD, this must be fixed
        if (!Mesibo.isReady()) {
            finish()
            return
        }

        mMesiboUIHelperlistener = Mesibo.getUIHelperListner()
        mMesiboUIOptions = MesiboUI.getConfig()

        val peer = args.getString("peer")
        val groupId = args.getLong("groupid")

        if (groupId > 0) {
            mUser = Mesibo.getUserProfile(groupId)
        } else
            mUser = Mesibo.getUserProfile(peer)

        if (null == mUser) {
            finish()
            return
        }

        mParameter = Mesibo.MessageParams(peer, groupId, Mesibo.FLAG_DEFAULT, 0)

        // setContentView(R.layout.activity_messaging_new);
        setContentView(R.layout.activity_chat_layout)

        mToolbar = findViewById(R.id.toolbar)
        /// Utils.setActivityStyle(this, mToolbar);


        setSupportActionBar(mToolbar)
        val ab = supportActionBar
        //getSupportActionBar().setHomeAsUpIndicator(new RoundImageDrawable(b));
        ab!!.setDisplayHomeAsUpEnabled(true)
        ab.setDisplayShowHomeEnabled(true)

        mUserStatus = findViewById<View>(R.id.chat_profile_subtitle) as TextView
        // Utils.setTextViewColor(mUserStatus, TOOLBAR_TEXT_COLOR);

        mProfileImage = findViewById<View>(R.id.chat_profile_pic) as ImageView
        if (mProfileImage != null) {

            mProfileImage!!.setOnClickListener(OnClickListener {
                if (null == mProfileImagePath) {
                    return@OnClickListener
                }

                // MesiboUIManager.launchPictureActivity(MessagingActivityNew.this, mUser.name, mProfileImagePath);
            })
        }

        val nameLayout = findViewById<View>(R.id.name_tite_layout) as RelativeLayout
        mTitle = findViewById<View>(R.id.chat_profile_title) as TextView
        mTitle!!.text = mUser!!.name
        // Utils.setTextViewColor(mTitle, TOOLBAR_TEXT_COLOR);

        if (mTitle != null) {
            nameLayout.setOnClickListener {
                //MesiboUIManager.launchUserProfile(MessagingActivityNew.this, mParameter.peer, mParameter.groupid, v);
                if (null != mMesiboUIHelperlistener)
                    mMesiboUIHelperlistener!!.Mesibo_onShowProfile(this@MessagingActivityNew, mUser)
            }
        }

        startFragment(savedInstanceState)

    }

    private fun startFragment(savedInstanceState: Bundle?) {
        // However, if we're being restored from a previous state,
        // then we don't need to do anything and should return or else
        // we could end up with overlapping fragments.
        if (findViewById<View>(R.id.fragment_container) == null || savedInstanceState != null) {
            return
        }

        // Create a new Fragment to be placed in the activity layout
        //if (mIsMessagingLite)
        //mFragment = new MessagingUIFragmentLite();
        // else
        mFragment = MessagingUIFragmentNew()

        // In case this activity was started with special instructions from an
        // Intent, pass the Intent's extras to the fragment as arguments
        mFragment!!.arguments = intent.extras

        // Add the fragment to the 'fragment_container' FrameLayout
        supportFragmentManager.beginTransaction()
                .add(R.id.fragment_container, mFragment!!).commit()
    }

    override fun onStart() {
        super.onStart()

    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        if (null == mMesiboUIHelperlistener)
            return true

        //int menuId = mMesiboUIHelperlistener.Mesibo_onGetMenuResourceId(FROM_MESSAGING_ACTIVITY);
        //getMenuInflater().inflate(menuId, menu);

        mMesiboUIHelperlistener!!.Mesibo_onGetMenuResourceId(this, FROM_MESSAGING_ACTIVITY, mParameter, menu)

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        val id = item.itemId

        if (id == android.R.id.home) {
            finish()
            return true
        } else {
            mMesiboUIHelperlistener!!.Mesibo_onMenuItemSelected(this, FROM_MESSAGING_ACTIVITY, mParameter, id)
        }
        return super.onOptionsItemSelected(item)
    }


    //TBD, note this requires API level 10
    /*
    private Bitmap createThumbnailAtTime(String filePath, int timeInSeconds){
        MediaMetadataRetriever mMMR = new MediaMetadataRetriever();
        mMMR.setDataSource(filePath);
        //api time unit is microseconds
        return mMMR.getFrameAtTime(timeInSeconds*1000000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC);
    }
    */


    override fun onDestroy() {

        super.onDestroy()
    }

    override fun onBackPressed() {
        if (mFragment!!.Mesibo_onBackPressed()) {
            return
        }

        super.onBackPressed() // allows standard use of backbutton for page 1

    }

    override fun Mesibo_onUpdateUserPicture(profile: Mesibo.UserProfile, thumbnail: Bitmap, picturePath: String?) {
        mProfileThumbnail = thumbnail
        mProfileImagePath = picturePath
        mProfileImage!!.setImageDrawable(MesiboUtils.getRoundImageDrawable(mProfileThumbnail))
    }

    override fun Mesibo_onUpdateUserOnlineStatus(profile: Mesibo.UserProfile, status: String?) {
        if (null == status) {
            mUserStatus!!.visibility = View.GONE
            return
        }

        mUserStatus!!.visibility = View.VISIBLE
        mUserStatus!!.text = status
        return
    }

    override fun Mesibo_onShowInContextUserInterface() {
        mActionMode = startSupportActionMode(mActionModeCallback)
    }

    override fun Mesibo_onHideInContextUserInterface() {
        if (null == mActionMode)
            return
        mActionMode!!.finish()
    }

    override fun Mesibo_onContextUserInterfaceCount(count: Int) {
        if (null == mActionMode)
            return

        mActionMode!!.title = count.toString()
        mActionMode!!.invalidate()
    }

    override fun Mesibo_onError(type: Int, title: String, message: String) {
        //  Utils.showAlert(this, title, message);
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>, grantResults: IntArray) {
        mFragment!!.Mesibo_onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        //super.onActivityResult(requestCode, resultCode, data);
        mFragment!!.Mesibo_onActivityResult(requestCode, resultCode, data)
    }

    //    private class ActionModeCallback implements ActionMode.Callback {
    //        @SuppressWarnings("unused")
    //        private final String TAG = ActionModeCallback.class.getSimpleName();
    //
    //
    //        @Override
    //        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
    //            menu.clear();
    //            mode.getMenuInflater().inflate(com.mesibo.messaging.R.menu.selected_menu, menu);
    //
    //            menu.findItem(com.mesibo.messaging.R.id.menu_reply).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
    //            menu.findItem(com.mesibo.messaging.R.id.menu_star).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
    //            menu.findItem(com.mesibo.messaging.R.id.menu_resend).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
    //            menu.findItem(com.mesibo.messaging.R.id.menu_copy).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
    //            menu.findItem(com.mesibo.messaging.R.id.menu_forward).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
    //            menu.findItem(com.mesibo.messaging.R.id.menu_forward).setVisible(mMesiboUIOptions.enableForward);
    //            menu.findItem(com.mesibo.messaging.R.id.menu_forward).setEnabled(mMesiboUIOptions.enableForward);
    //            menu.findItem(com.mesibo.messaging.R.id.menu_remove).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
    //
    //            return true;
    //        }
    //
    //        @Override
    //        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
    //
    //            int enabled = mFragment.Mesibo_onGetEnabledActionItems();
    //
    //
    //            menu.findItem(com.mesibo.messaging.R.id.menu_resend).setVisible((enabled&MesiboMessagingFragment.MESIBO_MESSAGECONTEXTACTION_RESEND) > 0);
    //
    //            //menu.findItem(R.id.menu_forward).setVisible(selection.size() == 1);
    //            menu.findItem(com.mesibo.messaging.R.id.menu_copy).setVisible((enabled&MesiboMessagingFragment.MESIBO_MESSAGECONTEXTACTION_COPY) > 0);
    //            menu.findItem(com.mesibo.messaging.R.id.menu_copy).setVisible((enabled&MesiboMessagingFragment.MESIBO_MESSAGECONTEXTACTION_REPLY) > 0);
    //            return true;
    //        }
    //
    //        @Override
    //        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
    //
    //            int mesiboItemId = 0;
    //
    //            if (item.getItemId() == com.mesibo.messaging.R.id.menu_remove) {
    //                mesiboItemId = MesiboMessagingFragment.MESIBO_MESSAGECONTEXTACTION_DELETE;
    //            } else if (item.getItemId() == com.mesibo.messaging.R.id.menu_copy) {
    //
    //                mesiboItemId = MesiboMessagingFragment.MESIBO_MESSAGECONTEXTACTION_COPY;
    //
    //            } else if (item.getItemId() == com.mesibo.messaging.R.id.menu_resend) {
    //                mesiboItemId = MesiboMessagingFragment.MESIBO_MESSAGECONTEXTACTION_RESEND;
    //            } else if (item.getItemId() == com.mesibo.messaging.R.id.menu_forward) {
    //                mesiboItemId = MesiboMessagingFragment.MESIBO_MESSAGECONTEXTACTION_FORWARD;
    //            } else if (item.getItemId() == com.mesibo.messaging.R.id.menu_star) {
    //                mesiboItemId = MesiboMessagingFragment.MESIBO_MESSAGECONTEXTACTION_FAVORITE;
    //            } else if (item.getItemId() == com.mesibo.messaging.R.id.menu_reply) {
    //                mesiboItemId = MesiboMessagingFragment.MESIBO_MESSAGECONTEXTACTION_REPLY;
    //            }
    //
    //            if(mesiboItemId > 0) {
    //                mFragment.Mesibo_onActionItemClicked(mesiboItemId);
    //                mode.finish();
    //                mFragment.Mesibo_onInContextUserInterfaceClosed();
    //                return true;
    //            }
    //
    //            return false;
    //        }
    //
    //        @Override
    //        public void onDestroyActionMode(ActionMode mode) {
    //            mFragment.Mesibo_onInContextUserInterfaceClosed();
    //            mActionMode = null;
    //        }
    //    }

    private inner class ActionModeCallback : ActionMode.Callback {
        private val TAG = MessagingActivityNew.ActionModeCallback::class.java.simpleName


        override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
            menu.clear()
            mode.menuInflater.inflate(R.menu.selected_menu_app, menu)
            menu.findItem(R.id.menu_reply).setVisible(false).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
            menu.findItem(R.id.menu_star).setVisible(false).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
            menu.findItem(R.id.menu_resend).setVisible(false).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
            menu.findItem(R.id.menu_copy).setVisible(false).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
            menu.findItem(R.id.menu_forward).setVisible(false).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
            menu.findItem(R.id.menu_remove).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
            menu.findItem(R.id.menu_share).setVisible(false).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
            return true
        }

        override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {

            val enabled = mFragment!!.Mesibo_onGetEnabledActionItems()

//            menu.findItem(R.id.menu_reply).isVisible = enabled and MessagingUIFragmentNew.MESIBO_MESSAGECONTEXTACTION_REPLY > 0
//            menu.findItem(R.id.menu_copy).isVisible = enabled and MessagingUIFragmentNew.MESIBO_MESSAGECONTEXTACTION_COPY > 0
//            menu.findItem(R.id.menu_share).isVisible = enabled and MessagingUIFragmentNew.MESIBO_MESSAGECONTEXTACTION_COPY > 0

            return true
        }

        override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {

            var mesiboItemId = 0

            if (item.itemId == R.id.menu_remove) {
                mesiboItemId = AppConfig.MESSAGECONTEXTACTION_DELETE
            } else if (item.itemId == R.id.menu_copy) {
                mesiboItemId = AppConfig.MESSAGECONTEXTACTION_COPY
            } else if (item.itemId == R.id.menu_resend) {
                mesiboItemId = AppConfig.MESSAGECONTEXTACTION_RESEND
            } else if (item.itemId == R.id.menu_forward) {
                mesiboItemId = AppConfig.MESSAGECONTEXTACTION_FORWARD
            } else if (item.itemId == R.id.menu_star) {
                mesiboItemId = AppConfig.MESSAGECONTEXTACTION_FAVORITE
            } else if (item.itemId == R.id.menu_reply) {
                mesiboItemId = AppConfig.MESSAGECONTEXTACTION_REPLY
            } else if (item.itemId == R.id.menu_share) {
                mesiboItemId = AppConfig.MESSAGECONTEXTACTION_SHARE
            }

            if (mesiboItemId > 0) {
                mFragment!!.onActionItemClicked(mesiboItemId)
                mode.finish()
                mFragment!!.Mesibo_onInContextUserInterfaceClosed()
                return true

            }

            return false
        }

        override fun onDestroyActionMode(mode: ActionMode) {
            mFragment!!.Mesibo_onInContextUserInterfaceClosed()
            mActionMode = null
        }
    }

    override fun onResume() {
        super.onResume()
        //MesiboUIManager.setMessagingActivityNew(this);
    }

    companion object {

        internal var FROM_MESSAGING_ACTIVITY = 1
    }
}

