package com.bitcoin.wallet.kmp

import com.bitcoin.wallet.kmp.domain.AddressIndices
import com.bitcoin.wallet.kmp.domain.BitcoinAddress
import com.bitcoin.wallet.kmp.domain.Mnemonic
import com.bitcoin.wallet.kmp.domain.WalletError
import com.bitcoin.wallet.kmp.domain.WalletResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/** Domain type invariants and combinators. */
class DomainModelsTest {

    @Test
    fun map_transforms_success() {
        val result: WalletResult<Int> = WalletResult.Success(21)
        assertEquals(WalletResult.Success(42), result.map { it * 2 })
    }

    @Test
    fun map_passes_failure_through_untouched() {
        val failure: WalletResult<Int> = WalletResult.Failure(WalletError.InvalidInput("bad"))
        assertEquals(failure, failure.map { it * 2 })
    }

    @Test
    fun empty_mnemonic_is_rejected() {
        assertFailsWith<IllegalArgumentException> { Mnemonic(emptyList()) }
    }

    @Test
    fun blank_address_is_rejected() {
        assertFailsWith<IllegalArgumentException> { BitcoinAddress("") }
        assertFailsWith<IllegalArgumentException> { BitcoinAddress("   ") }
    }

    @Test
    fun negative_address_indices_are_rejected() {
        assertFailsWith<IllegalArgumentException> { AddressIndices(nextExternal = -1) }
        assertFailsWith<IllegalArgumentException> { AddressIndices(nextInternal = -1) }
    }
}
