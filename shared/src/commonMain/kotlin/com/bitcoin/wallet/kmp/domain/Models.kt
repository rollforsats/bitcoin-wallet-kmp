package com.bitcoin.wallet.kmp.domain

enum class Network {
    MAINNET,
    TESTNET,
    SIGNET,
    REGTEST,
}

/** BIP84 chain: EXTERNAL = receive (`…/0/i`), INTERNAL = change (`…/1/i`). */
enum class AddressChain {
    EXTERNAL,
    INTERNAL,
}

/** BIP39 mnemonic as an ordered word list; [phrase] is the space-joined form. */
// Keeps the generated `copy()` as private as the primary constructor, so `copy()`
// can't be used to bypass the defensive copy in the public secondary constructor.
@ConsistentCopyVisibility
data class Mnemonic private constructor(val words: List<String>) {
    /**
     * Defensively copies [words] so a caller's mutable backing list can't mutate
     * this value object after construction — [phrase], equality, and hashCode stay
     * stable. The primary constructor is private and takes the already-copied list.
     */
    constructor(words: Iterable<String>) : this(words.toList())

    val phrase: String get() = words.joinToString(" ")

    init {
        require(words.isNotEmpty()) { "mnemonic must not be empty" }
    }

    /**
     * Redacted on purpose: the word list is the wallet master secret, and the
     * compiler-generated `toString()` would print every word. Implicit calls
     * (string templates, crash stack traces, logged wrapper objects) would then
     * leak the seed to logs / crash reporters — irreversible fund loss. Read the
     * secret only via [words] / [phrase] at the explicit point you need it.
     */
    override fun toString(): String = "Mnemonic(****)"
}

/** A validated Bitcoin address string (e.g. bech32 `tb1q…`). */
data class BitcoinAddress(val value: String) {
    init {
        require(value.isNotBlank()) { "address must not be blank" }
    }
}

sealed class WalletError {
    /** Failure in the crypto/engine layer (derivation, encoding, …). */
    data class Engine(val reason: String) : WalletError()

    /** Caller-supplied input was invalid (e.g. a malformed mnemonic). */
    data class InvalidInput(val reason: String) : WalletError()
}

sealed class WalletResult<out T> {
    data class Success<T>(val value: T) : WalletResult<T>()
    data class Failure(val error: WalletError) : WalletResult<Nothing>()

    inline fun <R> map(transform: (T) -> R): WalletResult<R> = when (this) {
        is Success -> Success(transform(value))
        is Failure -> this
    }
}
