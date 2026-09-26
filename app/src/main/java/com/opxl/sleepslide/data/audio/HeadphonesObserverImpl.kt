package com.opxl.sleepslide.data.audio

import android.content.Context
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import com.opxl.sleepslide.domain.observer.HeadphonesObserver
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Tracks headphone-type outputs via [AudioManager.registerAudioDeviceCallback].
 * Separate from [BluetoothReceiver], which only reports the Bluetooth headset profile and
 * drives pause-on-disconnect in the service.
 */
@Singleton
class HeadphonesObserverImpl @Inject constructor(
    @ApplicationContext context: Context,
) : HeadphonesObserver {

    private val audioManager = context.getSystemService(AudioManager::class.java)

    private val _isHeadphonesConnected = MutableStateFlow(scan())
    override val isHeadphonesConnected: StateFlow<Boolean> = _isHeadphonesConnected.asStateFlow()

    init {
        audioManager.registerAudioDeviceCallback(
            object : AudioDeviceCallback() {
                override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>) {
                    _isHeadphonesConnected.value = scan()
                }

                override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>) {
                    _isHeadphonesConnected.value = scan()
                }
            },
            Handler(Looper.getMainLooper()),
        )
    }

    private fun scan(): Boolean =
        audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS).any { it.type in HEADPHONE_TYPES }

    private companion object {
        val HEADPHONE_TYPES: Set<Int> = buildSet {
            add(AudioDeviceInfo.TYPE_WIRED_HEADPHONES)
            add(AudioDeviceInfo.TYPE_WIRED_HEADSET)
            add(AudioDeviceInfo.TYPE_BLUETOOTH_A2DP)
            add(AudioDeviceInfo.TYPE_USB_HEADSET)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) add(AudioDeviceInfo.TYPE_BLE_HEADSET)
        }
    }
}
