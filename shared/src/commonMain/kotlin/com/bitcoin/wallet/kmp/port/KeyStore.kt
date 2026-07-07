package com.bitcoin.wallet.kmp.port

import com.bitcoin.wallet.kmp.domain.BitcoinAddress
import com.bitcoin.wallet.kmp.domain.Mnemonic
import com.bitcoin.wallet.kmp.domain.Network

/** Key generation and BIP84 derivation, implemented per engine. */
interface KeyStore {
    /** Generate a fresh BIP39 mnemonic from secure entropy. */
    fun generateMnemonic(): Mnemonic

    /** Validate a mnemonic phrase (BIP39 wordlist + checksum). */
    fun isValidMnemonic(mnemonic: Mnemonic): Boolean

    /** Derive the first external BIP84 receive address (`m/84'/coin'/0'/0/0`). */
    fun firstReceiveAddress(mnemonic: Mnemonic, network: Network): BitcoinAddress
}
