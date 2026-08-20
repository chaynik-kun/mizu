package chaynik.mizu.ui.screens.settings

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.unit.dp
import org.koin.compose.koinInject
import chaynik.mizu.domain.manager.EqualizerController
import chaynik.mizu.domain.manager.formatEqualizerFrequency
import chaynik.mizu.domain.models.*
import chaynik.mizu.ui.components.sheets.ModalBottomSheet
import kotlin.math.roundToInt
import mizu.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EqualizerSheet(onDismissRequest: () -> Unit) {
	val controller = koinInject<EqualizerController>()
	val state by controller.state.collectAsState()
	val sheetState = rememberBottomSheetState(
		initialValue = SheetValue.Hidden,
		enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded)
	)
	ModalBottomSheet(
		onDismissRequest = onDismissRequest,
		sheetState = sheetState,
		shape = MaterialTheme.shapes.extraLarge,
		containerColor = MaterialTheme.colorScheme.surface,
		sheetTitle = stringResource(Res.string.title_equalizer)
	) {
		EqualizerSheetContent(state, controller)
	}
}

@Composable
private fun EqualizerSheetContent(state: EqualizerState, controller: EqualizerController) {
	var presetMenu by remember { mutableStateOf(false) }
	BoxWithConstraints(Modifier.fillMaxWidth().fillMaxHeight(.9f)) {
	Column(
		Modifier.fillMaxSize()
			.padding(start = 16.dp, end = 16.dp, bottom = 20.dp),
		verticalArrangement = Arrangement.spacedBy(10.dp)
	) {
		Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
			Column {
				Text(stringResource(Res.string.title_equalizer), style = MaterialTheme.typography.titleLarge)
				Text(stringResource(if (state.enabled) Res.string.option_equalizer_enabled else Res.string.option_equalizer_disabled), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
			}
			Switch(state.enabled, controller::setEnabled)
		}

		val message = when {
			state.unavailableReason == EqualizerUnavailableReason.REMOTE_PLAYBACK -> stringResource(Res.string.info_equalizer_remote_playback)
			!state.supported || state.unavailableReason == EqualizerUnavailableReason.INITIALIZATION_FAILED -> stringResource(Res.string.info_equalizer_unavailable)
			state.unavailableReason == EqualizerUnavailableReason.NO_AUDIO_SESSION -> stringResource(Res.string.info_equalizer_waiting)
			else -> null
		}
		message?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }

		Box {
			OutlinedButton(onClick = { presetMenu = true }, enabled = state.presets.isNotEmpty() && state.available) {
				Text(state.selectedPreset?.name ?: stringResource(Res.string.option_equalizer_custom))
			}
			DropdownMenu(presetMenu, { presetMenu = false }) {
				DropdownMenuItem({ Text(stringResource(Res.string.option_equalizer_custom)) }, onClick = { presetMenu = false; controller.useCustom() })
				HorizontalDivider()
				state.presets.forEach { preset -> DropdownMenuItem({ Text(preset.name) }, onClick = { presetMenu = false; controller.applyPreset(preset) }) }
			}
		}

		if (state.bands.isEmpty()) {
			Surface(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainerLow) {
				Text(stringResource(Res.string.info_equalizer_no_session), modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
			}
		} else BoxWithConstraints(Modifier.fillMaxWidth().weight(1f)) {
			val bandCount = state.bands.size.coerceAtLeast(1)
			val bandWidth = (maxWidth / bandCount).coerceIn(68.dp, 92.dp)
			val contentWidth = bandWidth * bandCount
			Row(
				Modifier.width(contentWidth.coerceAtLeast(maxWidth)).fillMaxHeight().horizontalScroll(rememberScrollState()),
				horizontalArrangement = Arrangement.Center
			) {
				state.bands.forEach { band -> EqualizerBandSlider(band, bandWidth, state.available && state.enabled, controller) }
			}
		}

		OutlinedButton(
			onClick = controller::reset,
			enabled = state.bands.isNotEmpty() && state.available,
			modifier = Modifier.fillMaxWidth()
		) { Text(stringResource(Res.string.action_equalizer_reset_flat)) }
	}
	}
}

@Composable
private fun EqualizerBandSlider(band: EqualizerBand, width: androidx.compose.ui.unit.Dp, enabled: Boolean, controller: EqualizerController) {
	var preview by remember(band.id, band.gainDb) { mutableFloatStateOf(band.gainDb) }
	Column(Modifier.width(width).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
		Text("${if (preview > 0) "+" else ""}${(preview * 10).roundToInt() / 10f} dB", style = MaterialTheme.typography.labelSmall)
		VerticalEqualizerSlider(
				value = preview,
				onValueChange = { preview = it; controller.setBandGain(band.id, it, persist = false) },
				onValueChangeFinished = { controller.setBandGain(band.id, preview, persist = true) },
				valueRange = band.minGainDb..band.maxGainDb,
				enabled = enabled,
				modifier = Modifier.weight(1f).width(64.dp)
			)
		Text(formatEqualizerFrequency(band.centerFrequencyHz), style = MaterialTheme.typography.labelMedium)
	}
}

@Composable
internal fun VerticalEqualizerSlider(
	value: Float,
	onValueChange: (Float) -> Unit,
	onValueChangeFinished: () -> Unit,
	valueRange: ClosedFloatingPointRange<Float>,
	enabled: Boolean,
	modifier: Modifier = Modifier
) {
	val primary = MaterialTheme.colorScheme.primary
	val inactive = MaterialTheme.colorScheme.secondaryContainer
	val thumbRadius = 14.dp
	val trackWidth = 8.dp
	val coercedValue = value.coerceIn(valueRange.start, valueRange.endInclusive)
	val rangeLength = (valueRange.endInclusive - valueRange.start).takeIf { it > 0f } ?: 1f
	val fraction = (coercedValue - valueRange.start) / rangeLength

	fun valueForY(y: Float, height: Float, radius: Float): Float {
		val travel = (height - radius * 2f).coerceAtLeast(1f)
		val positionFraction = 1f - ((y - radius) / travel).coerceIn(0f, 1f)
		return valueRange.start + positionFraction * rangeLength
	}

	Canvas(
		modifier
			.semantics {
				progressBarRangeInfo = ProgressBarRangeInfo(coercedValue, valueRange)
				if (!enabled) disabled()
				setProgress { requested ->
					if (!enabled) false else {
						onValueChange(requested.coerceIn(valueRange.start, valueRange.endInclusive))
						onValueChangeFinished()
						true
					}
				}
			}
			.pointerInput(enabled, valueRange) {
				if (!enabled) return@pointerInput
				detectDragGestures(
					onDragStart = { offset ->
						onValueChange(valueForY(offset.y, size.height.toFloat(), thumbRadius.toPx()))
					},
					onDragEnd = onValueChangeFinished,
					onDragCancel = onValueChangeFinished,
					onDrag = { change, _ ->
						change.consume()
						onValueChange(valueForY(change.position.y, size.height.toFloat(), thumbRadius.toPx()))
					}
				)
			}
	) {
		val radius = thumbRadius.toPx()
		val travel = (size.height - radius * 2f).coerceAtLeast(1f)
		val thumbY = radius + (1f - fraction) * travel
		val left = (size.width - trackWidth.toPx()) / 2f
		drawRoundRect(
			color = inactive,
			topLeft = Offset(left, radius),
			size = Size(trackWidth.toPx(), travel),
			cornerRadius = androidx.compose.ui.geometry.CornerRadius(trackWidth.toPx())
		)
		drawRoundRect(
			color = primary,
			topLeft = Offset(left, thumbY),
			size = Size(trackWidth.toPx(), (size.height - radius - thumbY).coerceAtLeast(0f)),
			cornerRadius = androidx.compose.ui.geometry.CornerRadius(trackWidth.toPx())
		)
		drawCircle(color = primary, radius = radius, center = Offset(size.width / 2f, thumbY))
	}
}
