package chaynik.mizu.external

import org.w3c.dom.Element
import java.net.URL
import chaynik.mizu.domain.models.*

internal data class ParsedRenderer(
	val target: PlaybackTarget.Dlna,
	val avTransport: String,
	val renderingControl: String?,
	val connectionManager: String?
)

internal object UpnpDescriptionParser {
	fun parse(bytes: ByteArray, location: String): ParsedRenderer? {
		val doc = SecureXmlParser.parse(bytes)
		val device = doc.getElementsByTagNameNS("*", "device").item(0) as? Element ?: return null
		if (!device.text("deviceType").orEmpty().contains("MediaRenderer")) return null
		val urlBase = doc.getElementsByTagNameNS("*", "URLBase").item(0)?.textContent?.trim()?.takeIf(String::isNotEmpty) ?: location
		var av: String? = null; var rc: String? = null; var cm: String? = null
		val services = device.getElementsByTagNameNS("*", "service")
		for (i in 0 until services.length) (services.item(i) as? Element)?.let { service ->
			val type = service.text("serviceType").orEmpty(); val control = service.text("controlURL") ?: return@let
			val absolute = URL(URL(urlBase), control).toString()
			when { type.contains("AVTransport") -> av = absolute; type.contains("RenderingControl") -> rc = absolute; type.contains("ConnectionManager") -> cm = absolute }
		}
		return av?.let { ParsedRenderer(PlaybackTarget.Dlna(device.text("UDN") ?: location, device.text("friendlyName") ?: "DLNA renderer", location, device.text("modelName")), it, rc, cm) }
	}
	private fun Element.text(name: String) = getElementsByTagNameNS("*", name).item(0)?.textContent?.trim()
}
