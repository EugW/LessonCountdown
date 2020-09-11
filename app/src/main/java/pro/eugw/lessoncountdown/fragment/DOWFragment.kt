package pro.eugw.lessoncountdown.fragment

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_day_of_week.*
import kotlinx.android.synthetic.main.fragment_day_of_week.view.*
import pro.eugw.lessoncountdown.R
import pro.eugw.lessoncountdown.activity.MainActivity
import pro.eugw.lessoncountdown.list.schedule.ScheduleAdapter
import pro.eugw.lessoncountdown.list.schedule.ScheduleElement
import pro.eugw.lessoncountdown.util.HIDE_CONTROLS
import pro.eugw.lessoncountdown.util.SCHEDULE
import pro.eugw.lessoncountdown.util.SCHEDULE_FILE
import pro.eugw.lessoncountdown.util.isEvenWeek
import java.io.File
import java.time.LocalDate
import java.util.*
import kotlin.concurrent.thread

class DOWFragment : Fragment() {

    val list = ArrayList<ScheduleElement>()
    private lateinit var adapter: ScheduleAdapter
    private lateinit var adapterEdit: ScheduleAdapter
    private var edit = false
    private var schedule = JsonObject()
    private var day = "0"

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        val v = inflater.inflate(R.layout.fragment_day_of_week, container, false)
        adapter = ScheduleAdapter(list, this, false)
        adapterEdit = ScheduleAdapter(list, this, true)
        v.dialogRecycler.adapter = adapter
        v.dialogRecycler.layoutManager = LinearLayoutManager(activity)
        v.dialogRecycler.addItemDecoration(DividerItemDecoration(activity, LinearLayoutManager(activity).orientation))
        return v
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val mActivity = activity as MainActivity
        val bundle = arguments
        val toolbar = mActivity.main_toolbar
        thread(true) {
            if (!mActivity.prefs.getBoolean(HIDE_CONTROLS, false)) {
                mActivity.runOnUiThread {
                    toolbar.menu.clear()
                    toolbar.inflateMenu(R.menu.dayofweek_menu)
                }
            }
            val job = mActivity.schedule
            if (!job.has(SCHEDULE))
                job.add(SCHEDULE, JsonObject())
            schedule = job[SCHEDULE].asJsonObject
            day = bundle?.getString("day") as String
            val even = isEvenWeek(LocalDate.now())
            if (even) {
                day += "e"
                mActivity.runOnUiThread {
                    toolbar.menu.findItem(R.id.menuEvenOdd).title = "E"
                }
            } else
                mActivity.runOnUiThread {
                    toolbar.menu.findItem(R.id.menuEvenOdd).title = "O"
                }
            if (!schedule.has(day))
                schedule.add(day, JsonArray())
            schedule.get(day).asJsonArray.forEach { jsonElement ->
                list.add(ScheduleElement(jsonElement.asJsonObject["lesson"].asString, jsonElement.asJsonObject["time"].asString, jsonElement.asJsonObject["cabinet"].asString))
            }
            mActivity.runOnUiThread {
                adapterEdit.notifyDataSetChanged()
                adapter.notifyDataSetChanged()
            }
        }
        toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.menuEvenOdd -> {
                    when (toolbar.menu.findItem(R.id.menuEvenOdd).title) {
                        "E" -> {
                            day = day[0].toString()
                            list.clear()
                            if (!schedule.has(day))
                                schedule.add(day, JsonArray())
                            schedule.get(day).asJsonArray.forEach { jsonElement ->
                                list.add(ScheduleElement(jsonElement.asJsonObject["lesson"].asString, jsonElement.asJsonObject["time"].asString, jsonElement.asJsonObject["cabinet"].asString))
                            }
                            toolbar.menu.findItem(R.id.menuEvenOdd).title = "O"
                        }
                        "O" -> {
                            day = if (day.length > 1) day else day + "e"
                            list.clear()
                            schedule.get(day).asJsonArray.forEach { jsonElement ->
                                list.add(ScheduleElement(jsonElement.asJsonObject["lesson"].asString, jsonElement.asJsonObject["time"].asString, jsonElement.asJsonObject["cabinet"].asString))
                            }
                            toolbar.menu.findItem(R.id.menuEvenOdd).title = "E"
                        }
                    }
                    if (edit) adapterEdit.notifyDataSetChanged() else adapter.notifyDataSetChanged()
                }
                R.id.menuEdit -> {
                    if (edit) {
                        while (schedule[day].asJsonArray.size() > 0)
                            schedule[day].asJsonArray.remove(0)
                        list.map {
                            if (it.lesson.isBlank()) {
                                Toast.makeText(activity, getString(R.string.lessonErr).replace("xxx", (list.indexOf(it) + 1).toString()), Toast.LENGTH_SHORT).show()
                                return@setOnMenuItemClickListener true
                            }
                            if (!it.time.matches("[0-9]{2}:[0-9]{2}-[0-9]{2}:[0-9]{2}".toRegex())) {
                                Toast.makeText(activity, getString(R.string.timeErr).replace("xxx", (list.indexOf(it) + 1).toString()), Toast.LENGTH_SHORT).show()
                                return@setOnMenuItemClickListener true
                            }
                            val lessonObject = JsonObject()
                            lessonObject.addProperty("lesson", it.lesson)
                            lessonObject.addProperty("time", it.time)
                            lessonObject.addProperty("cabinet", it.cabinet)
                            schedule[day].asJsonArray.add(lessonObject)
                        }
                        File(mActivity.filesDir, SCHEDULE_FILE).writeText(schedule.toString())
                    }
                    edit = !edit
                    dialogRecycler.adapter = if (edit) adapterEdit else adapter
                    if (edit) adapterEdit.notifyDataSetChanged() else adapter.notifyDataSetChanged()
                }
                R.id.menuAdd -> {
                    val ab = AlertDialog.Builder(activity)
                    ab.setTitle(R.string.addIndex)
                    val input = EditText(activity)
                    ab.setView(input)
                    ab.setPositiveButton(getString(android.R.string.ok)) { _, _ ->
                        val int = try {
                            input.text.toString().toInt()
                        } catch (e: Exception) {
                            Toast.makeText(activity, R.string.notNumErr, Toast.LENGTH_SHORT).show()
                            return@setPositiveButton
                        }
                        if (int !in 0..list.size) {
                            Toast.makeText(activity, R.string.indexErr, Toast.LENGTH_SHORT).show()
                            return@setPositiveButton
                        }
                        list.add(int, ScheduleElement("", "", ""))
                        if (edit) adapterEdit.notifyDataSetChanged() else adapter.notifyDataSetChanged()
                    }
                    ab.show()
                }
                R.id.menuRemove -> {
                    val ab = AlertDialog.Builder(activity)
                    ab.setTitle(R.string.removeIndex)
                    val input = EditText(activity)
                    ab.setView(input)
                    ab.setPositiveButton(getString(android.R.string.ok)) { _, _ ->
                        val int = try {
                            input.text.toString().toInt()
                        } catch (e: Exception) {
                            Toast.makeText(activity, R.string.notNumErr, Toast.LENGTH_SHORT).show()
                            return@setPositiveButton
                        }
                        if (int !in 1..list.size) {
                            Toast.makeText(activity, R.string.indexErr, Toast.LENGTH_SHORT).show()
                            return@setPositiveButton
                        }
                        list.removeAt(int - 1)
                        if (edit) adapterEdit.notifyDataSetChanged() else adapter.notifyDataSetChanged()
                    }
                    ab.show()
                }
            }
            true
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        list.clear()
    }

}