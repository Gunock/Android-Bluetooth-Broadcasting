package pl.gunock.bluetoothexample.ui.client

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
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
import pl.gunock.bluetoothexample.R
import pl.gunock.bluetoothexample.databinding.ActivityClientBinding
import pl.gunock.bluetoothexample.databinding.ContentClientBinding
import pl.gunock.bluetoothexample.extensions.getViewModelFactory
import pl.gunock.bluetoothexample.extensions.registerForActivityResult
import pl.gunock.bluetoothexample.ui.pickserver.PickDeviceDialogFragment
import pl.gunock.bluetoothexample.ui.pickserver.PickDeviceDialogViewModel

class ClientActivity : AppCompatActivity() {
    private companion object {
        const val TAG = "MainActivity"
        const val BT_PERMISSION_RESULT_CODE = 1
    }

    private val viewModel by viewModels<ClientViewModel> { getViewModelFactory() }

    private lateinit var mBinding: ContentClientBinding

    private lateinit var mPickDeviceDialogViewModel: PickDeviceDialogViewModel

    private val mEnableBluetoothActivityResultLauncher =
        registerForActivityResult(this::handleEnableBluetoothResult)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val rootBinding = ActivityClientBinding.inflate(layoutInflater)
        mBinding = rootBinding.content
        setContentView(rootBinding.root)


        mPickDeviceDialogViewModel =
            ViewModelProvider(this).get(PickDeviceDialogViewModel::class.java)

        setUpObservers()
        setUpListeners()

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
            setUpBluetooth()

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

        setUpBluetooth()
    }

    private fun setUpBluetooth() {
        val bluetoothManager =
            baseContext.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager

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
            mEnableBluetoothActivityResultLauncher.launch(enableBluetoothIntent)
        }
    }

    private fun setUpListeners() {
        val bluetoothManager =
            baseContext.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager

        mBinding.btnPickServerDevice.setOnClickListener {
            PickDeviceDialogFragment(
                ParcelUuid(ClientViewModel.SERVICE_UUID),
                bluetoothManager
            ).apply {
                setStyle(DialogFragment.STYLE_NORMAL, R.style.Theme_BluetoothTest_Dialog)
                show(supportFragmentManager, PickDeviceDialogFragment.TAG)
            }

            mPickDeviceDialogViewModel.bluetoothDevice.postValue(null)
            mPickDeviceDialogViewModel.bluetoothDevice.observe(
                this,
                this::observeDialogBluetoothDevice
            )
        }

        mBinding.btnDisconnect.setOnClickListener {
            viewModel.stopClient()
        }
    }

    private fun setUpObservers() {
        viewModel.clientStatus.observe(this) { (stringId, deviceName) ->
            var statusText = getString(stringId)
            if (deviceName != null) {
                statusText = statusText.format(deviceName)
            }

            mBinding.tvServerConnectionStatus.text = statusText
        }

        viewModel.receivedText.observe(this) {
            mBinding.tvMessagePreview.text = it
        }
    }

    private fun observeDialogBluetoothDevice(device: BluetoothDevice?) {
        mPickDeviceDialogViewModel.bluetoothDevice.removeObservers(this)
        viewModel.setClient(device ?: return)
        viewModel.startClient()
    }

    private fun handleEnableBluetoothResult(result: ActivityResult) {
        if (result.resultCode == RESULT_CANCELED) {
            Log.d(TAG, "User refused REQUEST_ENABLE_BT")
        }
    }
}