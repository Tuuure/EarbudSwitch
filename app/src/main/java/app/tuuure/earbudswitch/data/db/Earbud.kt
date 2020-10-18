package app.tuuure.earbudswitch.data.db

import android.bluetooth.BluetoothDevice
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "earbuds")
data class Earbud(@ColumnInfo(name = "name") val name: String, @PrimaryKey val address: String) {

    constructor(device: BluetoothDevice) : this(device.name, device.address)

    @ColumnInfo(name = "isAllowed")
    var isAllowed: Boolean = true

    @ColumnInfo(name = "isBlocked")
    var isBlocked: Boolean = false

    @ColumnInfo(name = "isA2dpConnected")
    var isA2dpConnected: Boolean = false

    @ColumnInfo(name = "isHeadSetConnected")
    var isHeadSetConnected: Boolean = false

    fun isConnected(): Boolean = isA2dpConnected || isHeadSetConnected
}