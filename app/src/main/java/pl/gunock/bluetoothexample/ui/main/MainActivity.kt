package pl.gunock.bluetoothexample.ui.main

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import pl.gunock.bluetoothexample.databinding.ActivityMainBinding
import pl.gunock.bluetoothexample.databinding.ContentMainBinding
import pl.gunock.bluetoothexample.ui.client.ClientActivity
import pl.gunock.bluetoothexample.ui.server.ServerActivity

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ContentMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val rootBinding = ActivityMainBinding.inflate(layoutInflater)
        binding = rootBinding.content
        setContentView(rootBinding.root)

        setUpButtons()
    }

    private fun setUpButtons() {
        binding.btnOpenClient.setOnClickListener {
            val intent = Intent(applicationContext, ClientActivity::class.java)
            startActivity(intent)
        }

        binding.btnOpenServer.setOnClickListener {
            val intent = Intent(applicationContext, ServerActivity::class.java)
            startActivity(intent)
        }
    }
}