package pro.eugw.lessoncountdown.activity

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText
import android.widget.Toast
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import pro.eugw.lessoncountdown.R
import pro.eugw.lessoncountdown.list.schedule.MAdapter
import pro.eugw.lessoncountdown.list.schedule.MLesson
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.util.*

class DayOfWeekActivity : AppCompatActivity() {

    val list = ArrayList<MLesson>()
    private val adapter = MAdapter(list, this, false)
    private val adapterEdit = MAdapter(list, this, true)
    private var edit = false
    private var schedule = JsonObject()
    private var bells = JsonObject()
    private var day = "0"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.day_of_week)
        setResult(Activity.RESULT_OK)
        val bundle = intent.extras ?: return
        title = bundle.getString("dayName")
        val dialogView = findViewById<RecyclerView>(R.id.dialogRecycler)
        dialogView.adapter = adapter
        dialogView.layoutManager = LinearLayoutManager(this)
        dialogView.addItemDecoration(DividerItemDecoration(dialogView.context, LinearLayoutManager(this).orientation))
        schedule = JsonParser().parse(bundle.getString("schedule", JsonObject().toString())).asJsonObject
        bells = JsonParser().parse(bundle.getString("bells", JsonObject().toString())).asJsonObject
        day = bundle.getString("day")
        val homework = JsonParser().parse(bundle.getString("homework", JsonObject().toString())).asJsonObject
        if (!schedule.has(day) || !bells.has(day))
            return
        (0 until schedule.get(bundle.getString("day")).asJsonArray.size()).map {
            val s = schedule.get(bundle.getString("day")).asJsonArray[it].asString
            val homeworkS = if (homework.has(s)) homework.get(s).asString else ""
            list.add(MLesson(s, bells.get(bundle.getString("day")).asJsonArray[it].asString, homeworkS))
        }
        if (edit) adapterEdit.notifyDataSetChanged() else adapter.notifyDataSetChanged()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        if (getSharedPreferences("newPrefs", Context.MODE_PRIVATE).getBoolean("CustomCfg", false))
            inflater.inflate(R.menu.dayofweek_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menuEdit -> {
                if (edit) {
                    if (!schedule.has(day) || !bells.has(day))
                        return true
                    while (schedule[day].asJsonArray.size() > 0)
                        schedule[day].asJsonArray.remove(0)
                    while (bells[day].asJsonArray.size() > 0)
                        bells[day].asJsonArray.remove(0)
                    list.map {
                        if (it.lesson.isBlank()) {
                            Toast.makeText(this, getString(R.string.lessonErr).replace("xxx", (list.indexOf(it) + 1).toString()), Toast.LENGTH_LONG).show()
                            return true
                        }
                        if (!it.time.matches("[0-9]{2}:[0-9]{2}-[0-9]{2}:[0-9]{2}".toRegex())) {
                            Toast.makeText(this, getString(R.string.timeErr).replace("xxx", (list.indexOf(it) + 1).toString()), Toast.LENGTH_LONG).show()
                            return true
                        }
                        schedule[day].asJsonArray.add(it.lesson)
                        bells[day].asJsonArray.add(it.time)
                    }
                    PrintWriter(FileWriter(File(filesDir, "schedule.json")), true).println(schedule)
                    PrintWriter(FileWriter(File(filesDir, "bells.json")), true).println(bells)
                }
                edit = !edit
                findViewById<RecyclerView>(R.id.dialogRecycler).adapter = if (edit) adapterEdit else adapter
                if (edit) adapterEdit.notifyDataSetChanged() else adapter.notifyDataSetChanged()
                true
            }
            R.id.menuAdd -> {
                val ab = AlertDialog.Builder(this)
                ab.setTitle(R.string.addIndex)
                val input = EditText(this)
                ab.setView(input)
                ab.setPositiveButton("ok", { _, _ ->
                    val int = try {
                        input.text.toString().toInt()
                    } catch (e: Exception) {
                        Toast.makeText(this, R.string.notNumErr, Toast.LENGTH_LONG).show()
                        return@setPositiveButton
                    }
                    if (int !in 0..list.size) {
                        Toast.makeText(this, R.string.indexErr, Toast.LENGTH_LONG).show()
                        return@setPositiveButton
                    }
                    list.add(int, MLesson("", "", ""))
                    if (edit) adapterEdit.notifyDataSetChanged() else adapter.notifyDataSetChanged()
                })
                ab.show()
                true
            }
            R.id.menuRemove -> {
                val ab = AlertDialog.Builder(this)
                ab.setTitle(R.string.removeIndex)
                val input = EditText(this)
                ab.setView(input)
                ab.setPositiveButton("ok", { _, _ ->
                    val int = try {
                        input.text.toString().toInt()
                    } catch (e: Exception) {
                        Toast.makeText(this, R.string.notNumErr, Toast.LENGTH_LONG).show()
                        return@setPositiveButton
                    }
                    if (int !in 1..list.size) {
                        Toast.makeText(this, R.string.indexErr, Toast.LENGTH_LONG).show()
                        return@setPositiveButton
                    }
                    list.removeAt(int - 1)
                    if (edit) adapterEdit.notifyDataSetChanged() else adapter.notifyDataSetChanged()
                })
                ab.show()
                true
            }
            else -> true
        }
    }

}
