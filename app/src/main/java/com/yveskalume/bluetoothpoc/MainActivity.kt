package com.yveskalume.bluetoothpoc

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.yveskalume.bluetoothpoc.ui.theme.BluetoothPOCTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

//    private var bluetoothService: BluetoothService? = null
//
//    private val serviceConnection: ServiceConnection = object : ServiceConnection {
//        override fun onServiceConnected(
//            componentName: ComponentName,
//            service: IBinder
//        ) {
//            bluetoothService = (service as BluetoothService.LocalBinder).getService()
//            bluetoothService?.let { bluetooth ->
//                if (!bluetooth.initialize()) {
//                    Log.e("MainActivity", "Unable to initialize Bluetooth")
//                    finish()
//                }
//
//                bluetooth.connect(deviceAddress)
//
//            }
//        }
//
//        override fun onServiceDisconnected(componentName: ComponentName) {
//            bluetoothService = null
//        }
//    }

    @OptIn(ExperimentalFoundationApi::class)
    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

//        val gattServiceIntent = Intent(this, BluetoothService::class.java)
//        bindService(gattServiceIntent, serviceConnection, Context.BIND_AUTO_CREATE)

        setContent {
            BluetoothPOCTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val bluetoothHelper = rememberBluetoothHelper()
                    val pairedDevices by bluetoothHelper.pairedDevicesFlow.collectAsState()
                    val scannedDevices by bluetoothHelper.scannedDevicesFlow.collectAsState()
                    val devicePairingWith by bluetoothHelper.devicePairingWith.collectAsState()

                    val isScanning by bluetoothHelper.isScanning.collectAsState()

                    val coroutineScope = rememberCoroutineScope()

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp)
                    ) {
                        stickyHeader {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Paired Devices",
                                style = MaterialTheme.typography.titleLarge,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                        }
                        items(pairedDevices.toList()) { device ->
                            Card(
                                modifier = Modifier
                                    .padding(8.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .wrapContentHeight()
                                        .fillMaxWidth()
                                        .padding(8.dp)
                                ) {
                                    Text(text = "Device Name: ${device.name}")
                                    Text(text = "Device Address: ${device.address}")
                                    Button(onClick = {

                                    }) {
                                        Text(text = "Unpair Device")
                                    }
                                }
                            }
                        }


                        stickyHeader {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Scanned Devices",
                                style = MaterialTheme.typography.titleLarge,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                        }
                        items(scannedDevices.toList()) { device ->
                            val isPairing = devicePairingWith == device.address
                            Card(
                                modifier = Modifier
                                    .padding(8.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .wrapContentHeight()
                                        .fillMaxWidth()
                                        .padding(8.dp)
                                ) {
                                    Text(text = "Device Name: ${device.name}")
                                    Text(text = "Device Address: ${device.address}")
                                    Button(
                                        enabled = devicePairingWith == null,
                                        onClick = {
                                            coroutineScope.launch {
                                                bluetoothHelper.connect(device.address)
                                            }
                                        }
                                    ) {
                                        Text(
                                            text = if (isPairing) {
                                                "Pairing"
                                            } else {
                                                "Pair Device"
                                            }
                                        )
                                        if (isPairing) {
                                            Spacer(modifier = Modifier.width(2.dp))
                                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            AnimatedVisibility(visible = isScanning) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    CircularProgressIndicator()
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

