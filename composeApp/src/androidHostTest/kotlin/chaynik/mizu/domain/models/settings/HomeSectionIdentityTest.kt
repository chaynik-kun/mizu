package chaynik.mizu.domain.models.settings

import kotlin.test.Test
import kotlin.test.assertEquals

class HomeSectionIdentityTest {
	@Test
	fun persistedSectionIdentityIsStableAcrossDisplayLocales() {
		val encoded = HomeSection.encode(HomeSection.entries)
		assertEquals(HomeSection.entries, HomeSection.decode(encoded))
		assertEquals(HomeSection.entries.map { it.name }, HomeSection.decode(encoded).map { it.name })
	}

	@Test
	fun newlyAddedSectionsAreRestoredWithoutLocalizedKeys() {
		val oldOrder = HomeSection.encode(HomeSection.entries.take(3))
		assertEquals(HomeSection.entries, HomeSection.decode(oldOrder))
	}
}
