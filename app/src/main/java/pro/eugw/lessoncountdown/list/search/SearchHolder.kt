package pro.eugw.lessoncountdown.list.search

import android.view.View
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import kotlinx.android.synthetic.main.search_view.view.*

internal class SearchHolder(itemView: View) : androidx.recyclerview.widget.RecyclerView.ViewHolder(itemView) {
    val clazz: TextView = itemView.searchText
    val info: TextView = itemView.searchMoreText
    val item: ConstraintLayout = itemView.searchItem
}