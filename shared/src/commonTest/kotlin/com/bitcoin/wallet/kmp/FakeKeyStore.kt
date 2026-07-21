package com.bitcoin.wallet.kmp

import com.bitcoin.wallet.kmp.domain.AddressChain
import com.bitcoin.wallet.kmp.domain.BitcoinAddress
import com.bitcoin.wallet.kmp.domain.Mnemonic
import com.bitcoin.wallet.kmp.domain.Network
import com.bitcoin.wallet.kmp.port.KeyStore

/**
 * Deterministic, crypto-free [KeyStore] for wallet-logic tests.
 *
 * Derived addresses are labeled `fake-<chain>-<index>` so assertions can read
 * the chain and index straight off the value. For behavior a single test needs
 * (capturing arguments, throwing from one method), wrap with interface
 * delegation instead of adding knobs here:
 * `object : KeyStore by FakeKeyStore(...) { override fun ... }`.
 */
class FakeKeyStore(
    private val mnemonic: Mnemonic = Mnemonic(List(12) { "test" }),
    private val validMnemonics: Set<String> = setOf(mnemonic.phrase),
    /** When set, the next derivation of this index throws once, then derivation recovers. */
    var failDerivationAtIndex: Int? = null,
) : KeyStore {

    override fun generateMnemonic() = mnemonic

    override fun isValidMnemonic(mnemonic: Mnemonic) = mnemonic.phrase in validMnemonics

    override fun deriveAddress(
        mnemonic: Mnemonic,
        network: Network,
        chain: AddressChain,
        index: Int,
    ): BitcoinAddress {
        if (index == failDerivationAtIndex) {
            failDerivationAtIndex = null
            throw IllegalStateException("derivation failed at index $index")
        }
        return BitcoinAddress("fake-${chain.name.lowercase()}-$index")
    }

    override fun isValidAddress(address: String, network: Network) = address.startsWith("fake-")
}
