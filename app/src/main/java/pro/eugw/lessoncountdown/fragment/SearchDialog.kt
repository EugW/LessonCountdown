package pro.eugw.lessoncountdown.fragment

import android.app.Activity
import android.content.Intent
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
import com.google.gson.JsonParser
import kotlinx.android.synthetic.main.fragment_search.*
import pro.eugw.lessoncountdown.R
import pro.eugw.lessoncountdown.activity.MainActivity
import pro.eugw.lessoncountdown.list.search.SearchAdapter
import pro.eugw.lessoncountdown.list.search.SearchElement
import pro.eugw.lessoncountdown.util.*
import pro.eugw.lessoncountdown.util.network.JsObRe
import java.util.*
import kotlin.collections.ArrayList
import kotlin.concurrent.thread

class SearchDialog : DialogFragment() {

    private lateinit var adapter: SearchAdapter
    private var searchResults = ArrayList<SearchElement>()
    private var baseArray = ArrayList<SearchElement>()
    private lateinit var host: String

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        return inflater.inflate(R.layout.fragment_search, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        val mActivity = activity as MainActivity
        host = mActivity.prefs.getString(CUSTOM_ADDRESS, getString(R.string.host)) as String
        if (host.isBlank())
            host = getString(R.string.host)
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
                            if (!(mmm.number + mmm.letter + mmm.subgroup + mmm.school_name).replace(" ", "").toUpperCase(Locale.getDefault()).contains(string)) {
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
        mActivity.queue.add(JsObRe(Request.Method.GET, "https://$host/classes?lang=${Locale.getDefault().language}",
                {
                    thread(true) {
                        JsonParser.parseString(it.toString()).asJsonObject[CLASSES].asJsonArray.forEach {jE ->
                            baseArray.add(SearchElement(jE.asJsonObject[NUMBER].asString, jE.asJsonObject[LETTER].asString, jE.asJsonObject[SUBGROUP].asString, jE.asJsonObject[SCHOOL_ID].asString, jE.asJsonObject[SCHOOL_NAME].asString))
                        }
                        searchResults.clear()
                        searchResults.addAll(baseArray)
                        mActivity.runOnUiThread {
                            adapter.notifyDataSetChanged()
                            progressBarSearch.visibility = View.GONE
                            searchRecycler.visibility = View.VISIBLE
                        }
                    }
                },
                {}
        ))
    }

    fun choose(id: String, number: String, letter: String, subgroup: String) {
        targetFragment!!.onActivityResult(SEARCH_REQUEST_CODE, Activity.RESULT_OK, Intent().putExtra(CLASS, "$number.$letter.$subgroup").putExtra(SCHOOL_ID, id))
        dismissAllowingStateLoss()
    }

}