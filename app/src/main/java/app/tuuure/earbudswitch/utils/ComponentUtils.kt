package app.tuuure.earbudswitch.utils

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import app.tuuure.earbudswitch.receiver.BackgroundScanReceiver
import app.tuuure.earbudswitch.receiver.ConnectionChangeReceiver
import app.tuuure.earbudswitch.service.VigilService

class ComponentUtils {
    companion object {
        @JvmStatic
        fun setEnableSettings(context: Context, enabled: Boolean) {
            setEnableSettings(
                context,
                if (enabled) PackageManager.COMPONENT_ENABLED_STATE_ENABLED else PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                VigilService::class.java,
                //CallReceiver::class.java,
                ConnectionChangeReceiver::class.java,
                BackgroundScanReceiver::class.java
            )
        }

        @JvmStatic
        fun setEnableSettings(context: Context, state: Int, vararg clazz: Class<*>) {
            if (clazz.isNullOrEmpty())
                return

            for (item in clazz) {
                context.packageManager.setComponentEnabledSetting(
                    ComponentName(context, item),
                    state,
                    PackageManager.DONT_KILL_APP
                )
            }
        }
    }
}