package com.example.androidproject1

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.androidproject1.service.core.data.crypto.KeystoreAead
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import java.security.KeyStore

/**
 * The session's cipher over the real `AndroidKeyStore`, which Robolectric does not have: the key is
 * made once under its alias, found again by a second instance, and a key under another alias
 * cannot read what it wrote.
 */
@RunWith(AndroidJUnit4::class)
class KeystoreAeadTest {

    private val alias = "test-${System.nanoTime()}"
    private val plaintext = "a session token".toByteArray()

    @Test
    fun aSecondInstanceUnderTheSameAliasReadsWhatTheFirstWrote() {
        val sealed = KeystoreAead(alias).encrypt(plaintext)

        assertFalse(sealed.contentEquals(plaintext))
        assertArrayEquals(plaintext, KeystoreAead(alias).decrypt(sealed))
        assert(KeyStore.getInstance("AndroidKeyStore").apply { load(null) }.containsAlias(alias))
    }

    @Test
    fun anotherAliasCannotReadIt() {
        val sealed = KeystoreAead(alias).encrypt(plaintext)

        assertThrows(Exception::class.java) { KeystoreAead("$alias-other").decrypt(sealed) }
    }
}
