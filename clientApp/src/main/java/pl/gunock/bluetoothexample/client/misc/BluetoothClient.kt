package pl.gunock.bluetoothexample.client.misc

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.util.Log
import kotlinx.coroutines.delay
import java.io.IOException
import java.util.*

class BluetoothClient(
    connectedDevice: BluetoothDevice,
    mServiceUUID: UUID,
    private val mOnDataCallback: suspend (data: ByteArray) -> Unit
) {
    private companion object {
        const val TAG = "BluetoothClient"
    }

    private var mStop: Boolean = false

    private val mSocket: BluetoothSocket =
        connectedDevice.createRfcommSocketToServiceRecord(mServiceUUID)

    @Suppress("BlockingMethodInNonBlockingContext")
    suspend fun startLoop() {
        mSocket.connect()

        if (mSocket.isConnected) {
            Log.i(TAG, "Connected")
        } else {
            Log.w(TAG, "Connection failed")
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
                break
            }

            mOnDataCallback(buffer)
        }
    }

    fun disconnect() {
        mStop = true
        mSocket.close()
    }

}