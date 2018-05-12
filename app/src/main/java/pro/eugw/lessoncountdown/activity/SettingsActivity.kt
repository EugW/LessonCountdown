package pro.eugw.lessoncountdown.activity

import android.app.Activity
import android.app.AlertDialog
import android.content.*
import android.graphics.Color
import android.os.Bundle
import android.support.constraint.ConstraintLayout
import android.support.v4.content.LocalBroadcastManager
import android.view.View
import android.widget.*
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.jaredrummler.android.colorpicker.ColorPickerDialog
import com.jaredrummler.android.colorpicker.ColorPickerDialogListener
import pro.eugw.lessoncountdown.BaseActivity
import pro.eugw.lessoncountdown.MService
import pro.eugw.lessoncountdown.R
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.io.PrintWriter
import java.net.HttpURLConnection
import java.net.URL

class SettingsActivity : BaseActivity() {

    private lateinit var prefs: SharedPreferences
    private lateinit var host: String
    private var colorTitle = Color.parseColor("#000000")
    private var colorTime = Color.parseColor("#999999")
    private var colorLessons = Color.parseColor("#999999")
    private var colorBackground = Color.parseColor("#ffffff")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        title = getString(R.string.settings)
        prefs = getSharedPreferences("newPrefs", Context.MODE_PRIVATE)
        host = prefs.getString("cAddress", getString(R.string.host))
        initClass()
        initCustomCfg()
        initOwnServer()
        initTheme()
        initCustomColors()
        val client = BillingClient.newBuilder(this).setListener { responseCode, purchases ->
            if (responseCode == BillingClient.BillingResponse.OK && purchases?.any { it.sku == "use_own_server" } == true) {
                findViewById<ConstraintLayout>(R.id.ownServer).setOnClickListener {
                    showDialogAddress()
                }
            }
        }.build()
        client.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(responseCode: Int) {
                if (responseCode == BillingClient.BillingResponse.OK) {
                    if (client.queryPurchases(BillingClient.SkuType.INAPP).purchasesList.any { it.sku == "use_own_server" }) {
                        findViewById<ConstraintLayout>(R.id.ownServer).setOnClickListener {
                            showDialogAddress()
                        }
                    } else {
                        findViewById<ConstraintLayout>(R.id.ownServer).setOnClickListener {
                            val flowParams = BillingFlowParams.newBuilder().setSku("use_own_server").setType(BillingClient.SkuType.INAPP).build()
                            client.launchBillingFlow(this@SettingsActivity, flowParams)
                        }
                    }
                } else {
                    Toast.makeText(this@SettingsActivity, "Google Play Error $responseCode", Toast.LENGTH_LONG).show()
                }
            }

            override fun onBillingServiceDisconnected() {
                Toast.makeText(this@SettingsActivity, "Google Play Disconnected", Toast.LENGTH_LONG).show()
            }

        })
        findViewById<Button>(R.id.devButton).setOnClickListener {
            client.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(responseCode: Int) {
                    if (responseCode == BillingClient.BillingResponse.OK) {
                        client.queryPurchases(BillingClient.SkuType.INAPP).purchasesList.forEach {
                            client.consumeAsync(it.purchaseToken, { _, _ -> })
                        }
                    } else {
                        Toast.makeText(this@SettingsActivity, "Google Play Error $responseCode", Toast.LENGTH_LONG).show()
                    }
                }

                override fun onBillingServiceDisconnected() {
                    Toast.makeText(this@SettingsActivity, "Google Play Disconnected", Toast.LENGTH_LONG).show()
                }

            })
        }
    }

    private fun initClass() {
        val string = prefs.getString("class", "")
        if (string.isNotEmpty()) {
            val text = findViewById<TextView>(R.id.selectedClass)
            text.visibility = View.VISIBLE
            text.text = string.replace(".", "")
        }
        findViewById<ConstraintLayout>(R.id.selClassLayout).setOnClickListener {
            val intent = Intent(this@SettingsActivity, SearchActivity::class.java)
            startActivityForResult(intent, 1)
        }
    }

    private fun initCustomCfg() {
        findViewById<Switch>(R.id.switchCustomCfg).isChecked = prefs.getBoolean("CustomCfg", false)
        findViewById<ConstraintLayout>(R.id.customLayout).visibility = if (prefs.getBoolean("CustomCfg", false)) View.VISIBLE else View.GONE
        findViewById<Switch>(R.id.switchCustomCfg).setOnCheckedChangeListener { _, state ->
            if (state)
                findViewById<ConstraintLayout>(R.id.customLayout).visibility = View.VISIBLE
            else
                findViewById<ConstraintLayout>(R.id.customLayout).visibility = View.GONE
            prefs.edit().putBoolean("CustomCfg", state).apply()
        }
        findViewById<Button>(R.id.buttonCopy).setOnClickListener {
            val jObject = JsonObject()
            jObject.add("schedule", JsonParser().parse(FileReader(File(filesDir, "schedule.json"))))
            jObject.add("bells", JsonParser().parse(FileReader(File(filesDir, "bells.json"))))
            val clip = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clip.primaryClip = ClipData.newPlainText("lcConfig", jObject.toString())
        }
        findViewById<Button>(R.id.buttonPaste).setOnClickListener {
            val clip = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            try {
                val jObject = JsonParser().parse(clip.primaryClip.getItemAt(0).text.toString()).asJsonObject
                PrintWriter(FileWriter(File(filesDir, "schedule.json")), true).println(jObject["schedule"])
                PrintWriter(FileWriter(File(filesDir, "bells.json")), true).println(jObject["bells"])
            } catch (e: Exception) {
                Toast.makeText(this, R.string.pasteErr, Toast.LENGTH_LONG).show()
                e.printStackTrace()
            }
        }
    }

    private fun initTheme() {
        val themeSwitch = findViewById<Switch>(R.id.switchDarkTheme)
        themeSwitch.isChecked = prefs.getBoolean("darkTheme", false)
        themeSwitch.setOnCheckedChangeListener { _, state ->
            prefs.edit().putBoolean("darkTheme", state).apply()
            val localIntent = Intent(baseContext.packageName + ".THEME_UPDATE")
            val sender = LocalBroadcastManager.getInstance(this)
            sender.sendBroadcast(localIntent)
        }
    }

    private fun initOwnServer() {
        val ownServerAddress = findViewById<TextView>(R.id.ownServerAddress)
        if (prefs.getString("cAddress", getString(R.string.host)) != getString(R.string.host)) {
            ownServerAddress.visibility = View.VISIBLE
            ownServerAddress.text = host
        }
    }

    private fun initCustomColors() {
        val localIntent = Intent(baseContext.packageName + ".NOTIFICATION_COLOR_UPDATE")
        val sender = LocalBroadcastManager.getInstance(this)
        findViewById<Switch>(R.id.switchNotificationColor).isChecked = prefs.getBoolean("CustomColor", false)
        findViewById<ConstraintLayout>(R.id.customColors).visibility = if (prefs.getBoolean("CustomColor", false)) View.VISIBLE else View.GONE
        findViewById<Switch>(R.id.switchNotificationColor).setOnCheckedChangeListener { _, state ->
            if (state)
                findViewById<ConstraintLayout>(R.id.customColors).visibility = View.VISIBLE
            else
                findViewById<ConstraintLayout>(R.id.customColors).visibility = View.GONE
            prefs.edit().putBoolean("CustomColor", state).apply()
        }
        val title = findViewById<Button>(R.id.buttonChooseDialogTitle)
        colorTitle = prefs.getInt("titleColor", Color.parseColor("#000000"))
        title.setBackgroundColor(colorTitle)
        title.setOnClickListener {
            val builder = ColorPickerDialog.newBuilder().setAllowPresets(false).setDialogType(0).setColor(colorTitle).create()
            builder.setColorPickerDialogListener(object : ColorPickerDialogListener {
                override fun onDialogDismissed(dialogId: Int) {}

                override fun onColorSelected(dialogId: Int, color: Int) {
                    colorTitle = color
                    title.setBackgroundColor(color)
                    prefs.edit().putInt("titleColor", colorTitle).apply()
                    sender.sendBroadcast(localIntent)
                }
            })
            builder.show(fragmentManager, "color-picker-dialog")
        }
        val textTime = findViewById<Button>(R.id.buttonChooseDialogTime)
        colorTime = prefs.getInt("timeColor", Color.parseColor("#999999"))
        textTime.setBackgroundColor(colorTime)
        textTime.setOnClickListener {
            val builder = ColorPickerDialog.newBuilder().setAllowPresets(false).setDialogType(0).setColor(colorTime).create()
            builder.setColorPickerDialogListener(object : ColorPickerDialogListener {
                override fun onDialogDismissed(dialogId: Int) {}

                override fun onColorSelected(dialogId: Int, color: Int) {
                    colorTime = color
                    textTime.setBackgroundColor(color)
                    prefs.edit().putInt("timeColor", colorTime).apply()
                    sender.sendBroadcast(localIntent)
                }
            })
            builder.show(fragmentManager, "color-picker-dialog")
        }
        val textLessons = findViewById<Button>(R.id.buttonChooseDialogLessons)
        colorLessons = prefs.getInt("lessonsColor", Color.parseColor("#999999"))
        textLessons.setBackgroundColor(colorLessons)
        textLessons.setOnClickListener {
            val builder = ColorPickerDialog.newBuilder().setAllowPresets(false).setDialogType(0).setColor(colorLessons).create()
            builder.setColorPickerDialogListener(object : ColorPickerDialogListener {
                override fun onDialogDismissed(dialogId: Int) {}

                override fun onColorSelected(dialogId: Int, color: Int) {
                    colorLessons = color
                    textLessons.setBackgroundColor(color)
                    prefs.edit().putInt("lessonsColor", colorLessons).apply()
                    sender.sendBroadcast(localIntent)
                }
            })
            builder.show(fragmentManager, "color-picker-dialog")
        }
        val background = findViewById<Button>(R.id.buttonChooseDialogBackground)
        colorBackground = prefs.getInt("backgroundColor", Color.parseColor("#ffffff"))
        background.setBackgroundColor(colorBackground)
        background.setOnClickListener {
            val builder = ColorPickerDialog.newBuilder().setAllowPresets(false).setDialogType(0).setColor(colorBackground).create()
            builder.setColorPickerDialogListener(object : ColorPickerDialogListener {
                override fun onDialogDismissed(dialogId: Int) {}

                override fun onColorSelected(dialogId: Int, color: Int) {
                    colorBackground = color
                    background.setBackgroundColor(color)
                    prefs.edit().putInt("backgroundColor", colorBackground).apply()
                    sender.sendBroadcast(localIntent)
                }
            })
            builder.show(fragmentManager, "color-picker-dialog")
        }
    }

    private fun showDialogAddress() {
        val ab = AlertDialog.Builder(this@SettingsActivity)
        ab.setTitle(R.string.address)
        val input = EditText(this@SettingsActivity)
        input.setText(host)
        ab.setView(input)
        ab.setPositiveButton("ok", { _, _ ->
            host = try {
                val url = URL("http://${input.text}")
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = resources.getInteger(R.integer.timeout)
                conn.readTimeout = resources.getInteger(R.integer.timeout)
                conn.connect()
                val ownText2 = findViewById<TextView>(R.id.ownServerAddress)
                ownText2.visibility = View.VISIBLE
                ownText2.text = input.text
                input.text.toString()
            } catch (e: Exception) {
                getString(R.string.host)
            }
            prefs.edit().putString("cAddress", host).apply()
        })
        ab.show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == 1 && resultCode == Activity.RESULT_OK && data != null) {
            val extras = data.extras
            prefs.edit().putString("class", extras["class"] as String).putString("school_id", extras["school_id"] as String).apply()
            val text = findViewById<TextView>(R.id.selectedClass)
            text.visibility = View.VISIBLE
            text.text = (extras["class"] as String).replace(".", "")
            val localIntent = Intent(baseContext.packageName + ".CLASS_UPDATE")
            val sender = LocalBroadcastManager.getInstance(this)
            stopService(Intent(this, MService::class.java))
            sender.sendBroadcast(localIntent)
        }
    }

}
