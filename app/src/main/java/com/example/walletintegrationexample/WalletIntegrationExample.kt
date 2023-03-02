package com.example.walletintegrationexample

import android.app.Application
import android.content.Context
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.walletconnect.android.Core
import com.walletconnect.android.CoreClient
import com.walletconnect.android.relay.ConnectionType
import com.walletconnect.auth.client.Auth
import com.walletconnect.auth.client.AuthClient
import com.walletconnect.sign.client.Sign
import com.walletconnect.sign.client.SignClient
import okhttp3.OkHttpClient
import org.komputing.khex.extensions.toNoPrefixHexString
import org.walletconnect.Session
import org.walletconnect.impls.*
import org.walletconnect.nullOnThrow
import java.io.File
import java.util.*

class WalletIntegrationExample: Application() {
    override fun onCreate() {
        super.onCreate()
        //  for v1
        moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
        client = OkHttpClient.Builder().build()
        storage = FileWCSessionStore(File(cacheDir, "session_store.json").apply { createNewFile() }, moshi)

        //for v2
        val projectId = "5c5eea0af0876592f35813c046742af3" //Get Project ID at https://cloud.walletconnect.com/
        val relayUrl = "relay.walletconnect.com"
        val serverUrl = "wss://$relayUrl?projectId=${projectId}"
        val connectionType = ConnectionType.AUTOMATIC
        val application = this //Android Application level class
        println("WALLET_CONN -> serverUrl: $serverUrl")
        val appMetaData = Core.Model.AppMetaData(
            name = "WalletIntegration Example",
            description = "Kotlin AuthSDK Requester Implementation",
            url = "wheelco.in",
            icons = listOf("https://raw.githubusercontent.com/WalletConnect/walletconnect-assets/master/Icon/Gradient/Icon.png"),
            redirect = null
        )
        CoreClient.initialize(relayServerUrl = serverUrl, connectionType = connectionType, application = application, metaData = appMetaData) {error ->
            println("WALLET_CONN -> Error initialize Core $error")
        }

        SignClient.initialize(init = Sign.Params.Init(core = CoreClient)) { error ->
            println("WALLET_CONN -> Error initialize Sign $error")
        }


    }

    companion object {
        private lateinit var client: OkHttpClient
        private lateinit var moshi: Moshi
        private lateinit var storage: WCSessionStore
        lateinit var config: Session.Config
        lateinit var session: Session

        private const val bridgeUrl = "https://bridge.walletconnect.org"
        private val metaData = Session.PeerMeta(
            name = "Wheelcoin",
            url = "wheelco.in",
            description = "Wheelcoin WalletConnect demo"
        )

        fun resetSession() {
            nullOnThrow { session }?.clearCallbacks()
            val key = ByteArray(32).also { Random().nextBytes(it) }.toNoPrefixHexString()
            config = Session.Config(UUID.randomUUID().toString(), bridgeUrl, key)
            session = WCSession(
                config,
                MoshiPayloadAdapter(moshi),
                storage,
                OkHttpTransport.Builder(client, moshi),
                metaData
            )
            session.offer()
        }
    }
}