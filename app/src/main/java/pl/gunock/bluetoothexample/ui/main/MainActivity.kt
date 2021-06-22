package pl.gunock.bluetoothexample.ui.main

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import pl.gunock.bluetoothexample.databinding.ActivityMainBinding
import pl.gunock.bluetoothexample.databinding.ContentMainBinding
import pl.gunock.bluetoothexample.ui.client.ClientActivity
import pl.gunock.bluetoothexample.ui.server.ServerActivity

class MainActivity : AppCompatActivity() {
    private lateinit var mBinding: ContentMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val rootBinding = ActivityMainBinding.inflate(layoutInflater)
        mBinding = rootBinding.content
        setContentView(rootBinding.root)

        setUpButtons()
    }

    private fun setUpButtons() {
        mBinding.btnOpenClient.setOnClickListener {
            val intent = Intent(applicationContext, ClientActivity::class.java)
            startActivity(intent)
        }

        mBinding.btnOpenServer.setOnClickListener {
            val intent = Intent(applicationContext, ServerActivity::class.java)
            startActivity(intent)
        }
    }
}