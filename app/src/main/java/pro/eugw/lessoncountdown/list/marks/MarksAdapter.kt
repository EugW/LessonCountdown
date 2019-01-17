package pro.eugw.lessoncountdown.list.marks

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import pro.eugw.lessoncountdown.R

class MarksAdapter(private var arrayList: ArrayList<MarksElement>): RecyclerView.Adapter<MarksHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MarksHolder {
        return MarksHolder(LayoutInflater.from(parent.context).inflate(R.layout.mark_view, parent, false))
    }

    override fun getItemCount(): Int = arrayList.size

    override fun onBindViewHolder(holder: MarksHolder, position: Int) {
        holder.mark.text = arrayList[position].mark
        holder.date.text = arrayList[position].mark
        holder.subject.text = arrayList[position].subject
    }

}