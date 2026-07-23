package com.bitcoin.wallet.kmp.wallet

import com.bitcoin.wallet.kmp.domain.WalletError
import com.bitcoin.wallet.kmp.domain.WalletResult

/** Maps anything thrown by the engine to [WalletError.Engine]; wallet-layer entry points never throw. */
internal inline fun <T> runEngine(block: () -> T): WalletResult<T> =
    runCatching { WalletResult.Success(block()) }
        .getOrElse { WalletResult.Failure(WalletError.Engine(it.message ?: it::class.simpleName ?: "engine error")) }
