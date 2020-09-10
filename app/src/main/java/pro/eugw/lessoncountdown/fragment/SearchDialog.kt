package pro.eugw.lessoncountdown.fragment

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import kotlinx.android.synthetic.main.fragment_search.*
import pro.eugw.lessoncountdown.R
import pro.eugw.lessoncountdown.activity.MainActivity
import pro.eugw.lessoncountdown.list.search.SearchAdapter
import pro.eugw.lessoncountdown.list.search.SearchElement
import pro.eugw.lessoncountdown.util.SCHEDULE_FILE
import pro.eugw.lessoncountdown.util.network.JsObRe
import java.io.File
import java.util.*
import kotlin.collections.ArrayList
import kotlin.concurrent.thread

class SearchDialog : DialogFragment() {

    private lateinit var adapter: SearchAdapter
    private var searchResults = ArrayList<SearchElement>()
    private var baseArray = ArrayList<SearchElement>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        return inflater.inflate(R.layout.fragment_search, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        val mActivity = activity as MainActivity
        adapter = SearchAdapter(searchResults, this)
        searchRecycler.layoutManager = LinearLayoutManager(mActivity)
        searchRecycler.addItemDecoration(DividerItemDecoration(mActivity, LinearLayoutManager(mActivity).orientation))
        searchRecycler.adapter = adapter
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                thread(true) {
                    val tamar = ArrayList<SearchElement>()
                    baseArray.forEach {
                        val mmm = it
                        var match = true
                        s.toString().toUpperCase(Locale.getDefault()).split(" ").forEach { string ->
                            if (!(mmm.groupName).toUpperCase(Locale.getDefault()).contains(string)) {
                                match = false
                            }
                        }
                        if (match)
                            tamar.add(it)
                    }
                    searchResults.clear()
                    searchResults.addAll(tamar)
                    mActivity.runOnUiThread {
                        adapter.notifyDataSetChanged()
                    }
                }
            }
        })
        mActivity.queue.add(JsObRe(Request.Method.GET,
                "https://raw.githubusercontent.com/EugW/PGUPS-rasp-storage/master/listing.json", {
                        it["list"].asJsonArray.forEach { groupElement ->
                            baseArray.add(SearchElement(it[groupElement.asString].asJsonObject["name"].asString, it[groupElement.asString].asJsonObject["path"].asString))
                        }
                        searchResults.clear()
                        searchResults.addAll(baseArray)
                        mActivity.runOnUiThread {
                            adapter.notifyDataSetChanged()
                            progressBarSearch.visibility = View.GONE
                            searchRecycler.visibility = View.VISIBLE
                        }
                },
                {}
        ))
    }

    fun choose(groupPath: String) {
        val mActivity = activity as MainActivity
        mActivity.queue.add(StringRequest("https://raw.githubusercontent.com/EugW/PGUPS-rasp-storage/master/$groupPath", {
            File(mActivity.filesDir, SCHEDULE_FILE).writeText(it)
            mActivity.updateSchedule()
        }, {}))
        dismissAllowingStateLoss()
    }

}