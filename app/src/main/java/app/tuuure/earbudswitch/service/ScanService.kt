package app.tuuure.earbudswitch.service

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.os.ParcelUuid
import app.tuuure.earbudswitch.data.Earbud
import no.nordicsemi.android.support.v18.scanner.*
import java.util.*

class ScanService : Service() {
    companion object {
        private const val SCAN_NO_ERROR = 0
        private const val SCAN_FAILED_OUT_OF_HARDWARE_RESOURCES = 5
        private const val SCAN_FAILED_SCANNING_TOO_FREQUENTLY = 6

        @JvmStatic
        fun codeStrize(code: Int): String = when (code) {
            SCAN_NO_ERROR -> "SCAN_NO_ERROR"
            android.bluetooth.le.ScanCallback.SCAN_FAILED_ALREADY_STARTED -> "SCAN_FAILED_ALREADY_STARTED"
            android.bluetooth.le.ScanCallback.SCAN_FAILED_APPLICATION_REGISTRATION_FAILED -> "SCAN_FAILED_APPLICATION_REGISTRATION_FAILED"
            android.bluetooth.le.ScanCallback.SCAN_FAILED_INTERNAL_ERROR -> "SCAN_FAILED_INTERNAL_ERROR"
            android.bluetooth.le.ScanCallback.SCAN_FAILED_FEATURE_UNSUPPORTED -> "SCAN_FAILED_FEATURE_UNSUPPORTED"
            SCAN_FAILED_OUT_OF_HARDWARE_RESOURCES -> "SCAN_FAILED_OUT_OF_HARDWARE_RESOURCES"
            SCAN_FAILED_SCANNING_TOO_FREQUENTLY -> "SCAN_FAILED_SCANNING_TOO_FREQUENTLY"
            else -> "SCAN_UNKNOWN_RESULT_CODE, $code"
        }
    }

    private val binder: LocalBinder = LocalBinder()
    private var devices: MutableMap<String, String> = mutableMapOf()

    private val scanner = BluetoothLeScannerCompat.getScanner()
    private val settings: ScanSettings = ScanSettings.Builder().apply {
        setCallbackType(ScanSettings.CALLBACK_TYPE_FIRST_MATCH or ScanSettings.CALLBACK_TYPE_MATCH_LOST)
        setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE)
        setScanMode(ScanSettings.SCAN_MODE_BALANCED)
        setUseHardwareBatchingIfSupported(true)
        setUseHardwareCallbackTypesIfSupported(true)
        setUseHardwareFilteringIfSupported(true)
    }.build()
    private val filters: MutableList<ScanFilter> = ArrayList()

    private fun updateFilter(earbuds: LinkedHashSet<Earbud>) {
        scanner.stopScan(scanCallback)

        devices.clear()
        filters.clear()
        for (item in earbuds) {
            devices[item.hashed] = item.address
            ScanFilter.Builder().also {
                it.setServiceUuid(ParcelUuid(UUID.fromString(item.hashed)))
                filters.add(it.build())
            }
        }

        scanner.startScan(filters, settings, scanCallback)
    }

    override fun onBind(intent: Intent): IBinder? {
        return binder
    }

    override fun onUnbind(intent: Intent?): Boolean {
        scanner.stopScan(scanCallback)
        return super.onUnbind(intent)
    }

    private val scanCallback: ScanCallback = object : ScanCallback() {
        override fun onScanFailed(errorCode: Int) {
            listener.onScanFailed(errorCode, codeStrize(errorCode))
            super.onScanFailed(errorCode)
        }

        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val resultDevices : MutableSet<String> = mutableSetOf()
            result.scanRecord?.serviceUuids?.forEach {
                val hashed = it.uuid.toString()
                if (devices.keys.contains(hashed)) {
                    resultDevices.add(devices[hashed].toString())
                }
            }
            listener.onScanResult(callbackType, result.device.address, resultDevices)
            super.onScanResult(callbackType, result)
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>) {
            val batchResults: MutableMap<String, String> = mutableMapOf()
            for (result in results) {
                result.scanRecord?.serviceUuids?.forEach {
                    val hashed = it.uuid.toString()
                    if (devices.keys.contains(hashed)) {
                        batchResults[hashed] = devices[hashed].toString()
                    }
                }
            }
            listener.onBatchScanResults(batchResults)
            super.onBatchScanResults(results)
        }
    }

    private lateinit var listener: ScanListener

    interface ScanListener {
        fun onScanFailed(errorCode: Int, errorInfo: String)
        fun onBatchScanResults(results: MutableMap<String, String>)
        fun onScanResult(callbackType: Int, server :String, devices:MutableSet<String>)
    }

    inner class LocalBinder : Binder() {
        fun setDevices(earbuds: LinkedHashSet<Earbud>) {
            updateFilter(earbuds)
        }

        fun addListener(listener: ScanListener) {
            this@ScanService.listener = listener
        }
    }
}