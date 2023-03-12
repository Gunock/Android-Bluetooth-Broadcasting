package pl.gunock.bluetoothexample.ui.client.pickserver

import android.bluetooth.BluetoothDevice
import android.util.Log
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class PickDeviceDialogViewModel : ViewModel() {
    companion object {
        const val TAG: String = "PickDeviceDialogViewModel"
    }

    private val _bluetoothDevice: MutableStateFlow<BluetoothDevice?> = MutableStateFlow(null)
    val bluetoothDevice: StateFlow<BluetoothDevice?>
        get() = _bluetoothDevice

    private val _message: MutableSharedFlow<String> = MutableSharedFlow()
    val message: Flow<String>
        get() = _message


    fun resetPickedBluetoothDevice() {
        _bluetoothDevice.value = null
    }

    fun pickBluetoothDeviceItem(item: BluetoothDeviceItem): Boolean {
        return if (!item.isAvailable) {
            false
        } else {
            _bluetoothDevice.value = item.bluetoothDevice
            Log.i(TAG, "Picked : ${item.bluetoothDevice.name}")
            true
        }
    }

}