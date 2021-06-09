/*
 * Created by Tomasz Kiljanczyk on 4/20/21 5:24 PM
 * Copyright (c) 2021 . All rights reserved.
 * Last modified 4/20/21 5:24 PM
 */

package pl.gunock.bluetoothexample.client.fragments.viemodels

import android.bluetooth.BluetoothDevice
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

data class PickDeviceDialogViewModel(
    var bluetoothDevice: MutableLiveData<BluetoothDevice> = MutableLiveData()
) : ViewModel()