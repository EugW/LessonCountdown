package pro.eugw.lessoncountdown.list.search

import android.support.constraint.ConstraintLayout
import android.support.v7.widget.RecyclerView
import android.view.View
import android.widget.TextView
import kotlinx.android.synthetic.main.search_view.view.*

internal class SearchHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val clazz: TextView = itemView.searchText
    val info: TextView = itemView.searchMoreText
    val item: ConstraintLayout = itemView.searchItem
}