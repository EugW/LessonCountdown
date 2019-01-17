package pro.eugw.lessoncountdown.fragment.small

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.volley.Response
import com.android.volley.toolbox.JsonArrayRequest
import com.android.volley.toolbox.JsonObjectRequest
import kotlinx.android.synthetic.main.fragment_lcapi_marks_log.*
import pro.eugw.lessoncountdown.R
import pro.eugw.lessoncountdown.activity.MainActivity
import pro.eugw.lessoncountdown.list.marks.MarksAdapter
import pro.eugw.lessoncountdown.list.marks.MarksElement
import pro.eugw.lessoncountdown.util.KUNDELIK_TOKEN
import java.text.SimpleDateFormat
import java.util.*

class LCAPIMarksLogFragment : DialogFragment() {

    private lateinit var adapter: MarksAdapter
    private lateinit var personId: String
    private lateinit var schoolId: String
    private var marks = ArrayList<MarksElement>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_lcapi_marks_log, container, false)
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        adapter = MarksAdapter(marks)
        val mActivity = activity as MainActivity
        recyclerViewMarksLog.layoutManager = LinearLayoutManager(context)
        recyclerViewMarksLog.addItemDecoration(DividerItemDecoration(recyclerViewMarksLog.context, LinearLayoutManager(context).orientation))
        recyclerViewMarksLog.adapter = adapter
        if (!mActivity.prefs.contains(KUNDELIK_TOKEN)) {
            dismiss()
            return
        }
        val token = mActivity.prefs.getString(KUNDELIK_TOKEN, "")
        mActivity.queue.add(JsonObjectRequest("https://api.kundelik.kz/v1/users/me?access_token=$token", null,
                Response.Listener { response ->
                personId = response.getString("personId")
                mActivity.queue.add(JsonArrayRequest("https://api.kundelik.kz/v1/users/me/schools?access_token=$token",
                        Response.Listener { response1 ->
                            schoolId = response1.getString(0)
                            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                                val calendar = Calendar.getInstance()
                                val to = sdf.format(calendar.time)
                                calendar.add(Calendar.MONTH, -1)
                                val from = sdf.format(calendar.time)
                                mActivity.queue.add(JsonArrayRequest("https://api.kundelik.kz/v1/persons/$personId/schools/$schoolId/marks/$from/$to?access_token=$token",
                                        Response.Listener {


                                        },
                                        Response.ErrorListener {
                                        }
                            ))
                        },
                        Response.ErrorListener {  }
                ))
                },
                Response.ErrorListener {}
        ))
    }

    

}