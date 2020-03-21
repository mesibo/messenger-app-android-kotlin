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
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.webkit.MimeTypeMap
import android.widget.Toast

import com.mesibo.api.Mesibo
import com.mesibo.calls.MesiboCall
import com.mesibo.mediapicker.AlbumListData
import com.mesibo.mediapicker.MediaPicker

import org.mesibo.messenger.AppSettings.SettingsActivity

import com.mesibo.uihelper.ILoginInterface
import com.mesibo.uihelper.IProductTourListener
import com.mesibo.uihelper.MesiboUiHelper
import com.mesibo.uihelper.MesiboUiHelperConfig
import com.mesibo.uihelper.WelcomeScreen
import com.mesibo.messaging.MesiboUI


import java.util.ArrayList

private var MesiboUiHelperConfig.mPermissionsRequestMessage: String
    get() {return MesiboUiHelperConfig.mPermissionsRequestMessage}
    set(s) {MesiboUiHelperConfig.mPermissionsRequestMessage = s}

private var MesiboUiHelperConfig.mPermissionsDeniedMessage: String
    get() {return MesiboUiHelperConfig.mPermissionsDeniedMessage}
    set(s) {MesiboUiHelperConfig.mPermissionsDeniedMessage = s}

private var MesiboUiHelperConfig.mPermissions: List<String>
    get() {return  MesiboUiHelperConfig.mPermissions}
    set(ls) {MesiboUiHelperConfig.mPermissions = ls}


private var MesiboUiHelperConfig.mSmartLockUrl: String
    get() {return MesiboUiHelperConfig.mSmartLockUrl}
    set(s) {MesiboUiHelperConfig.mSmartLockUrl = s}

private var MesiboUiHelperConfig.mScreenAnimation: Boolean
    get() {return  MesiboUiHelperConfig.mScreenAnimation}
    set(b) {MesiboUiHelperConfig.mScreenAnimation = b}

private var MesiboUiHelperConfig.mSecondaryTextColor: Int
    get() {return  MesiboUiHelperConfig.mSecondaryTextColor}
    set(i) {MesiboUiHelperConfig.mSecondaryTextColor}

private var MesiboUiHelperConfig.mButttonTextColor: Int
    get() {return MesiboUiHelperConfig.mButttonTextColor}
    set(i) { MesiboUiHelperConfig.mButttonTextColor = i}

private var MesiboUiHelperConfig.mButttonColor: Int
    get() {return MesiboUiHelperConfig.mButttonColor}
    set(i) { MesiboUiHelperConfig.mButttonColor = i}

private var MesiboUiHelperConfig.mPrimaryTextColor: Int
    get() { return  MesiboUiHelperConfig.mPrimaryTextColor}
    set(i) { MesiboUiHelperConfig.mPrimaryTextColor = i}

private var MesiboUiHelperConfig.mBackgroundColor: Int
    get() {return  MesiboUiHelperConfig.mBackgroundColor}
    set(i) {MesiboUiHelperConfig.mBackgroundColor = i}

private var MesiboUiHelperConfig.mWelcomeBackgroundColor: Int
    get() {return  MesiboUiHelperConfig.mWelcomeBackgroundColor}
    set(i) {MesiboUiHelperConfig.mWelcomeBackgroundColor = i}

private var MesiboUiHelperConfig.mWelcomeBottomText: String
    get() {return  MesiboUiHelperConfig.mWelcomeBottomText}
    set(s) {MesiboUiHelperConfig.mWelcomeBottomText = s}

private var MesiboUiHelperConfig.mScreens: List<WelcomeScreen>
    get() {return  MesiboUiHelperConfig.mScreens}
    set(res) {MesiboUiHelperConfig.mScreens = res}

object UIManager {
    var mMesiboLaunched = false

    var mProductTourShown = false

    fun launchStartupActivity(context: Context, skipTour: Boolean) {
        val intent = Intent(context, StartUpActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.putExtra(StartUpActivity.SKIPTOUR, skipTour)
        context.startActivity(intent)
    }

    fun launchMesibo(context: Context, flag: Int, startInBackground: Boolean, keepRunningOnBackPressed: Boolean) {
        mMesiboLaunched = true
        MesiboUI.launch(context, flag, startInBackground, keepRunningOnBackPressed)
    }

    fun launchPagerActivty(context: Context) {

        context.startActivity(Intent(context, MesiboMainActivity::class.java))
    }

    fun launchMesiboContacts(context: Context, forwardid: Long, selectionMode: Int, flag: Int, bundle: Bundle) {
        MesiboUI.launchContacts(context, forwardid, selectionMode, flag, bundle)
    }

    fun launchUserProfile(context: Context, groupid: Long, peer: String) {
        val subActivity = Intent(context, ShowProfileActivity::class.java)
        subActivity.putExtra("peer", peer).putExtra("groupid", groupid)
        context.startActivity(subActivity)
    }

    fun launchUserSettings(context: Context) {
        val intent = Intent(context, SettingsActivity::class.java)
        context.startActivity(intent)
    }

    fun launchUserRegistration(context: Context, flag: Int) {
        val subActivity = Intent(context, EditProfileActivity::class.java)
        if (flag > 0)
            subActivity.flags = flag
        context.startActivity(subActivity)
    }

    fun launchImageViewer(context: Activity, filePath: String) {
        MediaPicker.launchImageViewer(context, filePath)
    }

    fun launchImageViewer(context: Activity, files: ArrayList<String>, firstIndex: Int) {
        MediaPicker.launchImageViewer(context, files, firstIndex)
    }

    fun launchImageEditor(context: Context, type: Int, drawableid: Int, title: String, filePath: String, showEditControls: Boolean, showTitle: Boolean, showCropOverlay: Boolean, squareCrop: Boolean, maxDimension: Int, listener: MediaPicker.ImageEditorListener) {
        MediaPicker.launchEditor(context as AppCompatActivity, type, drawableid, title, filePath, showEditControls, showTitle, showCropOverlay, squareCrop, maxDimension, listener)
    }

    fun launchAlbum(context: Activity, albumList: List<AlbumListData>) {
        MediaPicker.launchAlbum(context, albumList)
    }

    fun launchWelcomeactivity(context: Activity, newtask: Boolean, loginInterface: ILoginInterface, tourListener: IProductTourListener) {


        val config = MesiboUiHelperConfig()

        val res = ArrayList<WelcomeScreen>()

        res.add(WelcomeScreen("Messaging in your apps", "Over 79% of all apps require some form of communications. Mesibo is built from ground-up to power this!", 0, R.drawable.welcome, -0xff7975))
        res.add(WelcomeScreen("Messaging, Voice, & Video", "Complete infrastructure with powerful APIs to get you started, rightaway!", 0, R.drawable.videocall, -0xf062a8))
        //res.add(new WelcomeScreen("Plug & Play", "Not just APIs, you can even use pluggable UI modules - Buid in just a few hours", 0, R.drawable.profile, 0xfff4b400));
        res.add(WelcomeScreen("Open Source", "Quickly integrate Mesibo in your own app using freely available source code", 0, R.drawable.opensource_ios, -0xfab59f))

        //res.add(new WelcomeScreen("No Sweat Pricing", "Start free & only pay as you grow!", 0, R.drawable.users, 0xff0f9d58));

        // dummy - requires
        res.add(WelcomeScreen("", ":", 0, R.drawable.welcome, -0xff7975))


        config.mScreens = res
        config.mWelcomeBottomText = "Mesibo will never share your information"
        config.mWelcomeBackgroundColor = -0xff7975

        config.mBackgroundColor = -0x1
        config.mPrimaryTextColor = -0xe8d8d9
        config.mButttonColor = -0xff7975
        config.mButttonTextColor = -0x1
        config.mSecondaryTextColor = -0x99999a

        config.mScreenAnimation = true
        config.mSmartLockUrl = "https://app.mesibo.com"

        val permissions = ArrayList<String>()

        permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        permissions.add(Manifest.permission.READ_CONTACTS)
        config.mPermissions = permissions
        config.mPermissionsRequestMessage = "Mesibo requires Storage and Contacts permissions so that you can send messages and make calls to your contacts. Please grant to continue!"
        config.mPermissionsDeniedMessage = "Mesibo will close now since the required permissions were not granted"


        MesiboUiHelper.setConfig(config)

        if (mMesiboLaunched) {
            launchLogin(context, MesiboListeners.instance)
            return
        }

        mProductTourShown = true
        MesiboUiHelper.launchTour(context, newtask, tourListener)
    }

    fun launchLogin(context: Activity, loginInterface: ILoginInterface) {
        MesiboUiHelper.launchLogin(context, true, 2, loginInterface)
    }

    fun showOnCallProgressGreenBar(view: View) {
        if (MesiboCall.getInstance().isCallInProgress) {

            view.visibility = View.VISIBLE
        } else {
            view.visibility = View.GONE
        }
    }


    @JvmOverloads
    fun showAlert(context: Context?, title: String, message: String, pl: DialogInterface.OnClickListener? = null, nl: DialogInterface.OnClickListener? = null) {
        if (null == context) {
            return  //
        }
        val dialog = android.support.v7.app.AlertDialog.Builder(context)
        dialog.setTitle(title)
        dialog.setMessage(message)
        // dialog.setIcon(android.R.drawable.ic_dialog_alert);
        dialog.setCancelable(true)

        dialog.setPositiveButton(android.R.string.ok, pl)
        dialog.setNegativeButton(android.R.string.cancel, nl)

        try {
            dialog.show()
        } catch (e: Exception) {

        }

    }

    fun openMedia(context: Context, fileUrl: String, filePath: String) {

        val myMime = MimeTypeMap.getSingleton()
        val newIntent = Intent(Intent.ACTION_VIEW)
        val mimeType = myMime.getMimeTypeFromExtension(fileExt(fileUrl))
        newIntent.setDataAndType(Mesibo.uriFromFile(context, filePath), mimeType)
        newIntent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        try {
            context.startActivity(newIntent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, "No App found for this type of file.", Toast.LENGTH_LONG).show()
        }

    }

    fun fileExt(url: String): String? {
        var url = url
        if (url.indexOf("?") > -1) {
            url = url.substring(0, url.indexOf("?"))
        }
        if (url.lastIndexOf(".") == -1) {
            return null
        } else {
            var ext = url.substring(url.lastIndexOf(".") + 1)
            if (ext.indexOf("%") > -1) {
                ext = ext.substring(0, ext.indexOf("%"))
            }
            if (ext.indexOf("/") > -1) {
                ext = ext.substring(0, ext.indexOf("/"))
            }
            return ext.toLowerCase()

        }
    }
}
