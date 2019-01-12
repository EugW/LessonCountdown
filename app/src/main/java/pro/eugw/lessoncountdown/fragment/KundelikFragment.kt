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
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_kundelik_panel.*
import org.json.JSONObject
import pro.eugw.lessoncountdown.R
import pro.eugw.lessoncountdown.activity.MainActivity
import pro.eugw.lessoncountdown.fragment.small.KundelikLoginFragment
import pro.eugw.lessoncountdown.fragment.small.LCAPILoginFragment
import pro.eugw.lessoncountdown.util.*
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

class KundelikFragment : Fragment() {

    private lateinit var mActivity: MainActivity
    private lateinit var token: String
    private lateinit var prefs: SharedPreferences

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_kundelik_panel, container, false)
    }

    override fun onStart() {
        super.onStart()
        mActivity = activity as MainActivity
        mActivity.main_toolbar.title = getString(R.string.kundelik)
        mActivity.main_toolbar.menu.clear()
        prefs = mActivity.prefs
        token = prefs.getString("kundelikToken", "")!!
        if (token.length < 5) {
            KundelikLoginFragment().show(mActivity.supportFragmentManager, "lol")
        } else {
            mActivity.queue.add(tokenTestRequest())
        }
        buttonKundelikDownload.setOnClickListener {
            mActivity.initScheduleKundelik()
        }
        buttonKundelikReset.setOnClickListener {
            prefs.edit {
                remove(KUNDELIK_TOKEN)
                remove(SECKEY1)
                remove(SECKEY2)
            }
            File(mActivity.filesDir, "encLogDet").delete()
        }
        val host = prefs.getString(CUSTOM_ADDRESS, getString(R.string.host))
        buttonRegisterNotification.setOnClickListener {
            LCAPILoginFragment().show(mActivity.supportFragmentManager, "lol")
        }
        buttonUnregisterNotification.setOnClickListener {
            val url = "https://$host/unregister?token=${prefs.getString(LCAPI_TOKEN, "")}"
            mActivity.queue.add(JsonObjectRequest(url, null,
                    Response.Listener { response ->
                        Toast.makeText(mActivity, "Success", Toast.LENGTH_SHORT).show()
                        prefs.edit {
                            remove(LCAPI_TOKEN)
                        }
                    },
                    Response.ErrorListener { error ->
                        Toast.makeText(mActivity, error.message, Toast.LENGTH_SHORT).show()
                    }))
        }
    }

    private fun updateKToken(token: String) {
        val url = "https://$host/updateKToken?token=${prefs.getString(LCAPI_TOKEN, "")}&ktoken=$token"
        mActivity.queue.add(JsonObjectRequest(url, null,
                Response.Listener { response ->
                    Toast.makeText(mActivity, "Success", Toast.LENGTH_SHORT).show()
                },
                Response.ErrorListener { error ->
                    Toast.makeText(mActivity, error.message, Toast.LENGTH_SHORT).show()
                }))
    }

    private fun tokenTestRequest(): JsonObjectRequest {
        val url = "https://api.kundelik.kz/v1/users/me?access_token=$token"
        return JsonObjectRequest(Request.Method.GET, url, null,
                Response.Listener { response ->
                    textViewKundelikName.text = response.getString("name")
                    Toast.makeText(mActivity, "Authentication succeed", Toast.LENGTH_SHORT).show()
                },
                Response.ErrorListener {
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
                        val jsonDetails = JSONObject()
                        jsonDetails.put("username", credentials[0])
                        jsonDetails.put("password", credentials[1])
                        jsonDetails.put("client_id", CLIENT_ID)
                        jsonDetails.put("client_secret", CLIENT_SECRET)
                        jsonDetails.put("scope", KUNDELIK_SCOPE)
                        mActivity.queue.add(JsonObjectRequest(Request.Method.POST, "https://api.kundelik.kz/v1/authorizations/bycredentials", jsonDetails,
                                Response.Listener { response ->
                                    Toast.makeText(context, "Token update succeed: $response", Toast.LENGTH_SHORT).show()
                                    mActivity.prefs.edit { putString(KUNDELIK_TOKEN, response.getString("accessToken")) }
                                    updateKToken(response.getString("accessToken"))
                                    mActivity.inflateKundelikFragment()
                                },
                                Response.ErrorListener { error ->
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
                            clip.primaryClip = ClipData.newPlainText("lcError", sw.toString())
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