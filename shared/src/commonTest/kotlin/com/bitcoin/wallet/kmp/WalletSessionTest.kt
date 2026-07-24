package com.bitcoin.wallet.kmp

import com.bitcoin.wallet.kmp.domain.AddressChain
import com.bitcoin.wallet.kmp.domain.AddressIndices
import com.bitcoin.wallet.kmp.domain.AddressInfo
import com.bitcoin.wallet.kmp.domain.BitcoinAddress
import com.bitcoin.wallet.kmp.domain.Mnemonic
import com.bitcoin.wallet.kmp.domain.Network
import com.bitcoin.wallet.kmp.domain.WalletError
import com.bitcoin.wallet.kmp.domain.WalletResult
import com.bitcoin.wallet.kmp.port.KeyStore
import com.bitcoin.wallet.kmp.wallet.WalletSession
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals

/** Reveal/peek index tracking against [FakeKeyStore] (no ACINQ, no network). */
class WalletSessionTest {

    private val mnemonic = Mnemonic(List(12) { "test" })

    private fun session(
        keyStore: KeyStore = FakeKeyStore(mnemonic),
        initialIndices: AddressIndices = AddressIndices(),
    ) = WalletSession(mnemonic, keyStore, Network.SIGNET, initialIndices)

    private fun WalletResult<AddressInfo>.get(): AddressInfo {
        val success = assertIs<WalletResult.Success<*>>(this)
        return success.value as AddressInfo
    }

    @Test
    fun peek_does_not_advance_the_index() {
        val session = session()
        val first = session.peekReceiveAddress().get()
        val second = session.peekReceiveAddress().get()
        assertEquals(first, second)
        assertEquals(AddressInfo(BitcoinAddress("fake-external-0"), AddressChain.EXTERNAL, 0), first)
        assertEquals(AddressIndices(0, 0), session.indices)
    }

    @Test
    fun reveal_advances_and_returns_fresh_addresses() {
        val session = session()
        val first = session.revealReceiveAddress().get()
        val second = session.revealReceiveAddress().get()
        assertEquals(0, first.index)
        assertEquals(1, second.index)
        assertNotEquals(first.address, second.address)
        assertEquals(2, session.indices.nextExternal)
    }

    @Test
    fun peek_after_reveal_shows_the_next_unrevealed_address() {
        val session = session()
        session.revealReceiveAddress()
        val peeked = session.peekReceiveAddress().get()
        val revealed = session.revealReceiveAddress().get()
        assertEquals(peeked, revealed)
        assertEquals(1, revealed.index)
    }

    @Test
    fun change_chain_is_tracked_independently() {
        val session = session()
        assertEquals(0, session.revealReceiveAddress().get().index)
        val change = session.revealChangeAddress().get()
        assertEquals(AddressChain.INTERNAL, change.chain)
        assertEquals("fake-internal-0", change.address.value)
        assertEquals(1, session.revealReceiveAddress().get().index)
        assertEquals(AddressIndices(nextExternal = 2, nextInternal = 1), session.indices)
    }

    @Test
    fun failed_derivation_does_not_advance_the_index() {
        val session = session(FakeKeyStore(mnemonic, failDerivationAtIndex = 0))
        val failure = assertIs<WalletResult.Failure>(session.revealReceiveAddress())
        assertIs<WalletError.Engine>(failure.error)
        assertEquals(AddressIndices(0, 0), session.indices)
        // The fake fails once; the retry claims the same index.
        assertEquals(0, session.revealReceiveAddress().get().index)
    }

    @Test
    fun peek_maps_engine_failure() {
        val session = session(FakeKeyStore(mnemonic, failDerivationAtIndex = 0))
        val failure = assertIs<WalletResult.Failure>(session.peekReceiveAddress())
        assertIs<WalletError.Engine>(failure.error)
    }

    @Test
    fun initial_indices_are_honored() {
        val session = session(initialIndices = AddressIndices(nextExternal = 5, nextInternal = 2))
        assertEquals(5, session.revealReceiveAddress().get().index)
        assertEquals(2, session.revealChangeAddress().get().index)
    }

    @Test
    fun sessions_track_indices_independently() {
        val first = session()
        val second = session()
        first.revealReceiveAddress()
        assertEquals(AddressIndices(1, 0), first.indices)
        assertEquals(AddressIndices(0, 0), second.indices)
    }
}
