package pl.gunock.bluetoothexample.ui.server

import android.bluetooth.BluetoothAdapter
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import pl.gunock.bluetoothexample.R
import pl.gunock.bluetoothexample.bluetooth.BluetoothServer
import java.util.*

class ServerViewModel : ViewModel() {
    private companion object {
        const val TAG = "ServerViewModel"

        const val SERVICE_NAME = "Broadcast Service"
        val SERVICE_UUID: UUID = UUID.fromString("2f58e6c0-5ccf-4d2f-afec-65a2d98e2141")
    }

    private var mServer: BluetoothServer? = null

    private val mServerStatus: MutableLiveData<Int> = MutableLiveData(R.string.activity_server_server_off)
    val serverStatus: LiveData<Int> = mServerStatus

    private val mMessage: MutableLiveData<String> = MutableLiveData()
    val message: LiveData<String> = mMessage

    fun setServer(bluetoothAdapter: BluetoothAdapter) {
        val bluetoothServer = BluetoothServer(
            bluetoothAdapter,
            SERVICE_NAME,
            SERVICE_UUID
        )

        bluetoothServer.setOnConnectListener {
            val messageText = "${it.remoteDevice.name} has connected"
            mMessage.postValue(messageText)
        }

        bluetoothServer.setOnDisconnectListener {
            val messageText = "${it.remoteDevice.name} has disconnected"
            mMessage.postValue(messageText)
        }

        bluetoothServer.setOnStateChangeListener { isStopped ->
            if(isStopped){
                mServerStatus.postValue(R.string.activity_server_server_off)
            } else {
                mServerStatus.postValue(R.string.activity_server_server_on)
            }
        }

        mServer = bluetoothServer
    }


    fun startServer() {
        viewModelScope.launch(Dispatchers.IO) {
            mServer?.apply {
                stop()
                startLoop()
            }
        }
    }

    fun stopServer() {
        mServer?.apply {
            stop()
        }
    }

    fun broadcastMessage(message: String) {
        mServer?.apply {
            Log.i(TAG, "Sending message : $message")
            broadcastMessage(message)
        } ?: Log.i(TAG, "Cannot send message, server is null")
    }

}