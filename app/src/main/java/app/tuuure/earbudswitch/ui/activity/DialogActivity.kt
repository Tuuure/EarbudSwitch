package app.tuuure.earbudswitch.ui.activity

import android.Manifest
import android.bluetooth.*
import android.content.*
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.location.LocationManager
import android.os.Bundle
import android.os.IBinder
import android.provider.Settings
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.location.LocationManagerCompat
import androidx.recyclerview.widget.LinearLayoutManager
import app.tuuure.earbudswitch.R
import app.tuuure.earbudswitch.data.Earbud
import app.tuuure.earbudswitch.data.Preferences
import app.tuuure.earbudswitch.data.db.DbRecord
import app.tuuure.earbudswitch.data.db.EarbudsDatabase
import app.tuuure.earbudswitch.service.ScanService
import app.tuuure.earbudswitch.service.ScanService.LocalBinder
import app.tuuure.earbudswitch.ui.adapter.ScanListAdapter
import app.tuuure.earbudswitch.utils.ComponentUtils
import app.tuuure.earbudswitch.utils.CryptoConvertUtils.Companion.bytesToUUID
import app.tuuure.earbudswitch.utils.CryptoConvertUtils.Companion.randomBytes
import kotlinx.android.synthetic.main.activity_dialog.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import no.nordicsemi.android.support.v18.scanner.ScanSettings
import org.koin.android.ext.android.inject


class DialogActivity : AppCompatActivity() {
    companion object {
        const val REQUEST_CODE_WELCOME = 9
    }

    private val preferences: Preferences by inject()
    private lateinit var adapter: ScanListAdapter
    private val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

    private var isBluetoothEnabled = false
        set(value) {
            if (field != value) {
                field = value
                if (this::menuWarning.isInitialized)
                    menuWarning.isVisible = !isBluetoothEnabled || !isLocationEnabled
            }
        }
    private var isLocationEnabled = false
        set(value) {
            if (field != value) {
                field = value
                if (this::menuWarning.isInitialized)
                    menuWarning.isVisible = !isBluetoothEnabled || !isLocationEnabled
            }
        }

    private val receiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action ?: return
            when (action) {
                BluetoothAdapter.ACTION_STATE_CHANGED -> {
                    val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1)
                    when (state) {
                        BluetoothAdapter.STATE_ON -> {
                            loadDevices()
                            isBluetoothEnabled = true
                        }
                        else -> {
                            isBluetoothEnabled = false
                        }
                    }
                }
                BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED, BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED -> {
                    val state = intent.getIntExtra(BluetoothProfile.EXTRA_STATE, -1)
                    val device: BluetoothDevice =
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)!!
                    adapter.setStatus(device, action, state)
                }
                LocationManager.PROVIDERS_CHANGED_ACTION -> {
                    isLocationEnabled =
                        LocationManagerCompat.isLocationEnabled(context.getSystemService(Context.LOCATION_SERVICE) as LocationManager)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dialog)

        setSupportActionBar(tb_dialog)
        window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        adapter = ScanListAdapter(this)
        rv_dialog.layoutManager = LinearLayoutManager(this)
        rv_dialog.adapter = adapter
    }


    private fun setup(): Boolean {
        if (preferences.key.isEmpty()
            || checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_DENIED
            || checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED
        ) {
            ComponentUtils.setEnableSettings(this, false)
            Intent(this, IntroActivity::class.java).also {
                startActivityForResult(it, REQUEST_CODE_WELCOME)
            }
            return false
        } else {
            ComponentUtils.setEnableSettings(this, true)
            return true
        }
    }

    private var isBound = false
        set(value) {
            field = value
        }
    private lateinit var binder: ScanService.LocalBinder
    private val listener: ScanService.ScanListener = object : ScanService.ScanListener {
        override fun onScanFailed(errorCode: Int, errorInfo: String) {
            Toast.makeText(this@DialogActivity, errorInfo, Toast.LENGTH_SHORT).show()
        }

        override fun onBatchScanResults(results: MutableMap<String, String>) {
        }

        override fun onScanResult(callbackType: Int, server: String, devices: MutableSet<String>) {
            when (callbackType) {
                ScanSettings.CALLBACK_TYPE_FIRST_MATCH -> adapter.setServer(server, devices)
                ScanSettings.CALLBACK_TYPE_MATCH_LOST -> adapter.removeServer(server, devices)
            }
        }
    }

    private val mConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            binder = service as LocalBinder

            binder.addListener(listener)
            binder.setDevices(adapter.data)
            isBound = true
        }

        override fun onServiceDisconnected(className: ComponentName) {
            isBound = false
        }
    }

    fun loadDevices() {
        CoroutineScope(Dispatchers.IO).launch {
            val database: EarbudsDatabase by inject()
            val deviceList: LinkedHashSet<Earbud> = LinkedHashSet()
            database.dbDao().getAll().forEach { record ->
                Earbud(record).also {
                    if (!deviceList.contains(it)) {
                        deviceList.add(it)
                    }
                }
            }
            if (bluetoothAdapter.isEnabled) {
                bluetoothAdapter.bondedDevices.filterNot {
                    it.name.isNullOrEmpty()
                            || it.bluetoothClass.majorDeviceClass != BluetoothClass.Device.Major.AUDIO_VIDEO
                }.forEach { device ->
                    Earbud(device).also {
                        if (!deviceList.contains(it)) {
                            database.dbDao().insert(DbRecord(it))
                            deviceList.add(it)
                        }
                    }
                }
            }
            withContext(Dispatchers.Main) {
                adapter.data = deviceList
                if (bluetoothAdapter.isEnabled) {
                    if (isBound) {
                        binder.setDevices(adapter.data)
                    } else {
                        bindService(
                            Intent(this@DialogActivity, ScanService::class.java),
                            mConnection,
                            Context.BIND_AUTO_CREATE
                        )
                    }
                }
            }
        }
    }

    private lateinit var menuWarning: MenuItem
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.menu_dialog, menu)
        val menuSettings = menu!!.findItem(R.id.app_bar_settings)
        menuSettings.setOnMenuItemClickListener {
            Intent(this, SettingsActivity::class.java).also {
                startActivity(it)
            }
            return@setOnMenuItemClickListener true
        }

        menuWarning = menu.findItem(R.id.app_bar_warning)

        if (this::menuWarning.isInitialized)
            menuWarning.isVisible = !isBluetoothEnabled || !isLocationEnabled

        menuWarning.setOnMenuItemClickListener {
            AlertDialog.Builder(this).apply {
                setTitle(R.string.dialog_potential_issues_title)
                val array: ArrayList<CharSequence> = arrayListOf()
                if (!isBluetoothEnabled)
                    array.add(getText(R.string.text_bluetooth_diable))
                if (!isLocationEnabled)
                    array.add(getText(R.string.text_location_diable))

                setItems(
                    array.toTypedArray()
                ) { _, i ->
                    when (array[i]) {
                        getText(R.string.text_bluetooth_diable) -> {
                            startActivity(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE));
                        }
                        getText(R.string.text_location_diable) -> {
                            startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                        }
                    }
                }
                setPositiveButton(R.string.dialog_button_back) { _, _ -> }
            }.show()

            return@setOnMenuItemClickListener true
        }
        return true
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK && requestCode == REQUEST_CODE_WELCOME) {
            preferences.key = bytesToUUID(randomBytes(16)).toString()

            Intent(this@DialogActivity, SettingsActivity::class.java).apply {
                startActivity(intent)
            }
        } else {
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        if (setup()) {
            loadDevices()

            isLocationEnabled =
                LocationManagerCompat.isLocationEnabled(getSystemService(Context.LOCATION_SERVICE) as LocationManager)
            isBluetoothEnabled = bluetoothAdapter.isEnabled

            registerReceiver(receiver, IntentFilter().apply {
                addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
                addAction(LocationManager.PROVIDERS_CHANGED_ACTION)
                addAction(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED)
                addAction(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED)
            })
        }
    }

    override fun onPause() {
        try {
            isBound = false
            unbindService(mConnection)
        } catch (ignored: IllegalArgumentException) {
        }
        try {
            unregisterReceiver(receiver)
        } catch (ignored: IllegalArgumentException) {
        }
        super.onPause()
    }
}