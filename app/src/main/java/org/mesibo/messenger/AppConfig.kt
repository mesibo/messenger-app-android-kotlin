package org.mesibo.messenger

import android.app.backup.BackupManager
import android.content.Context
import android.content.SharedPreferences
import android.content.SharedPreferences.Editor
import android.util.Log
import com.google.gson.Gson
import org.mesibo.messenger.SampleAPI.Invite

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
 * https://github.com/mesibo/messenger-app-android
 *
 */
class AppConfig(c: Context) {
    class Configuration {
        @JvmField
        var token = ""
        @JvmField
        var phone = ""
        @JvmField
        var cc = ""
        @JvmField
        var invite: Invite? = null
        @JvmField
        var uploadurl: String? = null
        @JvmField
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

    var isFirstTime = false
    private val mContext: Context
    var mSharedPref: SharedPreferences? = null
    private fun backup() {
        bm!!.dataChanged()
    }

    // We could use TAG also - to save/retrieve settings
    val appSetting: Unit
        get() {
            val gson = Gson()
            val json = mSharedPref!!.getString(systemPreferenceKey, "")
            config = gson.fromJson(json, Configuration::class.java)
            if (null == config) config = Configuration()
        }

    private fun putAppSetting(spe: Editor) {
        val gson = Gson()
        val json = gson.toJson(config)
        spe.putString(systemPreferenceKey, json)
        spe.commit()
    }

    fun saveSettings(): Boolean {
        Log.d(TAG, "Updating RMS .. ")
        return try {
            synchronized(mSharedPref!!) {
                val spe = mSharedPref!!.edit()
                putAppSetting(spe)
                backup()
                return true
            }
        } catch (e: Exception) {
            Log.d(TAG, "Unable to updateRMS(): " + e.message)
            false
        }

        //return false;
    }

    fun setStringValue(key: String?, value: String?): Boolean {
        return try {
            synchronized(mSharedPref!!) {
                val poEditor = mSharedPref!!.edit()
                poEditor.putString(key, value)
                poEditor.commit()
                //backup();
                return true
            }
        } catch (e: Exception) {
            Log.d(TAG, "Unable to set long value in RMS:" + e.message)
            false
        }
    }

    fun getStringValue(key: String?, defaultVal: String): String? {
        return try {
            synchronized(mSharedPref!!) {
                return if (mSharedPref!!.contains(key)) mSharedPref!!.getString(key, defaultVal) else defaultVal
            }
        } catch (e: Exception) {
            Log.d(TAG, "Unable to fet long value in RMS:" + e.message)
            defaultVal
        }
    }

    fun setLongValue(key: String?, value: Long): Boolean {
        return try {
            synchronized(mSharedPref!!) {
                val poEditor = mSharedPref!!.edit()
                poEditor.putLong(key, value)
                poEditor.commit()
                return true
            }
        } catch (e: Exception) {
            Log.d(TAG, "Unable to set long value in RMS:" + e.message)
            false
        }
    }

    fun getLongValue(key: String?, defaultVal: Long): Long {
        return try {
            synchronized(mSharedPref!!) {
                return if (mSharedPref!!.contains(key)) mSharedPref!!.getLong(key, defaultVal) else defaultVal
            }
        } catch (e: Exception) {
            Log.d(TAG, "Unable to fet long value in RMS:" + e.message)
            defaultVal
        }
    }

    fun setIntValue(key: String?, value: Int): Boolean {
        return try {
            synchronized(mSharedPref!!) {
                val poEditor = mSharedPref!!.edit()
                poEditor.putInt(key, value)
                poEditor.commit()
                return true
            }
        } catch (e: Exception) {
            Log.d(TAG, "Unable to set int value in RMS:" + e.message)
            false
        }
    }

    fun getIntValue(key: String?, defaultVal: Int): Int {
        return try {
            synchronized(mSharedPref!!) {
                return if (mSharedPref!!.contains(key)) mSharedPref!!.getInt(key, defaultVal) else defaultVal
            }
        } catch (e: Exception) {
            Log.d(TAG, "Unable to get int value in RMS:" + e.message)
            defaultVal
        }
    }

    fun setBooleanValue(key: String?, value: Boolean?): Boolean {
        return try {
            synchronized(mSharedPref!!) {
                val poEditor = mSharedPref!!.edit()
                poEditor.putBoolean(key, value!!)
                poEditor.commit()
                return true
            }
        } catch (e: Exception) {
            Log.d(TAG, "Unable to set long value in RMS:" + e.message)
            false
        }
    }

    fun getBooleanValue(key: String, defaultVal: Boolean): Boolean {
        Log.d(TAG, "Getting long value for $key in RMS directly")
        return try {
            synchronized(mSharedPref!!) {
                return if (mSharedPref!!.contains(key)) mSharedPref!!.getBoolean(key, defaultVal) else defaultVal
            }
        } catch (e: Exception) {
            Log.d(TAG, "Unable to fet long value in RMS:" + e.message)
            defaultVal
        }
    } /*
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

    companion object {
        private const val TAG = "AppSettings"
        const val sharedPrefKey = "org.mesibo.messenger"
        private const val systemPreferenceKey = "settings"

        //System Specific Preferences - does not change across logins
        var config: Configuration? = Configuration()
        private var bm: BackupManager? = null
        var instance: AppConfig? = null
            private set

        @JvmStatic
        fun reset() {
            //CrashLogs.setUID(0);

            //mConfig = new Configuration(); // we should not create new instance else app using this instance will have
            // and issue
            config!!.reset()
            save()
            instance!!.backup()
        }

        @JvmStatic
        fun save() {
            instance!!.saveSettings()
        }

        fun getConfig(): Any {
            return config!!;
        }

    }

    init {
        instance = this
        mContext = c
        bm = BackupManager(mContext)
        mSharedPref = c.getSharedPreferences(sharedPrefKey, Context.MODE_PRIVATE)
        isFirstTime = false
        if (!mSharedPref!!.contains(systemPreferenceKey)) {
            isFirstTime = true
        }
        appSetting
        if (isFirstTime) saveSettings()
    }
}