package com.marauder.mobile.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

private val Context.apiKeyStore: DataStore<Preferences> by preferencesDataStore(name = "api_keys")

class ApiKeyStore(context: Context) {

    private val store = context.applicationContext.apiKeyStore

    val wigleNameSet: Flow<Boolean> = isSet(WIGLE_NAME)
    val wigleKeySet: Flow<Boolean> = isSet(WIGLE_KEY)
    val wdgKeySet: Flow<Boolean> = isSet(WDG_KEY)

    private fun isSet(key: Preferences.Key<String>): Flow<Boolean> =
        store.data.map { !it[key].isNullOrEmpty() }

    suspend fun saveWigle(name: String?, key: String?) {
        store.edit {
            if (!name.isNullOrBlank()) it[WIGLE_NAME] = encrypt(name)
            if (!key.isNullOrBlank()) it[WIGLE_KEY] = encrypt(key)
        }
    }

    suspend fun clearWigle() {
        store.edit { it.remove(WIGLE_NAME); it.remove(WIGLE_KEY) }
    }

    suspend fun saveWdg(key: String) {
        if (key.isBlank()) return
        store.edit { it[WDG_KEY] = encrypt(key) }
    }

    suspend fun clearWdg() {
        store.edit { it.remove(WDG_KEY) }
    }

    suspend fun wigleNamePlain(): String? = read(WIGLE_NAME)
    suspend fun wigleKeyPlain(): String? = read(WIGLE_KEY)
    suspend fun wdgKeyPlain(): String? = read(WDG_KEY)

    private suspend fun read(key: Preferences.Key<String>): String? =
        store.data.first()[key]?.let { decrypt(it) }

    private fun secretKey(): SecretKey {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (ks.getEntry(ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(
                ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build(),
        )
        return generator.generateKey()
    }

    private fun encrypt(plain: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        val cipherText = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(cipher.iv + cipherText, Base64.NO_WRAP)
    }

    private fun decrypt(encoded: String): String {
        val raw = Base64.decode(encoded, Base64.NO_WRAP)
        val iv = raw.copyOfRange(0, GCM_IV_BYTES)
        val cipherText = raw.copyOfRange(GCM_IV_BYTES, raw.size)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(GCM_TAG_BITS, iv))
        return String(cipher.doFinal(cipherText), Charsets.UTF_8)
    }

    private companion object {
        const val ALIAS = "marauder_api_keys"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val GCM_IV_BYTES = 12
        const val GCM_TAG_BITS = 128
        val WIGLE_NAME = stringPreferencesKey("wigle_name")
        val WIGLE_KEY = stringPreferencesKey("wigle_key")
        val WDG_KEY = stringPreferencesKey("wdg_key")
    }
}
