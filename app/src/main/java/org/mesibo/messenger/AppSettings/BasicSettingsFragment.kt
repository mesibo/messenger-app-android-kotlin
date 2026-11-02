package org.mesibo.messenger.AppSettings

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.Fragment
import com.mesibo.api.Mesibo
import com.mesibo.api.MesiboProfile
import com.mesibo.api.RoundImageDrawable
import org.mesibo.messenger.R
import org.mesibo.messenger.SampleAPI
import org.mesibo.messenger.UIManager

class BasicSettingsFragment : Fragment() {
    private var mUserName: TextView? = null
    private var mUserStatus: TextView? = null
    private var mUserImage: ImageView? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val v = inflater.inflate(R.layout.fragment_basic_settings, container, false)

        mUserName = v.findViewById(R.id.set_self_user_name)
        mUserStatus = v.findViewById(R.id.set_self_status)
        mUserImage = v.findViewById(R.id.set_user_image)

        v.findViewById<View>(R.id.set_picture_name_status_layout).setOnClickListener { b: View? ->
            UIManager.launchEditProfile(
                requireActivity(),
                false
            )
        }

        val notifyMessages = v.findViewById<SwitchCompat>(R.id.set_notify_messages)
        notifyMessages.isChecked = SampleAPI.messageNotification
        notifyMessages.setOnCheckedChangeListener { b: CompoundButton?, checked: Boolean ->
            SampleAPI.messageNotification = checked
        }

        val notifyGroups = v.findViewById<SwitchCompat>(R.id.set_notify_groups)
        notifyGroups.isChecked = SampleAPI.groupMessageNotification
        notifyGroups.setOnCheckedChangeListener { b: CompoundButton?, checked: Boolean ->
            SampleAPI.groupMessageNotification = checked
        }

        val themeValue = v.findViewById<TextView>(R.id.set_theme_value)
        themeValue.text = UIManager.getThemeName(requireContext())
        v.findViewById<View>(R.id.set_theme_layout).setOnClickListener { b: View? ->
            UIManager.chooseTheme(requireActivity()) {
                themeValue.text = UIManager.getThemeName(requireContext())
            }
        }

        v.findViewById<View>(R.id.set_about_layout).setOnClickListener { b: View? ->
            parentFragmentManager.beginTransaction()
                .replace(R.id.settings_fragment_place, AboutFragment())
                .addToBackStack("about")
                .commit()
        }

        v.findViewById<View>(R.id.privacy_policy_layout).setOnClickListener { b: View? ->
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(PRIVACY_URL)))
            } catch (e: ActivityNotFoundException) {
                // no browser
            }
        }

        v.findViewById<View>(R.id.set_logout_layout).setOnClickListener { b: View? ->
            SampleAPI.startLogout()
            requireActivity().finish()
        }

        return v
    }

    override fun onResume() {
        super.onResume()

        val ab = (requireActivity() as AppCompatActivity).supportActionBar
        if (null != ab) {
            ab.setDisplayHomeAsUpEnabled(true)
            ab.title = "Settings"
        }

        // refreshed on every resume: the profile may have been edited
        val user: MesiboProfile? = Mesibo.getSelfProfile()
        if (null == user) {
            requireActivity().finish()
            return
        }

        val image = user.image.imageOrThumbnail
        if (null != image) mUserImage!!.setImageDrawable(RoundImageDrawable(image))
        mUserName!!.text = user.name
        mUserStatus!!.text = user.getString("status", "")
    }

    companion object {
        private const val PRIVACY_URL = "https://mesibo.com/terms-of-use/"
    }
}
