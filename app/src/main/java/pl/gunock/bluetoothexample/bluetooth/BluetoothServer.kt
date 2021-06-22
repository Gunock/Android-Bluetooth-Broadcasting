package pl.gunock.bluetoothexample.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.util.Log
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

    private var mOnConnectListener: (suspend (BluetoothSocket) -> Unit)? = null

    private var mOnDisconnectListener: ((BluetoothSocket) -> Unit)? = null

    private var mOnStateChangeListener: ((Boolean) -> Unit)? = null

    private var mServerSocket: BluetoothServerSocket? = null

    private val mClientSockets: MutableList<BluetoothSocket> = mutableListOf()

    private var mIsStopped: Boolean = true
        set(value) {
            mOnStateChangeListener?.invoke(value)
        }

    init {
        CoroutineScope(Dispatchers.Default).launch { monitorConnections() }
    }

    fun setOnConnectListener(listener: (suspend (BluetoothSocket) -> Unit)?) {
        mOnConnectListener = listener
    }

    fun setOnDisconnectListener(listener: ((BluetoothSocket) -> Unit)?) {
        mOnDisconnectListener = listener
    }

    fun setOnStateChangeListener(listener: ((Boolean) -> Unit)?) {
        mOnStateChangeListener = listener
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    suspend fun startLoop() {
        mServerSocket = mBluetoothAdapter.listenUsingRfcommWithServiceRecord(
            mServiceName,
            mServiceUUID
        )

        mIsStopped = false

        Log.i(TAG, "Server loop started")
        do {
            val clientSocket: BluetoothSocket =
                withContext(Dispatchers.IO) { acceptConnection() } ?: continue

            Log.i(TAG, "Connection accepted : ${clientSocket.remoteDevice.name}")

            withContext(Dispatchers.Main) { mOnConnectListener?.invoke(clientSocket) }
            mClientSockets.add(clientSocket)
        } while (!mIsStopped)
    }

    fun stop() {
        mClientSockets.forEach { it.close() }
        mClientSockets.clear()
        mServerSocket?.close()
        mIsStopped = true
    }

    fun broadcastMessage(message: String) {
        mClientSockets.forEach { socket ->
            CoroutineScope(Dispatchers.IO).launch { sendMessage(socket, message) }
        }
    }

    fun sendMessage(socket: BluetoothSocket, message: String) {
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
            mServerSocket!!.accept()
        } catch (e: Exception) {
            when (e) {
                is IOException,
                is NullPointerException -> {
                    Log.e(TAG, "Socket's accept() method failed", e)
                    mIsStopped = true

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
            withContext(Dispatchers.Main) { mOnDisconnectListener?.invoke(socket) }
            return false
        }
        return true
    }

}