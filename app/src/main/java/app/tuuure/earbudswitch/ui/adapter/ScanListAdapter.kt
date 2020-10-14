package app.tuuure.earbudswitch.ui.adapter

import android.annotation.SuppressLint
import android.bluetooth.BluetoothA2dp
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHeadset
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.graphics.drawable.Animatable
import android.graphics.drawable.Drawable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import app.tuuure.earbudswitch.ConnectGattEvent
import app.tuuure.earbudswitch.R
import app.tuuure.earbudswitch.data.Earbud
import app.tuuure.earbudswitch.utils.EarbudConnectUtils
import org.greenrobot.eventbus.EventBus
import java.util.*

class ScanListAdapter(val context: Context) :
    RecyclerView.Adapter<ScanListAdapter.ViewHolder>() {

    var data: LinkedHashSet<Earbud> = LinkedHashSet()
        set(value) {
            if (!value.isNullOrEmpty()) {
                Log.d("TAG","loadDevices")
                field = value
                notifyDataSetChanged()
            }
        }

    @SuppressLint("UseCompatLoadingForDrawables")
    val nearIcon: Drawable = context.getDrawable(R.drawable.ic_near_me_24)!!

    @SuppressLint("UseCompatLoadingForDrawables")
    val connectIcon: Drawable = context.getDrawable(R.drawable.ic_done_24)!!

    @SuppressLint("UseCompatLoadingForDrawables")
    val headSetIcon: Drawable = context.getDrawable(R.drawable.ic_headset_24)!!

    @SuppressLint("UseCompatLoadingForDrawables")
    val refreshIcon: Drawable = context.getDrawable(R.drawable.anim_refresh)!!

    fun setStatus(device: BluetoothDevice, action: String, state: Int) {
        val index = data.indexOf(Earbud(device))
        val tempItem = data.elementAt(index)
        when (state) {
            BluetoothProfile.STATE_CONNECTING -> {
                when (action) {
                    BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED -> {
                        tempItem.isA2dpConnected = false
                        tempItem.isA2dpConnecting = true
                    }
                    BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED -> {
                        tempItem.isHeadsetConnected = false
                        tempItem.isHeadsetConnecting = true
                    }
                }
            }
            BluetoothProfile.STATE_CONNECTED -> {
                when (action) {
                    BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED -> {
                        tempItem.isA2dpConnected = true
                        tempItem.isA2dpConnecting = false
                    }
                    BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED -> {
                        tempItem.isHeadsetConnected = true
                        tempItem.isHeadsetConnecting = false
                    }
                }
            }
            else -> {
                when (action) {
                    BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED -> {
                        tempItem.isA2dpConnected = false
                        tempItem.isA2dpConnecting = false
                    }
                    BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED -> {
                        tempItem.isHeadsetConnected = false
                        tempItem.isHeadsetConnecting = false
                    }
                }
            }
        }
        data.add(tempItem)
        notifyItemChanged(index)
    }

    fun setServer(server: String, devices: MutableSet<String>) {
        data.forEach {
            val isContained = it.address in devices
            if (isContained && it.server != server) {
                it.server = server
                notifyItemChanged(data.indexOf(it))
            }
        }
    }

    fun removeServer(server: String, devices: MutableSet<String>) {
        data.forEach {
            if (it.server == server) {
                it.server = ""
                notifyItemChanged(data.indexOf(it))
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_device, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = data.elementAt(position)
        holder.textView.text = item.name
        holder.itemView.setOnClickListener(holder)

        var drawable: Drawable? = null
        if (item.server.isNotEmpty())
            drawable = nearIcon
        if (item.isHeadsetConnecting || item.isA2dpConnecting) {
            drawable = refreshIcon
        }
        if (item.isA2dpConnected || item.isHeadsetConnected) {
            drawable = connectIcon
        }


        holder.textView.setCompoundDrawablesRelativeWithIntrinsicBounds(
            headSetIcon,
            null,
            drawable,
            null
        )

        if (drawable is Animatable) {
            (drawable as Animatable).start()
        }
    }

    override fun getItemCount(): Int {
        return data.size
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView),
        View.OnClickListener {
        val textView: TextView = itemView.findViewById(R.id.list_item_text)

        override fun onClick(view: View?) {
            val wrapperPosition = adapterPosition
            val item = data.elementAt(wrapperPosition)

            if (item.isHeadsetConnected || item.isA2dpConnected) {
                AlertDialog.Builder(context).apply {
                    setTitle(R.string.dialog_disconnect_title)
                    setMessage(
                        String.format(
                            context.getString(R.string.dialog_disconnect_content),
                            item.name
                        )
                    )
                    setNegativeButton(R.string.dialog_button_cancel) { _, _ -> }
                    setPositiveButton(R.string.dialog_button_ok) { _, _ ->
                        EarbudConnectUtils.disconnectEBS(
                            context,
                            item.address
                        )
                    }
                }.show()
            } else {
                if (item.server.isEmpty()) {
                    EarbudConnectUtils.connectEBS(context, item.address)
                } else {
                    EventBus.getDefault().post(ConnectGattEvent(item.server, item.address))
                }
            }
        }
    }
}