package dev.thomas_kiljanczyk.bluetoothbroadcasting.ui.shared

import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.PayloadCallback
import com.google.android.gms.nearby.connection.PayloadTransferUpdate

class SimpleNearbyPayloadCallback(
    private val onPayloadReceived: (ByteArray?) -> Unit
) : PayloadCallback() {
    override fun onPayloadReceived(
        endpointId: String,
        payload: Payload
    ) {
        onPayloadReceived(payload.asBytes())
    }

    override fun onPayloadTransferUpdate(
        endpointId: String,
        payload: PayloadTransferUpdate
    ) {
    }
}