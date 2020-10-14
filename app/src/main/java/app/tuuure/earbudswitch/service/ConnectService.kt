package app.tuuure.earbudswitch.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import app.tuuure.earbudswitch.R

class ConnectService : Service() {
    companion object {
        private val CHANNEL_ID = ConnectService::class.java.simpleName
        private const val CHANNEL_NAME = "Connect Service"
        private val NOTIFICATION_ID = CHANNEL_ID.hashCode()
    }

    private var notificationBuilder: NotificationCompat.Builder =
        NotificationCompat.Builder(this, CHANNEL_ID)
    private lateinit var notificationManager: NotificationManager

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        registerNotificationChannel()

        notificationBuilder.setSmallIcon(R.drawable.ic_ebs_connecting)
            .setColor(getColor(R.color.colorAccent))
            .setContentText(getString(R.string.notification_blank_content))
        startForeground(NOTIFICATION_ID, notificationBuilder.build())


    }

    override fun onDestroy() {
        stopForeground(true)
        super.onDestroy()
    }

    private fun registerNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = notificationManager.getNotificationChannel(CHANNEL_ID)
            if (notificationChannel == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_LOW
                )
                channel.enableLights(false)
                channel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                channel.setShowBadge(false)
                notificationManager.createNotificationChannel(channel)
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

}