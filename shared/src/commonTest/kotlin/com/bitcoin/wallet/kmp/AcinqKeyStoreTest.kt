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

/** AcinqKeyStore against the official vectors in [Bip84Vectors] and [AddressDecodeVectors]. */
class AcinqKeyStoreTest {

    private val keyStore = AcinqKeyStore()

    @Test
    fun bip84_mainnet_first_address_matches_official_vector() {
        val actual = keyStore.deriveAddress(Bip84Vectors.MNEMONIC, Network.MAINNET, AddressChain.EXTERNAL, 0)
        assertEquals(Bip84Vectors.MAINNET_RECEIVE_0, actual.value)
    }

    @Test
    fun bip84_mainnet_second_receive_address_matches_official_vector() {
        // Proves index advances the external chain, not just index 0.
        val actual = keyStore.deriveAddress(Bip84Vectors.MNEMONIC, Network.MAINNET, AddressChain.EXTERNAL, 1)
        assertEquals(Bip84Vectors.MAINNET_RECEIVE_1, actual.value)
    }

    @Test
    fun bip84_mainnet_first_change_address_matches_official_vector() {
        // Proves the INTERNAL chain maps to the …/1/i change branch.
        val actual = keyStore.deriveAddress(Bip84Vectors.MNEMONIC, Network.MAINNET, AddressChain.INTERNAL, 0)
        assertEquals(Bip84Vectors.MAINNET_CHANGE_0, actual.value)
    }

    @Test
    fun bip84_signet_first_address_is_native_segwit_tb() {
        val address = keyStore.deriveAddress(Bip84Vectors.MNEMONIC, Network.SIGNET, AddressChain.EXTERNAL, 0)
        assertEquals(Bip84Vectors.SIGNET_RECEIVE_0, address.value)
        assertTrue(address.value.startsWith("tb1q"), "signet P2WPKH must start with tb1q")
    }

    @Test
    fun negative_index_is_rejected() {
        assertFailsWith<IllegalArgumentException> {
            keyStore.deriveAddress(Bip84Vectors.MNEMONIC, Network.MAINNET, AddressChain.EXTERNAL, -1)
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
        assertTrue(keyStore.isValidAddress(AddressDecodeVectors.P2WPKH_MAINNET_UPPER, Network.MAINNET))
        assertTrue(keyStore.isValidAddress(AddressDecodeVectors.P2WPKH_MAINNET, Network.MAINNET))
        assertTrue(keyStore.isValidAddress(AddressDecodeVectors.P2WSH_TESTNET, Network.TESTNET))
        assertTrue(keyStore.isValidAddress(AddressDecodeVectors.P2WSH_TESTNET, Network.SIGNET))
        assertTrue(keyStore.isValidAddress(AddressDecodeVectors.P2TR_MAINNET, Network.MAINNET))
        // The decoder intentionally accepts pre-segwit addresses.
        assertTrue(keyStore.isValidAddress(AddressDecodeVectors.P2PKH_MAINNET_LEGACY, Network.MAINNET))
    }

    @Test
    fun invalid_addresses_are_rejected() {
        assertFalse(keyStore.isValidAddress(AddressDecodeVectors.V1_WITH_BECH32_CHECKSUM, Network.MAINNET))
        assertFalse(keyStore.isValidAddress(AddressDecodeVectors.V0_WITH_BECH32M_CHECKSUM, Network.MAINNET))
        assertFalse(keyStore.isValidAddress(AddressDecodeVectors.UNKNOWN_HRP, Network.MAINNET))
        assertFalse(keyStore.isValidAddress(AddressDecodeVectors.V0_BAD_PROGRAM_LENGTH, Network.MAINNET))
        assertFalse(keyStore.isValidAddress("", Network.MAINNET))
        assertFalse(keyStore.isValidAddress("not-an-address", Network.MAINNET))
    }

    @Test
    fun network_mismatched_addresses_are_rejected() {
        assertFalse(keyStore.isValidAddress(AddressDecodeVectors.P2WPKH_MAINNET, Network.SIGNET))
        assertFalse(keyStore.isValidAddress(Bip84Vectors.SIGNET_RECEIVE_0, Network.MAINNET))
    }

    @Test
    fun derived_addresses_validate_on_their_own_network() {
        for (network in listOf(Network.MAINNET, Network.SIGNET)) {
            val address = keyStore.deriveAddress(Bip84Vectors.MNEMONIC, network, AddressChain.EXTERNAL, 0)
            assertTrue(keyStore.isValidAddress(address.value, network), "$network address must round-trip")
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
