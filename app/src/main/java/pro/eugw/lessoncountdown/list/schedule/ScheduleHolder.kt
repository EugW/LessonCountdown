package pro.eugw.lessoncountdown.list.schedule

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.schedule_element.view.*

class ScheduleHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    val lesson: TextView = itemView.textLesson
    val time: TextView = itemView.textTime
    val cabinet: TextView = itemView.textCabinet
    val layout: View = itemView.layoutAll

}
