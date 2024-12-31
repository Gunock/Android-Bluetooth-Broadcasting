package dev.thomas_kiljanczyk.bluetoothbroadcasting.application

import android.Manifest
import android.app.Application
import android.os.Build
import com.google.android.material.color.DynamicColors
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class BluetoothApplication : Application() {
    companion object {
        val PERMISSIONS = preparePermissionArray()

        private fun preparePermissionArray(): Array<String> {
            val result = mutableListOf<String>(
                Manifest.permission.ACCESS_WIFI_STATE,
                Manifest.permission.CHANGE_WIFI_STATE
            )

            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.R) {
                result.add(Manifest.permission.BLUETOOTH)
                result.add(Manifest.permission.BLUETOOTH_ADMIN)
            }

            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
                result.add(Manifest.permission.ACCESS_COARSE_LOCATION)
                result.add(Manifest.permission.ACCESS_FINE_LOCATION)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                result.add(Manifest.permission.BLUETOOTH_ADVERTISE)
                result.add(Manifest.permission.BLUETOOTH_CONNECT)
                result.add(Manifest.permission.BLUETOOTH_SCAN)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                result.add(Manifest.permission.NEARBY_WIFI_DEVICES)
            }

            return result.toTypedArray()
        }
    }

    override fun onCreate() {
        super.onCreate()

        // TODO: add google play services availability check
//        if (!isGooglePlayServicesAvailable(applicationContext)) {
//
//        }

        DynamicColors.applyToActivitiesIfAvailable(this)
    }
}
