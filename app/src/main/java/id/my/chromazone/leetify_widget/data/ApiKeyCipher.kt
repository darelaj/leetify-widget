package id.my.chromazone.leetify_widget.data

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

internal class ApiKeyCipher(
    private val getKey: () -> SecretKey = ::androidKey,
    private val deleteKey: () -> Unit = {
        KeyStore.getInstance("AndroidKeyStore").apply { load(null) }.deleteEntry(KEY_ALIAS)
    },
) {
    fun encrypt(value: String): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, getKey())
        check(cipher.iv.size == 12)
        return cipher.iv + cipher.doFinal(value.toByteArray(Charsets.UTF_8))
    }

    fun decrypt(value: ByteArray): String {
        require(value.size >= 28)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, getKey(), GCMParameterSpec(128, value.copyOfRange(0, 12)))
        return String(cipher.doFinal(value.copyOfRange(12, value.size)), Charsets.UTF_8)
    }

    fun clear() = deleteKey()
}

private const val KEY_ALIAS = "stat_peek_api_key"

private fun androidKey(): SecretKey {
    val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
    return (store.getKey(KEY_ALIAS, null) as? SecretKey) ?: KeyGenerator.getInstance(
        KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore",
    ).apply {
        init(KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build())
    }.generateKey()
}
