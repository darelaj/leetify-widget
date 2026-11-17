package id.my.chromazone.leetify_widget.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.byteArrayPreferencesKey
import androidx.datastore.preferences.core.edit
import java.security.GeneralSecurityException
import javax.crypto.KeyGenerator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class AccountStorageTest {
    @get:Rule val temporary = TemporaryFolder()

    @Test fun encryptsWithFreshIvAndRejectsTamperedOrLostKeys() {
        val key = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
        val cipher = ApiKeyCipher({ key }, {})
        val first = cipher.encrypt("synthetic-secret")
        assertFalse(first.contentEquals(cipher.encrypt("synthetic-secret")))
        assertEquals("synthetic-secret", cipher.decrypt(first))
        first[first.lastIndex] = (first.last().toInt() xor 1).toByte()
        assertThrows(GeneralSecurityException::class.java) { cipher.decrypt(first) }
        assertThrows(IllegalArgumentException::class.java) { cipher.decrypt(byteArrayOf(1)) }
        val other = ApiKeyCipher({ KeyGenerator.getInstance("AES").generateKey() }, {})
        assertThrows(GeneralSecurityException::class.java) { other.decrypt(cipher.encrypt("secret")) }
    }

    @Test fun storesOnlyConfigurationAndEncryptedKeyAndCanRemoveOrResetIt() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try {
            val file = temporary.newFolder().resolve("account.preferences_pb")
            val store = PreferenceDataStoreFactory.create(scope = scope, produceFile = { file })
            val key = KeyGenerator.getInstance("AES").generateKey()
            var cleared = false
            val storage = PreferencesAccountStorage(store, ApiKeyCipher({ key }, { cleared = true }))
            assertNull(storage.load())
            storage.save(AccountCredentials("76561190000000000", "synthetic-secret"))
            assertEquals("synthetic-secret", storage.load()!!.apiKey)
            assertFalse(file.readText(Charsets.ISO_8859_1).contains("synthetic-secret"))
            assertEquals(3, store.data.first().asMap().size)
            val firstRevision = storage.load()!!.revision
            assertTrue(firstRevision.isNotBlank())
            storage.save(AccountCredentials("76561190000000001", "replacement"))
            assertEquals("76561190000000001", storage.load()!!.steamId)
            assertEquals("replacement", storage.load()!!.apiKey)
            assertNotEquals(firstRevision, storage.load()!!.revision)
            storage.save(AccountCredentials("76561190000000001", null))
            assertNull(storage.load()!!.apiKey)
            store.edit { it[byteArrayPreferencesKey("encrypted_api_key")] = byteArrayOf(1) }
            try {
                storage.load()
                fail("Corrupted credentials must not silently become keyless access")
            } catch (error: AccountStorageException) {
                assertNull(error.cause)
            }
            storage.clear()
            assertNull(storage.load())
            assertTrue(cleared)
        } finally {
            scope.cancel()
        }
    }
}
