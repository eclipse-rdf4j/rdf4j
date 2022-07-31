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
package org.eclipse.rdf4j.rio.trix;

import static org.eclipse.rdf4j.rio.trix.TriXConstants.BNODE_TAG;
import static org.eclipse.rdf4j.rio.trix.TriXConstants.CONTEXT_TAG;
import static org.eclipse.rdf4j.rio.trix.TriXConstants.DATATYPE_ATT;
import static org.eclipse.rdf4j.rio.trix.TriXConstants.LANGUAGE_ATT;
import static org.eclipse.rdf4j.rio.trix.TriXConstants.PLAIN_LITERAL_TAG;
import static org.eclipse.rdf4j.rio.trix.TriXConstants.TRIPLE_TAG;
import static org.eclipse.rdf4j.rio.trix.TriXConstants.TYPED_LITERAL_TAG;
import static org.eclipse.rdf4j.rio.trix.TriXConstants.URI_TAG;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.input.BOMInputStream;
import org.eclipse.rdf4j.common.xml.SimpleSAXAdapter;
import org.eclipse.rdf4j.common.xml.SimpleSAXParser;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.RioSetting;
import org.eclipse.rdf4j.rio.helpers.AbstractRDFParser;
import org.eclipse.rdf4j.rio.helpers.TriXParserSettings;
import org.eclipse.rdf4j.rio.helpers.XMLParserSettings;
import org.eclipse.rdf4j.rio.helpers.XMLReaderBasedParser;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;

/**
 * A parser that can parse RDF files that are in the <a href="http://www.w3.org/2004/03/trix/">TriX format</a> .
 *
 * @author Arjohn Kampman
 */
public class TriXParser extends XMLReaderBasedParser implements ErrorHandler {

	/*--------------*
	 * Constructors *
	 *--------------*/

	private SimpleSAXParser saxParser;

	/**
	 * Creates a new TriXParser that will use a {@link SimpleValueFactory} to create objects for resources, bNodes,
	 * literals and statements.
	 */
	public TriXParser() {
		this(SimpleValueFactory.getInstance());
	}

	/**
	 * Creates a new TriXParser that will use the supplied ValueFactory to create objects for resources, bNodes,
	 * literals and statements.
	 *
	 * @param valueFactory A ValueFactory.
	 */
	public TriXParser(ValueFactory valueFactory) {
		super(valueFactory);
	}

	/*---------*
	 * Methods *
	 *---------*/

	@Override
	public final RDFFormat getRDFFormat() {
		return RDFFormat.TRIX;
	}

	@Override
	public Collection<RioSetting<?>> getSupportedSettings() {
		// Override to add TriX/XML specific supported settings
		Set<RioSetting<?>> results = new HashSet<>(super.getSupportedSettings());

		results.addAll(getCompulsoryXmlPropertySettings());
		results.addAll(getCompulsoryXmlFeatureSettings());
		results.addAll(getOptionalXmlPropertySettings());
		results.addAll(getOptionalXmlFeatureSettings());

		results.add(XMLParserSettings.CUSTOM_XML_READER);
		results.add(XMLParserSettings.FAIL_ON_MISMATCHED_TAGS);
		results.add(XMLParserSettings.FAIL_ON_SAX_NON_FATAL_ERRORS);
		results.add(TriXParserSettings.FAIL_ON_INVALID_STATEMENT);
		results.add(TriXParserSettings.FAIL_ON_MISSING_DATATYPE);
		return results;
	}

	@Override
	public void parse(InputStream in, String baseURI) throws IOException, RDFParseException, RDFHandlerException {
		if (in == null) {
			throw new IllegalArgumentException("Input stream cannot be 'null'");
		}

		InputSource inputSource = new InputSource(new BOMInputStream(in, false));
		if (baseURI != null) {
			inputSource.setSystemId(baseURI);
		}

		parse(inputSource);
	}

	@Override
	public void parse(Reader reader, String baseURI) throws IOException, RDFParseException, RDFHandlerException {
		if (reader == null) {
			throw new IllegalArgumentException("Reader cannot be 'null'");
		}

		InputSource inputSource = new InputSource(reader);
		if (baseURI != null) {
			inputSource.setSystemId(baseURI);
		}

		parse(inputSource);
	}

	private void parse(InputSource inputStreamOrReader) throws IOException, RDFParseException, RDFHandlerException {
		clear();

		try {
			if (rdfHandler != null) {
				rdfHandler.startRDF();
			}

			XMLReader xmlReader = getXMLReader();
			xmlReader.setErrorHandler(this);

			saxParser = new SimpleSAXParser(xmlReader);
			saxParser.setPreserveWhitespace(true);
			saxParser.setListener(new TriXSAXHandler());

			saxParser.parse(inputStreamOrReader);
		} catch (SAXParseException e) {
			Exception wrappedExc = e.getException();

			if (wrappedExc == null) {
				reportFatalError(e, e.getLineNumber(), e.getColumnNumber());
			} else {
				reportFatalError(wrappedExc, e.getLineNumber(), e.getColumnNumber());
			}
		} catch (SAXException e) {
			Exception wrappedExc = e.getException();

			if (wrappedExc == null) {
				reportFatalError(e);
			} else if (wrappedExc instanceof RDFParseException) {
				throw (RDFParseException) wrappedExc;
			} else if (wrappedExc instanceof RDFHandlerException) {
				throw (RDFHandlerException) wrappedExc;
			} else {
				reportFatalError(wrappedExc);
			}
		} finally {
			clear();
		}

		if (rdfHandler != null) {
			rdfHandler.endRDF();
		}
	}

	@Override
	protected Literal createLiteral(String label, String lang, IRI datatype) throws RDFParseException {
		Locator locator = saxParser.getLocator();
		if (locator != null) {
			return createLiteral(label, lang, datatype, locator.getLineNumber(), locator.getColumnNumber());
		} else {
			return createLiteral(label, lang, datatype, -1, -1);
		}
	}

	/**
	 * Overrides {@link AbstractRDFParser#reportWarning(String)}, adding line- and column number information to the
	 * error.
	 */
	@Override
	protected void reportWarning(String msg) {
		Locator locator = saxParser.getLocator();
		if (locator != null) {
			reportWarning(msg, locator.getLineNumber(), locator.getColumnNumber());
		} else {
			reportWarning(msg, -1, -1);
		}
	}

	/**
	 * Overrides {@link AbstractRDFParser#reportError(String, RioSetting)}, adding line- and column number information
	 * to the error.
	 */
	@Override
	protected void reportError(String msg, RioSetting<Boolean> setting) throws RDFParseException {
		Locator locator = saxParser.getLocator();
		if (locator != null) {
			reportError(msg, locator.getLineNumber(), locator.getColumnNumber(), setting);
		} else {
			reportError(msg, -1, -1, setting);
		}
	}

	/**
	 * Overrides {@link AbstractRDFParser#reportFatalError(String)}, adding line- and column number information to the
	 * error.
	 */
	@Override
	protected void reportFatalError(String msg) throws RDFParseException {
		Locator locator = saxParser.getLocator();
		if (locator != null) {
			reportFatalError(msg, locator.getLineNumber(), locator.getColumnNumber());
		} else {
			reportFatalError(msg, -1, -1);
		}
	}

	/**
	 * Overrides {@link AbstractRDFParser#reportFatalError(Exception)}, adding line- and column number information to
	 * the error.
	 */
	@Override
	protected void reportFatalError(Exception e) throws RDFParseException {
		Locator locator = saxParser.getLocator();
		if (locator != null) {
			reportFatalError(e, locator.getLineNumber(), locator.getColumnNumber());
		} else {
			reportFatalError(e, -1, -1);
		}
	}

	/*----------------------------*
	 * Inner class TriXSAXHandler *
	 *----------------------------*/

	private class TriXSAXHandler extends SimpleSAXAdapter {

		private Resource currentContext;

		private boolean parsingContext;

		private final List<Value> valueList;

		public TriXSAXHandler() {
			currentContext = null;
			valueList = new ArrayList<>(3);
		}

		@Override
		public void startTag(String tagName, Map<String, String> atts, String text) throws SAXException {
			try {
				if (tagName.equals(URI_TAG)) {
					valueList.add(createURI(text));
				} else if (tagName.equals(BNODE_TAG)) {
					valueList.add(createNode(text));
				} else if (tagName.equals(PLAIN_LITERAL_TAG)) {
					String lang = atts.get(LANGUAGE_ATT);
					valueList.add(createLiteral(text, lang, null));
				} else if (tagName.equals(TYPED_LITERAL_TAG)) {
					String datatype = atts.get(DATATYPE_ATT);

					if (datatype == null) {
						reportError(DATATYPE_ATT + " attribute missing for typed literal",
								TriXParserSettings.FAIL_ON_MISSING_DATATYPE);
						valueList.add(createLiteral(text, null, null));
					} else {
						IRI dtURI = createURI(datatype);
						valueList.add(createLiteral(text, null, dtURI));
					}
				} else if (tagName.equals(TRIPLE_TAG)) {
					if (parsingContext) {
						try {
							// First triple in a context, valueList can contain
							// context information
							if (valueList.size() > 1) {
								reportError("At most 1 resource can be specified for the context",
										TriXParserSettings.FAIL_ON_INVALID_STATEMENT);
							} else if (valueList.size() == 1) {
								try {
									currentContext = (Resource) valueList.get(0);
								} catch (ClassCastException e) {
									reportError("Context identifier should be a URI or blank node",
											TriXParserSettings.FAIL_ON_INVALID_STATEMENT);
								}
							}
						} finally {
							parsingContext = false;
							valueList.clear();
						}
					}
				} else if (tagName.equals(CONTEXT_TAG)) {
					parsingContext = true;
				}
			} catch (RDFParseException e) {
				throw new SAXException(e);
			}
		}

		@Override
		public void endTag(String tagName) throws SAXException {
			try {
				if (tagName.equals(TRIPLE_TAG)) {
					reportStatement();
				} else if (tagName.equals(CONTEXT_TAG)) {
					currentContext = null;
				}
			} catch (RDFParseException | RDFHandlerException e) {
				throw new SAXException(e);
			}
		}

		private void reportStatement() throws RDFParseException, RDFHandlerException {
			try {
				if (valueList.size() != 3) {
					reportError("exactly 3 values are required for a triple",
							TriXParserSettings.FAIL_ON_INVALID_STATEMENT);
					return;
				}

				Resource subj;
				IRI pred;
				Value obj;

				try {
					subj = (Resource) valueList.get(0);
				} catch (ClassCastException e) {
					reportError("First value for a triple should be a URI or blank node",
							TriXParserSettings.FAIL_ON_INVALID_STATEMENT);
					return;
				}

				try {
					pred = (IRI) valueList.get(1);
				} catch (ClassCastException e) {
					reportError("Second value for a triple should be a URI",
							TriXParserSettings.FAIL_ON_INVALID_STATEMENT);
					return;
				}

				obj = valueList.get(2);

				Statement st = createStatement(subj, pred, obj, currentContext);
				if (rdfHandler != null) {
					rdfHandler.handleStatement(st);
				}
			} finally {
				valueList.clear();
			}
		}
	} // end inner class TriXSAXHandler

	/**
	 * Implementation of SAX ErrorHandler.warning
	 */
	@Override
	public void warning(SAXParseException exception) throws SAXException {
		this.reportWarning(exception.getMessage());
	}

	/**
	 * Implementation of SAX ErrorHandler.error
	 */
	@Override
	public void error(SAXParseException exception) throws SAXException {
		try {
			this.reportError(exception.getMessage(), XMLParserSettings.FAIL_ON_SAX_NON_FATAL_ERRORS);
		} catch (RDFParseException rdfpe) {
			throw new SAXException(rdfpe);
		}
	}

	/**
	 * Implementation of SAX ErrorHandler.fatalError
	 */
	@Override
	public void fatalError(SAXParseException exception) throws SAXException {
		try {
			this.reportFatalError(exception.getMessage());
		} catch (RDFParseException rdfpe) {
			throw new SAXException(rdfpe);
		}
	}
}
