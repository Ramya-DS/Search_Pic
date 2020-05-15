package com.example.searchpic.search

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.AsyncTask
import android.util.Log
import android.widget.ImageView
import com.example.searchpic.SearchPicApplication
import java.io.BufferedInputStream
import java.io.IOException
import java.io.InputStream
import java.lang.ref.WeakReference
import java.net.URL
import kotlin.concurrent.withLock


class LoadImage(
    private val imageView: WeakReference<ImageView>,
    private val activity: WeakReference<Activity>
) : AsyncTask<String, Unit, Bitmap>() {

    override fun doInBackground(vararg params: String?): Bitmap? {
        val url = params[0]!!
        val width = params[1]!!
        val height = params[2]!!
        val storage = params[3]!!

        val inputStream = URL(url).openStream()

        val bitmap =
            inputStream?.let { createScaledBitmapFromStream(it, width.toInt(), height.toInt()) }

        Log.d("converted", "${bitmap?.width}, ${bitmap?.height}")

        bitmap?.let {

            if (storage == "in memory")
                SearchPicApplication.accessCache().put(url, bitmap)

            if (storage == "disk") {
                val app = (activity.get()!!.application as SearchPicApplication)
                app.diskCacheLock.withLock {
                    app.apply {
                        if (!containsKey(params[4])) {
                            put(params[4]!!, bitmap)
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
        }

    }

    private fun createScaledBitmapFromStream(
        s: InputStream,
        minimumDesiredBitmapWidth: Int,
        minimumDesiredBitmapHeight: Int
    ): Bitmap? {
        var bitmap: Bitmap?
        val stream = BufferedInputStream(s, 8 * 1024)
        val decodeBitmapOptions = BitmapFactory.Options()
        if (minimumDesiredBitmapWidth > 0 && minimumDesiredBitmapHeight > 0) {
            val decodeBoundsOptions = BitmapFactory.Options()
            decodeBoundsOptions.inJustDecodeBounds = true
            stream.mark(8 * 1024)
            BitmapFactory.decodeStream(stream, null, decodeBoundsOptions)
            stream.reset()
            val originalWidth: Int = decodeBoundsOptions.outWidth
            val originalHeight: Int = decodeBoundsOptions.outHeight

            Log.d("original", "$originalWidth, $originalHeight")
            val scale =
                (originalWidth / minimumDesiredBitmapWidth).coerceAtMost(originalHeight / minimumDesiredBitmapHeight)

            return if (scale < 1) {
                Bitmap.createScaledBitmap(
                    BitmapFactory.decodeStream(stream),
                    minimumDesiredBitmapWidth,
                    minimumDesiredBitmapHeight,
                    false
                )
            } else {
                decodeBitmapOptions.inSampleSize = 1.coerceAtLeast(scale)
                BitmapFactory.decodeStream(stream, null, decodeBitmapOptions)
            }

        }

        return null

    }
}
