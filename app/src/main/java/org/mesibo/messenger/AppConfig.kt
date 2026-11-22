package org.mesibo.messenger

import android.content.Context
import android.content.SharedPreferences
import android.text.TextUtils
import android.util.Log
import com.google.gson.Gson
import org.mesibo.messenger.SampleAPI.Invite
import java.util.UUID

class AppConfig(c: Context) {
    class Configuration {
        var token: String = ""
        var phone: String = ""
        var invite: Invite? = null
        var uploadurl: String? = null
        var downloadurl: String? = null
        var uniqueid: String? = null

        fun reset() {
            token = ""
            phone = ""
            invite = null
            uploadurl = ""
            downloadurl = ""
            // uniqueid - DO NOT RESET
        }
    }

    private var firstTime = false
    private val mContext: Context = c
    val mSharedPref: SharedPreferences = c.getSharedPreferences(sharedPrefKey, Context.MODE_PRIVATE)

    init {
        instance = this

        firstTime = !mSharedPref.contains(systemPreferenceKey)

        loadAppSetting()

        // https://developer.android.com/training/articles/user-data-ids
        if (TextUtils.isEmpty(mConfig.uniqueid)) {
            mConfig.uniqueid = UUID.randomUUID().toString()
            saveSettings()
        } else if (firstTime) {
            saveSettings()
        }
    }

    // We could use TAG also - to save/retrieve settings
    private fun loadAppSetting() {
        val json = mSharedPref.getString(systemPreferenceKey, "")
        // fromJson returns null for empty (first start) or invalid settings
        mConfig = Gson().fromJson(json, Configuration::class.java) ?: Configuration()
    }

    private fun putAppSetting(spe: SharedPreferences.Editor) {
        val gson = Gson()

        val json = gson.toJson(mConfig)
        spe.putString(systemPreferenceKey, json)
        spe.apply() // async write; commit() blocked the main thread on every save
    }

    fun saveSettings(): Boolean {
        Log.d(TAG, "Updating RMS .. ")
        try {
            synchronized(mSharedPref) {
                val spe = mSharedPref.edit()
                putAppSetting(spe)
                return true
            }
        } catch (e: Exception) {
            Log.d(TAG, "Unable to updateRMS(): " + e.message)
            return false
        }
    }

    companion object {
        private const val TAG = "AppSettings"
        const val sharedPrefKey: String = "org.mesibo.messenger"
        private const val systemPreferenceKey = "mesibo-app-settings"

        //System Specific Preferences - does not change across logins
        var mConfig: Configuration = Configuration()

        val config: Configuration
            get() = mConfig

        var instance: AppConfig? = null
            private set

        fun reset() {
            // reset in place: callers may hold a reference to mConfig
            mConfig.reset()
            save()
        }


        fun save() {
            instance!!.saveSettings()
        }
    }
}
