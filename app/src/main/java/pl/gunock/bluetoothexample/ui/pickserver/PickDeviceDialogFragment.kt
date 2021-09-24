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
    private val bluetoothManager: BluetoothManager
) : DialogFragment() {
    companion object {
        const val TAG = "PickDeviceDialogFrag"
    }

    private lateinit var pickDeviceDialogViewModel: PickDeviceDialogViewModel

    private lateinit var binding: DialogFragmentPickDeviceBinding

    private lateinit var recyclerViewAdapter: BluetoothDeviceItemsAdapter

    private val serviceDiscoveryManager by lazy {
        BluetoothServiceDiscoveryManager(requireContext(), listOf(serviceUuid))
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        pickDeviceDialogViewModel =
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
        binding = DialogFragmentPickDeviceBinding.inflate(inflater)


        setUpRecyclerView()

        return binding.root
    }

    override fun onResume() {
        super.onResume()

        requireActivity().registerReceiver(
            serviceDiscoveryManager.receiver,
            IntentFilter(BluetoothDevice.ACTION_UUID)
        )
    }

    override fun onPause() {
        super.onPause()

        requireActivity().unregisterReceiver(serviceDiscoveryManager.receiver)
    }

    private fun setUpRecyclerView() {
        recyclerViewAdapter = BluetoothDeviceItemsAdapter({ item: BluetoothDeviceItem ->
            if (!item.isAvailable) {
                Toast.makeText(
                    requireContext(),
                    "Selected device is not available!",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                pickDeviceDialogViewModel.bluetoothDevice.postValue(item.bluetoothDevice)
                Log.i(TAG, "Picked : ${item.bluetoothDevice.name}")
                dismiss()
            }
        })

        binding.rcvBluetoothDevices.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(requireContext())
            adapter = recyclerViewAdapter

            addItemDecoration(
                DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL)
            )
        }

        lifecycleScope.launch(Dispatchers.IO) { checkDeviceStates() }

        serviceDiscoveryManager.devices.observe(this) { collection ->
            lifecycleScope.launch(Dispatchers.Default) {
                val devices = collection.map { BluetoothDeviceItem(it, true) }
                recyclerViewAdapter.submitCollection(devices)
            }
        }
    }

    private suspend fun checkDeviceStates() {
        while (true) {
            val pairedDevices: MutableList<BluetoothDevice> = bluetoothManager.adapter
                .bondedDevices
                .toMutableList()

            serviceDiscoveryManager.discoverServicesInDevices(pairedDevices)

            delay(20000)
        }
    }
}