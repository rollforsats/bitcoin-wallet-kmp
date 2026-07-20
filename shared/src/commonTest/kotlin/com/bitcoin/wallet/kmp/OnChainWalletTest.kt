package com.bitcoin.wallet.kmp

import com.bitcoin.wallet.kmp.domain.AddressChain
import com.bitcoin.wallet.kmp.domain.BitcoinAddress
import com.bitcoin.wallet.kmp.domain.Mnemonic
import com.bitcoin.wallet.kmp.domain.Network
import com.bitcoin.wallet.kmp.domain.WalletError
import com.bitcoin.wallet.kmp.domain.WalletResult
import com.bitcoin.wallet.kmp.port.KeyStore
import com.bitcoin.wallet.kmp.wallet.OnChainWallet
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

/** OnChainWallet logic against a fake KeyStore (no ACINQ, no network). */
class OnChainWalletTest {

    private val fakeMnemonic = Mnemonic(List(12) { "test" })
    private val fakeAddress = BitcoinAddress("tb1qfake000000000000000000000000000000000")

    /** A deterministic, dependency-free KeyStore for testing wallet logic. */
    private class FakeKeyStore(
        private val mnemonic: Mnemonic,
        private val address: BitcoinAddress,
        private val validMnemonics: Set<String> = setOf(mnemonic.phrase),
    ) : KeyStore {
        override fun generateMnemonic() = mnemonic
        override fun isValidMnemonic(mnemonic: Mnemonic) = mnemonic.phrase in validMnemonics
        override fun deriveAddress(mnemonic: Mnemonic, network: Network, chain: AddressChain, index: Int) = address
        override fun isValidAddress(address: String, network: Network) = address == this.address.value
    }

    private fun wallet(keyStore: KeyStore) = OnChainWallet(keyStore, Network.SIGNET)

    @Test
    fun create_returns_mnemonic_and_address() {
        val result = wallet(FakeKeyStore(fakeMnemonic, fakeAddress)).create()
        val success = assertIs<WalletResult.Success<*>>(result)
        val newWallet = success.value as com.bitcoin.wallet.kmp.wallet.NewWallet
        assertEquals(fakeMnemonic, newWallet.mnemonic)
        assertEquals(fakeAddress, newWallet.firstAddress)
    }

    @Test
    fun restore_with_valid_mnemonic_succeeds() {
        val result = wallet(FakeKeyStore(fakeMnemonic, fakeAddress)).restore(fakeMnemonic)
        assertIs<WalletResult.Success<*>>(result)
    }

    @Test
    fun restore_with_invalid_mnemonic_fails_with_invalid_input() {
        val keyStore = FakeKeyStore(fakeMnemonic, fakeAddress, validMnemonics = emptySet())
        val result = wallet(keyStore).restore(fakeMnemonic)
        val failure = assertIs<WalletResult.Failure>(result)
        assertIs<WalletError.InvalidInput>(failure.error)
    }

    @Test
    fun engine_exception_is_mapped_to_engine_error() {
        val throwing = object : KeyStore {
            override fun generateMnemonic() = fakeMnemonic
            override fun isValidMnemonic(mnemonic: Mnemonic) = true
            override fun deriveAddress(
                mnemonic: Mnemonic,
                network: Network,
                chain: AddressChain,
                index: Int,
            ): BitcoinAddress = throw IllegalStateException("boom")
            override fun isValidAddress(address: String, network: Network) = true
        }
        val result = wallet(throwing).create()
        val failure = assertIs<WalletResult.Failure>(result)
        val error = assertIs<WalletError.Engine>(failure.error)
        assertTrue(error.reason.contains("boom"))
    }

    @Test
    fun isValidAddress_delegates_to_keystore_with_wallet_network() {
        val wallet = wallet(FakeKeyStore(fakeMnemonic, fakeAddress))
        assertTrue(wallet.isValidAddress(fakeAddress.value))
        assertFalse(wallet.isValidAddress("something-else"))
    }

    @Test
    fun restore_maps_throwing_validation_to_engine_error() {
        val throwing = object : KeyStore {
            override fun generateMnemonic() = fakeMnemonic
            override fun isValidMnemonic(mnemonic: Mnemonic): Boolean =
                throw IllegalStateException("validate boom")
            override fun deriveAddress(mnemonic: Mnemonic, network: Network, chain: AddressChain, index: Int) = fakeAddress
            override fun isValidAddress(address: String, network: Network) = true
        }
        val result = wallet(throwing).restore(fakeMnemonic)
        val failure = assertIs<WalletResult.Failure>(result)
        val error = assertIs<WalletError.Engine>(failure.error)
        assertTrue(error.reason.contains("validate boom"))
    }
}
