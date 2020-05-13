package com.example.searchpic.search

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.os.AsyncTask
import android.widget.ImageView
import androidx.core.app.ActivityCompat.startPostponedEnterTransition
import androidx.core.content.ContextCompat
import com.example.searchpic.R
import java.lang.ref.WeakReference
import java.net.URL

class LoadImage(
    private val imageView: WeakReference<ImageView>,
    private val activity: WeakReference<Activity>
) :
    AsyncTask<String, Unit, Bitmap>() {
    override fun doInBackground(vararg params: String?): Bitmap {
        val inputStream = URL(params[0]).openStream()
        return BitmapFactory.decodeStream(inputStream)
    }

    override fun onPostExecute(result: Bitmap?) {
        val drawable =
            ContextCompat.getDrawable(imageView.get()!!.context, R.drawable.placeholder)
        imageView.get()!!.setImageBitmap(result ?: (drawable as BitmapDrawable).bitmap)
        startPostponedEnterTransition(activity.get()!!)
    }
}