package pl.gunock.bluetoothexample.ui.pickserver

import android.bluetooth.BluetoothDevice

data class BluetoothDeviceItem(
    val bluetoothDevice: BluetoothDevice,
    val isAvailable: Boolean
) {
    override fun equals(other: Any?): Boolean {
        if (other == null || other !is BluetoothDeviceItem) {
            return false
        }

        return bluetoothDevice == other.bluetoothDevice
    }

    override fun hashCode(): Int {
        return bluetoothDevice.hashCode()
    }
}
