package com.example.walletintegrationexample

import com.walletconnect.sign.client.Sign
import com.walletconnect.sign.client.SignClient

object SignRequesterDelegate: SignClient.DappDelegate {
    override fun onSessionApproved(approvedSession: Sign.Model.ApprovedSession) {
        // Triggered when Dapp receives the session approval from wallet
        println("WALLET_CONN -> Sign onSessionApproved $approvedSession")
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