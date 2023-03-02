package com.example.walletintegrationexample

import com.walletconnect.auth.client.Auth
import com.walletconnect.auth.client.AuthClient

object WalletRequesterDelegate : AuthClient.RequesterDelegate {
    override fun onAuthResponse(authResponse: Auth.Event.AuthResponse) {
        // Triggered when Wallet / Responder responds to authorization request. Result can be either signed Cacao object or Error
        println("WALLET_CONN -> Auth onAuthResponse $authResponse")
    }

    override fun onConnectionStateChange(connectionStateChange: Auth.Event.ConnectionStateChange) {
        // Triggered whenever the connection state is changed
        println("WALLET_CONN -> Auth onConnectionStateChange $connectionStateChange")
    }

    override fun onError(error: Auth.Event.Error) {
        //Triggered whenever the error occurs with Relay Server
        println("WALLET_CONN -> Auth onError $error")

    }
}
