package pl.gunock.bluetoothbroadcasting.ui.server

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import pl.gunock.bluetoothbroadcasting.R
import pl.gunock.bluetoothbroadcasting.databinding.ActivityServerBinding
import pl.gunock.bluetoothbroadcasting.databinding.ContentServerBinding
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

    private val discoverableActivityResultLauncher =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult(),
            this::handleDiscoverableResult
        )

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

    private fun setupObservers() {
        viewModel.serverStatus
            .onEach {
                binding.tvServerStatus.text = getString(it)
            }.launchIn(lifecycleScope)

        viewModel.message
            .onEach {
                Toast.makeText(baseContext, it, Toast.LENGTH_SHORT).show()
            }.launchIn(lifecycleScope)
    }

    private fun setupBluetooth() {
        val bluetoothAdapter = bluetoothManager.adapter

        if (bluetoothAdapter == null) {
            Toast.makeText(
                baseContext,
                "Oops! It looks like your device doesn't have bluetooth!",
                Toast.LENGTH_SHORT
            ).show()
        }

        if (!bluetoothAdapter.isEnabled) {
            val enableBluetoothIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            enableBluetoothActivityResultLauncher.launch(enableBluetoothIntent)
        }

        try {
            viewModel.setServer(bluetoothAdapter)
        } catch (ex: SecurityException) {
            finish()
            return
        }
    }

    private fun setupListeners() {
        binding.btnServerStart.setOnClickListener {
            val discoverableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE)
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 0)

            discoverableActivityResultLauncher.launch(discoverableIntent)
        }

        binding.btnServerStop.setOnClickListener {
            viewModel.stopServer()
        }

        binding.btnSendMessage.setOnClickListener {
            val message = binding.edMessage.text.toString()
            viewModel.broadcastMessage(message)
            Toast.makeText(
                baseContext,
                R.string.activity_server_message_sent,
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun handleDiscoverableResult(result: ActivityResult) {
        if (result.resultCode == RESULT_CANCELED) {
            Log.d(TAG, "User refused REQUEST_DISCOVERABLE")
        } else {
            Log.i(TAG, "Started listening")
            try {
                viewModel.startServer()
            } catch (ex: SecurityException) {
                finish()
                return
            }
        }
    }

    private fun handleEnableBluetoothResult(result: ActivityResult) {
        if (result.resultCode == RESULT_CANCELED) {
            Log.d(TAG, "User refused REQUEST_ENABLE_BT")
        }
    }
}

