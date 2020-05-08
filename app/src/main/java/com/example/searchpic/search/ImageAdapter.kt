package com.example.searchpic.search

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.content.res.AppCompatResources.getDrawable
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.example.searchpic.R
import com.example.searchpic.search.datamodel.ImageDetails

class ImageAdapter : RecyclerView.Adapter<ImageAdapter.ImageViewHolder>() {

    var resultList = mutableListOf<ImageDetails>()
    private val set = ConstraintSet()

    class ImageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.imgSource)
        val mConstraintLayout: ConstraintLayout = view.findViewById(R.id.parentConstraint)
        var image: ImageDetails? = null

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ImageViewHolder(
        LayoutInflater.from(parent.context).inflate(
            R.layout.search_result_layout,
            parent,
            false
        )
    )

    override fun getItemCount() = resultList.size

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        val result = resultList[position]
        holder.image = result
        Glide.with(holder.imageView.context)
            .load(result.urls.small)
            .placeholder(getDrawable(holder.imageView.context!!, R.drawable.placeholder))
            .transition(DrawableTransitionOptions.withCrossFade())
            .into(holder.imageView)

        val ratio = String.format("%d:%d", result.width, result.height)
        set.clone(holder.mConstraintLayout)
        set.setDimensionRatio(holder.imageView.id, ratio)
        set.applyTo(holder.mConstraintLayout)
    }

    override fun onBindViewHolder(
        holder: ImageViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        if (payloads.isEmpty()) {
            super.onBindViewHolder(holder, position, payloads)
        } else {
            val bundle = payloads[0] as Bundle
            var mImage: ImageDetails? = null
            for (key in bundle.keySet()) {
                if (key == "newNote")
                    mImage = bundle.getSerializable(key) as ImageDetails
            }
            mImage?.let {
                holder.image = mImage
                Glide.with(holder.imageView.context)
                    .load(mImage.urls.small)
                    .placeholder(getDrawable(holder.imageView.context!!, R.drawable.placeholder))
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .into(holder.imageView)

                val ratio = String.format("%d:%d", mImage.width, mImage.height)
                set.clone(holder.mConstraintLayout)
                set.setDimensionRatio(holder.imageView.id, ratio)
                set.applyTo(holder.mConstraintLayout)
            }
        }
    }

    fun setImageList(newResults: List<ImageDetails>) {
        val result = DiffUtil.calculateDiff(
            ResultListDiffUtilCallback(
                this.resultList,
                newResults
            )
        )
        result.dispatchUpdatesTo(this)
        resultList.clear()
        resultList.addAll(newResults)
    }

    fun appendImageList(newResults: List<ImageDetails>) {
        val newResultsList = mutableListOf<ImageDetails>()
        newResultsList.addAll(resultList)
        newResultsList.addAll(newResults)
        val result = DiffUtil.calculateDiff(
            ResultListDiffUtilCallback(
                this.resultList,
                newResultsList
            )
        )
        result.dispatchUpdatesTo(this)
        resultList.addAll(newResults)

    }

    class ResultListDiffUtilCallback(
        private var oldResult: List<ImageDetails>,
        private var newResult: List<ImageDetails>
    ) : DiffUtil.Callback() {

        override fun getOldListSize(): Int = oldResult.size

        override fun getNewListSize(): Int = newResult.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
            oldResult[oldItemPosition].id == newResult[newItemPosition].id

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
            oldResult[oldItemPosition] == newResult[newItemPosition]

        override fun getChangePayload(oldItemPosition: Int, newItemPosition: Int): Any? {
            val oldNote = oldResult[oldItemPosition]
            val newNote = newResult[newItemPosition]

            val diffBundle = Bundle()

            if (oldNote.id != newNote.id) {
                diffBundle.putSerializable("newNote", newNote)
            }

            return if (diffBundle.size() == 0) null else diffBundle

        }

    }


}
