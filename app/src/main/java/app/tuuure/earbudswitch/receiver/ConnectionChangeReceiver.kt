package app.tuuure.earbudswitch.receiver

import android.bluetooth.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import app.tuuure.earbudswitch.CancelAdvertiseEvent
import app.tuuure.earbudswitch.data.Preferences
import app.tuuure.earbudswitch.data.db.DbRecord
import app.tuuure.earbudswitch.data.db.EarbudsDatabase
import app.tuuure.earbudswitch.service.AdvertiseService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.greenrobot.eventbus.EventBus
import org.koin.java.KoinJavaComponent.inject

class ConnectionChangeReceiver : BroadcastReceiver() {
    val database: EarbudsDatabase by inject(EarbudsDatabase::class.java)
    val preferences: Preferences by inject(Preferences::class.java)

    private fun startService(context: Context, device: BluetoothDevice, state: Int) {
        val service = Intent(context, AdvertiseService::class.java).apply {
            putExtra(BluetoothDevice.EXTRA_DEVICE, device.address)
            putExtra(BluetoothProfile.EXTRA_STATE, state)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            context.startForegroundService(service)
        else
            context.startService(service)
    }

    private suspend fun updateRecord(record: DbRecord, action: String, state: Int) {
        when (state) {
            BluetoothProfile.STATE_CONNECTING -> {
                when (action) {
                    BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED -> {
                        record.isA2dpConnected = false
                        record.isA2dpConnecting = true
                    }
                    BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED -> {
                        record.isHeadsetConnected = false
                        record.isHeadsetConnecting = true
                    }
                }
            }
            BluetoothProfile.STATE_CONNECTED -> {
                when (action) {
                    BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED -> {
                        record.isA2dpConnected = true
                        record.isA2dpConnecting = false
                    }
                    BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED -> {
                        record.isHeadsetConnected = true
                        record.isHeadsetConnecting = false
                    }
                }
            }
            else -> {
                when (action) {
                    BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED -> {
                        record.isA2dpConnected = false
                        record.isA2dpConnecting = false
                    }
                    BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED -> {
                        record.isHeadsetConnected = false
                        record.isHeadsetConnecting = false
                    }
                }
            }
        }
        database.dbDao().update(record)
    }

    override fun onReceive(context: Context, intent: Intent) {
        val device: BluetoothDevice? = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
        if (device == null || device.name == null || device.name.isEmpty() || device.bluetoothClass.majorDeviceClass != BluetoothClass.Device.Major.AUDIO_VIDEO)
            return

        if (BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED == intent.action || BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED == intent.action) {
            runBlocking(Dispatchers.IO) {
                val state = intent.getIntExtra(BluetoothProfile.EXTRA_STATE, -1)
                val record = database.dbDao().findByAddress(device.address)
                updateRecord(record, intent.action!!, state)
                when (state) {
                    BluetoothProfile.STATE_CONNECTED -> {
                        if (when (preferences.restrictMode) {
                                Preferences.RestrictMode.ALLOW -> record.isAllowed
                                Preferences.RestrictMode.BLOCK -> !record.isBlocked
                            }
                        ) {
                            //Log.d("TAG", "CONNECTED")
                            // 已连接到设备，开始广播
                            startService(context, device, state)
                        }
                    }
                    BluetoothProfile.STATE_DISCONNECTED -> {
                        //Log.d("TAG", "DISCONNECTED")
                        EventBus.getDefault().post(CancelAdvertiseEvent(device.address))
                    }
                }
            }
        }
    }
}