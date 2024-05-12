package pl.gunock.bluetoothbroadcasting.lib

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothSocket
import android.util.Log
import androidx.annotation.RequiresPermission
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

class BluetoothClient(
    private val socket: BluetoothSocket
) {
    private companion object {
        const val TAG = "BluetoothClient"
    }

    private var onConnectionSuccessListener: (suspend (BluetoothSocket) -> Unit)? = null

    private var onConnectionFailureListener: (suspend (BluetoothSocket) -> Unit)? = null

    private var onDisconnectionListener: ((BluetoothSocket) -> Unit)? = null

    private var onDataListener: (suspend (ByteArray) -> Unit)? = null

    private var stop: Boolean = false

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

    @SuppressLint("InlinedApi")
    @RequiresPermission(anyOf = [Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH])

    suspend fun startLoop() = withContext(Dispatchers.IO) {
        if (!connect()) {
            return@withContext
        }

        CoroutineScope(Dispatchers.IO).launch {
            while (!stop) {
                delay(1000)
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

            withContext(Dispatchers.Main) { onDataListener?.invoke(messageBuffer) }
        }
    }

    suspend fun disconnect() = withContext(Dispatchers.IO) {
        stop = true
        socket.close()
        withContext(Dispatchers.Main) { onDisconnectionListener?.invoke(socket) }
    }

    private suspend fun checkConnectionState() = withContext(Dispatchers.IO) {
        try {
            socket.outputStream.write(0)
        } catch (ignored: IOException) {
            Log.i(TAG, "Socket connection closed")
            withContext(Dispatchers.Main) { onDisconnectionListener?.invoke(socket) }
            stop = true
        }
    }

    @SuppressLint("InlinedApi")
    @RequiresPermission(anyOf = [Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH])
    private suspend fun connect(): Boolean = withContext(Dispatchers.IO) {
        try {
            socket.connect()
        } catch (ignored: IOException) {
            socket.close()
        }

        return@withContext if (socket.isConnected) {
            Log.i(TAG, "Connected")
            withContext(Dispatchers.Main) { onConnectionSuccessListener?.invoke(socket) }
            true
        } else {
            Log.w(TAG, "Connection failed")
            withContext(Dispatchers.Main) { onConnectionFailureListener?.invoke(socket) }
            false
        }
    }
}