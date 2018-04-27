package pro.eugw.lessoncountdown.activity

import android.app.Activity
import android.content.*
import android.os.Bundle
import android.support.v4.content.LocalBroadcastManager
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import android.widget.ToggleButton
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import pro.eugw.lessoncountdown.MService
import pro.eugw.lessoncountdown.MainApp
import pro.eugw.lessoncountdown.R
import pro.eugw.lessoncountdown.list.schedule.MAdapter
import pro.eugw.lessoncountdown.list.schedule.MLesson
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.io.PrintWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.*
import kotlin.collections.ArrayList

class MainActivity : AppCompatActivity() {

    private val list = ArrayList<MLesson>()
    private val adapter = MAdapter(list, this, false)
    private lateinit var prefs: SharedPreferences
    private lateinit var host: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)
        prefs = getSharedPreferences("newPrefs", Context.MODE_PRIVATE)
        if (!prefs.contains("class") && !prefs.getBoolean("CustomCfg", false)) {
            startActivity(Intent(this, SettingsActivity::class.java))
            finish()
            return
        }
        host = prefs.getString("cAddress", getString(R.string.host))
        val toggle = findViewById<ToggleButton>(R.id.toggleButton)
        toggle.isChecked = (application as MainApp).running
        val filter = IntentFilter(baseContext.packageName + ".SERVICE_STATE")
        LocalBroadcastManager.getInstance(this).registerReceiver(object : BroadcastReceiver() {
            override fun onReceive(p0: Context?, p1: Intent?) {
                toggle.isChecked = p1!!.getBooleanExtra("isRun", false)
            }
        }, filter)
        val intent = Intent(this, MService::class.java)
        toggle.setOnClickListener {
            if (toggle.isChecked)
                startService(intent)
            else
                stopService(intent)
        }
        Thread {
            updateView()
            startService(intent)
        }.start()
    }



    private fun updateView() {
        list.clear()
        val job = initClass()
        val schedule = job["schedule"].asJsonObject
        val bells = job["bells"].asJsonObject
        val homework = initHomework()
        val dayOfWeek = Calendar.getInstance().get(Calendar.DAY_OF_WEEK).toString()
        if (!schedule.has(dayOfWeek) || !bells.has(dayOfWeek))
            return
        (0 until schedule.get(dayOfWeek).asJsonArray.size()).map {
            val b = bells.get(dayOfWeek).asJsonArray[it].asString.split("-")
            val s = schedule.get(dayOfWeek).asJsonArray[it].asString
            var homeworkS = ""
            if (homework.has(s))
                homeworkS = homework.get(s).asString
            list.add(MLesson(s, b[0] + "-" + b[1], homeworkS))
        }
        runOnUiThread { adapter.notifyDataSetChanged() }
    }

    private fun initClass(): JsonObject {
        val schedule = File(filesDir, "schedule.json")
        val bells = File(filesDir, "bells.json")
        try {
            if (!prefs.getBoolean("CustomCfg", false)) {
                val url = URL("http://" + host + "/class?school_id=" + prefs.getString("school_id", "") + "&clazz=" + URLEncoder.encode(prefs.getString("class", ""), "UTF-8"))
                val conn = url.openConnection() as HttpURLConnection
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
            e.printStackTrace()
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == 0)
            if (resultCode == Activity.RESULT_OK)
                Thread { updateView() }.start()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.user_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menuMonday -> {
                Thread {
                    val intent = Intent(this, DayOfWeekActivity::class.java)
                    val bundle = Bundle()
                    bundle.putString("day", "2")
                    bundle.putString("dayName", item.title.toString())
                    val job = initClass()
                    bundle.putString("schedule", job["schedule"].toString())
                    bundle.putString("bells", job["bells"].toString())
                    bundle.putString("homework", initHomework().toString())
                    intent.putExtras(bundle)
                    startActivityForResult(intent, 0)
                }.start()
                true
            }
            R.id.menuTuesday -> {
                Thread {
                    val intent = Intent(this, DayOfWeekActivity::class.java)
                    val bundle = Bundle()
                    bundle.putString("day", "3")
                    bundle.putString("dayName", item.title.toString())
                    val job = initClass()
                    bundle.putString("schedule", job["schedule"].toString())
                    bundle.putString("bells", job["bells"].toString())
                    bundle.putString("homework", initHomework().toString())
                    intent.putExtras(bundle)
                    startActivityForResult(intent, 0)
                }.start()
                true
            }
            R.id.menuWednesday -> {
                Thread {
                    val intent = Intent(this, DayOfWeekActivity::class.java)
                    val bundle = Bundle()
                    bundle.putString("day", "4")
                    bundle.putString("dayName", item.title.toString())
                    val job = initClass()
                    bundle.putString("schedule", job["schedule"].toString())
                    bundle.putString("bells", job["bells"].toString())
                    bundle.putString("homework", initHomework().toString())
                    intent.putExtras(bundle)
                    startActivityForResult(intent, 0)
                }.start()
                true
            }
            R.id.menuThursday -> {
                Thread {
                    val intent = Intent(this, DayOfWeekActivity::class.java)
                    val bundle = Bundle()
                    bundle.putString("day", "5")
                    bundle.putString("dayName", item.title.toString())
                    val job = initClass()
                    bundle.putString("schedule", job["schedule"].toString())
                    bundle.putString("bells", job["bells"].toString())
                    bundle.putString("homework", initHomework().toString())
                    intent.putExtras(bundle)
                    startActivityForResult(intent, 0)
                }.start()
                true
            }
            R.id.menuFriday -> {
                Thread {
                    val intent = Intent(this, DayOfWeekActivity::class.java)
                    val bundle = Bundle()
                    bundle.putString("day", "6")
                    bundle.putString("dayName", item.title.toString())
                    val job = initClass()
                    bundle.putString("schedule", job["schedule"].toString())
                    bundle.putString("bells", job["bells"].toString())
                    bundle.putString("homework", initHomework().toString())
                    intent.putExtras(bundle)
                    startActivityForResult(intent, 0)
                }.start()
                true
            }
            R.id.menuSaturday -> {
                Thread {
                    val intent = Intent(this, DayOfWeekActivity::class.java)
                    val bundle = Bundle()
                    bundle.putString("day", "7")
                    bundle.putString("dayName", item.title.toString())
                    val job = initClass()
                    bundle.putString("schedule", job["schedule"].toString())
                    bundle.putString("bells", job["bells"].toString())
                    bundle.putString("homework", initHomework().toString())
                    intent.putExtras(bundle)
                    startActivityForResult(intent, 0)
                }.start()
                true
            }
            R.id.menuSettings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }
            R.id.menuAbout -> {
                startActivity(Intent(this, AboutActivity::class.java))
                true
            }
            else -> true
        }
    }

}
