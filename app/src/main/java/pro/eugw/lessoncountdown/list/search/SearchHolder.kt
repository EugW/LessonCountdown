package pro.eugw.lessoncountdown.list.search

import android.view.View
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.search_view.view.*

class SearchHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val clazz: TextView = itemView.searchText
    val info: TextView = itemView.searchMoreText
    val item: ConstraintLayout = itemView.searchItem
}