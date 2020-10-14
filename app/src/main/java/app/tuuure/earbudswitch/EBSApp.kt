package app.tuuure.earbudswitch

import android.app.Application
import app.tuuure.earbudswitch.ui.activity.DialogActivity
import app.tuuure.earbudswitch.data.Preferences
import app.tuuure.earbudswitch.data.db.EarbudsDatabase
import app.tuuure.earbudswitch.service.AdvertiseService
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.meta.SimpleSubscriberInfo
import org.greenrobot.eventbus.meta.SubscriberMethodInfo
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.dsl.module


class EBSApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            // use AndroidLogger as Koin Logger - default Level.INFO
            androidLogger()
            // use the Android context given there
            androidContext(this@EBSApp)
//            // load properties from assets/koin.properties file
//            androidFileProperties()
            // module list
            modules(myModules)
        }

        EventBus.builder().addIndex {
            val infos = arrayOf(
                SubscriberMethodInfo("onDisconnectEvent", DisconnectEvent::class.java),
                SubscriberMethodInfo("onCancelAdvertise", CancelAdvertiseEvent::class.java)
            )
            SimpleSubscriberInfo(AdvertiseService::class.java, true, infos)
        }

        EventBus.builder().addIndex {
            val infos = arrayOf(
                SubscriberMethodInfo("onScanResult", ScanResultEvent::class.java),
                SubscriberMethodInfo("connectGatt", ConnectGattEvent::class.java),
                SubscriberMethodInfo("onSetFreshEvent", RefreshEvent::class.java)
            )
            SimpleSubscriberInfo(DialogActivity::class.java, true, infos)
        }
    }
}

val myModules = module {
    single { Preferences(get()) }
    single { EarbudsDatabase.buildDatabase(get()) }
}