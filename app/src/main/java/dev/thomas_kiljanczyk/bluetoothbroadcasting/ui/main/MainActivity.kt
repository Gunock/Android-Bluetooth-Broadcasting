package dev.thomas_kiljanczyk.bluetoothbroadcasting.ui.main

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import dev.thomas_kiljanczyk.bluetoothbroadcasting.application.BluetoothApplication
import dev.thomas_kiljanczyk.bluetoothbroadcasting.databinding.ActivityMainBinding
import dev.thomas_kiljanczyk.bluetoothbroadcasting.databinding.ContentMainBinding
import dev.thomas_kiljanczyk.bluetoothbroadcasting.ui.client.ClientActivity
import dev.thomas_kiljanczyk.bluetoothbroadcasting.ui.server.ServerActivity

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ContentMainBinding


    private val secondPermissionRequestLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { isGranted ->
            if (!isGranted.values.any { !it }) {
                return@registerForActivityResult
            }
            PermissionsDeniedDialogFragment(::checkPermissions).show(supportFragmentManager)
        }

    private val firstPermissionRequestLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { isGranted ->
            if (isGranted.values.all { it }) {
                return@registerForActivityResult
            }

            PermissionsRequestDialogFragment {
                secondPermissionRequestLauncher.launch(BluetoothApplication.PERMISSIONS)
            }.show(supportFragmentManager)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val rootBinding = ActivityMainBinding.inflate(layoutInflater)
        binding = rootBinding.content

        setSupportActionBar(rootBinding.toolbar)
        setContentView(rootBinding.root)

        setupButtons()
    }

    override fun onStart() {
        super.onStart()
        checkPermissions()
    }

    private fun setupButtons() {
        binding.btnOpenClient.setOnClickListener {
            val intent = Intent(applicationContext, ClientActivity::class.java)
            startActivity(intent)
        }

        binding.btnOpenServer.setOnClickListener {
            val intent = Intent(applicationContext, ServerActivity::class.java)
            startActivity(intent)
        }
    }

    private fun checkPermissions() {
        when {
            BluetoothApplication.PERMISSIONS.all {
                this.checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED
            } -> {
                return
            }
            BluetoothApplication.PERMISSIONS.any(::shouldShowRequestPermissionRationale) -> {
                PermissionsRequestDialogFragment {
                    secondPermissionRequestLauncher.launch(BluetoothApplication.PERMISSIONS)
                }.show(supportFragmentManager)
            }
            else -> {
                firstPermissionRequestLauncher.launch(BluetoothApplication.PERMISSIONS)
            }
        }
    }
}