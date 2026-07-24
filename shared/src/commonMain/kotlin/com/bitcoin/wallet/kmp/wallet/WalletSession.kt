package com.bitcoin.wallet.kmp.wallet

import com.bitcoin.wallet.kmp.domain.AddressChain
import com.bitcoin.wallet.kmp.domain.AddressIndices
import com.bitcoin.wallet.kmp.domain.AddressInfo
import com.bitcoin.wallet.kmp.domain.Mnemonic
import com.bitcoin.wallet.kmp.domain.Network
import com.bitcoin.wallet.kmp.domain.WalletResult
import com.bitcoin.wallet.kmp.port.KeyStore
import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.ExperimentalAtomicApi

/**
 * A loaded wallet: the mnemonic plus the next-unused address index per BIP84
 * chain. Indices live in memory only and start at [initialIndices]; a future
 * WalletStore hydrates and persists them.
 */
@OptIn(ExperimentalAtomicApi::class)
class WalletSession internal constructor(
    val mnemonic: Mnemonic,
    private val keyStore: KeyStore,
    private val network: Network,
    initialIndices: AddressIndices = AddressIndices(),
) {
    private val nextExternal = AtomicInt(initialIndices.nextExternal)
    private val nextInternal = AtomicInt(initialIndices.nextInternal)

    /** Snapshot of the next-unused index per chain. */
    val indices: AddressIndices
        get() = AddressIndices(nextExternal.load(), nextInternal.load())

    /** The next unused receive address, without advancing the index. */
    fun peekReceiveAddress(): WalletResult<AddressInfo> =
        runEngine { derive(AddressChain.EXTERNAL, nextExternal.load()) }

    /** The next unused receive address, advancing the index. */
    fun revealReceiveAddress(): WalletResult<AddressInfo> =
        runEngine { claimNext(AddressChain.EXTERNAL, nextExternal) }

    /** The next unused change address, advancing the index. Consumed by tx building. */
    internal fun revealChangeAddress(): WalletResult<AddressInfo> =
        runEngine { claimNext(AddressChain.INTERNAL, nextInternal) }

    // Derive first, then claim the index: a throwing derivation leaves the
    // counter unchanged, and the CAS retries if another thread claimed the index.
    private fun claimNext(chain: AddressChain, next: AtomicInt): AddressInfo {
        while (true) {
            val index = next.load()
            val info = derive(chain, index)
            if (next.compareAndSet(index, index + 1)) return info
        }
    }

    private fun derive(chain: AddressChain, index: Int): AddressInfo =
        AddressInfo(keyStore.deriveAddress(mnemonic, network, chain, index), chain, index)

    // Embeds the secret mnemonic; must stay redacted.
    override fun toString(): String = "WalletSession(mnemonic=****, network=$network, indices=$indices)"
}
