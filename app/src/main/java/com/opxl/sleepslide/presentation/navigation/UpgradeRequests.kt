package com.opxl.sleepslide.presentation.navigation

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * One-shot "start the purchase" requests from an Upgrade snackbar action anywhere in the app.
 * The NavGraph posts one and switches to the Settings tab; SettingsViewModel consumes it and
 * runs its normal purchase flow. A Channel, so a request survives until Settings is alive to
 * take it and is taken exactly once.
 */
@Singleton
class UpgradeRequests @Inject constructor() {

    private val _requests = Channel<Unit>(Channel.CONFLATED)
    val requests: Flow<Unit> = _requests.receiveAsFlow()

    fun request() {
        _requests.trySend(Unit)
    }
}
