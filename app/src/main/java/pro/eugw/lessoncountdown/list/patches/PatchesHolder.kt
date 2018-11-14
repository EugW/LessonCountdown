package pro.eugw.lessoncountdown.list.patches

import android.view.View
import android.widget.CheckBox
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.patches_element.view.*

class PatchesHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val name: TextView = itemView.textViewPatchName
    val id: TextView = itemView.textViewPatchId
    val checkBox: CheckBox = itemView.checkBoxPatch
    val element: ConstraintLayout = itemView.layoutPatch
}