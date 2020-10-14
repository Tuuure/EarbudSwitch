package app.tuuure.earbudswitch.ui.activity

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
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
import app.tuuure.earbudswitch.service.AudioMonitorService
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


class SettingsActivity : AppCompatActivity() {
    companion object {
        const val REQUEST_CODE_WELCOME = 9
        private const val REQUEST_CODE_SCAN = 12
    }

    private val preferences: Preferences by inject()

    private var key = preferences.key
        set(value) {
            preferences.key = value
            CoroutineScope(Dispatchers.Default).launch {
                val uiMode =
                    resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
                val bitmap =
                    createQRCode(value, Configuration.UI_MODE_NIGHT_YES == uiMode, 1200)
                withContext(Dispatchers.Main) {
                    text_key.text = value
                    image_qr_code.setImageBitmap(bitmap)
                }
            }
            field = value
        }

    private var restrictMode: Preferences.RestrictMode = preferences.restrictMode
        set(value) {
            if (field != value) {
                preferences.restrictMode = value

                adapter.restrictMode = value

                val mode = getString(
                    if (value == Preferences.RestrictMode.ALLOW)
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

                field = value
            }
        }

    private lateinit var adapter: FilterListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        initView()
        setSupportActionBar(toolbar)
    }

    private fun checkInit(): Boolean {
        if (key.isEmpty()
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

    override fun onResume() {
        if (checkInit()) {
            key = preferences.key
            restrictMode = preferences.restrictMode
        }
        super.onResume()
    }

    private fun initView() {
        adapter = FilterListAdapter(this)
        rc_devices.layoutManager = LinearLayoutManager(this)
        rc_devices.adapter = adapter

        adapter.updateList()
        adapter.restrictMode = preferences.restrictMode

        filterModeSpinner.onItemSelectedListener = object : OnItemSelectedListener {
            override fun onItemSelected(
                adapterView: AdapterView<*>,
                view: View?,
                position: Int,
                id: Long
            ) {
                adapterView.getItemAtPosition(position).toString().also {
                    restrictMode =
                        if (it == getString(R.string.filter_mode_allow))
                            Preferences.RestrictMode.ALLOW
                        else
                            Preferences.RestrictMode.BLOCK
                }
            }

            override fun onNothingSelected(adapterView: AdapterView<*>?) {}
        }

        swipeRefresh.setOnRefreshListener {
            adapter.updateList()
            swipeRefresh.isRefreshing = false
        }

        buttonCamera.setOnClickListener {
            Intent(this, CaptureActivity::class.java).also {
                startActivityForResult(it, REQUEST_CODE_SCAN)
            }
        }

        buttonRefresh.setOnClickListener {
            key = bytesToUUID(randomBytes(16)).toString()
        }

        toolbar.setOnLongClickListener {
            Intent(this, AudioMonitorService::class.java).also {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    startForegroundService(it)
                else
                    startService(it)
            }
            return@setOnLongClickListener true
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_settings, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_about -> {
                Toast.makeText(this, "About", Toast.LENGTH_SHORT).show()
            }
            R.id.action_warning -> {
                Toast.makeText(this, "Warning", Toast.LENGTH_SHORT).show()
            }
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
                    }
                }
                REQUEST_CODE_WELCOME -> {
                    key = bytesToUUID(randomBytes(16)).toString()
                }
            }
        }
    }
}