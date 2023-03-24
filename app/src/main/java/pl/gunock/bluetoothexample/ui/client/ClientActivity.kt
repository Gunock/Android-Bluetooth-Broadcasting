package pl.gunock.bluetoothexample.ui.client

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.ParcelUuid
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import pl.gunock.bluetoothexample.R
import pl.gunock.bluetoothexample.databinding.ActivityClientBinding
import pl.gunock.bluetoothexample.databinding.ContentClientBinding
import pl.gunock.bluetoothexample.shared.extensions.registerForActivityResult
import pl.gunock.bluetoothexample.ui.client.pickserver.PickDeviceDialogFragment
import pl.gunock.bluetoothexample.ui.client.pickserver.PickDeviceDialogViewModel
import javax.inject.Inject

@AndroidEntryPoint
class ClientActivity : AppCompatActivity() {
    private companion object {
        const val TAG = "MainActivity"
        const val BT_PERMISSION_RESULT_CODE = 1
    }

    private val viewModel: ClientViewModel by viewModels()

    @Inject
    lateinit var bluetoothManager: BluetoothManager

    private lateinit var binding: ContentClientBinding

    private lateinit var pickDeviceDialogViewModel: PickDeviceDialogViewModel

    private val enableBluetoothActivityResultLauncher =
        registerForActivityResult(this::handleEnableBluetoothResult)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val rootBinding = ActivityClientBinding.inflate(layoutInflater)
        binding = rootBinding.content
        setContentView(rootBinding.root)


        pickDeviceDialogViewModel =
            ViewModelProvider(this)[PickDeviceDialogViewModel::class.java]

        setupObservers()
        setupListeners()

        val permissions: MutableList<String> = mutableListOf()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val isPermissionGranted = ContextCompat.checkSelfPermission(
                baseContext,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            )

            if (isPermissionGranted != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            }
        }

        if (permissions.isEmpty()) {
            setupBluetooth()

        } else {
            requestPermissions(permissions.toTypedArray(), BT_PERMISSION_RESULT_CODE)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        Log.i(TAG, "Request permission result: $requestCode")
        if (requestCode != BT_PERMISSION_RESULT_CODE) {
            return
        }

        if (grantResults.any { it != PackageManager.PERMISSION_GRANTED }) {
            Log.i(TAG, "Permission not granted")
            return
        }

        setupBluetooth()
    }

    private fun setupBluetooth() {
        val bluetoothAdapter = bluetoothManager.adapter

        if (bluetoothAdapter == null) {
            Toast.makeText(
                baseContext,
                "Oops! It looks like your device doesn't have bluetooth!",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        if (!bluetoothAdapter.isEnabled) {
            val enableBluetoothIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            enableBluetoothActivityResultLauncher.launch(enableBluetoothIntent)
        }
    }

    private fun setupListeners() {
        binding.btnPickServerDevice.setOnClickListener {
            PickDeviceDialogFragment(
                ParcelUuid(ClientViewModel.SERVICE_UUID)
            ).apply {
                setStyle(DialogFragment.STYLE_NORMAL, R.style.Theme_BluetoothTest_Dialog)
                show(supportFragmentManager, PickDeviceDialogFragment.TAG)
            }

            pickDeviceDialogViewModel.resetPickedBluetoothDevice()
        }

        binding.btnDisconnect.setOnClickListener {
            viewModel.stopClient()
        }
    }

    private fun setupObservers() {
        viewModel.clientStatus
            .onEach { (stringId, deviceName) ->
                var statusText = getString(stringId)
                if (deviceName != null) {
                    statusText = statusText.format(deviceName)
                }

                binding.tvServerConnectionStatus.text = statusText
            }.launchIn(lifecycleScope)

        viewModel.receivedText
            .onEach { binding.tvMessagePreview.text = it }
            .launchIn(lifecycleScope)

        pickDeviceDialogViewModel.bluetoothDeviceAddress
            .onEach(this::observeDialogBluetoothDevice)
            .launchIn(lifecycleScope)

        pickDeviceDialogViewModel.message
            .onEach {
                if (it.isNotBlank()) {
                    Toast.makeText(baseContext, it, Toast.LENGTH_SHORT).show()
                }
            }.launchIn(lifecycleScope)
    }

    private fun observeDialogBluetoothDevice(deviceAddress: String?) {
        val bluetoothAdapter = bluetoothManager.adapter
        val device = bluetoothAdapter.getRemoteDevice(deviceAddress ?: return)

        try {
            viewModel.setClient(device)
            viewModel.startClient()
        } catch (ex: SecurityException) {
            finish()
            return
        }
    }

    private fun handleEnableBluetoothResult(result: ActivityResult) {
        if (result.resultCode == RESULT_CANCELED) {
            Log.d(TAG, "User refused REQUEST_ENABLE_BT")
        }
    }
}