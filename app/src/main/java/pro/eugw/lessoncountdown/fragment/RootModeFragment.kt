package pro.eugw.lessoncountdown.fragment

import android.app.AlertDialog
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import eu.chainfire.libsuperuser.Shell
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_root_mode.*
import pro.eugw.lessoncountdown.R
import pro.eugw.lessoncountdown.activity.MainActivity
import pro.eugw.lessoncountdown.util.SU
import java.io.File
import java.io.FileOutputStream

class RootModeFragment : Fragment() {

    private lateinit var mActivity: MainActivity

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_root_mode, container, false)
    }

    override fun onStart() {
        super.onStart()
        mActivity = activity as MainActivity
        mActivity.main_toolbar.title = getString(R.string.rootMode)
        mActivity.main_toolbar.menu.clear()
        initRootMode()
        initDozeAddWhitelist()
        initDozeRemoveWhitelist()
    }

    private fun initRootMode() {
        switchRootMode.isChecked = (activity as MainActivity).prefs.getBoolean(SU, false)
        switchRootMode.setOnCheckedChangeListener { _, _ ->
            val su = Shell.SU.available()
            switchRootMode.isChecked = su
            if (su)
                rootModeLayout.visibility = View.VISIBLE
            else
                rootModeLayout.visibility = View.GONE
            mActivity.prefs.edit().putBoolean(SU, su).apply()
        }
        if (switchRootMode.isChecked)
            rootModeLayout.visibility = View.VISIBLE
        else
            rootModeLayout.visibility = View.GONE
    }

    private fun initDozeAddWhitelist() {
        buttonDozeAdd.setOnClickListener {
            addAppToDozeWhitelist()
        }
    }

    private fun initDozeRemoveWhitelist() {
        buttonDozeRemove.setOnClickListener {
            removeAppFromDozeWhitelist()
        }
    }

    private fun addAppToDozeWhitelist() {
        val b = AlertDialog.Builder(mActivity)
        b.setTitle(R.string.suWarn)
        b.setMessage(R.string.suWarnDozeAdd)
        b.setPositiveButton(android.R.string.ok) { _, _ ->
            val tmp = File(mActivity.filesDir, "srv.xml.tmp")
            resources.openRawResource(R.raw.service).copyTo(FileOutputStream(tmp))
            val str = "mount -o rw,remount /system\n" +
                    "mv ${tmp.absolutePath} /system/etc/sysconfig/service_lessoncountdown.xml\n" +
                    "chmod 644 /system/etc/sysconfig/service_lessoncountdown.xml\n" +
                    "reboot\n"
            Shell.SU.run(str)
        }
        b.show()
    }

    private fun removeAppFromDozeWhitelist() {
        val b = AlertDialog.Builder(mActivity)
        b.setTitle(R.string.suWarn)
        b.setMessage(R.string.suWarnDozeRemove)
        b.setPositiveButton(android.R.string.ok) { _, _ ->
            val str = "mount -o rw,remount /system\n" +
                    "rm -rf /system/etc/sysconfig/service_lessoncountdown.xml\n" +
                    "reboot\n"
            Shell.SU.run(str)
        }
        b.show()
    }

}