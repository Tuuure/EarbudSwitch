package app.tuuure.earbudswitch.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import android.telephony.TelephonyManager.ACTION_PHONE_STATE_CHANGED


class CallReceiver: BroadcastReceiver() {
    // TelephonyManager.ACTION_PHONE_STATE_CHANGED
    // Intent.ACTION_NEW_OUTGOING_CALL
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_PHONE_STATE_CHANGED && intent.hasExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)){

        }
    }
}