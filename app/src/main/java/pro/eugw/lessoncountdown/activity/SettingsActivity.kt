package pro.eugw.lessoncountdown.activity

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.support.constraint.ConstraintLayout
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.*
import com.android.billingclient.api.*
import com.google.gson.JsonParser
import pro.eugw.lessoncountdown.MService
import pro.eugw.lessoncountdown.R
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.io.PrintWriter
import java.net.HttpURLConnection
import java.net.URL

class SettingsActivity : AppCompatActivity() {

    private lateinit var prefs: SharedPreferences
    private lateinit var host: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        title = getString(R.string.settings)
        prefs = getSharedPreferences("newPrefs", Context.MODE_PRIVATE)
        val readPrf = prefs.getString("cAddress", getString(R.string.host))
        host = if (readPrf.isBlank()) getString(R.string.host) else readPrf
        if (prefs.contains("class")) {
            val string = prefs.getString("class", "")
            if (string.isNotEmpty()) {
                val text = findViewById<TextView>(R.id.selectedClass)
                text.visibility = View.VISIBLE
                text.text = string.replace(".", "")
            }
        }
        if (!prefs.contains("CustomCfg"))
            prefs.edit().putBoolean("CustomCfg", false).apply()
        if (!prefs.contains("cAddress"))
            prefs.edit().putString("cAddress", getString(R.string.host)).apply()
        if (File(filesDir, "schedule.json").exists())
            (findViewById<EditText>(R.id.editTextCustomSchedule)).setText(FileReader(File(filesDir, "schedule.json")).readText())
        if (File(filesDir, "bells.json").exists())
            (findViewById<EditText>(R.id.editTextCustomBells)).setText(FileReader(File(filesDir, "bells.json")).readText())
        findViewById<Switch>(R.id.switchCustomCfg).isChecked = prefs.getBoolean("CustomCfg", false)
        findViewById<ConstraintLayout>(R.id.customLayout).visibility = if (prefs.getBoolean("CustomCfg", false)) View.VISIBLE else View.GONE
        findViewById<Switch>(R.id.switchCustomCfg).setOnCheckedChangeListener { _, state ->
            if (state)
                findViewById<ConstraintLayout>(R.id.customLayout).visibility = View.VISIBLE
            else
                findViewById<ConstraintLayout>(R.id.customLayout).visibility = View.GONE
        }
        val ownText2 = findViewById<TextView>(R.id.ownServerText2)
        if (prefs.contains("cAddress")) {
            ownText2.visibility = View.VISIBLE
            ownText2.text = host
        }
        val client = BillingClient.newBuilder(this).setListener { responseCode, purchases ->
            if (responseCode == BillingClient.BillingResponse.OK && purchases?.any { it.sku == "use_own_server" } == true) {
                findViewById<ConstraintLayout>(R.id.ownServer).setOnClickListener {
                    showDialog()
                }
                if (host != getString(R.string.host)) {
                    findViewById<ConstraintLayout>(R.id.selClassLayout).setOnClickListener {
                        val servers = ArrayList<String>()
                        servers.add(getString(R.string.ownServer))
                        servers.add(getString(R.string.mainServer))
                        val intent = Intent(this@SettingsActivity, SearchActivity::class.java).putExtra("servers", servers)
                        startActivityForResult(intent, 1)
                    }
                } else {
                    findViewById<ConstraintLayout>(R.id.selClassLayout).setOnClickListener {
                        val servers = ArrayList<String>()
                        servers.add(getString(R.string.mainServer))
                        val intent = Intent(this@SettingsActivity, SearchActivity::class.java).putExtra("servers", servers)
                        startActivityForResult(intent, 1)
                    }
                }
            }
        }.build()
        client.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(responseCode: Int) {
                if (responseCode == BillingClient.BillingResponse.OK) {
                    var hasOwn = false
                    client.queryPurchases(BillingClient.SkuType.INAPP).purchasesList.forEach {
                        if (it.sku == "use_own_server") {
                            hasOwn = true
                        }
                    }
                    if (hasOwn) {
                        if (prefs.contains("cAddress")) {
                            host = prefs.getString("cAddress", getString(R.string.host))
                            ownText2.visibility = View.VISIBLE
                            ownText2.text = host
                            if (host != getString(R.string.host)) {
                                findViewById<ConstraintLayout>(R.id.selClassLayout).setOnClickListener {
                                    val servers = ArrayList<String>()
                                    servers.add(getString(R.string.ownServer))
                                    servers.add(getString(R.string.mainServer))
                                    val intent = Intent(this@SettingsActivity, SearchActivity::class.java).putExtra("servers", servers)
                                    startActivityForResult(intent, 1)
                                }
                            } else {
                                findViewById<ConstraintLayout>(R.id.selClassLayout).setOnClickListener {
                                    val servers = ArrayList<String>()
                                    servers.add(getString(R.string.mainServer))
                                    val intent = Intent(this@SettingsActivity, SearchActivity::class.java).putExtra("servers", servers)
                                    startActivityForResult(intent, 1)
                                }
                            }
                        }
                        findViewById<ConstraintLayout>(R.id.ownServer).setOnClickListener {
                            showDialog()
                        }
                    } else {
                        findViewById<ConstraintLayout>(R.id.ownServer).setOnClickListener {
                            val flowParams = BillingFlowParams.newBuilder().setSku("use_own_server").setType(BillingClient.SkuType.INAPP).build()
                            client.launchBillingFlow(this@SettingsActivity, flowParams)
                        }
                        host = getString(R.string.host)
                        findViewById<ConstraintLayout>(R.id.selClassLayout).setOnClickListener {
                            val servers = ArrayList<String>()
                            servers.add(getString(R.string.mainServer))
                            val intent = Intent(this@SettingsActivity, SearchActivity::class.java).putExtra("servers", servers)
                            startActivityForResult(intent, 1)
                        }
                    }
                } else {
                    Toast.makeText(this@SettingsActivity, "Error $responseCode", Toast.LENGTH_LONG).show()
                }
            }
            override fun onBillingServiceDisconnected() {
                Toast.makeText(this@SettingsActivity, "Disconnected", Toast.LENGTH_LONG).show()
            }

        })
        findViewById<Button>(R.id.devbutton).setOnClickListener {
            client.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(responseCode: Int) {
                    if (responseCode == BillingClient.BillingResponse.OK) {
                        client.queryPurchases(BillingClient.SkuType.INAPP).purchasesList.forEach {
                            client.consumeAsync(it.purchaseToken, { _, _ -> })
                        }
                    } else {
                        Toast.makeText(this@SettingsActivity, "Error $responseCode", Toast.LENGTH_LONG).show()
                    }
                }

                override fun onBillingServiceDisconnected() {
                    Toast.makeText(this@SettingsActivity, "Disconnected", Toast.LENGTH_LONG).show()
                }

            })
        }
        findViewById<View>(R.id.buttonSave).setOnClickListener({
            Thread {
                if (findViewById<Switch>(R.id.switchCustomCfg).isChecked) {
                    val schedule = (findViewById<EditText>(R.id.editTextCustomSchedule)).text.toString()
                    val bells = (findViewById<EditText>(R.id.editTextCustomBells)).text.toString()
                    try {
                        val s = JsonParser().parse(schedule).asJsonObject
                        PrintWriter(FileWriter(File(filesDir, "schedule.json")), true).println(s)
                        val b = JsonParser().parse(bells).asJsonObject
                        PrintWriter(FileWriter(File(filesDir, "bells.json")), true).println(b)
                    } catch (e: Exception) {
                        runOnUiThread { Toast.makeText(this, R.string.configErr, Toast.LENGTH_LONG).show() }
                        return@Thread
                    }
                }
                prefs.edit().putBoolean("CustomCfg", findViewById<Switch>(R.id.switchCustomCfg).isChecked).apply()
                prefs.edit().putString("cAddress", host).apply()
                stopService(Intent(this, MService::class.java))
                val i = baseContext.packageManager.getLaunchIntentForPackage(baseContext.packageName)
                i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                startActivity(i)
            }.start()
        })
    }

    private fun showDialog() {
        val ab = AlertDialog.Builder(this@SettingsActivity)
        ab.setTitle(R.string.address)
        val input = EditText(this@SettingsActivity)
        input.setText(host)
        ab.setView(input)
        ab.setPositiveButton("ok", { _, _ ->
            prefs.edit().putString("cAddress", try {
                val url = URL("http://${input.text}")
                val conn = url.openConnection() as HttpURLConnection
                conn.connect()
                val ownText2 = findViewById<TextView>(R.id.ownServerText2)
                ownText2.visibility = View.VISIBLE
                ownText2.text = input.text
                input.text.toString()
            } catch (e: Exception) {
                getString(R.string.host)
            }).apply()

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
        }
    }
}
