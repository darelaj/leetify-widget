package id.my.chromazone.leetify_widget

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import id.my.chromazone.leetify_widget.data.ApiKeyCipher
import id.my.chromazone.leetify_widget.ui.AccountMessage
import id.my.chromazone.leetify_widget.ui.AccountScreen
import id.my.chromazone.leetify_widget.ui.AccountState
import id.my.chromazone.leetify_widget.ui.theme.LeetifywidgetTheme
import java.security.KeyStore
import javax.crypto.KeyGenerator
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class AccountSetupTest {
    @get:Rule val compose = createComposeRule()

    @Test fun keyInputIsMaskedAndFormCanValidateAndReset() {
        compose.setContent {
            var state by remember { mutableStateOf(AccountState(loading = false)) }
            LeetifywidgetTheme {
                AccountScreen(state, { state = state.copy(steamId = it) },
                    { state = state.copy(apiKey = it) },
                    { state = state.copy(message = AccountMessage.INVALID_ID) },
                    { state = AccountState(loading = false) }, {})
            }
        }
        compose.onNodeWithText("SteamID64").performTextInput("123")
        compose.onNodeWithText("API key (optional)").performTextInput("synthetic-key")
        compose.onNodeWithText("API key (optional)")
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Password))
        compose.onNodeWithText("Verify and save").performScrollTo().performClick()
        compose.onNodeWithText("Enter exactly 17 digits for SteamID64.").assertIsDisplayed()
        compose.onNodeWithText("Reset account").performScrollTo().performClick()
        compose.onNodeWithText("Enter exactly 17 digits for SteamID64.").assertDoesNotExist()
    }

    @Test fun cipherWorksWithAndroidKeystoreWithoutUsingOwnersKey() {
        val alias = "stat_peek_instrumentation_only"
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        try {
            val key = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
                init(KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
            }.generateKey()
            val cipher = ApiKeyCipher({ key }, { store.deleteEntry(alias) })
            assertEquals("synthetic-key", cipher.decrypt(cipher.encrypt("synthetic-key")))
            cipher.clear()
        } finally {
            store.deleteEntry(alias)
        }
    }
}
