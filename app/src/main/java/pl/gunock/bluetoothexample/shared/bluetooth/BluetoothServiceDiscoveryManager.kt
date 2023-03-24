package pl.gunock.bluetoothexample.shared.bluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.os.ParcelUuid
import androidx.annotation.RequiresPermission
import kotlinx.coroutines.flow.Flow

interface BluetoothServiceDiscoveryManager {
    companion object {
        const val TAG = "ServiceDiscoveryManager"
    }

    @SuppressLint("InlinedApi")
    @RequiresPermission(anyOf = [Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH])
    fun discoverServicesInDevices(devices: Collection<BluetoothDevice>)

    fun setExpectedUuids(uuids: Collection<ParcelUuid>)

    fun getBroadcastReceiver(): BroadcastReceiver

    fun getBluetoothDevices(): Flow<Set<BluetoothDevice>>

}