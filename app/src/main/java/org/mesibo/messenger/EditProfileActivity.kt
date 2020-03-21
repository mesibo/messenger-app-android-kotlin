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

import android.content.Context
import android.content.Intent
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.MenuItem
import android.view.inputmethod.InputMethodManager

import com.mesibo.api.Mesibo


class EditProfileActivity : AppCompatActivity() {


    internal var mRequestingFragment: Fragment? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register_new_profile)


        val registerNewProfileFragment = EditProfileFragment()
        mRequestingFragment = registerNewProfileFragment
        val fm = supportFragmentManager
        val ft = fm.beginTransaction()
        ft.replace(R.id.register_new_profile_fragment_place, registerNewProfileFragment, "null")
        ft.addToBackStack("registerNewProfileFragment")
        ft.commit()
    }

    public override fun onResume() {
        super.onResume()
        if (!Mesibo.setAppInForeground(this, 0x101, true)) {
            finish()
            return
        }
    }

    override fun onPause() {
        super.onPause()
        Mesibo.setAppInForeground(this, 0x101, false)
    }

    override fun onBackPressed() {
        if (supportFragmentManager.backStackEntryCount > 1)
            supportFragmentManager.popBackStackImmediate()

    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        if (imm.isAcceptingText) {
            imm.hideSoftInputFromWindow(currentFocus!!.windowToken, InputMethodManager.HIDE_NOT_ALWAYS)
        }

        for (fragment in supportFragmentManager.fragments) {
            if (fragment is EditProfileFragment && fragment.isVisible())
                fragment.onActivityResult(requestCode, resultCode, data)
        }

    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (mRequestingFragment != null)
            mRequestingFragment!!.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }
}
