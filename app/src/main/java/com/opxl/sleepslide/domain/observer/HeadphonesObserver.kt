package com.opxl.sleepslide.domain.observer

import kotlinx.coroutines.flow.StateFlow

/**
 * Whether a headphone-type audio output (wired, USB, Bluetooth A2DP / LE) is connected.
 * Independent of the playback service, so screens can read it before anything plays.
 */
interface HeadphonesObserver {

    val isHeadphonesConnected: StateFlow<Boolean>
}
