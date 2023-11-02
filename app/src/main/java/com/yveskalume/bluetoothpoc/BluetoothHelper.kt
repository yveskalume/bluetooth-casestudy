package com.yveskalume.bluetoothpoc

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity.RESULT_OK
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun rememberBluetoothHelper(): BluetoothHelper {
    val context = LocalContext.current
    val helper = remember { BluetoothHelper(context) }

    val coroutineScope = rememberCoroutineScope()

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
            coroutineScope.launch {
                helper.scanLeDevice()
            }
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
        helper.scanLeDevice()
    }


    LaunchedEffect(Unit) {
        permissionLauncher.launch(permissions.toTypedArray())
    }


    return helper
}

@SuppressLint("MissingPermission")
class BluetoothHelper(private val context: Context) {

    private val bluetoothManager = context.getSystemService(BluetoothManager::class.java)
    private val bluetoothAdapter = bluetoothManager.adapter

    private val scanSettings = ScanSettings.Builder()
        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
        .build()

    private val bluetoothLeScanner by lazy {
        bluetoothAdapter.bluetoothLeScanner
    }

    lateinit var bluetoothGatt: BluetoothGatt


    val isBluetoothEnabled: Boolean
        get() = bluetoothAdapter.isEnabled


    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _pairedDevicesFlow = MutableStateFlow<Set<BluetoothDevice>>(emptySet())
    val pairedDevicesFlow: StateFlow<Set<BluetoothDevice>> = _pairedDevicesFlow.asStateFlow()

    private val _scannedDevicesFlow = MutableStateFlow<Set<BluetoothDevice>>(emptySet())
    val scannedDevicesFlow: StateFlow<Set<BluetoothDevice>> = _scannedDevicesFlow.asStateFlow()


    private val _devicePairingWith = MutableStateFlow<String?>(null)
    val devicePairingWith: StateFlow<String?> = _devicePairingWith.asStateFlow()

    private fun getPairedDevices() {
        if (!hasPermission(Manifest.permission.BLUETOOTH_CONNECT)) {
            return
        }
        val pairedDevices = bluetoothAdapter.bondedDevices
        _pairedDevicesFlow.update {
            pairedDevices
        }
    }

    fun unpairDevice(device: BluetoothDevice) {
        device.createBond()
    }


    suspend fun scanLeDevice() {
        if (!hasPermission(Manifest.permission.BLUETOOTH_SCAN)) {
            return
        }
        if (!bluetoothAdapter.isEnabled) {
            return
        }
        _isScanning.value = true

        withContext(Dispatchers.IO) {
            launch { getPairedDevices() }
            bluetoothLeScanner.startScan(null, scanSettings, scanCallback)
            _isScanning.value = false
        }
    }

    private val scanCallback: ScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)
            if (result.device !in _scannedDevicesFlow.value) {
                _scannedDevicesFlow.update { it + result.device }
            }
        }
    }

    private val connectGattCallback: BluetoothGattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.d(TAG, "Connected")
                gatt.discoverServices()
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.e(TAG, "Disconnected")
                gatt.close()
            }

            _devicePairingWith.update { null }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                gatt.printGattTable()
            } else {
                Log.d(TAG, "onServicesDiscovered received: $status")
            }
        }
    }

    fun connect(address: String): Boolean {
        bluetoothAdapter?.let { adapter ->
            try {
                val device = adapter.getRemoteDevice(address)
                bluetoothGatt = device.connectGatt(context, false, connectGattCallback)
                bluetoothLeScanner.stopScan(scanCallback)
                return true
            } catch (exception: IllegalArgumentException) {
                Log.w("Helper", "Device not found with provided address.")
                return false
            }
        } ?: run {
            Log.w("Helper", "BluetoothAdapter not initialized")
            return false
        }
    }

    fun disconnect() {
        bluetoothGatt.disconnect()
    }

    private fun hasPermission(permission: String): Boolean {
        return context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
    }

    companion object {
        const val TAG = "BluetoothHelper"
    }
}