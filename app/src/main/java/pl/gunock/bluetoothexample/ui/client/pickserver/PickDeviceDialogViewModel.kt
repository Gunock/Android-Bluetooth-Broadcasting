package pl.gunock.bluetoothexample.ui.client.pickserver

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

    private val _bluetoothDeviceAddress: MutableStateFlow<String?> = MutableStateFlow(null)
    val bluetoothDeviceAddress: StateFlow<String?>
        get() = _bluetoothDeviceAddress

    private val _message: MutableSharedFlow<String> = MutableSharedFlow()
    val message: Flow<String>
        get() = _message


    fun resetPickedBluetoothDevice() {
        _bluetoothDeviceAddress.value = null
    }

    fun pickBluetoothDeviceItem(item: BluetoothDeviceItem): Boolean {
        return if (!item.isAvailable) {
            false
        } else {
            _bluetoothDeviceAddress.value = item.deviceAddress
            Log.i(TAG, "Picked : ${item.deviceName}")
            true
        }
    }

}