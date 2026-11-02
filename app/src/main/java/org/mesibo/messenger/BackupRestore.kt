package org.mesibo.messenger

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.mesibo.api.Mesibo
import com.mesibo.mediapicker.FilePicker
import com.mesibo.messaging.MesiboDialogs
import com.mesibo.messaging.MesiboDialogs.OnSelect
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max

/**
 * Message backups in the app's media folder, Android/media/<package>/Backups, like WhatsApp. It
 * needs no permission and users can see and copy the files with a file manager or a PC. Android
 * removes the folder on uninstall unless the user keeps the app data (hasFragileUserData in the
 * manifest), so restore also accepts a file picked from anywhere (FilePicker in mediapicker).
</package> */
object BackupRestore {
    private const val PREFIX = "mesibo-backup-"
    private const val SUFFIX = ".db"

    /** Back up now / Restore  */
    fun show(activity: Activity) {
        val o = MesiboDialogs.Options()
        o.title = "Backup & Restore"
        o.items = arrayOf("Back up now", "Restore")
        o.sheet = true
        o.onSelect = OnSelect { i: Int ->
            if (0 == i) backup(activity)
            else chooseRestore(activity)
        }
        MesiboDialogs.show(activity, o)
    }

    fun backup(activity: Activity) {
        val dir = backupDir(activity)
        if (null == dir) {
            toast(activity, "Storage not available")
            return
        }
        val name = PREFIX + SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date()) + SUFFIX
        val path = File(dir, name).absolutePath
        val app = activity.applicationContext

        toast(app, "Backing up messages...")
        Thread {
            val result = if ((null != Mesibo.backupDatabase(path))
            ) "Messages backed up to Android/media/" + app.packageName + "/Backups/" + name
            else "Backup failed"
            Handler(Looper.getMainLooper()).post { toast(app, result) }
        }.start()
    }

    /** backups in the backup folder (newest first), and Choose file...  */
    private fun chooseRestore(activity: Activity) {
        val backups = listBackups(activity)
        val items = arrayOfNulls<String>(backups.size + 1)
        for (i in backups.indices) items[i] = describe(backups[i])
        items[backups.size] = "Choose file..."

        val o = MesiboDialogs.Options()
        o.title = if (backups.isEmpty()) "No backups found on this phone" else "Restore from"
        o.items = items
        o.sheet = true
        o.onSelect = OnSelect { i: Int ->
            if (i < backups.size) confirmRestore(activity, backups[i], false)
            else pickFile(activity)
        }
        MesiboDialogs.show(activity, o)
    }

    private fun pickFile(activity: Activity) {
        if (activity !is ComponentActivity) return
        val o = FilePicker.Options()
        o.extensions = arrayOf("db")
        o.copyToCache = true // restore needs a file path
        FilePicker.pick(activity, o, object : FilePicker.Listener {
            override fun onFilePicked(uri: Uri, name: String, file: File) {
                if (null == uri) return  // cancelled

                if (null == file) toast(activity, "Could not read the file")
                else confirmRestore(activity, file, true)
            }

            override fun onFileRejected(uri: Uri, name: String) {
                MesiboDialogs.alert(
                    activity,
                    "Not a backup",
                    "$name is not a mesibo backup (.db) file"
                )
            }
        })
    }

    private fun confirmRestore(activity: Activity, file: File, deleteAfter: Boolean) {
        val o = MesiboDialogs.Options()
        o.title = "Restore messages?"
        o.message =
            "Your current messages will be replaced by the backup. mesibo will restart after restoring."
        o.positiveLabel = "Restore"
        o.negativeLabel = "Cancel"
        o.destructive = true
        o.onPositive = Runnable { restore(activity, file, deleteAfter) }
        if (deleteAfter) o.onNegative = Runnable { file.delete() }
        MesiboDialogs.show(activity, o)
    }

    private fun restore(activity: Activity, file: File, deleteAfter: Boolean) {
        val rv = Mesibo.restoreDatabase(file.absolutePath)
        if (deleteAfter)
            file.delete()
        if (0 != rv) {
            MesiboDialogs.alert(
                activity,
                "Restore failed",
                "The file is not a valid mesibo backup ($rv)"
            )
            return
        }

        val o = MesiboDialogs.Options()
        o.title = "Messages restored"
        o.message = "mesibo will now restart."
        o.cancelable = false
        o.onPositive = Runnable { restart(activity) }
        MesiboDialogs.show(activity, o)
    }

    /** messages and profiles are loaded in memory, so start a fresh process  */
    private fun restart(activity: Activity) {
        val launch = activity.packageManager.getLaunchIntentForPackage(activity.packageName)
        if (null != launch && null != launch.component) activity.startActivity(
            Intent.makeRestartActivityTask(
                launch.component
            )
        )
        activity.finishAffinity()
        Runtime.getRuntime().exit(0)
    }

    private fun backupDir(context: Context): File? {
        var base: File? = null
        val media = context.externalMediaDirs
        if (null != media && media.size > 0) base = media[0]
        if (null == base) base = context.getExternalFilesDir(null)
        if (null == base) return null
        val dir = File(base, "Backups")
        if (!dir.isDirectory && !dir.mkdirs()) return null
        return dir
    }

    /** backups in the backup folder, newest first */
    private fun listBackups(context: Context): List<File> {
        val files = backupDir(context)?.listFiles { _, n -> n.startsWith(PREFIX) && n.endsWith(SUFFIX) }
            ?: return emptyList()
        return files.sortedByDescending { it.lastModified() }
    }

    private fun describe(f: File): String {
        val `when` = SimpleDateFormat(
            "d MMM yyyy, HH:mm",
            Locale.getDefault()
        ).format(Date(f.lastModified()))
        val kb = max(1.0, (f.length() / 1024).toDouble()).toLong()
        return if (kb >= 1024) `when` + "  ·  " + (kb / 1024) + " MB" else "$`when`  ·  $kb KB"
    }

    private fun toast(c: Context, s: String) {
        Toast.makeText(c, s, Toast.LENGTH_LONG).show()
    }
}
