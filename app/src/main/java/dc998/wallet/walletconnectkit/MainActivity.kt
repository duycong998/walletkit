package dc998.wallet.walletconnectkit

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import dc998.wallet.walletconnectkit.databinding.ActivityMainBinding
import dev.pinkroom.walletconnectkit.WalletConnectKit
import dev.pinkroom.walletconnectkit.WalletConnectKitConfig
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


class MainActivity : AppCompatActivity(R.layout.activity_main) {
    private lateinit var binding: ActivityMainBinding

    private val config = WalletConnectKitConfig(
        bridgeUrl = "wss://bridge.walletconnect.org",
        appUrl = "walletconnectkit.com",
        appName = "WalletConnect Kit",
        appDescription = "This is the Swiss Army toolkit for WalletConnect!"
    )

    private val walletConnectKit by lazy {
        WalletConnectKit.builder(this).config(config).build().apply {
            onConnected = ::onConnected
            onDisconnected = ::onDisconnected
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initViews()
    }

    private fun initViews() {
        initLoginView()
        initPerformTransactionView()
    }

    private fun initLoginView() = with(binding) {
        loginView.start(walletConnectKit)
        setSupportActionBar(toolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.title = "Payment method"
        toolbar.setTitleTextColor(resources.getColor(R.color.black))
        toolbar.setNavigationOnClickListener { finish() }

        setSupportActionBar(toolbarConnectView)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.title = ""
        toolbarConnectView.setNavigationOnClickListener {
            onDisconnectClicked()
            finish()
        }
    }

    private fun onConnected(address: String) = with(binding) {
        constraintLayout.visibility = View.GONE
        connectedView.visibility = View.VISIBLE
        invalidateOptionsMenu()
    }

    private fun onDisconnected() = with(binding) {
        constraintLayout.visibility = View.VISIBLE
        connectedView.visibility = View.GONE
        invalidateOptionsMenu()
    }

    private fun initPerformTransactionView() = with(binding) {
        performTransactionView.setOnClickListener {
            val toAddress = toAddressView.text.toString()
            val value = "0.0001"
            lifecycleScope.launch {
                walletConnectKit.performTransaction(toAddress, value, gasLimit = "0.5")
                    .onSuccess {
                        linearLayoutAnimation.visibility = View.VISIBLE
                        connectedView.visibility = View.GONE
                        delay(2000L)
                        linearLayoutAnimation.visibility = View.GONE
                        linearLayoutSuccess.visibility = View.VISIBLE
                        Toast.makeText(
                            this@MainActivity,
                            "Transaction done!",
                            Toast.LENGTH_SHORT
                        ).show()
                        buttonPlayStream.setOnClickListener {
                            //play video stream in here
                            //startActivity(new Intent(requireActivity(), VlcActivity.class).putExtra("link",JSON.getString("sharingUrl")));
                            finish()
                        }
                    }.onFailure {
                        Toast.makeText(this@MainActivity, it.message, Toast.LENGTH_SHORT).show()
                        Log.d("####performTransaction", it.message.toString())
                    }


            }
        }
    }

    private fun onDisconnectClicked() {
        walletConnectKit.removeSession()
    }
}
