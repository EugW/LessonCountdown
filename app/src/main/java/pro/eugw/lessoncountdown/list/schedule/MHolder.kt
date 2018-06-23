package pro.eugw.lessoncountdown.list.schedule

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import pro.eugw.lessoncountdown.R

internal class MHolder(itemView: View) : androidx.recyclerview.widget.RecyclerView.ViewHolder(itemView) {

    val lesson: TextView = itemView.findViewById(R.id.textLesson)
    val time: TextView = itemView.findViewById(R.id.textTime)
    val imageHomework: ImageView = itemView.findViewById(R.id.imageHomework)
    val layout: View = itemView.findViewById(R.id.layoutAll)

}
