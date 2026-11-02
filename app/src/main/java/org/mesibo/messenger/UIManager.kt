package org.mesibo.messenger

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.ComponentActivity
import com.mesibo.api.Mesibo
import com.mesibo.calls.api.MesiboCall
import com.mesibo.messaging.MesiboDialogs
import com.mesibo.messaging.MesiboDialogs.OnSelect
import com.mesibo.messaging.MesiboUI
import com.mesibo.messaging.MesiboUI.MesiboEditProfileOptions
import com.mesibo.messaging.MesiboUI.MesiboUserListScreenOptions
import com.mesibo.uihelper.MesiboLoginOptions
import com.mesibo.uihelper.MesiboPermissions
import com.mesibo.uihelper.MesiboUiHelper
import com.mesibo.uihelper.MesiboWelcomeOptions
import com.mesibo.uihelper.NotificationPermission
import org.mesibo.messenger.AppSettings.SettingsActivity

object UIManager {
    private const val BRAND_COLOR = -0xff7975

    fun launchStartupActivity(context: Context, skipWelcome: Boolean) {
        val intent = Intent(context, StartUpActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.putExtra(StartUpActivity.SKIPTOUR, skipWelcome)
        context.startActivity(intent)
    }

    fun launchMesibo(context: Context?, opts: MesiboUserListScreenOptions?) {
        MesiboUI.launchUserList(context, opts)
    }

    /**
     * Requests the permissions the mesibo UI needs, then notification permission, then (once)
     * full-screen incoming calls, then calls onReady. If the required permissions are denied, the
     * app closes (as before).
     */
    fun requestPermissions(activity: ComponentActivity, onReady: Runnable) {
        MesiboPermissions.request(
            activity, MesiboPermissions.requiredPermissions(),
            "mesibo requires Storage and Contacts permissions so that you can send messages and make calls to your contacts. Please grant to continue!"
        ) { granted: Boolean ->
            if (!granted) {
                val o = MesiboDialogs.Options()
                o.message = "mesibo will close now since the required permissions were not granted"
                o.cancelable = false
                o.onPositive = Runnable { activity.finishAffinity() }
                MesiboDialogs.show(activity, o)
                return@request
            }
            // notifications are optional; continue either way
            NotificationPermission.request(
                activity,
                "Allow notifications so that you are informed about new messages and calls."
            ) { notify: Boolean -> requestFullScreenCalls(activity, onReady) }
        }
    }

    /**
     * Android 14+: incoming calls show full screen on a locked phone only if allowed (Google Play
     * grants it to apps declared as calling apps). Asked once; the app continues either way.
     */
    private fun requestFullScreenCalls(activity: Activity, onReady: Runnable) {
        val prefs = activity.getSharedPreferences("mesibo_prompts", Context.MODE_PRIVATE)
        if (activity.isFinishing || MesiboCall.canShowFullScreenCall(activity)
            || prefs.getBoolean("fullscreen_calls_asked", false)
        ) {
            onReady.run()
            return
        }
        prefs.edit().putBoolean("fullscreen_calls_asked", true).apply()

        val o = MesiboDialogs.Options()
        o.title = "Show incoming calls"
        o.message =
            "Allow mesibo to show incoming calls on the full screen, even when your phone is locked. Otherwise, calls only show as a notification."
        o.positiveLabel = "Allow"
        o.negativeLabel = "Not now"
        o.cancelable = false
        o.onPositive = Runnable { MesiboCall.requestFullScreenCallPermission(activity) }
        o.onDismiss = onReady
        MesiboDialogs.show(activity, o)
    }

    // -- theme (Settings): stored in preferences as it is applied at startup, before mesibo starts --
    private val THEME_NAMES = arrayOf("System default", "Light", "Dark")

    /** MesiboUI.THEME_SYSTEM (default), THEME_LIGHT or THEME_DARK  */
    fun getTheme(context: Context): Int {
        return context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
            .getInt("theme", MesiboUI.THEME_SYSTEM)
    }

    fun getThemeName(context: Context): String {
        val t = getTheme(context)
        return if ((t >= 0 && t < THEME_NAMES.size)) THEME_NAMES[t] else THEME_NAMES[0]
    }

    fun applyTheme(context: Context) {
        MesiboUI.setTheme(getTheme(context))
    }

    /** choose System default / Light / Dark; applied the next time the app starts  */
    fun chooseTheme(activity: Activity, onChanged: Runnable?) {
        val o = MesiboDialogs.Options()
        o.title = "Theme"
        o.items = THEME_NAMES
        o.sheet = true
        o.onSelect = OnSelect { which: Int ->
            val theme =
                if ((0 == which)) MesiboUI.THEME_SYSTEM else (if (1 == which) MesiboUI.THEME_LIGHT else MesiboUI.THEME_DARK)
            if (theme == getTheme(activity)) return@OnSelect
            activity.getSharedPreferences("app_settings", Context.MODE_PRIVATE).edit()
                .putInt("theme", theme).apply()
            onChanged?.run()
            MesiboDialogs.alert(
                activity,
                "Theme",
                "The new theme will be applied when you restart the app."
            )
        }
        MesiboDialogs.show(activity, o)
    }

    fun launchUserSettings(context: Context) {
        val intent = Intent(context, SettingsActivity::class.java)
        context.startActivity(intent)
    }

    fun launchEditProfile(context: Context, newUser: Boolean) {
        MesiboUI.setEditProfileListener { activity, profile, newUser ->
            if (newUser) // first sign-in: continue to the app via startup (permissions)
                launchStartupActivity(activity, true)
        }

        Mesibo.getSelfProfile().setSearchable(true)

        val opts = MesiboEditProfileOptions()
        opts.newUser = newUser
        // not from a screen (e.g. the app context): the screen needs a new task
        if (context !is android.app.Activity)
            opts.activityFlags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
        MesiboUI.launchEditProfile(context, opts)
    }

    fun launchWelcome(context: Context?) {
        val w = MesiboWelcomeOptions()
        w.logoResId = R.drawable.mesibo_logo_white
        w.title = "Messaging & Calls in your apps"
        w.description =
            "Add messaging, video and voice calls & conferencing in your apps in no time."
        w.githubUrl = "https://github.com/mesibo"
        w.buttonText = "Get Started"

        // colors: uihelper defaults (black background, white text and button)
        MesiboUiHelper.launchWelcome(
            context,
            w,
            loginOptions(),
            MesiboListeners.instance
        )
    }

    fun launchLogin(context: Context?) {
        MesiboUiHelper.launchLogin(context, loginOptions(), MesiboListeners.instance)
    }

    private fun loginOptions(): MesiboLoginOptions {
        val o = MesiboLoginOptions()

        val country = Mesibo.getPhoneContactsManager().countryCode
        o.defaultCountryCode =
            if ((null != country && country.countryPhoneCode > 0)) country.countryPhoneCode else 1

        // we do not send verification codes; testers generate them from the mesibo console
        o.note =
            "This demo app will NOT send verification code. You can generate yourself from the mesibo console https://console.mesibo.com"
        o.noteIconResId = R.drawable.ic_touch_app
        o.noteColor = -0x4bacf7
        o.noteBorderColor = 0x55ffb900
        o.noteBackgroundColor = 0x14ffb900
        o.codeMessage = "Enter the verification code for %PHONE% generated from the mesibo console"

        o.backgroundColor = -0x1
        o.titleColor = -0xe8d8d9
        o.messageColor = -0x99999a
        o.inputColor = -0xe8d8d9
        o.accentColor = BRAND_COLOR
        o.buttonColor = BRAND_COLOR
        o.buttonTextColor = -0x1
        return o
    }
}
