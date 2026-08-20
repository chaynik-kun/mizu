package chaynik.mizu.domain.manager

enum class LoginErrorCode {
	UnknownHost, Timeout, Tls, InvalidCredentials, UnsupportedServer, InsecureHttp, Generic
}

class LoginException(
	val code: LoginErrorCode,
	cause: Throwable? = null
) : Exception(code.name, cause)
