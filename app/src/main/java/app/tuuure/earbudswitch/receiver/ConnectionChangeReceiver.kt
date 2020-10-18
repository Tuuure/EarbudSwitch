package app.tuuure.earbudswitch.receiver

import android.bluetooth.BluetoothA2dp
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHeadset
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import app.tuuure.earbudswitch.ble.BleScanner
import app.tuuure.earbudswitch.data.Preferences
import app.tuuure.earbudswitch.data.db.EarbudsDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.koin.java.KoinJavaComponent.inject

class ConnectionChangeReceiver : BroadcastReceiver() {
    private val database: EarbudsDatabase by inject(EarbudsDatabase::class.java)
    private val preferences: Preferences by inject(Preferences::class.java)

    override fun onReceive(context: Context, intent: Intent) {
        if (BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED != intent.action && BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED != intent.action)
            return
        val device: BluetoothDevice? = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
        if (device == null || device.name == null || device.name.isEmpty())
            return

        val state = intent.getIntExtra(BluetoothProfile.EXTRA_STATE, -1)
        Log.d(intent.action, state.toString())

        runBlocking(Dispatchers.IO) {
            val record = database.dbDao().findByAddress(device.address)
            val isConnected = record.isA2dpConnected || record.isHeadSetConnected
            val restrictMode = preferences.restrictMode

            var isEmpty = true

            if (state == BluetoothProfile.STATE_CONNECTED) {
                isEmpty = database.dbDao().getConnected().none {
                    when (restrictMode) {
                        Preferences.RestrictMode.ALLOW -> it.isAllowed
                        Preferences.RestrictMode.BLOCK -> !it.isBlocked
                    }
                }
            }

            (state == BluetoothProfile.STATE_CONNECTED).also { connected ->
                when (intent.action) {
                    BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED -> {
                        if (connected != record.isA2dpConnected) {
                            record.isA2dpConnected = connected
                            database.dbDao().update(record)
                        }
                    }
                    BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED -> {
                        if (connected != record.isHeadSetConnected) {
                            record.isHeadSetConnected = connected
                            database.dbDao().update(record)
                        }
                    }
                }
            }

            if (state == BluetoothProfile.STATE_DISCONNECTED) {
                isEmpty = database.dbDao().getConnected().none {
                    when (restrictMode) {
                        Preferences.RestrictMode.ALLOW -> it.isAllowed
                        Preferences.RestrictMode.BLOCK -> !it.isBlocked
                    }
                }
            }

            if (isConnected != (record.isA2dpConnected || record.isHeadSetConnected)) {
                when (state) {
                    BluetoothProfile.STATE_CONNECTED -> {
                        if (isEmpty) {
                            BleScanner.stopScan(context)
                            BleScanner.startScan(context)
                        }
                    }
                    BluetoothProfile.STATE_DISCONNECTED -> {
                        if (isEmpty) {
                            BleScanner.stopScan(context)
                        }
                    }
                }
            }
        }
    }
}