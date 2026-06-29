package com.bitcoin.wallet.kmp.port

actual fun secureRandomBytes(size: Int): ByteArray =
    ByteArray(size).also { java.security.SecureRandom().nextBytes(it) }
