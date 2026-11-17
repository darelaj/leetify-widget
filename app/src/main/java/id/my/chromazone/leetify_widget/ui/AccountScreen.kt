package id.my.chromazone.leetify_widget.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import id.my.chromazone.leetify_widget.R

@Composable
internal fun AccountScreen(
    state: AccountState,
    onSteamId: (String) -> Unit,
    onApiKey: (String) -> Unit,
    onVerify: () -> Unit,
    onReset: () -> Unit,
    onRemoveKey: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(stringResource(R.string.account_title), style = MaterialTheme.typography.headlineMedium)
        Text(stringResource(R.string.account_intro))
        OutlinedTextField(
            value = state.steamId,
            onValueChange = onSteamId,
            enabled = !state.loading,
            label = { Text(stringResource(R.string.steam_id)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            isError = state.message == AccountMessage.INVALID_ID,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = state.apiKey,
            onValueChange = onApiKey,
            enabled = !state.loading,
            label = { Text(stringResource(R.string.api_key)) },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                autoCorrectEnabled = false,
            ),
            isError = state.message == AccountMessage.INVALID_KEY_FORMAT,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(stringResource(R.string.key_help), style = MaterialTheme.typography.bodySmall)
        if (state.configured) {
            Text(stringResource(R.string.account_saved))
        }
        if (state.hasSavedKey) {
            val keyHelp = if (state.removeSavedKey) R.string.key_removal_pending else R.string.saved_key_help
            Text(stringResource(keyHelp))
            TextButton(onClick = onRemoveKey, enabled = !state.loading) {
                Text(stringResource(R.string.remove_saved_key))
            }
        }
        Button(onClick = onVerify, enabled = !state.busy && !state.loading) {
            Text(stringResource(R.string.verify_profile))
        }
        if (state.busy || state.loading) {
            CircularProgressIndicator()
        }
        state.message?.let { message ->
            Text(
                text = stringResource(message.textResource()),
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
        TextButton(onClick = onReset, enabled = !state.loading) {
            Text(stringResource(R.string.reset_account))
        }
    }
}
