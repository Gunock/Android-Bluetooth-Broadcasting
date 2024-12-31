package dev.thomas_kiljanczyk.bluetoothbroadcasting.ui.client.pickserver

import android.app.Dialog
import android.bluetooth.BluetoothManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.nearby.connection.ConnectionsClient
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo
import com.google.android.gms.nearby.connection.DiscoveryOptions
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback
import com.google.android.gms.nearby.connection.Strategy
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import dev.thomas_kiljanczyk.bluetoothbroadcasting.R
import dev.thomas_kiljanczyk.bluetoothbroadcasting.databinding.DialogFragmentPickDeviceBinding
import dev.thomas_kiljanczyk.bluetoothbroadcasting.ui.shared.Constants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject


@AndroidEntryPoint
class PickDeviceDialogFragment : DialogFragment() {
    companion object {
        const val TAG = "PickDeviceDialogFrag"
    }

    private lateinit var viewModel: PickDeviceDialogViewModel

    private lateinit var binding: DialogFragmentPickDeviceBinding

    private lateinit var recyclerViewAdapter: BluetoothDeviceItemsAdapter

    @Inject
    lateinit var connectionsClient: ConnectionsClient

    @Inject
    lateinit var bluetoothManager: BluetoothManager

    private val deviceMap = mutableMapOf<String, BluetoothDeviceItem>()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
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

        val discoveryOptions = DiscoveryOptions.Builder().setStrategy(Strategy.P2P_STAR).build()
        connectionsClient
            .startDiscovery(
                Constants.SERVICE_UUID.toString(),
                object : EndpointDiscoveryCallback() {
                    override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
                        val deviceItem = BluetoothDeviceItem(info.endpointName, endpointId, true)
                        deviceMap[endpointId] = deviceItem

                        val devices = deviceMap.values.toList()
                        recyclerViewAdapter.submitList(devices)

                        CoroutineScope(Dispatchers.Main).launch {
                            binding.pbBluetoothDevices.visibility =
                                if (devices.isNotEmpty()) View.GONE else View.VISIBLE
                        }
                    }

                    override fun onEndpointLost(endpointId: String) {
                        deviceMap.remove(endpointId)

                        val devices = deviceMap.values.toList()
                        recyclerViewAdapter.submitList(devices)

                        CoroutineScope(Dispatchers.Main).launch {
                            binding.pbBluetoothDevices.visibility =
                                if (devices.isNotEmpty()) View.GONE else View.VISIBLE
                        }
                    }

                },
                discoveryOptions
            )
            .addOnSuccessListener { unused ->
                // We're discovering!
            }
            .addOnFailureListener { e ->
                // We're unable to start discovering.
                Log.e(TAG, "Failed to start discovering", e)
            }

        return binding.root
    }

    override fun onDestroy() {
        connectionsClient.stopDiscovery()
        super.onDestroy()
    }

    private fun setupRecyclerView() {
        recyclerViewAdapter = BluetoothDeviceItemsAdapter(
            binding.rcvBluetoothDevices.context
        ) { item: BluetoothDeviceItem ->
            if (viewModel.pickDevice(item)) {
                dismiss()
            }
        }

        binding.rcvBluetoothDevices.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context)
            adapter = recyclerViewAdapter
        }
    }
}