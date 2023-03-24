package pl.gunock.bluetoothexample.ui.client.pickserver

import android.app.Dialog
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.IntentFilter
import android.os.Bundle
import android.os.ParcelUuid
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import pl.gunock.bluetoothexample.databinding.DialogFragmentPickDeviceBinding
import pl.gunock.bluetoothexample.shared.bluetooth.BluetoothServiceDiscoveryManager
import javax.inject.Inject


@AndroidEntryPoint
class PickDeviceDialogFragment(
    private val serviceUuid: ParcelUuid,
) : DialogFragment() {
    companion object {
        const val TAG = "PickDeviceDialogFrag"
    }

    private lateinit var viewModel: PickDeviceDialogViewModel

    private lateinit var binding: DialogFragmentPickDeviceBinding

    private lateinit var recyclerViewAdapter: BluetoothDeviceItemsAdapter

    @Inject
    lateinit var bluetoothManager: BluetoothManager

    @Inject
    lateinit var serviceDiscoveryManager: BluetoothServiceDiscoveryManager

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        serviceDiscoveryManager.setExpectedUuids(listOf(serviceUuid))

        viewModel =
            ViewModelProvider(requireActivity())[PickDeviceDialogViewModel::class.java]

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

        setupRecyclerView()

        return binding.root
    }

    override fun onResume() {
        super.onResume()

        requireActivity().registerReceiver(
            serviceDiscoveryManager.getBroadcastReceiver(),
            IntentFilter(BluetoothDevice.ACTION_UUID)
        )
    }

    override fun onPause() {
        super.onPause()

        requireActivity().unregisterReceiver(serviceDiscoveryManager.getBroadcastReceiver())
    }


    private fun setupRecyclerView() {
        recyclerViewAdapter = BluetoothDeviceItemsAdapter(
            binding.rcvBluetoothDevices.context
        ) { item: BluetoothDeviceItem ->
            if (viewModel.pickBluetoothDeviceItem(item)) {
                dismiss()
            }
        }

        binding.rcvBluetoothDevices.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(requireContext())
            adapter = recyclerViewAdapter

            addItemDecoration(
                DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL)
            )
        }

        lifecycleScope.launch(Dispatchers.IO) { checkDeviceStates() }

        serviceDiscoveryManager.getBluetoothDevices()
            .onEach { collection ->
                val devices = try {
                    collection.map { BluetoothDeviceItem(it.name, it.address, true) }
                } catch (ex: SecurityException) {
                    requireActivity().finish()
                    return@onEach
                }
                recyclerViewAdapter.submitList(devices)
            }.flowOn(Dispatchers.Default)
            .launchIn(lifecycleScope)
    }

    private suspend fun checkDeviceStates() {
        while (true) {
            try {
                val pairedDevices = bluetoothManager.adapter
                    .bondedDevices
                    .toMutableList()
                serviceDiscoveryManager.discoverServicesInDevices(pairedDevices)
            } catch (ex: SecurityException) {
                requireActivity().finish()
                return
            }

            delay(20000)
        }
    }
}