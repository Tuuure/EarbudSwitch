package app.tuuure.earbudswitch.ui.adapter

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
import org.koin.java.KoinJavaComponent.inject
import java.util.*

class FilterListAdapter(var context: Context) :
    RecyclerView.Adapter<FilterListAdapter.ViewHolder>() {

    var data: LinkedList<DbRecord> = LinkedList()
        set(value) {
            if (!value.isNullOrEmpty()) {
                field = value
                notifyDataSetChanged()
            }
        }

    var restrictMode: Preferences.RestrictMode = Preferences.RestrictMode.BLOCK

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_device, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = data[position]
        holder.textView.text = item.name
        holder.itemView.setOnClickListener(holder)
        holder.checkBox.visibility = View.VISIBLE
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
            val database: EarbudsDatabase by inject(EarbudsDatabase::class.java)

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