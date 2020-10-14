package app.tuuure.earbudswitch.ui.activity

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothClass
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import app.tuuure.earbudswitch.R
import app.tuuure.earbudswitch.data.Preferences
import app.tuuure.earbudswitch.data.db.DbRecord
import app.tuuure.earbudswitch.data.db.EarbudsDatabase
import app.tuuure.earbudswitch.ui.adapter.FilterListAdapter
import app.tuuure.earbudswitch.utils.ComponentUtils
import app.tuuure.earbudswitch.utils.CryptoConvertUtils.Companion.bytesToUUID
import app.tuuure.earbudswitch.utils.CryptoConvertUtils.Companion.randomBytes
import app.tuuure.earbudswitch.utils.QRKeyUtils
import app.tuuure.earbudswitch.utils.QRKeyUtils.Companion.createQRCode
import com.king.zxing.CaptureActivity
import com.king.zxing.Intents
import kotlinx.android.synthetic.main.activity_settings.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject
import java.util.*


class SettingsActivity : AppCompatActivity() {
    companion object {
        const val REQUEST_CODE_WELCOME = 9
        const val KEY_EXTRA = "keyExtra"
        private const val REQUEST_CODE_SCAN = 12
    }

    private val preferences: Preferences by inject()

    private var key = preferences.key
        set(value) {
            preferences.key = value
            field = value
        }

    private lateinit var adapter: FilterListAdapter

    private fun updateList() {
        CoroutineScope(Dispatchers.IO).launch {
            adapter.restrictMode = preferences.restrictMode

            val database: EarbudsDatabase by inject()
            val deviceList: LinkedList<DbRecord> = LinkedList()

            database.dbDao().getAll().sortedBy {
                when (preferences.restrictMode) {
                    Preferences.RestrictMode.ALLOW -> it.isAllowed
                    Preferences.RestrictMode.BLOCK -> it.isBlocked
                }
            }.also { list ->
                deviceList.addAll(list)
            }

            BluetoothAdapter.getDefaultAdapter()?.also { bluetoothAdapter ->
                if (bluetoothAdapter.isEnabled) {
                    bluetoothAdapter.bondedDevices.filterNot {
                        it.name.isNullOrEmpty()
                                || it.bluetoothClass.majorDeviceClass != BluetoothClass.Device.Major.AUDIO_VIDEO
                    }.forEach { device ->
                        DbRecord(device).also {
                            if (!deviceList.contains(it)) {
                                database.dbDao().insert(it)
                                deviceList.add(it)
                            }
                        }
                    }
                }
            }
            withContext(Dispatchers.Main) {
                adapter.data = deviceList
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        initView()
        setSupportActionBar(toolbar)
    }

    private fun setup(): Boolean {
        if (key.isEmpty()
            || checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_DENIED
            || checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED
        ) {
            ComponentUtils.setEnableSettings(this, false)
            Intent(this, IntroActivity::class.java).also {
                startActivityForResult(it, DialogActivity.REQUEST_CODE_WELCOME)
            }
            return false
        } else {
            ComponentUtils.setEnableSettings(this, true)
            return true
        }
    }

    override fun onResume() {
        if (setup()) {
            updateQRCode()
        }
        super.onResume()
    }

    private fun initView() {
        filterModeSpinner.onItemSelectedListener = object : OnItemSelectedListener {
            override fun onItemSelected(
                adapterView: AdapterView<*>,
                view: View?,
                position: Int,
                id: Long
            ) {
                val mode = adapterView.getItemAtPosition(position).toString()
                preferences.restrictMode =
                    if (mode == getString(R.string.filter_mode_allow))
                        Preferences.RestrictMode.ALLOW
                    else
                        Preferences.RestrictMode.BLOCK
                updateList()
            }

            override fun onNothingSelected(adapterView: AdapterView<*>?) {}
        }

        adapter = FilterListAdapter(this)
        rc_devices.layoutManager = LinearLayoutManager(this)
        rc_devices.adapter = adapter

        val mode = getString(
            if (preferences.restrictMode == Preferences.RestrictMode.ALLOW)
                R.string.filter_mode_allow
            else
                R.string.filter_mode_block
        )

        for (i in filterModeSpinner.count - 1 downTo 0) {
            if (mode == filterModeSpinner.getItemAtPosition(i)) {
                filterModeSpinner.setSelection(i, false)
                break
            }
        }

        swipeRefresh.setOnRefreshListener {
            updateList()
            swipeRefresh.isRefreshing = false
        }

        buttonCamera.setOnClickListener {
            startActivityForResult(
                Intent(this, CaptureActivity::class.java),
                REQUEST_CODE_SCAN
            )
        }

        buttonRefresh.setOnClickListener {
            key = bytesToUUID(randomBytes(16)).toString()
            updateQRCode()
        }
    }


    private fun updateQRCode() {
        CoroutineScope(Dispatchers.Default).launch {
            val nightMode =
                Configuration.UI_MODE_NIGHT_YES == resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
            val bitmap = createQRCode(key, nightMode, 1200)
            withContext(Dispatchers.Main) {
                text_key.text = key
                image_qr_code.setImageBitmap(bitmap)
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_about) {
            Toast.makeText(this, "About", Toast.LENGTH_SHORT).show()
        }
        return super.onOptionsItemSelected(item)
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            when (requestCode) {
                REQUEST_CODE_SCAN -> {
                    val result = data!!.getStringExtra(Intents.Scan.RESULT)!!
                    val content: String = QRKeyUtils.deprefix(result)
                    if (key == content) {
                        Toast.makeText(this, getString(R.string.toast_same_key), Toast.LENGTH_LONG)
                            .show()
                    } else {
                        key = content
                        updateQRCode()
                    }
                }
                REQUEST_CODE_WELCOME -> {
                    key = bytesToUUID(randomBytes(16)).toString()
                    updateQRCode()
                }
            }
        }
    }
}