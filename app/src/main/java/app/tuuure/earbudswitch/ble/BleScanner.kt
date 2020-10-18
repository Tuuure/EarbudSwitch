package app.tuuure.earbudswitch.ble

import android.app.PendingIntent
import android.bluetooth.le.ScanCallback
import android.content.Context
import android.content.Intent
import android.os.ParcelUuid
import android.util.Log
import app.tuuure.earbudswitch.Constants
import app.tuuure.earbudswitch.receiver.BackgroundScanReceiver
import no.nordicsemi.android.support.v18.scanner.BluetoothLeScannerCompat
import no.nordicsemi.android.support.v18.scanner.ScanFilter
import no.nordicsemi.android.support.v18.scanner.ScanSettings


class BleScanner {
    companion object {
        private const val REQUEST_CODE = 1
        const val SCAN_NO_ERROR = 0
        const val SCAN_FAILED_OUT_OF_HARDWARE_RESOURCES = 5
        const val SCAN_FAILED_SCANNING_TOO_FREQUENTLY = 6

        @JvmStatic
        fun codeStrize(code: Int): String = when (code) {
            SCAN_NO_ERROR -> "SCAN_NO_ERROR"
            ScanCallback.SCAN_FAILED_ALREADY_STARTED -> "SCAN_FAILED_ALREADY_STARTED"
            ScanCallback.SCAN_FAILED_APPLICATION_REGISTRATION_FAILED -> "SCAN_FAILED_APPLICATION_REGISTRATION_FAILED"
            ScanCallback.SCAN_FAILED_INTERNAL_ERROR -> "SCAN_FAILED_INTERNAL_ERROR"
            ScanCallback.SCAN_FAILED_FEATURE_UNSUPPORTED -> "SCAN_FAILED_FEATURE_UNSUPPORTED"
            SCAN_FAILED_OUT_OF_HARDWARE_RESOURCES -> "SCAN_FAILED_OUT_OF_HARDWARE_RESOURCES"
            SCAN_FAILED_SCANNING_TOO_FREQUENTLY -> "SCAN_FAILED_SCANNING_TOO_FREQUENTLY"
            else -> "SCAN_UNKNOWN_RESULT_CODE, $code"
        }

        @JvmStatic
        fun getPendingIntent(context: Context): PendingIntent =
            PendingIntent.getBroadcast(
                context,
                REQUEST_CODE,
                Intent(context, BackgroundScanReceiver::class.java).apply {
                    action = BackgroundScanReceiver.ACTION_BG_SCAN
                },
                PendingIntent.FLAG_UPDATE_CURRENT
            )

        @JvmStatic
        fun startScan(context: Context) {
            Log.d("TAG", "startScan")
            val appContext = context.applicationContext
            BluetoothLeScannerCompat.getScanner().startScan(
                arrayListOf(
                    ScanFilter.Builder().run {
                        setServiceUuid(ParcelUuid(Constants.ServiceUUID))
                        setManufacturerData(Constants.ManufacturerID, null)
                        build()
                    }
                ),
                ScanSettings.Builder().run {
                    setCallbackType(ScanSettings.CALLBACK_TYPE_FIRST_MATCH or ScanSettings.CALLBACK_TYPE_MATCH_LOST)
                    setNumOfMatches(ScanSettings.MATCH_NUM_ONE_ADVERTISEMENT)
                    setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE)
                    setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
                    build()
                },
                appContext,
                getPendingIntent(appContext)
            )
        }

        @JvmStatic
        fun stopScan(context: Context) {
            Log.d("TAG", "stopScan")
            val appContext = context.applicationContext
            BluetoothLeScannerCompat.getScanner().stopScan(appContext, getPendingIntent(appContext))
        }
    }
}