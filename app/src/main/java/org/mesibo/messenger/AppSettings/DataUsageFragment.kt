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


import android.content.SharedPreferences
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.preference.PreferenceCategory
import android.support.v7.preference.PreferenceFragmentCompat
import android.support.v7.preference.PreferenceManager
import android.support.v7.preference.PreferenceScreen
import android.support.v7.preference.SwitchPreferenceCompat

import org.mesibo.messenger.SampleAPI

import org.mesibo.messenger.R

class DataUsageFragment : PreferenceFragmentCompat(), SharedPreferences.OnSharedPreferenceChangeListener {

    internal lateinit var sharedPreferences: SharedPreferences


    override fun onCreatePreferences(bundle: Bundle, s: String) {
        //add xml
        val ab = (activity as AppCompatActivity).supportActionBar
        ab!!.setDisplayHomeAsUpEnabled(true)
        ab.title = "Data usage settings"

        addPreferencesFromResource(R.xml.data_usage)
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(activity!!)

        val preference = findPreference("auto")
        val preferenceScreen = findPreference("preferenceScreen") as PreferenceScreen
        val myPrefCatcell = findPreference("preferenceCategorycell") as PreferenceCategory
        val myPrefCatwifi = findPreference("preferenceCategorywifi") as PreferenceCategory
        val myPrefCatroam = findPreference("preferenceCategoryroam") as PreferenceCategory

        //temporary
        preferenceScreen.removePreference(myPrefCatcell)
        preferenceScreen.removePreference(myPrefCatwifi)
        preferenceScreen.removePreference(myPrefCatroam)
    }

    override fun onResume() {
        super.onResume()
        //unregister the preferenceChange listener
        preferenceScreen.sharedPreferences
                .registerOnSharedPreferenceChangeListener(this)
        displaySwitches()
    }


    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {

        val preferenceScreen = findPreference("preferenceScreen") as PreferenceScreen
        val myPrefCatcell = findPreference("preferenceCategorycell") as PreferenceCategory
        val myPrefCatwifi = findPreference("preferenceCategorywifi") as PreferenceCategory
        val myPrefCatroam = findPreference("preferenceCategoryroam") as PreferenceCategory

        val preference = findPreference(key)
        if (preference is SwitchPreferenceCompat && key.equals("auto", ignoreCase = true)) {

            val enabled = preference.isChecked
            SampleAPI.mediaAutoDownload = enabled

            try {
                //when this is null
                myPrefCatcell.isEnabled = !enabled
                myPrefCatwifi.isEnabled = !enabled
                myPrefCatroam.isEnabled = !enabled
            } catch (e: Exception) {
            }


        }

    }

    override fun onPause() {
        super.onPause()
        //unregister the preference change listener
        preferenceScreen.sharedPreferences
                .unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        //unregister event bus.
    }


    fun displaySwitches() {
        val preference = findPreference("auto")

        val autoDownload = SampleAPI.mediaAutoDownload
        //preference.setEnabled(autoDownload);
        val datausage = preference as SwitchPreferenceCompat
        datausage.isChecked = autoDownload
    }

}