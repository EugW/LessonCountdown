package pro.eugw.lessoncountdown.activity

import android.content.*
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.view.MenuItem
import android.widget.Toast
import android.widget.ToggleButton
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.core.content.edit
import androidx.core.view.GravityCompat
import androidx.fragment.app.FragmentActivity
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.Volley
import com.google.android.material.navigation.NavigationView
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.android.synthetic.main.activity_main.*
import pro.eugw.lessoncountdown.MService
import pro.eugw.lessoncountdown.R
import pro.eugw.lessoncountdown.fragment.DOWFragment
import pro.eugw.lessoncountdown.fragment.HelpFragment
import pro.eugw.lessoncountdown.fragment.KundelikFragment
import pro.eugw.lessoncountdown.fragment.SettingsFragment
import pro.eugw.lessoncountdown.util.*
import pro.eugw.lessoncountdown.util.network.JsArRe
import pro.eugw.lessoncountdown.util.network.JsObRe
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.io.PrintWriter
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.*


class MainActivity : FragmentActivity(), NavigationView.OnNavigationItemSelectedListener {

    lateinit var prefs: SharedPreferences
    lateinit var broadcastManager: LocalBroadcastManager
    lateinit var queue: RequestQueue
    var clazz = JsonObject()
    var homework = JsonObject()
    private var saved = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = getSharedPreferences(APP_PREFERENCES, Context.MODE_PRIVATE)
        setContentView(R.layout.activity_main)
        val toggle = ActionBarDrawerToggle(this, drawer_layout, main_toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        drawer_layout.addDrawerListener(toggle)
        toggle.syncState()
        nav_view.setNavigationItemSelectedListener(this)
        queue = Volley.newRequestQueue(this)
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
            } else {
                File(filesDir, SERVICE_PID).delete()
                broadcastManager.sendBroadcast(Intent(baseContext.packageName + SERVICE_SIGNAL).putExtra("STOP", true))
            }
        }
        initClass()
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

    fun initClass() {
        val schedule = File(filesDir, SCHEDULE_FILE)
        val bells = File(filesDir, BELLS_FILE)
        if (!prefs.getBoolean(CUSTOM_CONFIG, false)) {
            val host = prefs.getString(CUSTOM_ADDRESS, getString(R.string.host))
            val schoolId = prefs.getString(SCHOOL_ID, "")
            val className = URLEncoder.encode(prefs.getString(CLASS, ""), "UTF-8")
            queue.add(JsObRe(Request.Method.GET, "https://$host/class?school_id=$schoolId&clazz=$className",
                    Response.Listener {
                        PrintWriter(FileWriter(schedule), true).println(it[SCHEDULE].asJsonObject)
                        PrintWriter(FileWriter(bells), true).println(it[BELLS].asJsonObject)
                        clazz = try {
                            val jsonObject = JsonObject()
                            val scheduleJ = JsonParser().parse(FileReader(schedule)).asJsonObject
                            val bellsJ = JsonParser().parse(FileReader(bells)).asJsonObject
                            jsonObject.add(SCHEDULE, scheduleJ)
                            jsonObject.add(BELLS, bellsJ)
                            jsonObject
                        } catch (e: Exception) {
                            Toast.makeText(this, R.string.configErr, Toast.LENGTH_SHORT).show()
                            JsonObject()
                        }
                        initHomework()
                        inflateDOWFragment()
                    },
                    Response.ErrorListener {
                        Toast.makeText(this, R.string.networkErr, Toast.LENGTH_SHORT).show()
                    }
            ))
        } else {
            clazz = try {
                val jsonObject = JsonObject()
                val scheduleJ = JsonParser().parse(FileReader(schedule)).asJsonObject
                val bellsJ = JsonParser().parse(FileReader(bells)).asJsonObject
                jsonObject.add(SCHEDULE, scheduleJ)
                jsonObject.add(BELLS, bellsJ)
                jsonObject
            } catch (e: Exception) {
                Toast.makeText(this, R.string.configErr, Toast.LENGTH_SHORT).show()
                JsonObject()
            }
            initHomework()
            inflateDOWFragment()
        }
    }

    fun initScheduleKundelik() {
        val token = prefs.getString(KUNDELIK_TOKEN, "")!!
        if (token.length < 5)
            return
        queue.add(JsObRe(Request.Method.GET, "https://api.kundelik.kz/v1/users/me?access_token=$token",
                Response.Listener { responsePerson ->
                    val personId = responsePerson["personId"].asString
                    Toast.makeText(this, "Person ID request succeed: $personId", Toast.LENGTH_SHORT).show()
                    queue.add(JsArRe(Request.Method.GET, "https://api.kundelik.kz/v1/users/me/schools?access_token=$token",
                            Response.Listener { responseSchool ->
                                val schoolId = responseSchool[0].asString
                                Toast.makeText(this, "School ID request succeed: $schoolId", Toast.LENGTH_SHORT).show()
                                queue.add(JsArRe(Request.Method.GET, "https://api.kundelik.kz/v1/persons/$personId/schools/$schoolId/edu-groups?access_token=$token",
                                        Response.Listener { responseEduGroup ->
                                            val eduGroupId = responseEduGroup[0].asJsonObject["id"].asString
                                            Toast.makeText(this, "Edu Group ID request succeed: $eduGroupId", Toast.LENGTH_SHORT).show()
                                            val calendar = Calendar.getInstance()
                                            calendar.set(Calendar.DAY_OF_WEEK, 2)
                                            val firstDay = calendar.time
                                            calendar.add(Calendar.WEEK_OF_YEAR, 1)
                                            calendar.set(Calendar.DAY_OF_WEEK, 1)
                                            val lastDay = calendar.time
                                            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                                            queue.add(JsObRe(Request.Method.GET, "https://api.kundelik.kz/v1/persons/$personId/groups/$eduGroupId/schedules?startDate=${sdf.format(firstDay)}&endDate=${sdf.format(lastDay)}&access_token=$token",
                                                    Response.Listener { responseSchedule ->
                                                        val scheduleArray = responseSchedule["days"].asJsonArray
                                                        Toast.makeText(this, "Schedule request succeed: $scheduleArray", Toast.LENGTH_SHORT).show()
                                                        convertKundelikToMSchedule(scheduleArray)
                                                    },
                                                    Response.ErrorListener { error ->
                                                        Toast.makeText(this, "Schedule request failed: ${error.message}", Toast.LENGTH_SHORT).show()
                                                    }
                                            ))
                                        },
                                        Response.ErrorListener { error ->
                                            Toast.makeText(this, "Edu Group ID request failed: ${error.message}", Toast.LENGTH_SHORT).show()
                                        }
                                ))
                            },
                            Response.ErrorListener { error ->
                                Toast.makeText(this, "School ID request failed: ${error.message}", Toast.LENGTH_SHORT).show()
                            }
                    ))
                },
                Response.ErrorListener { error ->
                    Toast.makeText(this, "Person ID request failed: ${error.message}", Toast.LENGTH_SHORT).show()
                }
        ))
    }

    private fun convertKundelikToMSchedule(origin: JsonArray) {
        val schedule = File(filesDir, SCHEDULE_FILE)
        val bells = File(filesDir, BELLS_FILE)
        origin.forEach {
            val obj = it.asJsonObject
            obj.remove("marks")
            obj.remove("works")
            obj.remove("homeworks")
            obj.remove("workTypes")
            obj.remove("lessonLogEntries")
            obj.remove("teaches")
            obj.remove("nextDate")
        }
        val scheduleObject = JsonObject()
        val bellsObject = JsonObject()
        origin.forEach {
            val lss = it.asJsonObject["lessons"].asJsonArray
            val sdlArr = JsonArray()
            val bllArr = JsonArray()
            lss.forEach { lesson ->
                val subId = lesson.asJsonObject["subjectId"].asLong
                val name = it.asJsonObject["subjects"].asJsonArray.find { prd -> prd.asJsonObject["id"].asLong == subId }!!.asJsonObject["name"].asString
                sdlArr.add(name)
                val hours = lesson.asJsonObject["hours"].asString.replace(" ", "")
                bllArr.add(hours)
            }
            var dow = origin.indexOf(it) + 2
            if (dow == 8)
                dow = 1
            scheduleObject.add("$dow", sdlArr)
            bellsObject.add("$dow", bllArr)
        }
        PrintWriter(FileWriter(schedule), true).println(scheduleObject)
        PrintWriter(FileWriter(bells), true).println(bellsObject)
        prefs.edit {
            putBoolean(CUSTOM_CONFIG, true)
        }
        initClass()
    }

    private fun initHomework() {
        val file = File(filesDir, "homework.json")
        if (!file.exists())
            PrintWriter(FileWriter(file), true).println(JsonObject())
        homework = JsonParser().parse(FileReader(file)).asJsonObject
    }

    private fun inflateDOWFragment() {
        inflateDOWFragment(Calendar.getInstance().get(Calendar.DAY_OF_WEEK), resources.getStringArray(R.array.days)[Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 1])
    }

    private fun inflateDOWFragment(day: Int, dayName: String) {
        try {
            val bundle = Bundle()
            bundle.putString("day", day.toString())
            bundle.putString("dayName", dayName)
            Handler(mainLooper).postDelayed({
                val fragment = DOWFragment()
                fragment.arguments = bundle
                supportFragmentManager.beginTransaction().replace(R.id.content_frame, fragment).commit()
            }, 500)
        } catch (e: Exception) {

        }
    }

    fun inflateKundelikFragment() {
        try {
            Handler(mainLooper).postDelayed({
                supportFragmentManager.beginTransaction().replace(R.id.content_frame, KundelikFragment()).commit()
            }, 500)
        } catch (e: Exception) {

        }
    }

    private fun inflatePatchesFragment() {
        /*if (saved)
            return
        Handler(mainLooper).postDelayed({
            try {
                supportFragmentManager.beginTransaction().replace(R.id.content_frame, PatchesFragment()).commit()
            } catch (e: Exception) { }
        }, 500)*/
        Toast.makeText(this, R.string.patches, Toast.LENGTH_SHORT).show()
    }

    private fun inflateSettingsFragment() {
        try {
            Handler(mainLooper).postDelayed({
                supportFragmentManager.beginTransaction().replace(R.id.content_frame, SettingsFragment()).commit()
             }, 500)
        } catch (e: Exception) {

        }
    }

    private fun inflateHelpFragment() {
        try {
            Handler(mainLooper).postDelayed({
                supportFragmentManager.beginTransaction().replace(R.id.content_frame, HelpFragment()).commit()
            }, 500)
        } catch (e: Exception) {

        }
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
            R.id.menuMonday -> inflateDOWFragment(2, item.title.toString())
            R.id.menuTuesday -> inflateDOWFragment(3, item.title.toString())
            R.id.menuWednesday -> inflateDOWFragment(4, item.title.toString())
            R.id.menuThursday -> inflateDOWFragment(5, item.title.toString())
            R.id.menuFriday -> inflateDOWFragment(6, item.title.toString())
            R.id.menuSaturday -> inflateDOWFragment(7, item.title.toString())
            R.id.menuSunday -> inflateDOWFragment(1, item.title.toString())
            R.id.menuKundelik -> inflateKundelikFragment()
            R.id.menuPatches -> inflatePatchesFragment()
            R.id.menuSettings -> inflateSettingsFragment()
            R.id.menuHelp -> inflateHelpFragment()
        }
        return true
    }


    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        saved = true
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle?) {
        super.onRestoreInstanceState(savedInstanceState)
        saved = false
    }

}
