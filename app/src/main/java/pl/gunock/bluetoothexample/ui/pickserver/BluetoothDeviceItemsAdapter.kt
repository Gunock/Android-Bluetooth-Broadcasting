package pl.gunock.bluetoothexample.ui.pickserver

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import pl.gunock.bluetoothexample.databinding.ItemBluetoothDeviceBinding

class BluetoothDeviceItemsAdapter(
    private val mOnItemClick: (item: BluetoothDeviceItem) -> Unit,
    private val mItems: MutableList<BluetoothDeviceItem> = mutableListOf()
) : RecyclerView.Adapter<BluetoothDeviceItemsAdapter.ViewHolder>() {

    suspend fun submitCollection(collection: Collection<BluetoothDeviceItem>) {
        if (mItems.size == collection.size && mItems.containsAll(collection)) {
            return
        }

        withContext(Dispatchers.Main) { notifyItemRangeRemoved(0, mItems.size) }
        mItems.clear()
        mItems.addAll(collection)
        withContext(Dispatchers.Main) { notifyItemRangeInserted(0, mItems.size) }
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

    override fun getItemCount() = mItems.size

    inner class ViewHolder(
        private val mBinding: ItemBluetoothDeviceBinding
    ) : RecyclerView.ViewHolder(mBinding.root) {
        fun bind(position: Int) {
            val item: BluetoothDeviceItem = mItems[position]

            if (item.isAvailable) {
                mBinding.imvDeviceConnected.visibility = View.VISIBLE
                mBinding.imvDeviceDisconnected.visibility = View.GONE
            } else {
                mBinding.imvDeviceConnected.visibility = View.GONE
                mBinding.imvDeviceDisconnected.visibility = View.VISIBLE
            }

            mBinding.tvItemDeviceName.text = item.bluetoothDevice.name
            mBinding.root.setOnClickListener {
                mOnItemClick(item)
            }
        }
    }

}