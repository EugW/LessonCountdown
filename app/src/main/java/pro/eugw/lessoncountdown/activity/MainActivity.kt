package pro.eugw.lessoncountdown.activity

import android.content.*
import android.os.Bundle
import android.support.design.widget.NavigationView
import android.support.v4.content.LocalBroadcastManager
import android.support.v4.view.GravityCompat
import android.support.v7.app.ActionBarDrawerToggle
import android.view.MenuItem
import android.widget.Toast
import android.widget.ToggleButton
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.main_toolbar.*
import pro.eugw.lessoncountdown.BaseActivity
import pro.eugw.lessoncountdown.MService
import pro.eugw.lessoncountdown.MainApp
import pro.eugw.lessoncountdown.R
import pro.eugw.lessoncountdown.fragment.DayOfWeekFragment
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.io.PrintWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.*
import kotlin.concurrent.thread


class MainActivity : BaseActivity(), NavigationView.OnNavigationItemSelectedListener {


    private lateinit var prefs: SharedPreferences
    var clazz = JsonObject()
    var homework = JsonObject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        prefs = getSharedPreferences("newPrefs", Context.MODE_PRIVATE)
        val toggle = ActionBarDrawerToggle(this, drawer_layout, main_toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        drawer_layout.addDrawerListener(toggle)
        toggle.syncState()
        nav_view.setNavigationItemSelectedListener(this)
        val toggleButton = nav_view.getHeaderView(0).findViewById<ToggleButton>(R.id.toggleButton)
        toggleButton.isChecked = (application as MainApp).running
        val instance = LocalBroadcastManager.getInstance(this)
        instance.registerReceiver(object : BroadcastReceiver() {
            override fun onReceive(p0: Context?, p1: Intent?) {
                toggleButton.isChecked = p1!!.getBooleanExtra("isRun", false)
            }
        }, IntentFilter(baseContext.packageName + ".SERVICE_STATE"))
        instance.registerReceiver(object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                thread(true) {
                    clazz = initClass()
                    homework = initHomework()
                    try {
                        inflateFragment()
                    } catch (e: Exception) { }
                }
            }
        }, IntentFilter(baseContext.packageName + ".CLASS_UPDATE"))
        val service = Intent(this, MService::class.java)
        toggleButton.setOnClickListener {
            if (toggleButton.isChecked)
                startService(service)
            else
                stopService(service)
        }
        startService(service)
        thread(true) {
            clazz = initClass()
            homework = initHomework()
            inflateFragment()
        }
    }


    private fun initClass(): JsonObject {
        val schedule = File(filesDir, "schedule.json")
        val bells = File(filesDir, "bells.json")
        try {
            if (!prefs.getBoolean("CustomCfg", false)) {
                val url = URL("http://" + prefs.getString("cAddress", getString(R.string.host)) + "/class?school_id=" + prefs.getString("school_id", "") + "&clazz=" + URLEncoder.encode(prefs.getString("class", ""), "UTF-8"))
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = resources.getInteger(R.integer.timeout)
                conn.readTimeout = resources.getInteger(R.integer.timeout)
                conn.connect()
                val reader = JsonParser().parse(conn.inputStream.reader())
                PrintWriter(FileWriter(schedule), true).println(try {
                    reader.asJsonObject["schedule"]
                } catch (e: Exception) {
                    JsonObject()
                })
                PrintWriter(FileWriter(bells), true).println(try {
                    reader.asJsonObject["bells"]
                } catch (e: Exception) {
                    JsonObject()
                })
            }
        } catch (e: Exception) {
            runOnUiThread { Toast.makeText(this, R.string.networkErr, Toast.LENGTH_LONG).show() }
        }
        return try {
            val jsonObject = JsonObject()
            val scheduleJ = JsonParser().parse(FileReader(schedule)).asJsonObject
            val bellsJ = JsonParser().parse(FileReader(bells)).asJsonObject
            jsonObject.add("schedule", scheduleJ)
            jsonObject.add("bells", bellsJ)
            jsonObject
        } catch (e: Exception) {
            runOnUiThread { Toast.makeText(this, R.string.configErr, Toast.LENGTH_LONG).show() }
            JsonObject()
        }
    }

    private fun initHomework(): JsonObject {
        val file = File(filesDir, "homework.json")
        if (!file.exists())
            PrintWriter(FileWriter(file), true).println(JsonObject())
        return JsonParser().parse(FileReader(file)).asJsonObject
    }

    private fun inflateFragment() {
        inflateFragment(Calendar.getInstance().get(Calendar.DAY_OF_WEEK), resources.getStringArray(R.array.days)[Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 1])
    }

    private fun inflateFragment(day: Int, dayName: String) {
        val bundle = Bundle()
        bundle.putString("day", day.toString())
        bundle.putString("dayName", dayName)
        val fragment = DayOfWeekFragment()
        fragment.arguments = bundle
        fragmentManager.beginTransaction().replace(R.id.content_frame, fragment).commit()
    }

    override fun onBackPressed() {
        if (drawer_layout.isDrawerOpen(GravityCompat.START)) {
            drawer_layout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menuMonday -> inflateFragment(2, item.title.toString())
            R.id.menuTuesday -> inflateFragment(3, item.title.toString())
            R.id.menuWednesday -> inflateFragment(4, item.title.toString())
            R.id.menuThursday -> inflateFragment(5, item.title.toString())
            R.id.menuFriday -> inflateFragment(6, item.title.toString())
            R.id.menuSaturday -> inflateFragment(7, item.title.toString())
            R.id.menuSettings -> startActivity(Intent(this, SettingsActivity::class.java))
            R.id.menuAbout -> startActivity(Intent(this, AboutActivity::class.java))
        }
        drawer_layout.closeDrawer(GravityCompat.START)
        return true
    }

}
