package app.tuuure.earbudswitch.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.ParcelUuid
import android.util.Log
import android.widget.Toast
import app.tuuure.earbudswitch.Constants
import app.tuuure.earbudswitch.Constants.Companion.VersionByte
import app.tuuure.earbudswitch.ble.BleScanner
import app.tuuure.earbudswitch.data.Preferences
import app.tuuure.earbudswitch.utils.CryptoConvertUtils.Companion.otpsGenerater
import no.nordicsemi.android.support.v18.scanner.BluetoothLeScannerCompat
import no.nordicsemi.android.support.v18.scanner.ScanResult
import org.koin.java.KoinJavaComponent.inject
import java.nio.ByteBuffer
import java.util.*

class BackgroundScanReceiver : BroadcastReceiver() {
    companion object {
        const val ACTION_BG_SCAN = "app.tuuure.earbudswitch.action.BG_SCAN"
    }

    private val preferences: Preferences by inject(Preferences::class.java)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_BG_SCAN) {
            val bleCallbackType =
                intent.getIntExtra(BluetoothLeScannerCompat.EXTRA_CALLBACK_TYPE, -1)
            val bleErrorCode =
                intent.getIntExtra(
                    BluetoothLeScannerCompat.EXTRA_ERROR_CODE,
                    BleScanner.SCAN_NO_ERROR
                )

            if (bleErrorCode != BleScanner.SCAN_NO_ERROR) {
                Toast.makeText(context, BleScanner.codeStrize(bleErrorCode), Toast.LENGTH_SHORT)
                    .show()
            } else {
                val scanResults: ArrayList<ScanResult> =
                    intent.getParcelableArrayListExtra(
                        BluetoothLeScannerCompat.EXTRA_LIST_SCAN_RESULT
                    ) ?: return
                for (result in scanResults) {
                    val scanRecord = result.scanRecord ?: return
                    if (scanRecord.serviceUuids?.contains(ParcelUuid(Constants.ServiceUUID)) != true)
                        return
                    val manufacturerData =
                        scanRecord.getManufacturerSpecificData(Constants.ManufacturerID) ?: return

                    val versionByte = VersionByte(manufacturerData.copyOfRange(0, 2))

                    Log.d("Version", "${versionByte.code} ${versionByte.action}")

                    val authByte = manufacturerData.copyOfRange(2, manufacturerData.size)

                    Log.d("TAG", String(authByte))

                    val periodCount = System.currentTimeMillis() / Constants.OTP_TIMEOUT
                    Log.d("Period", periodCount.toString())

                    for (byte in otpsGenerater(
                        preferences.key,
                        ByteBuffer.wrap(ByteArray(Long.SIZE_BYTES))
                            .putLong(periodCount - 1)
                            .array(),
                        ByteBuffer.wrap(ByteArray(Long.SIZE_BYTES))
                            .putLong(periodCount)
                            .array(),
                        ByteBuffer.wrap(ByteArray(Long.SIZE_BYTES))
                            .putLong(periodCount + 1)
                            .array()
                    )) {
                        if (Arrays.equals(byte, authByte)) {
                            Log.d("TAG", "got")
                            break
                        }
                    }
                }
//                when (bleCallbackType) {
//                    ScanSettings.CALLBACK_TYPE_FIRST_MATCH -> Preferences.getInstance(context!!)
//                        .putRecord(Preferences.RECORD_TYPE.TargetFound, "address: $address")
//                    ScanSettings.CALLBACK_TYPE_MATCH_LOST -> Preferences.getInstance(context!!)
//                        .putRecord(Preferences.RECORD_TYPE.TargetLost, "address: $address")
//                    else -> Preferences.getInstance(context!!)
//                        .putRecord(
//                            Preferences.RECORD_TYPE.BleError,
//                            "unknown, ErrorCode: $bleErrorCode, CallbackType: $bleCallbackType"
//                        )
//                }
            }
        }
    }
}