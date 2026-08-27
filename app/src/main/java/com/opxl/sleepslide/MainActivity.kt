package com.opxl.sleepslide

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.opxl.sleepslide.data.audio.AudioServiceImpl
import com.opxl.sleepslide.ui.theme.SleepSlideTheme

class MainActivity : ComponentActivity() {

    // In MainActivity or a ServiceConnectionManager
    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, binder: IBinder) {
            val service = (binder as AudioServiceImpl.LocalBinder).getService()
            // pass to ViewModel or a shared holder
        }
        override fun onServiceDisconnected(name: ComponentName) {}
    }

    override fun onStart() {
        super.onStart()
        Intent(this, AudioServiceImpl::class.java).also {
            bindService(it, connection, Context.BIND_AUTO_CREATE)
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SleepSlideTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(
                        name = "Android",
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    SleepSlideTheme {
        Greeting("Android")
    }
}