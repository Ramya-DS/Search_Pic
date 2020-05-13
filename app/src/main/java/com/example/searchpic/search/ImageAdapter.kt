package com.example.searchpic.search

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.searchpic.R
import com.example.searchpic.SearchPicApplication
import com.example.searchpic.search.datamodel.ImageDetails
import com.example.searchpic.util.OnImageClickedListener
import java.lang.ref.WeakReference


class ImageAdapter(
    val onImageClickedListener: OnImageClickedListener,
    private val activity: WeakReference<Activity>
) :
    RecyclerView.Adapter<ImageAdapter.ImageViewHolder>() {

    var resultList = mutableListOf<ImageDetails>()
    private val set = ConstraintSet()

    inner class ImageViewHolder(view: View) : RecyclerView.ViewHolder(view), View.OnClickListener {
        val imageView: ImageView = view.findViewById(R.id.imgSource)
        var imageTask: LoadImage? = null
        val mConstraintLayout: ConstraintLayout = view.findViewById(R.id.parentConstraint)
        var image: ImageDetails? = null

        init {
            mConstraintLayout.setOnClickListener(this)
        }

        override fun onClick(v: View?) {
            image?.let {
                Log.d("image", image.toString())
                val bundle = Bundle().apply {
                    this.putString("id", it.id)
                    this.putString("raw", it.urls.regular)
                    this.putString("description", it.description)
                    this.putString("download", it.links.download)
                    this.putInt("width", it.width)
                    this.putInt("height", it.height)
                }
                onImageClickedListener.onImageClicked(bundle, imageView)
            }
        }
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
        holder.imageView.setImageBitmap(
            getBitmapFromVectorDrawable(R.drawable.placeholder,
                50,50)
        )
        val bitmap = SearchPicApplication.accessCache()[result.urls.small]
        if (bitmap == null) {
            if (holder.imageTask != null) {
                holder.imageTask!!.cancel(true)
            }
            holder.imageTask = LoadImage(WeakReference(holder.imageView), activity)
            holder.imageTask!!.execute(result.urls.small, "thumb")
        } else
            holder.imageView.setImageBitmap(bitmap)

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
                    mImage = bundle.getParcelable(key)
            }
            mImage?.let {
                holder.imageView.setImageBitmap(
                    getBitmapFromVectorDrawable(
                        R.drawable.placeholder,
                        50,50
                    )
                )
                holder.image = it
                val bitmap = SearchPicApplication.accessCache()[it.urls.small]
                if (bitmap == null) {
                    if (holder.imageTask != null) {
                        holder.imageTask!!.cancel(true)
                    }
                    holder.imageTask = LoadImage(WeakReference(holder.imageView), activity)
                    holder.imageTask!!.execute(it.urls.small, "thumb")
                } else
                    holder.imageView.setImageBitmap(bitmap)

                val ratio = String.format("%d:%d", it.width, it.height)
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
                diffBundle.putParcelable("newNote", newNote)
            }

            return if (diffBundle.size() == 0) null else diffBundle

        }

    }

    override fun onViewRecycled(holder: ImageViewHolder) {
        super.onViewRecycled(holder)
        holder.imageTask?.cancel(true)
    }

    private fun getBitmapFromVectorDrawable(drawableId: Int, width: Int, height: Int): Bitmap? {
        var drawable =
            ContextCompat.getDrawable(activity.get()!!, drawableId)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            drawable = DrawableCompat.wrap(drawable!!).mutate()
        }
        val bitmap = Bitmap.createBitmap(
            width,
            height, Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)

        drawable!!.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }
}
