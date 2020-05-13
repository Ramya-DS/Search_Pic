package com.example.searchpic.search

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.os.AsyncTask
import android.util.Log
import android.widget.ImageView
import androidx.core.app.ActivityCompat.startPostponedEnterTransition
import androidx.core.content.ContextCompat
import com.example.searchpic.R
import com.example.searchpic.SearchPicApplication
import java.lang.ref.WeakReference
import java.net.URL

class LoadImage(
    private val imageView: WeakReference<ImageView>,
    private val activity: WeakReference<Activity>
) :
    AsyncTask<String, Unit, Bitmap>() {
    override fun doInBackground(vararg params: String?): Bitmap {
        val inputStream = URL(params[0]).openStream()
        val bitmap = BitmapFactory.decodeStream(inputStream)
        SearchPicApplication.accessCache().put(params[0]!!, bitmap)
        Log.d("LRU", "${SearchPicApplication.accessCache()}\n")
        if (params[1] == "image") {
            val app = (activity.get()!!.application as SearchPicApplication)
            synchronized(app.diskCacheLock) {
                app.diskLruCache?.apply {
                    if (!app.containsKey(params[2])) {
                        app.put(params[2]!!, bitmap)
                    }
                }
            }
        }
        return bitmap
    }

    override fun onPostExecute(result: Bitmap?) {
        result?.let {
            imageView.get()!!.setImageBitmap(it)
            startPostponedEnterTransition(activity.get()!!)
        }

    }
}