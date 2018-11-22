package pro.eugw.lessoncountdown.fragment

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.google.gson.JsonParser
import pro.eugw.lessoncountdown.R
import pro.eugw.lessoncountdown.activity.MainActivity
import pro.eugw.lessoncountdown.list.search.SearchAdapter
import pro.eugw.lessoncountdown.list.search.SearchElement
import pro.eugw.lessoncountdown.util.*
import java.util.*
import kotlin.collections.ArrayList

class SearchDialog : DialogFragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: SearchAdapter
    private var arrayList = ArrayList<SearchElement>()
    private var baseArray = ArrayList<SearchElement>()
    private lateinit var host: String

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val v = inflater.inflate(R.layout.fragment_search, container, false)
        v.findViewById<EditText>(R.id.searchEditText).addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val tamar = ArrayList<SearchElement>()
                baseArray.forEach {
                    val mmm = it
                    var match = true
                    s.toString().toUpperCase().split(" ").forEach { string ->
                        if (!(mmm.number + mmm.letter + mmm.subgroup + mmm.school_name).replace(" ", "").toUpperCase().contains(string)) {
                            match = false
                        }
                    }
                    if (match)
                        tamar.add(it)
                }
                arrayList.clear()
                arrayList.addAll(tamar)
                if (isVisible)
                    adapter.notifyDataSetChanged()
            }
        })
        recyclerView = v.findViewById(R.id.searchRecycler)
        return v
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        val mActivity = activity as MainActivity
        host = mActivity.prefs.getString(CUSTOM_ADDRESS, getString(R.string.host)) as String
        if (host.isBlank())
            host = getString(R.string.host)
        adapter = SearchAdapter(arrayList, this)
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.addItemDecoration(androidx.recyclerview.widget.DividerItemDecoration(recyclerView.context, LinearLayoutManager(context).orientation))
        recyclerView.adapter = adapter
        mActivity.queue.add(JsonObjectRequest("https://$host/classes?lang=${Locale.getDefault().language}", null,
                Response.Listener {
                    try {
                        JsonParser().parse(it.toString()).asJsonObject[CLASSES].asJsonArray.forEach {jE ->
                            baseArray.add(SearchElement(jE.asJsonObject[NUMBER].asString, jE.asJsonObject[LETTER].asString, jE.asJsonObject[SUBGROUP].asString, jE.asJsonObject[SCHOOL_ID].asString, jE.asJsonObject[SCHOOL_NAME].asString))
                        }
                        arrayList.clear()
                        arrayList.addAll(baseArray)
                        if (isVisible)
                            adapter.notifyDataSetChanged()
                    } catch (e: Exception) {
                        e.printStackTrace()
                        if (isVisible)
                            Toast.makeText(context, R.string.networkErr, Toast.LENGTH_SHORT).show()
                        dismissAllowingStateLoss()
                    }
                },
                Response.ErrorListener {

                }
        ))
    }

    fun choose(id: String, number: String, letter: String, subgroup: String) {
        targetFragment!!.onActivityResult(SEARCH_REQUEST_CODE, Activity.RESULT_OK, Intent().putExtra(CLASS, "$number.$letter.$subgroup").putExtra(SCHOOL_ID, id))
        dismissAllowingStateLoss()
    }

}