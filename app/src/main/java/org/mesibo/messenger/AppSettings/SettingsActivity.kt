package org.mesibo.messenger.AppSettings

import android.os.Bundle
import android.view.MenuItem
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.mesibo.api.Mesibo
import org.mesibo.messenger.R

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val toolbar = findViewById<Toolbar>(R.id.settings_toolbar)
        setSupportActionBar(toolbar)
        val ab = supportActionBar
        ab!!.setDisplayHomeAsUpEnabled(true)
        ab.title = "Settings"

        // colors come from MesiboUiDefaults, same as the mesibo screens
        MesiboToolbarStyle.apply(
            this, toolbar, findViewById(R.id.settings_appbar),
            findViewById(R.id.settings_fragment_place)
        )

        if (null == savedInstanceState) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.settings_fragment_place, BasicSettingsFragment())
                .addToBackStack("account")
                .commit()
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (supportFragmentManager.backStackEntryCount > 1) supportFragmentManager.popBackStackImmediate()
                else finish()
            }
        })
    }

    public override fun onResume() {
        super.onResume()
        Mesibo.setForegroundContext(this, 0x100, true)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressedDispatcher.onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
