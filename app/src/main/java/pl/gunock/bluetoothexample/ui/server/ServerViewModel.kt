package pl.gunock.bluetoothexample.ui.server

import android.bluetooth.BluetoothAdapter
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import pl.gunock.bluetoothexample.R
import pl.gunock.bluetoothexample.bluetooth.BluetoothServer
import java.util.*
import javax.inject.Inject

@HiltViewModel
class ServerViewModel @Inject constructor() : ViewModel() {
    private companion object {
        const val TAG = "ServerViewModel"

        const val SERVICE_NAME = "Broadcast Service"
        val SERVICE_UUID: UUID = UUID.fromString("2f58e6c0-5ccf-4d2f-afec-65a2d98e2141")
    }

    private var server: BluetoothServer? = null

    private val _serverStatus: MutableStateFlow<Int> =
        MutableStateFlow(R.string.activity_server_server_off)
    val serverStatus: StateFlow<Int> = _serverStatus

    private val _message: MutableSharedFlow<String> = MutableSharedFlow(replay = 1)
    val message: Flow<String> = _message

    fun setServer(bluetoothAdapter: BluetoothAdapter) {
        val bluetoothServer = BluetoothServer(
            bluetoothAdapter,
            SERVICE_NAME,
            SERVICE_UUID
        )

        bluetoothServer.setOnConnectListener {
            val messageText = "${it.remoteDevice.name} has connected"
            _message.tryEmit(messageText)
        }

        bluetoothServer.setOnDisconnectListener {
            val messageText = "${it.remoteDevice.name} has disconnected"
            _message.tryEmit(messageText)
        }

        bluetoothServer.setOnStateChangeListener { isStopped ->
            if (isStopped) {
                _serverStatus.value = R.string.activity_server_server_off
            } else {
                _serverStatus.value = R.string.activity_server_server_on
            }
        }

        server = bluetoothServer
    }


    fun startServer() {
        viewModelScope.launch(Dispatchers.IO) {
            server?.apply {
                stop()
                startLoop()
            }
        }
    }

    fun stopServer() {
        server?.apply {
            stop()
        }
    }

    fun broadcastMessage(message: String) {
        server?.apply {
            Log.i(TAG, "Sending message : $message")
            broadcastMessage(message)
        } ?: Log.i(TAG, "Cannot send message, server is null")
    }

}