package com.bitcoin.wallet.kmp

import com.bitcoin.wallet.kmp.domain.AddressChain
import com.bitcoin.wallet.kmp.domain.Mnemonic
import com.bitcoin.wallet.kmp.domain.Network
import com.bitcoin.wallet.kmp.onchain.engine.acinq.AcinqKeyStore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * AcinqKeyStore against the official BIP84 test vector. The "abandon … about"
 * mnemonic and its first mainnet address are published in BIP84.
 */
class AcinqKeyStoreTest {

    private val testVectorMnemonic = Mnemonic(
        ("abandon abandon abandon abandon abandon abandon " +
            "abandon abandon abandon abandon abandon about").split(" ")
    )

    private val keyStore = AcinqKeyStore()

    @Test
    fun bip84_mainnet_first_address_matches_official_vector() {
        // From BIP84 spec, Account 0, first receiving address.
        val expected = "bc1qcr8te4kr609gcawutmrza0j4xv80jy8z306fyu"
        val actual = keyStore.deriveAddress(testVectorMnemonic, Network.MAINNET, AddressChain.EXTERNAL, 0)
        assertEquals(expected, actual.value)
    }

    @Test
    fun bip84_signet_first_address_is_native_segwit_tb() {
        val address = keyStore.deriveAddress(testVectorMnemonic, Network.SIGNET, AddressChain.EXTERNAL, 0)
        // Same witness program as mainnet, signet/testnet HRP "tb".
        assertEquals("tb1q6rz28mcfaxtmd6v789l9rrlrusdprr9pqcpvkl", address.value)
        assertTrue(address.value.startsWith("tb1q"), "signet P2WPKH must start with tb1q")
    }

    @Test
    fun negative_index_is_rejected() {
        assertFailsWith<IllegalArgumentException> {
            keyStore.deriveAddress(testVectorMnemonic, Network.MAINNET, AddressChain.EXTERNAL, -1)
        }
    }

    @Test
    fun generated_mnemonic_is_valid_and_12_words() {
        val mnemonic = keyStore.generateMnemonic()
        assertEquals(12, mnemonic.words.size)
        assertTrue(keyStore.isValidMnemonic(mnemonic))
    }

    @Test
    fun invalid_mnemonic_is_rejected() {
        val bad = Mnemonic(List(12) { "abandon" }) // wrong checksum
        assertFalse(keyStore.isValidMnemonic(bad))
    }

    @Test
    fun valid_entropy_sizes_produce_expected_word_counts() {
        // BIP39: each 32 bits of entropy adds 3 words (16B→12, 32B→24).
        val expectedWords = mapOf(16 to 12, 20 to 15, 24 to 18, 28 to 21, 32 to 24)
        for ((bytes, words) in expectedWords) {
            val mnemonic = AcinqKeyStore(entropyBytes = bytes).generateMnemonic()
            assertEquals(words, mnemonic.words.size, "$bytes bytes should yield $words words")
            assertTrue(keyStore.isValidMnemonic(mnemonic), "$bytes-byte mnemonic must validate")
        }
    }

    @Test
    fun invalid_entropy_size_is_rejected_at_construction() {
        // 17 is not a BIP39 size; this used to fail cryptically deep inside ACINQ.
        assertFailsWith<IllegalArgumentException> { AcinqKeyStore(entropyBytes = 17) }
        // Boundary: 0 and a too-large value are also rejected.
        assertFailsWith<IllegalArgumentException> { AcinqKeyStore(entropyBytes = 0) }
        assertFailsWith<IllegalArgumentException> { AcinqKeyStore(entropyBytes = 64) }
    }
}
