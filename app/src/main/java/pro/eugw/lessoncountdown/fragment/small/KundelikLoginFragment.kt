package pro.eugw.lessoncountdown.fragment.small

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
import com.google.gson.JsonObject
import kotlinx.android.synthetic.main.fragment_kundelik_login.*
import pro.eugw.lessoncountdown.R
import pro.eugw.lessoncountdown.activity.MainActivity
import pro.eugw.lessoncountdown.util.*
import pro.eugw.lessoncountdown.util.network.JsArRe
import pro.eugw.lessoncountdown.util.network.JsObRe
import java.io.File
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec


class KundelikLoginFragment : DialogFragment() {

    private lateinit var mActivity: MainActivity

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        return inflater.inflate(R.layout.fragment_kundelik_login, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        mActivity = activity as MainActivity
        buttonKundelikLogin.setOnClickListener {
            kundelikLoginLayout.visibility = View.GONE
            kundelikProgressBar.visibility = View.VISIBLE
            val jsonDetails = JsonObject()
            jsonDetails.addProperty("username", editTextKundelikLogin.text.toString())
            jsonDetails.addProperty("password", editTextKundelikPassword.text.toString())
            jsonDetails.addProperty("client_id", CLIENT_ID)
            jsonDetails.addProperty("client_secret", CLIENT_SECRET)
            jsonDetails.addProperty("scope", KUNDELIK_SCOPE)
            mActivity.queue.add(JsObRe(Request.Method.POST, "https://api.kundelik.kz/v1/authorizations/bycredentials", jsonDetails,
                    { response ->
                        mActivity.queue.add(JsArRe(Request.Method.GET, "https://api.kundelik.kz/v1/users/me/roles?access_token=${response["accessToken"].asString}",
                                {
                                    when {
                                        it.contains("EduStudent") -> continueAsStudent(response)
                                        it.contains("EduStaff") -> continueAsStaff(response)
                                        it.contains("EduParent") -> continueAsParent(response)
                                        it.contains("OrganizationStaff") -> continueAsOrganizationStaff(response)
                                        else -> continueAsUnknown(response)
                                    }
                                },
                                {}
                        ))
                    },
                    { error ->
                        Toast.makeText(context, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                        showResultDialog("Error", error.message.toString())
                        dismiss()
                    }
            ))
        }
    }

    private fun continueAsStudent(response: JsonObject) {
        mActivity.prefs.edit { putString(KUNDELIK_ROLE, "EduStudent") }
        if (mActivity.prefs.contains(LCAPI_TOKEN)) {
            mActivity.queue.add(JsObRe(Request.Method.GET, "https://$host/updateKCred?kusername=${editTextKundelikLogin.text}&kpassword=${editTextKundelikPassword.text}&token=${mActivity.prefs.getString(LCAPI_TOKEN, "")}",
                    {
                        Toast.makeText(context, "Kundelik credentials successfully updated on server", Toast.LENGTH_SHORT).show()
                    },
                    { error ->
                        Toast.makeText(context, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                    }
            ))
        }
        showResultDialog("Success", response.toString())
        mActivity.prefs.edit { putString(KUNDELIK_TOKEN, response["accessToken"].asString) }
        saveEncodedCred()
        dismiss()
        mActivity.inflateKundelikFragment()
    }

    private fun continueAsStaff(response: JsonObject) {
        mActivity.prefs.edit { putString(KUNDELIK_ROLE, "EduStaff") }
        showResultDialog("Error", "EduStaff role is not supported yet\n$response")
        mActivity.prefs.edit { putString(KUNDELIK_TOKEN, response["accessToken"].asString) }
        saveEncodedCred()
        dismiss()
        mActivity.inflateKundelikFragment()
    }

    private fun continueAsParent(response: JsonObject) {
        mActivity.prefs.edit { putString(KUNDELIK_ROLE, "EduParent") }
        showResultDialog("Error", "EduParent role is not supported yet\n$response")
        mActivity.prefs.edit { putString(KUNDELIK_TOKEN, response["accessToken"].asString) }
        saveEncodedCred()
        dismiss()
        mActivity.inflateKundelikFragment()
    }

    private fun continueAsOrganizationStaff(response: JsonObject) {
        mActivity.prefs.edit { putString(KUNDELIK_ROLE, "EduOrganizationStaff") }
        showResultDialog("Error", "EduOrganizationStaff role is not supported yet\n$response")
        mActivity.prefs.edit { putString(KUNDELIK_TOKEN, response["accessToken"].asString) }
        saveEncodedCred()
        dismiss()
        mActivity.inflateKundelikFragment()
    }

    private fun continueAsUnknown(response: JsonObject) {
        mActivity.prefs.edit { putString(KUNDELIK_ROLE, "Unknown") }
        showResultDialog("Error", "Unknown role is not supported yet\n$response")
        mActivity.prefs.edit { putString(KUNDELIK_TOKEN, response["accessToken"].asString) }
        saveEncodedCred()
        dismiss()
        mActivity.inflateKundelikFragment()
    }

    private fun saveEncodedCred() {
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

    private fun showResultDialog(title: String, message: String) {
        val builder = AlertDialog.Builder(context)
        builder.setPositiveButton("COPY") { _, _ ->
            val clip = mActivity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clip.setPrimaryClip(ClipData.newPlainText("lcResponse", message))
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