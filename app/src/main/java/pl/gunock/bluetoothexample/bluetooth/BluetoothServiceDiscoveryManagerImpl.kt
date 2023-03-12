package pl.gunock.bluetoothexample.bluetooth

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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import pl.gunock.bluetoothexample.bluetooth.BluetoothServiceDiscoveryManager.Companion.TAG
import pl.gunock.bluetoothexample.extensions.order
import java.nio.ByteOrder
import java.util.concurrent.locks.ReentrantLock

class BluetoothServiceDiscoveryManagerImpl(
    private val context: Context,
) : BluetoothServiceDiscoveryManager {

    private val receiver: BroadcastReceiver = ServiceDiscoveryBroadcastReceiver()

    private val bluetoothDevices: MutableStateFlow<Set<BluetoothDevice>> = MutableStateFlow(setOf())

    private val devicesToFetch: MutableList<BluetoothDevice> = mutableListOf()

    private val expectedUuidsLock = ReentrantLock()

    private var expectedUuids: Collection<ParcelUuid> = listOf()

    override fun discoverServicesInDevices(devices: Collection<BluetoothDevice>) {
        if (devices.isEmpty()) {
            return
        }

        val deviceList = devices.toMutableList()
        devicesToFetch.clear()
        devicesToFetch.addAll(deviceList)

        val firstDevice: BluetoothDevice = devicesToFetch.removeFirst()
        fetchDevicesUuidsWithSdp(firstDevice)
    }

    override fun setExpectedUuids(uuids: Collection<ParcelUuid>) {
        expectedUuidsLock.lock()
        try {
            expectedUuids = uuids
        } finally {
            expectedUuidsLock.unlock()
        }
    }

    override fun getBroadcastReceiver(): BroadcastReceiver {
        return receiver
    }

    override fun getBluetoothDevices(): Flow<Set<BluetoothDevice>> {
        return bluetoothDevices
    }

    private fun fetchDevicesUuidsWithSdp(bluetoothDevice: BluetoothDevice?) {
        bluetoothDevice ?: return

        val callback = object : BluetoothGattCallback() {
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
                    val nextDevice = devicesToFetch.removeFirstOrNull()
                    fetchDevicesUuidsWithSdp(nextDevice)
                }
            }
        }

        val gatt = bluetoothDevice.connectGatt(context, false, callback)
        gatt.connect()
    }

    private inner class ServiceDiscoveryBroadcastReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                BluetoothDevice.ACTION_UUID -> handleActionUuid(intent)
            }
        }

        private fun handleActionUuid(intent: Intent) {
            val deviceExtra: BluetoothDevice =
                intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)!!

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

            expectedUuidsLock.lock()
            val hasService = try {
                uuids.any { it in expectedUuids }
            } finally {
                expectedUuidsLock.unlock()
            }

            Log.d(TAG, "${deviceExtra.name} : ${uuids.map { it.uuid }}")
            Log.d(TAG, "${deviceExtra.name} : $hasService")
            val newDevices = if (hasService) {
                bluetoothDevices.value + deviceExtra
            } else {
                bluetoothDevices.value - deviceExtra
            }
            bluetoothDevices.value = newDevices

            val nextDevice = devicesToFetch.removeFirstOrNull()
            fetchDevicesUuidsWithSdp(nextDevice)
        }
    }

}