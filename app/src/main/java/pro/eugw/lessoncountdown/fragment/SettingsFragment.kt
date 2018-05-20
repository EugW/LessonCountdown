package pro.eugw.lessoncountdown.fragment

import android.app.Activity
import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.content.LocalBroadcastManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.jaredrummler.android.colorpicker.ColorPickerDialog
import com.jaredrummler.android.colorpicker.ColorPickerDialogListener
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_settings.*
import pro.eugw.lessoncountdown.R
import pro.eugw.lessoncountdown.activity.MainActivity
import pro.eugw.lessoncountdown.activity.SearchActivity
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.io.PrintWriter
import java.net.HttpURLConnection
import java.net.URL

@Suppress("DEPRECATION")
class SettingsFragment : Fragment() {

    private lateinit var host: String
    private lateinit var broadcastManager: LocalBroadcastManager
    private var colorTitle = Color.parseColor("#000000")
    private var colorTime = Color.parseColor("#999999")
    private var colorLessons = Color.parseColor("#999999")
    private var colorBackground = Color.parseColor("#ffffff")
    private lateinit var mActivity: MainActivity

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onStart() {
        super.onStart()
        mActivity = activity as MainActivity
        mActivity.main_toolbar.title = getString(R.string.settings)
        mActivity.main_toolbar.menu.clear()
        broadcastManager = mActivity.broadcastManager
        host = mActivity.prefs.getString("cAddress", getString(R.string.host))
        initClass()
        initCustomCfg()
        initOwnServer()
        initTheme()
        initCustomColors()
        val client = BillingClient.newBuilder(mActivity).setListener { responseCode, purchases ->
            if (responseCode == BillingClient.BillingResponse.OK && purchases?.any { it.sku == "use_own_server" } == true) {
                ownServer.setOnClickListener {
                    showDialogAddress()
                }
            }
        }.build()
        client.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(responseCode: Int) {
                if (responseCode == BillingClient.BillingResponse.OK) {
                    if (client.queryPurchases(BillingClient.SkuType.INAPP).purchasesList.any { it.sku == "use_own_server" }) {
                        ownServer.setOnClickListener {
                            showDialogAddress()
                        }
                    } else {
                        ownServer.setOnClickListener {
                            val flowParams = BillingFlowParams.newBuilder().setSku("use_own_server").setType(BillingClient.SkuType.INAPP).build()
                            client.launchBillingFlow(mActivity, flowParams)
                        }
                    }
                } else {
                    Toast.makeText(mActivity, "Google Play Error $responseCode", Toast.LENGTH_LONG).show()
                }
            }

            override fun onBillingServiceDisconnected() {
                Toast.makeText(mActivity, "Google Play Disconnected", Toast.LENGTH_LONG).show()
            }

        })
        /*devButton.setOnClickListener {
            client.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(responseCode: Int) {
                    if (responseCode == BillingClient.BillingResponse.OK) {
                        client.queryPurchases(BillingClient.SkuType.INAPP).purchasesList.forEach {
                            client.consumeAsync(it.purchaseToken, { _, _ -> })
                        }
                    } else {
                        Toast.makeText(mActivity, "Google Play Error $responseCode", Toast.LENGTH_LONG).show()
                    }
                }

                override fun onBillingServiceDisconnected() {
                    Toast.makeText(mActivity, "Google Play Disconnected", Toast.LENGTH_LONG).show()
                }

            })
        }*/
    }

    private fun initClass() {
        val string = mActivity.prefs.getString("class", "")
        if (string.isNotEmpty()) {
            val text = selectedClass
            text.visibility = View.VISIBLE
            text.text = string.replace(".", "")
        }
        selClassLayout.setOnClickListener {
            val intent = Intent(mActivity, SearchActivity::class.java)
            startActivityForResult(intent, 1)
        }
    }

    private fun initCustomCfg() {
        switchCustomCfg.isChecked = mActivity.prefs.getBoolean("CustomCfg", false)
        customLayout.visibility = if (mActivity.prefs.getBoolean("CustomCfg", false)) View.VISIBLE else View.GONE
        switchCustomCfg.setOnCheckedChangeListener { _, state ->
            if (state)
                customLayout.visibility = View.VISIBLE
            else
                customLayout.visibility = View.GONE
            mActivity.prefs.edit().putBoolean("CustomCfg", state).apply()
        }
        buttonCopy.setOnClickListener {
            val jObject = JsonObject()
            jObject.add("schedule", JsonParser().parse(FileReader(File(mActivity.filesDir, "schedule.json"))))
            jObject.add("bells", JsonParser().parse(FileReader(File(mActivity.filesDir, "bells.json"))))
            val clip = mActivity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clip.primaryClip = ClipData.newPlainText("lcConfig", jObject.toString())
        }
        buttonPaste.setOnClickListener {
            val clip = mActivity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            try {
                val jObject = JsonParser().parse(clip.primaryClip.getItemAt(0).text.toString()).asJsonObject
                PrintWriter(FileWriter(File(mActivity.filesDir, "schedule.json")), true).println(jObject["schedule"])
                PrintWriter(FileWriter(File(mActivity.filesDir, "bells.json")), true).println(jObject["bells"])
            } catch (e: Exception) {
                Toast.makeText(mActivity, R.string.pasteErr, Toast.LENGTH_LONG).show()
                e.printStackTrace()
            }
        }
    }

    private fun initTheme() {
        val themeSwitch = switchDarkTheme
        themeSwitch.isChecked = mActivity.prefs.getBoolean("darkTheme", false)
        themeSwitch.setOnCheckedChangeListener { _, state ->
            mActivity.prefs.edit().putBoolean("darkTheme", state).apply()
            mActivity.recreate()
        }
    }

    private fun initOwnServer() {
        val ownServerAddress = ownServerAddress
        if (mActivity.prefs.getString("cAddress", getString(R.string.host)) != getString(R.string.host)) {
            ownServerAddress.visibility = View.VISIBLE
            ownServerAddress.text = host
        }
    }

    private fun initCustomColors() {
        val localIntent = Intent(mActivity.packageName + ".NOTIFICATION_COLOR_UPDATE")
        val sender = broadcastManager
        switchNotificationColor.isChecked = mActivity.prefs.getBoolean("CustomColor", false)
        customColors.visibility = if (mActivity.prefs.getBoolean("CustomColor", false)) View.VISIBLE else View.GONE
        switchNotificationColor.setOnCheckedChangeListener { _, state ->
            if (state)
                customColors.visibility = View.VISIBLE
            else
                customColors.visibility = View.GONE
            mActivity.prefs.edit().putBoolean("CustomColor", state).apply()
        }
        val title = buttonChooseDialogTitle
        colorTitle = mActivity.prefs.getInt("titleColor", Color.parseColor("#000000"))
        title.setBackgroundColor(colorTitle)
        title.setOnClickListener {
            val builder = ColorPickerDialog.newBuilder().setAllowPresets(false).setDialogType(0).setColor(colorTitle).create()
            builder.setColorPickerDialogListener(object : ColorPickerDialogListener {
                override fun onDialogDismissed(dialogId: Int) {}

                override fun onColorSelected(dialogId: Int, color: Int) {
                    colorTitle = color
                    title.setBackgroundColor(color)
                    mActivity.prefs.edit().putInt("titleColor", colorTitle).apply()
                    sender.sendBroadcast(localIntent)
                }
            })
            builder.show(mActivity.fragmentManager, "color-picker-dialog")
        }
        val textTime = buttonChooseDialogTime
        colorTime = mActivity.prefs.getInt("timeColor", Color.parseColor("#999999"))
        textTime.setBackgroundColor(colorTime)
        textTime.setOnClickListener {
            val builder = ColorPickerDialog.newBuilder().setAllowPresets(false).setDialogType(0).setColor(colorTime).create()
            builder.setColorPickerDialogListener(object : ColorPickerDialogListener {
                override fun onDialogDismissed(dialogId: Int) {}

                override fun onColorSelected(dialogId: Int, color: Int) {
                    colorTime = color
                    textTime.setBackgroundColor(color)
                    mActivity.prefs.edit().putInt("timeColor", colorTime).apply()
                    sender.sendBroadcast(localIntent)
                }
            })
            builder.show(mActivity.fragmentManager, "color-picker-dialog")
        }
        val textLessons = buttonChooseDialogLessons
        colorLessons = mActivity.prefs.getInt("lessonsColor", Color.parseColor("#999999"))
        textLessons.setBackgroundColor(colorLessons)
        textLessons.setOnClickListener {
            val builder = ColorPickerDialog.newBuilder().setAllowPresets(false).setDialogType(0).setColor(colorLessons).create()
            builder.setColorPickerDialogListener(object : ColorPickerDialogListener {
                override fun onDialogDismissed(dialogId: Int) {}

                override fun onColorSelected(dialogId: Int, color: Int) {
                    colorLessons = color
                    textLessons.setBackgroundColor(color)
                    mActivity.prefs.edit().putInt("lessonsColor", colorLessons).apply()
                    sender.sendBroadcast(localIntent)
                }
            })
            builder.show(mActivity.fragmentManager, "color-picker-dialog")
        }
        val background = buttonChooseDialogBackground
        colorBackground = mActivity.prefs.getInt("backgroundColor", Color.parseColor("#ffffff"))
        background.setBackgroundColor(colorBackground)
        background.setOnClickListener {
            val builder = ColorPickerDialog.newBuilder().setAllowPresets(false).setDialogType(0).setColor(colorBackground).create()
            builder.setColorPickerDialogListener(object : ColorPickerDialogListener {
                override fun onDialogDismissed(dialogId: Int) {}

                override fun onColorSelected(dialogId: Int, color: Int) {
                    colorBackground = color
                    background.setBackgroundColor(color)
                    mActivity.prefs.edit().putInt("backgroundColor", colorBackground).apply()
                    sender.sendBroadcast(localIntent)
                }
            })
            builder.show(mActivity.fragmentManager, "color-picker-dialog")
        }
    }

    private fun showDialogAddress() {
        val ab = AlertDialog.Builder(mActivity)
        ab.setTitle(R.string.address)
        val input = EditText(mActivity)
        input.setText(host)
        ab.setView(input)
        ab.setPositiveButton("ok", { _, _ ->
            host = try {
                val url = URL("http://${input.text}")
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = resources.getInteger(R.integer.timeout)
                conn.readTimeout = resources.getInteger(R.integer.timeout)
                conn.connect()
                val ownText2 = ownServerAddress
                ownText2.visibility = View.VISIBLE
                ownText2.text = input.text
                input.text.toString()
            } catch (e: Exception) {
                getString(R.string.host)
            }
            mActivity.prefs.edit().putString("cAddress", host).apply()
        })
        ab.show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == 1 && resultCode == Activity.RESULT_OK && data != null) {
            val extras = data.extras
            mActivity.prefs.edit().putString("class", extras["class"] as String).putString("school_id", extras["school_id"] as String).apply()
            val text = selectedClass
            text.visibility = View.VISIBLE
            text.text = (extras["class"] as String).replace(".", "")
            mActivity.clazz = mActivity.initClass()
            mActivity.homework = mActivity.initHomework()
            println("RES")
        }
    }
    
}