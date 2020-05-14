package com.example.searchpic.search

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.net.ConnectivityManager
import android.net.ConnectivityManager.TYPE_MOBILE
import android.net.ConnectivityManager.TYPE_WIFI
import android.net.NetworkInfo
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.app.ActivityOptionsCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.ViewModelProvider
import com.example.searchpic.R
import com.example.searchpic.imagedisplay.ImageDisplayActivity
import com.example.searchpic.search.datamodel.SearchResult
import com.example.searchpic.util.OnImageClickedListener
import com.example.searchpic.util.OnLoadMoreItemsListener
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.floatingactionbutton.FloatingActionButton
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class SearchActivity : AppCompatActivity(), DrawerLayout.DrawerListener, OnImageClickedListener,
    OnLoadMoreItemsListener {

    private lateinit var viewModel: SearchActivityViewModel
    private lateinit var searchView: SearchView
    private lateinit var searchApiCall: SearchApiCall
    private var currentOptions = HashMap<String, String>()
    private lateinit var filter: FloatingActionButton
    private var mQuery: String = ""
    private lateinit var textInfo: TextView
    var resultFragment: SearchResultsFragment? = null

    companion object {
        val ORDER_BY = arrayListOf("relevant", "latest")
        val CONTENT_FILTER = arrayListOf("low", "high")
        val COLOR = arrayListOf(
            "black_and_white",
            "black",
            "white",
            "yellow",
            "orange",
            "red",
            "purple",
            "magenta",
            "green",
            "teal",
            "blue"
        )
        val ORIENTATION = arrayListOf("landscape", "portrait", "squarish")

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)

        textInfo = findViewById(R.id.searchInfo)

        viewModelInitialisation()

        val client = OkHttpClient.Builder().addInterceptor { chain ->
            val newRequest: Request = chain.request().newBuilder()
                .addHeader(
                    "Authorization",
                    "Client-ID sexbGgsXWJ6Qa1xIkrHOo_432vbZPZaAOLYMX1Oqy_8"
                )
                .build()
            chain.proceed(newRequest)
        }.build()

        val retrofit: Retrofit = Retrofit.Builder()
            .client(client)
            .baseUrl("https://api.unsplash.com/search/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        searchApiCall = retrofit.create(SearchApiCall::class.java)

        filter = findViewById(R.id.filter)
        filter.hide()
        searchBarInitialisation()
        sideSheetInitialisation()

        networkErrorDialog()

        if (savedInstanceState != null) {
            supportFragmentManager.findFragmentByTag("Result")?.let {
                resultFragment = it as SearchResultsFragment
                resultFragment?.onLoadMoreItemsListener = this
                supportFragmentManager.beginTransaction()
                    .replace(R.id.container, it).commit()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        searchView.clearFocus()
    }

    private fun viewModelInitialisation() {
        viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        ).get(SearchActivityViewModel::class.java)
    }

    private fun searchBarInitialisation() {

        searchView = findViewById(R.id.search_bar)
        searchView.isFocusable = false
        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        searchView.maxWidth = (displayMetrics.widthPixels * 0.90).toInt()

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let {
                    if (it.trim().isNotEmpty()) {
                        viewModel.mQuery = it
                        clearFilterOptions()
                        removeNoResultFragment()
                        removeDisplayResultFragment()
                        displayLoadingFragment()
                        parametersInitialised(it)

                        fetchSearchResult()
                    } else {
                        Toast.makeText(
                            this@SearchActivity,
                            "Please enter some text to search",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                newText?.let {
                    mQuery = it
                }
                return false
            }
        })
    }

    private fun parametersInitialised(query: String) {
        viewModel.optionQueryMap.apply {
            put("query", query)
            put("page", "1")
            put("per_page", "50")
            remove("color")
            remove("content_filter")
            remove("order_by")
            remove("orientation")
        }
    }

    private fun sideSheetInitialisation() {

        val drawerLayout: DrawerLayout = findViewById(R.id.drawer_layout)
        filter.setOnClickListener {
            searchView.clearFocus()
            if (mQuery == "")
                searchView.setQuery(viewModel.mQuery, false)
            else if (mQuery != viewModel.optionQueryMap["query"]) {
                viewModel.optionQueryMap["query"] = mQuery
                viewModel.mQuery = mQuery
                removeDisplayResultFragment()
            }
            drawerLayout.openDrawer(GravityCompat.END)
        }
        val filterButton: MaterialButton = findViewById(R.id.filter_button)


        filterButton.setOnClickListener {
            viewModel.optionQueryMap["page"] = "1"
            currentOptions["order_by"]?.let {
                viewModel.optionQueryMap["order_by"] = it
            }
            currentOptions["content_filter"]?.let {
                viewModel.optionQueryMap["content_filter"] = it
            }
            currentOptions["color"]?.let {
                if (it == "deselect") {
                    viewModel.optionQueryMap.remove("color")
                } else
                    viewModel.optionQueryMap["color"] = it
            }
            currentOptions["orientation"]?.let {
                if (it == "deselect") {
                    viewModel.optionQueryMap.remove("orientation")
                } else
                    viewModel.optionQueryMap["orientation"] = it
            }
            currentOptions.clear()
            Log.d("query map", viewModel.optionQueryMap.toString())
            fetchSearchResult()
            drawerLayout.closeDrawer(GravityCompat.END)

        }

        drawerLayout.setDrawerListener(this)
    }

    private fun fetchSearchResult() {
        val call = searchApiCall.getImageResultsList(viewModel.optionQueryMap)
        call.enqueue(object : Callback<SearchResult> {
            override fun onFailure(call: Call<SearchResult>, t: Throwable) {
                networkErrorDialog()
            }

            override fun onResponse(call: Call<SearchResult>, response: Response<SearchResult>) {
                if (response.isSuccessful) {
                    when {
                        viewModel.optionQueryMap["page"] == "1" -> displayFirstPageOfSearchResult(
                            response.body()
                        )
                        viewModel.optionQueryMap["page"]!!.toInt() != response.body()!!.totalPages -> displayNextSetOfSearchResults(
                            response.body()
                        )
                        else -> {
                            Toast.makeText(
                                this@SearchActivity,
                                "End of search Results",
                                Toast.LENGTH_SHORT
                            ).show()
                            resultFragment?.end = true
                        }
                    }
                } else {
                    try {
                        val jObjError = JSONObject(response.errorBody()!!.string())
                        Toast.makeText(
                            this@SearchActivity,
                            "Error: ${jObjError.getJSONObject("error").getString("message")}",
                            Toast.LENGTH_LONG
                        ).show()
                    } catch (e: Exception) {
                        Toast.makeText(this@SearchActivity, e.message, Toast.LENGTH_LONG).show()
                    }
                }
            }
        })
    }

    private fun displayFirstPageOfSearchResult(result: SearchResult?) {
        result?.let {
            if (it.results.isNotEmpty()) {
                removeNoResultFragment()
                removeLoadingFragment()
                displayResultFragment()
                resultFragment?.setFirstPage(it.results)
                filter.show()
            } else {
                removeDisplayResultFragment()
                displayNoResultFragment()
                resultFragment?.recyclerView?.recycledViewPool?.clear()
                resultFragment?.adapter?.notifyDataSetChanged()
            }
            textInfo.text = "Search results for \"${viewModel.mQuery}\""
        }
    }

    private fun displayNextSetOfSearchResults(result: SearchResult?) {
        result?.let {
            resultFragment?.adapter?.appendImageList(it.results)
            resultFragment?.loading = true
        }
    }

    fun onOrderByOptionsClicked(view: View) {
        if (view is RadioButton) {
            val checked = view.isChecked
            if (checked) {
                when (view.getId()) {
                    R.id.relevant -> currentOptions["order_by"] = ORDER_BY[0]
                    R.id.latest -> currentOptions["order_by"] = ORDER_BY[1]
                }
            }
        }
    }

    fun onContentFilterOptionClicked(view: View) {
        if (view is RadioButton) {
            val checked = view.isChecked
            if (checked) {
                when (view.getId()) {
                    R.id.high -> currentOptions["content_filter"] = CONTENT_FILTER[1]
                    R.id.low -> currentOptions["content_filter"] = CONTENT_FILTER[0]
                }
            }
        }
    }

    fun onColorOptionsClicked(view: View) {
        if (view is Chip) {
            val checked = view.isChecked
            if (checked) {
                when (view.id) {
                    R.id.black_and_white -> currentOptions["color"] = COLOR[0]
                    R.id.black -> currentOptions["color"] = COLOR[1]
                    R.id.white -> currentOptions["color"] = COLOR[2]
                    R.id.yellow -> currentOptions["color"] = COLOR[3]
                    R.id.orange -> currentOptions["color"] = COLOR[4]
                    R.id.red -> currentOptions["color"] = COLOR[5]
                    R.id.purple -> currentOptions["color"] = COLOR[6]
                    R.id.magenta -> currentOptions["color"] = COLOR[7]
                    R.id.green -> currentOptions["color"] = COLOR[8]
                    R.id.teal -> currentOptions["color"] = COLOR[9]
                    R.id.blue -> currentOptions["color"] = COLOR[10]
                }
            } else currentOptions["color"] = "deselect"
        }
    }

    fun onOrientationOptionsClicked(view: View) {
        if (view is Chip) {
            val checked = view.isChecked
            if (checked) {
                when (view.id) {
                    R.id.landscape -> currentOptions["orientation"] = ORIENTATION[0]
                    R.id.portrait -> currentOptions["orientation"] = ORIENTATION[1]
                    R.id.squarish -> currentOptions["orientation"] = ORIENTATION[2]

                }
            } else
                currentOptions["orientation"] = "deselect"
        }
    }

    override fun onDrawerStateChanged(newState: Int) {
    }

    override fun onDrawerSlide(drawerView: View, slideOffset: Float) {
    }

    override fun onDrawerClosed(drawerView: View) {
        if (currentOptions.isNotEmpty()) {
            findViewById<RadioGroup>(R.id.order_by_group)?.let {
                it.clearCheck()
                if (viewModel.optionQueryMap["order_by"] == null) {
                    it.check(R.id.relevant)
                } else {
                    (it.getChildAt(ORDER_BY.indexOf(viewModel.optionQueryMap["order_by"])) as RadioButton).isChecked =
                        true
                }
            }

            findViewById<RadioGroup>(R.id.content_group)?.let {
                it.clearCheck()
                if (viewModel.optionQueryMap["content_filter"] == null) {
                    it.check(R.id.low)
                } else {
                    (it.getChildAt(CONTENT_FILTER.indexOf(viewModel.optionQueryMap["content_filter"])) as RadioButton).isChecked =
                        true
                }
            }

            findViewById<ChipGroup>(R.id.color_group).let {
                it.clearCheck()
                if (viewModel.optionQueryMap["color"] != null) {
                    (it.getChildAt(COLOR.indexOf(viewModel.optionQueryMap["color"])) as Chip).isChecked =
                        true
                }
            }

            findViewById<ChipGroup>(R.id.orientation_group).let {
                it.clearCheck()
                if (viewModel.optionQueryMap["orientation"] != null) {
                    (it.getChildAt(ORIENTATION.indexOf(viewModel.optionQueryMap["orientation"])) as Chip).isChecked =
                        true
                }
            }

            currentOptions.clear()
        }
    }

    private fun clearFilterOptions() {
        findViewById<RadioGroup>(R.id.order_by_group)?.let {
            it.clearCheck()
            it.check(R.id.relevant)
        }

        findViewById<RadioGroup>(R.id.content_group)?.let {
            it.clearCheck()
            it.check(R.id.low)
        }

        findViewById<ChipGroup>(R.id.color_group).clearCheck()
        findViewById<ChipGroup>(R.id.orientation_group).clearCheck()
    }

    override fun onDrawerOpened(drawerView: View) {

    }

    override fun onImageClicked(imageBundle: Bundle, view: View) {
        val intent = Intent(this, ImageDisplayActivity::class.java)
        intent.putExtra("image", imageBundle)
        val options = ActivityOptionsCompat.makeSceneTransitionAnimation(this, view, "image")
        startActivity(intent, options.toBundle())
    }

    private fun networkErrorDialog() {

        val alertDialog = AlertDialog.Builder(this).setTitle("No internet connection")
            .setMessage("Internet is essential for the working of the application.Connect to the internet.")
            .setPositiveButton("Refresh") { _: DialogInterface, i: Int ->
                if (checkConnectivity()) {
                    resultFragment?.loading = true
                    return@setPositiveButton
                } else
                    networkErrorDialog()
            }
            .setNegativeButton("Exit") { _, _ ->
                finishAffinity()
            }.setCancelable(false)
            .create()

        if (checkConnectivity())
            alertDialog.dismiss()
        else
            alertDialog.show()
    }

    private fun checkConnectivity(): Boolean {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        if (cm.getNetworkInfo(TYPE_MOBILE)?.state == NetworkInfo.State.CONNECTED
            || cm.getNetworkInfo(TYPE_WIFI)?.state == NetworkInfo.State.CONNECTED
        ) {
            return true
        }
        return false
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finishAffinity()
    }

    override fun onLoadMoreItems() {
        incrementPageNumber()
        fetchSearchResult()
    }

    private fun displayResultFragment() {
        if (resultFragment == null) {
            resultFragment = SearchResultsFragment.newInstance()
            resultFragment?.onLoadMoreItemsListener = this
        }
        supportFragmentManager.beginTransaction()
            .replace(R.id.container, resultFragment!!, "Result").commit()

        supportFragmentManager.executePendingTransactions()
    }

    private fun displayNoResultFragment() {
        supportFragmentManager.beginTransaction()
            .replace(
                R.id.container,
                NoResultFragment(), "No result"
            ).commit()
    }

    private fun removeNoResultFragment() {
        supportFragmentManager.findFragmentByTag("No result")?.let {
            supportFragmentManager.beginTransaction().remove(it)
        }
    }

    private fun removeDisplayResultFragment() {
        supportFragmentManager.findFragmentByTag("Result")?.let {
            supportFragmentManager.beginTransaction().remove(it)
        }
        resultFragment = null
    }

    private fun displayLoadingFragment() {
        supportFragmentManager.beginTransaction()
            .replace(
                R.id.container,
                LoadingFragment(), "Loading"
            ).commit()
    }

    private fun removeLoadingFragment() {
        supportFragmentManager.findFragmentByTag("Loading")?.let {
            supportFragmentManager.beginTransaction().remove(it)
        }
    }

    private fun incrementPageNumber() {
        viewModel.optionQueryMap["page"] =
            (viewModel.optionQueryMap["page"]!!.toInt() + 1).toString()
    }

    override fun showFilter() {
        filter.show()
    }
}
