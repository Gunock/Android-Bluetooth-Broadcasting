package pl.gunock.bluetoothexample.client.activities

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
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import pl.gunock.bluetoothexample.client.R
import pl.gunock.bluetoothexample.client.databinding.ActivityClientMainBinding
import pl.gunock.bluetoothexample.client.databinding.ContentClientMainBinding
import pl.gunock.bluetoothexample.client.fragments.dialogs.PickDeviceDialogFragment
import pl.gunock.bluetoothexample.client.fragments.viemodels.PickDeviceDialogViewModel
import pl.gunock.bluetoothexample.common.bluetooth.BluetoothClient
import pl.gunock.bluetoothexample.server.registerForActivityResult
import java.util.*

class MainActivity : AppCompatActivity() {
    private companion object {
        const val TAG = "MainActivity"
        const val BT_PERMISSION_RESULT_CODE = 1

        val SERVICE_UUID: UUID = UUID.fromString("2f58e6c0-5ccf-4d2f-afec-65a2d98e2141")
    }

    private lateinit var mBinding: ContentClientMainBinding

    private lateinit var mPickDeviceDialogViewModel: PickDeviceDialogViewModel

    private var mBluetoothAdapter: BluetoothAdapter? = null

    private lateinit var mBluetoothManager: BluetoothManager

    private var mBluetoothClient: BluetoothClient? = null

    private val mEnableBluetoothActivityResultLauncher =
        registerForActivityResult { result: ActivityResult ->
            if (result.resultCode == RESULT_CANCELED) {
                Log.d(TAG, "User refused REQUEST_ENABLE_BT")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val rootBinding = ActivityClientMainBinding.inflate(layoutInflater)
        mBinding = rootBinding.contentMain
        setContentView(rootBinding.root)

        mPickDeviceDialogViewModel =
            ViewModelProvider(this).get(PickDeviceDialogViewModel::class.java)

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
            initializeButtons()
        } else {
            requestPermissions(permissions.toTypedArray(), BT_PERMISSION_RESULT_CODE)
        }
    }

    private fun initializeButtons() {
        setUpBluetooth()
        setUpListeners()
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

        initializeButtons()
    }

    private fun setUpBluetooth() {
        mBluetoothManager =
            baseContext.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager

        mBluetoothAdapter = mBluetoothManager.adapter

        if (mBluetoothAdapter == null) {
            Toast.makeText(
                baseContext,
                "Oops! It looks like your device doesn't have bluetooth!",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        if (mBluetoothAdapter?.isEnabled == false) {
            val enableBluetoothIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            mEnableBluetoothActivityResultLauncher.launch(enableBluetoothIntent)
        }
    }

    private fun setUpListeners() {
        mBinding.btnPickServerDevice.setOnClickListener {
            PickDeviceDialogFragment(
                ParcelUuid(SERVICE_UUID),
                mBluetoothManager
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
            lifecycleScope.launch(Dispatchers.Default) { mBluetoothClient?.disconnect() }
            Log.i(TAG, "Client disconnected")
        }
    }

    private fun observeDialogBluetoothDevice(device: BluetoothDevice?) {
        if (device == null) {
            return
        }
        mPickDeviceDialogViewModel.bluetoothDevice.removeObservers(this)

        runBlocking { mBluetoothClient?.disconnect() }
        mBluetoothClient = BluetoothClient(
            device,
            SERVICE_UUID
        ) {
            val text = it.decodeToString()
            Log.i(TAG, "Received message '$text'")
            withContext(Dispatchers.Main) {
                mBinding.tvMessagePreview.text = text
            }
        }.apply {
            setOnConnectionSuccessListener {
                mBinding.tvServerConnectionStatus.text =
                    getString(R.string.activity_main_connected).format(it.remoteDevice.name)
            }

            setOnConnectionFailureListener {
                mBinding.tvServerConnectionStatus.text =
                    getString(R.string.activity_main_disconnected)
            }

            setOnDisconnectionListener {
                mBinding.tvServerConnectionStatus.text =
                    getString(R.string.activity_main_disconnected)
            }
        }

        lifecycleScope.launch(Dispatchers.IO) {
            Log.i(TAG, "Started listening")
            mBluetoothClient?.startLoop()
        }
    }
}