package com.yveskalume.bluetoothpoc

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHeadset
import android.bluetooth.BluetoothProfile
import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object BluetoothHelper {

    private var bluetoothHeadset: BluetoothHeadset? = null

    private lateinit var profileListener: BluetoothProfile.ServiceListener

    private lateinit var bluetoothAdapter: BluetoothAdapter

    private val _connectedDevicesFlow: MutableStateFlow<List<BluetoothDevice>> = MutableStateFlow(
        emptyList()
    )
    val connectedDevicesFlow: StateFlow<List<BluetoothDevice>> = _connectedDevicesFlow.asStateFlow()


    @SuppressLint("MissingPermission")
    fun initialize(context: Context): Boolean {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

        if (!bluetoothAdapter.isEnabled) {
            return false
        }

        profileListener = object : BluetoothProfile.ServiceListener {

            override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
                if (profile == BluetoothProfile.HEADSET) {
                    bluetoothHeadset = proxy as BluetoothHeadset
                    _connectedDevicesFlow.value = bluetoothHeadset!!.connectedDevices
                }
            }

            override fun onServiceDisconnected(profile: Int) {
                if (profile == BluetoothProfile.HEADSET) {
                    bluetoothHeadset = null
                    _connectedDevicesFlow.value = emptyList()
                }
            }
        }
        bluetoothAdapter.getProfileProxy(context, profileListener, BluetoothProfile.HEADSET)
        bluetoothAdapter.closeProfileProxy(BluetoothProfile.HEADSET, bluetoothHeadset)
        return true
    }
}