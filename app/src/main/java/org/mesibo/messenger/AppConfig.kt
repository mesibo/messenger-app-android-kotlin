package org.mesibo.messenger

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

import com.google.gson.Gson

import android.app.backup.BackupManager
import android.content.Context
import android.content.SharedPreferences
import android.util.Log

class AppConfig(private val mContext: Context) {

    private var firstTime = false
    internal var mSharedPref: SharedPreferences? = null

    val isFirstTime: Boolean?
        get() = firstTime

    class Configuration {
        var token = ""
        var phone = ""
        var cc = ""
        var invite: SampleAPI.Invite? = null
        var uploadurl: String? = null
        var downloadurl: String? = null

        fun reset() {
            token = ""
            phone = ""
            cc = ""
            invite = null
            uploadurl = ""
            downloadurl = ""
        }
    }

    init {
        instance = this
        bm = BackupManager(mContext)
        mSharedPref = mContext.getSharedPreferences(sharedPrefKey, Context.MODE_PRIVATE)
        firstTime = false
        if (!mSharedPref!!.contains(systemPreferenceKey)) {
            Log.d(TAG, "First time Launch")
            firstTime = true
        }

        if(!firstTime){
            Log.d(TAG, "Loading from Shared Preferences")
        }

        getAppSetting()

        if (firstTime)
            saveSettings()
    }

    private fun backup() {
        bm!!.dataChanged()
    }

    // We could use TAG also - to save/retrieve settings
    fun getAppSetting() {
        val gson = Gson()
        var json = mSharedPref?.getString(systemPreferenceKey, "")
        Log.d("MesiboKotlin", "================> SHARED PREFERENCES"+json.toString())

        /**Fix**/
        if(!json!!.isEmpty() && json.toString().length!=0)
            config = gson.fromJson(json, Configuration::class.java)

        if (null == config)
            config = Configuration()
    }

    private fun putAppSetting(spe: SharedPreferences.Editor) {
        val gson = Gson()

        val json = gson.toJson(config)
        spe.putString(systemPreferenceKey, json)
        spe.commit()
    }


    fun saveSettings(): Boolean {
        Log.d(TAG, "Updating RMS .. ")
        try {
            mSharedPref?.let {
                synchronized(it){
                    val spe = mSharedPref!!.edit()
                    putAppSetting(spe)
                    backup()
                    return true
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "Unable to updateRMS(): " + e.message)
            return false
        }

        return false;
    }

    fun setStringValue(key: String, value: String): Boolean {
        try {
            mSharedPref?.let {
                synchronized (it){
                    val poEditor = mSharedPref!!.edit()
                    poEditor.putString(key, value)
                    poEditor.commit()
                    //backup();
                    return true
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "Unable to set long value in RMS:" + e.message)
            return false
        }
        return false
    }

    fun getStringValue(key: String, defaultVal: String): String? {
        try {
            mSharedPref?.let {
                synchronized (it){
                                                                                                                                return if (mSharedPref!!.contains(key)) mSharedPref!!.getString(key, defaultVal) else defaultVal
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "Unable to fet long value in RMS:" + e.message)
            return defaultVal
        }

        return defaultVal

    }

    fun setLongValue(key: String, value: Long): Boolean {
        try {
            mSharedPref?.let {
                synchronized (it){
                    val poEditor = mSharedPref!!.edit()
                    poEditor.putLong(key, value)
                    poEditor.commit()
                    return true
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "Unable to set long value in RMS:" + e.message)
            return false
        }

        return false
    }

    fun getLongValue(key: String, defaultVal: Long): Long {
        try {
            mSharedPref?.let {
                synchronized (it){
                    return if (mSharedPref!!.contains(key)) mSharedPref!!.getLong(key, defaultVal) else defaultVal
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "Unable to fet long value in RMS:" + e.message)
            return defaultVal
        }

        return defaultVal
    }

    fun setIntValue(key: String, value: Int): Boolean {
        try {
            mSharedPref?.let {
                synchronized (it) {
                    val poEditor = mSharedPref!!.edit()
                    poEditor.putInt(key, value)
                    poEditor.commit()
                    return true
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "Unable to set int value in RMS:" + e.message)
            return false
        }

        return false
    }

    fun getIntValue(key: String, defaultVal: Int): Int {

        try {
            mSharedPref?.let {
                synchronized (it){
                    return if (mSharedPref!!.contains(key)) mSharedPref!!.getInt(key, defaultVal) else defaultVal
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "Unable to get int value in RMS:" + e.message)
            return defaultVal
        }

        return defaultVal
    }

    fun setBooleanValue(key: String, value: Boolean?): Boolean {
        try {
            mSharedPref?.let {
                synchronized (it){
                    val poEditor = mSharedPref!!.edit()
                    poEditor.putBoolean(key, value!!)
                    poEditor.commit()
                    return true
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "Unable to set long value in RMS:" + e.message)
            return false
        }

        return false
    }

    fun getBooleanValue(key: String, defaultVal: Boolean?): Boolean? {
        Log.d(TAG, "Getting long value for $key in RMS directly")
        try {
            mSharedPref?.let {
                synchronized (it) {
                    return if (mSharedPref!!.contains(key)) mSharedPref!!.getBoolean(key, defaultVal!!) else defaultVal
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "Unable to get long value in RMS:" + e.message)
            return defaultVal
        }

        return defaultVal
    }

    companion object {
        private val TAG = "AppSettings"
        val sharedPrefKey = "org.mesibo.messenger.kotlin"
        private val systemPreferenceKey = "settings"

        val MESSAGECONTEXTACTION_FORWARD = 1
        val MESSAGECONTEXTACTION_REPLY = 2
        val MESSAGECONTEXTACTION_RESEND = 3
        val MESSAGECONTEXTACTION_DELETE = 4
        val MESSAGECONTEXTACTION_COPY = 5
        val MESSAGECONTEXTACTION_FAVORITE = 6
        val MESSAGECONTEXTACTION_SHARE = 7

        val Grey_color = -0x363637
        val Transparent = 0x00000000

        val statusList = "statusList"

        //System Specific Preferences - does not change across logins
        @JvmStatic var config: Configuration = Configuration()

        private var bm: BackupManager? = null

        var instance: AppConfig? = null

        fun reset() {
            //CrashLogs.setUID(0);

            //mConfig = new Configuration(); // we should not create new instance else app using this instance will have
            // and issue
            config!!.reset()
            save()
            instance!!.backup()
        }


        fun save() {
            instance!!.saveSettings()
        }
    }

    /*
    private static String serializeVector(Vector<String> vss) {
        ByteArrayOutputStream arrayOutputStream = new ByteArrayOutputStream();
        ObjectOutputStream out;
        try {
            out = new ObjectOutputStream(arrayOutputStream);
            out.writeObject(vss);
            out.close();
            arrayOutputStream.close();
        } catch (IOException e) {
        }
        return DataUtils.bytesToHex(arrayOutputStream.toByteArray());
    }

    private static Vector<String> deSerializeVector(String s) {
        Vector<String> vss = null;
        ByteArrayInputStream byteArray = null;
        byteArray = new ByteArrayInputStream(DataUtils.hexToBytes(s));

        ObjectInputStream in;
        try {
            in = new ObjectInputStream(byteArray);
            vss = (Vector<String>) in.readObject();
        } catch (Exception e) {
        }
        return vss;
    }
    */

}

