package app.tuuure.earbudswitch

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class AudioFocusManager(val context: Context) {
    companion object {
        private val TAG = AudioFocusManager::class.java.simpleName
        private const val timeInterval = 5000L
    }

    private val audioManager: AudioManager =
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    val isMusicActive: Boolean
        get() = audioManager.isMusicActive
    val isFocusGain: MutableLiveData<Boolean> = MutableLiveData(false)

    private fun MutableLiveData<Boolean>.postIfNot(newState: Boolean) {
        if (this.value != newState) {
            this.postValue(newState)
        }
    }

    private val listener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_GAIN -> {
                Log.d(TAG, "AUDIOFOCUS_GAIN")
                isFocusGain.postIfNot(true)
            }
            AudioManager.AUDIOFOCUS_LOSS -> {
                Log.d(TAG, "AUDIOFOCUS_LOSS")
                isFocusGain.postIfNot(false)
                abandonAudioFocus()
                waitForFocus()
            }
        }
    }

    init {
        waitForFocus()
    }

    private fun waitForFocus() = GlobalScope.launch {
        Log.d(TAG, "AUDIOFOCUS_WAITING")
        while (isMusicActive || AudioManager.AUDIOFOCUS_REQUEST_FAILED == requestAudioFocus()) {
            delay(timeInterval)
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

    @Suppress("DEPRECATION")
    private fun abandonAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioManager.abandonAudioFocusRequest(audioFocusRequest)
        } else {
            audioManager.abandonAudioFocus(listener)
        }
    }

    @Suppress("DEPRECATION")
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
            isFocusGain.postIfNot(it == AudioManager.AUDIOFOCUS_REQUEST_GRANTED)
            Log.d(
                TAG,
                when (it) {
                    AudioManager.AUDIOFOCUS_REQUEST_FAILED -> "AUDIOFOCUS_REQUEST_FAILED"
                    AudioManager.AUDIOFOCUS_REQUEST_GRANTED -> "AUDIOFOCUS_REQUEST_GRANTED"

                    AudioManager.AUDIOFOCUS_REQUEST_DELAYED -> "AUDIOFOCUS_REQUEST_DELAYED"
                    else -> {
                        throw Error("AUDIOFOCUS_REQUEST_UNKNOWN_RESULT_$it")
                    }
                }
            )
        }
}