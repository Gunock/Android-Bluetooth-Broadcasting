package pl.gunock.bluetoothbroadcasting.lib

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.ParcelUuid
import android.util.Log
import androidx.annotation.RequiresPermission
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import pl.gunock.bluetoothbroadcasting.lib.BluetoothServiceDiscoveryManager.Companion.TAG
import pl.gunock.bluetoothbroadcasting.lib.extensions.order
import java.nio.ByteOrder
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

// TODO: Add support for companion device pairing
// https://developer.android.com/guide/topics/connectivity/companion-device-pairing
class BluetoothServiceDiscoveryManagerImpl(
    private val context: Context,
) : BluetoothServiceDiscoveryManager {

    private val receiver: BroadcastReceiver = ServiceDiscoveryBroadcastReceiver()

    private val bluetoothDevices: MutableStateFlow<Set<BluetoothDevice>> = MutableStateFlow(setOf())

    private val expectedUuidsLock = ReentrantReadWriteLock(true)

    private var expectedUuids: Collection<ParcelUuid> = listOf()

    @SuppressLint("InlinedApi")
    @RequiresPermission(anyOf = [Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH])
    override fun discoverServicesInDevices(devices: Collection<BluetoothDevice>) {
        if (devices.isEmpty()) {
            return
        }

        devices.forEach { fetchDevicesUuidsWithSdp(it) }
    }

    override fun setExpectedUuids(uuids: Collection<ParcelUuid>) {
        expectedUuidsLock.write {
            expectedUuids = uuids
        }
    }

    override fun getBroadcastReceiver(): BroadcastReceiver {
        return receiver
    }

    override fun getBluetoothDevices(): Flow<Set<BluetoothDevice>> {
        return bluetoothDevices
    }

    @SuppressLint("InlinedApi")
    @RequiresPermission(anyOf = [Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH])
    private fun fetchDevicesUuidsWithSdp(bluetoothDevice: BluetoothDevice) {
        val callback = object : BluetoothGattCallback() {
            @SuppressLint("MissingPermission")
            override fun onConnectionStateChange(
                gatt: BluetoothGatt?,
                status: Int,
                newState: Int
            ) {
                super.onConnectionStateChange(gatt, status, newState)

                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    Log.i(TAG, "${bluetoothDevice.name} is reachable")
                    bluetoothDevice.fetchUuidsWithSdp()
                } else {
                    Log.i(TAG, "${bluetoothDevice.name} is unreachable")
                }
            }
        }

        val gatt = bluetoothDevice.connectGatt(context, false, callback)
        gatt.connect()
    }

    private inner class ServiceDiscoveryBroadcastReceiver : BroadcastReceiver() {
        @SuppressLint("InlinedApi")
        @RequiresPermission(anyOf = [Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH])
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                BluetoothDevice.ACTION_UUID -> handleActionUuid(intent)
            }
        }

        @SuppressLint("InlinedApi")
        @RequiresPermission(anyOf = [Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH])
        private fun handleActionUuid(intent: Intent) {
            val deviceExtra: BluetoothDevice =
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)!!
                } else {
                    intent.getParcelableExtra(
                        BluetoothDevice.EXTRA_DEVICE,
                        BluetoothDevice::class.java
                    )!!
                }

            // This is a workaround for bluetooth problem in android 6.0.1 and 7
            // https://issuetracker.google.com/issues/37075233
            val buggyAndroidVersion = Build.VERSION.SDK_INT <= Build.VERSION_CODES.N

            val uuids: Array<ParcelUuid> = if (buggyAndroidVersion) {
                deviceExtra.uuids
                    .map { it.order(ByteOrder.LITTLE_ENDIAN) }
                    .toTypedArray()
            } else {
                deviceExtra.uuids
            }

            val hasService = expectedUuidsLock.read {
                uuids.any { it in expectedUuids }
            }

            Log.d(TAG, "${deviceExtra.name} : ${uuids.map { it.uuid }}")
            Log.d(TAG, "${deviceExtra.name} : $hasService")

            if (hasService) {
                bluetoothDevices.value += deviceExtra
            } else {
                bluetoothDevices.value -= deviceExtra
            }
        }
    }

}