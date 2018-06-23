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
import com.google.gson.JsonParser
import pro.eugw.lessoncountdown.R
import pro.eugw.lessoncountdown.activity.MainActivity
import pro.eugw.lessoncountdown.list.search.SearchAdapter
import pro.eugw.lessoncountdown.list.search.SearchItem
import pro.eugw.lessoncountdown.util.*
import java.net.HttpURLConnection
import java.net.URL
import java.util.*
import kotlin.collections.ArrayList
import kotlin.concurrent.thread

class SearchDialog : DialogFragment() {

    private lateinit var recyclerView: androidx.recyclerview.widget.RecyclerView
    private lateinit var adapter: SearchAdapter
    private var arrayList = ArrayList<SearchItem>()
    private var baseArray = ArrayList<SearchItem>()
    private lateinit var host: String

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val v = inflater.inflate(R.layout.fragment_search, container, false)
        v.findViewById<EditText>(R.id.searchEditText).addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val tamar = ArrayList<SearchItem>()
                baseArray.forEach {
                    val mmm = it
                    var match = true
                    s.toString().toUpperCase().split(" ").forEach {
                        if (!(mmm.number + mmm.letter + mmm.subgroup + mmm.school_name).replace(" ", "").toUpperCase().contains(it)) {
                            match = false
                        }
                    }
                    if (match)
                        tamar.add(it)
                }
                arrayList.clear()
                arrayList.addAll(tamar)
                activity!!.runOnUiThread { adapter.notifyDataSetChanged() }
            }
        })
        recyclerView = v.findViewById(R.id.searchRecycler)
        return v
    }

    override fun onStart() {
        super.onStart()
        dialog.window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        host = (activity as MainActivity).prefs.getString(CUSTOM_ADDRESS, getString(R.string.host))
        if (host.isBlank())
            host = getString(R.string.host)
        adapter = SearchAdapter(arrayList, this)
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.addItemDecoration(androidx.recyclerview.widget.DividerItemDecoration(recyclerView.context, LinearLayoutManager(context).orientation))
        recyclerView.adapter = adapter
        thread(true) {
            try {
                val url = URL("http://$host/classes?lang=${Locale.getDefault().language}")
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = HTTP_TIMEOUT
                conn.readTimeout = HTTP_TIMEOUT
                conn.connect()
                JsonParser().parse(conn.inputStream.reader()).asJsonObject[CLASSES].asJsonArray.forEach {
                    baseArray.add(SearchItem(it.asJsonObject[NUMBER].asString, it.asJsonObject[LETTER].asString, it.asJsonObject[SUBGROUP].asString, it.asJsonObject[SCHOOL_ID].asString, it.asJsonObject[SCHOOL_NAME].asString))
                }
                arrayList.clear()
                arrayList.addAll(baseArray)
                activity!!.runOnUiThread { adapter.notifyDataSetChanged() }
            } catch (e: Exception) {
                e.printStackTrace()
                activity!!.runOnUiThread { Toast.makeText(context, R.string.networkErr, Toast.LENGTH_LONG).show() }
                dismiss()
            }
        }

    }

    fun choose(id: String, number: String, letter: String, subgroup: String) {
        targetFragment!!.onActivityResult(SEARCH_REQUEST_CODE, Activity.RESULT_OK, Intent().putExtra(CLASS, "$number.$letter.$subgroup").putExtra(SCHOOL_ID, id))
        dialog.dismiss()
    }

}