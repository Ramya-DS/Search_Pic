package com.example.searchpic.search

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.AsyncTask
import android.util.Log
import android.widget.ImageView
import androidx.core.app.ActivityCompat.startPostponedEnterTransition
import com.example.searchpic.SearchPicApplication
import java.io.BufferedInputStream
import java.io.FileInputStream
import java.io.InputStream
import java.lang.ref.WeakReference
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.withLock


class LoadImage(
    private val imageView: WeakReference<ImageView>,
    private val activity: WeakReference<Activity>
) :
    AsyncTask<String, Unit, Bitmap>() {
    override fun doInBackground(vararg params: String?): Bitmap? {
        val inputStream = URL(params[0]).openStream()
        val bitmap = BitmapFactory.decodeStream(inputStream)
        Log.d("bitmap", "${bitmap?.width} ${bitmap.height}")

        bitmap?.let {
            SearchPicApplication.accessCache().put(params[0]!!, bitmap)
            Log.d("LRU", "${SearchPicApplication.accessCache()}\n")
            if (params[1] == "image") {
                val app = (activity.get()!!.application as SearchPicApplication)
                app.diskCacheLock.withLock {
                    app.apply {
                        if (!containsKey(params[2])) {
                            put(params[2]!!, bitmap)
                        }
                    }
                }
            }
        }
        return bitmap
    }

    override fun onPostExecute(result: Bitmap?) {
        result?.let {
            imageView.get()?.setImageBitmap(it)
            activity.get()?.let { mActivity ->
//                startPostponedEnterTransition(mActivity)
            }
        }

    }
}