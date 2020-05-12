package com.example.searchpic.search

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.res.Configuration
import android.net.*
import android.os.Build
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.View
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.example.searchpic.R
import com.example.searchpic.imagedisplay.ImageDisplayActivity
import com.example.searchpic.search.datamodel.SearchResult
import com.example.searchpic.util.OnImageClickedListener
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.floatingactionbutton.FloatingActionButton
import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.lang.Exception


class SearchActivity : AppCompatActivity(), DrawerLayout.DrawerListener, OnImageClickedListener {

    private lateinit var adapter: ImageAdapter
    private var loading = true
    private lateinit var recyclerView: RecyclerView
    private lateinit var viewModel: StateRestoringViewModel
    private lateinit var searchView: SearchView
    private var end = false
    private var currentOptions = HashMap<String, String>()
    private lateinit var filter: FloatingActionButton
    private var mQuery: String = ""

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

        val apiResponse = retrofit.create(ApiResponse::class.java)

        filter = findViewById(R.id.filter)
        filter.hide()
        searchBarInitialisation(apiResponse)

        recyclerViewInitialisation(apiResponse)

        sideSheetInitialisation(apiResponse)

        var mQuery = ""
        if (viewModel.optionQueryMap["query"] != null) {
            mQuery = viewModel.optionQueryMap["query"]!!
        }

        if (viewModel.resultList.isNotEmpty() && mQuery != "") {
            filter.show()
            (recyclerView.adapter as ImageAdapter).setImageList(viewModel.resultList)
        }

        networkErrorDialog()
    }

    override fun onResume() {
        super.onResume()
        searchView.clearFocus()
    }

    private fun viewModelInitialisation() {
        viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        ).get(StateRestoringViewModel::class.java)
    }

    private fun searchBarInitialisation(apiResponse: ApiResponse) {

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
                        parametersInitialised(it)
                        (recyclerView.adapter as ImageAdapter).resultList.clear()
                        fetchSearchResult(viewModel.optionQueryMap, apiResponse)
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
        viewModel.resultList.clear()
        viewModel.optionQueryMap["query"] = query
        viewModel.optionQueryMap["page"] = "1"
        viewModel.optionQueryMap["per_page"] = "50"
        viewModel.optionQueryMap.remove("color")
        viewModel.optionQueryMap.remove("content_filter")
        viewModel.optionQueryMap.remove("order_by")
    }

    private fun recyclerViewInitialisation(apiResponse: ApiResponse) {
        recyclerView = findViewById(R.id.image_recycler)
        val spanCount =
            if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) 2 else 3
        val mLayoutManager = StaggeredGridLayoutManager(spanCount, 1)
        mLayoutManager.gapStrategy =
            StaggeredGridLayoutManager.GAP_HANDLING_MOVE_ITEMS_BETWEEN_SPANS;
        recyclerView.layoutManager = mLayoutManager
        recyclerView.setHasFixedSize(true)
        recyclerView.itemAnimator = null
        adapter = ImageAdapter(this)
        recyclerView.adapter = adapter

        var pastVisibleItems = 0
        var visibleItemCount: Int
        var totalItemCount: Int

        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                visibleItemCount = mLayoutManager.childCount
                totalItemCount = mLayoutManager.itemCount
                var firstVisibleItems: IntArray? = null
                firstVisibleItems = mLayoutManager.findFirstVisibleItemPositions(firstVisibleItems);
                if (firstVisibleItems != null && firstVisibleItems.isNotEmpty()) {
                    pastVisibleItems = firstVisibleItems[0]
                }

                if (loading && !end) {
                    if ((visibleItemCount + pastVisibleItems) >= totalItemCount) {
                        loading = false
                        viewModel.optionQueryMap["page"] =
                            (viewModel.optionQueryMap["page"]!!.toInt() + 1).toString()
                        fetchSearchResult(viewModel.optionQueryMap, apiResponse)
                    }
                }
            }
        })
    }

    private fun sideSheetInitialisation(apiResponse: ApiResponse) {

        val drawerLayout: DrawerLayout = findViewById(R.id.drawer_layout)
        filter.setOnClickListener {
            searchView.clearFocus()
            searchView.setQuery(viewModel.mQuery, false)
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
                viewModel.optionQueryMap["color"] = it
            }
            currentOptions["orientation"]?.let {
                viewModel.optionQueryMap["orientation"] = it
            }
            currentOptions.clear()

            fetchSearchResult(viewModel.optionQueryMap, apiResponse)
            drawerLayout.closeDrawer(GravityCompat.END)

        }

        drawerLayout.setDrawerListener(this)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        (recyclerView.adapter as ImageAdapter).resultList.let {
            viewModel.resultList.clear()
            viewModel.resultList.addAll(it)
        }
    }

    private fun fetchSearchResult(parameters: Map<String, String>, apiResponse: ApiResponse) {
        val call = apiResponse.getImageResultsList(parameters)
        call.enqueue(object : Callback<SearchResult> {
            override fun onFailure(call: Call<SearchResult>, t: Throwable) {
//                Toast.makeText(applicationContext, "Error", Toast.LENGTH_SHORT).show()
                networkErrorDialog()
            }

            override fun onResponse(call: Call<SearchResult>, response: Response<SearchResult>) {
                if (response.isSuccessful) {
                    when {
                        parameters["page"] == "1" -> displayFirstPageOfSearchResult(response.body())
                        parameters["page"]!!.toInt() != response.body()!!.totalPages -> displayNextSetOfSearchResults(
                            response.body()
                        )
                        else -> {
                            Toast.makeText(
                                this@SearchActivity,
                                "End of search Results",
                                Toast.LENGTH_SHORT
                            ).show()
                            end = true
                        }
                    }
                } else {
                    Toast.makeText(
                        this@SearchActivity,
                        "${response.code()} ${response.errorBody()}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        })
    }

    private fun displayFirstPageOfSearchResult(result: SearchResult?) {
        result?.let {
            if (it.results.isNotEmpty()) {
                adapter.setImageList(it.results)
                filter.show()
            } else
                Toast.makeText(this, "No matches found", Toast.LENGTH_SHORT).show()
        }
    }

    private fun displayNextSetOfSearchResults(result: SearchResult?) {
        result?.let {
            adapter.appendImageList(it.results)
            loading = true
        }
    }

    fun onOrderByOptionsClicked(view: View) {
        if (view is RadioButton) {
            val checked = view.isChecked
            when (view.getId()) {
                R.id.relevant ->
                    if (checked) {
                        currentOptions["order_by"] = ORDER_BY[0]
                    }
                R.id.latest ->
                    if (checked) {
                        currentOptions["order_by"] = ORDER_BY[1]
                    }
            }
        }
    }

    fun onContentFilterOptionClicked(view: View) {
        if (view is RadioButton) {
            val checked = view.isChecked
            when (view.getId()) {
                R.id.high ->
                    if (checked) {
                        currentOptions["content_filter"] = CONTENT_FILTER[1]
                    }
                R.id.low ->
                    if (checked) {
                        currentOptions["content_filter"] = CONTENT_FILTER[0]
                    }
            }
        }
    }

    fun onColorOptionsClicked(view: View) {
        if (view is Chip) {
            val checked = view.isChecked
            when (view.id) {
                R.id.black_and_white -> {
                    if (checked) {
                        currentOptions["color"] = COLOR[0]
                    } else
                        currentOptions.remove("color")
                }
                R.id.black ->
                    if (checked) {
                        currentOptions["color"] = COLOR[1]
                    } else
                        currentOptions.remove("color")
                R.id.white ->
                    if (checked) {
                        currentOptions["color"] = COLOR[2]
                    } else
                        currentOptions.remove("color")
                R.id.yellow ->
                    if (checked) {
                        currentOptions["color"] = COLOR[3]
                    } else
                        currentOptions.remove("color")
                R.id.orange ->
                    if (checked) {
                        currentOptions["color"] = COLOR[4]
                    } else
                        currentOptions.remove("color")
                R.id.red ->
                    if (checked) {
                        currentOptions["color"] = COLOR[5]
                    } else
                        currentOptions.remove("color")
                R.id.purple ->
                    if (checked) {
                        currentOptions["color"] = COLOR[6]
                    } else
                        currentOptions.remove("color")
                R.id.magenta ->
                    if (checked) {
                        currentOptions["color"] = COLOR[7]
                    } else
                        currentOptions.remove("color")
                R.id.green ->
                    if (checked) {
                        currentOptions["color"] = COLOR[8]
                    } else
                        currentOptions.remove("color")
                R.id.teal ->
                    if (checked) {
                        currentOptions["color"] = COLOR[9]
                    } else
                        currentOptions.remove("color")
                R.id.blue ->
                    if (checked) {
                        currentOptions["color"] = COLOR[10]
                    } else
                        currentOptions.remove("color")
            }
        }
    }

    fun onOrientationOptionsClicked(view: View) {
        if (view is Chip) {
            val checked = view.isChecked
            when (view.id) {
                R.id.landscape ->
                    if (checked) {
                        currentOptions["orientation"] = ORIENTATION[0]
                    } else
                        currentOptions.remove("orientation")
                R.id.portrait ->
                    if (checked) {
                        currentOptions["orientation"] = ORIENTATION[1]
                    } else
                        currentOptions.remove("orientation")
                R.id.squarish ->
                    if (checked) {
                        currentOptions["orientation"] = ORIENTATION[2]
                    } else
                        currentOptions.remove("orientation")
            }
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

    override fun onImageClicked(imageBundle: Bundle) {
        val intent = Intent(this, ImageDisplayActivity::class.java)
        intent.putExtra("image", imageBundle)
        startActivity(intent)
    }

    private fun networkErrorDialog() {

        val alertDialog = AlertDialog.Builder(this).setTitle("No internet connection")
            .setMessage("Internet is essential for the working of the application.Connect to the internet.")
            .setPositiveButton("Refresh") { _: DialogInterface, i: Int ->
                if (checkConnectivity()) {
                    loading = true
                    return@setPositiveButton
                } else
                    networkErrorDialog()
            }
            .setNegativeButton("Exit") { dialog, which ->
//                dialog.dismiss()
                finishAffinity()
            }.setCancelable(false)
            .create()

        if (checkConnectivity())
            alertDialog.dismiss()
        else
            alertDialog.show()
    }

    private fun checkConnectivity(): Boolean {
        var isConnected: Boolean
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
            val activeNetwork: NetworkInfo? = cm.activeNetworkInfo
            isConnected = activeNetwork?.isConnectedOrConnecting == true
        } else {
            try {
                val builder = NetworkRequest.Builder()
                cm.registerDefaultNetworkCallback(object :
                    ConnectivityManager.NetworkCallback() {
                    override fun onLost(network: Network) {
                        super.onLost(network)
                        isConnected = false
                    }

                    override fun onAvailable(network: Network) {
                        super.onAvailable(network)
                        isConnected = true
                    }
                })

                isConnected = false
            } catch (e: Exception) {
                isConnected = false
            }
        }

        return isConnected
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finishAffinity()
    }
}
