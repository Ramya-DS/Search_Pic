package com.example.searchpic.search


import android.content.res.Configuration
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.example.searchpic.R
import com.example.searchpic.search.datamodel.ImageDetails
import com.example.searchpic.util.OnImageClickedListener
import com.example.searchpic.util.OnLoadMoreItemsListener
import java.lang.ref.WeakReference

class SearchResultsFragment : Fragment() {

    private lateinit var rootView: View
    lateinit var recyclerView: RecyclerView
    lateinit var viewModel: ResultFragmentViewModel
    lateinit var adapter: ImageAdapter
    var loading = true
    var end = false
    var onLoadMoreItemsListener: OnLoadMoreItemsListener? = null

    companion object {
        fun newInstance(): SearchResultsFragment {
            return SearchResultsFragment()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        rootView = inflater.inflate(R.layout.fragment_search_results, container, false)

        viewModelInitialisation()

        recyclerViewInitialisation()

        if (viewModel.resultList.isNotEmpty()) {
            onLoadMoreItemsListener?.showFilter()
            (recyclerView.adapter as ImageAdapter).setImageList(viewModel.resultList)
        }

        return rootView
    }

    private fun viewModelInitialisation() {
        viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(activity!!.application)
        ).get(ResultFragmentViewModel::class.java)

    }

    private fun recyclerViewInitialisation() {
        recyclerView = rootView.findViewById(R.id.image_recycler)
        val spanCount =
            if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) 2 else 3
        val mLayoutManager = StaggeredGridLayoutManager(spanCount, 1)
        val displayMetrics = DisplayMetrics()
        activity!!.windowManager.defaultDisplay.getMetrics(displayMetrics)
        val width = displayMetrics.widthPixels/spanCount
        Log.d("fragment", width.toString())
        mLayoutManager.gapStrategy =
            StaggeredGridLayoutManager.GAP_HANDLING_MOVE_ITEMS_BETWEEN_SPANS;
        recyclerView.layoutManager = mLayoutManager
        recyclerView.setHasFixedSize(true)
        recyclerView.itemAnimator = null
        adapter = ImageAdapter(activity as OnImageClickedListener, WeakReference(activity!!),width)
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
                        onLoadMoreItemsListener?.onLoadMoreItems()
                    }
                }
            }
        })
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        (recyclerView.adapter as ImageAdapter).resultList.let {
            viewModel.resultList.clear()
            viewModel.resultList.addAll(it)
        }
    }

    fun setFirstPage(result: List<ImageDetails>) {
        adapter.setImageList(result)
    }
}
