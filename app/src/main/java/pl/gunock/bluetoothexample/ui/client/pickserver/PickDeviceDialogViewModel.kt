package pl.gunock.bluetoothexample.ui.client.pickserver

import android.bluetooth.BluetoothDevice
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

data class PickDeviceDialogViewModel(
    var bluetoothDevice: MutableLiveData<BluetoothDevice> = MutableLiveData()
) : ViewModel() {

    companion object {
        const val TAG: String = "PickDeviceDialogViewModel"
    }

    private var _message: MutableLiveData<String> = MutableLiveData("")

    val message: LiveData<String>
        get() = _message


    fun pickBluetoothDeviceItem(item: BluetoothDeviceItem): Boolean {
        return if (!item.isAvailable) {
            false
        } else {
            bluetoothDevice.postValue(item.bluetoothDevice)
            Log.i(TAG, "Picked : ${item.bluetoothDevice.name}")
            true
        }
    }

}