package pro.eugw.lessoncountdown.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_kundelik_panel.*
import pro.eugw.lessoncountdown.R
import pro.eugw.lessoncountdown.activity.MainActivity
import pro.eugw.lessoncountdown.fragment.lil.KundelikLoginFragment

class KundelikFragment : Fragment() {

    private lateinit var mActivity: MainActivity
    private lateinit var token: String

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_kundelik_panel, container, false)
    }

    override fun onStart() {
        super.onStart()
        mActivity = activity as MainActivity
        mActivity.main_toolbar.title = getString(R.string.kundelik)
        mActivity.main_toolbar.menu.clear()
        token = mActivity.prefs.getString("kundelikToken", "")!!
        if (token.length < 5) {
            val fragment = KundelikLoginFragment()
            fragment.show(mActivity.supportFragmentManager, "lol")
        } else {
        }
    }

    fun profileRetrieveRequest(): JsonObjectRequest {
        val url = "https://api.kundelik.kz/v1/users/me?access_token=$token"
        return JsonObjectRequest(Request.Method.GET, url, null,
                Response.Listener { response ->
                    textViewKundelikName.text = response.getString("name")
                },
                Response.ErrorListener { error ->

                }
        )
    }

}