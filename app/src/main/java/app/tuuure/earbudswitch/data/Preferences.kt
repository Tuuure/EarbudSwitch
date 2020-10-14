package app.tuuure.earbudswitch.data

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class Preferences constructor(context: Context) {
    companion object {
        private const val SP_SETTINGS_NAME = "Settings"
        private const val KEY_NAME = "Key"
        private const val RESTRICT_MODE_NAME = "RestrictMode"
        private const val BACKGROUND_SCAN_NAME = "BackgroundScan"
        private const val REROUTE_CALLS_NAME = "RerouteCalls"
        private const val POPUP_WINDOW_NAME = "PopupWindow"
        private const val SIMULTANEOUS_SWITCH_NAME = "SimultaneousSwitch"
    }

    private val sp: SharedPreferences = context.applicationContext.getSharedPreferences(
        SP_SETTINGS_NAME,
        MODE_PRIVATE
    )

    var key: String = ""
        get() = sp.getString(KEY_NAME, "") as String
        set(value) {
            CoroutineScope(Dispatchers.IO).launch {
                sp.edit().putString(KEY_NAME, value).apply()
            }
            field = value
        }

    enum class RestrictMode {
        ALLOW,
        BLOCK
    }

    var restrictMode: RestrictMode = RestrictMode.BLOCK
        get() =
            try {
                RestrictMode.valueOf(sp.getString(RESTRICT_MODE_NAME, "").toString())
            } catch (e: IllegalArgumentException) {
                RestrictMode.BLOCK
            }
        set(value) {
            CoroutineScope(Dispatchers.IO).launch {
                sp.edit().putString(RESTRICT_MODE_NAME, value.toString()).apply()
            }
            field = value
        }

    var backgroundScan: Boolean = false
        get() = sp.getBoolean(BACKGROUND_SCAN_NAME, false)
        set(value) {
            CoroutineScope(Dispatchers.IO).launch {
                sp.edit().putBoolean(BACKGROUND_SCAN_NAME, value).apply()
            }
            field = value
        }

    var rerouteCalls: Boolean = false
        get() = sp.getBoolean(REROUTE_CALLS_NAME, false)
        set(value) {
            CoroutineScope(Dispatchers.IO).launch {
                sp.edit().putBoolean(REROUTE_CALLS_NAME, value).apply()
            }
            field = value
        }

    var popupWindow: Boolean = false
        get() = sp.getBoolean(POPUP_WINDOW_NAME, false)
        set(value) {
            CoroutineScope(Dispatchers.IO).launch {
                sp.edit().putBoolean(POPUP_WINDOW_NAME, value).apply()
            }
            field = value
        }

    var simultaneousSwitch: Boolean = false
        get() = sp.getBoolean(SIMULTANEOUS_SWITCH_NAME, false)
        set(value) {
            CoroutineScope(Dispatchers.IO).launch {
                sp.edit().putBoolean(SIMULTANEOUS_SWITCH_NAME, value).apply()
            }
            field = value
        }

}
