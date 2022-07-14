package dev.pinkroom.walletconnectkit.data.wallet

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import dev.pinkroom.walletconnectkit.common.Dispatchers
import dev.pinkroom.walletconnectkit.common.hasHexPrefix
import dev.pinkroom.walletconnectkit.common.toHex
import dev.pinkroom.walletconnectkit.common.toWei
import dev.pinkroom.walletconnectkit.data.session.SessionRepository
import kotlinx.coroutines.withContext
import org.walletconnect.Session
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

internal class WalletRepository(
    private val context: Context,
    private val sessionRepository: SessionRepository,
    private val dispatchers: Dispatchers,
) : WalletManager {

    override fun openWallet() {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = Uri.parse("wc:")
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
    }

    override fun requestHandshake() {
        try {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse(sessionRepository.wcUri)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.d("####", "no vi" + e.message)
            Toast.makeText(context, "no vi", Toast.LENGTH_SHORT).show()
        }
    }

    override suspend fun performTransaction(
        address: String,
        value: String,
        data: String?,
        nonce: String?,
        gasPrice: String?,
        gasLimit: String?,
    ): Result<Session.MethodCall.Response> {
        return withContext(dispatchers.io) {
            suspendCoroutine { continuation ->
                performTransaction(
                    address,
                    value,
                    data,
                    nonce,
                    gasPrice,
                    gasLimit,
                    continuation::resume
                )
            }
        }
    }

    override suspend fun personalSign(message: String): Result<Session.MethodCall.Response> {
        return withContext(dispatchers.io) {
            suspendCoroutine { continuation ->
                personalSign(message, continuation::resume)
            }
        }
    }

    override fun performTransaction(
        address: String,
        value: String,
        data: String?,
        nonce: String?,
        gasPrice: String?,
        gasLimit: String?,
        onResult: (Result<Session.MethodCall.Response>) -> Unit
    ) {
        sessionRepository.address?.let { fromAddress ->
            sessionRepository.session?.let { session ->
                val id = System.currentTimeMillis()
                session.performMethodCall(
                    Session.MethodCall.SendTransaction(
                        id,
                        fromAddress,
                        address,
                        nonce,
                        gasPrice,
                        gasLimit,
                        value.toWei().toHex(),
                        data ?: ""
                    )
                ) { response -> onPerformTransactionResponse(id, response, onResult) }
                openWallet()
            } ?: onResult(Result.failure(Throwable("Session not found!")))
        } ?: onResult(Result.failure(Throwable("Address not found!")))
    }

    override fun personalSign(
        message: String,
        onResult: (Result<Session.MethodCall.Response>) -> Unit
    ) {
        sessionRepository.address?.let { address ->
            sessionRepository.session?.let { session ->
                val id = System.currentTimeMillis()
                val messageParam = if (message.hasHexPrefix()) message else message.toHex()
                session.performMethodCall(
                    Session.MethodCall.Custom(
                        id,
                        "personal_sign",
                        listOf(messageParam, address)
                    )
                ) { response -> onPerformTransactionResponse(id, response, onResult) }
                openWallet()
            } ?: onResult(Result.failure(Throwable("Session not found!")))
        } ?: onResult(Result.failure(Throwable("Address not found!")))
    }

//
//    override suspend fun personalSign(message: String,onResult: Result<Session.MethodCall.Response>){
//        return withContext(dispatchers.io) {
//            suspendCoroutine { continuation ->
//                sessionRepository.address?.let { address ->
//                    sessionRepository.session?.let { session ->
//                        val id = System.currentTimeMillis()
//                        val messageParam = if (message.hasHexPrefix()) message else message.toHex()
//                        session.performMethodCall(
//                            Session.MethodCall.Custom(
//                                id,
//                                "personal_sign",
//                                listOf(messageParam, address)
//                            )
//                        ) { onPerformTransactionResponse1(id, it, onResult) }
//                        openWallet()
//                    } ?: continuation.resumeWith(Result.failure(Throwable("Session not found!")))
//                } ?: continuation.resumeWith(Result.failure(Throwable("Address not found!")))
//            }
//        }
//    }

    private fun onPerformTransactionResponse1(
        id: Long,
        response: Session.MethodCall.Response,
        onResult: Result<Session.MethodCall.Response>
    ) {
        if (id != response.id) {
            val throwable = Throwable("The response id is different from the transaction id!")
            onResult.onFailure {
                throwable
            }
            return
        }
        response.error?.let {
            onResult.onFailure {
                it.message
            }
        } ?: onResult.onSuccess { response }
    }

    private fun onPerformTransactionResponse(
        id: Long,
        response: Session.MethodCall.Response,
        onResult: (Result<Session.MethodCall.Response>) -> Unit
    ) {
        if (id != response.id) {
            val throwable = Throwable("The response id is different from the transaction id!")
            onResult(Result.failure(throwable))
            return
        }
        response.error?.let {
            onResult(Result.failure(Throwable(it.message)))
        } ?: onResult(Result.success(response))
    }
}