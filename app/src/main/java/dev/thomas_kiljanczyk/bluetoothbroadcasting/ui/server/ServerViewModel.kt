package dev.thomas_kiljanczyk.bluetoothbroadcasting.ui.server

import android.Manifest
import android.annotation.SuppressLint
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.lifecycle.ViewModel
import com.google.android.gms.nearby.connection.AdvertisingOptions
import com.google.android.gms.nearby.connection.ConnectionInfo
import com.google.android.gms.nearby.connection.ConnectionResolution
import com.google.android.gms.nearby.connection.ConnectionsClient
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.Strategy
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.thomas_kiljanczyk.bluetoothbroadcasting.R
import dev.thomas_kiljanczyk.bluetoothbroadcasting.ui.shared.Constants
import dev.thomas_kiljanczyk.bluetoothbroadcasting.ui.shared.NearbyConnectionLifecycleCallback
import dev.thomas_kiljanczyk.bluetoothbroadcasting.ui.shared.SimpleNearbyPayloadCallback
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject


@HiltViewModel
class ServerViewModel @Inject constructor(
    private val connectionsClient: ConnectionsClient
) : ViewModel() {
    companion object {
        const val TAG = "ServerViewModel"
    }

    private val _serverStatusFlow: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val serverOnFlow: StateFlow<Boolean> = _serverStatusFlow

    private val _messageFlow: MutableSharedFlow<Pair<Int, String?>> = MutableSharedFlow(replay = 1)
    val messageFlow: Flow<Pair<Int, String?>> = _messageFlow
    private val connectedEndpointIds = mutableSetOf<String>()

    private inner class ServerConnectionLifecycleCallback : NearbyConnectionLifecycleCallback() {
        override fun onConnectionInitiated(
            endpointId: String, connectionInfo: ConnectionInfo
        ) {
            super.onConnectionInitiated(endpointId, connectionInfo)
            connectionsClient.acceptConnection(endpointId, SimpleNearbyPayloadCallback {})
        }

        override fun onConnectionResult(
            endpointId: String, connectionInfo: ConnectionInfo?, result: ConnectionResolution
        ) {
            if (result.status.isSuccess) {
                val endpointName = connectionInfo?.endpointName
                val messageResId =
                    if (endpointName != null) R.string.activity_server_connected else R.string.activity_server_connected_unknown
                _messageFlow.tryEmit(Pair(messageResId, endpointName))

                connectedEndpointIds.add(endpointId)
            }
        }

        override fun onDisconnected(endpointId: String, connectionInfo: ConnectionInfo?) {
            val endpointName = connectionInfo?.endpointName
            val messageResId =
                if (endpointName != null) R.string.activity_server_disconnected else R.string.activity_server_disconnected_unknown

            _messageFlow.tryEmit(Pair(messageResId, endpointName))
            connectedEndpointIds.remove(endpointId)
        }
    }

    @SuppressLint("InlinedApi")
    @RequiresPermission(anyOf = [Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH])
    fun startServer(deviceName: String) {
        val advertisingOptions = AdvertisingOptions.Builder().setStrategy(Strategy.P2P_STAR).build()

        connectionsClient.startAdvertising(
            deviceName,
            Constants.SERVICE_UUID.toString(),
            ServerConnectionLifecycleCallback(),
            advertisingOptions
        ).addOnSuccessListener { unused: Void? ->
            _serverStatusFlow.value = true
        }.addOnFailureListener { e: Exception? ->
            Log.e(TAG, "Failed to start server", e)
            _serverStatusFlow.value = false
        }
    }


    fun stopServer() {
        connectionsClient.stopAdvertising()
        connectionsClient.stopAllEndpoints()
        _serverStatusFlow.value = false
    }

    fun broadcastMessage(message: String) {
        Log.i(TAG, "Sending message : $message")
        connectionsClient.sendPayload(
            connectedEndpointIds.toList(), Payload.fromBytes(message.toByteArray())
        ).addOnSuccessListener {
            Log.i(TAG, "Message sent")
        }.addOnFailureListener { e: Exception? ->
            Log.e(TAG, "Failed to send message", e)
        }
    }

}