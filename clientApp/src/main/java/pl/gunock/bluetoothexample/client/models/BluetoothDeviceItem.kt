package pl.gunock.bluetoothexample.client.models

import android.bluetooth.BluetoothDevice

data class BluetoothDeviceItem(
    val bluetoothDevice: BluetoothDevice,
    val isAvailable: Boolean
)
