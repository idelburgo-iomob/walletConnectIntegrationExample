package com.example.walletintegrationexample.ui.main

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.walletintegrationexample.SignRequesterDelegate
import com.example.walletintegrationexample.WalletIntegrationExample
import com.example.walletintegrationexample.WalletRequesterDelegate
import com.walletconnect.android.Core
import com.walletconnect.android.CoreClient
import com.walletconnect.auth.client.Auth
import com.walletconnect.auth.client.AuthClient
import com.walletconnect.sign.client.Sign
import com.walletconnect.sign.client.SignClient
import org.walletconnect.Session
import java.security.MessageDigest
import kotlin.random.Random

class MainViewModel: ViewModel(), Session.Callback {

    private val _connectionUri = MutableLiveData<String>()
    val connectionUri: LiveData<String>
        get() = _connectionUri

    private val _address = MutableLiveData<String?>()
    val address: LiveData<String?>
        get() = _address

    private val _goMetamask = MutableLiveData<Boolean>()
    val goMetamask: LiveData<Boolean>
        get() = _goMetamask

    init {
        SignClient.setDappDelegate(SignRequesterDelegate)
    }

    fun connect() {
        WalletIntegrationExample.resetSession()
        WalletIntegrationExample.session.addCallback(this)
        var deep = WalletIntegrationExample.config.toWCUri()
        println("WALLET_CONN -> V1 LINK: $deep")
        _connectionUri.value = "metamask://wc?uri=${WalletIntegrationExample.config.toWCUri()}"
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
    }

    override fun onStatus(status: Session.Status) {
        println("WALLET_CONN -> V1 status $status")
        when(status) {
            Session.Status.Approved -> {
                sessionApproved()
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


    fun signV2() {
        val namespace: String = "eip155"/*Namespace identifier, see for reference: https://github.com/ChainAgnostic/CAIPs/blob/master/CAIPs/caip-2.md#syntax*/
        val chains: List<String> = listOf("eip155:42220")/*List of chains that wallet will be requested for*/
        val methods: List<String> = listOf(
            "personal_sign"
        )/*List of methods that wallet will be requested for*/
        val events: List<String> = listOf("chainChanged", "accountChanged")/*List of events that wallet will be requested for*/
        val namespaces: Map<String, Sign.Model.Namespace.Proposal> = mapOf(namespace to Sign.Model.Namespace.Proposal(chains, methods, events))
        val pairing: Core.Model.Pairing = CoreClient.Pairing.create()!! /*Either an active or inactive pairing*/
        val connectParams = Sign.Params.Connect(
            namespaces = namespaces,
            optionalNamespaces = null,
            properties = null,
            pairing = pairing)

        SignClient.connect(connectParams,
            onSuccess = {
                println("WALLET_CONN -> SignClient success")
                var deeplink = pairing.uri
                println("WALLET_CONN -> link: $deeplink")
                _connectionUri.postValue("$deeplink")
            }, onError = { error ->
                println("WALLET_CONN -> SignClient error: $error")
            })
//        val params = getPersonalSignBody("")
//        val requestParams = Sign.Params.Request(
//            sessionTopic = requireNotNull(SignClient.DappDelegate.selectedSessionTopic),
//            method = "personal_sign",
//            params = params, // stringified JSON
//            chainId = "eip155:42220"
//        )
//        SignClient.request(requestParams, onSuccess = {}, onError = {})

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