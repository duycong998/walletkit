package dev.pinkroom.walletconnectkit.data.session

import dev.pinkroom.walletconnectkit.WalletConnectKitConfig
import dev.pinkroom.walletconnectkit.crypto.KeyRepository
import org.komputing.khex.extensions.toNoPrefixHexString
import org.walletconnect.Session
import org.walletconnect.impls.OkHttpTransport
import org.walletconnect.impls.WCSession
import org.walletconnect.impls.WCSessionStore
import java.util.*

internal class SessionRepository(
    private val payloadAdapter: Session.PayloadAdapter,
    private val storage: WCSessionStore,
    private val transporter: OkHttpTransport.Builder,
    private val walletConnectKitConfig: WalletConnectKitConfig,
) : SessionManager {

    private var config: Session.Config? = null
    override var session: Session? = null
    override val address get() = session?.approvedAccounts()?.firstOrNull()
    internal val wcUri get() = config?.toWCUri()

    override fun createSession() = createSession(null)

    override fun removeSession() {
        session?.kill()
        session?.clearCallbacks()
        storage.clean()
        session = null
    }

    override fun loadSession() = loadSession(null)

    override val isSessionStored get() = storage.list().firstOrNull() != null

    internal fun createSession(callback: Session.Callback?) {
        config = buildConfig().also {
            session = buildSession(it).apply {
                callback?.let(::addCallback)
                offer()
            }
        }
    }

    internal fun loadSession(callback: Session.Callback?) {
        storage.list().firstOrNull()?.let {
            config = Session.Config(
                it.config.handshakeTopic,
                it.config.bridge,
                it.config.key,
                it.config.protocol,
                it.config.version
            )
            session = WCSession(
                it.config,
                payloadAdapter,
                storage,
                transporter,
                walletConnectKitConfig.clientMeta
            ).apply { callback?.let(::addCallback) }
        }
    }

    private fun buildConfig(): Session.Config {
        val handshakeTopic = UUID.randomUUID().toString()
        val key = KeyRepository.generate()
        return Session.Config(handshakeTopic, walletConnectKitConfig.bridgeUrl, key, "wc", 1)
    }

    private fun buildSession(config: Session.Config) = WCSession(
        config.toFullyQualifiedConfig(),
        payloadAdapter,
        storage,
        transporter,
        walletConnectKitConfig.clientMeta
    )

    private fun WCSessionStore.clean() = list().forEach { remove(it.config.handshakeTopic) }
}