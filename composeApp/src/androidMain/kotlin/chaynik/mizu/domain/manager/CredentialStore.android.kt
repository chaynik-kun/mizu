package chaynik.mizu.domain.manager

import android.app.Application
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class AndroidCredentialStore(private val application: Application) : CredentialStore {
	private val preferences = application.getSharedPreferences(FILE_NAME, 0)

	override fun getPassword(): String {
		val encrypted = preferences.getString(KEY_PASSWORD, null) ?: return ""
		return runCatching {
			val bytes = Base64.decode(encrypted, Base64.NO_WRAP)
			val ivLength = bytes.first().toInt() and 0xff
			val iv = bytes.copyOfRange(1, ivLength + 1)
			val ciphertext = bytes.copyOfRange(ivLength + 1, bytes.size)
			Cipher.getInstance(TRANSFORMATION).run {
				init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(128, iv))
				doFinal(ciphertext).decodeToString()
			}
		}.getOrDefault("")
	}

	override fun setPassword(password: String) {
		val cipher = Cipher.getInstance(TRANSFORMATION).apply {
			init(Cipher.ENCRYPT_MODE, secretKey())
		}
		val output = byteArrayOf(cipher.iv.size.toByte()) + cipher.iv + cipher.doFinal(password.encodeToByteArray())
		preferences.edit().putString(KEY_PASSWORD, Base64.encodeToString(output, Base64.NO_WRAP)).apply()
	}

	override fun clear() {
		preferences.edit().clear().apply()
	}

	private fun secretKey(): SecretKey {
		val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
		(keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
		return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").run {
			init(
				KeyGenParameterSpec.Builder(
					KEY_ALIAS,
					KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
				).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
					.setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
					.build()
			)
			generateKey()
		}
	}

	private companion object {
		const val FILE_NAME = "secure_credentials"
		const val KEY_PASSWORD = "password"
		const val KEY_ALIAS = "mizu_credentials_key"
		const val TRANSFORMATION = "AES/GCM/NoPadding"
	}
}
