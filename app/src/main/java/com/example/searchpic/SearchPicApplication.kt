package com.example.searchpic

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.AsyncTask
import android.os.Environment
import android.os.Environment.isExternalStorageRemovable
import android.util.Log
import androidx.collection.LruCache
import com.jakewharton.disklrucache.DiskLruCache
import java.io.*
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock


class SearchPicApplication : Application() {

    var diskLruCache: DiskLruCache? = null
    val diskCacheLock = ReentrantLock()
    private val diskCacheLockCondition: Condition = diskCacheLock.newCondition()
    private var diskCacheStarting = true

    companion object {
        private var memoryCache: LruCache<String, Bitmap>? = null

        private const val DISK_CACHE_SIZE: Long = 1024 * 1024 * 10 // 10MB
        private const val DISK_CACHE_SUBDIR = "thumbnails"

        fun accessCache(): LruCache<String, Bitmap> {
            val tempInstance = memoryCache
            if (tempInstance != null) {
                return tempInstance
            }

            val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
            val cacheSize = maxMemory / 8
            val cache = object : LruCache<String, Bitmap>(cacheSize) {
                override fun sizeOf(key: String, bitmap: Bitmap): Int {
                    return bitmap.byteCount / 1024
                }
            }
            memoryCache = cache

            return cache
        }
    }

    override fun onCreate() {
        super.onCreate()
        val cacheDir = getDiskCacheDir(this, DISK_CACHE_SUBDIR)
        InitDiskCacheTask().execute(cacheDir)
    }

    internal inner class InitDiskCacheTask : AsyncTask<File, Void, Void>() {
        override fun doInBackground(vararg params: File): Void? {
            diskCacheLock.withLock {
                val cacheDir = params[0]
                diskLruCache =
                    DiskLruCache.open(cacheDir, 1, 1, DISK_CACHE_SIZE)
                diskCacheStarting = false // Finished initialization
                diskCacheLockCondition.signalAll() // Wake any waiting threads
            }
            return null
        }
    }

    // Creates a unique subdirectory of the designated app cache directory. Tries to use external
// but if not mounted, falls back on internal storage.
    private fun getDiskCacheDir(context: Context, uniqueName: String): File {
        // Check if media is mounted or storage is built-in, if so, try and use external cache dir
        // otherwise use internal cache dir
        val cachePath =
            if (Environment.MEDIA_MOUNTED == Environment.getExternalStorageState()
                || !isExternalStorageRemovable()
            ) {
                context.externalCacheDir?.path
            } else {
                context.cacheDir.path
            }

        return File(cachePath + File.separator + uniqueName)
    }

    fun containsKey(key: String?): Boolean {
        var contained = false
        var snapshot: DiskLruCache.Snapshot? = null
        try {
            snapshot = diskLruCache?.get(key)
            contained = snapshot != null
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            snapshot?.close()
        }
        return contained
    }

    fun put(key: String, data: Bitmap) {
        var editor: DiskLruCache.Editor? = null
        try {
            editor = diskLruCache?.edit(key)
            if (editor == null) {
                return
            }
            if (writeBitmapToFile(data, editor)) {
                diskLruCache?.flush()
                editor.commit()
                if (BuildConfig.DEBUG) {
                    Log.d("cache_test_DISK_", "image put on disk cache $key")
                }
            } else {
                editor.abort()
                if (BuildConfig.DEBUG) {
                    Log.d("cache_test_DISK_", "ERROR on: image put on disk cache $key")
                }
            }
        } catch (e: IOException) {
            if (BuildConfig.DEBUG) {
                Log.d("cache_test_DISK_", "ERROR on: image put on disk cache $key")
            }
            try {
                editor?.abort()
            } catch (ignored: IOException) {
            }
        }
    }

    @Throws(IOException::class, FileNotFoundException::class)
    private fun writeBitmapToFile(
        bitmap: Bitmap,
        editor: DiskLruCache.Editor
    ): Boolean {
        var out: OutputStream? = null
        return try {
            out = BufferedOutputStream(editor.newOutputStream(0), 8 * 1024)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 70, out)
        } finally {
            out?.close()
        }
    }

    fun getBitmap(key: String): Bitmap? {
        var bitmap: Bitmap? = null
        var snapshot: DiskLruCache.Snapshot? = null
        try {
            snapshot = diskLruCache?.get(key)
            if (snapshot == null) {
                return null
            }
            val `in` = snapshot.getInputStream(0)
            if (`in` != null) {
                val buffIn = BufferedInputStream(`in`, 8 * 1024)
                bitmap = BitmapFactory.decodeStream(buffIn)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            snapshot?.close()
        }
        if (BuildConfig.DEBUG) {
            Log.d(
                "cache_test_DISK_",
                if (bitmap == null) "" else "image read from disk $key"
            )
        }
        return bitmap
    }

}
