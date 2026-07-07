package com.bitcoin.wallet.kmp.onchain.engine.acinq

import com.bitcoin.wallet.kmp.domain.BitcoinAddress
import com.bitcoin.wallet.kmp.domain.Mnemonic
import com.bitcoin.wallet.kmp.domain.Network
import com.bitcoin.wallet.kmp.port.KeyStore
import com.bitcoin.wallet.kmp.port.secureRandomBytes
import fr.acinq.bitcoin.Bitcoin
import fr.acinq.bitcoin.Block
import fr.acinq.bitcoin.BlockHash
import fr.acinq.bitcoin.DeterministicWallet
import fr.acinq.bitcoin.MnemonicCode

/**
 * [KeyStore] backed by ACINQ bitcoin-kmp. Confines all `fr.acinq.*` usage.
 *
 * `internal`: consumers wire the wallet through [com.bitcoin.wallet.kmp.WalletFactory]
 * and depend only on the [KeyStore] port — the concrete engine adapter is never
 * visible outside this module.
 */
internal class AcinqKeyStore(
    /** Entropy size in bytes: 16 → 12 words, 32 → 24 words. */
    private val entropyBytes: Int = 16,
) : KeyStore {

    init {
        // BIP39 permits entropy of 128/160/192/224/256 bits = 16/20/24/28/32 bytes.
        // Reject up front: an invalid size otherwise fails deep inside ACINQ's
        // MnemonicCode with a cryptic message.
        require(entropyBytes in VALID_ENTROPY_BYTES) {
            "entropyBytes must be one of $VALID_ENTROPY_BYTES (BIP39), was $entropyBytes"
        }
    }

    override fun generateMnemonic(): Mnemonic {
        val entropy = secureRandomBytes(entropyBytes)
        val words = MnemonicCode.toMnemonics(entropy)
        return Mnemonic(words)
    }

    override fun isValidMnemonic(mnemonic: Mnemonic): Boolean =
        runCatching { MnemonicCode.validate(mnemonic.words) }.isSuccess

    override fun firstReceiveAddress(mnemonic: Mnemonic, network: Network): BitcoinAddress {
        // BIP39 mnemonic -> seed -> BIP32 master key.
        val seed = MnemonicCode.toSeed(mnemonic.words, passphrase = "")
        val master = DeterministicWallet.generate(seed)

        // BIP84 external receive path: m/84'/coin'/0'/0/0.
        // coin' = 0' for mainnet, 1' for test/signet/regtest (BIP44 registered coin type).
        val coinType = if (network == Network.MAINNET) 0 else 1
        val path = "m/84'/$coinType'/0'/0/0"
        val derived = master.derivePrivateKey(path)

        // P2WPKH (native SegWit) address for the network's chain hash.
        val address = Bitcoin.computeP2WpkhAddress(derived.publicKey, network.chainHash())
        return BitcoinAddress(address)
    }

    private companion object {
        /** BIP39 entropy sizes in bytes (128/160/192/224/256 bits). */
        val VALID_ENTROPY_BYTES = setOf(16, 20, 24, 28, 32)
    }
}

/** Maps the engine-agnostic [Network] to ACINQ's genesis-block chain hash. */
private fun Network.chainHash(): BlockHash = when (this) {
    Network.MAINNET -> Block.LivenetGenesisBlock.hash
    Network.TESTNET -> Block.Testnet3GenesisBlock.hash
    Network.SIGNET -> Block.SignetGenesisBlock.hash
    Network.REGTEST -> Block.RegtestGenesisBlock.hash
}
