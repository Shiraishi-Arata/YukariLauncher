package com.arata.yukarilauncher.feature.network

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.lifecycle.MutableLiveData

import com.arata.yukarilauncher.R

class HostServerService : Service() {

    private lateinit var playitManager: PlayitManager
    private val binder = LocalBinder()

    val logs = MutableLiveData<String>()
    val authUrl = MutableLiveData<String?>()
    val tunnelAddress = MutableLiveData<String?>()
    val isRunning = MutableLiveData(false)
    val downloadProgress = MutableLiveData<Int?>()

    inner class LocalBinder : android.os.Binder() {
/**
 * getServiceする
 */
        fun getService(): HostServerService = this@HostServerService
    }

/**
 * onCreateする
 */
    override fun onCreate() {
        super.onCreate()
        playitManager = PlayitManager(applicationContext)
        setupPlayitCallbacks()
        startForegroundNotification()
    }

/**
 * onBindする
 */
    override fun onBind(intent: Intent?): IBinder = binder

/**
 * onStartCommandする
 */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("HostServerService", "onStartCommand action: ${intent?.action}")
        when (intent?.action) {
            ACTION_START -> {
                val port = intent.getIntExtra(EXTRA_PORT, 25565)
                startTunnel(port)
            }
            ACTION_STOP -> {
                stopTunnel()
                stopForeground(STOP_FOREGROUND_REMOVE)
                cancelNotification()
                stopSelf()
            }
        }
        return START_STICKY
    }

/**
 * setupPlayitCallbacksする
 */
    private fun setupPlayitCallbacks() {
        playitManager.onLog = { line ->
            logs.postValue(line)
        }
        playitManager.onAuthRequired = { url ->
            authUrl.postValue(url)
        }
        playitManager.onTunnelReady = { address ->
            tunnelAddress.postValue(address)
            updateNotification("Tunnel ready: $address")
        }
        playitManager.onTunnelRunning = {
            if (tunnelAddress.value == null) {
                tunnelAddress.postValue("")
            }
            isRunning.postValue(true)
            updateNotification("Tunnel running")
        }
        playitManager.onStopped = {
            isRunning.postValue(false)
            tunnelAddress.postValue(null)
            authUrl.postValue(null)
        }
        // Forward download progress
        playitManager.onDownloadProgress = { progress ->
            downloadProgress.postValue(progress)
        }
    }

/**
 * startTunnelする
 */
    private fun startTunnel(port: Int) {
        try {
            playitManager.start(port)
            isRunning.postValue(true)
            updateNotification("Tunnel connecting...")
            Toast.makeText(this, "Tunnel started on port $port", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e("HostServerService", "Failed to start tunnel", e)
            Toast.makeText(this, "Failed to start tunnel: ${e.message}", Toast.LENGTH_LONG).show()
            stopSelf()
        }
    }

/**
 * stopTunnelする
 */
    private fun stopTunnel() {
        playitManager.stop()
        isRunning.postValue(false)
        cancelNotification()
    }

/**
 * cancelNotificationする
 */
    private fun cancelNotification() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(NOTIFICATION_ID)
    }

/**
 * startForegroundNotificationする
 */
    private fun startForegroundNotification() {
        val channelId = "tunnel_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Tunnel Service",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }

        val stopIntent = Intent(this, HostServerService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 0, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Playit Tunnel")
            .setContentText("Tunnel is active")
            .setSmallIcon(R.drawable.ic_notification_yl)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopPendingIntent)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

/**
 * updateNotificationする
 */
    private fun updateNotification(text: String) {
        val notification = NotificationCompat.Builder(this, "tunnel_channel")
            .setContentTitle("Playit Tunnel")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_notification_yl)
            .setOngoing(true)
            .build()
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .notify(NOTIFICATION_ID, notification)
    }

/**
 * onDestroyする
 */
    override fun onDestroy() {
        stopTunnel()
        cancelNotification()
        super.onDestroy()
    }

    companion object {
        const val ACTION_START = "START_TUNNEL"
        const val ACTION_STOP = "STOP_TUNNEL"
        const val EXTRA_PORT = "port"
        private const val NOTIFICATION_ID = 2001
    }
}