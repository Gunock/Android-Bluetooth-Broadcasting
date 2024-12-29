package dev.thomas_kiljanczyk.bluetoothbroadcasting.ui.client.pickserver

data class BluetoothDeviceItem(
    val deviceName: String,
    val deviceAddress: String,
    val isAvailable: Boolean
) {
    override fun equals(other: Any?): Boolean {
        if (other == null || other !is BluetoothDeviceItem) {
            return false
        }

        return deviceAddress == other.deviceAddress
    }

    override fun hashCode(): Int {
        return deviceAddress.hashCode()
    }
}
