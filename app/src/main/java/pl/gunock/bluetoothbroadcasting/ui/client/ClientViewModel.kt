package pl.gunock.bluetoothbroadcasting.ui.client

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
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
import pl.gunock.bluetoothbroadcasting.R
import pl.gunock.bluetoothbroadcasting.lib.BluetoothClient
import java.util.*
import javax.inject.Inject

@HiltViewModel
class ClientViewModel @Inject constructor() : ViewModel() {
    companion object {
        private const val TAG = "ClientViewModel"

        val SERVICE_UUID: UUID = UUID.fromString("2f58e6c0-5ccf-4d2f-afec-65a2d98e2141")
    }

    private var bluetoothClient: BluetoothClient? = null

    private val _clientStatus: MutableStateFlow<Pair<Int, String?>> =
        MutableStateFlow(Pair(R.string.activity_client_disconnected, null))
    val clientStatus: StateFlow<Pair<Int, String?>> = _clientStatus

    private val _receivedText: MutableSharedFlow<String> = MutableSharedFlow(replay = 1)
    val receivedText: Flow<String> = _receivedText

    @SuppressLint("InlinedApi")
    @RequiresPermission(anyOf = [Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH])
    fun setClient(device: BluetoothDevice) {
        val bluetoothSocket = device.createRfcommSocketToServiceRecord(SERVICE_UUID)
        val bluetoothClient = BluetoothClient(bluetoothSocket)

        bluetoothClient.setOnDataListener {
            val text = it.decodeToString()
            Log.i(TAG, "Received message '$text'")
            _receivedText.tryEmit(text)
        }

        bluetoothClient.setOnConnectionSuccessListener {
            _clientStatus.value = Pair(R.string.activity_client_connected, it.remoteDevice.name)
        }

        bluetoothClient.setOnConnectionFailureListener {
            _clientStatus.value = Pair(R.string.activity_client_disconnected, null)
        }

        bluetoothClient.setOnDisconnectionListener {
            _clientStatus.value = Pair(R.string.activity_client_disconnected, null)
        }

        this.bluetoothClient = bluetoothClient
    }

    @SuppressLint("InlinedApi")
    @RequiresPermission(anyOf = [Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH])
    fun startClient() {
        viewModelScope.launch(Dispatchers.IO) {
            Log.i(TAG, "Started listening")
            bluetoothClient?.startLoop()
        }
    }

    fun stopClient() {
        viewModelScope.launch(Dispatchers.Default) { bluetoothClient?.disconnect() }
        Log.i(TAG, "Client disconnected")
    }

}