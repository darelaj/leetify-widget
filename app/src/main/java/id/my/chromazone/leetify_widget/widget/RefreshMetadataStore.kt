package id.my.chromazone.leetify_widget.widget

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import id.my.chromazone.leetify_widget.data.ProfileApiException

internal val Context.refreshDataStore by preferencesDataStore(name = "widget_refresh")

// Only request timing/attempt metadata; never provider data or credentials.
internal class RefreshMetadataStore(private val store: DataStore<Preferences>) {
    suspend fun claimManual(now: Long): Boolean {
        var accepted = false
        store.edit { values ->
            val deadline = maxOf(values[BLOCKED_UNTIL] ?: 0L,
                values[LAST_ATTEMPT]?.let { after(it, COOLDOWN) } ?: 0L,
                values[LAST_MANUAL]?.let { after(it, COOLDOWN) } ?: 0L)
            if (now >= deadline) {
                values[LAST_MANUAL] = now
                accepted = true
            }
        }
        return accepted
    }

    suspend fun startAttempt(workId: String, revision: String, now: Long): RefreshPermit {
        var permit: RefreshPermit = RefreshPermit.Wait
        store.edit { values ->
            val same = values[WORK_ID] == workId && values[REVISION] == revision
            val attempts = if (same) values[ATTEMPTS] ?: 0 else 0
            if (same && (values[COMPLETED] == true || attempts >= 3)) {
                permit = RefreshPermit.Finished
            } else if (now >= maxOf(values[BLOCKED_UNTIL] ?: 0L,
                    values[LAST_ATTEMPT]?.let { after(it, COOLDOWN) } ?: 0L)) {
                values[WORK_ID] = workId
                values[REVISION] = revision
                values[ATTEMPTS] = attempts + 1
                values[COMPLETED] = false
                values[LAST_ATTEMPT] = now
                permit = AllowedRefresh(attempts + 1)
            }
        }
        return permit
    }

    suspend fun finishAttempt(workId: String, revision: String, attempt: Int, error: ProfileApiException?, now: Long): Boolean {
        var retry = false
        store.edit { values ->
            if (values[WORK_ID] != workId || values[REVISION] != revision || values[ATTEMPTS] != attempt) return@edit
            val delay = error?.retryDelayMillis(attempt, now)
            retry = delay != null
            values[COMPLETED] = !retry
            values[BLOCKED_UNTIL] = maxOf(values[BLOCKED_UNTIL] ?: 0L,
                error?.retryAtMillis ?: 0L, delay?.let { after(now, it) } ?: 0L)
        }
        return retry
    }

    suspend fun resetAccount() {
        store.edit { values ->
            val blockedUntil = values[BLOCKED_UNTIL]
            values.clear()
            // Changing accounts must not bypass a known server-requested wait.
            if (blockedUntil != null) values[BLOCKED_UNTIL] = blockedUntil
        }
    }

    private fun after(time: Long, delay: Long): Long =
        if (time > Long.MAX_VALUE - delay) Long.MAX_VALUE else time + delay

    companion object {
        private const val COOLDOWN = 60_000L
        private val WORK_ID = stringPreferencesKey("work_id")
        private val REVISION = stringPreferencesKey("revision")
        private val ATTEMPTS = intPreferencesKey("attempts")
        private val COMPLETED = booleanPreferencesKey("completed")
        private val LAST_ATTEMPT = longPreferencesKey("last_attempt")
        private val LAST_MANUAL = longPreferencesKey("last_manual")
        private val BLOCKED_UNTIL = longPreferencesKey("blocked_until")
    }
}
