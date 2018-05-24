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
import android.widget.Toast
import com.google.gson.JsonParser
import kotlinx.android.synthetic.main.activity_search.*
import pro.eugw.lessoncountdown.R
import pro.eugw.lessoncountdown.list.search.SearchAdapter
import pro.eugw.lessoncountdown.list.search.SearchItem
import pro.eugw.lessoncountdown.util.*
import java.net.HttpURLConnection
import java.net.URL
import java.util.*
import kotlin.collections.ArrayList
import kotlin.concurrent.thread

class SearchActivity : Activity() {

    private lateinit var recyclerView: RecyclerView
    private var arrayList = ArrayList<SearchItem>()
    private lateinit var host: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(if (getSharedPreferences(APP_PREFERENCES, Context.MODE_PRIVATE).getBoolean(DARK_THEME, false)) R.style.AppTheme_Dark else R.style.AppTheme)
        setContentView(R.layout.activity_search)
        host = getSharedPreferences(APP_PREFERENCES, Context.MODE_PRIVATE).getString(CUSTOM_ADDRESS, getString(R.string.host))
        if (host.isBlank())
            host = getString(R.string.host)
        recyclerView = searchRecycler
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.addItemDecoration(DividerItemDecoration(recyclerView.context, LinearLayoutManager(this).orientation))
        thread(true) {
            try {
                val url = URL(host + "/classes?lang=${Locale.getDefault().language}")
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = HTTP_TIMEOUT
                conn.readTimeout = HTTP_TIMEOUT
                conn.connect()
                arrayList = ArrayList()
                JsonParser().parse(conn.inputStream.reader()).asJsonObject[CLASSES].asJsonArray.forEach {
                    arrayList.add(SearchItem(it.asJsonObject[NUMBER].asString, it.asJsonObject[LETTER].asString, it.asJsonObject[SUBGROUP].asString, it.asJsonObject[SCHOOL_ID].asString, it.asJsonObject[SCHOOL_NAME].asString))
                }
                runOnUiThread {
                    updateAdapter(arrayList)
                }
            } catch (e: Exception) {
                runOnUiThread { Toast.makeText(this@SearchActivity, R.string.networkErr, Toast.LENGTH_LONG).show() }
                finish()
            }
        }
        searchEditText.addTextChangedListener(object : TextWatcher {
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
        setResult(Activity.RESULT_OK, Intent().putExtra(CLASS, "$number.$letter.$subgroup").putExtra(SCHOOL_ID, id))
        finish()
    }

    private fun updateAdapter(arr: ArrayList<SearchItem>) {
        recyclerView.adapter = SearchAdapter(arr, this)
    }



}