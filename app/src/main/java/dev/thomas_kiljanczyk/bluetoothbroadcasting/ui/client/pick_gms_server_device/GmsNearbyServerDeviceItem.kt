package dev.thomas_kiljanczyk.bluetoothbroadcasting.ui.client.pick_gms_server_device

data class GmsNearbyServerDeviceItem(
    val deviceName: String,
    val endpointId: String
) {
    override fun equals(other: Any?): Boolean {
        if (other == null || other !is GmsNearbyServerDeviceItem) {
            return false
        }

        return endpointId == other.endpointId
    }

    override fun hashCode(): Int {
        return endpointId.hashCode()
    }
}
