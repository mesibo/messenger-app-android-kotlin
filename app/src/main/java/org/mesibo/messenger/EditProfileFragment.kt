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


import android.Manifest
import android.annotation.SuppressLint
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.view.menu.MenuBuilder
import android.support.v7.view.menu.MenuPopupHelper
import android.text.Editable
import android.text.InputFilter
import android.text.TextUtils
import android.text.TextWatcher
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView

import com.mesibo.api.Mesibo
import com.mesibo.api.MesiboUtils
import com.mesibo.emojiview.EmojiconEditText
import com.mesibo.emojiview.EmojiconGridView
import com.mesibo.emojiview.EmojiconsPopup
import com.mesibo.mediapicker.MediaPicker
import com.mesibo.messaging.MesiboActivity

import org.mesibo.messenger.Utils.AppUtils

import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException

import android.app.Activity.RESULT_OK
import android.content.ContentValues.TAG

@Suppress("NAME_SHADOWING")
class EditProfileFragment : android.support.v4.app.Fragment(), MediaPicker.ImageEditorListener, Mesibo.FileTransferListener {
    var mView: View? = null
    //private RoundedImageView mProfileImage;
    private var mProfileImage: ImageView? = null

    private var mProfileButton: ImageView? = null
    private val mTempFilePath = Mesibo.getFilePath(Mesibo.FileInfo.TYPE_PROFILEIMAGE) + "myProfile.jpg"

    private var mProgressDialog: ProgressDialog? = null

    internal lateinit var mEmojiNameEditText: EmojiconEditText
    internal lateinit var mEmojiStatusEditText: EmojiconEditText

    internal lateinit var mEmojiNameBtn: ImageView
    internal lateinit var mEmojiStatusBtn: ImageView

    internal lateinit var mNameCharCounter: TextView
    internal lateinit var mStatusCharCounter: TextView
    private var mProfile: Mesibo.UserProfile? = Mesibo.getSelfProfile()

    internal lateinit var mHost: Fragment
    internal lateinit var mSaveBtn: LinearLayout
    internal lateinit var mPhoneNumber: TextView
    @SuppressLint("HandlerLeak")
    private val mHandler = object : SampleAPI.ResponseHandler() {
        override fun HandleAPIResponse(response: SampleAPI.Response?) {
            Log.d(TAG, "Response: " + response!!)

            //http://stackoverflow.com/questions/22924825/view-not-attached-to-window-manager-crash
            if (null == activity)
                return

            if (/*&& !getActivity().isDestroyed()*/  mProgressDialog!!.isShowing)
                mProgressDialog!!.dismiss()

            if (null == response)
                return


            if (response.op == "upload") {
                if (null != SampleAPI.token && response.result == "OK") {
                    mProfile = Mesibo.getSelfProfile()

                    if (TextUtils.isEmpty(mProfile!!.picturePath)) {
                        setUserPicture()
                        return
                    }

                    //TBD, copy original picture so that download is not required - this may be having issue as
                    // we uploaded cropped version
                    val profilePath = Mesibo.getFilePath(Mesibo.FileInfo.TYPE_PROFILEIMAGE) + response.photo
                    if (Mesibo.renameFile(mTempFilePath, profilePath, true)) {
                    }

                    setUserPicture()
                }

            } else if (response.op == "profile") {
                if (null != SampleAPI.token && response.result == "OK") {
                    mProfile!!.name = mEmojiNameEditText.text.toString()
                    mProfile!!.status = mEmojiStatusEditText.text.toString()
                    Mesibo.setSelfProfile(mProfile)

                    if (mSettingsMode!!) {
                        activity!!.onBackPressed()
                    } else {
                        val myIntent = Intent(activity, MesiboActivity::class.java)
                        myIntent.putExtra("homebtn", true)
                        startActivity(myIntent)
                    }
                }

            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>, grantResults: IntArray) {

        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                MediaPicker.launchPicker(activity, MediaPicker.TYPE_CAMERAIMAGE)

            } else {
                //TBD, show alert that you can't continue
                UIManager.showAlert(activity, TITLE_PERMISON_CAMERA_FAIL, MSG_PERMISON_CAMERA_FAIL)

            }
            return

        }

        // other 'case' lines to check for other
        // permissions this app might request

    }

    fun activateInSettingsMode() {
        mSettingsMode = true
    }

    override fun Mesibo_onFileTransferProgress(file: Mesibo.FileInfo): Boolean {
        if (100 == file.progress)
            setUserPicture()

        return true
    }

    internal fun setUserPicture() {
        val filePath = Mesibo.getUserProfilePicturePath(mProfile, Mesibo.FileInfo.TYPE_AUTO)

        val b: Bitmap?
        if (Mesibo.fileExists(filePath)) {
            b = BitmapFactory.decodeFile(filePath)
            if (null != b) {
                mProfileImage!!.setImageDrawable(MesiboUtils.getRoundImageDrawable(b))
            }
        } else {
            //TBD, getActivity.getresource crashes sometime if activity is closing
            mProfileImage!!.setImageDrawable(MesiboUtils.getRoundImageDrawable(BitmapFactory.decodeResource(MainApplication.appContext?.resources, com.mesibo.messaging.R.drawable.default_user_image)))
        }
    }

    @SuppressLint("RestrictedApi")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val v = inflater.inflate(R.layout.fragment_register_new_profile, container, false)

        if (null == mProfile) {
            //TBD, set warning
            activity!!.finish()
            return v
        }


        val ab = (activity as AppCompatActivity).supportActionBar
        if (null != ab) {
            ab.setDisplayHomeAsUpEnabled(true)
            ab.title = "Edit profile details"
        }
        mView = v

        mHost = this
        mProgressDialog = AppUtils.getProgressDialog(activity, "Please wait...")
        mPhoneNumber = v.findViewById<View>(R.id.profile_self_phone) as TextView
        mPhoneNumber.text = mProfile!!.address

        mSaveBtn = v.findViewById<View>(R.id.register_profile_save) as LinearLayout
        mSaveBtn.setOnClickListener(View.OnClickListener {
            val name = mEmojiNameEditText.text.toString()

            if (false && name.length < MIN_NAME_CHAR) {
                openDialogue("Name can not be less than 3 characters", "Change Name")
                return@OnClickListener
            }

            val status = mEmojiStatusEditText.text.toString()
            if (false && status.length < MIN_STATUS_CHAR) {
                openDialogue("Status can not be less than 3 characters", "Change Status")
                return@OnClickListener
            }

            if (TextUtils.isEmpty(mProfile!!.name) || !name.equals(mProfile!!.name, ignoreCase = true) || TextUtils.isEmpty(mProfile!!.status) || !status.equals(mProfile!!.status, ignoreCase = true)) {
                mProgressDialog!!.show()
                mHandler.context = activity
                SampleAPI.setProfile(mEmojiNameEditText.text.toString(), mEmojiStatusEditText.text.toString(), 0, mHandler)
            } else {
                activity!!.finish()
            }
        })

        mProfileImage = v.findViewById<View>(R.id.self_user_image) as ImageView
        Mesibo.startUserProfilePictureTransfer(mProfile, this)
        setUserPicture()

        mProfileImage!!.setOnClickListener { UIManager.launchImageViewer(activity as AppCompatActivity, Mesibo.getUserProfilePicturePath(mProfile, Mesibo.FileInfo.TYPE_AUTO)) }

        mProfileButton = v.findViewById<View>(R.id.edit_user_image) as ImageView
        mProfileButton!!.setOnClickListener { v ->
            val menuBuilder = MenuBuilder(activity!!)
            val inflater = MenuInflater(activity)
            inflater.inflate(R.menu.image_source_menu, menuBuilder)
            val optionsMenu = MenuPopupHelper(activity!!, menuBuilder, v)
            optionsMenu.setForceShowIcon(true)
            menuBuilder.setCallback(object : MenuBuilder.Callback {
                override fun onMenuItemSelected(menu: MenuBuilder, item: MenuItem): Boolean {
                    if (item.itemId == R.id.popup_camera) {
                        if (AppUtils.aquireUserPermission(activity, Manifest.permission.CAMERA, CAMERA_PERMISSION_CODE)) {
                            MediaPicker.launchPicker(activity, MediaPicker.TYPE_CAMERAIMAGE)
                        }
                        return true
                    } else if (item.itemId == R.id.popup_gallery) {
                        MediaPicker.launchPicker(activity, MediaPicker.TYPE_FILEIMAGE)
                        return true
                    } else if (item.itemId == R.id.popup_remove) {
                        mHandler.context = activity
                        SampleAPI.setProfilePicture("", 0, mHandler)
                        return true
                    }
                    return false

                }

                override fun onMenuModeChange(menu: MenuBuilder) {

                }
            })
            optionsMenu.show()
        }


        mNameCharCounter = v.findViewById<View>(R.id.name_char_counter) as TextView
        mNameCharCounter.text = MAX_NAME_CHAR.toString()

        mEmojiNameEditText = v.findViewById<View>(R.id.name_emoji_edittext) as EmojiconEditText
        mEmojiNameEditText.isEnabled = false
        if (!TextUtils.isEmpty(mProfile!!.name))
            mEmojiNameEditText.setText(mProfile!!.name)
        mEmojiNameEditText.filters = arrayOf<InputFilter>(InputFilter.LengthFilter(MAX_NAME_CHAR))
        mEmojiNameEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {

            }

            override fun afterTextChanged(s: Editable) {

                mNameCharCounter.text = (MAX_NAME_CHAR - mEmojiNameEditText.text.length).toString()

            }
        })

        mStatusCharCounter = v.findViewById<View>(R.id.status_char_counter) as TextView
        mStatusCharCounter.text = MAX_STATUS_CHAR.toString()

        mEmojiStatusEditText = v.findViewById<View>(R.id.status_emoji_edittext) as EmojiconEditText
        mEmojiStatusEditText.isEnabled = false
        if (!TextUtils.isEmpty(mProfile!!.status))
            mEmojiStatusEditText.setText(mProfile!!.status)
        mEmojiStatusEditText.filters = arrayOf<InputFilter>(InputFilter.LengthFilter(MAX_STATUS_CHAR))
        mEmojiStatusEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {

            }

            override fun afterTextChanged(s: Editable) {

                mStatusCharCounter.text = (MAX_STATUS_CHAR - mEmojiStatusEditText.text.length).toString()

            }
        })

        mEmojiNameBtn = v.findViewById<View>(R.id.name_emoji_btn) as ImageView
        mEmojiStatusBtn = v.findViewById<View>(R.id.status_emoji_btn) as ImageView

        mEmojiNameBtn.setOnClickListener {
            mEmojiNameBtn.visibility = View.GONE
            mEmojiStatusEditText.isEnabled = true
            mEmojiNameEditText.isEnabled = true

            mEmojiNameEditText.requestFocus()
        }


        val rootView = v.findViewById<View>(R.id.register_new_profile_rootlayout) as FrameLayout
        // Give the topmost view of your activity layout hierarchy. This will be used to measure soft keyboard height
        val popup = EmojiconsPopup(rootView, activity!!)

        //Will automatically set size according to the soft keyboard size
        popup.setSizeForSoftKeyboard()


        val emojilistener = View.OnClickListener { v ->
            var mEmojiEditText = mEmojiNameEditText
            var mEmojiButton = mEmojiNameBtn

            if (v.id == R.id.status_emoji_btn) {
                mEmojiEditText = mEmojiStatusEditText
                mEmojiButton = mEmojiStatusBtn
            }

            //If popup is not showing => emoji keyboard is not visible, we need to show it
            if (!popup.isShowing) {

                mEmojiStatusEditText.isEnabled = true
                mEmojiNameEditText.isEnabled = true

                //If keyboard is visible, simply show the emoji popup
                if (popup.isKeyBoardOpen!!) {
                    popup.showAtBottom()
                    changeEmojiKeyboardIcon(mEmojiButton, R.drawable.ic_keyboard)
                } else {
                    mEmojiEditText.isFocusableInTouchMode = true
                    mEmojiEditText.requestFocus()
                    popup.showAtBottomPending()
                    val inputMethodManager = activity!!.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    inputMethodManager.showSoftInput(mEmojiEditText, InputMethodManager.SHOW_IMPLICIT)
                    changeEmojiKeyboardIcon(mEmojiButton, R.drawable.ic_keyboard)
                }//else, open the text keyboard first and immediately after that show the emoji popup
            } else {
                popup.dismiss()
            }//If popup is showing, simply dismiss it to show the undelying text keyboard
        }


        //mEmojiNameBtn.setOnClickListener(emojilistener);
        // mEmojiStatusBtn.setOnClickListener(emojilistener);

        //If the emoji popup is dismissed, change emojiButton to smiley icon
        popup.setOnDismissListener {
            changeEmojiKeyboardIcon(mEmojiNameBtn, R.drawable.ic_action_edit)
            changeEmojiKeyboardIcon(mEmojiStatusBtn, R.drawable.ic_action_edit)
        }

        //If the text keyboard closes, also dismiss the emoji popup
        popup.setOnSoftKeyboardOpenCloseListener(object : EmojiconsPopup.OnSoftKeyboardOpenCloseListener {

            override fun onKeyboardOpen(keyBoardHeight: Int) {

            }

            override fun onKeyboardClose() {
                //                mEmojiStatusEditText.setEnabled(true);
                //                mEmojiNameEditText.setEnabled(true);
                //
                //                mEmojiNameEditText.requestFocus();
                if (popup.isShowing)
                    popup.dismiss()
            }
        })

        //On emoji clicked, add it to edittext
        popup.setOnEmojiconClickedListener(EmojiconGridView.OnEmojiconClickedListener { emojicon ->
            var mEmojiEditText: EmojiconEditText? = mEmojiNameEditText
            if (mEmojiStatusEditText.hasFocus()) {
                mEmojiEditText = mEmojiStatusEditText
            }

            if (mEmojiEditText == null || emojicon == null) {
                return@OnEmojiconClickedListener
            }


            val start = mEmojiEditText!!.selectionStart
            val end = mEmojiEditText!!.selectionEnd
            if (start < 0) {
                mEmojiEditText!!.append(emojicon.emoji)
            } else {
                mEmojiEditText!!.text.replace(Math.min(start, end),
                        Math.max(start, end), emojicon.emoji, 0,
                        emojicon.emoji.length)
            }
        })

        //On backspace clicked, emulate the KEYCODE_DEL key event
        popup.setOnEmojiconBackspaceClickedListener {
            var mEmojiEditText = mEmojiNameEditText
            if (mEmojiStatusEditText.hasFocus()) {
                mEmojiEditText = mEmojiStatusEditText
            }
            val event = KeyEvent(
                    0, 0, 0, KeyEvent.KEYCODE_DEL, 0, 0, 0, 0, KeyEvent.KEYCODE_ENDCALL)
            mEmojiEditText.dispatchKeyEvent(event)
        }







        return v
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        Log.d("test1", "result2")

        if (RESULT_OK != resultCode)
            return

        val filePath = MediaPicker.processOnActivityResult(activity, requestCode, resultCode, data)
                ?: return


        UIManager.launchImageEditor(activity as AppCompatActivity, MediaPicker.TYPE_FILEIMAGE, -1, "", filePath, false, false, true, true, 600, this)
        //mProgressDialog.show();
    }


    private fun changeEmojiKeyboardIcon(iconToBeChanged: ImageView, drawableResourceId: Int) {
        iconToBeChanged.setImageResource(drawableResourceId)
    }

    fun setImageProfile(bmp: Bitmap) {
        mProfileImage!!.setImageDrawable(MesiboUtils.getRoundImageDrawable(bmp))

    }

    fun openDialogue(title: String, message: String) {
        val alertDialogBuilder = AlertDialog.Builder(activity!!)
        //alertDialogBuilder.setTitle(title);
        alertDialogBuilder.setMessage(message)
        alertDialogBuilder.setPositiveButton("OK"
        ) { arg0, arg1 -> }


        val alertDialog = alertDialogBuilder.create()
        alertDialog.show()
    }

    override fun onImageEdit(i: Int, s: String, filePath: String, bitmap: Bitmap, status: Int) {
        //SampleAPI.setProfilePicture(mProfileFilePath, 0, mHandler);
        if (0 != status) {
            if (mProgressDialog!!.isShowing)
                mProgressDialog!!.dismiss()
            return
        }

        if (!saveBitmpToFilePath(bitmap, mTempFilePath)) {
            if (mProgressDialog!!.isShowing)
                mProgressDialog!!.dismiss()
            return
        }

        mHandler.context = activity
        SampleAPI.setProfilePicture(mTempFilePath, 0, mHandler)
        setImageProfile(bitmap)
    }

    companion object {
        private val MAX_NAME_CHAR = 50
        private val MIN_NAME_CHAR = 3
        private val MAX_STATUS_CHAR = 150
        private val MIN_STATUS_CHAR = 3
        private var mSettingsMode: Boolean? = false

        internal val CAMERA_PERMISSION_CODE = 102

        val TITLE_PERMISON_CAMERA_FAIL = "Permission Denied"
        val MSG_PERMISON_CAMERA_FAIL = "Camera permission was denied by you! Change the permission from settings menu"

        fun getFileDrawable(context: Context, fileName: String): Int {
            var ext = fileName.substring(fileName.lastIndexOf(".") + 1, fileName.length)
            if (ext.length > 3)
                ext = ext.substring(0, 3)
            val checkExistence = context.resources.getIdentifier("file_$ext", "drawable", context.packageName)
            return if (checkExistence != 0) {  // the resouce exists...
                checkExistence
            } else 0

        }

        fun saveBitmpToFilePath(bmp: Bitmap?, filePath: String): Boolean {
            val file = File(filePath)
            var fOut: FileOutputStream? = null
            try {
                fOut = FileOutputStream(file)
            } catch (e: FileNotFoundException) {
                e.printStackTrace()
                return false
            }

            if (null != bmp) {
                bmp.compress(Bitmap.CompressFormat.JPEG, 80, fOut)

                try {
                    fOut.flush()
                } catch (e: IOException) {
                    e.printStackTrace()
                }

                try {
                    fOut.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }

            }

            return true
        }
    }

}// Required empty public constructor

