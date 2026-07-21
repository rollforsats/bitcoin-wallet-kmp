package com.bitcoin.wallet.kmp

import com.bitcoin.wallet.kmp.domain.Mnemonic

/**
 * Official BIP84 test vectors
 * (github.com/bitcoin/bips/blob/master/bip-0084.mediawiki).
 *
 * All addresses derive from [MNEMONIC], the canonical test mnemonic: the
 * 12 words encoding all-zeros entropy plus its checksum word ("about").
 * Path anatomy: m/84'/coin'/account'/chain/index, where chain 0 = external
 * (receive) and 1 = internal (change).
 */
object Bip84Vectors {
    val MNEMONIC = Mnemonic(
        ("abandon abandon abandon abandon abandon abandon " +
            "abandon abandon abandon abandon abandon about").split(" ")
    )

    /** m/84'/0'/0'/0/0 — mainnet, first receiving address. */
    const val MAINNET_RECEIVE_0 = "bc1qcr8te4kr609gcawutmrza0j4xv80jy8z306fyu"

    /** m/84'/0'/0'/0/1 — mainnet, second receiving address. */
    const val MAINNET_RECEIVE_1 = "bc1qnjg0jd8228aq7egyzacy8cys3knf9xvrerkf9g"

    /** m/84'/0'/0'/1/0 — mainnet, first change address. */
    const val MAINNET_CHANGE_0 = "bc1q8c6fshw2dlwun7ekn9qwf37cu2rn755upcp6el"

    /**
     * m/84'/1'/0'/0/0 — first receiving address for coin type 1' (all test
     * networks), HRP "tb". The BIP only publishes mainnet addresses; this one
     * is pinned from our own derivation so signet output can't drift.
     */
    const val SIGNET_RECEIVE_0 = "tb1q6rz28mcfaxtmd6v789l9rrlrusdprr9pqcpvkl"
}

/**
 * Address decode vectors from BIP350's test-vector table
 * (github.com/bitcoin/bips/blob/master/bip-0350.mediawiki), which supersedes
 * BIP173: segwit v0 must be bech32-encoded, v1+ must be bech32m-encoded.
 */
object AddressDecodeVectors {
    // -- Valid --

    /** Mainnet P2WPKH (segwit v0, 20-byte witness program), uppercase form. */
    const val P2WPKH_MAINNET_UPPER = "BC1QW508D6QEJXTDG4Y5R3ZARVARY0C5XW7KV8F3T4"

    /** Same P2WPKH lowercase — bech32 is case-insensitive (but never mixed-case). */
    const val P2WPKH_MAINNET = "bc1qw508d6qejxtdg4y5r3zarvary0c5xw7kv8f3t4"

    /** Testnet/signet P2WSH (segwit v0, 32-byte witness program); HRP "tb" serves both. */
    const val P2WSH_TESTNET = "tb1qrp33g0q5c5txsp9arysrx4k6zdkfs4nce4xj0gdcccefvpysxf3q0sl5k7"

    /** Mainnet P2TR (taproot, segwit v1, bech32m, HRP "bc1p"). */
    const val P2TR_MAINNET = "bc1p0xlxvlhemja6c4dqv22uapctqupfhlxm9h8z3k2e72q4k9hcz7vqzk5jj0"

    /** Mainnet legacy P2PKH (base58check, pre-segwit, leading "1"). */
    const val P2PKH_MAINNET_LEGACY = "1BvBMSEYstWetqTFn5Au4m4GFg7xJaNVN2"

    // -- Invalid --

    /** Witness v1 program encoded with bech32 where bech32m is required. */
    const val V1_WITH_BECH32_CHECKSUM = "bc1p0xlxvlhemja6c4dqv22uapctqupfhlxm9h8z3k2e72q4k9hcz7vqh2y7hd"

    /** Witness v0 program encoded with bech32m where bech32 is required. */
    const val V0_WITH_BECH32M_CHECKSUM = "bc1qw508d6qejxtdg4y5r3zarvary0c5xw7kemeawh"

    /** Human-readable part "tc" belongs to no Bitcoin network. */
    const val UNKNOWN_HRP = "tc1p0xlxvlhemja6c4dqv22uapctqupfhlxm9h8z3k2e72q4k9hcz7vq5zuyut"

    /** Witness v0 program that is neither 20 nor 32 bytes (BIP141 rejects it). */
    const val V0_BAD_PROGRAM_LENGTH = "BC1QR508D6QEJXTDG4Y5R3ZARVARYV98GJ9P"
}
