package pl.gunock.bluetoothexample.server

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothServerSocket
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
import com.example.bluetoothexample.databinding.ContentMainBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.*


class MainActivity : AppCompatActivity() {
    private companion object {
        const val TAG = "MainActivity"

        const val BT_PERMISSION = 1

        const val REQUEST_ENABLE_BT = 1
        const val REQUEST_DISCOVERABLE = 2

        const val SERVICE_NAME = "SERVICE"
        val SERVICE_UUID = UUID(1, 1)
    }

    private lateinit var mBinding: ContentMainBinding

    private var mBluetoothAdapter: BluetoothAdapter? = null

    private val mSocket: BluetoothServerSocket? by lazy(LazyThreadSafetyMode.NONE) {
        mBluetoothAdapter?.listenUsingInsecureRfcommWithServiceRecord(SERVICE_NAME, SERVICE_UUID)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = ContentMainBinding.inflate(layoutInflater)
        setContentView(mBinding.btnServer)


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
        }

        if (mBluetoothAdapter?.isEnabled == false) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
        }

        CoroutineScope(Dispatchers.Main).launch {
            val discoverableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE)
            intent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 600)
            startActivityForResult(discoverableIntent, REQUEST_DISCOVERABLE)
        }

        mBinding.btnServer.setOnClickListener {
            CoroutineScope(Dispatchers.IO).launch {
                Log.i(TAG, "Started listening")
                acceptConnection()
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

        if (requestCode == REQUEST_DISCOVERABLE && resultCode == RESULT_CANCELED) {
            Log.d(TAG, "User refused REQUEST_DISCOVERABLE")
        }

    }

    private fun manageMyConnectedSocket(bluetoothSocket: BluetoothSocket) {

    }

    @Suppress("BlockingMethodInNonBlockingContext")
    suspend fun acceptConnection() {
        var shouldLoop = true
        while (shouldLoop) {
            val socket: BluetoothSocket? = try {
                mSocket?.accept()
            } catch (e: IOException) {
                Log.e(TAG, "Socket's accept() method failed", e)
                shouldLoop = false
                null
            }
            socket?.also {
                Log.i(TAG, "Connection accepted")
                manageMyConnectedSocket(it)

                val text = "Hello there!"
                socket.outputStream.write(text.toByteArray())
                socket.outputStream.flush()
                Log.i(TAG, "Sent message '$text'")

                delay(5000)

                socket.close()
                shouldLoop = false
            }
        }
    }
}

