package pro.eugw.lessoncountdown.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.android.volley.toolbox.Volley
import kotlinx.android.synthetic.main.activity_main.*
import pro.eugw.lessoncountdown.R
import pro.eugw.lessoncountdown.activity.MainActivity

class KundelikFragment : Fragment() {

    private lateinit var mActivity: MainActivity
    private lateinit var token: String

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_kundelik_loading, container, false)
    }

    override fun onStart() {
        super.onStart()
        mActivity = activity as MainActivity
        mActivity.main_toolbar.title = getString(R.string.kundelik)
        mActivity.main_toolbar.menu.clear()
        token = mActivity.prefs.getString("kundelikToken", "")!!
        if (token.length < 5) {
            val queue = Volley.newRequestQueue(context)
            
        } else {
            if (view != null)
                (view as ViewGroup).removeAllViews()
            layoutInflater.inflate(R.layout.fragment_kundelik_login, view as ViewGroup)
        }
    }
}