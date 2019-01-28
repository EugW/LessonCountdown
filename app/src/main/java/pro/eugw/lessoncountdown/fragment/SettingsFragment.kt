package pro.eugw.lessoncountdown.fragment

import android.app.Activity
import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.custom_colors_layout.*
import kotlinx.android.synthetic.main.custom_config_layout.*
import kotlinx.android.synthetic.main.fragment_settings.*
import pro.eugw.lessoncountdown.R
import pro.eugw.lessoncountdown.activity.MainActivity
import pro.eugw.lessoncountdown.util.*
import pro.eugw.lessoncountdown.util.color.ColorPickerDialog
import pro.eugw.lessoncountdown.util.color.ColorPickerDialogListener
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.io.PrintWriter
import java.net.URL
import javax.net.ssl.HttpsURLConnection

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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        super.onStart()
        mActivity = activity as MainActivity
        mActivity.main_toolbar.title = getString(R.string.settings)
        mActivity.main_toolbar.menu.clear()
        broadcastManager = mActivity.broadcastManager
        host = mActivity.prefs.getString(CUSTOM_ADDRESS, getString(R.string.host)) as String
        initClass()
        initCustomCfg()
        initOwnServer()
        initCustomColors()
    }

    private fun initClass() {
        val string = mActivity.prefs.getString(CLASS, "") as String
        if (string.isNotEmpty()) {
            val text = selectedClass
            text.visibility = View.VISIBLE
            text.text = string.replace(".", "")
        }
        selClassLayout.setOnClickListener {
            val fragment = SearchDialog()
            fragment.setTargetFragment(this, SEARCH_REQUEST_CODE)
            fragmentManager!!.beginTransaction().add(fragment, "search-dialog").commit()
        }
    }

    private fun initCustomCfg() {
        switchCustomCfg.isChecked = mActivity.prefs.getBoolean(CUSTOM_CONFIG, false)
        includedConfig.visibility = if (mActivity.prefs.getBoolean(CUSTOM_CONFIG, false)) View.VISIBLE else View.GONE
        switchCustomCfg.setOnCheckedChangeListener { _, state ->
            if (state)
                includedConfig.visibility = View.VISIBLE
            else
                includedConfig.visibility = View.GONE
            mActivity.prefs.edit { putBoolean(CUSTOM_CONFIG, state) }
        }
        switchVisibleEditing.isChecked = mActivity.prefs.getBoolean(HIDE_CONTROLS, false)
        switchVisibleEditing.setOnCheckedChangeListener { _, state -> mActivity.prefs.edit { putBoolean(HIDE_CONTROLS, state) } }
        switchEvenOddWeeks.isChecked = mActivity.prefs.getBoolean(EVEN_ODD_WEEKS, false)
        switchEvenOddWeeks.setOnCheckedChangeListener { _, state -> mActivity.prefs.edit { putBoolean(EVEN_ODD_WEEKS, state) } }
        switchInverseEvenOddWeeks.isChecked = mActivity.prefs.getBoolean(INVERSE_EVEN_ODD_WEEKS, false)
        switchInverseEvenOddWeeks.setOnCheckedChangeListener { _, state -> mActivity.prefs.edit { putBoolean(INVERSE_EVEN_ODD_WEEKS, state) } }
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
                val jObject = JsonParser().parse(clip.primaryClip?.getItemAt(0)?.text.toString()).asJsonObject
                if (jObject.has("1e")
                        || jObject.has("2e")
                        || jObject.has("3e")
                        || jObject.has("4e")
                        || jObject.has("5e")
                        || jObject.has("6e")
                        || jObject.has("7e")) {
                    switchEvenOddWeeks.isChecked = true
                    mActivity.prefs.edit { putBoolean(EVEN_ODD_WEEKS, true) }
                }
                PrintWriter(FileWriter(File(mActivity.filesDir, SCHEDULE_FILE)), true).println(jObject[SCHEDULE])
                PrintWriter(FileWriter(File(mActivity.filesDir, BELLS_FILE)), true).println(jObject[BELLS])
                mActivity.initClass()
            } catch (e: Exception) {
                Toast.makeText(mActivity, R.string.pasteErr, Toast.LENGTH_SHORT).show()
            }
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
            ab.setPositiveButton(getString(android.R.string.ok)) { _, _ ->
                host = try {
                    val url = URL("https://" + input.text)
                    val conn = url.openConnection() as HttpsURLConnection
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
            }
            ab.show()
        }
    }

    private fun initCustomColors() {
        val localIntent = Intent(mActivity.packageName + NOTIFICATION_COLOR_UPDATE)
        switchNotificationColor.isChecked = mActivity.prefs.getBoolean(CUSTOM_COLOR, false)
        includedColors.visibility = if (mActivity.prefs.getBoolean(CUSTOM_COLOR, false)) View.VISIBLE else View.GONE
        switchNotificationColor.setOnCheckedChangeListener { _, state ->
            if (state) {
                includedColors.visibility = View.VISIBLE
                broadcastManager.sendBroadcast(localIntent.putExtra("cVal", true))
            }
            else {
                includedColors.visibility = View.GONE
                broadcastManager.sendBroadcast(localIntent.putExtra("cVal", false))
            }
            mActivity.prefs.edit().putBoolean(CUSTOM_COLOR, state).apply()
        }
        val title = buttonChooseDialogTitle
        colorTitle = mActivity.prefs.getInt(TITLE_COLOR, Color.parseColor("#000000"))
        title.setBackgroundColor(colorTitle)
        title.setOnClickListener {
            val builder = ColorPickerDialog.newBuilder().setColor(colorTitle).create()
            builder.setColorPickerDialogListener(object : ColorPickerDialogListener {
                override fun onDialogDismissed(dialogId: Int) {}

                override fun onColorSelected(dialogId: Int, color: Int) {
                    colorTitle = color
                    title.setBackgroundColor(color)
                    mActivity.prefs.edit().putInt(TITLE_COLOR, colorTitle).apply()
                    broadcastManager.sendBroadcast(localIntent)
                }
            })
            builder.show(mActivity.supportFragmentManager, COLOR_PICKER_DIALOG)
        }
        val textTime = buttonChooseDialogTime
        colorTime = mActivity.prefs.getInt(TIME_COLOR, Color.parseColor("#999999"))
        textTime.setBackgroundColor(colorTime)
        textTime.setOnClickListener {
            val builder = ColorPickerDialog.newBuilder().setColor(colorTime).create()
            builder.setColorPickerDialogListener(object : ColorPickerDialogListener {
                override fun onDialogDismissed(dialogId: Int) {}

                override fun onColorSelected(dialogId: Int, color: Int) {
                    colorTime = color
                    textTime.setBackgroundColor(color)
                    mActivity.prefs.edit().putInt(TIME_COLOR, colorTime).apply()
                    broadcastManager.sendBroadcast(localIntent)
                }
            })
            builder.show(mActivity.supportFragmentManager, COLOR_PICKER_DIALOG)
        }
        val textLessons = buttonChooseDialogLessons
        colorLessons = mActivity.prefs.getInt(LESSONS_COLOR, Color.parseColor("#999999"))
        textLessons.setBackgroundColor(colorLessons)
        textLessons.setOnClickListener {
            val builder = ColorPickerDialog.newBuilder().setColor(colorLessons).create()
            builder.setColorPickerDialogListener(object : ColorPickerDialogListener {
                override fun onDialogDismissed(dialogId: Int) {}

                override fun onColorSelected(dialogId: Int, color: Int) {
                    colorLessons = color
                    textLessons.setBackgroundColor(color)
                    mActivity.prefs.edit().putInt(LESSONS_COLOR, colorLessons).apply()
                    broadcastManager.sendBroadcast(localIntent)
                }
            })
            builder.show(mActivity.supportFragmentManager, COLOR_PICKER_DIALOG)
        }
        val background = buttonChooseDialogBackground
        colorBackground = mActivity.prefs.getInt(BACKGROUND_COLOR, Color.parseColor("#ffffff"))
        background.setBackgroundColor(colorBackground)
        background.setOnClickListener {
            val builder = ColorPickerDialog.newBuilder().setColor(colorBackground).create()
            builder.setColorPickerDialogListener(object : ColorPickerDialogListener {
                override fun onDialogDismissed(dialogId: Int) {}

                override fun onColorSelected(dialogId: Int, color: Int) {
                    colorBackground = color
                    background.setBackgroundColor(color)
                    mActivity.prefs.edit().putInt(BACKGROUND_COLOR, colorBackground).apply()
                    broadcastManager.sendBroadcast(localIntent)
                }
            })
            builder.show(mActivity.supportFragmentManager, COLOR_PICKER_DIALOG)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == SEARCH_REQUEST_CODE && resultCode == Activity.RESULT_OK && data != null) {
            val extras = data.extras!!
            mActivity.prefs.edit {
                putString(CLASS, extras[CLASS] as String)
                putString(SCHOOL_ID, extras[SCHOOL_ID] as String)
            }
            val text = selectedClass
            text.visibility = View.VISIBLE
            text.text = (extras[CLASS] as String).replace(".", "")
            mActivity.initClass()
        }
    }

}