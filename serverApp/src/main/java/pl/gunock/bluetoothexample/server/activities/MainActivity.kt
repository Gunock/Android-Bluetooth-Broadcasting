package pl.gunock.bluetoothexample.server.activities

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
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
import kotlinx.coroutines.launch
import pl.gunock.bluetoothexample.server.misc.BluetoothServer
import pl.gunock.bluetoothexample.server.databinding.ActivityMainBinding
import pl.gunock.bluetoothexample.server.databinding.ContentMainBinding


class MainActivity : AppCompatActivity() {
    private companion object {
        const val TAG = "MainActivity"

        const val BT_PERMISSION = 1

        const val REQUEST_ENABLE_BT = 1
        const val REQUEST_DISCOVERABLE = 2
    }

    private lateinit var mBinding: ContentMainBinding

    private var mBluetoothAdapter: BluetoothAdapter? = null
    private var mBluetoothServer: BluetoothServer? = null


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
        setUpListeners()
        setUpBluetooth()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
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

    private fun setUpBluetooth() {
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

        val discoverableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE)
        intent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 600)
        startActivityForResult(discoverableIntent, REQUEST_DISCOVERABLE)

        mBluetoothServer = BluetoothServer(mBluetoothAdapter!!).apply {
            onConnectCallback = {
                Toast.makeText(
                    baseContext,
                    "${it.remoteDevice.name} has connected",
                    Toast.LENGTH_SHORT
                ).show()
            }
            onDisconnectCallback = {
                Toast.makeText(
                    baseContext,
                    "${it.remoteDevice.name} has disconnected",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun setUpListeners() {
        mBinding.btnServer.setOnClickListener {
            lifecycleScope.launch(Dispatchers.Main) {
                Log.i(TAG, "Started listening")
                mBluetoothServer!!.acceptConnection()
            }
        }

        mBinding.btnSendMessage.setOnClickListener {
            val message = mBinding.edMessage.text.toString()
            mBluetoothServer!!.broadcastMessage(message)
        }
    }
}

