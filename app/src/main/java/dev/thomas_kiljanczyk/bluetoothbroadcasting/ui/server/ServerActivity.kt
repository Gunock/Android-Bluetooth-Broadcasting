package dev.thomas_kiljanczyk.bluetoothbroadcasting.ui.server

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import dev.thomas_kiljanczyk.bluetoothbroadcasting.R
import dev.thomas_kiljanczyk.bluetoothbroadcasting.databinding.ActivityServerBinding
import dev.thomas_kiljanczyk.bluetoothbroadcasting.databinding.ContentServerBinding
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

@AndroidEntryPoint
class ServerActivity @Inject constructor() : AppCompatActivity() {
    private companion object {
        const val TAG = "MainActivity"
    }

    private val viewModel: ServerViewModel by viewModels()

    @Inject
    lateinit var bluetoothManager: BluetoothManager

    private lateinit var binding: ContentServerBinding

    private val enableBluetoothActivityResultLauncher =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult(),
            this::handleEnableBluetoothResult
        )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val rootBinding = ActivityServerBinding.inflate(layoutInflater)
        binding = rootBinding.content

        setSupportActionBar(rootBinding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        setContentView(rootBinding.root)

        setupObservers()
        setupListeners()

        setupBluetooth()
    }

    override fun onDestroy() {
        viewModel.stopServer()
        super.onDestroy()
    }

    private fun setupObservers() {
        viewModel.serverOnFlow
            .onEach {
                val serverStateText = if (it)
                    getString(R.string.activity_server_server_on)
                else
                    getString(R.string.activity_server_server_off)

                binding.tvServerStatus.text = serverStateText

                if (it) {
                    binding.btnServerStart.isEnabled = false
                    binding.btnServerStop.isEnabled = true
                } else {
                    binding.btnServerStart.isEnabled = true
                    binding.btnServerStop.isEnabled = false
                }
            }.launchIn(lifecycleScope)

        viewModel.messageFlow
            .onEach { (stringId, deviceName) ->
                var statusText = getString(stringId)
                if (deviceName != null) {
                    statusText = statusText.format(deviceName)
                }

                Toast.makeText(baseContext, statusText, Toast.LENGTH_SHORT).show()
            }.launchIn(lifecycleScope)
    }

    private fun setupBluetooth() {
        val bluetoothAdapter = bluetoothManager.adapter
        if (bluetoothAdapter == null) {
            Toast.makeText(
                baseContext,
                R.string.toast_no_bluetooth,
                Toast.LENGTH_SHORT
            ).show()
            finish()
            return
        }

        if (!bluetoothAdapter.isEnabled) {
            val enableBluetoothIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            enableBluetoothActivityResultLauncher.launch(enableBluetoothIntent)
        }
    }

    private fun setupListeners() {
        binding.btnServerStart.setOnClickListener {
            try {
                val deviceName =
                    Settings.Global.getString(contentResolver, Settings.Global.DEVICE_NAME)
                viewModel.startServer(deviceName)
            } catch (ex: SecurityException) {
                Log.e(TAG, "Failed to start server", ex)
                finish()
            }
        }

        binding.btnServerStop.setOnClickListener {
            viewModel.stopServer()
        }

        binding.btnSendMessage.setOnClickListener {
            val message = binding.edMessage.text.toString()
            viewModel.broadcastMessage(message)

            val messageSentText =
                if (viewModel.serverOnFlow.value)
                    R.string.activity_server_message_sent
                else
                    R.string.activity_server_message_not_sent

            Toast.makeText(
                baseContext,
                messageSentText,
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun handleEnableBluetoothResult(result: ActivityResult) {
        if (result.resultCode == RESULT_CANCELED) {
            Log.d(TAG, "User refused REQUEST_ENABLE_BT")
        }
    }
}

