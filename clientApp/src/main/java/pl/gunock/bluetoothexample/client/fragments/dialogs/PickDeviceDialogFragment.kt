package pl.gunock.bluetoothexample.client.fragments.dialogs

import android.app.Dialog
import android.bluetooth.*
import android.os.Bundle
import android.os.ParcelUuid
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import pl.gunock.bluetoothexample.client.adapters.BluetoothDeviceItemsAdapter
import pl.gunock.bluetoothexample.client.databinding.DialogFragmentPickDeviceBinding
import pl.gunock.bluetoothexample.client.fragments.viemodels.PickDeviceDialogViewModel
import pl.gunock.bluetoothexample.client.models.BluetoothDeviceItem


class PickDeviceDialogFragment(
    private val mBluetoothManager: BluetoothManager,
    private val mServiceParcelUuid: ParcelUuid
) : DialogFragment() {
    companion object {
        const val TAG = "PickDeviceDialogFrag"
    }

    private lateinit var mPickDeviceDialogViewModel: PickDeviceDialogViewModel

    private lateinit var mBinding: DialogFragmentPickDeviceBinding

    private lateinit var mAdapter: BluetoothDeviceItemsAdapter

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        mPickDeviceDialogViewModel =
            ViewModelProvider(requireActivity()).get(PickDeviceDialogViewModel::class.java)

        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.setTitle("Pick server device")
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        mBinding = DialogFragmentPickDeviceBinding.inflate(inflater)
        setUpRecyclerView()

        return mBinding.root
    }

    private fun setUpRecyclerView() {
        mAdapter = BluetoothDeviceItemsAdapter({ item: BluetoothDeviceItem ->
            if (!item.isAvailable) {
                Toast.makeText(
                    requireContext(),
                    "Selected device is not available!",
                    Toast.LENGTH_SHORT
                ).show()
                return@BluetoothDeviceItemsAdapter
            }

            mPickDeviceDialogViewModel.bluetoothDevice.postValue(item.bluetoothDevice)
            dismiss()
        })

        mBinding.rcvBluetoothDevices.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(requireContext())
            adapter = mAdapter

            addItemDecoration(
                DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL)
            )
        }

        lifecycleScope.launch(Dispatchers.IO) { checkDeviceStates() }
    }

    private suspend fun checkDeviceStates() {
        while (true) {
            val pairedDevices: MutableSet<BluetoothDevice> = mBluetoothManager.adapter.bondedDevices
            val filteredDevices = pairedDevices.filter { mServiceParcelUuid in it.uuids }
                .map { BluetoothDeviceItem(it, checkDeviceAvailability(it)) }

            mAdapter.submitCollection(filteredDevices)
            delay(1000)
        }
    }

    private fun checkDeviceAvailability(device: BluetoothDevice): Boolean {
        val connection: BluetoothGatt =
            device.connectGatt(requireContext(), false, object : BluetoothGattCallback() {})

        connection.connect()

        val connectionState = mBluetoothManager.getConnectionState(device, BluetoothGatt.GATT)
        val available = connectionState == BluetoothProfile.STATE_CONNECTED

        connection.close()
        return available
    }

}