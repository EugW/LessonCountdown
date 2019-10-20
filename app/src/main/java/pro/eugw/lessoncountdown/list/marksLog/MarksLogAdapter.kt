package pro.eugw.lessoncountdown.list.marksLog

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import pro.eugw.lessoncountdown.R
import java.text.SimpleDateFormat
import java.util.*

class MarksLogAdapter(private var arrayList: ArrayList<MarksLogElement>, private var context: Context): RecyclerView.Adapter<MarksLogHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MarksLogHolder {
        return MarksLogHolder(LayoutInflater.from(parent.context).inflate(R.layout.log_element, parent, false))
    }

    override fun getItemCount(): Int = arrayList.size

    override fun onBindViewHolder(holder: MarksLogHolder, position: Int) {
        val sdf = SimpleDateFormat("yy.MM.dd HH:mm:ss", Locale.getDefault())
        holder.textViewIStart.text = "IStart" + arrayList[position].iStart.toString()
        holder.textViewIEnd.text = "IEnd" + arrayList[position].iEnd.toString()
        holder.textViewTimeStart.text = "TimeStart" + sdf.format(arrayList[position].timeStart)
        holder.textViewTimeEnd.text = "TimeEnd" + sdf.format(arrayList[position].timeEnd)
        holder.textViewOmBefore.text = "OmBefore" + arrayList[position].omBefore.toString()
        holder.textViewOmAfter.text = "OmAfter" + arrayList[position].omAfter.toString()
        holder.textViewDiffSize.text = "DiffSize" + arrayList[position].diffSize.toString()
        holder.textViewNotSent.text = "NotSent" + arrayList[position].notSent.toString()
        holder.buttongetNewMarks.setOnClickListener {
            val clip = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clip.setPrimaryClip(ClipData.newPlainText("lcError", arrayList[position].newMarks.toString()))
        }
    }

}