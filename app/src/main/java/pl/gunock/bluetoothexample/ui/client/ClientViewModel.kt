package pl.gunock.bluetoothexample.ui.client

import android.bluetooth.BluetoothDevice
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
import pl.gunock.bluetoothexample.bluetooth.BluetoothClient
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

    private val _receivedText: MutableSharedFlow<String> = MutableSharedFlow()
    val receivedText: Flow<String> = _receivedText

    fun setClient(device: BluetoothDevice) {
        val bluetoothClient = BluetoothClient(
            device,
            SERVICE_UUID
        ) {
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