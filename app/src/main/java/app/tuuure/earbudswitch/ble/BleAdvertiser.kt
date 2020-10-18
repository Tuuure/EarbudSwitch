package app.tuuure.earbudswitch.ble

import android.bluetooth.*
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.Context
import android.os.ParcelUuid
import android.util.Log
import androidx.lifecycle.MutableLiveData
import app.tuuure.earbudswitch.Constants
import app.tuuure.earbudswitch.Constants.Companion.Action
import app.tuuure.earbudswitch.Constants.Companion.DescriptorUUID
import app.tuuure.earbudswitch.Constants.Companion.ManufacturerID
import app.tuuure.earbudswitch.Constants.Companion.ServiceUUID
import app.tuuure.earbudswitch.Constants.Companion.VersionByte
import app.tuuure.earbudswitch.Constants.Companion.VersionCode
import app.tuuure.earbudswitch.data.Preferences
import app.tuuure.earbudswitch.utils.CryptoConvertUtils.Companion.otpGenerater
import app.tuuure.earbudswitch.utils.CryptoConvertUtils.Companion.randomBytes
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.java.KoinJavaComponent.inject
import java.nio.ByteBuffer

class BleAdvertiser(val context: Context) {

    private val preferences: Preferences by inject(Preferences::class.java)
    private lateinit var bluetoothLeAdvertiser: BluetoothLeAdvertiser
    private lateinit var gattServer: BluetoothGattServer
    val isAdvertise: MutableLiveData<Boolean> = MutableLiveData(false)

    private val advertiseSettings = AdvertiseSettings.Builder()
        .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
        .setConnectable(true)
        .setTimeout(0)
        .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
        .build()

    private val advertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
            super.onStartSuccess(settingsInEffect)
            isAdvertise.postValue(true)
            GlobalScope.launch {
                delay(Constants.ADVERTISE_TIMEOUT.toLong())
                stop()
            }
        }
    }

    private var gattServerCallback: BluetoothGattServerCallback =
        object : BluetoothGattServerCallback() {

            override fun onConnectionStateChange(
                device: BluetoothDevice,
                status: Int,
                newState: Int
            ) {
                Log.d("ConnectionState", "${device.address} $newState")
                super.onConnectionStateChange(device, status, newState)
            }

            override fun onCharacteristicReadRequest(
                device: BluetoothDevice?,
                requestId: Int,
                offset: Int,
                characteristic: BluetoothGattCharacteristic?
            ) {
                gattServer.sendResponse(
                    device,
                    requestId,
                    BluetoothGatt.GATT_SUCCESS,
                    0,
                    randomBytes(8)
                )
                super.onCharacteristicReadRequest(device, requestId, offset, characteristic)
            }

            override fun onCharacteristicWriteRequest(
                device: BluetoothDevice,
                requestId: Int,
                characteristic: BluetoothGattCharacteristic,
                preparedWrite: Boolean,
                responseNeeded: Boolean,
                offset: Int,
                value: ByteArray
            ) {
                if (responseNeeded) {
                    gattServer.sendResponse(
                        device,
                        requestId,
                        BluetoothGatt.GATT_SUCCESS,
                        0,
                        null
                    )
                }


//            val target = deviceMap[characteristic.uuid]!!
//
//            Log.d("value", value.toString())
//            Log.d("authCode", authCode.toString())
//
//            if (value.contentEquals(authCode)) {
//                EventBus.getDefault().post(DisconnectEvent(target))
//
//                characteristic.setValue("")
//                gattServer.notifyCharacteristicChanged(device, characteristic, false)
//            } else {
//                gattServer.cancelConnection(device)
//            }
                super.onCharacteristicWriteRequest(
                    device,
                    requestId,
                    characteristic,
                    preparedWrite,
                    responseNeeded,
                    offset,
                    value
                )
            }

            override fun onDescriptorWriteRequest(
                device: BluetoothDevice,
                requestId: Int,
                descriptor: BluetoothGattDescriptor,
                preparedWrite: Boolean,
                responseNeeded: Boolean,
                offset: Int,
                value: ByteArray
            ) {
                if (responseNeeded) {
                    gattServer.sendResponse(
                        device,
                        requestId,
                        BluetoothGatt.GATT_SUCCESS,
                        0,
                        null
                    )
                }
                super.onDescriptorWriteRequest(
                    device,
                    requestId,
                    descriptor,
                    preparedWrite,
                    responseNeeded,
                    offset,
                    value
                )
            }
        }

    private val gattService =
        BluetoothGattService(ServiceUUID, BluetoothGattService.SERVICE_TYPE_PRIMARY).apply {
            addCharacteristic(
                BluetoothGattCharacteristic(
                    ServiceUUID,
                    BluetoothGattCharacteristic.PROPERTY_READ or BluetoothGattCharacteristic.PROPERTY_WRITE or BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                    BluetoothGattCharacteristic.PERMISSION_READ or BluetoothGattCharacteristic.PERMISSION_WRITE
                ).apply {
                    addDescriptor(
                        BluetoothGattDescriptor(
                            DescriptorUUID,
                            BluetoothGattDescriptor.PERMISSION_WRITE
                        )
                    )
                }
            )
        }

    fun start(action: Action) {
        stop()
        val bluetoothManager =
            context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager

        bluetoothLeAdvertiser = bluetoothManager.adapter.bluetoothLeAdvertiser

        gattServer = bluetoothManager.openGattServer(context, gattServerCallback)

        gattServer.addService(gattService)

        val versionByte = VersionByte(VersionCode, action)
        val currentPeriod = System.currentTimeMillis() / Constants.OTP_TIMEOUT
        Log.d("Period", currentPeriod.toString())
        val authByte = otpGenerater(
            preferences.key,
            ByteBuffer.wrap(ByteArray(Long.SIZE_BYTES))
                .putLong(currentPeriod)
                .array()
        )

        val advertiseData = AdvertiseData.Builder().run {
            setIncludeDeviceName(false)
            setIncludeTxPowerLevel(false)
            addServiceUuid(ParcelUuid(ServiceUUID))
            addManufacturerData(
                ManufacturerID,
                versionByte.byteArray() + authByte
            )
            build()
        }

        bluetoothLeAdvertiser.startAdvertising(
            advertiseSettings,
            advertiseData,
            advertiseCallback
        )
    }

    fun stop() {
        if (isAdvertise.value == true) {
            bluetoothLeAdvertiser.stopAdvertising(advertiseCallback)
            gattServer.clearServices()
            gattServer.close()
        }
        isAdvertise.postValue(false)
    }
}
