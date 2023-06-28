package org.mesibo.messenger;

/** Copyright (c) 2021 Mesibo
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

import com.google.gson.Gson;

import android.app.backup.BackupManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

public class AppConfig {
    private static final String TAG = "AppSettings";
    public static final String sharedPrefKey = "org.mesibo.messenger";
    private static final String systemPreferenceKey = "mesibo-app-settings";

    public static class Configuration {
        public String token = "";
        public String phone = "";
        public SampleAPI.Invite invite = null;
        public String uploadurl = null;
        public String downloadurl = null;

        public void reset() {
            token = "";
            phone = "";
            invite = null;
            uploadurl = "";
            downloadurl = "";
        }
    }

    //System Specific Preferences - does not change across logins
    public static Configuration mConfig = new Configuration();

    private boolean firstTime = false;
    private Context mContext;
    SharedPreferences mSharedPref = null;


    public static AppConfig getInstance() {
        return _instance;
    }

    public static Configuration getConfig() {
        return mConfig;
    }

    private static AppConfig _instance  = null;
    public AppConfig(Context c) {
        _instance = this;
        mContext = c;

        mSharedPref = c.getSharedPreferences(sharedPrefKey, Context.MODE_PRIVATE);
        firstTime = false;
        if (!mSharedPref.contains(systemPreferenceKey)) {
            firstTime = true;
        }

        getAppSetting();

        if (firstTime)
            saveSettings();
    }

    private void backup() {
    }

    public Boolean isFirstTime() {
        return firstTime;
    }

    // We could use TAG also - to save/retrieve settings
    public void getAppSetting() {
        Gson gson = new Gson();
        String json = mSharedPref.getString(systemPreferenceKey, "");
        mConfig = gson.fromJson(json, Configuration.class);

        if(null == mConfig)
            mConfig = new Configuration();
    }

    private void putAppSetting(SharedPreferences.Editor spe) {
        Gson gson = new Gson();

        String json = gson.toJson(mConfig);
        spe.putString(systemPreferenceKey, json);
        spe.commit();
    }

    public static void reset() {
        mConfig.reset();
        save();
        getInstance().backup();
    }


    public static void save() {
        getInstance().saveSettings();
    }

    public boolean saveSettings() {
        Log.d(TAG, "Updating RMS .. ");
        try {
            synchronized (mSharedPref) {
                SharedPreferences.Editor spe = mSharedPref.edit();
                putAppSetting(spe);
                backup();
                return true;
            }
        } catch (Exception e) {
            Log.d(TAG, "Unable to updateRMS(): " + e.getMessage());
            return false;
        }

    }

    public boolean setStringValue(String key, String value) {
        try {
            synchronized (mSharedPref) {
                SharedPreferences.Editor poEditor = mSharedPref.edit();
                poEditor.putString(key, value);
                poEditor.commit();
                return true;
            }
        } catch (Exception e) {
            Log.d(TAG, "Unable to set long value in RMS:" + e.getMessage());
            return false;
        }
    }

    public String getStringValue(String key, String defaultVal) {
        try {
            synchronized (mSharedPref) {
                if (mSharedPref.contains(key))
                    return mSharedPref.getString(key, defaultVal);
                return defaultVal;
            }
        } catch (Exception e) {
            Log.d(TAG, "Unable to fet long value in RMS:" + e.getMessage());
            return defaultVal;
        }
    }

    public boolean setLongValue(String key, long value) {
        try {
            synchronized (mSharedPref) {
                SharedPreferences.Editor poEditor = mSharedPref.edit();
                poEditor.putLong(key, value);
                poEditor.commit();
                return true;
            }
        } catch (Exception e) {
            Log.d(TAG, "Unable to set long value in RMS:" + e.getMessage());
            return false;
        }
    }

    public long getLongValue(String key, long defaultVal) {
        try {
            synchronized (mSharedPref) {
                if (mSharedPref.contains(key))
                    return mSharedPref.getLong(key, defaultVal);
                return defaultVal;
            }
        } catch (Exception e) {
            Log.d(TAG, "Unable to fet long value in RMS:" + e.getMessage());
            return defaultVal;
        }
    }

    public boolean setIntValue(String key, int value) {
        try {
            synchronized (mSharedPref) {
                SharedPreferences.Editor poEditor = mSharedPref.edit();
                poEditor.putInt(key, value);
                poEditor.commit();
                return true;
            }
        } catch (Exception e) {
            Log.d(TAG, "Unable to set int value in RMS:" + e.getMessage());
            return false;
        }
    }

    public int getIntValue(String key, int defaultVal) {

        try {
            synchronized (mSharedPref) {
                if (mSharedPref.contains(key))
                    return mSharedPref.getInt(key, defaultVal);
                return defaultVal;
            }
        } catch (Exception e) {
            Log.d(TAG, "Unable to get int value in RMS:" + e.getMessage());
            return defaultVal;
        }
    }

    public boolean setBooleanValue(String key, Boolean value) {
        try {
            synchronized (mSharedPref) {
                SharedPreferences.Editor poEditor = mSharedPref.edit();
                poEditor.putBoolean(key, value);
                poEditor.commit();
                return true;
            }
        } catch (Exception e) {
            Log.d(TAG, "Unable to set long value in RMS:" + e.getMessage());
            return false;
        }
    }


};

