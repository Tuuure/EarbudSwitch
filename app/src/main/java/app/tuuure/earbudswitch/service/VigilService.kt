package app.tuuure.earbudswitch.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import app.tuuure.earbudswitch.AudioFocusManager
import app.tuuure.earbudswitch.Constants.Companion.Action
import app.tuuure.earbudswitch.R
import app.tuuure.earbudswitch.ble.BleAdvertiser

class VigilService : LifecycleService() {
    companion object {
        private val TAG = VigilService::class.java.simpleName
        private val CHANNEL_ID = VigilService::class.java.simpleName
        private const val CHANNEL_NAME = "Vigil Service"
        private val NOTIFICATION_ID = CHANNEL_ID.hashCode()
    }

    private var notificationBuilder: NotificationCompat.Builder =
        NotificationCompat.Builder(this, CHANNEL_ID)
    private lateinit var notificationManager: NotificationManager
    private lateinit var audioFocusManager: AudioFocusManager
    private lateinit var bleAdvertiser: BleAdvertiser

    private var isAdvertise = false
        set(value) {
            field = value
            updateNotification()
        }
    private var isFocusGain = false
        set(value) {
            if (field != value && !value && BluetoothAdapter.getDefaultAdapter().isEnabled) {
                bleAdvertiser.start(Action.ASK)
            }
            field = value
            updateNotification()
        }

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        registerNotificationChannel()

        initNotification()

        //注册用于关闭服务的receiver
        IntentFilter(CHANNEL_ID).also { intentFilter ->
            registerReceiver(receiver, intentFilter)
        }

        audioFocusManager = AudioFocusManager(this).apply {
            isFocusGain.observe(this@VigilService, { value ->
                this@VigilService.isFocusGain = value
            })
        }

        bleAdvertiser = BleAdvertiser(this).apply {
            isAdvertise.observe(this@VigilService, { value ->
                this@VigilService.isAdvertise = value
            })
        }
    }

    override fun onDestroy() {
        try {
            unregisterReceiver(receiver)
        } catch (ignored: IllegalArgumentException) {
        }
        stopForeground(true)
        super.onDestroy()
    }

    private fun updateNotification() {
        var content: String
        if (isFocusGain) {
            notificationBuilder.setSmallIcon(R.drawable.ic_monitor_paused)
            content = getString(R.string.notification_audio_gain)
        } else {
            notificationBuilder.setSmallIcon(R.drawable.ic_monitoring)
            content = getString(R.string.notification_audio_wait)
        }
        if (isAdvertise) {
            notificationBuilder.setSmallIcon(R.drawable.ic_ebs_connecting)
            content = getString(R.string.notification_advertise)
        }

        notificationBuilder.setContentText(content)

        notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build())
    }

    private fun initNotification() {
        notificationBuilder.apply {
            setSmallIcon(R.drawable.ic_monitoring)
            setContentText(CHANNEL_NAME)
            setOnlyAlertOnce(true)

            PendingIntent.getBroadcast(
                this@VigilService,
                0,
                Intent(CHANNEL_ID),
                PendingIntent.FLAG_CANCEL_CURRENT
            ).also { pendIntent ->
                addAction(
                    NotificationCompat.Action.Builder(
                        null,
                        getString(R.string.action_stop),
                        pendIntent
                    ).build()
                )
            }
        }

        startForeground(NOTIFICATION_ID, notificationBuilder.build())
    }

    private fun registerNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = notificationManager.getNotificationChannel(CHANNEL_ID)
            if (notificationChannel == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT
                )
                channel.enableLights(false)
                channel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                channel.setShowBadge(false)
                notificationManager.createNotificationChannel(channel)
            }
        }
    }

    // 用于关闭service
    private val receiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (CHANNEL_ID == intent.action) {
                stopSelf()
            }
        }
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return null
    }
}