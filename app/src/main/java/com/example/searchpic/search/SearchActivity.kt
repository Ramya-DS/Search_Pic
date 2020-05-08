package com.example.searchpic.search

import android.os.Bundle
import android.os.Parcelable
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.example.searchpic.R
import com.example.searchpic.search.datamodel.SearchResult
import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory


class SearchActivity : AppCompatActivity() {

    private lateinit var adapter: ImageAdapter
    private var page = 1
    private var mQuery: String = ""
    private var loading = true
    private lateinit var recyclerView: RecyclerView
    private lateinit var viewModel: StateRestoringViewModel
    private lateinit var searchView: SearchView
//    var scrollState: Parcelable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)

        savedInstanceState?.let {
            Log.d("query restored", mQuery)
            page = savedInstanceState.getInt("page")

        }

        viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        ).get(StateRestoringViewModel::class.java)


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

        searchView = findViewById(R.id.search_bar)
        searchView.isFocusable = false

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let {
                    if (it.isNotEmpty()) {
                        mQuery = it
                        page = 1
                        fetchSearchResult(it, apiResponse, page)
                    } else {
                        Toast.makeText(
                            this@SearchActivity,
                            "Please enter some text to search",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {

                return false
            }

        })

        recyclerView = findViewById(R.id.image_recycler)
        val mLayoutManager = StaggeredGridLayoutManager(2, 1)
        mLayoutManager.gapStrategy =
            StaggeredGridLayoutManager.GAP_HANDLING_MOVE_ITEMS_BETWEEN_SPANS;
        recyclerView.layoutManager = mLayoutManager
        recyclerView.setHasFixedSize(true)
        adapter = ImageAdapter()
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

                if (loading) {
                    if ((visibleItemCount + pastVisibleItems) >= totalItemCount) {
                        loading = false
                        page++
                        fetchSearchResult(mQuery, apiResponse, page)
                    }
                }
            }
        })

        mQuery = viewModel.mQuery

        if (viewModel.resultList.isNotEmpty() && mQuery != "") {
            (recyclerView.adapter as ImageAdapter).setImageList(viewModel.resultList)
        }
    }

    override fun onResume() {
        super.onResume()
        searchView.clearFocus()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("page", page)
        viewModel.resultList = (recyclerView.adapter as ImageAdapter).resultList
        viewModel.mQuery = mQuery
    }

    private fun fetchSearchResult(query: String, apiResponse: ApiResponse, mPage: Int) {
        val call = apiResponse.getImageResultsList(query, mPage)
        call.enqueue(object : Callback<SearchResult> {
            override fun onFailure(call: Call<SearchResult>, t: Throwable) {
                Toast.makeText(applicationContext, "Error", Toast.LENGTH_SHORT).show()
                //check for connectivity issues and display
            }

            override fun onResponse(call: Call<SearchResult>, response: Response<SearchResult>) {
                if (response.isSuccessful) {
                    if (mPage == 1)
                        displayFirstPageOfSearchResult(response.body())
                    else
                        displayNextSetOfSearchResults(response.body())
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
            adapter.setImageList(result.results)

        }
    }

    private fun displayNextSetOfSearchResults(result: SearchResult?) {
        result?.let {
            adapter.appendImageList(result.results)
            loading = true
        }
    }

}
