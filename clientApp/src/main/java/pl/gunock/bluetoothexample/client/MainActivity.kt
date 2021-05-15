package pl.gunock.bluetoothexample.client

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import pl.gunock.bluetoothexample.client.databinding.ActivityMainBinding
import pl.gunock.bluetoothexample.client.databinding.ContentMainBinding
import java.util.*

class MainActivity : AppCompatActivity() {
    private companion object {
        const val TAG = "MainActivity"
        const val BT_PERMISSION = 1
        const val REQUEST_ENABLE_BT = 2

        val SERVICE_UUID = UUID(1, 1)
    }

    private lateinit var mBinding: ContentMainBinding

    private var mBluetoothAdapter: BluetoothAdapter? = null

    private var mConnectedDevice: BluetoothDevice? = null

    private val mSocket: BluetoothSocket? by lazy(LazyThreadSafetyMode.NONE) {
        mConnectedDevice?.createRfcommSocketToServiceRecord(SERVICE_UUID)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val rootBinding = ActivityMainBinding.inflate(layoutInflater)
        mBinding = rootBinding.contentMain
        setContentView(rootBinding.root)

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
            requestPermissions(permissions.toTypedArray(), BT_PERMISSION)
        }
    }

    private fun initializeButtons() {
        val bluetoothManager =
            baseContext.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        mBluetoothAdapter = bluetoothManager.adapter

        if (mBluetoothAdapter == null) {
            Toast.makeText(
                baseContext,
                "Oops! It looks like your device doesn't have bluetooth!",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        if (mBluetoothAdapter?.isEnabled == false) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
        }

        val pairedDevices: Set<BluetoothDevice>? = mBluetoothAdapter?.bondedDevices
        pairedDevices?.forEach { device ->
            val deviceName = device.name
            val deviceHardwareAddress = device.address
            Log.d(TAG, "DevName: $deviceName")
            Log.d(TAG, "DevMAC: $deviceHardwareAddress")
        }
        mConnectedDevice = pairedDevices?.firstOrNull { device ->
            listOf("xperia", "oneplus").any {
//            listOf("xperia", "galaxy", "oneplus").any {
                device.name.contains(it, true)
            }
        }

        mBinding.btnClient.setOnClickListener {
            lifecycleScope.launch(Dispatchers.IO) {
                Log.i(TAG, "Started listening")
                connectToServer()
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        Log.i(TAG, "Request permission result: $requestCode")
        if (requestCode != BT_PERMISSION) {
            return
        }

        if (grantResults.any { it != PackageManager.PERMISSION_GRANTED }) {
            Log.i(TAG, "Permission not granted")
            return
        }

        initializeButtons()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_ENABLE_BT && resultCode == RESULT_CANCELED) {
            Log.d(TAG, "User refused REQUEST_ENABLE_BT")
        }
    }

    private fun manageMyConnectedSocket(bluetoothSocket: BluetoothSocket) {

    }

    @Suppress("BlockingMethodInNonBlockingContext")
    private suspend fun connectToServer() {
        mBluetoothAdapter?.cancelDiscovery()

        mSocket?.use { socket ->
            socket.connect()
            if (socket.isConnected) {
                Log.i(TAG, "Connected")
            }

            delay(1000)
            val available = socket.inputStream.available()
            Log.i(TAG, "Available data: $available")

            val buffer = ByteArray(available)
            socket.inputStream.read(buffer)
            val text = buffer.decodeToString()
            Log.i(TAG, "Received message '$text'")

            manageMyConnectedSocket(socket)
        }
    }
}