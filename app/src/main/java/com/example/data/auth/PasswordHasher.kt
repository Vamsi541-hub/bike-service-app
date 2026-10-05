package com.example.data.auth

import java.security.MessageDigest

object PasswordHasher {
    private const val PREFIX = "sha256:"

    fun hash(password: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(password.toByteArray(Charsets.UTF_8))
        return PREFIX + bytes.joinToString("") { "%02x".format(it) }
    }

    fun isHashed(value: String): Boolean = value.startsWith(PREFIX)

    fun matches(password: String, storedValue: String): Boolean =
        hash(password) == storedValue
}
