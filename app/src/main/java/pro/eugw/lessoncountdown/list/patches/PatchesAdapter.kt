package pro.eugw.lessoncountdown.list.patches

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import pro.eugw.lessoncountdown.R
import pro.eugw.lessoncountdown.fragment.PatchesFragment

class PatchesAdapter(var patchesArrayList: ArrayList<PatchesElement>, var fragment: PatchesFragment) : RecyclerView.Adapter<PatchesHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PatchesHolder {
        return PatchesHolder(LayoutInflater.from(parent.context).inflate(R.layout.patches_element, parent, false))
    }

    override fun getItemCount(): Int = patchesArrayList.size

    override fun onBindViewHolder(holder: PatchesHolder, position: Int) {
        holder.name.text = patchesArrayList[position].name
        holder.id.text = patchesArrayList[position].id.toString()
        holder.checkBox.setOnCheckedChangeListener { _, isChecked ->
            //add to active patches list
        }
        holder.element.setOnClickListener {
            //show dialog with more info
            //context.choose(arrayList[position].school_id, arrayList[position].number, arrayList[position].letter, arrayList[position].subgroup)
        }
    }


}