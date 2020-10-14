package app.tuuure.earbudswitch.data.db

import android.bluetooth.BluetoothDevice
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import app.tuuure.earbudswitch.data.Earbud

@Entity(tableName = "earbuds")
data class DbRecord(@ColumnInfo(name = "name") val name: String, @PrimaryKey val address: String) {

    constructor(device: BluetoothDevice) : this(device.name, device.address)

    constructor(earbud: Earbud) : this(earbud.name, earbud.address) {
        this.server = earbud.server
        this.isPersist = earbud.isPersist
        this.isAllowed = earbud.isAllowed
        this.isBlocked = earbud.isBlocked

        this.isA2dpConnecting = earbud.isA2dpConnecting
        this.isHeadsetConnecting = earbud.isHeadsetConnecting
        this.isA2dpConnected = earbud.isA2dpConnected
        this.isHeadsetConnected = earbud.isHeadsetConnected
    }

    @ColumnInfo(name = "server")
    var server: String = ""

    @ColumnInfo(name = "isPersist")
    var isPersist: Boolean = false

    @ColumnInfo(name = "isAllowed")
    var isAllowed: Boolean = true

    @ColumnInfo(name = "isBlocked")
    var isBlocked: Boolean = false

    var isA2dpConnecting: Boolean = false
    var isHeadsetConnecting: Boolean = false
    var isA2dpConnected: Boolean = false
    var isHeadsetConnected: Boolean = false
}