package com.bitcoin.wallet.kmp.wallet

import com.bitcoin.wallet.kmp.domain.BitcoinAddress
import com.bitcoin.wallet.kmp.domain.Mnemonic
import com.bitcoin.wallet.kmp.domain.Network
import com.bitcoin.wallet.kmp.domain.WalletError
import com.bitcoin.wallet.kmp.domain.WalletResult
import com.bitcoin.wallet.kmp.port.KeyStore

/** A newly created wallet: its mnemonic and its first receive address. */
data class NewWallet(
    val mnemonic: Mnemonic,
    val firstAddress: BitcoinAddress,
) {
    // Redacted: this embeds the secret [Mnemonic], so the default toString()
    // would transitively print the seed. Address is non-secret, so keep it.
    override fun toString(): String = "NewWallet(mnemonic=****, firstAddress=$firstAddress)"
}

class OnChainWallet(
    private val keyStore: KeyStore,
    private val network: Network,
) {
    /** Generate a new mnemonic and derive its first receive address. */
    fun create(): WalletResult<NewWallet> = runEngine {
        val mnemonic = keyStore.generateMnemonic()
        NewWallet(mnemonic, keyStore.firstReceiveAddress(mnemonic, network))
    }

    /** Restore from an existing mnemonic, validating it first. */
    fun restore(mnemonic: Mnemonic): WalletResult<NewWallet> {
        if (!keyStore.isValidMnemonic(mnemonic)) {
            return WalletResult.Failure(WalletError.InvalidInput("invalid mnemonic"))
        }
        return runEngine {
            NewWallet(mnemonic, keyStore.firstReceiveAddress(mnemonic, network))
        }
    }

    private inline fun <T> runEngine(block: () -> T): WalletResult<T> =
        runCatching { WalletResult.Success(block()) }
            .getOrElse { WalletResult.Failure(WalletError.Engine(it.message ?: it::class.simpleName ?: "engine error")) }
}
