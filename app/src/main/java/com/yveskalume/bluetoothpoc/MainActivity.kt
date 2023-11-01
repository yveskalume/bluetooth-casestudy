package com.yveskalume.bluetoothpoc

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import com.yveskalume.bluetoothpoc.ui.theme.BluetoothPOCTheme

class MainActivity : ComponentActivity() {


    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkPermissions()

        setContent {
            BluetoothPOCTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val connectedDevices by BluetoothHelper.connectedDevicesFlow.collectAsState()

                    LazyColumn(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                        items(connectedDevices) {device ->
                            Column(modifier = Modifier.wrapContentSize()) {
                                Text(text = "Device Name: ${device.name}")
                                Text(text = "Device Address: ${device.address}")
                                Text(text = "Device Type: ${device.type}")
                            }
                        }
                    }
                }
            }
        }
    }

    private fun accessBluetooth() {
        val isBluetoothEnabled = BluetoothHelper.initialize(this)

        if (!isBluetoothEnabled) {
            enableBluetooth()
        }
    }

    private fun enableBluetooth() {
        val activityResultLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) {
            it.resultCode
        }

        activityResultLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
    }

    private fun checkPermissions() {

        val permissions = mutableListOf(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,

            )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
        }

        val launcher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { results ->
            if (results.containsValue(false)) {
                Toast.makeText(
                    this,
                    "Some permissions are missing",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                accessBluetooth()
            }
        }

        launcher.launch(permissions.toTypedArray())
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}

