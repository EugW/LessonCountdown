package pro.eugw.lessoncountdown.fragment.lil

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import kotlinx.android.synthetic.main.fragment_kundelik_login.*
import org.json.JSONObject
import pro.eugw.lessoncountdown.R
import pro.eugw.lessoncountdown.activity.MainActivity

class KundelikLoginFragment : DialogFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_kundelik_login, container, false)
    }

    override fun onStart() {
        super.onStart()
        val url = "https://api.kundelik.kz/v1/authorizations/bycredentials"
        val jsonDetails = JSONObject()
        jsonDetails.put("username", editTextKundelikLogin.text)
        jsonDetails.put("password", editTextKundelikPassword.text)
        jsonDetails.put("client_id", "387d44e3e0c94265a9e4a4caaad5111c")
        jsonDetails.put("client_secret", "8a7d709cfdbb4047b0ea8947afe89d67")
        jsonDetails.put("scope", "CommonInfo,ContactInfo,FriendsAndRelatives,EducationalInfo,SocialInfo,Files,Wall,Messages,Schools,Relatives,EduGroups,Lessons,Marks,EduWorks,Avatar")
        buttonKundelikLogin.setOnClickListener {
            loglay.visibility = View.GONE
            progressBar.visibility = View.VISIBLE
            (activity as MainActivity).queue.add(JsonObjectRequest(Request.Method.POST, url, jsonDetails,
                    Response.Listener { response ->
                        Toast.makeText(context, "Success: $response", Toast.LENGTH_LONG).show()
                        dismiss()
                    },
                    Response.ErrorListener { error ->
                        Toast.makeText(context, "ERROR get: ${error.message}", Toast.LENGTH_LONG).show()
                        dismiss()
                    }
            ))
        }
    }
}