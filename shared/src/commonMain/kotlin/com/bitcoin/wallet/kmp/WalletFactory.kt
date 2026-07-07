package com.bitcoin.wallet.kmp

import com.bitcoin.wallet.kmp.domain.Network
import com.bitcoin.wallet.kmp.onchain.engine.acinq.AcinqKeyStore
import com.bitcoin.wallet.kmp.wallet.OnChainWallet

/** Builds an [OnChainWallet] wired to the ACINQ engine. */
object WalletFactory {
    fun onChainWallet(network: Network = Network.SIGNET): OnChainWallet =
        OnChainWallet(keyStore = AcinqKeyStore(), network = network)
}
