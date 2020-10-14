package app.tuuure.earbudswitch.data

import android.bluetooth.BluetoothDevice
import app.tuuure.earbudswitch.data.db.DbRecord
import app.tuuure.earbudswitch.utils.CryptoConvertUtils.Companion.bytesToUUID
import app.tuuure.earbudswitch.utils.CryptoConvertUtils.Companion.md5code32

data class Earbud(val name: String, val address: String) {

    constructor(device: BluetoothDevice) : this(device.name, device.address)

    constructor(record: DbRecord) : this(record.name, record.address) {
        this.isAllowed = record.isAllowed
        this.isBlocked = record.isBlocked
    }

    var isAllowed: Boolean = true
    var isBlocked: Boolean = false

    var hashed: String = bytesToUUID(md5code32(address)).toString()
        get() = bytesToUUID(md5code32(address)).toString()
        private set

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as Earbud
        return address == other.address
    }

    override fun hashCode(): Int = address.hashCode()
}