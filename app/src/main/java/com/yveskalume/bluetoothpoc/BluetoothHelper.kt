package com.yveskalume.bluetoothpoc

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity.RESULT_OK
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import java.util.UUID

@Composable
fun rememberBluetoothHelper(): BluetoothHelper {
    val context = LocalContext.current
    val helper = remember { BluetoothHelper(context) }

    val permissions = mutableListOf(
        Manifest.permission.BLUETOOTH,
        Manifest.permission.BLUETOOTH_ADMIN,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_FINE_LOCATION,
    )

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        permissions.addAll(
            listOf(
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN
            )
        )
    }

    val startActivityLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (it.resultCode == RESULT_OK) {
            helper.startDiscovery()
        } else {
            Toast.makeText(
                context,
                "Bluetooth is not enabled",
                Toast.LENGTH_SHORT
            ).show()
        }
    }


    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        if (results.containsValue(false)) {
            Toast.makeText(
                context,
                "Some permissions are missing",
                Toast.LENGTH_SHORT
            ).show()
        } else {
            if (!helper.isBluetoothEnabled) {
                startActivityLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
            }
        }
    }



    LaunchedEffect(Unit) {
        permissionLauncher.launch(permissions.toTypedArray())
    }

    DisposableEffect(Unit) {
        onDispose {
            helper.release()
        }
    }
    return helper
}

@SuppressLint("MissingPermission")
class BluetoothHelper(private val context: Context) {

    private val bluetoothManager by lazy {
        context.getSystemService(BluetoothManager::class.java)
    }
    private val bluetoothAdapter by lazy {
        bluetoothManager.adapter
    }

    val isBluetoothEnabled: Boolean
        get() = bluetoothAdapter.isEnabled


    private val _isDiscovering = MutableStateFlow(bluetoothAdapter.isDiscovering)
    val isDiscovering: StateFlow<Boolean> = _isDiscovering.asStateFlow()

    private val _pairedDevicesFlow = MutableStateFlow<Set<BluetoothDevice>>(emptySet())
    val pairedDevicesFlow: StateFlow<Set<BluetoothDevice>> = _pairedDevicesFlow.asStateFlow()

    private val _scannedDevicesFlow = MutableStateFlow<Set<BluetoothDevice>>(emptySet())
    val scannedDevicesFlow: StateFlow<Set<BluetoothDevice>> = _scannedDevicesFlow.asStateFlow()


    private val _devicePairingWith = MutableStateFlow<String?>(null)
    val devicePairingWith: StateFlow<String?> = _devicePairingWith.asStateFlow()

    private val receiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    if (hasPermission(Manifest.permission.BLUETOOTH_CONNECT)) {
                        val device: BluetoothDevice? =
                            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                        if (device != null) {
                            _scannedDevicesFlow.update { devices ->
                                if (device in devices) devices else devices + device
                            }
                        }
                    }
                }
            }
        }
    }

    init {
        val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        context.registerReceiver(receiver, filter)

        startDiscovery()
    }

    private fun getPairedDevices() {
        if (!hasPermission(Manifest.permission.BLUETOOTH_CONNECT)) {
            return
        }
        val pairedDevices = bluetoothAdapter.bondedDevices
        _pairedDevicesFlow.update {
            pairedDevices
        }
    }

    fun unPairDevice(device: BluetoothDevice) {

    }

    suspend fun pairDevice(device: BluetoothDevice,onFailure: () -> Unit) {
        return withContext(Dispatchers.IO) {
            try {
                _devicePairingWith.update { device.address }
                val bluetoothSocket = device
                    .createRfcommSocketToServiceRecord(UUID.fromString("27b7d1da-08c7-4505-a6d1-2459987e5e2d"))
                cancelDiscovery()
                bluetoothSocket.connect()

            } catch (e: Exception) {
                Log.e("BluetoothHelper", e.toString())
                onFailure()
            } finally {
                _devicePairingWith.update { null }
            }
        }
    }

    fun startDiscovery() {
        _isDiscovering.update { true }

        if (!hasPermission(Manifest.permission.BLUETOOTH_SCAN)) {
            return
        }

        context.registerReceiver(
            receiver,
            IntentFilter(BluetoothDevice.ACTION_FOUND)
        )


        getPairedDevices()

        bluetoothAdapter.startDiscovery()
        _isDiscovering.update { bluetoothAdapter.isDiscovering }
    }


    private fun cancelDiscovery() {
        if (!hasPermission(Manifest.permission.BLUETOOTH_SCAN)) {
            return
        }

        bluetoothAdapter.cancelDiscovery()
        _isDiscovering.update { bluetoothAdapter.isDiscovering }
    }

    fun release() {
        context.unregisterReceiver(receiver)
    }

    private fun hasPermission(permission: String): Boolean {
        return context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
    }
}