package tkhamez.discordRelay.android

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import tkhamez.discordRelay.lib.*

class ForegroundService : Service() {
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onCreate() {
        super.onCreate()

        @SuppressLint("WakelockTimeout")
        wakeLock = (getSystemService(Context.POWER_SERVICE) as PowerManager).run {
            newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "DiscordRelay::WakelockTag").apply {
                acquire()
            }
        }
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        val channelId = "DiscordRelay WebSocket" // Must be unique per package

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) { // >= 8 (Orio), API 26
            // IMPORTANCE_LOW = No sound
            val serviceChannel = NotificationChannel(channelId, ResString.appName, NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(serviceChannel)
        }

        val pendingIntent = Intent(this, MainActivity::class.java).let { notificationIntent ->
            @SuppressLint("UnspecifiedImmutableFlag")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) { // >= 6 (Marshmallow), API 23
                PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE)
            } else {
                PendingIntent.getActivity(this, 0, notificationIntent, 0)
            }
        }
        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle(getString(R.string.connected_discord))
            .setSmallIcon(R.drawable.ic_stat_name)
            .setSound(null) // No sound for versions < Orio
            .setContentIntent(pendingIntent)
            .build()
        startForeground(1, notification)

        getGateway().init()

        return START_STICKY
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onDestroy() {
        wakeLock?.let {
            if (it.isHeld) {
                it.release()
            }
        }
        wakeLock = null

        getGateway().close()

        super.onDestroy()
    }
}
