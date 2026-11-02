package org.mesibo.messenger.AppSettings

import android.graphics.Color
import android.os.Build
import android.view.View
import android.view.Window
import androidx.annotation.ColorInt
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.mesibo.messaging.MesiboUI

/**
 * Applies the mesibo UI colors to the app's own screens so that Settings/About look the same as
 * the mesibo messaging screens: MesiboUiDefaults when set, else the mesibo theme colors (which
 * follow the system light/dark mode). Also lays the screen out edge-to-edge, as Android 15
 * enforces: the app bar extends under the status bar and the content stays clear of the
 * navigation bar.
 */
internal object MesiboToolbarStyle {
    fun apply(activity: AppCompatActivity, toolbar: Toolbar, appBar: View, content: View?) {
        val opt = MesiboUI.getUiDefaults()
        val bar = if ((0 != opt.toolbarColor)) opt.toolbarColor
        else ContextCompat.getColor(activity, com.mesibo.messaging.R.color.mesibo_bar)
        val text = if (0 != opt.toolbarTextColor) opt.toolbarTextColor
        else if (0 != opt.toolbarColor) if (isLight(opt.toolbarColor)) -0x22000000 else -0x1
        else ContextCompat.getColor(activity, com.mesibo.messaging.R.color.mesibo_on_bar)
        val background =
            ContextCompat.getColor(activity, com.mesibo.messaging.R.color.mesibo_background)

        appBar.setBackgroundColor(bar)
        toolbar.setBackgroundColor(bar)
        toolbar.setTitleTextColor(text)
        toolbar.setSubtitleTextColor(text)

        val nav = toolbar.navigationIcon
        nav?.mutate()?.setTint(text)
        val overflow = toolbar.overflowIcon
        overflow?.mutate()?.setTint(text)

        val window = activity.window
        WindowCompat.setDecorFitsSystemWindows(window, false)
        if (Build.VERSION.SDK_INT < 35) transparentSystemBars(window)

        val c = WindowCompat.getInsetsController(window, window.decorView)
        c.isAppearanceLightStatusBars = isLight(bar)
        c.isAppearanceLightNavigationBars = isLight(background)

        ViewCompat.setOnApplyWindowInsetsListener(appBar) { v: View, insets: WindowInsetsCompat ->
            val i =
                insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            v.setPadding(i.left, i.top, i.right, 0)
            insets
        }
        ViewCompat.setOnApplyWindowInsetsListener(content!!) { v: View, insets: WindowInsetsCompat ->
            val i =
                insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            v.setPadding(i.left, 0, i.right, i.bottom)
            insets
        }
    }

    // Android 15+ draws the bars transparent by itself (and deprecates these calls)
    @Suppress("deprecation")
    private fun transparentSystemBars(window: Window) {
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
    }

    fun isLight(@ColorInt color: Int): Boolean {
        return ColorUtils.calculateLuminance(color) > 0.5
    }
}
