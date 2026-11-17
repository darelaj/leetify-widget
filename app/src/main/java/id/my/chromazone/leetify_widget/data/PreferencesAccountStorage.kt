package id.my.chromazone.leetify_widget.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.byteArrayPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import java.security.GeneralSecurityException
import java.security.ProviderException
import kotlinx.coroutines.flow.first

internal val Context.accountDataStore by preferencesDataStore(name = "account")

internal class PreferencesAccountStorage(
    private val store: DataStore<Preferences>,
    private val cipher: ApiKeyCipher = ApiKeyCipher(),
) : AccountStorage {
    override suspend fun load(): AccountCredentials? = protect {
        val values = store.data.first()
        val id = values[STEAM_ID] ?: return@protect null
        if (!id.matches(Regex("[0-9]{17}"))) throw AccountStorageException()
        AccountCredentials(id, values[API_KEY]?.let(cipher::decrypt))
    }

    override suspend fun save(account: AccountCredentials) = protect {
        require(account.steamId.matches(Regex("[0-9]{17}")))
        val encrypted = account.apiKey?.let {
            require(it.isNotBlank() && it.all { char -> char in '!'..'~' })
            cipher.encrypt(it)
        }
        store.edit {
            it.clear()
            it[STEAM_ID] = account.steamId
            if (encrypted != null) it[API_KEY] = encrypted
        }
        Unit
    }

    override suspend fun clear() = protect {
        store.edit { it.clear() }
        cipher.clear()
    }

    private suspend fun <T> protect(block: suspend () -> T): T = try {
        block()
    } catch (_: GeneralSecurityException) {
        throw AccountStorageException()
    } catch (_: ProviderException) {
        throw AccountStorageException()
    } catch (_: IOException) {
        throw AccountStorageException()
    } catch (_: IllegalArgumentException) {
        throw AccountStorageException()
    }

    companion object {
        private val STEAM_ID = stringPreferencesKey("steam_id")
        private val API_KEY = byteArrayPreferencesKey("encrypted_api_key")
    }
}
