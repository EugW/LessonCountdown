package pro.eugw.lessoncountdown.list.schedule

import android.app.AlertDialog
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.JsonParser
import pro.eugw.lessoncountdown.R
import pro.eugw.lessoncountdown.fragment.DayOfWeekFragment
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.io.PrintWriter


class ScheduleAdapter(private var list: List<ScheduleElement>, private var fragment: DayOfWeekFragment, private val edit: Boolean) : RecyclerView.Adapter<ScheduleHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScheduleHolder {
        val view = if (edit) LayoutInflater.from(parent.context).inflate(R.layout.schedule_element_edit, parent, false) else LayoutInflater.from(parent.context).inflate(R.layout.schedule_element, parent, false)
        return ScheduleHolder(view)
    }

    override fun onBindViewHolder(holder: ScheduleHolder, position: Int) {
        val name = list[position].lesson
        val homework = list[position].homework
        holder.imageHomework.visibility = if (!homework.isEmpty()) View.VISIBLE else View.GONE
        holder.lesson.text = if (edit) name else ((position + 1).toString() + ". " + name)
        holder.time.text = list[position].time
        if (edit) {
            holder.lesson.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    fragment.list[holder.adapterPosition] = ScheduleElement(s.toString(), list[holder.adapterPosition].time, homework)
                }
            })
            holder.time.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    fragment.list[holder.adapterPosition] = ScheduleElement(list[holder.adapterPosition].lesson, s.toString(), homework)
                }
            })
        } else
            holder.layout.setOnClickListener {
                val builder = AlertDialog.Builder(fragment.activity)
                builder.setTitle(fragment.getString(R.string.homework) + " - " + name)
                val input = EditText(fragment.activity)
                if (!homework.isEmpty())
                    input.setText(homework)
                builder.setView(input)
                builder.setPositiveButton(android.R.string.ok) { _, _ -> setList(name, input.text.toString(), position) }
                builder.setNegativeButton(fragment.getString(R.string.cancel)) { dialogInterface, _ -> dialogInterface.cancel() }
                builder.setNeutralButton(fragment.getString(R.string.clear)) { _, _ -> setList(name, "", position) }
                builder.show()
            }
    }

    private fun setList(prop: String, homework: String, position: Int) {
        val file = File(fragment.activity!!.filesDir, "homework.json")
        val jsonObject = JsonParser().parse(FileReader(file)).asJsonObject
        list[position].homework = homework
        jsonObject.addProperty(prop, homework)
        PrintWriter(FileWriter(file), true).println(jsonObject)
        notifyDataSetChanged()
    }


    override fun getItemCount(): Int {
        return list.size
    }

}
