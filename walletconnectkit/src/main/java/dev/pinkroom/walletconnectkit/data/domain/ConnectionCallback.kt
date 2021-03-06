package dev.pinkroom.walletconnectkit.data.domain

import org.walletconnect.Session

interface ConnectionCallback {
    var onConnected: ((address: String) -> Unit)?
    var onDisconnected: (() -> Unit)?
    var sessionCallback: Session.Callback?
}