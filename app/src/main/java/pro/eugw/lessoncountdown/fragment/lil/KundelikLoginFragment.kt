package pro.eugw.lessoncountdown.fragment.lil

import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.edit
import androidx.fragment.app.DialogFragment
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import kotlinx.android.synthetic.main.fragment_kundelik_login.*
import org.json.JSONObject
import pro.eugw.lessoncountdown.R
import pro.eugw.lessoncountdown.activity.MainActivity
import pro.eugw.lessoncountdown.util.*
import java.io.File
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec


class KundelikLoginFragment : DialogFragment() {

    private lateinit var mActivity: MainActivity

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_kundelik_login, container, false)
    }

    override fun onStart() {
        super.onStart()
        mActivity = activity as MainActivity
        val url = "https://api.kundelik.kz/v1/authorizations/bycredentials"

        val jsonDetails = JSONObject()
        jsonDetails.put("username", editTextKundelikLogin.text)
        jsonDetails.put("password", editTextKundelikPassword.text)
        jsonDetails.put("client_id", CLIENT_ID)
        jsonDetails.put("client_secret", CLIENT_SECRET)
        jsonDetails.put("scope", KUNDELIK_SCOPE)
        buttonKundelikLogin.setOnClickListener {
            loginLayout.visibility = View.GONE
            progressBar.visibility = View.VISIBLE
            (activity as MainActivity).queue.add(JsonObjectRequest(Request.Method.POST, url, jsonDetails,
                    Response.Listener { response ->
                        Toast.makeText(context, "Success: $response", Toast.LENGTH_LONG).show()
                        showResultDialog("Success", response.toString())
                        mActivity.prefs.edit { putString(KUNDELIK_TOKEN, response.getString("accessToken")) }
                        if (checkBoxSavePassword.isChecked) {
                            val cred = File(mActivity.filesDir, "encLogDet")
                            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                            val secureRandom = SecureRandom()
                            var key = ByteArray(16)
                            var iv = ByteArray(16)
                            if (mActivity.prefs.contains(SECKEY1) && mActivity.prefs.contains(SECKEY2)) {
                                key = Base64.decode(mActivity.prefs.getString(SECKEY1, ""), Base64.NO_WRAP)
                                iv = Base64.decode(mActivity.prefs.getString(SECKEY2, ""), Base64.NO_WRAP)
                            } else {
                                secureRandom.nextBytes(key)
                                secureRandom.nextBytes(iv)
                            }
                            mActivity.prefs.edit {
                                putString(SECKEY1, Base64.encodeToString(key, Base64.NO_WRAP))
                                putString(SECKEY2, Base64.encodeToString(iv, Base64.NO_WRAP))
                            }
                            cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), IvParameterSpec(iv))
                            cred.writeText(Base64.encodeToString(cipher.doFinal("${editTextKundelikLogin.text}|${editTextKundelikPassword.text}".toByteArray()), Base64.NO_WRAP))
                        }
                        dismiss()
                        mActivity.inflateKundelikFragment()
                    },
                    Response.ErrorListener { error ->
                        Toast.makeText(context, "ERROR get: ${error.message}", Toast.LENGTH_LONG).show()
                        showResultDialog("Error", error.message.toString())
                        dismiss()
                    }
            ))
        }
    }

    private fun showResultDialog(title: String, message: String) {
        val builder = AlertDialog.Builder(context)
        builder.setPositiveButton("COPY") { _, _ ->
            val clip = mActivity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clip.primaryClip = ClipData.newPlainText("lcResponse", message)
            dismiss()
        }
        builder.setNeutralButton("DISMISS") { _, _ ->
            dismiss()
        }
        builder.setTitle(title)
        builder.setMessage(message)
        builder.create().show()
    }
}