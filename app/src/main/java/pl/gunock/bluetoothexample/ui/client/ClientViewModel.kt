package pl.gunock.bluetoothexample.ui.client

import android.bluetooth.BluetoothDevice
import android.util.Log
import androidx.lifecycle.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import pl.gunock.bluetoothexample.R
import pl.gunock.bluetoothexample.bluetooth.BluetoothClient
import java.util.*

class ClientViewModel : ViewModel() {
    companion object {
        private const val TAG = "ClientViewModel"

        val SERVICE_UUID: UUID = UUID.fromString("2f58e6c0-5ccf-4d2f-afec-65a2d98e2141")
    }

    private var bluetoothClient: BluetoothClient? = null

    private val _clientStatus: MutableLiveData<Pair<Int, String?>> =
        MutableLiveData(Pair(R.string.activity_client_disconnected, null))
    val clientStatus: LiveData<Pair<Int, String?>> = _clientStatus

    private val _receivedText: MutableLiveData<String> = MutableLiveData()
    val receivedText: LiveData<String> = _receivedText

    private val _message: MutableLiveData<String> = MutableLiveData()
    val message: LiveData<String> = _message


    fun setClient(device: BluetoothDevice) {
        val bluetoothClient = BluetoothClient(
            device,
            SERVICE_UUID
        ) {
            val text = it.decodeToString()
            Log.i(TAG, "Received message '$text'")
            _receivedText.postValue(text)
        }

        bluetoothClient.setOnConnectionSuccessListener {
            _clientStatus.postValue(Pair(R.string.activity_client_connected, it.remoteDevice.name))
        }

        bluetoothClient.setOnConnectionFailureListener {
            _clientStatus.postValue(Pair(R.string.activity_client_disconnected, null))
        }

        bluetoothClient.setOnDisconnectionListener {
            _clientStatus.postValue(Pair(R.string.activity_client_disconnected, null))
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