package pl.gunock.bluetoothexample.server.misc

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.util.Log
import kotlinx.coroutines.*
import java.io.IOException
import java.util.*

class BluetoothServer(
    bluetoothAdapter: BluetoothAdapter,

    ) {
    private companion object {
        const val TAG = "BluetoothClient"

        const val SERVICE_NAME = "SERVICE"
        val SERVICE_UUID = UUID(1, 1)
    }

    var onConnectCallback: (suspend (BluetoothSocket) -> Unit)? = null
    var onDisconnectCallback: ((BluetoothSocket) -> Unit)? = null

    private val mServerSocket: BluetoothServerSocket =
        bluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord(
            SERVICE_NAME,
            SERVICE_UUID
        )

    private val mClientSockets: MutableList<BluetoothSocket> = mutableListOf()

    init {
        CoroutineScope(Dispatchers.Main).launch {
            while (true) {
                mClientSockets.removeAll {
                    try {
                        it.inputStream.skip(1)
                        false
                    } catch (ignored: IOException) {
                        Log.i(TAG, "Socket connection closed")
                        onDisconnectCallback?.invoke(it)
                        true
                    }
                }
                delay(1000)
            }
        }
    }


    @Suppress("BlockingMethodInNonBlockingContext")
    suspend fun acceptConnection() {
        var shouldLoop = true
        while (shouldLoop) {
            val clientSocket: BluetoothSocket = try {
                withContext(Dispatchers.IO) { mServerSocket.accept() }
            } catch (e: IOException) {
                Log.e(TAG, "Socket's accept() method failed", e)
                shouldLoop = false
                null
            } ?: continue

            Log.i(TAG, "Connection accepted")
            onConnectCallback?.invoke(clientSocket)
            mClientSockets.add(clientSocket)
        }
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
}