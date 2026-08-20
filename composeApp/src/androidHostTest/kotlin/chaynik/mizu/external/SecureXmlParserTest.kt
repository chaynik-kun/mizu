package chaynik.mizu.external

import kotlin.test.*

class SecureXmlParserTest {
	private val normal = """<?xml version="1.0"?><root xmlns="urn:schemas-upnp-org:device-1-0"><URLBase>http://192.168.1.4:1400/</URLBase><device><deviceType>urn:schemas-upnp-org:device:MediaRenderer:1</deviceType><friendlyName>Renderer</friendlyName><UDN>uuid:one</UDN><serviceList><service><serviceType>urn:schemas-upnp-org:service:AVTransport:1</serviceType><controlURL>/MediaRenderer/AVTransport/Control</controlURL></service></serviceList></device></root>"""

	@Test fun parsesNamespacedDescriptionAndResolvesRelativeControlUrl() {
		val parsed = assertNotNull(UpnpDescriptionParser.parse(normal.encodeToByteArray(), "http://192.168.1.4/device.xml"))
		assertEquals("http://192.168.1.4:1400/MediaRenderer/AVTransport/Control", parsed.avTransport)
	}

	@Test fun prefixVariationParses() {
		val xml = normal.replace("<root xmlns=", "<d:root xmlns:d=").replace("</root>", "</d:root>")
		assertNotNull(UpnpDescriptionParser.parse(xml.encodeToByteArray(), "http://host/device.xml"))
	}

	@Test fun doctypeAndExternalEntityAreRejected() {
		val xxe = """<?xml version="1.0"?><!DOCTYPE root [<!ENTITY xxe SYSTEM "file:///etc/passwd">]><root>&xxe;</root>"""
		assertFails { SecureXmlParser.parse(xxe.encodeToByteArray()) }
	}
}
