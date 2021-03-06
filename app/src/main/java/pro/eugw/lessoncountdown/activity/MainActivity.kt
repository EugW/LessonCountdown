package pro.eugw.lessoncountdown.activity

import android.app.AlertDialog
import android.content.*
import android.os.Bundle
import android.os.Handler
import android.text.method.LinkMovementMethod
import android.view.MenuItem
import android.widget.TextView
import android.widget.ToggleButton
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.core.content.edit
import androidx.core.view.GravityCompat
import androidx.fragment.app.FragmentActivity
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.android.volley.RequestQueue
import com.android.volley.toolbox.Volley
import com.google.android.material.navigation.NavigationView
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.nav_header_main.*
import kotlinx.android.synthetic.main.nav_header_main.view.*
import pro.eugw.lessoncountdown.MService
import pro.eugw.lessoncountdown.R
import pro.eugw.lessoncountdown.fragment.DOWFragment
import pro.eugw.lessoncountdown.fragment.HelpFragment
import pro.eugw.lessoncountdown.fragment.SettingsFragment
import pro.eugw.lessoncountdown.util.*
import java.io.File
import java.io.FileReader
import java.util.*
import kotlin.system.exitProcess

class MainActivity : FragmentActivity(), NavigationView.OnNavigationItemSelectedListener {

    lateinit var prefs: SharedPreferences
    lateinit var queue: RequestQueue
    var schedule = JsonObject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = getSharedPreferences(APP_PREFERENCES, Context.MODE_PRIVATE)
        queue = Volley.newRequestQueue(this)
        setContentView(R.layout.activity_main)
        val toggle = ActionBarDrawerToggle(this, drawer_layout, main_toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        drawer_layout.addDrawerListener(toggle)
        toggle.syncState()
        nav_view.setNavigationItemSelectedListener(this)
        nav_view.getHeaderView(0).toggleButton.setOnClickListener {
            try {
                LocalBroadcastManager.getInstance(this).sendBroadcast(Intent(baseContext.packageName + SERVICE_SIGNAL).putExtra("SIG", !(it as ToggleButton).isChecked))
            } catch (e: Exception) {
                shortShow("LocalBroadcastManager.getInstance(this) Exception", this)
            }
        }
        LocalBroadcastManager.getInstance(this).registerReceiver(object : BroadcastReceiver() {
            override fun onReceive(p0: Context?, p1: Intent?) {
                if (p1 != null) {
                    if (toggleButton != null)
                        toggleButton.isChecked = p1.getBooleanExtra("isRun", false)
/*                    if (p1.getBooleanExtra("isRun", false)) {
                        shortShow("Service started", this@MainActivity)
                    } else {
                        shortShow("Service stopped", this@MainActivity)
                    }*/
                }
            }
        }, IntentFilter(baseContext.packageName + SERVICE_STATE))
        val service = Intent(this, MService::class.java)
        LocalBroadcastManager.getInstance(this).registerReceiver(object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                stopService(service)
                startForegroundService(service)
            }
        }, IntentFilter(baseContext.packageName + PEND_SERVICE_RESTART))
        updateSchedule()
        inflateDOWFragment()
        startForegroundService(service)
        if (!prefs.getBoolean(LC_PP, false)) {
            runOnUiThread {
                val dialog = AlertDialog.Builder(this)
                        .setPositiveButton("Accept") { _, _ ->
                            prefs.edit {
                                putBoolean(LC_PP, true)
                            }
                        }.setNeutralButton("Decline") { _, _ ->
                            prefs.edit {
                                putBoolean(LC_PP, false)
                            }
                            finish()
                            exitProcess(0)
                        }.setOnCancelListener {
                            prefs.edit {
                                putBoolean(LC_PP, false)
                            }
                            finish()
                            exitProcess(0)
                        }.setTitle(R.string.privacyPolicy)
                        .setMessage(R.string.requestPolicyAccept).create()
                dialog.show()
                dialog.findViewById<TextView>(android.R.id.message).movementMethod = LinkMovementMethod.getInstance()
            }
        }
    }

    fun updateSchedule() {
        val schedFile = File(filesDir, SCHEDULE_FILE)
        schedule = try {
            val jsonObject = JsonObject()
            val scheduleJ = JsonParser.parseReader(FileReader(schedFile)).asJsonObject
            jsonObject.add(SCHEDULE, scheduleJ)
            jsonObject
        } catch (e: Exception) {
            shortShow(R.string.configErr, this)
            JsonObject()
        }
    }

    private fun inflateDOWFragment() {
        with(Calendar.getInstance().get(Calendar.DAY_OF_WEEK)) {
            if (this in 2..7)
                inflateDOWFragment(this, resources.getStringArray(R.array.days)[Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 1])
        }
    }

    private fun inflateDOWFragment(day: Int, dayName: String) {
        try {
            Handler(mainLooper).postDelayed({
                val bundle = Bundle()
                bundle.putString("day", day.toString())
                bundle.putString("dayName", dayName)
                val fragment = DOWFragment()
                fragment.arguments = bundle
                supportFragmentManager.beginTransaction().replace(R.id.content_frame, fragment).commit()
            }, FRAGMENT_DRAW_DELAY)
        } catch (e: Exception) {
        }
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
            R.id.menuSettings -> inflateSettingsFragment()
            R.id.menuHelp -> inflateHelpFragment()
        }
        return true
    }

}
