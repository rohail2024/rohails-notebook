
package com.penandpaper.app.crypto
import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.util.Base64

private val Context.vault by preferencesDataStore("vault")

object PasswordVault {
    private val SALT = stringPreferencesKey("salt")
    private val WRAPPED_DATA_KEY = stringPreferencesKey("wrapped_data_key")
    private val TEST_CIPHER = stringPreferencesKey("test_cipher")

    fun isInitialized(ctx: Context): Boolean = runBlocking {
        val prefs = ctx.vault.data.first()
        prefs[SALT] != null && prefs[WRAPPED_DATA_KEY] != null && prefs[TEST_CIPHER] != null
    }

    fun initialize(ctx: Context, password: CharArray) = runBlocking {
        val salt = CryptoManager.randomBytes(16)
        val pwdKey = CryptoManager.deriveKeyFromPassword(password, salt)
        val dataKey = CryptoManager.randomBytes(32)
        val wrapped = CryptoManager.wrapWithMaster(dataKey)
        val test = CryptoManager.encryptAesGcm("ok".encodeToByteArray(), pwdKey)
        ctx.vault.edit {
            it[SALT] = Base64.getEncoder().encodeToString(salt)
            it[WRAPPED_DATA_KEY] = Base64.getEncoder().encodeToString(wrapped)
            it[TEST_CIPHER] = Base64.getEncoder().encodeToString(test)
        }
    }

    fun unlock(ctx: Context, password: CharArray): ByteArray? = runBlocking {
        val prefs = ctx.vault.data.first()
        val salt = Base64.getDecoder().decode(prefs[SALT] ?: return@runBlocking null)
        val pwdKey = CryptoManager.deriveKeyFromPassword(password, salt)
        val test = Base64.getDecoder().decode(prefs[TEST_CIPHER] ?: return@runBlocking null)
        return@runBlocking try {
            val ok = CryptoManager.decryptAesGcm(test, pwdKey).decodeToString() == "ok"
            if (!ok) null else CryptoManager.unwrapWithMaster(Base64.getDecoder().decode(prefs[WRAPPED_DATA_KEY]))
        } catch (t: Throwable) { null }
    }
}
