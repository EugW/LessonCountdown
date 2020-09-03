package pro.eugw.lessoncountdown.fragment.small

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.volley.Request
import com.google.gson.JsonArray
import kotlinx.android.synthetic.main.fragment_marks_log.*
import pro.eugw.lessoncountdown.R
import pro.eugw.lessoncountdown.activity.MainActivity
import pro.eugw.lessoncountdown.list.marks.MarksAdapter
import pro.eugw.lessoncountdown.list.marks.MarksElement
import pro.eugw.lessoncountdown.util.KUNDELIK_ROLE
import pro.eugw.lessoncountdown.util.KUNDELIK_TOKEN
import pro.eugw.lessoncountdown.util.SUPPORTED_KUNDELIK_ROLES
import pro.eugw.lessoncountdown.util.network.JsArRe
import pro.eugw.lessoncountdown.util.network.JsObRe
import java.text.SimpleDateFormat
import java.util.*
import kotlin.concurrent.thread

class MarksLogFragment : DialogFragment() {

    private lateinit var adapter: MarksAdapter
    private lateinit var personId: String
    private lateinit var schoolId: String
    private var marks = ArrayList<MarksElement>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        return inflater.inflate(R.layout.fragment_marks_log, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        adapter = MarksAdapter(marks)
        val mActivity = activity as MainActivity
        recyclerViewMarksLog.layoutManager = LinearLayoutManager(mActivity)
        recyclerViewMarksLog.addItemDecoration(DividerItemDecoration(mActivity, LinearLayoutManager(mActivity).orientation))
        recyclerViewMarksLog.adapter = adapter
        if (!mActivity.prefs.contains(KUNDELIK_TOKEN) || !SUPPORTED_KUNDELIK_ROLES.contains(mActivity.prefs.getString(KUNDELIK_ROLE, ""))) {
            dismiss()
            return
        }
        val token = mActivity.prefs.getString(KUNDELIK_TOKEN, "")
        mActivity.queue.add(JsObRe(Request.Method.GET, "https://api.kundelik.kz/v1/users/me?access_token=$token",
                { response ->
                    personId = response["personId"].asString
                    mActivity.queue.add(JsArRe(Request.Method.GET, "https://api.kundelik.kz/v1/users/me/schools?access_token=$token",
                            { response1 ->
                                schoolId = response1[0].asString
                                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                                val calendar = Calendar.getInstance()
                                val to = sdf.format(calendar.time)
                                calendar.add(Calendar.MONTH, -1)
                                val from = sdf.format(calendar.time)
                                mActivity.queue.add(JsArRe(Request.Method.GET, "https://api.kundelik.kz/v1/persons/$personId/schools/$schoolId/marks/$from/$to?access_token=$token",
                                        { response2 ->
                                            val jArr = JsonArray()
                                            response2.forEach {
                                                jArr.add(it.asJsonObject["lesson"].asString)
                                            }
                                            mActivity.queue.add(JsArRe(Request.Method.POST, "https://api.kundelik.kz/v1/lessons/many?access_token=$token", jArr,
                                                    { response3 ->
                                                        thread(true) {
                                                            val sdf2 = SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.getDefault())
                                                            response2.sortedBy { sdf2.parse(it.asJsonObject["date"].asString)!!.time }.forEach {
                                                                var name = "null"
                                                                response3.forEach { el ->
                                                                    if (el.asJsonObject["id"].asString == it.asJsonObject["lesson"].asString)
                                                                        name = el.asJsonObject["subject"].asJsonObject["name"].asString
                                                                }
                                                                marks.add(0, MarksElement(it.asJsonObject["value"].asString, it.asJsonObject["date"].asString, name))
                                                            }
                                                            mActivity.runOnUiThread {
                                                                adapter.notifyDataSetChanged()
                                                                marksProgressBar.visibility = View.GONE
                                                                recyclerViewMarksLog.visibility = View.VISIBLE
                                                            }
                                                        }
                                                    },
                                                    {
                                                    }
                                            ))
                                        },
                                        {
                                        }
                                ))
                            },
                            {
                            }
                    ))
                },
                {
                }
        ))
    }

}
