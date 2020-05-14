package com.example.searchpic.search

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.AsyncTask
import android.util.Log
import android.widget.ImageView
import androidx.core.app.ActivityCompat.startPostponedEnterTransition
import com.example.searchpic.SearchPicApplication
import java.lang.ref.WeakReference
import java.net.URL

class LoadImage(
    private val imageView: WeakReference<ImageView>,
    private val activity: WeakReference<Activity>
) : AsyncTask<String, Unit, Bitmap>() {
    override fun doInBackground(vararg params: String?): Bitmap {
        val inputStream = URL(params[0]).openStream()
        //TODO Bitmap is directly loaded from network stream instead of downsizing to view dimensions
        val bitmap = BitmapFactory.decodeStream(inputStream)
        SearchPicApplication.accessCache().put(params[0]!!, bitmap)
        Log.d("LRU", "${SearchPicApplication.accessCache()}\n")
        if (params[1] == "image") {
            val app = (activity.get()!!.application as SearchPicApplication)
            //TODO this synchronization won't work. study the difference between synchronized block and withLock block
            synchronized(app.diskCacheLock) {
                //TODO why non-null check ? is apply block required here ?
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
            //TODO How can a weak reference be asserted as non-null ?
            imageView.get()!!.setImageBitmap(it)
            startPostponedEnterTransition(activity.get()!!)
        }

    }
}