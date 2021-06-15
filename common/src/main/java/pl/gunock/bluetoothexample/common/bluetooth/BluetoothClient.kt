package pl.gunock.bluetoothexample.common.bluetooth

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

    private var mOnConnectionSuccessListener: (suspend (BluetoothSocket) -> Unit)? = null

    private var mOnConnectionFailureListener: (suspend (BluetoothSocket) -> Unit)? = null

    private var mOnDisconnectionListener: ((BluetoothSocket) -> Unit)? = null

    private var mOnDataListener: (suspend (ByteArray) -> Unit)? = onDataListener

    private var mStop: Boolean = false

    private val mSocket: BluetoothSocket =
        connectedDevice.createRfcommSocketToServiceRecord(serviceUUID)

    fun setOnConnectionSuccessListener(listener: (suspend (BluetoothSocket) -> Unit)?) {
        mOnConnectionSuccessListener = listener
    }

    fun setOnConnectionFailureListener(listener: (suspend (BluetoothSocket) -> Unit)?) {
        mOnConnectionFailureListener = listener
    }

    fun setOnDisconnectionListener(listener: ((BluetoothSocket) -> Unit)?) {
        mOnDisconnectionListener = listener
    }

    fun setOnDataListener(listener: (suspend (ByteArray) -> Unit)?) {
        mOnDataListener = listener
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    suspend fun startLoop() {
        if (!connect()) {
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            while (!mStop) {
                delay(500)
                checkConnectionState()
            }
        }

        var buffer: ByteArray = byteArrayOf()
        while (!mStop) {
            if (!mSocket.isConnected) {
                Log.i(TAG, "Socket disconnected")
                break
            }

            try {
                val available = mSocket.inputStream.available()
                if (available == 0 && buffer.isEmpty()) {
                    delay(50)
                    continue
                }
                Log.i(TAG, "Available data: $available")

                val subBuffer = ByteArray(available)
                mSocket.inputStream.read(subBuffer)
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
                mOnDataListener?.invoke(messageBuffer)
            }
        }
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    suspend fun disconnect() {
        mStop = true
        mSocket.close()
        withContext(Dispatchers.Main) {
            mOnDisconnectionListener?.invoke(mSocket)
        }
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    private suspend fun checkConnectionState() {
        try {
            mSocket.outputStream.write(0)
        } catch (ignored: IOException) {
            Log.i(TAG, "Socket connection closed")
            withContext(Dispatchers.Main) { mOnDisconnectionListener?.invoke(mSocket) }
            mStop = true
        }
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    private suspend fun connect(): Boolean {
        try {
            mSocket.connect()
        } catch (ignored: IOException) {
            Log.i(TAG, "Connection failed")
            mSocket.close()
        }

        if (mSocket.isConnected) {
            Log.i(TAG, "Connected")
            withContext(Dispatchers.Main) {
                mOnConnectionSuccessListener?.invoke(mSocket)
            }
            return true
        } else {
            Log.w(TAG, "Connection failed")
            withContext(Dispatchers.Main) {
                mOnConnectionFailureListener?.invoke(mSocket)
            }
            return false
        }
    }
}