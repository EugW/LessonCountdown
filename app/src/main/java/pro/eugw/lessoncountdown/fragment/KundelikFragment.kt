package pro.eugw.lessoncountdown.fragment

import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import com.android.volley.Request
import com.google.gson.JsonObject
import kotlinx.android.synthetic.main.fragment_kundelik_panel.*
import pro.eugw.lessoncountdown.R
import pro.eugw.lessoncountdown.activity.MainActivity
import pro.eugw.lessoncountdown.fragment.small.KundelikLoginFragment
import pro.eugw.lessoncountdown.fragment.small.MarksLogFragment
import pro.eugw.lessoncountdown.util.*
import pro.eugw.lessoncountdown.util.network.JsObRe
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.*
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

class KundelikFragment : Fragment() {

    private lateinit var mActivity: MainActivity
    private lateinit var token: String
    private lateinit var prefs: SharedPreferences

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        return inflater.inflate(R.layout.fragment_kundelik_panel, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        mActivity = activity as MainActivity
        prefs = mActivity.prefs
        token = prefs.getString(KUNDELIK_TOKEN, "")!!
        buttonKundelikReset.setOnClickListener {
            prefs.edit {
                remove(KUNDELIK_TOKEN)
                remove(KUNDELIK_ROLE)
                remove(SECKEY1)
                remove(SECKEY2)
            }
            File(mActivity.filesDir, "encLogDet").delete()
            mActivity.inflateKundelikFragment()
        }
        buttonKundelikDownload.setOnClickListener {
            mActivity.initScheduleKundelik()
        }
        buttonMarksLog.setOnClickListener {
            MarksLogFragment().show(mActivity.supportFragmentManager, "marks log")
        }
        switchLocalMarksService.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit {
                putBoolean(LOCAL_MARKS_SERVICE, isChecked)
            }
            localMarksLayout.visibility = if (prefs.getBoolean(LOCAL_MARKS_SERVICE, false)) View.VISIBLE else View.GONE
        }
        switchLocalMarksService.isChecked = prefs.getBoolean(LOCAL_MARKS_SERVICE, false)
        localMarksLayout.visibility = if (prefs.getBoolean(LOCAL_MARKS_SERVICE, false)) View.VISIBLE else View.GONE
        editTextServiceDelay.setText(prefs.getInt(LOCAL_SERVICE_DELAY, 15).toString())
        if (token.length < 5) {
            KundelikLoginFragment().show(mActivity.supportFragmentManager, "lol")
        } else {
            mActivity.queue.add(tokenTestRequest())
        }
        if (prefs.contains(LAST_CHECK))
            textViewLastCheck.text = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(prefs.getLong(LAST_CHECK, 0))
        if (!SUPPORTED_KUNDELIK_ROLES.contains(mActivity.prefs.getString(KUNDELIK_ROLE, "")) && mActivity.prefs.getString(KUNDELIK_TOKEN, "")!!.isNotBlank()) {
            val str = "(${getString(R.string.unsupportedRole)}:${mActivity.prefs.getString(KUNDELIK_ROLE, "")})"
            val str1 = getString(R.string.download) + str
            buttonKundelikDownload.text = str1
            buttonKundelikDownload.isClickable = false
            val str4 = getString(R.string.marksLastList) + str
            buttonMarksLog.text = str4
            buttonMarksLog.isClickable = false
        }
    }

    private fun tokenTestRequest(): JsObRe {
        val url = "https://api.kundelik.kz/v1/users/me?access_token=$token"
        return JsObRe(Request.Method.GET, url,
                { response ->
                    textViewKundelikName.text = response["name"].asString
                    Toast.makeText(mActivity, "Authentication succeed", Toast.LENGTH_SHORT).show()
                },
                {
                    Toast.makeText(mActivity, "Token is out of date. Updating...", Toast.LENGTH_SHORT).show()
                    val cred = File(mActivity.filesDir, "encLogDet")
                    if (!cred.exists()) {
                        Toast.makeText(mActivity, "Authentication failed. Unable to found saved credentials. Re-login please", Toast.LENGTH_SHORT).show()
                        KundelikLoginFragment().show(mActivity.supportFragmentManager, "lol")
                    }
                    try {
                        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                        val key = Base64.decode(mActivity.prefs.getString(SECKEY1, ""), Base64.NO_WRAP)
                        val iv = Base64.decode(mActivity.prefs.getString(SECKEY2, ""), Base64.NO_WRAP)
                        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), IvParameterSpec(iv))
                        val encoded = Base64.decode(cred.readText(), Base64.NO_WRAP)
                        val decoded = cipher.doFinal(encoded)
                        val credentials = String(decoded).split("|")
                        val jsonDetails = JsonObject()
                        jsonDetails.addProperty("username", credentials[0])
                        jsonDetails.addProperty("password", credentials[1])
                        jsonDetails.addProperty("client_id", CLIENT_ID)
                        jsonDetails.addProperty("client_secret", CLIENT_SECRET)
                        jsonDetails.addProperty("scope", KUNDELIK_SCOPE)
                        mActivity.queue.add(JsObRe(Request.Method.POST, "https://api.kundelik.kz/v1/authorizations/bycredentials", jsonDetails,
                                { response ->
                                    Toast.makeText(context, "Token update succeed: $response", Toast.LENGTH_SHORT).show()
                                    mActivity.prefs.edit { putString(KUNDELIK_TOKEN, response["accessToken"].asString) }
                                    mActivity.inflateKundelikFragment()
                                },
                                { error ->
                                    Toast.makeText(context, "Token update failed: $error", Toast.LENGTH_SHORT).show()
                                }
                        ))
                    } catch (e: Exception) {
                        e.printStackTrace()
                        val sw = StringWriter()
                        e.printStackTrace(PrintWriter(sw))
                        val builder = AlertDialog.Builder(context)
                        builder.setPositiveButton("COPY") { _, _ ->
                            val clip = mActivity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clip.setPrimaryClip(ClipData.newPlainText("lcError", sw.toString()))
                        }
                        builder.setNeutralButton("DISMISS") { _, _ ->
                        }
                        builder.setTitle("Something went wrong. Send it to developer")
                        builder.setMessage(sw.toString())
                        builder.create().show()
                    }
                }
        )
    }

}