package dev.thomas_kiljanczyk.bluetoothbroadcasting.ui.server

import android.Manifest
import android.annotation.SuppressLint
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.lifecycle.ViewModel
import com.google.android.gms.nearby.connection.AdvertisingOptions
import com.google.android.gms.nearby.connection.ConnectionInfo
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback
import com.google.android.gms.nearby.connection.ConnectionResolution
import com.google.android.gms.nearby.connection.ConnectionsClient
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.PayloadCallback
import com.google.android.gms.nearby.connection.PayloadTransferUpdate
import com.google.android.gms.nearby.connection.Strategy
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.thomas_kiljanczyk.bluetoothbroadcasting.ui.shared.Constants
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

    private val _messageFlow: MutableSharedFlow<String> = MutableSharedFlow(replay = 1)
    val messageFlow: Flow<String> = _messageFlow

    private val connectedEndpointIds = mutableSetOf<String>()

    @SuppressLint("InlinedApi")
    @RequiresPermission(anyOf = [Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH])
    fun startServer(deviceName: String) {
        val advertisingOptions = AdvertisingOptions.Builder()
            .setStrategy(Strategy.P2P_STAR)
            .build()

        connectionsClient
            .startAdvertising(
                deviceName,
                Constants.SERVICE_UUID.toString(),
                object : ConnectionLifecycleCallback() {
                    private val endpointMap = mutableMapOf<String, String>()

                    override fun onConnectionInitiated(
                        endpointId: String,
                        connectionInfo: ConnectionInfo
                    ) {
                        val messageText = "${connectionInfo.endpointName} has connected"
                        endpointMap[endpointId] = connectionInfo.endpointName
                        _messageFlow.tryEmit(messageText)
                        connectionsClient.acceptConnection(endpointId, object : PayloadCallback() {
                            override fun onPayloadReceived(endpointId: String, p1: Payload) {
                            }

                            override fun onPayloadTransferUpdate(
                                endpointId: String,
                                payload: PayloadTransferUpdate
                            ) {
                            }
                        })
                    }

                    override fun onConnectionResult(
                        endpointId: String,
                        result: ConnectionResolution
                    ) {
                        if (result.status.isSuccess) {
                            connectedEndpointIds.add(endpointId)
                        } else {
                            endpointMap.remove(endpointId)
                        }
                    }

                    override fun onDisconnected(endpointId: String) {
                        val endpointName = endpointMap[endpointId] ?: "Unknown"

                        // TODO: should be localized
                        val messageText = "$endpointName has disconnected"
                        _messageFlow.tryEmit(messageText)
                        endpointMap.remove(endpointId)
                        connectedEndpointIds.remove(endpointId)
                    }
                },
                advertisingOptions
            )
            .addOnSuccessListener { unused: Void? ->
                _serverStatusFlow.value = true
            }
            .addOnFailureListener { e: Exception? ->
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
        connectionsClient
            .sendPayload(
                connectedEndpointIds.toList(),
                Payload.fromBytes(message.toByteArray())
            )
            .addOnSuccessListener {
                Log.i(TAG, "Message sent")
            }
            .addOnFailureListener { e: Exception? ->
                Log.e(TAG, "Failed to send message", e)
            }
    }

}