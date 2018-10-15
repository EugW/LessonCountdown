package pro.eugw.lessoncountdown.activity

import android.content.*
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.view.MenuItem
import android.widget.Toast
import android.widget.ToggleButton
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.core.view.GravityCompat
import androidx.fragment.app.FragmentActivity
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.material.navigation.NavigationView
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.android.synthetic.main.activity_main.*
import pro.eugw.lessoncountdown.MService
import pro.eugw.lessoncountdown.R
import pro.eugw.lessoncountdown.fragment.DayOfWeekFragment
import pro.eugw.lessoncountdown.fragment.KundelikFragment
import pro.eugw.lessoncountdown.fragment.SettingsFragment
import pro.eugw.lessoncountdown.util.*
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.io.PrintWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.*
import kotlin.concurrent.thread


class MainActivity : FragmentActivity(), NavigationView.OnNavigationItemSelectedListener {

    lateinit var prefs: SharedPreferences
    lateinit var broadcastManager: LocalBroadcastManager
    var clazz = JsonObject()
    var homework = JsonObject()
    var kundelikMenu: MenuItem? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = getSharedPreferences(APP_PREFERENCES, Context.MODE_PRIVATE)
        setContentView(R.layout.activity_main)
        val toggle = ActionBarDrawerToggle(this, drawer_layout, main_toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        drawer_layout.addDrawerListener(toggle)
        toggle.syncState()
        nav_view.setNavigationItemSelectedListener(this)
        kundelikMenu = nav_view.menu.add("Kundelik")
        broadcastManager = LocalBroadcastManager.getInstance(this)
        val toggleButton = nav_view.getHeaderView(0).findViewById<ToggleButton>(R.id.toggleButton)
        broadcastManager.registerReceiver(object : BroadcastReceiver() {
            override fun onReceive(p0: Context?, p1: Intent?) {
                toggleButton.isChecked = p1!!.getBooleanExtra("isRun", false)
            }
        }, IntentFilter(baseContext.packageName + SERVICE_STATE))
        toggleButton.setOnClickListener {
            if (toggleButton.isChecked) {
                File(filesDir, SERVICE_PID).createNewFile()
                broadcastManager.sendBroadcast(Intent(baseContext.packageName + SERVICE_SIGNAL).putExtra("START", true))
            }
            else {
                File(filesDir, SERVICE_PID).delete()
                broadcastManager.sendBroadcast(Intent(baseContext.packageName + SERVICE_SIGNAL).putExtra("STOP", true))
            }
        }
        thread(true) {
            clazz = initClass()
            homework = initHomework()
            inflateFragment()
        }
        File(filesDir, SERVICE_PID).createNewFile()
        val service = Intent(this, MService::class.java)
        startService(service)
        bindService(service, object : ServiceConnection {
            override fun onServiceDisconnected(p0: ComponentName?) {}

            override fun onServiceConnected(p0: ComponentName?, p1: IBinder?) {
                toggleButton.isChecked = (p1 as MService.MBinder).service.running
                unbindService(this)
            }
        }, Context.BIND_AUTO_CREATE)
        broadcastManager.registerReceiver(object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                stopService(service)
                startService(service)
            }
        }, IntentFilter(baseContext.packageName + PEND_SERVICE_RESTART))
    }

    fun initClass(): JsonObject {
        val schedule = File(filesDir, SCHEDULE_FILE)
        val bells = File(filesDir, BELLS_FILE)
        try {
            if (!prefs.getBoolean(CUSTOM_CONFIG, false)) {
                val url = URL("http://" + prefs.getString(CUSTOM_ADDRESS, getString(R.string.host)) + "/class?school_id=" + prefs.getString(SCHOOL_ID, "") + "&clazz=" + URLEncoder.encode(prefs.getString(CLASS, ""), "UTF-8"))
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = HTTP_TIMEOUT
                conn.readTimeout = HTTP_TIMEOUT
                conn.connect()
                val reader = JsonParser().parse(conn.inputStream.reader())
                PrintWriter(FileWriter(schedule), true).println(reader.asJsonObject[SCHEDULE])
                PrintWriter(FileWriter(bells), true).println(reader.asJsonObject[BELLS])
            } else if (!schedule.exists() || !bells.exists()) {
                PrintWriter(FileWriter(schedule), true).println(JsonObject())
                PrintWriter(FileWriter(bells), true).println(JsonObject())
            }
        } catch (e: Exception) {
            e.printStackTrace()
            PrintWriter(FileWriter(schedule), true).println(JsonObject())
            PrintWriter(FileWriter(bells), true).println(JsonObject())
            runOnUiThread { Toast.makeText(this, R.string.networkErr, Toast.LENGTH_LONG).show() }
        }
        return try {
            val jsonObject = JsonObject()
            val scheduleJ = JsonParser().parse(FileReader(schedule)).asJsonObject
            val bellsJ = JsonParser().parse(FileReader(bells)).asJsonObject
            jsonObject.add(SCHEDULE, scheduleJ)
            jsonObject.add(BELLS, bellsJ)
            jsonObject
        } catch (e: Exception) {
            runOnUiThread { Toast.makeText(this, R.string.configErr, Toast.LENGTH_LONG).show() }
            JsonObject()
        }
    }

    fun initHomework(): JsonObject {
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
        Handler(mainLooper).postDelayed({
            try {
                val fragment = DayOfWeekFragment()
                fragment.arguments = bundle
                supportFragmentManager.beginTransaction().replace(R.id.content_frame, fragment).commit()
            } catch (e: Exception) { }
        }, 500)
    }

    private fun inflateKundelikFragment() {
        Handler(mainLooper).postDelayed({
            try {
                val fragment = KundelikFragment()
                supportFragmentManager.beginTransaction().replace(R.id.content_frame, fragment).commit()
            } catch (e: Exception) { }
        }, 500)
    }


    override fun onBackPressed() {
        if (drawer_layout.isDrawerOpen(GravityCompat.START)) {
            drawer_layout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        drawer_layout.closeDrawer(GravityCompat.START)
        when (item.itemId) {
            R.id.menuMonday -> inflateFragment(2, item.title.toString())
            R.id.menuTuesday -> inflateFragment(3, item.title.toString())
            R.id.menuWednesday -> inflateFragment(4, item.title.toString())
            R.id.menuThursday -> inflateFragment(5, item.title.toString())
            R.id.menuFriday -> inflateFragment(6, item.title.toString())
            R.id.menuSaturday -> inflateFragment(7, item.title.toString())
            R.id.menuSunday -> inflateFragment(1, item.title.toString())
            R.id.menuSettings -> Handler(mainLooper).postDelayed({ supportFragmentManager.beginTransaction().replace(R.id.content_frame, SettingsFragment()).commit() }, 500)
            R.id.menuHelp -> startActivity(Intent(this, HelpActivity::class.java))
            kundelikMenu?.itemId -> inflateKundelikFragment()
        }
        return true
    }

}
