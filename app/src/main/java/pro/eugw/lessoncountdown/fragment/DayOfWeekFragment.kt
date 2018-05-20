package pro.eugw.lessoncountdown.fragment

import android.app.AlertDialog
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_day_of_week.view.*
import pro.eugw.lessoncountdown.R
import pro.eugw.lessoncountdown.activity.MainActivity
import pro.eugw.lessoncountdown.list.schedule.MAdapter
import pro.eugw.lessoncountdown.list.schedule.MLesson
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.util.*

class DayOfWeekFragment : Fragment() {

    val list = ArrayList<MLesson>()
    private lateinit var adapter: MAdapter
    private lateinit var adapterEdit: MAdapter
    private var edit = false
    private var schedule = JsonObject()
    private var bells = JsonObject()
    private var day = "0"
    private lateinit var dialogView: RecyclerView
    private lateinit var mActivity: MainActivity

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val v = inflater.inflate(R.layout.fragment_day_of_week, container, false)
        dialogView = v.dialogRecycler
        return v
    }

    override fun onStart() {
        super.onStart()
        mActivity = activity as MainActivity
        val bundle = arguments
        val toolbar = activity!!.main_toolbar
        toolbar.title = bundle!!.getString("dayName")
        if (mActivity.prefs.getBoolean("CustomCfg", false))
            if (toolbar.menu.size() <= 0)
                toolbar.inflateMenu(R.menu.dayofweek_menu)
        adapter = MAdapter(list, this, false)
        adapterEdit = MAdapter(list, this, true)
        dialogView.adapter = adapter
        dialogView.layoutManager = LinearLayoutManager(activity)
        dialogView.addItemDecoration(DividerItemDecoration(dialogView.context, LinearLayoutManager(activity).orientation))
        val job = mActivity.clazz
        if (!job.has("schedule") || !job.has("bells")) {
            return
        }
        schedule = job["schedule"].asJsonObject
        bells = job["bells"].asJsonObject
        day = bundle.getString("day")
        val homework = JsonParser().parse(bundle.getString("homework", JsonObject().toString())).asJsonObject
        if (!schedule.has(day) || !bells.has(day))
            return
        schedule.get(day).asJsonArray.forEachIndexed { index, jsonElement ->
            list.add(MLesson(jsonElement.asString, bells.get(day).asJsonArray[index].asString, if (homework.has(jsonElement.asString)) homework.get(jsonElement.asString).asString else ""))
        }
        adapterEdit.notifyDataSetChanged()
        adapter.notifyDataSetChanged()
        toolbar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.menuEdit -> {
                    if (edit) {
                        if (!schedule.has(day) || !bells.has(day))
                            return@setOnMenuItemClickListener true
                        while (schedule[day].asJsonArray.size() > 0)
                            schedule[day].asJsonArray.remove(0)
                        while (bells[day].asJsonArray.size() > 0)
                            bells[day].asJsonArray.remove(0)
                        list.map {
                            if (it.lesson.isBlank()) {
                                Toast.makeText(activity, getString(R.string.lessonErr).replace("xxx", (list.indexOf(it) + 1).toString()), Toast.LENGTH_LONG).show()
                                return@setOnMenuItemClickListener true
                            }
                            if (!it.time.matches("[0-9]{2}:[0-9]{2}-[0-9]{2}:[0-9]{2}".toRegex())) {
                                Toast.makeText(activity, getString(R.string.timeErr).replace("xxx", (list.indexOf(it) + 1).toString()), Toast.LENGTH_LONG).show()
                                return@setOnMenuItemClickListener true
                            }
                            schedule[day].asJsonArray.add(it.lesson)
                            bells[day].asJsonArray.add(it.time)
                        }
                        PrintWriter(FileWriter(File(mActivity.filesDir, "schedule.json")), true).println(schedule)
                        PrintWriter(FileWriter(File(mActivity.filesDir, "bells.json")), true).println(bells)
                    }
                    edit = !edit
                    dialogView.adapter = if (edit) adapterEdit else adapter
                    if (edit) adapterEdit.notifyDataSetChanged() else adapter.notifyDataSetChanged()
                }
                R.id.menuAdd -> {
                    val ab = AlertDialog.Builder(activity)
                    ab.setTitle(R.string.addIndex)
                    val input = EditText(activity)
                    ab.setView(input)
                    ab.setPositiveButton("ok", { _, _ ->
                        val int = try {
                            input.text.toString().toInt()
                        } catch (e: Exception) {
                            Toast.makeText(activity, R.string.notNumErr, Toast.LENGTH_LONG).show()
                            return@setPositiveButton
                        }
                        if (int !in 0..list.size) {
                            Toast.makeText(activity, R.string.indexErr, Toast.LENGTH_LONG).show()
                            return@setPositiveButton
                        }
                        list.add(int, MLesson("", "", ""))
                        if (edit) adapterEdit.notifyDataSetChanged() else adapter.notifyDataSetChanged()
                    })
                    ab.show()
                }
                R.id.menuRemove -> {
                    val ab = AlertDialog.Builder(activity)
                    ab.setTitle(R.string.removeIndex)
                    val input = EditText(activity)
                    ab.setView(input)
                    ab.setPositiveButton("ok", { _, _ ->
                        val int = try {
                            input.text.toString().toInt()
                        } catch (e: Exception) {
                            Toast.makeText(activity, R.string.notNumErr, Toast.LENGTH_LONG).show()
                            return@setPositiveButton
                        }
                        if (int !in 1..list.size) {
                            Toast.makeText(activity, R.string.indexErr, Toast.LENGTH_LONG).show()
                            return@setPositiveButton
                        }
                        list.removeAt(int - 1)
                        if (edit) adapterEdit.notifyDataSetChanged() else adapter.notifyDataSetChanged()
                    })
                    ab.show()
                }
            }
            true
        }
    }

    override fun onStop() {
        super.onStop()
        list.clear()
    }

}