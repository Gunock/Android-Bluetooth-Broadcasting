package pl.gunock.bluetoothexample.shared.bluetooth

import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.os.ParcelUuid
import kotlinx.coroutines.flow.Flow

interface BluetoothServiceDiscoveryManager {
    companion object {
        const val TAG = "ServiceDiscoveryManager"
    }

    fun discoverServicesInDevices(devices: Collection<BluetoothDevice>)

    fun setExpectedUuids(uuids: Collection<ParcelUuid>)

    fun getBroadcastReceiver(): BroadcastReceiver

    fun getBluetoothDevices(): Flow<Set<BluetoothDevice>>

}