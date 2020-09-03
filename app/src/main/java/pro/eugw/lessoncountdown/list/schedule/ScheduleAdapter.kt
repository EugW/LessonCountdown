package pro.eugw.lessoncountdown.list.schedule

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import pro.eugw.lessoncountdown.R
import pro.eugw.lessoncountdown.fragment.DOWFragment

class ScheduleAdapter(private var list: List<ScheduleElement>, private var fragment: DOWFragment, private val edit: Boolean) : RecyclerView.Adapter<ScheduleHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScheduleHolder {
        val view = if (edit) LayoutInflater.from(parent.context).inflate(R.layout.schedule_element_edit, parent, false) else LayoutInflater.from(parent.context).inflate(R.layout.schedule_element, parent, false)
        return ScheduleHolder(view)
    }

    override fun onBindViewHolder(holder: ScheduleHolder, position: Int) {
        val name = list[position].lesson
        holder.lesson.text = if (edit) name else ((position + 1).toString() + ". " + name)
        holder.time.text = list[position].time
        holder.cabinet.text = list[position].cabinet
        if (list[position].cabinet.isBlank())
            holder.cabinet.visibility = View.GONE
        if (edit) {
            holder.lesson.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    fragment.list[holder.adapterPosition] = ScheduleElement(s.toString(), list[holder.adapterPosition].time, list[holder.adapterPosition].cabinet)
                }
            })
            holder.time.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    fragment.list[holder.adapterPosition] = ScheduleElement(list[holder.adapterPosition].lesson, s.toString(), list[holder.adapterPosition].cabinet)
                }
            })
            holder.cabinet.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    fragment.list[holder.adapterPosition] = ScheduleElement(list[holder.adapterPosition].lesson, list[holder.adapterPosition].time, s.toString())
                }
            })
        }
    }

    override fun getItemCount(): Int {
        return list.size
    }

}
