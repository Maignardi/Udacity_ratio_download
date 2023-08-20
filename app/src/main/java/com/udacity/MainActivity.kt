package com.udacity

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.RemoteViews
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.constraintlayout.motion.widget.MotionLayout
import com.udacity.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var motionLayout: MotionLayout
    private var downloadID: Long = 0
    private lateinit var notificationManager: NotificationManager
    private lateinit var downloadManager: DownloadManager
    private var totalBytes = -1L
    private var downloadedBytes = -1L

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        motionLayout = binding.contentMain.mainMotionLayout

        createNotificationChannel()

        downloadManager = getSystemService(DOWNLOAD_SERVICE) as DownloadManager

        registerReceiver(receiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))

        binding.contentMain.customButton.setOnClickListener {
            binding.contentMain.customButton.buttonState = ButtonState.Loading
            monitorDownloadProgress()

            val url = getSelectedURL()
            if (url == "") {
                showToast("Please select an option to download")
                Handler().postDelayed({
                    binding.contentMain.customButton.buttonState = ButtonState.Completed
                }, 1000)
                return@setOnClickListener
            }

            startDownload(url)
        }
    }

    private fun startDownload(url: String) {
        val request = DownloadManager.Request(Uri.parse(url))
            .setTitle(getString(R.string.app_name))
            .setDescription(getString(R.string.app_description))
            .setRequiresCharging(false)
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)

        downloadID = downloadManager.enqueue(request)

        val notificationLayout = RemoteViews(packageName, R.layout.custom_notification_layout)
        notificationLayout.setTextViewText(R.id.notificationTitle, "Download Complete")
        notificationLayout.setTextViewText(R.id.notificationMessage, "File downloaded successfully.")

        val contentIntent = Intent(this@MainActivity, DetailActivity::class.java).apply {
            putExtra("FILE_NAME", url)
            putExtra("STATUS", "Download Complete")
        }

        val animationPendingIntent = PendingIntent.getActivity(
            this@MainActivity,
            0,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        notificationLayout.setOnClickPendingIntent(R.id.checkDetailsButton, animationPendingIntent)

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setCustomBigContentView(notificationLayout)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(1, notification)
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val id = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)

            if (id == downloadID) {
                val status = getDownloadStatus(id)

                if (status == DownloadManager.STATUS_SUCCESSFUL) {
                    val radioGroup: RadioGroup = findViewById(R.id.radioGroup)
                    val selectedRadioButtonId = radioGroup.checkedRadioButtonId
                    val selectedRadioButton: RadioButton? = findViewById(selectedRadioButtonId)

                    val uriString = selectedRadioButton?.text?.toString() ?: "Default Text"
                    val message = "File: $uriString downloaded successfully."
                    showToast(message)

                    binding.contentMain.customButton.buttonState = ButtonState.Completed

                    showCustomNotification(uriString, "Download Complete", message)

                } else {
                    val message = "There was an error during download."
                    showToast(message)
                }
            }
        }
    }

    private fun showCustomNotification(fileName: String, title: String, message: String) {
        val notificationLayout = RemoteViews(packageName, R.layout.custom_notification_layout)
        notificationLayout.setTextViewText(R.id.notificationTitle, title)
        notificationLayout.setTextViewText(R.id.notificationMessage, message)

        val contentIntent = Intent(this@MainActivity, DetailActivity::class.java).apply {
            putExtra("FILE_NAME", fileName)
            putExtra("STATUS", title)
        }

        val pendingIntentFlags = PendingIntent.FLAG_UPDATE_CURRENT or
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    PendingIntent.FLAG_MUTABLE
                } else {
                    0
                }

        val animationPendingIntent = PendingIntent.getActivity(this@MainActivity, 0, contentIntent, pendingIntentFlags)
        notificationLayout.setOnClickPendingIntent(R.id.checkDetailsButton, animationPendingIntent)

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setCustomBigContentView(notificationLayout)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(1, notification)
    }

    private fun monitorDownloadProgress() {
        val handler = Handler()
        val runnable = object : Runnable {
            override fun run() {
                if (binding.contentMain.customButton.buttonState == ButtonState.Loading) {
                    updateProgress()
                    handler.postDelayed(this, 50)
                }
            }
        }
        handler.post(runnable)
    }

    @SuppressLint("Range")
    private fun updateProgress() {
        val downloadQuery = DownloadManager.Query()
        downloadQuery.setFilterById(downloadID)

        val cursor = downloadManager.query(downloadQuery)
        if (cursor.moveToFirst()) {
            totalBytes = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)).toLong()
            downloadedBytes = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)).toLong()

            val progress = (downloadedBytes.toFloat() / totalBytes.toFloat()).coerceAtMost(1.0f)
            binding.contentMain.customButton.updateDownloadProgress(progress)
        }
        cursor.close()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelName = "Download Notification Channel"
            val channelDescription = "Notifications for download status"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, channelName, importance).apply {
                description = channelDescription
            }

            notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    @SuppressLint("Range")
    private fun getDownloadStatus(id: Long): Int {
        val query = DownloadManager.Query()
        query.setFilterById(id)
        val cursor = downloadManager.query(query)
        if (cursor.moveToFirst()) {
            return cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS))
        }
        return DownloadManager.STATUS_FAILED
    }

    private fun getSelectedURL(): String {
        return when (binding.contentMain.radioGroup.checkedRadioButtonId) {
            R.id.radioOption1 -> ratio1
            R.id.radioOption2 -> ratio2
            R.id.radioOption3 -> ratio3
            else -> ""
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    companion object {
        private const val ratio1 = "https://github.com/bumptech/glide"
        private const val ratio2 = "https://github.com/udacity/nd940-c3-advanced-android-programming-project-starter/archive/master.zip"
        private const val ratio3 = "https://github.com/square/retrofit"
        private const val CHANNEL_ID = "channelId"
    }
}
