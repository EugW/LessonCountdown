package pro.eugw.lessoncountdown.list.search

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import pro.eugw.lessoncountdown.R
import pro.eugw.lessoncountdown.fragment.SearchDialog

class SearchAdapter(private var arrayList: ArrayList<SearchElement>, private val context: SearchDialog): RecyclerView.Adapter<SearchHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchHolder {
        return SearchHolder(LayoutInflater.from(parent.context).inflate(R.layout.search_view, parent, false))
    }

    override fun getItemCount(): Int = arrayList.size

    override fun onBindViewHolder(holder: SearchHolder, position: Int) {
        val clazzS = arrayList[position].number + arrayList[position].letter + " " + arrayList[position].subgroup
        holder.clazz.text = clazzS
        holder.info.text = arrayList[position].school_name
        holder.item.setOnClickListener {
            context.choose(arrayList[position].school_id, arrayList[position].number, arrayList[position].letter, arrayList[position].subgroup)
        }
    }

}