package com.bitcoin.wallet.kmp

import com.bitcoin.wallet.kmp.domain.Mnemonic
import com.bitcoin.wallet.kmp.domain.Network
import com.bitcoin.wallet.kmp.domain.WalletError
import com.bitcoin.wallet.kmp.domain.WalletResult
import com.bitcoin.wallet.kmp.port.KeyStore
import com.bitcoin.wallet.kmp.wallet.NewWallet
import com.bitcoin.wallet.kmp.wallet.OnChainWallet
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

/** OnChainWallet logic against [FakeKeyStore] (no ACINQ, no network). */
class OnChainWalletTest {

    private val fakeMnemonic = Mnemonic(List(12) { "test" })

    private fun wallet(keyStore: KeyStore) = OnChainWallet(keyStore, Network.SIGNET)

    @Test
    fun create_returns_mnemonic_and_first_external_address() {
        val result = wallet(FakeKeyStore(fakeMnemonic)).create()
        val success = assertIs<WalletResult.Success<*>>(result)
        val newWallet = success.value as NewWallet
        assertEquals(fakeMnemonic, newWallet.mnemonic)
        assertEquals("fake-external-0", newWallet.firstAddress.value)
    }

    @Test
    fun restore_with_valid_mnemonic_succeeds() {
        val result = wallet(FakeKeyStore(fakeMnemonic)).restore(fakeMnemonic)
        assertIs<WalletResult.Success<*>>(result)
    }

    @Test
    fun restore_with_invalid_mnemonic_fails_with_invalid_input() {
        val keyStore = FakeKeyStore(fakeMnemonic, validMnemonics = emptySet())
        val result = wallet(keyStore).restore(fakeMnemonic)
        val failure = assertIs<WalletResult.Failure>(result)
        assertIs<WalletError.InvalidInput>(failure.error)
    }

    @Test
    fun engine_exception_is_mapped_to_engine_error() {
        val result = wallet(FakeKeyStore(fakeMnemonic, failDerivationAtIndex = 0)).create()
        val failure = assertIs<WalletResult.Failure>(result)
        val error = assertIs<WalletError.Engine>(failure.error)
        assertTrue(error.reason.contains("derivation failed"))
    }

    @Test
    fun isValidAddress_delegates_to_keystore_with_wallet_network() {
        var capturedNetwork: Network? = null
        val capturingKeyStore = object : KeyStore by FakeKeyStore(fakeMnemonic) {
            override fun isValidAddress(address: String, network: Network): Boolean {
                capturedNetwork = network
                return address.startsWith("fake-")
            }
        }
        val wallet = wallet(capturingKeyStore)
        assertTrue(wallet.isValidAddress("fake-external-0"))
        assertFalse(wallet.isValidAddress("something-else"))
        assertEquals(Network.SIGNET, capturedNetwork)
    }

    @Test
    fun restore_maps_throwing_validation_to_engine_error() {
        val throwingKeyStore = object : KeyStore by FakeKeyStore(fakeMnemonic) {
            override fun isValidMnemonic(mnemonic: Mnemonic): Boolean =
                throw IllegalStateException("validate boom")
        }
        val result = wallet(throwingKeyStore).restore(fakeMnemonic)
        val failure = assertIs<WalletResult.Failure>(result)
        val error = assertIs<WalletError.Engine>(failure.error)
        assertTrue(error.reason.contains("validate boom"))
    }
}
