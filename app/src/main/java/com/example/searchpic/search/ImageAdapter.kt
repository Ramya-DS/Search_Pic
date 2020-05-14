package com.example.searchpic.search

import android.app.Activity
import android.graphics.Bitmap
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
                    putString("id", it.id)
                    putString("raw", it.urls.regular)
                    putString("description", it.description)
                    putString("download", it.links.download)
                    putInt("width", it.width)
                    putInt("height", it.height)
                    putString("bitmap", it.urls.small)
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
            getBitmapFromVectorDrawable(
                R.drawable.placeholder,
                50, 50
            )
        )

        setBitmapInHolder(holder, result)
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
            val mImage: ImageDetails?
            mImage = bundle.getParcelable("newImage")
            mImage?.let {
                holder.imageView.setImageBitmap(
                    getBitmapFromVectorDrawable(
                        R.drawable.placeholder,
                        50, 50
                    )
                )
                holder.image = it
                setBitmapInHolder(holder, it)
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
                diffBundle.putParcelable("newImage", newNote)
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

    private fun setBitmapInHolder(holder: ImageViewHolder, image: ImageDetails) {
        val bitmap = SearchPicApplication.accessCache()[image.urls.small]
        if (bitmap == null) {
            holder.imageTask?.cancel(true)
            holder.imageTask = LoadImage(WeakReference(holder.imageView), activity)
            Log.d("main", " ")
            holder.imageTask!!.execute(image.urls.small, "thumb")
        } else
            holder.imageView.setImageBitmap(bitmap)

        val ratio = String.format("%d:%d", image.width, image.height)
        Log.d("view", "${image.width} ${image.height}")
        set.clone(holder.mConstraintLayout)
        set.setDimensionRatio(holder.imageView.id, ratio)
        set.applyTo(holder.mConstraintLayout)
    }
}
