package app.tuuure.earbudswitch.data

import android.bluetooth.BluetoothDevice
import app.tuuure.earbudswitch.data.db.DbRecord
import app.tuuure.earbudswitch.utils.CryptoConvertUtils.Companion.bytesToUUID
import app.tuuure.earbudswitch.utils.CryptoConvertUtils.Companion.md5code32

data class Earbud(val name: String, val address: String) {
    constructor(record: DbRecord) : this(record.name, record.address) {
        this.server = record.server
        this.isPersist = record.isPersist
        this.isAllowed = record.isAllowed
        this.isBlocked = record.isBlocked

        this.isA2dpConnecting = record.isA2dpConnecting
        this.isHeadsetConnecting = record.isHeadsetConnecting
        this.isA2dpConnected = record.isA2dpConnected
        this.isHeadsetConnected = record.isHeadsetConnected
    }

    constructor(device: BluetoothDevice) : this(device.name, device.address)

    var server: String = ""
    var isPersist: Boolean = false
    var isAllowed: Boolean = true
    var isBlocked: Boolean = false

    var hashed: String = bytesToUUID(md5code32(address)).toString()
        private set

    var isA2dpConnecting: Boolean = false
    var isHeadsetConnecting: Boolean = false
    var isA2dpConnected: Boolean = false
    var isHeadsetConnected: Boolean = false

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as Earbud
        return address == other.address
    }

    override fun hashCode(): Int = address.hashCode()
}