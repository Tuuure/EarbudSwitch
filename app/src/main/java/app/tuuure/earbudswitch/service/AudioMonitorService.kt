package app.tuuure.earbudswitch.service

import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import app.tuuure.earbudswitch.R
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean


class AudioMonitorService : Service() {
    companion object {
        private val TAG = AudioMonitorService::class.java.simpleName
        private val CHANNEL_ID = AudioMonitorService::class.java.simpleName
        private const val CHANNEL_NAME = "Audio Monitor Service"
        private val NOTIFICATION_ID = CHANNEL_ID.hashCode()

        private const val timeInterval = 3000L
    }

    private var notificationBuilder: NotificationCompat.Builder =
        NotificationCompat.Builder(this, CHANNEL_ID)
    private lateinit var notificationManager: NotificationManager
    private lateinit var audioManager: AudioManager

    private val isMusicActive: Boolean
        get() = !this::audioManager.isInitialized || audioManager.isMusicActive
    private val isFocusGain = AtomicBoolean(false)

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        registerNotificationChannel()
        val pendIntent = PendingIntent.getBroadcast(
            this@AudioMonitorService,
            0,
            Intent(CHANNEL_ID),
            PendingIntent.FLAG_CANCEL_CURRENT
        )
        val actionBuilder = NotificationCompat.Action.Builder(null, "Stop", pendIntent)
        notificationBuilder.setSmallIcon(R.drawable.ic_monitoring)
            .setColor(getColor(R.color.colorAccent))
            .setContentText("Audio Monitor")
        notificationBuilder.addAction(actionBuilder.build())
        startForeground(NOTIFICATION_ID, notificationBuilder.build())

        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

        //注册用于关闭服务的receiver
        val intentFilter = IntentFilter()
        intentFilter.addAction(CHANNEL_ID)
        registerReceiver(receiver, intentFilter)

        waitForFocus()
    }

    enum class State {
        FOCUS_GAIN,
        FOCUS_LOSS,
        FOCUS_WAIT,
        FOCUS_ERROR
    }

    private fun updateNotification(state: State, errorCode: Int = 0) {
        when (state) {
            State.FOCUS_GAIN -> getText(R.string.notification_audio_gain)
            State.FOCUS_LOSS -> getText(R.string.notification_audio_loss)
            State.FOCUS_WAIT -> getText(R.string.notification_audio_wait)
            State.FOCUS_ERROR -> String.format(
                getString(R.string.notification_audio_error),
                errorCode
            )
        }.also {
            notificationBuilder.setContentText(it)
        }
        if (state == State.FOCUS_GAIN || state == State.FOCUS_ERROR) {
            notificationBuilder.setSmallIcon(R.drawable.ic_monitor_paused)
        } else {
            notificationBuilder.setSmallIcon(R.drawable.ic_monitoring)
        }
        notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build())
    }

    private val listener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_GAIN -> {
                Log.d(TAG, "AUDIOFOCUS_GAIN")
                updateNotification(State.FOCUS_GAIN)
                isFocusGain.set(true)
            }
            AudioManager.AUDIOFOCUS_LOSS -> {
                Log.d(TAG, "AUDIOFOCUS_LOSS")
                updateNotification(State.FOCUS_LOSS)
                isFocusGain.set(false)
                abandonAudioFocus()
                waitForFocus()
            }
        }
    }

    @RequiresApi(26)
    private val audioFocusRequest: AudioFocusRequest =
        AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN).run {
            setAudioAttributes(AudioAttributes.Builder().run {
                setUsage(AudioAttributes.USAGE_UNKNOWN)
                setContentType(AudioAttributes.CONTENT_TYPE_UNKNOWN)
                build()
            })
            setAcceptsDelayedFocusGain(true)
            setOnAudioFocusChangeListener(listener)
            build()
        }

    private fun abandonAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioManager.abandonAudioFocusRequest(audioFocusRequest)
        } else {
            audioManager.abandonAudioFocus(listener)
        }
    }

    private fun requestAudioFocus(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioManager.requestAudioFocus(audioFocusRequest)
        } else {
            audioManager.requestAudioFocus(
                listener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            )
        }.also {
            when (it) {
                AudioManager.AUDIOFOCUS_REQUEST_FAILED -> Log.d(TAG, "AUDIOFOCUS_REQUEST_FAILED")
                AudioManager.AUDIOFOCUS_REQUEST_GRANTED -> {
                    Log.d(TAG, "AUDIOFOCUS_REQUEST_GRANTED")
                    updateNotification(State.FOCUS_GAIN)
                }
                AudioManager.AUDIOFOCUS_REQUEST_DELAYED -> Log.d(TAG, "AUDIOFOCUS_REQUEST_DELAYED")
                else -> {
                    Log.d(TAG, "AUDIOFOCUS_REQUEST_UNKNOWN_RESULT_$it")
                    updateNotification(State.FOCUS_ERROR, errorCode = it)
                }
            }
        }


    private fun waitForFocus() = GlobalScope.launch {
        Log.d(TAG, "AUDIOFOCUS_WAITING")
        updateNotification(State.FOCUS_WAIT)
        while (isMusicActive || AudioManager.AUDIOFOCUS_REQUEST_FAILED == requestAudioFocus()) {
            delay(timeInterval)
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

    // 监听蓝牙关闭与自定义广播，用于关闭service
    private val receiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (CHANNEL_ID == intent.action)
                stopSelf()
        }
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }
}