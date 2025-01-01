package dev.thomas_kiljanczyk.bluetoothbroadcasting.ui.client.pickserver

import android.util.Log
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class PickDeviceDialogViewModel @Inject constructor() : ViewModel() {
    companion object {
        const val TAG: String = "PickDeviceDialogViewModel"
    }

    private val _serverEndpointId: MutableStateFlow<String?> = MutableStateFlow(null)
    val serverEndpointId: StateFlow<String?>
        get() = _serverEndpointId

    private val _message: MutableSharedFlow<String> = MutableSharedFlow(replay = 1)
    val message: Flow<String>
        get() = _message

    fun resetPickedDevice() {
        _serverEndpointId.value = null
    }

    fun pickDevice(item: BluetoothDeviceItem) {
        _serverEndpointId.value = item.endpointId
        Log.i(TAG, "Picked : ${item.deviceName}")
    }

}