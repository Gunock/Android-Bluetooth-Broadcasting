package pl.gunock.bluetoothexample.bluetooth

import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.os.ParcelUuid
import androidx.lifecycle.MutableLiveData

interface BluetoothServiceDiscoveryManager {
    companion object {
        const val TAG = "ServiceDiscoveryManager"
    }

    fun discoverServicesInDevices(devices: Collection<BluetoothDevice>)

    fun setExpectedUuids(uuids: Collection<ParcelUuid>)

    fun getBroadcastReceiver(): BroadcastReceiver

    fun getBluetoothDevices(): MutableLiveData<Set<BluetoothDevice>>

}