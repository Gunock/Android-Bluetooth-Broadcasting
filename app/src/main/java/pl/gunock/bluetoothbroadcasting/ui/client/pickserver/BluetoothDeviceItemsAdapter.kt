package pl.gunock.bluetoothbroadcasting.ui.client.pickserver

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import pl.gunock.bluetoothbroadcasting.databinding.ItemBluetoothDeviceBinding

// TODO: Implement loading spinner for empty state
class BluetoothDeviceItemsAdapter(
    context: Context,
    private val onItemClick: (item: BluetoothDeviceItem) -> Unit,
) : ListAdapter<BluetoothDeviceItem, BluetoothDeviceItemsAdapter.ViewHolder>(DiffCallback()) {

    private val inflater: LayoutInflater = LayoutInflater.from(context)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding =
            ItemBluetoothDeviceBinding.inflate(inflater, parent, false)

        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(position)
    }

    override fun getItemId(position: Int): Long {
        return -1
    }

    override fun getItemCount() = currentList.size

    inner class ViewHolder(
        private val binding: ItemBluetoothDeviceBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(position: Int) {
            val item: BluetoothDeviceItem = currentList[position]

            if (item.isAvailable) {
                binding.imvDeviceConnected.visibility = View.VISIBLE
                binding.imvDeviceDisconnected.visibility = View.GONE
            } else {
                binding.imvDeviceConnected.visibility = View.GONE
                binding.imvDeviceDisconnected.visibility = View.VISIBLE
            }

            binding.tvItemDeviceName.text = item.deviceName
            binding.root.setOnClickListener {
                onItemClick(item)
            }
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<BluetoothDeviceItem>() {
        override fun areItemsTheSame(
            oldItem: BluetoothDeviceItem,
            newItem: BluetoothDeviceItem
        ): Boolean =
            oldItem.deviceAddress == newItem.deviceAddress

        override fun areContentsTheSame(
            oldItem: BluetoothDeviceItem,
            newItem: BluetoothDeviceItem
        ): Boolean =
            oldItem == newItem
    }
}