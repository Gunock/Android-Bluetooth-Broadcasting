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
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pl.gunock.bluetoothexample.server.databinding.ActivityMainBinding
import pl.gunock.bluetoothexample.server.databinding.ContentMainBinding
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
        }

        if (mBluetoothAdapter?.isEnabled == false) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
        }

        mBinding.btnServer.setOnClickListener {
            lifecycleScope.launch(Dispatchers.Main) {
                Log.i(TAG, "Started listening")
                acceptConnection()
            }
        }

        val discoverableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE)
        intent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 600)
        startActivityForResult(discoverableIntent, REQUEST_DISCOVERABLE)
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

    private suspend fun acceptConnection() {
        var shouldLoop = true
        while (shouldLoop) {
            val socket: BluetoothSocket = try {
                withContext(Dispatchers.IO) {
                    @Suppress("BlockingMethodInNonBlockingContext")
                    mSocket?.accept()
                }
            } catch (e: IOException) {
                Log.e(TAG, "Socket's accept() method failed", e)
                shouldLoop = false
                null
            } ?: continue

            Log.i(TAG, "Connection accepted")
            manageMyConnectedSocket(socket)

            withContext(Dispatchers.IO) {
                @Suppress("BlockingMethodInNonBlockingContext")
                socket.use {
                    val text = "Hello there!"
                    Log.i(TAG, "Sent message '$text'")

                    socket.outputStream.write(text.toByteArray())
                    socket.outputStream.flush()
                }
            }

            delay(5000)
            shouldLoop = false
        }
    }
}

