package pl.gunock.bluetoothexample.ui.pickserver

import android.app.Dialog
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.IntentFilter
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
import pl.gunock.bluetoothexample.databinding.DialogFragmentPickDeviceBinding
import pl.gunock.bluetoothexample.bluetooth.BluetoothServiceDiscoveryManager


class PickDeviceDialogFragment(
    serviceUuid: ParcelUuid,
    private val mBluetoothManager: BluetoothManager
) : DialogFragment() {
    companion object {
        const val TAG = "PickDeviceDialogFrag"
    }

    private lateinit var mPickDeviceDialogViewModel: PickDeviceDialogViewModel

    private lateinit var mBinding: DialogFragmentPickDeviceBinding

    private lateinit var mRecyclerViewAdapter: BluetoothDeviceItemsAdapter

    private val mServiceDiscoveryManager by lazy {
        BluetoothServiceDiscoveryManager(requireContext(), listOf(serviceUuid))
    }

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

    override fun onResume() {
        super.onResume()

        requireActivity().registerReceiver(
            mServiceDiscoveryManager.receiver,
            IntentFilter(BluetoothDevice.ACTION_UUID)
        )
    }

    override fun onPause() {
        super.onPause()

        requireActivity().unregisterReceiver(mServiceDiscoveryManager.receiver)
    }

    private fun setUpRecyclerView() {
        mRecyclerViewAdapter = BluetoothDeviceItemsAdapter({ item: BluetoothDeviceItem ->
            if (!item.isAvailable) {
                Toast.makeText(
                    requireContext(),
                    "Selected device is not available!",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                mPickDeviceDialogViewModel.bluetoothDevice.postValue(item.bluetoothDevice)
                Log.i(TAG, "Picked : ${item.bluetoothDevice.name}")
                dismiss()
            }
        })

        mBinding.rcvBluetoothDevices.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(requireContext())
            adapter = mRecyclerViewAdapter

            addItemDecoration(
                DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL)
            )
        }

        lifecycleScope.launch(Dispatchers.IO) { checkDeviceStates() }

        mServiceDiscoveryManager.devices.observe(this) { collection ->
            lifecycleScope.launch(Dispatchers.Default) {
                val devices = collection.map { BluetoothDeviceItem(it, true) }
                mRecyclerViewAdapter.submitCollection(devices)
            }
        }
    }

    private suspend fun checkDeviceStates() {
        while (true) {
            val pairedDevices: MutableList<BluetoothDevice> = mBluetoothManager.adapter
                .bondedDevices
                .toMutableList()

            mServiceDiscoveryManager.discoverServicesInDevices(pairedDevices)

            delay(20000)
        }
    }
}