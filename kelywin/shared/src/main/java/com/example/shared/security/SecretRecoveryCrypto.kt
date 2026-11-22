package com.example.shared.security

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Locale
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Cryptographic helpers for Kelywin's Zero-OTP Secret Recovery Code flow.
 * - Generates a fixed unique Secret Recovery Code (`KW-XXXX-YYYY`) per account.
 * - Hashes passwords and secret codes via SHA-256.
 * - Encrypts/decrypts the user's real password with AES-256-GCM using a key derived from
 *   the user's normalized secretCode + userId, so only the user possessing their Secret Code
 *   can recover their real login password on Forgot Password.
 */
object SecretRecoveryCrypto {
    private const val ALPHABET = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ"
    private const val GCM_IV_LEN = 12
    private const val GCM_TAG_BITS = 128
    private val secureRandom = SecureRandom()

    fun generateSecretCode(): String {
        fun block(len: Int): String = buildString(len) {
            repeat(len) {
                append(ALPHABET[secureRandom.nextInt(ALPHABET.length)])
            }
        }
        return "KW-${block(4)}-${block(4)}"
    }

    fun normalizeSecretCode(code: String): String =
        code.trim().uppercase(Locale.US).replace("\\s+".toRegex(), "")

    fun hashPassword(rawPassword: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(("kelywin_salt_$rawPassword").toByteArray(Charsets.UTF_8))
        return digest.toHex()
    }

    fun hashSecretCode(secretCode: String): String {
        val normalized = normalizeSecretCode(secretCode)
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(("kelywin_secret_salt_$normalized").toByteArray(Charsets.UTF_8))
        return digest.toHex()
    }

    fun encryptPassword(plainPassword: String, secretCode: String, userId: String): String {
        val keySpec = deriveKey(secretCode, userId)
        val iv = ByteArray(GCM_IV_LEN).also { secureRandom.nextBytes(it) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, GCMParameterSpec(GCM_TAG_BITS, iv))
        val cipherText = cipher.doFinal(plainPassword.toByteArray(Charsets.UTF_8))
        return "${iv.toHex()}:${cipherText.toHex()}"
    }

    fun decryptPassword(encryptedBlob: String, secretCode: String, userId: String): Result<String> {
        val parts = encryptedBlob.split(":")
        if (parts.size != 2) {
            return Result.failure(IllegalArgumentException("Encrypted password record is not available."))
        }
        return runCatching {
            val iv = parts[0].fromHex()
            val cipherText = parts[1].fromHex()
            val keySpec = deriveKey(secretCode, userId)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, keySpec, GCMParameterSpec(GCM_TAG_BITS, iv))
            String(cipher.doFinal(cipherText), Charsets.UTF_8)
        }.recoverCatching { err ->
            throw IllegalArgumentException("Secret Recovery Code did not match this account.", err)
        }
    }

    private fun deriveKey(secretCode: String, userId: String): SecretKeySpec {
        val normalized = normalizeSecretCode(secretCode)
        val keyMaterial = MessageDigest.getInstance("SHA-256")
            .digest(("kw_rec_${userId}_$normalized").toByteArray(Charsets.UTF_8))
        return SecretKeySpec(keyMaterial, "AES")
    }

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }

    private fun String.fromHex(): ByteArray {
        require(length % 2 == 0) { "Invalid hex length" }
        return ByteArray(length / 2) { i ->
            substring(i * 2, i * 2 + 2).toInt(16).toByte()
        }
    }
}
