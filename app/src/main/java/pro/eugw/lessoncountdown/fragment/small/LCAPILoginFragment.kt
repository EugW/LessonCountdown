package pro.eugw.lessoncountdown.fragment.small

import android.os.Bundle
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.edit
import androidx.fragment.app.DialogFragment
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.google.firebase.iid.FirebaseInstanceId
import kotlinx.android.synthetic.main.fragment_kundelik_panel.*
import kotlinx.android.synthetic.main.fragment_lcapi_login.*
import pro.eugw.lessoncountdown.activity.MainActivity
import pro.eugw.lessoncountdown.util.CUSTOM_ADDRESS
import pro.eugw.lessoncountdown.util.LCAPI_TOKEN
import pro.eugw.lessoncountdown.util.SECKEY1
import pro.eugw.lessoncountdown.util.SECKEY2
import java.io.File
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec


class LCAPILoginFragment : DialogFragment() {

    private lateinit var mActivity: MainActivity

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        return inflater.inflate(pro.eugw.lessoncountdown.R.layout.fragment_lcapi_login, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        mActivity = activity as MainActivity
        val prefs = mActivity.prefs
        val host = prefs.getString(CUSTOM_ADDRESS, getString(pro.eugw.lessoncountdown.R.string.host))
        buttonLCAPILogin.setOnClickListener {
            lcapiLoginLayout.visibility = View.GONE
            lcapiProgressBar.visibility = View.VISIBLE
            FirebaseInstanceId.getInstance().instanceId.addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Toast.makeText(mActivity, "Failed ${task.exception}", Toast.LENGTH_SHORT).show()
                }
                val token = task.result!!.token
                val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                val key = Base64.decode(mActivity.prefs.getString(SECKEY1, ""), Base64.NO_WRAP)
                val iv = Base64.decode(mActivity.prefs.getString(SECKEY2, ""), Base64.NO_WRAP)
                cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), IvParameterSpec(iv))
                val encoded = Base64.decode(File(mActivity.filesDir, "encLogDet").readText(), Base64.NO_WRAP)
                val decoded = cipher.doFinal(encoded)
                val credentials = String(decoded).split("|")
                val url = "https://$host/register?username=${editTextLCAPILogin.text}&password=${editTextLCAPIPassword.text}&kusername=${credentials[0]}&kpassword=${credentials[1]}&fcmtoken=$token"
                mActivity.queue.add(JsonObjectRequest(url, null,
                        Response.Listener { response ->
                            Toast.makeText(mActivity, "Success", Toast.LENGTH_SHORT).show()
                            prefs.edit {
                                putString(LCAPI_TOKEN, response.getString("token"))
                            }
                            buttonRegisterNotification.visibility = View.GONE
                            buttonUnregisterNotification.visibility = View.VISIBLE
                            dismiss()
                        },
                        Response.ErrorListener { error ->
                            Toast.makeText(mActivity, "Failed ${error.message}", Toast.LENGTH_SHORT).show()
                            dismiss()
                        }
                ))
            }

        }

    }

}