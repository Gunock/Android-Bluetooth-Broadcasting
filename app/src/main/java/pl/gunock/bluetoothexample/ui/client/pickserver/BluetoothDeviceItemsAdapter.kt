package pl.gunock.bluetoothexample.ui.client.pickserver

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import pl.gunock.bluetoothexample.databinding.ItemBluetoothDeviceBinding

class BluetoothDeviceItemsAdapter(
    private val onItemClick: (item: BluetoothDeviceItem) -> Unit,
    private val items: MutableList<BluetoothDeviceItem> = mutableListOf()
) : RecyclerView.Adapter<BluetoothDeviceItemsAdapter.ViewHolder>() {

    suspend fun submitCollection(collection: Collection<BluetoothDeviceItem>) {
        if (items.size == collection.size && items.containsAll(collection)) {
            return
        }

        withContext(Dispatchers.Main) { notifyItemRangeRemoved(0, items.size) }
        items.clear()
        items.addAll(collection)
        withContext(Dispatchers.Main) { notifyItemRangeInserted(0, items.size) }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding =
            ItemBluetoothDeviceBinding.inflate(LayoutInflater.from(parent.context), parent, false)

        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(position)
    }

    override fun getItemId(position: Int): Long {
        return -1
    }

    override fun getItemCount() = items.size

    inner class ViewHolder(
        private val binding: ItemBluetoothDeviceBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(position: Int) {
            val item: BluetoothDeviceItem = items[position]

            if (item.isAvailable) {
                binding.imvDeviceConnected.visibility = View.VISIBLE
                binding.imvDeviceDisconnected.visibility = View.GONE
            } else {
                binding.imvDeviceConnected.visibility = View.GONE
                binding.imvDeviceDisconnected.visibility = View.VISIBLE
            }

            binding.tvItemDeviceName.text = item.bluetoothDevice.name
            binding.root.setOnClickListener {
                onItemClick(item)
            }
        }
    }

}