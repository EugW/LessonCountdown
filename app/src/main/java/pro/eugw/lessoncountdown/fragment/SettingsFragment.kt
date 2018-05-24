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
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.jaredrummler.android.colorpicker.ColorPickerDialog
import com.jaredrummler.android.colorpicker.ColorPickerDialogListener
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_settings.*
import pro.eugw.lessoncountdown.R
import pro.eugw.lessoncountdown.activity.MainActivity
import pro.eugw.lessoncountdown.activity.SearchActivity
import pro.eugw.lessoncountdown.util.*
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.io.PrintWriter
import java.net.HttpURLConnection
import java.net.URL

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
        host = mActivity.prefs.getString(CUSTOM_ADDRESS, getString(R.string.host))
        initClass()
        initCustomCfg()
        initOwnServer()
        initTheme()
        initCustomColors()
        initBigNotification()
    }

    private fun initClass() {
        val string = mActivity.prefs.getString(CLASS, "")
        if (string.isNotEmpty()) {
            val text = selectedClass
            text.visibility = View.VISIBLE
            text.text = string.replace(".", "")
        }
        selClassLayout.setOnClickListener {
            val intent = Intent(mActivity, SearchActivity::class.java)
            startActivityForResult(intent, SEARCH_REQUEST_CODE)
        }
    }

    private fun initCustomCfg() {
        switchCustomCfg.isChecked = mActivity.prefs.getBoolean(CUSTOM_CONFIG, false)
        customLayout.visibility = if (mActivity.prefs.getBoolean(CUSTOM_CONFIG, false)) View.VISIBLE else View.GONE
        switchCustomCfg.setOnCheckedChangeListener { _, state ->
            if (state)
                customLayout.visibility = View.VISIBLE
            else
                customLayout.visibility = View.GONE
            mActivity.prefs.edit().putBoolean(CUSTOM_CONFIG, state).apply()
        }
        buttonCopy.setOnClickListener {
            val jObject = JsonObject()
            jObject.add(SCHEDULE, JsonParser().parse(FileReader(File(mActivity.filesDir, SCHEDULE_FILE))))
            jObject.add(BELLS, JsonParser().parse(FileReader(File(mActivity.filesDir, BELLS_FILE))))
            val clip = mActivity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clip.primaryClip = ClipData.newPlainText("lcConfig", jObject.toString())
        }
        buttonPaste.setOnClickListener {
            val clip = mActivity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            try {
                val jObject = JsonParser().parse(clip.primaryClip.getItemAt(0).text.toString()).asJsonObject
                PrintWriter(FileWriter(File(mActivity.filesDir, SCHEDULE_FILE)), true).println(jObject[SCHEDULE])
                PrintWriter(FileWriter(File(mActivity.filesDir, BELLS_FILE)), true).println(jObject[BELLS])
            } catch (e: Exception) {
                Toast.makeText(mActivity, R.string.pasteErr, Toast.LENGTH_LONG).show()
                e.printStackTrace()
            }
        }
    }

    private fun initTheme() {
        switchDarkTheme.isChecked = mActivity.prefs.getBoolean(DARK_THEME, false)
        switchDarkTheme.setOnCheckedChangeListener { _, state ->
            mActivity.prefs.edit().putBoolean(DARK_THEME, state).apply()
            mActivity.recreate()
        }
    }

    private fun initOwnServer() {
        if (mActivity.prefs.getString(CUSTOM_ADDRESS, getString(R.string.host)) != getString(R.string.host)) {
            ownServerAddress.visibility = View.VISIBLE
            ownServerAddress.text = host
        }
        ownServer.setOnClickListener {
            val ab = AlertDialog.Builder(mActivity)
            ab.setTitle(R.string.address)
            val input = EditText(mActivity)
            input.setText(host)
            ab.setView(input)
            ab.setPositiveButton(getString(android.R.string.ok), { _, _ ->
                host = try {
                    val url = URL(input.text.toString())
                    val conn = url.openConnection() as HttpURLConnection
                    conn.connectTimeout = HTTP_TIMEOUT
                    conn.readTimeout = HTTP_TIMEOUT
                    conn.connect()
                    ownServerAddress.visibility = View.VISIBLE
                    ownServerAddress.text = input.text
                    input.text.toString()
                } catch (e: Exception) {
                    getString(R.string.host)
                }
                mActivity.prefs.edit().putString(CUSTOM_ADDRESS, host).apply()
            })
            ab.show()
        }
    }

    private fun initCustomColors() {
        val localIntent = Intent(mActivity.packageName + NOTIFICATION_COLOR_UPDATE)
        switchNotificationColor.isChecked = mActivity.prefs.getBoolean(CUSTOM_COLOR, false)
        customColors.visibility = if (mActivity.prefs.getBoolean(CUSTOM_COLOR, false)) View.VISIBLE else View.GONE
        switchNotificationColor.setOnCheckedChangeListener { _, state ->
            if (state) {
                customColors.visibility = View.VISIBLE
                broadcastManager.sendBroadcast(localIntent.putExtra("cVal", true))
            }
            else {
                customColors.visibility = View.GONE
                broadcastManager.sendBroadcast(localIntent.putExtra("cVal", false))
            }
            mActivity.prefs.edit().putBoolean(CUSTOM_COLOR, state).apply()
        }
        val title = buttonChooseDialogTitle
        colorTitle = mActivity.prefs.getInt(TITLE_COLOR, Color.parseColor("#000000"))
        title.setBackgroundColor(colorTitle)
        title.setOnClickListener {
            val builder = ColorPickerDialog.newBuilder().setAllowPresets(false).setDialogType(0).setColor(colorTitle).create()
            builder.setColorPickerDialogListener(object : ColorPickerDialogListener {
                override fun onDialogDismissed(dialogId: Int) {}

                override fun onColorSelected(dialogId: Int, color: Int) {
                    colorTitle = color
                    title.setBackgroundColor(color)
                    mActivity.prefs.edit().putInt(TITLE_COLOR, colorTitle).apply()
                    broadcastManager.sendBroadcast(localIntent)
                }
            })
            builder.show(mActivity.fragmentManager, COLOR_PICKER_DIALOG)
        }
        val textTime = buttonChooseDialogTime
        colorTime = mActivity.prefs.getInt(TIME_COLOR, Color.parseColor("#999999"))
        textTime.setBackgroundColor(colorTime)
        textTime.setOnClickListener {
            val builder = ColorPickerDialog.newBuilder().setAllowPresets(false).setDialogType(0).setColor(colorTime).create()
            builder.setColorPickerDialogListener(object : ColorPickerDialogListener {
                override fun onDialogDismissed(dialogId: Int) {}

                override fun onColorSelected(dialogId: Int, color: Int) {
                    colorTime = color
                    textTime.setBackgroundColor(color)
                    mActivity.prefs.edit().putInt(TIME_COLOR, colorTime).apply()
                    broadcastManager.sendBroadcast(localIntent)
                }
            })
            builder.show(mActivity.fragmentManager, COLOR_PICKER_DIALOG)
        }
        val textLessons = buttonChooseDialogLessons
        colorLessons = mActivity.prefs.getInt(LESSONS_COLOR, Color.parseColor("#999999"))
        textLessons.setBackgroundColor(colorLessons)
        textLessons.setOnClickListener {
            val builder = ColorPickerDialog.newBuilder().setAllowPresets(false).setDialogType(0).setColor(colorLessons).create()
            builder.setColorPickerDialogListener(object : ColorPickerDialogListener {
                override fun onDialogDismissed(dialogId: Int) {}

                override fun onColorSelected(dialogId: Int, color: Int) {
                    colorLessons = color
                    textLessons.setBackgroundColor(color)
                    mActivity.prefs.edit().putInt(LESSONS_COLOR, colorLessons).apply()
                    broadcastManager.sendBroadcast(localIntent)
                }
            })
            builder.show(mActivity.fragmentManager, COLOR_PICKER_DIALOG)
        }
        val background = buttonChooseDialogBackground
        colorBackground = mActivity.prefs.getInt(BACKGROUND_COLOR, Color.parseColor("#ffffff"))
        background.setBackgroundColor(colorBackground)
        background.setOnClickListener {
            val builder = ColorPickerDialog.newBuilder().setAllowPresets(false).setDialogType(0).setColor(colorBackground).create()
            builder.setColorPickerDialogListener(object : ColorPickerDialogListener {
                override fun onDialogDismissed(dialogId: Int) {}

                override fun onColorSelected(dialogId: Int, color: Int) {
                    colorBackground = color
                    background.setBackgroundColor(color)
                    mActivity.prefs.edit().putInt(BACKGROUND_COLOR, colorBackground).apply()
                    broadcastManager.sendBroadcast(localIntent)
                }
            })
            builder.show(mActivity.fragmentManager, COLOR_PICKER_DIALOG)
        }
    }
    
    private fun initBigNotification() {
        switchBigNotification.isChecked = mActivity.prefs.getBoolean(BIG_NOTIFICATION, false)
        switchBigNotification.setOnCheckedChangeListener { _, isChecked -> 
            mActivity.prefs.edit().putBoolean(BIG_NOTIFICATION, isChecked).apply()
            broadcastManager.sendBroadcast(Intent(mActivity.packageName + NOTIFICATION_STYLE_UPDATE))
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == SEARCH_REQUEST_CODE && resultCode == Activity.RESULT_OK && data != null) {
            val extras = data.extras
            mActivity.prefs.edit().putString(CLASS, extras[CLASS] as String).putString(SCHOOL_ID, extras[SCHOOL_ID] as String).apply()
            val text = selectedClass
            text.visibility = View.VISIBLE
            text.text = (extras[CLASS] as String).replace(".", "")
            mActivity.clazz = mActivity.initClass()
            mActivity.homework = mActivity.initHomework()
        }
    }
    
}