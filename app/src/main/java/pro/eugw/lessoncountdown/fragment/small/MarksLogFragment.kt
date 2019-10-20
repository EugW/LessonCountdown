package pro.eugw.lessoncountdown.fragment.small

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.JsonParser
import kotlinx.android.synthetic.main.fragment_marks_log.*
import pro.eugw.lessoncountdown.R
import pro.eugw.lessoncountdown.activity.MainActivity
import pro.eugw.lessoncountdown.list.marksLog.MarksLogAdapter
import pro.eugw.lessoncountdown.list.marksLog.MarksLogElement
import java.io.File
import java.util.*

class MarksLogFragment(activity: MainActivity) : DialogFragment() {

    private lateinit var adapter: MarksLogAdapter
    private var log = ArrayList<MarksLogElement>()
    private val logFile = File(activity.filesDir, "marksLog.json")


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        println(logFile.absolutePath)
        JsonParser().parse(logFile.readText()).asJsonArray.forEach {
            val obj = it.asJsonObject
            log.add(MarksLogElement(obj["iStart"].asInt, obj["iEnd"].asInt,
                    obj["timeStart"].asLong,
                    obj["timeEnd"].asLong,
                    obj["omBefore"].asInt,
                    obj["omAfter"].asInt,
                    obj["newMarks"].asJsonArray,
                    obj["diffSize"].asInt,
                    obj["notSent"].asInt))
        }
        return inflater.inflate(R.layout.fragment_marks_log, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        adapter = MarksLogAdapter(log, context!!)
        recyclerViewMarksLogAc.layoutManager = LinearLayoutManager(activity)
        recyclerViewMarksLogAc.addItemDecoration(DividerItemDecoration(activity, LinearLayoutManager(activity).orientation))
        recyclerViewMarksLogAc.adapter = adapter
        adapter.notifyDataSetChanged()
    }

}
