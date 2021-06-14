package pl.gunock.bluetoothexample.common.bluetooth

import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.ParcelUuid
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import pl.gunock.bluetoothexample.client.extensions.order
import java.nio.ByteOrder

class BluetoothServiceDiscoveryManager(
    private val mExpectedUuids: Collection<ParcelUuid>
) {
    private companion object {
        const val TAG = "ServiceDiscoveryManager"
    }

    val devices: LiveData<Set<BluetoothDevice>> get() = mDevices

    private val mDevices: MutableLiveData<Set<BluetoothDevice>> = MutableLiveData(setOf())

    val receiver: BroadcastReceiver = ServiceDiscoveryBroadcastReceiver()

    fun discoverServicesInDevices(devices: Collection<BluetoothDevice>) {
        devices.forEach { it.fetchUuidsWithSdp() }
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

            // This is a workaround for bluetooth problem in android 6.0.1
            // https://issuetracker.google.com/issues/37075233
            val uuids: Array<ParcelUuid> = if (Build.VERSION.SDK_INT == Build.VERSION_CODES.M) {
                deviceExtra.uuids
                    .map { it.order(ByteOrder.LITTLE_ENDIAN) }
                    .toTypedArray()
            } else {
                deviceExtra.uuids
            }

            val hasService = uuids.any { it in mExpectedUuids }

            Log.d(TAG, "${deviceExtra.name} : ${uuids.map { it.uuid }}")
            Log.d(TAG, "${deviceExtra.name} : $hasService")
            val newDevices = if (hasService) {
                mDevices.value!! + deviceExtra
            } else {
                mDevices.value!! - deviceExtra
            }
            mDevices.postValue(newDevices)
        }
    }

}