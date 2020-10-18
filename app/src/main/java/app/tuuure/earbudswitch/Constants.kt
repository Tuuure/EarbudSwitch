package app.tuuure.earbudswitch

import java.util.*

class Constants {
    companion object {
        const val ManufacturerID = 0xEB5
        const val VersionCode = 101

        const val ADVERTISE_TIMEOUT = 1000 * 5
        const val OTP_TIMEOUT = 1000 * 60

        val ServiceUUID: UUID = UUID.fromString("248c1b0b-cb74-394b-eb4a-08030c4a6847")
        val DescriptorUUID: UUID = UUID.fromString("00001181-0000-1000-8000-00805f9b34fb")

        val AudioOutUUIDs = arrayListOf(
            UUID.fromString("0000110B-0000-1000-8000-00805f9b34fb"), //AudioSink
            UUID.fromString("00001108-0000-1000-8000-00805f9b34fb"), //Headset
            UUID.fromString("00001112-0000-1000-8000-00805f9b34fb"), //HeadsetAG
            UUID.fromString("00001131-0000-1000-8000-00805f9b34fb") //HeadsetHS
        )

        enum class State {
            FOCUS_GAIN,
            FOCUS_LOST,
            ADVERTISE,
            FOCUS_WAIT,
            SCANNING,
            ERROR
        }

        enum class Action {
            ASK,
            CALL
        }

        //  0 0 0 0  0 0 0 0  0 0 0 0  0 0 0 0
        // |   |                     |        |
        //  保留         版本            指令

        class VersionByte {
            var code: Int
            var action: Action

            constructor(code: Int, action: Action) {
                this.code = code
                this.action = action
            }

            constructor(bytes: ByteArray) {
                if (bytes.size != 2) {
                    throw Error("Wrong VersionByte Size")
                } else {
                    code = bytes[0].toInt().shl(4) + bytes[1].toInt().shr(4)
                    action = Action.values()[bytes[1].toInt() and 0xF]
                }
            }

            fun byteArray() =
                byteArrayOf(
                    (code.shr(4) and 0xFF).toByte(),
                    (code.shl(4) and 0xF0 or action.ordinal).toByte()
                )
        }
    }
}