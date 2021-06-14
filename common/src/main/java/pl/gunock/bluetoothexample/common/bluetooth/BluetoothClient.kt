package pl.gunock.bluetoothexample.common.bluetooth

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.*

class BluetoothClient(
    connectedDevice: BluetoothDevice,
    serviceUUID: UUID,
    private val mOnDataCallback: suspend (ByteArray) -> Unit
) {
    private companion object {
        const val TAG = "BluetoothClient"
    }

    private var onConnectionSuccessListener: (suspend (BluetoothSocket) -> Unit)? = null

    private var onConnectionFailureListener: (suspend (BluetoothSocket) -> Unit)? = null

    private var onDisconnectionListener: ((BluetoothSocket) -> Unit)? = null

    private var mStop: Boolean = false

    private val mSocket: BluetoothSocket =
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

    @Suppress("BlockingMethodInNonBlockingContext")
    suspend fun startLoop() {
        try {
            mSocket.connect()
        } catch (ignored: IOException) {
            Log.i(TAG, "Connection failed")
            mSocket.close()
        }

        if (mSocket.isConnected) {
            Log.i(TAG, "Connected")
            withContext(Dispatchers.Main) {
                onConnectionSuccessListener?.invoke(mSocket)
            }
        } else {
            Log.w(TAG, "Connection failed")
            withContext(Dispatchers.Main) {
                onConnectionFailureListener?.invoke(mSocket)
            }
            return
        }

        while (!mStop) {
            if (!mSocket.isConnected) {
                Log.i(TAG, "Socket disconnected")
                break
            }

            val buffer: ByteArray
            try {
                val available = mSocket.inputStream.available()
                if (available == 0) {
                    delay(100)
                    continue
                }

                Log.i(TAG, "Available data: $available")

                buffer = ByteArray(available)
                mSocket.inputStream.read(buffer)
            } catch (ignored: IOException) {
                Log.i(TAG, "Socket suddenly closed")
                onDisconnectionListener?.invoke(mSocket)
                mStop = true
                break
            }

            withContext(Dispatchers.Main) {
                mOnDataCallback(buffer)
            }
        }
    }

    fun disconnect() {
        mStop = true
        mSocket.close()
        onDisconnectionListener?.invoke(mSocket)
    }

}