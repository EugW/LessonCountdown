package pro.eugw.lessoncountdown.activity

import android.app.AlarmManager
import android.app.AlertDialog
import android.app.PendingIntent
import android.content.*
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.text.method.LinkMovementMethod
import android.view.MenuItem
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.core.content.edit
import androidx.core.view.GravityCompat
import androidx.fragment.app.FragmentActivity
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.Volley
import com.google.android.material.navigation.NavigationView
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.nav_header_main.*
import kotlinx.android.synthetic.main.nav_header_main.view.*
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
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

class MainActivity : FragmentActivity(), NavigationView.OnNavigationItemSelectedListener {

    lateinit var prefs: SharedPreferences
    lateinit var broadcastManager: LocalBroadcastManager
    lateinit var queue: RequestQueue
    var clazz = JsonObject()
    var homework = JsonObject()
    private var saved = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        graphicalInit()
        thread(true) {
            variablesInit()
            postInit()
        }
    }

    private fun graphicalInit() {
        setContentView(R.layout.activity_main)
        val toggle = ActionBarDrawerToggle(this, drawer_layout, main_toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        drawer_layout.addDrawerListener(toggle)
        toggle.syncState()
        nav_view.setNavigationItemSelectedListener(this)
        val toggleButton = nav_view.getHeaderView(0).toggleButton
        toggleButton.setOnClickListener {
            if (toggleButton.isChecked) {
                File(filesDir, SERVICE_PID).createNewFile()
                if (::broadcastManager.isInitialized)
                    broadcastManager.sendBroadcast(Intent(baseContext.packageName + SERVICE_SIGNAL).putExtra("START", true))
                else
                    Toast.makeText(this, "BroadcastManager not initialized yet", Toast.LENGTH_SHORT).show()
            } else {
                File(filesDir, SERVICE_PID).delete()
                if (::broadcastManager.isInitialized)
                    broadcastManager.sendBroadcast(Intent(baseContext.packageName + SERVICE_SIGNAL).putExtra("STOP", true))
                else
                    Toast.makeText(this, "BroadcastManager not initialized yet", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun variablesInit() {
        prefs = getSharedPreferences(APP_PREFERENCES, Context.MODE_PRIVATE)
        queue = Volley.newRequestQueue(this)
        broadcastManager = LocalBroadcastManager.getInstance(this)
    }

    private fun postInit() {
        if (!::prefs.isInitialized || !::broadcastManager.isInitialized) return
        if (!prefs.getBoolean(LC_PP, false)) {
            val dialog = AlertDialog.Builder(this)
                    .setPositiveButton("Accept") { _, _ ->
                        prefs.edit {
                            putBoolean(LC_PP, true)
                        }
                    }
                    .setNeutralButton("Decline") { _, _ ->
                        prefs.edit {
                            putBoolean(LC_PP, false)
                        }
                        finish()
                        System.exit(0)
                    }
                    .setOnCancelListener {
                        prefs.edit {
                            putBoolean(LC_PP, false)
                        }
                        finish()
                        System.exit(0)
                    }
                    .setTitle(R.string.privacyPolicy)
                    .setMessage(R.string.requestPolicyAccept)
                    .create()
            dialog.show()
            dialog.findViewById<TextView>(android.R.id.message).movementMethod = LinkMovementMethod.getInstance()
        }
        if (prefs.getBoolean(LOCAL_MARKS_SERVICE, false)) {
            when (prefs.getInt(LOCAL_MODE, 0)) {
                0 -> {
                    val svcIntent = Intent(this, MarksListenerService::class.java)
                    val pendingIntent = PendingIntent.getService(this, 0, svcIntent, PendingIntent.FLAG_UPDATE_CURRENT)
                    val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
                    alarmManager.cancel(pendingIntent)
                    alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 1000, (prefs.getInt(LOCAL_SERVICE_DELAY, 15) * 60 * 1000).toLong(), pendingIntent)
                }
                1 -> {
                    val marksRequest = PeriodicWorkRequestBuilder<MarksListenerWorker>((prefs.getInt(LOCAL_SERVICE_DELAY, 15)).toLong(), TimeUnit.MINUTES).addTag(MAKRS_WORK).build()
                    WorkManager.getInstance().cancelAllWorkByTag(MAKRS_WORK)
                    WorkManager.getInstance().enqueue(marksRequest)
                }
            }
        }
        broadcastManager.registerReceiver(object : BroadcastReceiver() {
            override fun onReceive(p0: Context?, p1: Intent?) {
                toggleButton.isChecked = p1!!.getBooleanExtra("isRun", false)
            }
        }, IntentFilter(baseContext.packageName + SERVICE_STATE))
        val service = Intent(this, MService::class.java)
        broadcastManager.registerReceiver(object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                stopService(service)
                startService(service)
            }
        }, IntentFilter(baseContext.packageName + PEND_SERVICE_RESTART))
        initClass()
        File(filesDir, SERVICE_PID).createNewFile()
        startService(service)
        bindService(service, object : ServiceConnection {
            override fun onServiceDisconnected(p0: ComponentName?) {}

            override fun onServiceConnected(p0: ComponentName?, p1: IBinder?) {
                toggleButton.isChecked = (p1 as MService.MBinder).service.running
                unbindService(this)
            }
        }, Context.BIND_AUTO_CREATE)
    }

    fun initClass() {
        val schedule = File(filesDir, SCHEDULE_FILE)
        val bells = File(filesDir, BELLS_FILE)
        if (::queue.isInitialized && ::prefs.isInitialized)
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
        if (!(::prefs.isInitialized && ::queue.isInitialized))
            return
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
            Handler(mainLooper).postDelayed({
                main_toolbar.title = dayName
                val bundle = Bundle()
                bundle.putString("day", day.toString())
                val fragment = DOWFragment()
                fragment.arguments = bundle
                supportFragmentManager.beginTransaction().replace(R.id.content_frame, fragment).commit()
            }, FRAGMENT_DRAW_DELAY)
        } catch (e: Exception) {
        }
    }

    fun inflateKundelikFragment() {
        try {
            main_toolbar.menu.clear()
            Handler(mainLooper).postDelayed({
                main_toolbar.title = getString(R.string.kundelik)
                supportFragmentManager.beginTransaction().replace(R.id.content_frame, KundelikFragment()).commit()
            }, FRAGMENT_DRAW_DELAY)
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
            main_toolbar.menu.clear()
            Handler(mainLooper).postDelayed({
                main_toolbar.title = getString(R.string.settings)
                supportFragmentManager.beginTransaction().replace(R.id.content_frame, SettingsFragment()).commit()
             }, FRAGMENT_DRAW_DELAY)
        } catch (e: Exception) {

        }
    }

    private fun inflateHelpFragment() {
        try {
            main_toolbar.menu.clear()
            Handler(mainLooper).postDelayed({
                main_toolbar.title = getString(R.string.help)
                supportFragmentManager.beginTransaction().replace(R.id.content_frame, HelpFragment()).commit()
            }, FRAGMENT_DRAW_DELAY)
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
