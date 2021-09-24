package pl.gunock.bluetoothexample.bluetooth

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.util.Log
import kotlinx.coroutines.*
import java.io.IOException
import java.util.*

class BluetoothClient(
    connectedDevice: BluetoothDevice,
    serviceUUID: UUID,
    onDataListener: suspend (ByteArray) -> Unit
) {
    private companion object {
        const val TAG = "BluetoothClient"
    }

    private var onConnectionSuccessListener: (suspend (BluetoothSocket) -> Unit)? = null

    private var onConnectionFailureListener: (suspend (BluetoothSocket) -> Unit)? = null

    private var onDisconnectionListener: ((BluetoothSocket) -> Unit)? = null

    private var onDataListener: (suspend (ByteArray) -> Unit)? = onDataListener

    private var stop: Boolean = false

    private val socket: BluetoothSocket =
        connectedDevice.createRfcommSocketToServiceRecord(serviceUUID)

    fun setOnConnectionSuccessListener(listener: (suspend (BluetoothSocket) -> Unit)?) {
        onConnectionSuccessListener = listener
    }

    fun setOnConnectionFailureListener(listener: (suspend (BluetoothSocket) -> Unit)?) {
        onConnectionFailureListener = listener
    }

    fun setOnDisconnectionListener(listener: ((BluetoothSocket) -> Unit)?) {
        onDisconnectionListener = listener
    }

    fun setOnDataListener(listener: (suspend (ByteArray) -> Unit)?) {
        onDataListener = listener
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    suspend fun startLoop() {
        if (!connect()) {
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            while (!stop) {
                delay(500)
                checkConnectionState()
            }
        }

        var buffer: ByteArray = byteArrayOf()
        while (!stop) {
            if (!socket.isConnected) {
                Log.i(TAG, "Socket disconnected")
                break
            }

            try {
                val available = socket.inputStream.available()
                if (available == 0 && buffer.isEmpty()) {
                    delay(50)
                    continue
                }
                Log.i(TAG, "Available data: $available")

                val subBuffer = ByteArray(available)
                socket.inputStream.read(subBuffer)
                buffer += subBuffer
            } catch (ignored: IOException) {
                Log.i(TAG, "Socket suddenly closed")
                break
            }

            val terminationSymbolPosition = buffer.indexOf(4.toByte())
            if (terminationSymbolPosition == -1) {
                continue
            }

            val messageBuffer = buffer.copyOfRange(0, terminationSymbolPosition)
            Log.i(TAG, "Received whole message with size ${messageBuffer.size}")

            buffer = if (terminationSymbolPosition != buffer.size - 1) {
                buffer.copyOfRange(terminationSymbolPosition + 1, buffer.size)
            } else {
                byteArrayOf()
            }

            withContext(Dispatchers.Main) {
                onDataListener?.invoke(messageBuffer)
            }
        }
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    suspend fun disconnect() {
        stop = true
        socket.close()
        withContext(Dispatchers.Main) {
            onDisconnectionListener?.invoke(socket)
        }
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    private suspend fun checkConnectionState() {
        try {
            socket.outputStream.write(0)
        } catch (ignored: IOException) {
            Log.i(TAG, "Socket connection closed")
            withContext(Dispatchers.Main) { onDisconnectionListener?.invoke(socket) }
            stop = true
        }
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    private suspend fun connect(): Boolean {
        try {
            socket.connect()
        } catch (ignored: IOException) {
            Log.i(TAG, "Connection failed")
            socket.close()
        }

        if (socket.isConnected) {
            Log.i(TAG, "Connected")
            withContext(Dispatchers.Main) {
                onConnectionSuccessListener?.invoke(socket)
            }
            return true
        } else {
            Log.w(TAG, "Connection failed")
            withContext(Dispatchers.Main) {
                onConnectionFailureListener?.invoke(socket)
            }
            return false
        }
    }
}