package dev.pinkroom.walletconnectkit.data.wallet

import kotlinx.coroutines.flow.Flow
import org.walletconnect.Session

interface WalletManager {
    fun openWallet()
    fun requestHandshake()

    suspend fun performTransaction(
        address: String,
        value: String,
        data: String? = null ,
        nonce: String? = null,
        gasPrice: String? = null,
        gasLimit: String? = null,
    ): Result<Session.MethodCall.Response>

    fun performTransaction(
        address: String,
        value: String,
        data: String? = null,
        nonce: String? = null,
        gasPrice: String? = null,
        gasLimit: String? = null,
        onResult: (Result<Session.MethodCall.Response>) -> Unit
    )

    suspend fun personalSign(message: String): Result<Session.MethodCall.Response>

    fun personalSign(message: String, onResult: (Result<Session.MethodCall.Response>) -> Unit )
}