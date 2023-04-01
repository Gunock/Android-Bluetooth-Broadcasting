package pl.gunock.bluetoothbroadcasting.application

import android.bluetooth.BluetoothManager
import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    fun provideBluetoothManager(@ApplicationContext context: Context): BluetoothManager {
        return context.getSystemService(BluetoothManager::class.java)
    }

}