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
import androidx.activity.result.ActivityResult
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import pl.gunock.bluetoothexample.common.bluetooth.BluetoothServer
import pl.gunock.bluetoothexample.server.R
import pl.gunock.bluetoothexample.server.databinding.ActivityServerMainBinding
import pl.gunock.bluetoothexample.server.databinding.ContentServerMainBinding
import pl.gunock.bluetoothexample.server.registerForActivityResult
import java.util.*


class MainActivity : AppCompatActivity() {
    private companion object {
        const val TAG = "MainActivity"

        const val BT_PERMISSION_RESULT_CODE = 1

        const val SERVICE_NAME = "Broadcast Service"
        val SERVICE_UUID: UUID = UUID.fromString("2f58e6c0-5ccf-4d2f-afec-65a2d98e2141")
    }

    private lateinit var mBinding: ContentServerMainBinding

    private var mBluetoothAdapter: BluetoothAdapter? = null
    private lateinit var mBluetoothServer: BluetoothServer

    private val mDiscoverableActivityResultLauncher =
        registerForActivityResult { result: ActivityResult ->
            if (result.resultCode == RESULT_CANCELED) {
                Log.d(TAG, "User refused REQUEST_DISCOVERABLE")
            } else {
                lifecycleScope.launch(Dispatchers.IO) {
                    mBluetoothServer.stop()
                    mBluetoothServer.startLoop()
                }
            }
        }

    private val mEnableBluetoothActivityResultLauncher =
        registerForActivityResult { result: ActivityResult ->
            if (result.resultCode == RESULT_CANCELED) {
                Log.d(TAG, "User refused REQUEST_ENABLE_BT")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val rootBinding = ActivityServerMainBinding.inflate(layoutInflater)
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
            requestPermissions(permissions.toTypedArray(), BT_PERMISSION_RESULT_CODE)
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
            val enableBluetoothIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            mEnableBluetoothActivityResultLauncher.launch(enableBluetoothIntent)
        }

        mBluetoothServer = BluetoothServer(
            mBluetoothAdapter!!,
            SERVICE_NAME,
            SERVICE_UUID
        )

        mBluetoothServer.isStopped.observe(this) {
            if (it) {
                mBinding.tvServerStatus.text = getString(R.string.activity_main_server_off)
            } else {
                mBinding.tvServerStatus.text = getString(R.string.activity_main_server_on)
            }
        }

        mBluetoothServer.setOnConnectListener {
            Toast.makeText(
                baseContext,
                "${it.remoteDevice.name} has connected",
                Toast.LENGTH_SHORT
            ).show()
        }

        mBluetoothServer.setOnDisconnectListener {
            Toast.makeText(
                baseContext,
                "${it.remoteDevice.name} has disconnected",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun setUpListeners() {
        mBinding.btnServerStart.setOnClickListener {
            Log.i(TAG, "Started listening")
            val discoverableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE)
            intent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 600)

            mDiscoverableActivityResultLauncher.launch(discoverableIntent)
        }

        mBinding.btnServerStop.setOnClickListener {
            mBluetoothServer.stop()
        }

        mBinding.btnSendMessage.setOnClickListener {
            val message = mBinding.edMessage.text.toString()
            Log.i(TAG, "Sending message : $message")
            mBluetoothServer.broadcastMessage(message)
        }
    }
}

