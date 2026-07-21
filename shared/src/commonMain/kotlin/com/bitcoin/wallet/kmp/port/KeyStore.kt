package com.bitcoin.wallet.kmp.port

import com.bitcoin.wallet.kmp.domain.AddressChain
import com.bitcoin.wallet.kmp.domain.BitcoinAddress
import com.bitcoin.wallet.kmp.domain.Mnemonic
import com.bitcoin.wallet.kmp.domain.Network

/** Key generation and BIP84 derivation, implemented per engine. */
interface KeyStore {
    /** Generate a fresh BIP39 mnemonic from secure entropy. */
    fun generateMnemonic(): Mnemonic

    /** Validate a mnemonic phrase (BIP39 wordlist + checksum). */
    fun isValidMnemonic(mnemonic: Mnemonic): Boolean

    /** Derive the BIP84 address at `m/84'/coin'/0'/chain/index`. */
    fun deriveAddress(
        mnemonic: Mnemonic,
        network: Network,
        chain: AddressChain,
        index: Int,
    ): BitcoinAddress

    /**
     * True iff [address] parses (bech32/bech32m segwit or base58 legacy) and
     * belongs to [network]. Never throws.
     */
    fun isValidAddress(address: String, network: Network): Boolean
}
