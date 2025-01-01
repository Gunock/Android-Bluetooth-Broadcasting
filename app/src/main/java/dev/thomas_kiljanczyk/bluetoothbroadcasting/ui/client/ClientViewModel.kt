package dev.thomas_kiljanczyk.bluetoothbroadcasting.ui.client

import android.Manifest
import android.annotation.SuppressLint
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.lifecycle.ViewModel
import com.google.android.gms.nearby.connection.ConnectionInfo
import com.google.android.gms.nearby.connection.ConnectionResolution
import com.google.android.gms.nearby.connection.ConnectionsClient
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.thomas_kiljanczyk.bluetoothbroadcasting.R
import dev.thomas_kiljanczyk.bluetoothbroadcasting.ui.shared.NearbyConnectionLifecycleCallback
import dev.thomas_kiljanczyk.bluetoothbroadcasting.ui.shared.SimpleNearbyPayloadCallback
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

    private inner class ClientConnectionLifecycleCallback : NearbyConnectionLifecycleCallback() {
        override fun onConnectionInitiated(
            endpointId: String, connectionInfo: ConnectionInfo
        ) {
            super.onConnectionInitiated(endpointId, connectionInfo)
            connectionsClient.acceptConnection(endpointId, SimpleNearbyPayloadCallback { payload ->
                _receivedText.tryEmit(payload?.decodeToString() ?: "")
            })
        }

        override fun onConnectionResult(
            endpointId: String, connectionInfo: ConnectionInfo?, result: ConnectionResolution
        ) {
            if (result.status.isSuccess) {
                val endpointName = connectionInfo?.endpointName
                _clientStatus.value = Pair(
                    if (endpointName != null) R.string.activity_client_connected
                    else R.string.activity_client_connected_unknown, endpointName
                )
            } else {
                _clientStatus.value = Pair(R.string.activity_client_disconnected, null)
            }
        }

        override fun onDisconnected(endpointId: String, connectionInfo: ConnectionInfo?) {
            connectionsClient.disconnectFromEndpoint(endpointId)
            _clientStatus.value = Pair(R.string.activity_client_disconnected, null)
        }
    }

    @SuppressLint("InlinedApi")
    @RequiresPermission(anyOf = [Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH])
    fun startClient(endpointId: String, deviceName: String) {
        connectionsClient.requestConnection(
            deviceName, endpointId, ClientConnectionLifecycleCallback()
        )
    }

    fun stopClient() {
        connectionsClient.stopAllEndpoints()
        Log.i(TAG, "Client disconnected")
    }

}