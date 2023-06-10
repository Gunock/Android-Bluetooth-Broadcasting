package pl.gunock.bluetoothbroadcasting.ui.client.pickserver

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
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pl.gunock.bluetoothbroadcasting.R
import pl.gunock.bluetoothbroadcasting.databinding.DialogFragmentPickDeviceBinding
import pl.gunock.bluetoothbroadcasting.lib.BluetoothServiceDiscoveryManager
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

        binding = DialogFragmentPickDeviceBinding.inflate(layoutInflater)

        return MaterialAlertDialogBuilder(requireActivity())
            .setTitle(R.string.dialog_fragment_pick_device_title)
            .setNegativeButton(R.string.dialog_fragment_close, null)
            .setView(binding.root)
            .create()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
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
            layoutManager = LinearLayoutManager(context)
            adapter = recyclerViewAdapter
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
                withContext(Dispatchers.Main) {
                    binding.pbBluetoothDevices.visibility =
                        if (devices.isNotEmpty()) View.GONE else View.VISIBLE
                }
            }.flowOn(Dispatchers.Default)
            .launchIn(lifecycleScope)
    }

    private suspend fun checkDeviceStates() {
        while (true) {
            try {
                val pairedDevices = bluetoothManager.adapter
                    .bondedDevices
                    ?.toMutableList() ?: mutableListOf()
                serviceDiscoveryManager.discoverServicesInDevices(pairedDevices)
            } catch (ex: SecurityException) {
                requireActivity().finish()
                return
            }

            delay(20000)
        }
    }
}