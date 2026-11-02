package org.mesibo.messenger.AppSettings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import org.mesibo.messenger.BuildConfig
import org.mesibo.messenger.R

class AboutFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val v = inflater.inflate(R.layout.about, container, false)
        (v.findViewById<View>(R.id.version) as TextView).text =
            "Version " + BuildConfig.BUILD_VERSION
        (v.findViewById<View>(R.id.builddate) as TextView).text =
            "Built " + BuildConfig.BUILD_TIMESTAMP
        return v
    }

    override fun onResume() {
        super.onResume()
        setTitle("About")
    }

    private fun setTitle(title: String) {
        val ab = (requireActivity() as AppCompatActivity).supportActionBar
        if (null != ab) ab.title = title
    }
}
