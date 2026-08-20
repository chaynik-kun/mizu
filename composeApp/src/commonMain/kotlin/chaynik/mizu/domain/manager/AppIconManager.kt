package chaynik.mizu.domain.manager

import chaynik.mizu.domain.models.settings.AppIconVariant

expect class AppIconManager {
	fun setVariant(newVariant: AppIconVariant)
	fun getIcon(variant: AppIconVariant): Any?
}
