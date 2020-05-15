package com.example.searchpic.imagedisplay

import android.app.DownloadManager
import android.content.*
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.util.DisplayMetrics
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.WorkerThread
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.example.searchpic.R
import com.example.searchpic.SearchPicApplication
import com.example.searchpic.search.LoadImage
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import java.lang.ref.WeakReference
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.ThreadPoolExecutor

class ImageDisplayActivity : AppCompatActivity() {

    var width: Int = 0
    var height: Int = 0
    var downloadId: Long = -1L
    lateinit var id: String
    private var descriptionText: String? = null
    lateinit var raw: String
    private lateinit var downloadLink: String
    private val set = ConstraintSet()
    lateinit var coordinatorLayout: CoordinatorLayout
    private lateinit var bitmap: String
    lateinit var executor: ThreadPoolExecutor

    companion object {
        val permissions = arrayOf(
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
            android.Manifest.permission.READ_EXTERNAL_STORAGE
        )
        const val REQUEST_PERMISSION = 1234
    }

    private var onComplete: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctxt: Context, intent: Intent) {
            val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
            if (id == downloadId) {
                val manager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                val uri = manager.getUriForDownloadedFile(downloadId)

                if (uri != null)
                    Snackbar
                        .make(coordinatorLayout, "Download complete!", Snackbar.LENGTH_LONG)
                        .setAction("View") {
                            val i = Intent()
                            i.action = Intent.ACTION_VIEW
                            i.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                            i.setDataAndType(uri, "image/*")
                            startActivity(i)
                        }
                        .show()
                else
                    Snackbar
                        .make(coordinatorLayout, "Download failed!", Snackbar.LENGTH_LONG)
                        .show()
            }
        }
    }

    private var onNotificationClick: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctxt: Context, intent: Intent) {
            Toast.makeText(ctxt, "on notification click", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_display)

        executor = Executors.newFixedThreadPool(5) as ThreadPoolExecutor

        coordinatorLayout = findViewById(R.id.coordinatorLayout)
        val toolbar: MaterialToolbar = findViewById(R.id.top_actionBar)
        setSupportActionBar(toolbar)

        toolbar.setNavigationOnClickListener {
            supportFinishAfterTransition()
        }

        registerReceiver(onComplete, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
        registerReceiver(
            onNotificationClick,
            IntentFilter(DownloadManager.ACTION_NOTIFICATION_CLICKED)
        )

        getIntentData()

        setImage()

        setDescription()

        val download: MaterialButton = findViewById(R.id.download)

        download.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED && checkSelfPermission(
                        android.Manifest.permission.READ_EXTERNAL_STORAGE
                    ) == PackageManager.PERMISSION_GRANTED
                )
                    downloadImage()
                else
                    requestPermissions(permissions, REQUEST_PERMISSION)
            } else {
                downloadImage()
            }

        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_PERMISSION && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
            downloadImage()
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && (shouldShowRequestPermissionRationale(
                android.Manifest.permission.READ_EXTERNAL_STORAGE
            ) || shouldShowRequestPermissionRationale(android.Manifest.permission.WRITE_EXTERNAL_STORAGE))
        ) {
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Permission is needed")
                .setMessage("This permission is essential for the downloading and storing the image")
                .setPositiveButton("Ok") { _: DialogInterface, i: Int ->
                    requestPermissions(Companion.permissions, REQUEST_PERMISSION)
                }
                .setNegativeButton("Cancel") { dialog, _ ->
                    dialog.dismiss()
                    Toast.makeText(
                        this,
                        "Storage permission is necessary for downloading image",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                .create().show()
        } else {
            val snackBar = Snackbar
                .make(
                    coordinatorLayout,
                    "You have denied permission to access storage. Approve this permission in the app settings of your device ",
                    Snackbar.LENGTH_LONG
                )
                .setAction("Settings") {
                    val i = Intent()
                    i.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                    i.addCategory(Intent.CATEGORY_DEFAULT)
                    i.data = Uri.parse("package:$packageName")
                    i.flags =
                        Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                    startActivity(i)
                }
            snackBar.view.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)
                .setLines(4)
            snackBar.show()
        }
    }


    private fun getIntentData() {
        val bundle = intent.getBundleExtra("image")
        bundle?.let {
            id = it.getString("id", "1")
            descriptionText = it.getString("description")
            raw = it.getString("raw", "raw")
            downloadLink = it.getString("download", "download")
            width = it.getInt("width")
            height = it.getInt("height")
            bitmap = it.getString("bitmap", "")
        }
    }

    private fun setImage() {
        val imageDisplay: ImageView = findViewById(R.id.image_display)
        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        val mWidth = displayMetrics.widthPixels
        val mHeight = (mWidth * height) / width
        Log.d("preview", "$mWidth, $mHeight")

        imageFetch(imageDisplay, mWidth, mHeight)

        val constraintLayout = findViewById<ConstraintLayout>(R.id.displayConstraint)
        val ratio = String.format("%d:%d", width, height)
        set.clone(constraintLayout)
        set.setDimensionRatio(imageDisplay.id, ratio)
        set.applyTo(constraintLayout)
    }

    private fun setDescription() {
        val description: TextView = findViewById(R.id.description)
        descriptionText?.let {
            description.text = it
        }
    }

    private fun downloadImage() {
        var fileName = id
        fileName += ".jpg"
        Log.d("ext", fileName)

        val downloadManager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val uri: Uri = Uri.parse(downloadLink)
        val request = DownloadManager.Request(uri)
        request.setTitle("SearchPic Image Download")
        request.setDescription("Downloading")
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        Log.d("download", Environment.DIRECTORY_DOWNLOADS)
        request.setDestinationInExternalPublicDir(
            Environment.DIRECTORY_DOWNLOADS,
            fileName
        )
        downloadId = downloadManager.enqueue(request)
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(onComplete)
        unregisterReceiver(onNotificationClick)
    }

    private fun getBitmapFromDiskCache(key: String): Bitmap? {
        val app = (application as SearchPicApplication)
        if (app.containsKey(key))
            return app.getBitmap(key)
        return null
    }


    private fun imageFetch(imageDisplay: ImageView, mWidth: Int, mHeight: Int) {
        executor.execute(Runnable {
            val diskImage = getBitmapFromDiskCache(id.toLowerCase(Locale.ENGLISH))
            if (diskImage == null) {
                val cacheImage = SearchPicApplication.accessCache()[bitmap]
                cacheImage?.let {
                    val scaledBitmap = Bitmap.createScaledBitmap(it, mWidth, mHeight, false)
                    runOnUiThread {
                        imageDisplay.setImageBitmap(scaledBitmap)
                    }
                }
                LoadImage(WeakReference(imageDisplay), WeakReference(this)).execute(
                    raw,
                    mWidth.toString(),
                    mHeight.toString(),
                    "disk",
                    id.toLowerCase(Locale.ENGLISH)
                )
            } else {
                runOnUiThread {
                    imageDisplay.setImageBitmap(diskImage)
                }
            }
        })
    }

    override fun onStop() {
        super.onStop()
        executor.shutdown()
    }
}
