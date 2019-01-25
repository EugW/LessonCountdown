package pro.eugw.lessoncountdown.list.marks

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.mark_view.view.*

class MarksHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val mark: TextView = itemView.markText
    val date: TextView = itemView.markDate
    val time: TextView = itemView.markTime
    val subject: TextView = itemView.markSubject
}