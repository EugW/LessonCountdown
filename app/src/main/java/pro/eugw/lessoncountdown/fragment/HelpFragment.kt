package pro.eugw.lessoncountdown.fragment

import android.graphics.BitmapFactory
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_help.*
import pro.eugw.lessoncountdown.BuildConfig
import pro.eugw.lessoncountdown.R
import pro.eugw.lessoncountdown.activity.MainActivity

class HelpFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        return inflater.inflate(R.layout.fragment_help, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        (activity as MainActivity).main_toolbar.menu.clear()
        (activity as MainActivity).main_toolbar.title = getString(R.string.help)
        imageViewUltimate.setImageBitmap(BitmapFactory.decodeStream(resources.openRawResource(R.raw.lc_ic_hires)))
        textViewPP.movementMethod = LinkMovementMethod.getInstance()
        textViewTermsAndConditions.movementMethod = LinkMovementMethod.getInstance()
        textViewDev.movementMethod = LinkMovementMethod.getInstance()
        textViewVKGroup.movementMethod = LinkMovementMethod.getInstance()
        textViewVer.text = BuildConfig.VERSION_NAME
    }
}