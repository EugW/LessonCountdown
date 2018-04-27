package pro.eugw.lessoncountdown.list.search

import android.support.constraint.ConstraintLayout
import android.support.v7.widget.RecyclerView
import android.view.View
import android.widget.TextView
import pro.eugw.lessoncountdown.R

internal class SearchHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val clazz: TextView = itemView.findViewById(R.id.searchText)
    val info: TextView = itemView.findViewById(R.id.searchMoreText)
    val item: ConstraintLayout = itemView.findViewById(R.id.searchItem)
}