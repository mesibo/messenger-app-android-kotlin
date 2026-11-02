package org.mesibo.messenger

import android.app.Application
import android.content.Context
import android.util.Log
import com.mesibo.api.Mesibo
import com.mesibo.api.Mesibo.RestartListener
import com.mesibo.calls.api.MesiboCall
import com.mesibo.calls.ui.MesiboCallUi
import com.mesibo.messaging.MesiboUI

class MainApplication : Application(), RestartListener {
    override fun onCreate() {
        super.onCreate()

        // light/dark: as chosen in Settings (default: follow the system), see UIManager.applyTheme
        UIManager.applyTheme(this)

        appContext = applicationContext
        Mesibo.setRestartListener(this)
        AppConfig(this)
        SampleAPI.init(appContext)

        MesiboCallUi.getInstance()
        MesiboCall.getInstance().init(appContext)


        val opt = MesiboUI.getUiDefaults()
        opt.emptyUserListMessage =
            "No messages! Click on the message icon above to start messaging!"
        opt.showAddressInProfileView = true

        val up = MesiboCall.getInstance().defaultUiProperties
        up.showScreenSharing = true
    }

    override fun Mesibo_onRestart() {
        Log.d(TAG, "Mesibo_onRestart")
        // keep the app's current visibility; do not push a visible app to the back
        StartUpActivity.newInstance(this, !Mesibo.isAppInForeground())
    }

    companion object {
        const val TAG: String = "MesiboDemoApplication"
        // set first thing in onCreate
        lateinit var appContext: Context
            private set

        val restartIntent: String
            get() = "org.mesibo.messenger.restart"
    }
}
