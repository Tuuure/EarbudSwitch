package app.tuuure.earbudswitch.ui.adapter

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothClass
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import app.tuuure.earbudswitch.R
import app.tuuure.earbudswitch.data.Preferences
import app.tuuure.earbudswitch.data.db.DbRecord
import app.tuuure.earbudswitch.data.db.EarbudsDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.java.KoinJavaComponent.inject
import java.util.*

class FilterListAdapter(var context: Context) :
    RecyclerView.Adapter<FilterListAdapter.ViewHolder>() {

    private val database: EarbudsDatabase by inject(EarbudsDatabase::class.java)
    private val data: LinkedList<DbRecord> = LinkedList()

    var restrictMode: Preferences.RestrictMode = Preferences.RestrictMode.BLOCK
        set(value) {
            if (field != value) {
                field = value
                updateList()
            }
        }

    fun updateList() {
        CoroutineScope(Dispatchers.IO).launch {
            database.dbDao().getAll().sortedBy {
                when (restrictMode) {
                    Preferences.RestrictMode.ALLOW -> it.isAllowed
                    Preferences.RestrictMode.BLOCK -> it.isBlocked
                }
            }.also { list ->
                data.clear()
                data.addAll(list)
            }

            BluetoothAdapter.getDefaultAdapter()?.also { bluetoothAdapter ->
                if (bluetoothAdapter.isEnabled) {
                    bluetoothAdapter.bondedDevices.filterNot {
                        it.name.isNullOrEmpty()
                                || it.bluetoothClass.majorDeviceClass != BluetoothClass.Device.Major.AUDIO_VIDEO
                    }.forEach { device ->
                        DbRecord(device).also {
                            if (!data.contains(it)) {
                                database.dbDao().insert(it)
                                data.add(it)
                            }
                        }
                    }
                }
            }

            withContext(Dispatchers.Main) {
                notifyDataSetChanged()
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_device, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = data[position]
        holder.textView.text = item.name
        holder.itemView.setOnClickListener(holder)
        holder.checkBox.isChecked = when (restrictMode) {
            Preferences.RestrictMode.ALLOW -> item.isAllowed
            Preferences.RestrictMode.BLOCK -> item.isBlocked
        }
    }

    override fun getItemCount(): Int {
        return data.size
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView),
        View.OnClickListener {
        val textView: TextView = itemView.findViewById(R.id.list_item_text)
        val checkBox: CheckBox = itemView.findViewById(R.id.list_item_check)

        override fun onClick(view: View?) {
            val record: DbRecord = data[adapterPosition]

            when (restrictMode) {
                Preferences.RestrictMode.ALLOW -> {
                    record.isAllowed = !record.isAllowed
                }
                Preferences.RestrictMode.BLOCK -> {
                    record.isBlocked = !record.isBlocked
                }
            }

            CoroutineScope(Dispatchers.IO).launch {
                database.dbDao().update(record)
            }

            notifyItemChanged(adapterPosition)
        }
    }
}