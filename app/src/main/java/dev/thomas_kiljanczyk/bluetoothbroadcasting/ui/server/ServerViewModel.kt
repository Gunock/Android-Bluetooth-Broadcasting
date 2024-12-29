package dev.thomas_kiljanczyk.bluetoothbroadcasting.ui.server

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import dev.thomas_kiljanczyk.bluetoothbroadcasting.lib.BluetoothServer
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

    private val _serverStatusFlow: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val serverOnFlow: StateFlow<Boolean> = _serverStatusFlow

    private val _messageFlow: MutableSharedFlow<String> = MutableSharedFlow(replay = 1)
    val messageFlow: Flow<String> = _messageFlow

    @SuppressLint("InlinedApi")
    @RequiresPermission(anyOf = [Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH])
    fun setServer(bluetoothAdapter: BluetoothAdapter) {
        val bluetoothServer = BluetoothServer(
            bluetoothAdapter,
            SERVICE_NAME,
            SERVICE_UUID
        )

        bluetoothServer.setOnConnectListener {
            val messageText = "${it.remoteDevice.name} has connected"
            _messageFlow.tryEmit(messageText)
        }

        bluetoothServer.setOnDisconnectListener {
            val messageText = "${it.remoteDevice.name} has disconnected"
            _messageFlow.tryEmit(messageText)
        }

        bluetoothServer.setOnStateChangeListener { isStopped ->
            _serverStatusFlow.value = !isStopped
        }

        server = bluetoothServer
    }

    @SuppressLint("InlinedApi")
    @RequiresPermission(anyOf = [Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH])
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