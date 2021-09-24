package pl.gunock.bluetoothexample.ui.client.pickserver

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.FragmentComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import pl.gunock.bluetoothexample.bluetooth.BluetoothServiceDiscoveryManager
import pl.gunock.bluetoothexample.bluetooth.BluetoothServiceDiscoveryManagerImpl

@Module
@InstallIn(FragmentComponent::class)
object PickDeviceDialogFragmentModule {

    @Provides
    fun provideBluetoothServiceDiscoveryManager(
        @ApplicationContext context: Context
    ): BluetoothServiceDiscoveryManager {
        return BluetoothServiceDiscoveryManagerImpl(context)
    }

}