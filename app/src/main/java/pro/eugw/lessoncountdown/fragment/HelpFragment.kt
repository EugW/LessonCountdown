package pro.eugw.lessoncountdown.fragment

import android.graphics.BitmapFactory
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_help.view.*
import pro.eugw.lessoncountdown.BuildConfig
import pro.eugw.lessoncountdown.R
import pro.eugw.lessoncountdown.activity.MainActivity

class HelpFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        val v = inflater.inflate(R.layout.fragment_help, container, false)
        v.imageViewUltimate.setImageBitmap(BitmapFactory.decodeStream(resources.openRawResource(R.raw.lc_ic_hires)))
        v.textViewPP.movementMethod = LinkMovementMethod.getInstance()
        v.textViewTermsAndConditions.movementMethod = LinkMovementMethod.getInstance()
        v.textViewDev.movementMethod = LinkMovementMethod.getInstance()
        v.textViewVKGroup.movementMethod = LinkMovementMethod.getInstance()
        v.textViewVer.text = BuildConfig.VERSION_NAME
        return v
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        (activity as MainActivity).main_toolbar.menu.clear()
        (activity as MainActivity).main_toolbar.title = getString(R.string.help)

    }
}