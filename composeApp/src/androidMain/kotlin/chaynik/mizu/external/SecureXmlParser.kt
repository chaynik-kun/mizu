package chaynik.mizu.external

import org.w3c.dom.Document
import java.io.ByteArrayInputStream
import javax.xml.XMLConstants
import javax.xml.parsers.DocumentBuilderFactory

object SecureXmlParser {
	fun parse(bytes: ByteArray): Document {
		val factory = DocumentBuilderFactory.newInstance().apply {
			isNamespaceAware = true
			isXIncludeAware = false
			isExpandEntityReferences = false
			setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true)
			setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
			setFeature("http://xml.org/sax/features/external-general-entities", false)
			setFeature("http://xml.org/sax/features/external-parameter-entities", false)
			setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
			setAttribute("http://javax.xml.XMLConstants/property/accessExternalDTD", "")
			setAttribute("http://javax.xml.XMLConstants/property/accessExternalSchema", "")
		}
		return factory.newDocumentBuilder().apply { setEntityResolver { _, _ -> throw org.xml.sax.SAXException("External entities disabled") } }
			.parse(ByteArrayInputStream(bytes))
	}
}
