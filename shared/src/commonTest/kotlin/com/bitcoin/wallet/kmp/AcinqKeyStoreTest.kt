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
    fun bip84_mainnet_second_receive_address_matches_official_vector() {
        // From BIP84 spec, Account 0, second receiving address (m/84'/0'/0'/0/1):
        // proves index advances the external chain, not just index 0.
        val expected = "bc1qnjg0jd8228aq7egyzacy8cys3knf9xvrerkf9g"
        val actual = keyStore.deriveAddress(testVectorMnemonic, Network.MAINNET, AddressChain.EXTERNAL, 1)
        assertEquals(expected, actual.value)
    }

    @Test
    fun bip84_mainnet_first_change_address_matches_official_vector() {
        // From BIP84 spec, Account 0, first change address (m/84'/0'/0'/1/0):
        // proves the INTERNAL chain maps to the …/1/i change branch.
        val expected = "bc1q8c6fshw2dlwun7ekn9qwf37cu2rn755upcp6el"
        val actual = keyStore.deriveAddress(testVectorMnemonic, Network.MAINNET, AddressChain.INTERNAL, 0)
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
    fun valid_addresses_are_accepted() {
        // BIP350 test vectors: segwit v0 is bech32, v1+ is bech32m; case-insensitive.
        assertTrue(keyStore.isValidAddress("BC1QW508D6QEJXTDG4Y5R3ZARVARY0C5XW7KV8F3T4", Network.MAINNET))
        assertTrue(keyStore.isValidAddress("bc1qw508d6qejxtdg4y5r3zarvary0c5xw7kv8f3t4", Network.MAINNET))
        // P2WSH (32-byte witness program); "tb" HRP serves testnet AND signet.
        assertTrue(keyStore.isValidAddress("tb1qrp33g0q5c5txsp9arysrx4k6zdkfs4nce4xj0gdcccefvpysxf3q0sl5k7", Network.TESTNET))
        assertTrue(keyStore.isValidAddress("tb1qrp33g0q5c5txsp9arysrx4k6zdkfs4nce4xj0gdcccefvpysxf3q0sl5k7", Network.SIGNET))
        // Taproot (witness v1, bech32m).
        assertTrue(keyStore.isValidAddress("bc1p0xlxvlhemja6c4dqv22uapctqupfhlxm9h8z3k2e72q4k9hcz7vqzk5jj0", Network.MAINNET))
        // Base58 legacy P2PKH: the decoder intentionally accepts pre-segwit addresses.
        assertTrue(keyStore.isValidAddress("1BvBMSEYstWetqTFn5Au4m4GFg7xJaNVN2", Network.MAINNET))
    }

    @Test
    fun invalid_addresses_are_rejected() {
        // BIP350 invalid vectors: wrong checksum algorithm for the witness version.
        assertFalse(keyStore.isValidAddress("bc1p0xlxvlhemja6c4dqv22uapctqupfhlxm9h8z3k2e72q4k9hcz7vqh2y7hd", Network.MAINNET)) // bech32 where bech32m required (v1)
        assertFalse(keyStore.isValidAddress("bc1qw508d6qejxtdg4y5r3zarvary0c5xw7kemeawh", Network.MAINNET)) // bech32m where bech32 required (v0)
        assertFalse(keyStore.isValidAddress("tc1p0xlxvlhemja6c4dqv22uapctqupfhlxm9h8z3k2e72q4k9hcz7vq5zuyut", Network.MAINNET)) // invalid HRP
        assertFalse(keyStore.isValidAddress("BC1QR508D6QEJXTDG4Y5R3ZARVARYV98GJ9P", Network.MAINNET)) // invalid program length for v0
        assertFalse(keyStore.isValidAddress("", Network.MAINNET))
        assertFalse(keyStore.isValidAddress("not-an-address", Network.MAINNET))
    }

    @Test
    fun network_mismatched_addresses_are_rejected() {
        assertFalse(keyStore.isValidAddress("bc1qw508d6qejxtdg4y5r3zarvary0c5xw7kv8f3t4", Network.SIGNET))
        assertFalse(keyStore.isValidAddress("tb1q6rz28mcfaxtmd6v789l9rrlrusdprr9pqcpvkl", Network.MAINNET))
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
