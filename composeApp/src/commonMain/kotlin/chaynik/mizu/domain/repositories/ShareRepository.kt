package chaynik.mizu.domain.repositories

import chaynik.mizu.data.database.mappers.toDomainModel
import chaynik.mizu.domain.manager.SessionManager

class ShareRepository(
	private val sessionManager: SessionManager
) {
	suspend fun getShares() = sessionManager.api.getShares().map { it.toDomainModel() }
}
