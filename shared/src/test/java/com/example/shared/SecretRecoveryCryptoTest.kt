package com.example.shared

import com.example.shared.security.SecretRecoveryCrypto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SecretRecoveryCryptoTest {

    @Test
    fun `generated secret code matches KW-XXXX-YYYY format`() {
        val code = SecretRecoveryCrypto.generateSecretCode()
        assertTrue(code.matches(Regex("^KW-[2-9A-HJ-NP-Z]{4}-[2-9A-HJ-NP-Z]{4}$")))
    }

    @Test
    fun `aes gcm encryption and decryption round-trip recovers real password`() {
        val uid = "uid_player_9981"
        val secretCode = SecretRecoveryCrypto.generateSecretCode()
        val rawPassword = "MyStrong#Password2026"

        val encrypted = SecretRecoveryCrypto.encryptPassword(rawPassword, secretCode, uid)
        val decrypted = SecretRecoveryCrypto.decryptPassword(encrypted, " ${secretCode.lowercase()} ", uid)

        assertTrue(decrypted.isSuccess)
        assertEquals(rawPassword, decrypted.getOrNull())
    }

    @Test
    fun `wrong secret code or different userId fails decryption safely`() {
        val uid = "uid_player_9981"
        val secretCode = SecretRecoveryCrypto.generateSecretCode()
        val encrypted = SecretRecoveryCrypto.encryptPassword("SecretPass123", secretCode, uid)

        val wrongCodeResult = SecretRecoveryCrypto.decryptPassword(encrypted, "KW-AAAA-BBBB", uid)
        assertTrue(wrongCodeResult.isFailure)

        val wrongUidResult = SecretRecoveryCrypto.decryptPassword(encrypted, secretCode, "other_uid")
        assertTrue(wrongUidResult.isFailure)
    }
}
