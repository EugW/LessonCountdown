package pro.eugw.lessoncountdown.fragment

import android.app.Activity
import android.app.AlertDialog
import android.content.*
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import kotlinx.android.synthetic.main.custom_colors_layout.*
import kotlinx.android.synthetic.main.fragment_settings.*
import pro.eugw.lessoncountdown.R
import pro.eugw.lessoncountdown.activity.MainActivity
import pro.eugw.lessoncountdown.util.*
import pro.eugw.lessoncountdown.util.color.ColorPickerDialog
import pro.eugw.lessoncountdown.util.color.ColorPickerDialogListener
import java.io.File
import kotlin.Exception

class SettingsFragment : Fragment() {

    private var colorTitle = Color.parseColor("#000000")
    private var colorTime = Color.parseColor("#999999")
    private var colorLessons = Color.parseColor("#999999")
    private var colorBackground = Color.parseColor("#ffffff")
    private lateinit var mActivity: MainActivity

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        mActivity = activity as MainActivity
        selectClass.setOnClickListener {
            val fragment = SearchDialog()
            fragment.show(mActivity.supportFragmentManager, "search-dialog")
        }
        selectPeriod.setOnClickListener {
            val periodView = EditText(mActivity)
            periodView.setText(mActivity.prefs.getLong(SERVICE_PERIOD, 5000).toString())
            val builder = AlertDialog.Builder(mActivity)
                    .setView(periodView)
                    .setPositiveButton("OK") { _, _ ->
                        try {
                            mActivity.prefs.edit(true) {
                                putLong(SERVICE_PERIOD, periodView.text.toString().toLong())
                            }
                            LocalBroadcastManager.getInstance(mActivity).sendBroadcast(Intent(mActivity.packageName + PERIOD_UPDATE)
                                    .putExtra("period", periodView.text.toString().toLong()))
                        } catch (e: Exception) {
                            shortShow("Something wrong", mActivity)
                        }
                    }.setNegativeButton(R.string.cancel) { _, _ -> }
            builder.create().show()
        }
        initCustomCfg()
        initCustomColors()
    }

    private fun initCustomCfg() {
        switchVisibleEditing.isChecked = mActivity.prefs.getBoolean(HIDE_CONTROLS, false)
        switchVisibleEditing.setOnCheckedChangeListener { _, state -> mActivity.prefs.edit { putBoolean(HIDE_CONTROLS, state) } }
        buttonCopy.setOnClickListener {
            val clip = mActivity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clip.setPrimaryClip(ClipData.newPlainText("lcConfig", File(mActivity.filesDir, SCHEDULE_FILE).readText()))
        }
        buttonPaste.setOnClickListener {
            val clip = mActivity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            File(mActivity.filesDir, SCHEDULE_FILE).writeText(clip.primaryClip?.getItemAt(0)?.text.toString())
        }
    }

    private fun initCustomColors() {
        val localIntent = Intent(mActivity.packageName + NOTIFICATION_COLOR_UPDATE)
        switchNotificationColor.isChecked = mActivity.prefs.getBoolean(CUSTOM_COLOR, false)
        includedColors.visibility = if (mActivity.prefs.getBoolean(CUSTOM_COLOR, false)) View.VISIBLE else View.GONE
        switchNotificationColor.setOnCheckedChangeListener { _, state ->
            if (state) {
                includedColors.visibility = View.VISIBLE
                LocalBroadcastManager.getInstance(requireContext()).sendBroadcast(localIntent.putExtra("cVal", true))
            }
            else {
                includedColors.visibility = View.GONE
                LocalBroadcastManager.getInstance(requireContext()).sendBroadcast(localIntent.putExtra("cVal", false))
            }
            mActivity.prefs.edit(true) {
                putBoolean(CUSTOM_COLOR, state)
            }
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
                    mActivity.prefs.edit(true) {
                        putInt(TITLE_COLOR, colorTitle)
                    }
                    LocalBroadcastManager.getInstance(context!!).sendBroadcast(localIntent)
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
                    mActivity.prefs.edit(true) {
                        putInt(TIME_COLOR, colorTime)
                    }
                    LocalBroadcastManager.getInstance(context!!).sendBroadcast(localIntent)
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
                    mActivity.prefs.edit(true) {
                        putInt(LESSONS_COLOR, colorLessons)
                    }
                    LocalBroadcastManager.getInstance(context!!).sendBroadcast(localIntent)
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
                    mActivity.prefs.edit(true) {
                        putInt(BACKGROUND_COLOR, colorBackground)
                    }
                    LocalBroadcastManager.getInstance(context!!).sendBroadcast(localIntent)
                }
            })
            builder.show(mActivity.supportFragmentManager, COLOR_PICKER_DIALOG)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == SEARCH_REQUEST_CODE && resultCode == Activity.RESULT_OK && data != null) {
            mActivity.updateSchedule()
        }
    }

}