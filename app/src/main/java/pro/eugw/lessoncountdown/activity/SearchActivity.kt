package pro.eugw.lessoncountdown.activity

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.Toast
import com.google.gson.JsonParser
import pro.eugw.lessoncountdown.BaseActivity
import pro.eugw.lessoncountdown.R
import pro.eugw.lessoncountdown.list.search.SearchAdapter
import pro.eugw.lessoncountdown.list.search.SearchItem
import java.net.HttpURLConnection
import java.net.URL
import java.util.*
import kotlin.collections.ArrayList
import kotlin.concurrent.thread

class SearchActivity: BaseActivity() {

    private lateinit var recyclerView: RecyclerView
    private var arrayList = ArrayList<SearchItem>()
    private lateinit var host: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)
        host = getSharedPreferences("newPrefs", Context.MODE_PRIVATE).getString("cAddress", getString(R.string.host))
        if (host.isBlank())
            host = getString(R.string.host)
        recyclerView = findViewById(R.id.searchRecycler)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.addItemDecoration(DividerItemDecoration(recyclerView.context, LinearLayoutManager(this).orientation))
        thread(true) {
            try {
                val url = URL("http://" + host + "/classes?lang=${Locale.getDefault().language}")
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = resources.getInteger(R.integer.timeout)
                conn.readTimeout = resources.getInteger(R.integer.timeout)
                conn.connect()
                arrayList = ArrayList()
                JsonParser().parse(conn.inputStream.reader()).asJsonObject["classes"].asJsonArray.forEach {
                    arrayList.add(SearchItem(it.asJsonObject["number"].asString, it.asJsonObject["letter"].asString, it.asJsonObject["subgroup"].asString, it.asJsonObject["school_id"].asString, it.asJsonObject["school_name"].asString))
                }
                runOnUiThread {
                    updateAdapter(arrayList)
                }
            } catch (e: Exception) {
                runOnUiThread { Toast.makeText(this@SearchActivity, R.string.networkErr, Toast.LENGTH_LONG).show() }
                finish()
            }
        }
        findViewById<EditText>(R.id.searchEditText).addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val tamar = ArrayList<SearchItem>()
                arrayList.forEach {
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
                updateAdapter(tamar)
            }
        })
    }


    fun choose(id: String, number: String, letter: String, subgroup: String) {
        setResult(Activity.RESULT_OK, Intent().putExtra("class", "$number.$letter.$subgroup").putExtra("school_id", id))
        finish()
    }

    private fun updateAdapter(arr: ArrayList<SearchItem>) {
        recyclerView.adapter = SearchAdapter(arr, this)
    }



}