package pro.eugw.lessoncountdown.list.marksLog

import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.log_element.view.*

class MarksLogHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val textViewIStart: TextView = itemView.textViewIStart
    val textViewIEnd: TextView = itemView.textViewIEnd
    val textViewTimeStart: TextView = itemView.textViewTimeStart
    val textViewTimeEnd: TextView = itemView.textViewTimeEnd
    val textViewOmBefore: TextView = itemView.textViewOmBefore
    val textViewOmAfter: TextView = itemView.textViewOmAfter
    val buttongetNewMarks: Button = itemView.buttongetNewMarks
    val textViewDiffSize: TextView = itemView.textViewDiffSize
    val textViewNotSent: TextView = itemView.textViewNotSent
}