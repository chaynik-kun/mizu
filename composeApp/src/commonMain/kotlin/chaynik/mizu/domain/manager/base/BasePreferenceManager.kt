package chaynik.mizu.domain.manager.base

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.russhwolf.settings.Settings
import com.russhwolf.settings.get
import com.russhwolf.settings.set
import kotlin.enums.enumEntries
import kotlin.reflect.KProperty
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

private typealias Getter<T> = (key: String, defaultValue: T) -> T
private typealias Setter<T> = (key: String, newValue: T) -> Unit

/**
 * Base class for managing preferences.
 *
 * @property settings
 */
@Suppress("SameParameterValue", "MemberVisibilityCanBePrivate")
abstract class BasePreferenceManager(
	protected val settings: Settings
) {
	protected fun preference(
		key: String?,
		defaultValue: String
	) = PreferenceProvider(
		key = key,
		defaultValue = defaultValue,
		getter = settings::get,
		setter = settings::set
	)

	protected fun preference(defaultValue: String) = preference(null, defaultValue)

	protected fun preference(
		key: String?,
		defaultValue: Boolean
	) = PreferenceProvider(
		key = key,
		defaultValue = defaultValue,
		getter = settings::get,
		setter = settings::set
	)

	protected fun preference(defaultValue: Boolean) = preference(null, defaultValue)

	protected fun preference(
		key: String?,
		defaultValue: Int
	) = PreferenceProvider(
		key = key,
		defaultValue = defaultValue,
		getter = settings::get,
		setter = settings::set
	)

	protected fun preference(defaultValue: Int) = preference(null, defaultValue)

	protected fun preference(
		key: String?,
		defaultValue: Float
	) = PreferenceProvider(
		key = key,
		defaultValue = defaultValue,
		getter = settings::get,
		setter = settings::set
	)

	protected fun preference(defaultValue: Float) = preference(null, defaultValue)

	protected fun preference(
		key: String?,
		defaultValue: Long
	) = PreferenceProvider(
		key = key,
		defaultValue = defaultValue,
		getter = settings::get,
		setter = settings::set
	)

	protected fun preference(defaultValue: Long) = preference(null, defaultValue)

	protected inline fun <reified E : Enum<E>> preference(
		key: String?,
		defaultValue: E
	) = PreferenceProvider(
		key = key,
		defaultValue = defaultValue,
		getter = settings::getEnum,
		setter = settings::putEnum
	)

	protected inline fun <reified E : Enum<E>> preference(defaultValue: E): PreferenceProvider<E> {
		return preference(null, defaultValue)
	}

	protected class Preferences<T>(
		private val key: String,
		defaultValue: T,
		getter: Getter<T>,
		private val setter: Setter<T>
	) {
		private var value by mutableStateOf(getter(key, defaultValue))

		operator fun getValue(
			thisRef: Any,
			property: KProperty<*>
		) = value

		operator fun setValue(
			thisRef: Any,
			property: KProperty<*>,
			value: T
		) {
			this.value = value
			setter(key, value)
		}
	}

	protected class ObservablePreferences<T>(
		private val key: String,
		defaultValue: T,
		getter: Getter<T>,
		private val setter: Setter<T>
	) {
		private val state = MutableStateFlow(getter(key, defaultValue))
		val flow: StateFlow<T> = state
		operator fun getValue(thisRef: Any, property: KProperty<*>) = state.value
		operator fun setValue(thisRef: Any, property: KProperty<*>, value: T) {
			state.value = value
			setter(key, value)
		}
	}

	protected fun <T> observablePreference(key: String, defaultValue: T, getter: Getter<T>, setter: Setter<T>) =
		ObservablePreferences(key, defaultValue, getter, setter)

	protected fun observablePreference(key: String, defaultValue: String) =
		observablePreference(key, defaultValue, settings::get, settings::set)

	protected fun observablePreference(key: String, defaultValue: Boolean) =
		observablePreference(key, defaultValue, settings::get, settings::set)

	protected fun observablePreference(key: String, defaultValue: Int) =
		observablePreference(key, defaultValue, settings::get, settings::set)

	protected inline fun <reified E : Enum<E>> observablePreference(key: String, defaultValue: E) =
		observablePreference(key, defaultValue, settings::getEnum, settings::putEnum)

	/**
	 * Provides a delegate for a property that is backed by a preference.
	 *
	 * @param T
	 * @property key
	 * @property defaultValue
	 * @property getter
	 * @property setter
	 */
	protected class PreferenceProvider<T>(
		private val key: String?,
		private val defaultValue: T,
		private val getter: Getter<T>,
		private val setter: Setter<T>
	) {
		operator fun provideDelegate(
			thisRef: Any,
			property: KProperty<*>
		) = Preferences(key ?: property.name, defaultValue, getter, setter)
	}

	fun clear() = settings.clear()
}

@PublishedApi
internal inline fun <reified E : Enum<E>> Settings.getEnum(
	key: String,
	defaultValue: E
): E {
	// Fall back to the default when a persisted ordinal no longer maps to an
	// entry (e.g. an enum value was removed or reordered between versions),
	// otherwise the out-of-range index crashes the app on startup.
	return enumEntries<E>().getOrElse(getInt(key, defaultValue.ordinal)) { defaultValue }
}

@PublishedApi
internal inline fun <reified E : Enum<E>> Settings.putEnum(
	key: String,
	value: E
) {
	putInt(key, value.ordinal)
}
