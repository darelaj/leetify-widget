package id.my.chromazone.leetify_widget

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import id.my.chromazone.leetify_widget.data.LeetifyApi
import id.my.chromazone.leetify_widget.ui.AccountScreen
import id.my.chromazone.leetify_widget.ui.AccountViewModel
import id.my.chromazone.leetify_widget.ui.theme.LeetifywidgetTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val model = ViewModelProvider(this, viewModelFactory {
            initializer { AccountViewModel(LeetifyApi()) }
        })[AccountViewModel::class.java]
        setContent {
            LeetifywidgetTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    val state by model.state.collectAsState()
                    AccountScreen(
                        state = state,
                        onSteamId = model::editSteamId,
                        onApiKey = model::editApiKey,
                        onVerify = model::verify,
                        onReset = model::reset,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

