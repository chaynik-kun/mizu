package chaynik.mizu.domain.manager

import chaynik.mizu.domain.parser.LogLine

expect class LogManager {
	val logs: List<LogLine>
	fun startStreaming()
	fun stopStreaming()
	fun clearLogs()
}
