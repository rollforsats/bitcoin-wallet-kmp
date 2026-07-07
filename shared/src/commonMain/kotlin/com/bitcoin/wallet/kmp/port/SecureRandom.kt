package com.bitcoin.wallet.kmp.port

/**
 * CSPRNG bytes for BIP39 entropy — not `kotlin.random.Random` (insecure).
 * JVM/Android: `java.security.SecureRandom`; iOS: `SecRandomCopyBytes`.
 */
expect fun secureRandomBytes(size: Int): ByteArray
