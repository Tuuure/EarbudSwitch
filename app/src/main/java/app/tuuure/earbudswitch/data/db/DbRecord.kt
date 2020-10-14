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
        this.isAllowed = earbud.isAllowed
        this.isBlocked = earbud.isBlocked
    }

    @ColumnInfo(name = "isAllowed")
    var isAllowed: Boolean = true

    @ColumnInfo(name = "isBlocked")
    var isBlocked: Boolean = false
}