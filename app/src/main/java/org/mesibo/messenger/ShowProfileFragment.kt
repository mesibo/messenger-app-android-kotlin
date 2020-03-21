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

import android.app.Activity
import android.app.ProgressDialog
import android.content.Context
import android.content.DialogInterface
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.app.AlertDialog
import android.support.v7.widget.CardView
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.SwitchCompat
import android.text.TextUtils
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView

import com.mesibo.api.Mesibo
import com.mesibo.api.MesiboUtils
import com.mesibo.emojiview.EmojiconTextView

import org.mesibo.messenger.Utils.AppUtils


import java.net.URLConnection
import java.util.ArrayList

import com.mesibo.mediapicker.AlbumListData
import com.mesibo.mediapicker.AlbumPhotosData
import com.mesibo.messaging.MesiboUI

import android.view.View.GONE
import android.view.View.VISIBLE


class ShowProfileFragment : Fragment(), Mesibo.MessageListener, Mesibo.UserProfileUpdateListener {
    private var mListener: OnFragmentInteractionListener? = null
    private var mThumbnailMediaFiles: ArrayList<String>? = null
    private var mGallery: LinearLayout? = null
    private var mMediaFilesCounter = 0
    private var mMediaCounterView: TextView? = null
    private var mGalleryData: ArrayList<AlbumListData>? = null
    private var mMessageBtn: ImageView? = null
    private var mMediaCardView: CardView? = null
    private var mStatusPhoneCard: CardView? = null
    private var mGroupMemebersCard: CardView? = null
    private var mExitGroupCard: CardView? = null
    private var mExitGroupText: TextView? = null

    var mAdminCount = 0
    var mAdmin = 0

    internal lateinit var mRecyclerView: RecyclerView
    internal var mAdapter: RecyclerView.Adapter<*>? = null
    internal lateinit var mAddMemebers: LinearLayout
    internal var mGroupMemberList = ArrayList<Mesibo.UserProfile>()
    internal lateinit var mProgressDialog: ProgressDialog

    internal lateinit var mll: LinearLayout
    internal lateinit var mStatus: TextView
    internal lateinit var mStatusTime: TextView
    internal lateinit var mMobileNumber: TextView
    internal lateinit var mPhoneType: TextView
    private var mReadSession: Mesibo.ReadDbSession? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)

    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        setHasOptionsMenu(true)

        val v = inflater.inflate(R.layout.fragment_show_user_profile_details, container, false)



        mDefaultProfileBmp = BitmapFactory.decodeResource(activity!!.resources, R.drawable.default_user_image)
        mThumbnailMediaFiles = ArrayList()
        mGalleryData = ArrayList()

        val Images = AlbumListData()
        Images.setmAlbumName("Images")
        val Video = AlbumListData()
        Video.setmAlbumName("Videos")
        val Documents = AlbumListData()
        Documents.setmAlbumName("Documents")
        mGalleryData!!.add(Images)
        mGalleryData!!.add(Video)
        mGalleryData!!.add(Documents)

        mMediaCardView = v.findViewById<View>(R.id.up_media_layout) as CardView
        mMediaCardView!!.visibility = GONE
        Mesibo.addListener(this)

        mReadSession = Mesibo.ReadDbSession(mUser!!.address, mUser!!.groupid, null, this)
        mReadSession!!.enableFiles(true)
        mReadSession!!.enableReadReceipt(true)
        mReadSession!!.read(100)

        mProgressDialog = AppUtils.getProgressDialog(activity, "Please wait...")
        mMessageBtn = v.findViewById<View>(R.id.up_message_btn) as ImageView
        mMessageBtn!!.setOnClickListener { activity!!.finish() }
        mRecyclerView = v.findViewById<View>(R.id.showprofile_memebers_rview) as RecyclerView

        // change in file
        mAddMemebers = v.findViewById<View>(R.id.showprofile_add_memeber) as LinearLayout
        mAddMemebers.visibility = GONE
        mll = v.findViewById<View>(R.id.up_status_card) as LinearLayout
        mStatus = v.findViewById<View>(R.id.up_status_text) as TextView
        mStatusTime = v.findViewById<View>(R.id.up_status_update_time) as TextView
        mMobileNumber = v.findViewById<View>(R.id.up_number) as TextView
        mPhoneType = v.findViewById<View>(R.id.up_phone_type) as TextView

        mRecyclerView.layoutManager = LinearLayoutManager(mRecyclerView.context)
        mAdapter = GroupMemeberAdapter(activity!!, mGroupMemberList)
        mRecyclerView.adapter = mAdapter
        ///
        mGallery = v.findViewById<View>(R.id.up_gallery) as LinearLayout
        mMediaCounterView = v.findViewById<View>(R.id.up_media_counter) as TextView
        mMediaCounterView!!.text = "$mMediaFilesCounter\u3009 "

        mStatusPhoneCard = v.findViewById<View>(R.id.status_phone_card) as CardView
        mGroupMemebersCard = v.findViewById<View>(R.id.showprofile_members_card) as CardView
        mExitGroupCard = v.findViewById<View>(R.id.group_exit_card) as CardView
        mExitGroupText = v.findViewById<View>(R.id.group_exit_text) as TextView
        mExitGroupCard!!.visibility = GONE
        mExitGroupCard!!.setOnClickListener {
            mProgressDialog.show()
            SampleAPI.deleteGroup(mUser!!.groupid, object : SampleAPI.ResponseHandler() {
                override fun HandleAPIResponse(response: SampleAPI.Response?) {
                    if (null != response && response.result == "OK") {
                        if (mProgressDialog.isShowing)
                            mProgressDialog.dismiss()
                        Mesibo.deleteUserProfile(mUser, true, false)
                        activity!!.finish()
                        //UIManager.launchMesiboContacts(getActivity(), 0, 0, Intent.FLAG_ACTIVITY_CLEAR_TOP, null);

                    }

                }
            })
        }

        val switchCompat = v.findViewById<View>(R.id.up_mute_switch) as SwitchCompat
        switchCompat.isChecked = mUser!!.isMuted
        switchCompat.setOnCheckedChangeListener { buttonView, isChecked ->
            mUser!!.toggleMute()
            Mesibo.setUserProfile(mUser, false)
        }

        val linearLayout = v.findViewById<View>(R.id.up_open_media) as LinearLayout
        linearLayout.setOnClickListener {
            if (mGalleryData!!.size > 0) {
                for (i in mGalleryData!!.indices.reversed()) {
                    val tempdata = mGalleryData!![i]
                    if (tempdata.getmPhotoCount() == 0)
                        mGalleryData!!.remove(tempdata)
                }
                /*
                    Intent fbIntent = new Intent(getActivity(),
                            AlbumStartActivity.class);
                    startActivity(fbIntent);*/

                UIManager.launchAlbum(activity!!, mGalleryData!!)
            }
        }


        return v
    }


    private fun addThumbnailToGallery(fileInfo: Mesibo.FileInfo) {
        var thumbnailView: View? = null
        val path = fileInfo.path
        mThumbnailMediaFiles!!.add(path)
        if (mThumbnailMediaFiles!!.size < MAX_THUMBNAIL_GALERY_SIZE) {
            if (null != path) {
                thumbnailView = getThumbnailView(fileInfo.image, if (fileInfo.type == VIDEO_FILE) true else false)
                /*
                if (isImageFile(path)) {
                    thumbnailView = getThumbnailView(fileInfo.image, false);
                } else if (isVideoFile(path)) {
                    thumbnailView = getThumbnailView(fileInfo.image, (fileInfo.type == 2 ? true:false));
                }*/
                if (null != thumbnailView) {
                    thumbnailView.isClickable = true
                    thumbnailView.tag = mMediaFilesCounter - 1
                    thumbnailView.setOnClickListener { v ->
                        val index = v.tag as Int
                        val path = mThumbnailMediaFiles!![index]
                        UIManager.launchImageViewer(activity!!, mThumbnailMediaFiles!!, index)
                    }
                    mGallery!!.addView(thumbnailView)
                }
            }
        }
    }


    internal fun getThumbnailView(bm: Bitmap, isVideo: Boolean?): View {
        val layoutInflater = LayoutInflater.from(activity)
        val view = layoutInflater.inflate(R.layout.video_layer_layout_horizontal_gallery, null, false)
        val thumbpic = view.findViewById<View>(R.id.mp_thumbnail) as ImageView
        thumbpic.setImageBitmap(bm)
        //thumbpic.setScaleType(ImageView.ScaleType.CENTER_CROP);
        val layer = view.findViewById<View>(R.id.video_layer) as ImageView
        layer.visibility = if (isVideo!!) VISIBLE else GONE
        val metrics = DisplayMetrics()
        activity!!.windowManager.defaultDisplay.getMetrics(metrics)
        val width = (metrics.widthPixels - 50) / 5 //number of pics in media view
        view.layoutParams = ViewGroup.LayoutParams(width, width)
        return view
    }


    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater) {


        inflater.inflate(R.menu.user_profile_menu, menu)



        if (null != mUser && mUser!!.isBlocked) {

            val menuItem = menu!!.findItem(R.id.menu_user_block)
            if (null != menuItem)
                menuItem.title = "Unblock"

        }



        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                activity!!.onBackPressed()
                return true
            }

            R.id.menu_user_block -> {


                //Check if user is blocked or unblocked, handle accordingly
                if (null != mUser && mUser!!.isBlocked) {

                    mUser!!.blockMessages(false)
                    mUser!!.blockGroupMessages(false)
                    Mesibo.setUserProfile(mUser, false)

                } else {


                    val mUserProfile = Mesibo.UserProfile()

                    mUserProfile.address = mUser!!.address
                    mUserProfile.name = mUser!!.name
                    mUserProfile.picturePath = mUser!!.picturePath
                    mUserProfile.status = mUser!!.status

                    mUserProfile.blockMessages(true)
                    mUserProfile.blockGroupMessages(true)
                    Mesibo.setUserProfile(mUserProfile, false)

                }





                (activity as Activity).invalidateOptionsMenu()

                return true
            }
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        if (context is OnFragmentInteractionListener) {
            mListener = context
        } else {
            throw RuntimeException(context!!.toString() + " must implement OnFragmentInteractionListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        mListener = null
    }

    override fun Mesibo_onMessage(messageParams: Mesibo.MessageParams, bytes: ByteArray): Boolean {
        return false
    }

    override fun Mesibo_onMessageStatus(params: Mesibo.MessageParams) {

    }

    override fun Mesibo_onActivity(messageParams: Mesibo.MessageParams, i: Int) {

    }

    override fun Mesibo_onLocation(messageParams: Mesibo.MessageParams, location: Mesibo.Location) {

    }

    override fun Mesibo_onFile(messageParams: Mesibo.MessageParams, fileInfo: Mesibo.FileInfo) {
        mMediaCardView!!.visibility = VISIBLE
        mMediaFilesCounter++
        mMediaCounterView!!.text = "$mMediaFilesCounter\u3009 "
        val newPhoto = AlbumPhotosData()
        newPhoto.setmPictueUrl(fileInfo.path)
        newPhoto.setmSourceUrl(fileInfo.path)
        val tempAlbum: AlbumListData
        var index = 0
        if (fileInfo.type == VIDEO_FILE)
            index = 1
        else if (fileInfo.type != IMAGE_FILE)
            index = 2
        tempAlbum = mGalleryData!![index]

        if (tempAlbum.getmPhotosList() == null) {
            val newPhotoList = ArrayList<AlbumPhotosData>()
            tempAlbum.setmPhotosList(newPhotoList)
        }
        if (tempAlbum.getmPhotosList().size == 0) {
            tempAlbum.setmAlbumPictureUrl(fileInfo.path)
        }
        tempAlbum.getmPhotosList().add(newPhoto)
        tempAlbum.setmPhotoCount(tempAlbum.getmPhotosList().size)
        addThumbnailToGallery(fileInfo)
    }

    fun parseGroupMembers(members: String?): Boolean {
        var members = members
        if (TextUtils.isEmpty(members))
            return false

        val s = members!!.split("\\:".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        if (null == s || s.size < 2)
            return false

        try {
            mAdminCount = Integer.parseInt(s[0])
        } catch (nfe: NumberFormatException) {
            return false
        }

        members = s[1]
        val users = members.split("\\,".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                ?: return false

        val phone = SampleAPI.phone
        //MUST not happen
        if (TextUtils.isEmpty(phone))
            return false

        mGroupMemberList.clear()
        //only owner can delete group
        mExitGroupText!!.text = if (phone!!.equals(users[0], ignoreCase = true)) "Delete Group" else "Exit Group"

        mAdmin = 0

        val unknownProfiles = ArrayList<Mesibo.UserProfile>()

        for (i in users.indices) {
            var up: Mesibo.UserProfile? = null
            val peer = users[i]
            if (phone.equals(peer, ignoreCase = true)) {
                up = Mesibo.getSelfProfile()
                if (i < mAdminCount)
                    mAdmin = 1
            }

            if (null == up)
                up = Mesibo.getUserProfile(peer, 0)

            if (null == up) {

                // we can do this instead but the problem is all unknown people will be shown in
                // contact list
                up = Mesibo.createUserProfile(peer, 0, peer)

            }

            if (up!!.flag and Mesibo.UserProfile.FLAG_TEMPORARY.toLong() > 0)
                unknownProfiles.add(up)

            mGroupMemberList.add(up)
        }

        if (unknownProfiles.size > 0) {
            SampleAPI.addContacts(unknownProfiles, true)
        }

        mAddMemebers.visibility = if (mAdmin != 0) VISIBLE else GONE
        mAdapter!!.notifyDataSetChanged()
        return true
    }

    override fun Mesibo_onUserProfileUpdated(userProfile: Mesibo.UserProfile, i: Int, b: Boolean) {
        if (null != mAdapter)
            mAdapter!!.notifyDataSetChanged()
    }

    override fun onResume() {
        super.onResume()
        if (mUser!!.groupid > 0 && 0L == mUser!!.flag and Mesibo.UserProfile.FLAG_DELETED.toLong()) {
            mExitGroupCard!!.visibility = VISIBLE
            mGroupMemebersCard!!.visibility = VISIBLE
            mStatusPhoneCard!!.visibility = GONE
            mAddMemebers.setOnClickListener {
                val bundle = Bundle()
                bundle.putLong("groupid", mUser!!.groupid)
                UIManager.launchMesiboContacts(activity!!, 0, 4, 0, bundle)
                activity!!.finish()
            }

            if (TextUtils.isEmpty(mUser!!.groupMembers)) {
                mProgressDialog.show()
                SampleAPI.getGroup(mUser!!.groupid, object : SampleAPI.ResponseHandler() {
                    override fun HandleAPIResponse(response: SampleAPI.Response?) {

                        if (mProgressDialog.isShowing)
                            mProgressDialog.dismiss()

                        if (null == response || response.result != "OK") {
                            mGroupMemebersCard!!.visibility = GONE
                            mExitGroupText!!.visibility = GONE
                            return
                        }

                        parseGroupMembers(response.members)
                    }
                })
            } else
                parseGroupMembers(mUser!!.groupMembers)

        } else {
            mExitGroupCard!!.visibility = GONE
            mGroupMemebersCard!!.visibility = GONE
            mStatusPhoneCard!!.visibility = VISIBLE

            if (null == mUser!!.status) {
                mll.visibility = GONE
            } else {
                mll.visibility = VISIBLE
                mStatus.text = mUser!!.status
            }

            //statusTime.setText((mUserProfiledata.lastActive));
            mStatusTime.text = ""
            mMobileNumber.text = mUser!!.address
            mPhoneType.text = "Mobile"
        }

    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     *
     *
     * See the Android Training lesson [Communicating with Other Fragments](http://developer.android.com/training/basics/fragments/communicating.html) for more information.
     */
    interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        fun onFragmentInteraction(uri: Uri)
    }

    inner class GroupMemeberAdapter(context: Context, list: ArrayList<Mesibo.UserProfile>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        private var mContext: Context? = null
        private var mDataList: ArrayList<Mesibo.UserProfile>? = null

        init {
            this.mContext = context
            mDataList = list
        }

        inner class GroupMembersCellsViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            var mBoundString: String? = null
            var mView: View? = null
            var mContactsProfile: ImageView? = null
            var mContactsName: TextView? = null
            var mAdminTextView: TextView? = null
            var mContactsStatus: EmojiconTextView? = null

            init {
                mView = view
                mContactsProfile = view.findViewById<View>(R.id.sp_rv_profile) as ImageView
                mContactsName = view.findViewById<View>(R.id.sp_rv_name) as TextView
                mContactsStatus = view.findViewById<View>(R.id.sp_memeber_status) as EmojiconTextView
                mAdminTextView = view.findViewById<View>(R.id.admin_info) as TextView
            }
        }


        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.showprofile_group_member_rv_item, parent, false)
            return GroupMembersCellsViewHolder(view)

        }


        override fun onBindViewHolder(holderr: RecyclerView.ViewHolder, position: Int) {
            val pos = position
            val user = mDataList!![position]
            val holder = holderr as GroupMembersCellsViewHolder



            if (!TextUtils.isEmpty(user.name))
                holder.mContactsName!!.text = user.name
            else
                holder.mContactsName!!.text = ""
            val b: Bitmap? = null
            val filePath = Mesibo.getUserProfilePicturePath(user, Mesibo.FileInfo.TYPE_AUTO)

            val memberImage = BitmapFactory.decodeFile(filePath)
            if (null != memberImage)
                holder.mContactsProfile!!.setImageDrawable(MesiboUtils.getRoundImageDrawable(memberImage))
            else
                holder.mContactsProfile!!.setImageDrawable(MesiboUtils.getRoundImageDrawable(mDefaultProfileBmp))

            if (position < mAdminCount) {
                holder.mAdminTextView!!.visibility = VISIBLE
            } else {
                holder.mAdminTextView!!.visibility = GONE
            }

            if (TextUtils.isEmpty(user.status)) {
                user.status = ""
            }
            holder.mContactsStatus!!.text = user.status

            // only admin can have menu, also owner can't be deleted

            holder.mView!!.setOnClickListener(View.OnClickListener {
                val profile = mDataList!![position]

                if (0 == mAdmin || 0 == position) {
                    if (profile.isSelfProfile) {
                        return@OnClickListener
                    }

                    MesiboUI.launchMessageView(activity!!, profile.address, profile.groupid)
                    activity!!.finish()
                    return@OnClickListener
                }

                val items = ArrayList<String>()

                val makeAdmin: Boolean
                if (position >= mAdminCount) {
                    items.add("Make Admin")
                    makeAdmin = true
                } else {
                    items.add("Remove Admin")
                    makeAdmin = false
                }

                // don't allow self messaging or self delete member
                if (!profile.isSelfProfile) {
                    items.add("Delete member")
                    items.add("Message")
                }

                val cs = items.toTypedArray<CharSequence>()

                val builder = AlertDialog.Builder(mContext!!)
                //builder.setTitle("Select The Action");
                builder.setItems(cs, DialogInterface.OnClickListener { dialog, item ->
                    //Delete member
                    if (item == 1) {
                        val members = arrayOfNulls<String>(1)
                        members[0] = mDataList!![position].address
                        mProgressDialog.show()
                        SampleAPI.editMembers(mUser!!.groupid, members, true, object : SampleAPI.ResponseHandler() {
                            override fun HandleAPIResponse(response: SampleAPI.Response?) {
                                if (mProgressDialog.isShowing)
                                    mProgressDialog.dismiss()

                                if (null == response) {
                                    //TBD, show error
                                    return
                                }

                                if (response.result == "OK") {
                                    mDataList!!.removeAt(position)
                                    notifyItemRemoved(position)
                                    notifyDataSetChanged()
                                }

                            }
                        })

                    } else if (item == 0) {
                        mProgressDialog.show()
                        SampleAPI.setAdmin(mUser!!.groupid, mDataList!![position].address, makeAdmin, object : SampleAPI.ResponseHandler() {
                            override fun HandleAPIResponse(response: SampleAPI.Response?) {
                                if (mProgressDialog.isShowing)
                                    mProgressDialog.dismiss()

                                if (null == response) {
                                    //TBD, show error
                                    return
                                }

                                if (response.result == "OK") {
                                    parseGroupMembers(mUser!!.groupMembers)
                                }

                            }
                        })

                    } else if (2 == item) {
                        MesiboUI.launchMessageView(activity!!, profile.address, profile.groupid)
                        activity!!.finish()
                        return@OnClickListener
                    }
                })
                builder.show()
            })

        }


        override fun getItemCount(): Int {
            return mDataList!!.size
        }

    }

    companion object {
        private val MAX_THUMBNAIL_GALERY_SIZE = 35

        private var mUser: Mesibo.UserProfile? = null
        private val VIDEO_FILE = 2
        private val IMAGE_FILE = 1
        private val OTHER_FILE = 2

        private var mDefaultProfileBmp: Bitmap? = null

        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @return A new instance of fragment ShowProfileFragment.
         */
        // TODO: Rename and change types and number of parameters
        fun newInstance(userdata: Mesibo.UserProfile): ShowProfileFragment {
            val fragment = ShowProfileFragment()
            mUser = userdata
            return fragment
        }


        fun isImageFile(path: String): Boolean {
            val mimeType = URLConnection.guessContentTypeFromName(path)
            return mimeType != null && mimeType.startsWith("image")
        }


        fun isVideoFile(path: String): Boolean {
            val mimeType = URLConnection.guessContentTypeFromName(path)
            return mimeType != null && mimeType.startsWith("video")
        }
    }
}// Required empty public constructor

private fun SampleAPI.editMembers(groupid: Long, members: Array<String?>, remove: Boolean, handler: SampleAPI.ResponseHandler) {
    return
}
