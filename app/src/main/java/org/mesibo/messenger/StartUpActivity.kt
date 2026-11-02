package org.mesibo.messenger

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import com.mesibo.api.Mesibo
import com.mesibo.messaging.MesiboUI.MesiboUserListScreenOptions

/**
 * Trampoline activity: routes to login or the user list and then finishes.
 *
 * It must never be left alive in the task, otherwise bringing the task to the
 * front (launcher icon, notification tap) shows its empty layout - the
 * "blank startup" screen. Hence:
 * - every launch, including re-delivery via onNewIntent(), routes to the next screen
 * - the activity always finishes, whether or not the Play update check succeeds
 * It is not noHistory: the permission and update dialogs cover it and must deliver results.
 */
class StartUpActivity : AppCompatActivity() {
    private var mRunInBackground = false
    private var mUpdateFlowStarted = false

    // Play in-app update flow; whatever the result, this trampoline finishes
    private val mUpdateLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { _: ActivityResult -> finish() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIntent(intent)
    }

    // singleTop: a launch while this activity is on top arrives here instead of onCreate.
    // Previously such launches were ignored, leaving the blank screen visible.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        if (intent.getBooleanExtra(INTENTEXIT, false)) {
            finish()
            return
        }

        // Only honour a background start when the app is genuinely not visible; a user
        // tapping the launcher must never get the task pushed to the back.
        mRunInBackground = (intent.getBooleanExtra(STARTINBACKGROUND, false)
                && !Mesibo.isAppInForeground()) && (Mesibo.getTimestamp() - Mesibo.getStartTimestamp() <= 30000)

        Log.d(TAG, "Starting " + (if (mRunInBackground) "in background" else "in foreground"))

        if (mRunInBackground) moveTaskToBack(true)

        startNextActivity()
    }

    private fun startNextActivity() {
        if (TextUtils.isEmpty(SampleAPI.token)) {
            if (intent.getBooleanExtra(SKIPTOUR, false)) UIManager.launchLogin(
                this
            )
            else UIManager.launchWelcome(this)
            fadeTransition()
            onNextActivityStarted()
            return
        }

        val opts = MesiboUserListScreenOptions()
        opts.keepRunning = true
        opts.startInBackground = mRunInBackground

        // no permission prompts when starting in background
        if (mRunInBackground) {
            UIManager.launchMesibo(this, opts)
            onNextActivityStarted()
            return
        }

        UIManager.requestPermissions(this) {
            UIManager.launchMesibo(this, opts)
            onNextActivityStarted()
        }
    }

    @Suppress("deprecation") // pre-34 branch
    private fun fadeTransition() {
        if (Build.VERSION.SDK_INT >= 34) overrideActivityTransition(
            OVERRIDE_TRANSITION_OPEN,
            android.R.anim.fade_in,
            android.R.anim.fade_out
        )
        else overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    private fun onNextActivityStarted() {
        if (mRunInBackground || mUpdateFlowStarted) finish()
        else checkUpdates()
    }

    /* Testing tips:
       https://stackoverflow.com/questions/57534545/appupdatemanager-startupdateflowforresult-not-showing-dialog
     */
    private fun checkUpdates() {
        val appUpdateManager = AppUpdateManagerFactory.create(this)

        // Previously only a success listener was attached: on sideloaded/debug builds, devices
        // without Play, or network errors, the task failed and finish() was never called.
        appUpdateManager.appUpdateInfo
            .addOnSuccessListener { info: AppUpdateInfo ->
                if (!startUpdateFlow(appUpdateManager, info)) finish()
            }
            .addOnFailureListener { e: Exception ->
                Log.d(TAG, "Update check failed: $e")
                finish()
            }
    }

    private fun startUpdateFlow(appUpdateManager: AppUpdateManager, info: AppUpdateInfo): Boolean {
        if (info.updateAvailability() != UpdateAvailability.UPDATE_AVAILABLE) return false
        val type = if (info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) AppUpdateType.IMMEDIATE
        else if (info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)) AppUpdateType.FLEXIBLE
        else return false

        try {
            mUpdateFlowStarted = appUpdateManager.startUpdateFlowForResult(
                info, mUpdateLauncher,
                AppUpdateOptions.newBuilder(type).build()
            )
        } catch (e: Exception) {
            Log.d(TAG, "Unable to start update flow: $e")
            mUpdateFlowStarted = false
        }
        return mUpdateFlowStarted
    }

    companion object {
        private const val TAG = "MesiboStartupActivity"
        const val INTENTEXIT: String = "exit"
        const val SKIPTOUR: String = "skipTour"
        const val STARTINBACKGROUND: String = "startinbackground"

        fun newInstance(context: Context, startInBackground: Boolean) {
            val i = Intent(context, StartUpActivity::class.java)
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            i.putExtra(STARTINBACKGROUND, startInBackground)
            context.startActivity(i)
        }
    }
}
