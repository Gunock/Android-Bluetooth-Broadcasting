package dev.thomas_kiljanczyk.bluetoothbroadcasting.ui.client.pick_gms_server_device

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import dev.thomas_kiljanczyk.bluetoothbroadcasting.databinding.ItemGmsServerDeviceBinding

class GmsNearbyServerDeviceItemsAdapter(
    context: Context,
    private val onItemClick: (item: GmsNearbyServerDeviceItem) -> Unit,
) : ListAdapter<GmsNearbyServerDeviceItem, GmsNearbyServerDeviceItemsAdapter.ViewHolder>(
    DiffCallback()
) {

    private val inflater: LayoutInflater = LayoutInflater.from(context)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding =
            ItemGmsServerDeviceBinding.inflate(inflater, parent, false)

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
        private val binding: ItemGmsServerDeviceBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(position: Int) {
            val item: GmsNearbyServerDeviceItem = currentList[position]

            binding.tvItemDeviceName.text = item.deviceName
            binding.root.setOnClickListener {
                onItemClick(item)
            }
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<GmsNearbyServerDeviceItem>() {
        override fun areItemsTheSame(
            oldItem: GmsNearbyServerDeviceItem,
            newItem: GmsNearbyServerDeviceItem
        ): Boolean =
            oldItem.endpointId == newItem.endpointId

        override fun areContentsTheSame(
            oldItem: GmsNearbyServerDeviceItem,
            newItem: GmsNearbyServerDeviceItem
        ): Boolean =
            oldItem == newItem
    }
}