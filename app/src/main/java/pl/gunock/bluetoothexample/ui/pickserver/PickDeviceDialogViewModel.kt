package pl.gunock.bluetoothexample.ui.pickserver

import android.bluetooth.BluetoothDevice
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

data class PickDeviceDialogViewModel(
    var bluetoothDevice: MutableLiveData<BluetoothDevice> = MutableLiveData()
) : ViewModel()