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
import kotlinx.android.synthetic.main.fragment_day_of_week.view.*
import pro.eugw.lessoncountdown.R
import pro.eugw.lessoncountdown.activity.MainActivity
import pro.eugw.lessoncountdown.list.schedule.ScheduleAdapter
import pro.eugw.lessoncountdown.list.schedule.ScheduleElement
import pro.eugw.lessoncountdown.util.*
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.util.*

class DOWFragment : Fragment() {

    val list = ArrayList<ScheduleElement>()
    private lateinit var adapter: ScheduleAdapter
    private lateinit var adapterEdit: ScheduleAdapter
    private var edit = false
    private var schedule = JsonObject()
    private var bells = JsonObject()
    private var day = "0"
    private lateinit var dialogView: androidx.recyclerview.widget.RecyclerView
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
        if (mActivity.prefs.getBoolean(CUSTOM_CONFIG, false))
            if (!mActivity.prefs.getBoolean(HIDE_CONTROLS, false))
                if (toolbar.menu.size() <= 0)
                    toolbar.inflateMenu(R.menu.dayofweek_menu)
        adapter = ScheduleAdapter(list, this, false)
        adapterEdit = ScheduleAdapter(list, this, true)
        dialogView.adapter = adapter
        dialogView.layoutManager = LinearLayoutManager(activity)
        dialogView.addItemDecoration(DividerItemDecoration(dialogView.context, LinearLayoutManager(activity).orientation))
        val job = mActivity.clazz
        if (!job.has(SCHEDULE))
            job.add(SCHEDULE, JsonObject())
        if (!job.has(BELLS))
            job.add(BELLS, JsonObject())
        schedule = job[SCHEDULE].asJsonObject
        bells = job[BELLS].asJsonObject
        day = bundle.getString("day") as String
        if (mActivity.prefs.getBoolean(EVEN_ODD_WEEKS, false) || schedule.has("${day}e")) {
            val preEven = Calendar.getInstance().get(Calendar.WEEK_OF_YEAR) % 2 == 0
            val even = if (!mActivity.prefs.getBoolean(INVERSE_EVEN_ODD_WEEKS, false)) preEven else !preEven
            if (even) {
                day += "e"
                toolbar.menu.findItem(R.id.menuEvenOdd).title = "E"
            }
            else
                toolbar.menu.findItem(R.id.menuEvenOdd).title = "O"
        }
        val homework = mActivity.homework
        if (!schedule.has(day))
            schedule.add(day, JsonArray())
        if (!bells.has(day))
            bells.add(day, JsonArray())
        schedule.get(day).asJsonArray.forEachIndexed { index, jsonElement ->
            list.add(ScheduleElement(jsonElement.asString, bells.get(day).asJsonArray[index].asString, if (homework.has(jsonElement.asString)) homework.get(jsonElement.asString).asString else ""))
        }
        adapterEdit.notifyDataSetChanged()
        adapter.notifyDataSetChanged()
        toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.menuEvenOdd -> {
                    when (toolbar.menu.findItem(R.id.menuEvenOdd).title) {
                        "E" -> {
                            day = if (day.length > 1) day else day + "e"
                            list.clear()
                            if (!schedule.has(day))
                                schedule.add(day, JsonArray())
                            if (!bells.has(day))
                                bells.add(day, JsonArray())
                            schedule.get(day).asJsonArray.forEachIndexed { index, jsonElement ->
                                list.add(ScheduleElement(jsonElement.asString, bells.get(day).asJsonArray[index].asString, if (homework.has(jsonElement.asString)) homework.get(jsonElement.asString).asString else ""))
                            }
                            toolbar.menu.findItem(R.id.menuEvenOdd).title = "O"
                        }
                        "O" -> {
                            day = day[0].toString()
                            list.clear()
                            println(day)
                            schedule.get(day).asJsonArray.forEachIndexed { index, jsonElement ->
                                list.add(ScheduleElement(jsonElement.asString, bells.get(day).asJsonArray[index].asString, if (homework.has(jsonElement.asString)) homework.get(jsonElement.asString).asString else ""))
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
                        while (bells[day].asJsonArray.size() > 0)
                            bells[day].asJsonArray.remove(0)
                        list.map {
                            if (it.lesson.isBlank()) {
                                Toast.makeText(activity, getString(R.string.lessonErr).replace("xxx", (list.indexOf(it) + 1).toString()), Toast.LENGTH_SHORT).show()
                                return@setOnMenuItemClickListener true
                            }
                            if (!it.time.matches("[0-9]{2}:[0-9]{2}-[0-9]{2}:[0-9]{2}".toRegex())) {
                                Toast.makeText(activity, getString(R.string.timeErr).replace("xxx", (list.indexOf(it) + 1).toString()), Toast.LENGTH_SHORT).show()
                                return@setOnMenuItemClickListener true
                            }
                            schedule[day].asJsonArray.add(it.lesson)
                            bells[day].asJsonArray.add(it.time)
                        }
                        PrintWriter(FileWriter(File(mActivity.filesDir, SCHEDULE_FILE)), true).println(schedule)
                        PrintWriter(FileWriter(File(mActivity.filesDir, BELLS_FILE)), true).println(bells)
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

    override fun onStop() {
        super.onStop()
        list.clear()
    }

}