package com.bitcoin.wallet.kmp

import com.bitcoin.wallet.kmp.domain.BitcoinAddress
import com.bitcoin.wallet.kmp.domain.Mnemonic
import com.bitcoin.wallet.kmp.domain.WalletError
import com.bitcoin.wallet.kmp.domain.WalletResult
import com.bitcoin.wallet.kmp.wallet.NewWallet
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The BIP39 mnemonic is the wallet master secret. toString() must never expose
 * it, because implicit calls (string templates, crash stack traces, logged
 * wrapper objects) would leak the seed to logs / crash reporters — irreversible
 * fund loss. These tests pin that property at the source and transitively
 * through every wrapper that embeds the mnemonic.
 *
 * Sentinel words are real BIP39 wordlist entries so the values are realistic;
 * the test only cares that they don't appear in any toString() output.
 */
class MnemonicRedactionTest {

    // A distinctive phrase: if any of these words leaks into a string, we catch it.
    private val secretWords = listOf(
        "zoo", "wing", "vault", "uncle", "trophy", "spice",
        "ribbon", "puzzle", "oyster", "narrow", "legend", "kidney",
    )
    private val mnemonic = Mnemonic(secretWords)

    private fun assertNoSecretLeak(rendered: String) {
        for (word in secretWords) {
            assertFalse(
                rendered.contains(word),
                "secret word \"$word\" leaked into toString(): $rendered",
            )
        }
    }

    @Test
    fun mnemonic_toString_is_redacted() {
        assertEquals("Mnemonic(****)", mnemonic.toString())
        assertNoSecretLeak(mnemonic.toString())
    }

    @Test
    fun mnemonic_redacted_in_string_template() {
        // The realistic leak path: someone interpolates the object into a log line.
        assertNoSecretLeak("restoring wallet from $mnemonic")
    }

    @Test
    fun newwallet_toString_redacts_embedded_mnemonic_but_keeps_address() {
        val address = BitcoinAddress("tb1qexampleaddressvalue00000000000000000000")
        val rendered = NewWallet(mnemonic, address).toString()
        assertNoSecretLeak(rendered)
        // Address is non-secret and useful for debugging — it should still show.
        assertTrue(rendered.contains(address.value), "address should remain visible: $rendered")
    }

    @Test
    fun mnemonic_does_not_leak_through_logged_walletresult() {
        // Success(NewWallet(...)) is the object most likely to be logged wholesale.
        val address = BitcoinAddress("tb1qexampleaddressvalue00000000000000000000")
        val result: WalletResult<NewWallet> =
            WalletResult.Success(NewWallet(mnemonic, address))
        assertNoSecretLeak(result.toString())
    }

    @Test
    fun redaction_does_not_break_access_to_the_real_words() {
        // Redaction is display-only: the secret must still be readable explicitly.
        assertEquals(secretWords, mnemonic.words)
        assertEquals(secretWords.joinToString(" "), mnemonic.phrase)
    }

    @Test
    fun failure_toString_is_unaffected() {
        // Sanity: non-secret wrappers print normally (the reason is not a secret).
        val rendered = WalletResult.Failure(WalletError.InvalidInput("bad")).toString()
        assertTrue(rendered.contains("bad"))
    }
}
