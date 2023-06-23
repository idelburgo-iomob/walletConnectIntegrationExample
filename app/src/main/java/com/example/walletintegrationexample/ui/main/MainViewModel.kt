package com.example.walletintegrationexample.ui.main

import android.content.ClipData
import android.content.ClipboardManager
import android.net.Uri
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.walletintegrationexample.WalletIntegrationExample
import com.walletconnect.android.Core
import com.walletconnect.android.CoreClient
import com.walletconnect.android.pairing.model.PairingParams
import com.walletconnect.sign.client.Sign
import com.walletconnect.sign.client.SignClient
import org.walletconnect.Session
import java.math.BigInteger
import java.net.URLEncoder
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import kotlin.random.Random

class MainViewModel: ViewModel(), Session.Callback, SignClient.DappDelegate {

    private val _connectionUri = MutableLiveData<String>()
    val connectionUri: LiveData<String>
        get() = _connectionUri

    private val _address = MutableLiveData<String?>()
    val address: LiveData<String?>
        get() = _address
    private var pairingDeeplink = ""

    private val _goMetamask = MutableLiveData<Boolean>()
    val goMetamask: LiveData<Boolean>
        get() = _goMetamask

    init {
        SignClient.setDappDelegate(this)
    }

    fun connect() {
        WalletIntegrationExample.resetSession()
        WalletIntegrationExample.session.addCallback(this)
        var deep = WalletIntegrationExample.config.toWCUri()
        println("WALLET_CONN -> V1 LINK: $deep")
//        _connectionUri.value = "metamask://wc?uri=${WalletIntegrationExample.config.toWCUri()}"
//        _connectionUri.value = "https://valoraapp.com/wc?uri=${WalletIntegrationExample.config.toWCUri()}"
//        _connectionUri.value = "wc://wc?uri=${deep}"
        _connectionUri.value = deep
        _goMetamask.value = false
    }

    fun disconnect() {
        WalletIntegrationExample.session.kill()
    }

    fun signMessage() {
        val address = WalletIntegrationExample.session.approvedAccounts()?.first() ?: ""
        println("WALLET_CONN -> V1 ADDRESS: $address")
        val unsignedMessage = "Logging in to wallet %s at %s".format(address, System.currentTimeMillis().toString())
        println("WALLET_CONN -> V1 MESSAGE: $unsignedMessage")
        WalletIntegrationExample.session.performMethodCall(
            Session.MethodCall.Custom(
                id = System.currentTimeMillis(),
                method = "personal_sign",
                params = listOf(unsignedMessage, address)
            )
        ) {
            println("WALLET_CONN -> V1 signingResponse: $it")
            _goMetamask.postValue(false)
        }
        _goMetamask.postValue(true)
    }

    fun String.hash(): String {
        val bytes = this.toString().toByteArray()
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return digest.fold("", { str, it -> str + "%02x".format(it) })
    }

    override fun onMethodCall(call: Session.MethodCall) {
        println("WALLET_CONN -> V1 onMethodCall $call")
        val parsed = (call as Session.MethodCall.Response)
        val tree = parsed.result as? Map<String, Any>
        // Use this to check if user approved and know the chainId to which was connected
        if (tree != null && tree["approved"] == true && tree["chainId"] ==
            42220.0) {
            sessionApproved()
        }
    }

    override fun onStatus(status: Session.Status) {
        println("WALLET_CONN -> V1 status $status")
        when(status) {
            Session.Status.Approved -> {
//                sessionApproved() // use onMethodCall to get the chainID
            }
            Session.Status.Closed -> {
                sessionClosed()
            }
            Session.Status.Connected,
            Session.Status.Disconnected,
            is Session.Status.Error -> {
                // Do Stuff
            }
        }
    }

    private fun sessionClosed() {
        _address.postValue(null)
    }

    private fun sessionApproved() {
        _address.postValue(WalletIntegrationExample.session.approvedAccounts()?.first())
    }

    fun signV2Message() {
        val params = getPersonalSignBody(account)
        val (parentChain, chainId,  account) = account.split(":")
        val requestParams = Sign.Params.Request(
            sessionTopic = requireNotNull(topicApproved),
            method = "personal_sign",
            params = params, // stringified JSON
            chainId = "$parentChain:$chainId"
        )
        val redirect = SignClient.getActiveSessionByTopic(requestParams.sessionTopic)
        println("WALLET_CONN: active session $redirect")
        SignClient.request(request = requestParams, onSuccess = { it: Sign.Model.SentRequest ->
            println("WALLET_CONN -> Sign request success $it")
        }, onError = { error ->
            println("WALLET_CONN -> Sign request error $error")
        })
        _connectionUri.postValue(pairingDeeplink)
    }


    // This method uses sign, but it is to connect to the wallet
    fun signV2() {
        /*Namespace identifier*/
        val namespace: String = "eip155" // reference: https://github.com/ChainAgnostic/CAIPs/blob/master/CAIPs/caip-2.md#syntax
        /*List of chains that wallet will be requested for*/
        val chains: List<String> = listOf(
            "eip155:1" // Celo main net
        )
        /*List of methods that wallet will be requested for*/
        val methods: List<String> = listOf(
            "personal_sign" // ATM we only want to sign
        )
        /*List of events that wallet will be requested for*/
        val events: List<String> = listOf(
            "chainChanged",
            "accountChanged"
        ) // Actually all possible events

        val expiry = (System.currentTimeMillis() / 1000) + TimeUnit.SECONDS.convert(7, TimeUnit.DAYS)
        val properties: Map<String, String> = mapOf("sessionExpiry" to "$expiry")

        val namespaces: Map<String, Sign.Model.Namespace.Proposal> = mapOf(
            namespace to Sign.Model.Namespace.Proposal(
                chains,
                methods,
                events)
        )

        val pairing: Core.Model.Pairing = CoreClient.Pairing.create()!!

        val connectParams = Sign.Params.Connect(
            namespaces = namespaces,
            optionalNamespaces = null,
            properties = null,
            pairing = pairing)

        SignClient.connect(connectParams,
        onSuccess = {
            println("WALLET_CONN -> SignClient success")
            var deeplink = pairing.uri
            val replaced = deeplink.replace("wc:", "wc://")
            println("WALLET_CONN -> link: $replaced")
            pairingDeeplink = replaced
            _connectionUri.postValue(replaced)
        }, onError = { error ->
            println("WALLET_CONN -> SignClient error: $error")
        })
    }

    fun getPersonalSignBody(account: String): String {
        val msg = "My email is john@doe.com - ${System.currentTimeMillis()}".encodeToByteArray()
            .joinToString(separator = "", prefix = "0x") { eachByte -> "%02x".format(eachByte) }
        return "[\"$msg\", \"$account\"]"
    }
    private var topicApproved = ""
    private var account = ""
    override fun onSessionApproved(approvedSession: Sign.Model.ApprovedSession) {
        // Triggered when Dapp receives the session approval from wallet
        println("WALLET_CONN -> Sign onSessionApproved $approvedSession")
        topicApproved = approvedSession.topic
        account = approvedSession.accounts[0]
        _address.postValue(account)

    }

    override fun onSessionRejected(rejectedSession: Sign.Model.RejectedSession) {
    // Triggered when Dapp receives the session rejection from wallet
        println("WALLET_CONN -> Sign onSessionRejected $rejectedSession")
    }

    override fun onSessionUpdate(updatedSession: Sign.Model.UpdatedSession) {
        // Triggered when Dapp receives the session update from wallet
        println("WALLET_CONN -> Sign onSessionUpdate $updatedSession")
    }

    override fun onSessionExtend(session: Sign.Model.Session) {
    // Triggered when Dapp receives the session extend from wallet
        println("WALLET_CONN -> Sign onSessionExtended $session")
    }

    override fun onSessionEvent(sessionEvent: Sign.Model.SessionEvent) {
    // Triggered when the peer emits events that match the list of events agreed upon session settlement
        println("WALLET_CONN -> Sign onSessionEvent $sessionEvent")
    }

    override fun onSessionDelete(deletedSession: Sign.Model.DeletedSession) {
    // Triggered when Dapp receives the session delete from wallet
        println("WALLET_CONN -> Sign onSessionDelete $deletedSession")

    }

    override fun onSessionRequestResponse(response: Sign.Model.SessionRequestResponse) {
    // Triggered when Dapp receives the session request response from wallet
        println("WALLET_CONN -> Sign onSessionRequest $response")

    }

    override fun onConnectionStateChange(state: Sign.Model.ConnectionState) {
    //Triggered whenever the connection state is changed
        println("WALLET_CONN -> Sign onConnectionState $state")

    }

    override fun onError(error: Sign.Model.Error) {
    // Triggered whenever there is an issue inside the SDK
        println("WALLET_CONN -> Sign onError $error")
    }


    }

    fun randomNonce(): String = Random.nextBytes(16).bytesToHex()

    fun ByteArray.bytesToHex(): String {
        val hexString = StringBuilder(2 * this.size)

        this.indices.forEach { i ->
        val hex = Integer.toHexString(0xff and this[i].toInt())

        if (hex.length == 1) {
            hexString.append('0')
        }

        hexString.append(hex)
        }

        return hexString.toString()
    }