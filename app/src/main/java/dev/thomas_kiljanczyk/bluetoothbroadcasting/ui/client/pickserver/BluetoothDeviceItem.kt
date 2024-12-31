package dev.thomas_kiljanczyk.bluetoothbroadcasting.ui.client.pickserver

data class BluetoothDeviceItem(
    val deviceName: String,
    val endpointId: String,
    val isAvailable: Boolean
) {
    override fun equals(other: Any?): Boolean {
        if (other == null || other !is BluetoothDeviceItem) {
            return false
        }

        return endpointId == other.endpointId
    }

    override fun hashCode(): Int {
        return endpointId.hashCode()
    }
}
