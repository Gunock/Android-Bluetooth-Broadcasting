package dev.thomas_kiljanczyk.bluetoothbroadcasting.ui.client

import android.Manifest
import android.annotation.SuppressLint
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.lifecycle.ViewModel
import com.google.android.gms.nearby.connection.ConnectionInfo
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback
import com.google.android.gms.nearby.connection.ConnectionResolution
import com.google.android.gms.nearby.connection.ConnectionsClient
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.PayloadCallback
import com.google.android.gms.nearby.connection.PayloadTransferUpdate
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.thomas_kiljanczyk.bluetoothbroadcasting.R
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject


@HiltViewModel
class ClientViewModel @Inject constructor(
    private val connectionsClient: ConnectionsClient
) : ViewModel() {
    companion object {
        private const val TAG = "ClientViewModel"
    }

    private val _clientStatus: MutableStateFlow<Pair<Int, String?>> =
        MutableStateFlow(Pair(R.string.activity_client_disconnected, null))
    val clientStatus: StateFlow<Pair<Int, String?>> = _clientStatus

    private val _receivedText: MutableSharedFlow<String> = MutableSharedFlow(replay = 1)
    val receivedText: Flow<String> = _receivedText

    @SuppressLint("InlinedApi")
    @RequiresPermission(anyOf = [Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH])
    fun startClient(endpointId: String, deviceName: String) {
        connectionsClient.requestConnection(
            deviceName,
            endpointId,
            object : ConnectionLifecycleCallback() {
                var connectionInfo: ConnectionInfo? = null

                override fun onConnectionInitiated(
                    endpointId: String,
                    info: ConnectionInfo
                ) {
                    connectionInfo = info

                    connectionsClient.acceptConnection(endpointId, object : PayloadCallback() {
                        override fun onPayloadReceived(
                            endpointId: String,
                            payload: Payload
                        ) {
                            // TODO: handle null text
                            val text = payload.asBytes()?.decodeToString()
                            if (text != null) {
                                Log.i(TAG, "Received message '$text'")
                                _receivedText.tryEmit(text)
                            }
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
                        // TODO: replace endpoint id with unknown
                        _clientStatus.value = Pair(
                            R.string.activity_client_connected,
                            connectionInfo?.endpointName ?: endpointId
                        )
                    } else {
                        _clientStatus.value = Pair(R.string.activity_client_disconnected, null)
                        connectionInfo = null
                    }
                }

                override fun onDisconnected(endpointId: String) {
                    connectionsClient.disconnectFromEndpoint(endpointId)
                    _clientStatus.value = Pair(R.string.activity_client_disconnected, null)
                    connectionInfo = null
                }
            })
    }

    fun stopClient() {
        connectionsClient.stopAllEndpoints()
        Log.i(TAG, "Client disconnected")
    }

}