/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.rio.rdfxml;

import static org.eclipse.rdf4j.rio.rdfxml.util.RDFXMLConstants.ITS_NAMESPACE;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.eclipse.rdf4j.common.net.ParsedIRI;
import org.eclipse.rdf4j.common.xml.XMLUtil;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.rio.ParseLocationListener;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.helpers.XMLParserSettings;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;

/**
 * A filter on SAX events to make life easier on the RDF parser itself. This filter does things like combining a call to
 * startElement() that is directly followed by a call to endElement() to a single call to emptyElement().
 */
class SAXFilter implements ContentHandler {

	/*-----------*
	 * Variables *
	 *-----------*/

	/**
	 * The RDF parser to supply the filtered SAX events to.
	 */
	private final RDFXMLParser rdfParser;

	/**
	 * A Locator indicating a position in the text that is currently being parsed by the SAX parser.
	 */
	private Locator locator;

	/**
	 * Stack of ElementInfo objects.
	 */
	private final Stack<ElementInfo> elInfoStack = new Stack<>();

	/**
	 * StringBuilder used to collect text during parsing.
	 */
	private final StringBuilder charBuf = new StringBuilder(512);

	/**
	 * The document's URI.
	 */
	private ParsedIRI documentURI;

	/**
	 * Flag indicating whether the parser parses stand-alone RDF documents. In stand-alone documents, the rdf:RDF
	 * element is optional if it contains just one element.
	 */
	private boolean parseStandAloneDocuments = true;

	/**
	 * Variable used to defer reporting of start tags. Reporting start tags is deferred to be able to combine a start
	 * tag and an immediately following end tag to a single call to emptyElement().
	 */
	private ElementInfo deferredElement = null;

	/**
	 * New namespace mappings that have been reported for the next start tag by the SAX parser, but that are not yet
	 * assigned to an ElementInfo object.
	 */
	private final Map<String, String> newNamespaceMappings = new LinkedHashMap<>();

	/**
	 * Flag indicating whether we're currently parsing RDF elements.
	 */
	private boolean inRDFContext;

	/**
	 * The number of elements on the stack that are in the RDF context.
	 */
	private int rdfContextStackHeight;

	/**
	 * Flag indicating whether we're currently parsing an XML literal.
	 */
	private boolean parseLiteralMode = false;

	/**
	 * The number of elements on the stack that are part of an XML literal.
	 */
	private int xmlLiteralStackHeight;

	/**
	 * The prefixes that were used in an XML literal, but that were not defined in it (but rather in the XML literal's
	 * context).
	 */
	private final List<String> unknownPrefixesInXMLLiteral = new ArrayList<>();

	/**
	 * Tracks the namespace context for the currently parsed XML literal. This includes prefixes declared within the
	 * literal, prefixes used in element/attribute names, and the inferred default namespace. It is initialized when
	 * entering XML literal mode and cleared when leaving it.
	 */
	private XmlLiteralNamespaceContext xmlLiteralContext;

	/*--------------*
	 * Constructors *
	 *--------------*/

	public SAXFilter(RDFXMLParser rdfParser) {
		this.rdfParser = rdfParser;
	}

	/*---------*
	 * Methods *
	 *---------*/

	public Locator getLocator() {
		return locator;
	}

	public void clear() {
		locator = null;
		elInfoStack.clear();
		charBuf.setLength(0);
		documentURI = null;
		deferredElement = null;

		newNamespaceMappings.clear();

		inRDFContext = false;
		rdfContextStackHeight = 0;

		parseLiteralMode = false;
		xmlLiteralStackHeight = 0;

		if (xmlLiteralContext != null) {
			xmlLiteralContext.initFromStack(null);
		}
	}

	public void setDocumentURI(String documentURI) {
		if (documentURI != null) {
			this.documentURI = createBaseURI(documentURI);
		}
	}

	public void setParseStandAloneDocuments(boolean standAloneDocs) {
		parseStandAloneDocuments = standAloneDocs;
	}

	/*---------------------------------------*
	 * Methods from interface ContentHandler *
	 *---------------------------------------*/

	@Override
	public void setDocumentLocator(Locator loc) {
		locator = loc;

		ParseLocationListener pll = rdfParser.getParseLocationListener();
		if (pll != null && loc != null) {
			pll.parseLocationUpdate(loc.getLineNumber(), loc.getColumnNumber());
		}
	}

	@Override
	public void startDocument() throws SAXException {
		try {
			rdfParser.startDocument();
		} catch (RDFParseException | RDFHandlerException e) {
			throw new SAXException(e);
		}
	}

	@Override
	public void endDocument() throws SAXException {
		try {
			rdfParser.endDocument();
		} catch (RDFParseException | RDFHandlerException e) {
			throw new SAXException(e);
		}
	}

	@Override
	public void startPrefixMapping(String prefix, String uri) throws SAXException {
		try {
			if (deferredElement != null) {
				// This new prefix mapping must come from a new start tag
				reportDeferredStartElement();
			}

			newNamespaceMappings.put(prefix, uri);

			if (parseLiteralMode && xmlLiteralContext != null) {
				xmlLiteralContext.registerDeclaredPrefix(prefix);
			}

			if (rdfParser.getRDFHandler() != null) {
				rdfParser.getRDFHandler().handleNamespace(prefix, uri);
			}
		} catch (RDFParseException | RDFHandlerException e) {
			throw new SAXException(e);
		}
	}

	@Override
	public void endPrefixMapping(String prefix) {
		if (parseLiteralMode && xmlLiteralContext != null) {
			xmlLiteralContext.unregisterDeclaredPrefix(prefix);
		}
	}

	@Override
	public void startElement(String namespaceURI, String localName, String qName, Attributes attributes)
			throws SAXException {
		try {
			if (deferredElement != null) {
				// The next call could set parseLiteralMode to true!
				reportDeferredStartElement();
			}

			if (parseLiteralMode) {
				appendStartTag(qName, attributes, namespaceURI);
				xmlLiteralStackHeight++;
			} else {
				ElementInfo parent = peekStack();
				ElementInfo elInfo = new ElementInfo(parent, qName, namespaceURI, localName);

				elInfo.setNamespaceMappings(newNamespaceMappings);
				newNamespaceMappings.clear();

				if (!inRDFContext && parseStandAloneDocuments
						&& (!localName.equals("RDF") || !namespaceURI.equals(RDF.NAMESPACE))) {
					// Stand-alone document that does not start with an rdf:RDF root
					// element. Assume this root element is omitted.
					inRDFContext = true;
				}

				if (!inRDFContext) {
					// Check for presence of xml:base and xlm:lang attributes.
					for (int i = 0; i < attributes.getLength(); i++) {
						String attQName = attributes.getQName(i);

						if ("xml:base".equals(attQName)) {
							elInfo.setBaseURI(attributes.getValue(i));
						} else if ("xml:lang".equals(attQName)) {
							elInfo.xmlLang = attributes.getValue(i);
						}
					}

					elInfoStack.push(elInfo);

					// Check if we are entering RDF context now.
					if (localName.equals("RDF") && namespaceURI.equals(RDF.NAMESPACE)) {

						// Detect RDF 1.2
						for (int i = 0; i < attributes.getLength(); i++) {
							String attURI = attributes.getURI(i);
							String attLocal = attributes.getLocalName(i);

							if (RDF.NAMESPACE.equals(attURI) && "version".equals(attLocal)) {
								String value = attributes.getValue(i);
								if ("1.2".equals(value)) {
									rdfParser.setRdf12Mode();
								}
							}
						}

						// Detect ITS 1.0/1.2 directional literals
						for (int i = 0; i < attributes.getLength(); i++) {
							String attURI = attributes.getURI(i);
							String attLocal = attributes.getLocalName(i);

							if (ITS_NAMESPACE.equals(attURI) && "dir".equals(attLocal)) {
								String dirValue = attributes.getValue(i);
								if (!rdfParser.getRdf12Mode()) {
									dirValue = null;
								}
								if (dirValue != null) {
									rdfParser.setDefaultBaseDirection(dirValue);
								}
							}
						}

						inRDFContext = true;
						rdfContextStackHeight = 0;
					}
				} else {
					// We're parsing RDF elements.
					checkAndCopyAttributes(attributes, elInfo);

					// Don't report the new element to the RDF parser just yet.
					deferredElement = elInfo;
				}

				charBuf.setLength(0);
			}
		} catch (RDFParseException | RDFHandlerException e) {
			throw new SAXException(e);
		}
	}

	private void reportDeferredStartElement() throws RDFParseException, RDFHandlerException {

		// Only useful for debugging.
		// if (deferredElement == null) {
		// throw new RuntimeException("no deferred start element available");
		// }

		elInfoStack.push(deferredElement);
		rdfContextStackHeight++;

		if (deferredElement.baseURI != null) {
			rdfParser.setBaseURI(deferredElement.baseURI.toString());
		}
		rdfParser.setXMLLang(deferredElement.xmlLang);

		rdfParser.startElement(deferredElement.namespaceURI, deferredElement.localName, deferredElement.qName,
				deferredElement.atts);

		deferredElement = null;
	}

	@Override
	public void endElement(String namespaceURI, String localName, String qName) throws SAXException {
		try {
			// FIXME: in parseLiteralMode we should also check if start- and
			// end-tags match but these start tags are not tracked yet.

			if (rdfParser.getParserConfig().get(XMLParserSettings.FAIL_ON_MISMATCHED_TAGS) && !parseLiteralMode) {
				// Verify that the end tag matches the start tag.
				ElementInfo elInfo;

				if (deferredElement != null) {
					elInfo = deferredElement;
				} else {
					elInfo = peekStack();
				}

				if (!qName.equals(elInfo.qName)) {
					rdfParser.reportError("expected end tag </'" + elInfo.qName + ">, found </" + qName + ">",
							XMLParserSettings.FAIL_ON_MISMATCHED_TAGS);
				}
			}

			if (!inRDFContext) {
				elInfoStack.pop();
				charBuf.setLength(0);
				return;
			}

			if (deferredElement == null && rdfContextStackHeight == 0) {
				// This end tag removes the element that signaled the start
				// of the RDF context (i.e. <rdf:RDF>) from the stack.
				inRDFContext = false;

				elInfoStack.pop();
				charBuf.setLength(0);
				return;
			}

			// We're still in RDF context.

			if (parseLiteralMode && xmlLiteralStackHeight > 0) {
				appendEndTag(qName);
				xmlLiteralStackHeight--;
				return;
			}

			// Check for any deferred start elements
			if (deferredElement != null) {
				// Start element still deferred, this is an empty element
				if (deferredElement.baseURI != null) {
					rdfParser.setBaseURI(deferredElement.baseURI.toString());
				}
				rdfParser.setXMLLang(deferredElement.xmlLang);

				rdfParser.emptyElement(deferredElement.namespaceURI, deferredElement.localName, deferredElement.qName,
						deferredElement.atts);

				deferredElement = null;
			} else {
				if (parseLiteralMode) {
					// Insert any used namespace prefixes from the XML literal's
					// context that are not defined in the XML literal itself.
					insertContextNamespaces();

					rdfParser.text(charBuf.toString());

					parseLiteralMode = false;
				} else {
					String s = charBuf.toString();

					// ignore whitespace-only nodes
					if (!s.trim().isEmpty()) {
						rdfParser.text(s);
					}
				}

				charBuf.setLength(0);

				// Handle the end tag
				elInfoStack.pop();
				rdfContextStackHeight--;

				rdfParser.endElement();
			}
		} catch (RDFParseException | RDFHandlerException e) {
			throw new SAXException(e);
		}
	}

	@Override
	public void characters(char[] ch, int start, int length) throws SAXException {
		try {
			if (inRDFContext) {
				// verify if we need to switch to XMLLiteral processing mode immediately.
				if (deferredElement != null && !parseLiteralMode) {
					Att parseType = deferredElement.atts.getAtt(RDF.NAMESPACE, "parseType");
					if (parseType != null && parseType.getValue().equals("Literal")) {
						setParseLiteralMode();
					}
				}

				if (parseLiteralMode) {
					if (deferredElement != null) {
						reportDeferredStartElement();
					}

					// Characters like '<', '>', and '&' must be escaped to
					// prevent breaking the XML text.
					String s = new String(ch, start, length);
					s = XMLUtil.escapeCharacterData(s);
					charBuf.append(s);
				} else {
					charBuf.append(ch, start, length);

					// if the element is not empty we need to process it as such. Otherwise,
					// we keep the start element deferred for now.
					if (deferredElement != null && !charBuf.toString().trim().isEmpty()) {
						reportDeferredStartElement();
					}
				}
			}
		} catch (RDFParseException | RDFHandlerException e) {
			throw new SAXException(e);
		}
	}

	@Override
	public void ignorableWhitespace(char[] ch, int start, int length) {
		if (parseLiteralMode) {
			charBuf.append(ch, start, length);
		}
	}

	@Override
	public void processingInstruction(String target, String data) {
		// ignore
	}

	@Override
	public void skippedEntity(String name) {
		// ignore
	}

	private void checkAndCopyAttributes(Attributes attributes, ElementInfo elInfo)
			throws RDFParseException {
		Atts atts = new Atts(attributes.getLength());

		int attCount = attributes.getLength();
		for (int i = 0; i < attCount; i++) {
			String qName = attributes.getQName(i);
			String value = attributes.getValue(i);

			// attributes starting with "xml" should be ignored, except for the
			// ones that are handled by this parser (xml:lang and xml:base).
			if (qName.startsWith("xml")) {
				if (qName.equals("xml:lang")) {
					elInfo.xmlLang = value;
				} else if (qName.equals("xml:base")) {
					elInfo.setBaseURI(value);
				}
			} else {
				String namespace = attributes.getURI(i);
				String localName = attributes.getLocalName(i);

				// A limited set of unqualified attributes must be supported by
				// parsers, as is specified in section 6.1.4 of the spec
				if ("".equals(namespace)) {
					if (localName.equals("ID") || localName.equals("about") || localName.equals("resource")
							|| localName.equals("parseType") || localName.equals("type")) {
						rdfParser.reportWarning("use of unqualified attribute " + localName + " has been deprecated");
						namespace = RDF.NAMESPACE;
					}
				}

				if ("".equals(namespace)) {
					rdfParser.reportError("unqualified attribute '" + qName + "' not allowed",
							XMLParserSettings.FAIL_ON_INVALID_QNAME);
				}

				Att att = new Att(namespace, localName, qName, value);
				atts.addAtt(att);
			}
		}

		elInfo.atts = atts;
	}

	public void setParseLiteralMode() {
		parseLiteralMode = true;
		xmlLiteralStackHeight = 0;

		// All currently known namespace prefixes are
		// new for this XML literal.
		unknownPrefixesInXMLLiteral.clear();
		xmlLiteralContext = new XmlLiteralNamespaceContext();
		xmlLiteralContext.initFromStack(peekStack());
	}

	private ParsedIRI createBaseURI(String uriString) {
		return ParsedIRI.create(uriString).normalize();
	}

	/*---------------------------------*
	 * Methods related to XML literals *
	 *---------------------------------*/

	/**
	 * Appends a start tag to charBuf. This method is used during the parsing of an XML Literal.
	 */
	private void appendStartTag(String qName, Attributes attributes, String namespaceURI) {

		charBuf.append("<").append(qName);

		// track default namespace usage
		xmlLiteralContext.registerDefaultNamespaceIfNeeded(namespaceURI);

		// write namespace declarations from the SAX parser for this element
		for (Map.Entry<String, String> entry : newNamespaceMappings.entrySet()) {
			appendNamespaceDecl(charBuf, entry.getKey(), entry.getValue());

			if (parseLiteralMode) {
				xmlLiteralContext.registerDeclaredPrefix(entry.getKey());
			}
		}

		// detect prefix from element name
		xmlLiteralContext.registerUsedQName(qName);

		// attributes
		int attCount = attributes.getLength();
		for (int i = 0; i < attCount; i++) {
			String attQName = attributes.getQName(i);
			String value = attributes.getValue(i);

			appendAttribute(charBuf, attQName, value);

			// detect prefix from attribute name
			xmlLiteralContext.registerUsedQName(attQName);
		}

		charBuf.append(">");
	}

	/**
	 * Appends an end tag to charBuf. This method is used during the parsing of an XML Literal.
	 */
	private void appendEndTag(String qName) {
		charBuf.append("</").append(qName).append(">");
	}

	/**
	 * Inserts prefix mappings from an XML Literal's context for all prefixes that are used in the XML Literal and that
	 * are not defined in the XML Literal itself.
	 */
	private void insertContextNamespaces() {
		if (xmlLiteralContext != null) {
			xmlLiteralContext.injectNamespaces(charBuf);
		}
	}

	private void appendNamespaceDecl(StringBuilder sb, String prefix, String namespace) {
		String attName = "xmlns";

		if (!"".equals(prefix)) {
			attName += ":" + prefix;
		}

		appendAttribute(sb, attName, namespace);
	}

	private void appendAttribute(StringBuilder sb, String name, String value) {
		sb.append(" ");
		sb.append(name);
		sb.append("=\"");
		sb.append(XMLUtil.escapeDoubleQuotedAttValue(value));
		sb.append("\"");
	}

	/*------------------------------------------*
	 * Methods related to the ElementInfo stack *
	 *------------------------------------------*/

	private ElementInfo peekStack() {
		ElementInfo result = null;

		if (!elInfoStack.empty()) {
			result = elInfoStack.peek();
		}

		return result;
	}

	/*----------------------------*
	 * Internal class ElementInfo *
	 *----------------------------*/

	private class ElementInfo {

		public String qName;

		public String namespaceURI;

		public String localName;

		public Atts atts;

		public ElementInfo parent;

		private Map<String, String> namespaceMap;

		public ParsedIRI baseURI;

		public String xmlLang;

		public ElementInfo(String qName, String namespaceURI, String localName) {
			this(null, qName, namespaceURI, localName);
		}

		public ElementInfo(ElementInfo parent, String qName, String namespaceURI, String localName) {
			this.parent = parent;
			this.qName = qName;
			this.namespaceURI = namespaceURI;
			this.localName = localName;

			if (parent != null) {
				// Inherit baseURI and xmlLang from parent
				this.baseURI = parent.baseURI;
				this.xmlLang = parent.xmlLang;
			} else {
				this.baseURI = documentURI;
				this.xmlLang = null;
			}
		}

		public void setBaseURI(String uriString) {
			// Resolve the specified base URI against the inherited base URI (if any)
			baseURI = baseURI != null ? baseURI.resolve(createBaseURI(uriString)) : createBaseURI(uriString);
		}

		public void setNamespaceMappings(Map<String, String> namespaceMappings) {
			if (namespaceMappings.isEmpty()) {
				namespaceMap = null;
			} else {
				namespaceMap = new HashMap<>(namespaceMappings);
			}
		}

	}

	private class XmlLiteralNamespaceContext {

		private final Map<String, String> contextNamespaces = new LinkedHashMap<>();
		private final Set<String> usedPrefixes = new HashSet<>();
		private final Set<String> declaredPrefixes = new HashSet<>();
		private String inferredDefaultNamespace = null;
		private boolean defaultNamespaceSeen = false;

		void initFromStack(ElementInfo top) {
			contextNamespaces.clear();

			while (top != null) {
				if (top.namespaceMap != null) {
					for (Map.Entry<String, String> e : top.namespaceMap.entrySet()) {
						contextNamespaces.putIfAbsent(e.getKey(), e.getValue());
					}
				}
				top = top.parent;
			}

			usedPrefixes.clear();
			declaredPrefixes.clear();
			inferredDefaultNamespace = null;
			defaultNamespaceSeen = false;
		}

		void registerDeclaredPrefix(String prefix) {
			declaredPrefixes.add(prefix);
		}

		void registerUsedQName(String qName) {
			int idx = qName.indexOf(':');
			String prefix = (idx > 0) ? qName.substring(0, idx) : "";
			usedPrefixes.add(prefix); // "" = default namespace
		}

		void registerDefaultNamespaceIfNeeded(String namespaceURI) {
			if (!defaultNamespaceSeen) {
				defaultNamespaceSeen = true;
				inferredDefaultNamespace = namespaceURI;
			}
		}

		void appendNamespaceDecl(StringBuilder sb, String prefix, String namespace) {
			String attName = "xmlns";
			if (!prefix.isEmpty()) {
				attName += ":" + prefix;
			}
			sb.append(" ")
					.append(attName)
					.append("=\"")
					.append(XMLUtil.escapeDoubleQuotedAttValue(namespace))
					.append("\"");
		}

		void unregisterDeclaredPrefix(String prefix) {
			declaredPrefixes.remove(prefix);
		}

		void injectNamespaces(StringBuilder buffer) {
			if (contextNamespaces.isEmpty()) {
				return;
			}
			StringBuilder decls = new StringBuilder(128);
			boolean hasExplicitPrefixUsage = false;
			for (String p : usedPrefixes) {
				if (!p.isEmpty()) {
					hasExplicitPrefixUsage = true;
					break;
				}
			}

			if (hasExplicitPrefixUsage) {
				// CASE 1: prefixed usage → inject only missing prefixes
				for (String prefix : usedPrefixes) {
					if (!declaredPrefixes.contains(prefix) && contextNamespaces.containsKey(prefix)) {
						appendNamespaceDecl(decls, prefix, contextNamespaces.get(prefix));
					}
				}

			} else if (inferredDefaultNamespace != null && !inferredDefaultNamespace.isEmpty()) {
				// CASE 2: default namespace
				if (!declaredPrefixes.contains("")) {
					appendNamespaceDecl(decls, "", inferredDefaultNamespace);
				}

			} else {
				// CASE 3: no namespace at all
				for (Map.Entry<String, String> e : contextNamespaces.entrySet()) {
					if (!declaredPrefixes.contains(e.getKey())) {
						appendNamespaceDecl(decls, e.getKey(), e.getValue());
					}
				}
			}

			injectIntoTags(buffer, decls);
		}

		private void injectIntoTags(StringBuilder buffer, StringBuilder decls) {
			int i = 0;
			int opentag = 0;
			while (i < buffer.length()) {
				char ch = buffer.charAt(i);
				if (ch == '<') {
					if ((i + 1) < buffer.length()) {
						char nextChar = buffer.charAt(i + 1);
						if (nextChar != '/' && opentag == 0) {
							opentag++;
							int endOfFirstStartTag = buffer.substring(i).indexOf(">");
							buffer.insert(endOfFirstStartTag + i, decls.toString());
						} else {
							opentag--;
						}
					}
				}
				i += 1;
			}
		}
	}
}
