package pl.gunock.bluetoothbroadcasting.ui.client.pickserver

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.FragmentComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import pl.gunock.bluetoothbroadcasting.shared.bluetooth.BluetoothServiceDiscoveryManager
import pl.gunock.bluetoothbroadcasting.shared.bluetooth.BluetoothServiceDiscoveryManagerImpl

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