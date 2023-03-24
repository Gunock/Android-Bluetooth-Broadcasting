package pl.gunock.bluetoothexample.shared.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.util.Log
import kotlinx.coroutines.*
import java.io.IOException
import java.util.*

class BluetoothServer(
    private val bluetoothAdapter: BluetoothAdapter,
    private val serviceName: String,
    private val serviceUUID: UUID
) {
    private companion object {
        const val TAG = "BluetoothClient"
    }

    private var onConnectListener: (suspend (BluetoothSocket) -> Unit)? = null

    private var onDisconnectListener: ((BluetoothSocket) -> Unit)? = null

    private var onStateChangeListener: ((Boolean) -> Unit)? = null

    private var serverSocket: BluetoothServerSocket? = null

    private val clientSockets: MutableList<BluetoothSocket> = mutableListOf()

    private var _isStopped: Boolean = true
    private var isStopped: Boolean
        get() = _isStopped
        set(value) {
            onStateChangeListener?.invoke(value)
            _isStopped = value
        }

    init {
        CoroutineScope(Dispatchers.Default).launch { monitorConnections() }
    }

    fun setOnConnectListener(listener: (suspend (BluetoothSocket) -> Unit)?) {
        onConnectListener = listener
    }

    fun setOnDisconnectListener(listener: ((BluetoothSocket) -> Unit)?) {
        onDisconnectListener = listener
    }

    fun setOnStateChangeListener(listener: ((Boolean) -> Unit)?) {
        onStateChangeListener = listener
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    suspend fun startLoop() {
        serverSocket = bluetoothAdapter
            .listenUsingRfcommWithServiceRecord(serviceName, serviceUUID)

        isStopped = false

        Log.i(TAG, "Server loop started")
        do {
            Log.v(TAG, "Server loop next iteration")
            val clientSocket: BluetoothSocket =
                withContext(Dispatchers.IO) { acceptConnection() } ?: continue

            Log.i(TAG, "Connection accepted : ${clientSocket.remoteDevice.name}")

            withContext(Dispatchers.Main) { onConnectListener?.invoke(clientSocket) }
            delay(1000)
            clientSockets.add(clientSocket)
        } while (!isStopped)
    }

    fun stop() {
        isStopped = true
        clientSockets.forEach { it.close() }
        clientSockets.clear()
        serverSocket?.close()
    }

    fun broadcastMessage(message: String) {
        clientSockets.forEach { socket ->
            CoroutineScope(Dispatchers.IO).launch { sendMessage(socket, message) }
        }
    }

    private fun sendMessage(socket: BluetoothSocket, message: String) {
        Log.i(TAG, "Broadcast message '$message'")

        val nullTerminatedMessage = (message + 4.toChar()).toByteArray()
        try {
            socket.outputStream.write(nullTerminatedMessage)
            socket.outputStream.flush()
        } catch (ignored: IOException) {
        }
    }

    private fun acceptConnection(): BluetoothSocket? {
        return try {
            serverSocket!!.accept()
        } catch (e: Exception) {
            when (e) {
                is IOException,
                is NullPointerException -> {
                    Log.w(TAG, "Socket's accept() method failed", e)
                    isStopped = true

                    null
                }
                else -> throw e
            }
        }
    }

    private suspend fun monitorConnections() {
        while (true) {
            clientSockets.removeAll { runBlocking { !checkConnectionState(it) } }
            delay(1000)
        }
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    private suspend fun checkConnectionState(socket: BluetoothSocket): Boolean {
        try {
            socket.inputStream.skip(1)
        } catch (ignored: IOException) {
            Log.i(TAG, "Socket connection closed")
            socket.close()
            withContext(Dispatchers.Main) { onDisconnectListener?.invoke(socket) }
            return false
        }
        return true
    }

}