package app.tuuure.earbudswitch.service

import android.app.Service
import android.content.Intent
import android.os.IBinder

class AudioMonitorService: Service() {
    override fun onBind(intent: Intent): IBinder? {
        return null
    }
}