package chaynik.mizu.domain.manager

interface CredentialStore {
	fun getPassword(): String
	fun setPassword(password: String)
	fun clear()
}
