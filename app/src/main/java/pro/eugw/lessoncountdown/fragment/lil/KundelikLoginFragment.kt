package pro.eugw.lessoncountdown.fragment.lil

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
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
import pro.eugw.lessoncountdown.util.KUNDELIK_TOKEN
import pro.eugw.lessoncountdown.util.SECKEY
import java.io.File
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec


class KundelikLoginFragment : DialogFragment() {

    private lateinit var mActivity: MainActivity

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_kundelik_login, container, false)
    }

    @SuppressLint("HardwareIds")
    override fun onStart() {
        super.onStart()
        mActivity = activity as MainActivity
        val url = "https://api.kundelik.kz/v1/authorizations/bycredentials"
        val jsonDetails = JSONObject()
        jsonDetails.put("username", editTextKundelikLogin.text)
        jsonDetails.put("password", editTextKundelikPassword.text)
        jsonDetails.put("client_id", "387d44e3e0c94265a9e4a4caaad5111c")
        jsonDetails.put("client_secret", "8a7d709cfdbb4047b0ea8947afe89d67")
        jsonDetails.put("scope", "CommonInfo,ContactInfo,FriendsAndRelatives,EducationalInfo,SocialInfo,Files,Wall,Messages,Schools,Relatives,EduGroups,Lessons,Marks,EduWorks,Avatar")
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
                            if (mActivity.prefs.contains(SECKEY)) {
                                val arrString = mActivity.prefs.getString(SECKEY, "")?.split(",")
                                val arrByte = ""
                                //key = mActivity.prefs.getString(SECKEY, "").split(",").let {  }
                            } else {
                                secureRandom.nextBytes(key)
                            }

                            mActivity.prefs.edit { putString(SECKEY, key.let { string -> string.toList().toString().substring(1, string.toList().toString().lastIndex - 1) }) }
                            val secretKey = SecretKeySpec(key, "AES")
                            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
                            cred.writeText(cipher.doFinal("${editTextKundelikLogin.text}|${editTextKundelikPassword.text}".toByteArray()).toString())
                        }
                        dismiss()
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