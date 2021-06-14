package pl.gunock.bluetoothexample.common.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.*
import java.io.IOException
import java.util.*

class BluetoothServer(
    private val mBluetoothAdapter: BluetoothAdapter,
    private val mServiceName: String,
    private val mServiceUUID: UUID
) {
    private companion object {
        const val TAG = "BluetoothClient"
    }

    val isStopped: LiveData<Boolean> get() = mIsStopped


    private var mOnConnectCallback: (suspend (BluetoothSocket) -> Unit)? = null

    private var mOnDisconnectCallback: ((BluetoothSocket) -> Unit)? = null

    private var mServerSocket: BluetoothServerSocket? = null

    private val mClientSockets: MutableList<BluetoothSocket> = mutableListOf()

    private var mIsStopped: MutableLiveData<Boolean> = MutableLiveData(true)


    init {
        CoroutineScope(Dispatchers.Default).launch { monitorConnections() }
    }

    fun setOnConnectListener(listener: (suspend (BluetoothSocket) -> Unit)?) {
        mOnConnectCallback = listener
    }

    fun setOnDisconnectListener(listener: ((BluetoothSocket) -> Unit)?) {
        mOnDisconnectCallback = listener
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    suspend fun startLoop() {
        mServerSocket = mBluetoothAdapter.listenUsingRfcommWithServiceRecord(
            mServiceName,
            mServiceUUID
        )

        mIsStopped.postValue(false)
        do {
            val clientSocket: BluetoothSocket =
                withContext(Dispatchers.IO) { acceptConnection() } ?: continue

            Log.i(TAG, "Connection accepted : ${clientSocket.remoteDevice.name}")

            withContext(Dispatchers.Main) { mOnConnectCallback?.invoke(clientSocket) }
            mClientSockets.add(clientSocket)
        } while (!mIsStopped.value!!)
    }

    fun stop() {
        mClientSockets.forEach { it.close() }
        mClientSockets.clear()
        mServerSocket?.close()
        mIsStopped.postValue(true)
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    fun broadcastMessage(message: String) {
        mClientSockets.forEach { socket ->
            CoroutineScope(Dispatchers.IO).launch {
                Log.i(TAG, "Broadcast message '$message'")

                try {
                    socket.outputStream.write(message.toByteArray())
                    socket.outputStream.flush()
                } catch (ignored: IOException) {
                }
            }
        }
    }

    private fun acceptConnection(): BluetoothSocket? {
        return try {
            mServerSocket!!.accept()
        } catch (e: Exception) {
            when (e) {
                is IOException,
                is NullPointerException -> {
                    Log.e(TAG, "Socket's accept() method failed", e)
                    mIsStopped.postValue(true)
                    null
                }
                else -> throw e
            }
        }
    }

    private suspend fun monitorConnections() {
        while (true) {
            mClientSockets.removeAll { runBlocking { !checkConnectionState(it) } }
            delay(1000)
        }
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    private suspend fun checkConnectionState(socket: BluetoothSocket): Boolean {
        try {
            socket.inputStream.skip(1)
        } catch (ignored: IOException) {
            Log.i(TAG, "Socket connection closed")
            withContext(Dispatchers.Main) { mOnDisconnectCallback?.invoke(socket) }
            return false
        }
        return true
    }

}