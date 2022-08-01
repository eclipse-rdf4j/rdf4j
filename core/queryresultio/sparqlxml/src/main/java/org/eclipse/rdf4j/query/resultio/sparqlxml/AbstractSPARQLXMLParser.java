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
package org.eclipse.rdf4j.query.resultio.sparqlxml;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.rdf4j.common.io.UncloseableInputStream;
import org.eclipse.rdf4j.common.xml.SimpleSAXParser;
import org.eclipse.rdf4j.common.xml.XMLReaderFactory;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.query.QueryResultHandlerException;
import org.eclipse.rdf4j.query.resultio.AbstractQueryResultParser;
import org.eclipse.rdf4j.query.resultio.QueryResultParseException;
import org.eclipse.rdf4j.rio.RioSetting;
import org.eclipse.rdf4j.rio.helpers.XMLParserSettings;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;

/**
 * Abstract base class for SPARQL Results XML Parsers.
 *
 * @author Peter Ansell
 */
public abstract class AbstractSPARQLXMLParser extends AbstractQueryResultParser implements ErrorHandler {

	private SimpleSAXParser internalSAXParser;

	/**
	 *
	 */
	protected AbstractSPARQLXMLParser() {
		super();
	}

	/**
	 *
	 */
	protected AbstractSPARQLXMLParser(ValueFactory valueFactory) {
		super(valueFactory);
	}

	@Override
	public void parseQueryResult(InputStream in)
			throws IOException, QueryResultParseException, QueryResultHandlerException {
		parseQueryResultInternal(in, true, true);
	}

	protected boolean parseQueryResultInternal(InputStream in, boolean attemptParseBoolean, boolean attemptParseTuple)
			throws IOException, QueryResultParseException, QueryResultHandlerException {
		if (!attemptParseBoolean && !attemptParseTuple) {
			throw new IllegalArgumentException(
					"Internal error: Did not specify whether to parse as either boolean and/or tuple");
		}

		BufferedInputStream buff = new BufferedInputStream(in);
		// Wrap in a custom InputStream that doesn't allow close to be called by dependencies before we are ready to
		// call it
		UncloseableInputStream uncloseable = new UncloseableInputStream(buff);

		SAXException caughtException = null;

		boolean result = false;

		try {
			if (attemptParseBoolean) {
				buff.mark(Integer.MAX_VALUE);
				try {
					SPARQLBooleanSAXParser valueParser = new SPARQLBooleanSAXParser();

					XMLReader xmlReader;

					if (getParserConfig().isSet(XMLParserSettings.CUSTOM_XML_READER)) {
						xmlReader = getParserConfig().get(XMLParserSettings.CUSTOM_XML_READER);
					} else {
						xmlReader = XMLReaderFactory.createXMLReader();
					}
					xmlReader.setErrorHandler(this);

					// Set all compulsory feature settings, using the defaults if they are
					// not explicitly set
					for (RioSetting<Boolean> aSetting : getCompulsoryXmlFeatureSettings()) {
						try {
							xmlReader.setFeature(aSetting.getKey(), getParserConfig().get(aSetting));
						} catch (SAXNotRecognizedException e) {
							reportWarning(String.format("%s is not a recognized SAX feature.", aSetting.getKey()));
						} catch (SAXNotSupportedException e) {
							reportWarning(String.format("%s is not a supported SAX feature.", aSetting.getKey()));
						}
					}

					// Set all compulsory property settings, using the defaults if they are
					// not explicitly set
					for (RioSetting<?> aSetting : getCompulsoryXmlPropertySettings()) {
						try {
							xmlReader.setProperty(aSetting.getKey(), getParserConfig().get(aSetting));
						} catch (SAXNotRecognizedException e) {
							reportWarning(String.format("%s is not a recognized SAX property.", aSetting.getKey()));
						} catch (SAXNotSupportedException e) {
							reportWarning(String.format("%s is not a supported SAX property.", aSetting.getKey()));
						}
					}

					// Check for any optional feature settings that are explicitly set in
					// the parser config
					for (RioSetting<Boolean> aSetting : getOptionalXmlFeatureSettings()) {
						try {
							if (getParserConfig().isSet(aSetting)) {
								xmlReader.setFeature(aSetting.getKey(), getParserConfig().get(aSetting));
							}
						} catch (SAXNotRecognizedException e) {
							reportWarning(String.format("%s is not a recognized SAX feature.", aSetting.getKey()));
						} catch (SAXNotSupportedException e) {
							reportWarning(String.format("%s is not a supported SAX feature.", aSetting.getKey()));
						}
					}

					// Check for any optional property settings that are explicitly set in
					// the parser config
					for (RioSetting<?> aSetting : getOptionalXmlPropertySettings()) {
						try {
							if (getParserConfig().isSet(aSetting)) {
								xmlReader.setProperty(aSetting.getKey(), getParserConfig().get(aSetting));
							}
						} catch (SAXNotRecognizedException e) {
							reportWarning(String.format("%s is not a recognized SAX property.", aSetting.getKey()));
						} catch (SAXNotSupportedException e) {
							reportWarning(String.format("%s is not a supported SAX property.", aSetting.getKey()));
						}
					}

					internalSAXParser = new SimpleSAXParser(xmlReader);
					internalSAXParser.setPreserveWhitespace(true);

					internalSAXParser.setListener(valueParser);
					internalSAXParser.parse(uncloseable);

					result = valueParser.getValue();

					try {
						if (this.handler != null) {
							this.handler.handleBoolean(result);
						}
					} catch (QueryResultHandlerException e) {
						if (e.getCause() != null && e.getCause() instanceof IOException) {
							throw (IOException) e.getCause();
						} else {
							throw new QueryResultParseException("Found an issue with the query result handler", e);
						}
					}
					// if there were no exceptions up to this point, return the
					// boolean
					// result;
					return result;
				} catch (SAXException e) {
					caughtException = e;
				}

				// Reset the buffered input stream and try again looking for tuple
				// results
				buff.reset();
			}

			if (attemptParseTuple) {
				try {
					XMLReader xmlReader = XMLReaderFactory.createXMLReader();
					xmlReader.setErrorHandler(this);
					internalSAXParser = new SimpleSAXParser(xmlReader);
					internalSAXParser.setPreserveWhitespace(true);

					internalSAXParser.setListener(new SPARQLResultsSAXParser(this.valueFactory, this.handler));

					internalSAXParser.parse(uncloseable);

					// we had success, so remove the exception that we were tracking
					// from
					// the boolean failure
					caughtException = null;
				} catch (SAXException e) {
					caughtException = e;
				}
			}

			if (caughtException != null) {
				Exception wrappedExc = caughtException.getException();

				if (wrappedExc == null) {
					throw new QueryResultParseException(caughtException);
				} else if (wrappedExc instanceof QueryResultParseException) {
					throw (QueryResultParseException) wrappedExc;
				} else if (wrappedExc instanceof QueryResultHandlerException) {
					throw (QueryResultHandlerException) wrappedExc;
				} else {
					throw new QueryResultParseException(wrappedExc);
				}
			}

		} finally {
			// Explicitly call the delegator to the close method to actually close it
			uncloseable.doClose();
		}

		return result;
	}

	protected void reportWarning(String msg) {
		if (getParseErrorListener() != null) {
			getParseErrorListener().warning(msg, internalSAXParser.getLocator().getLineNumber(),
					internalSAXParser.getLocator().getColumnNumber());
		}
	}

	/**
	 * Returns a collection of settings that will always be set as XML parser properties using
	 * {@link XMLReader#setProperty(String, Object)}
	 * <p>
	 * Subclasses can override this to specify more supported settings.
	 *
	 * @return A collection of {@link RioSetting}s that indicate which properties will always be setup using
	 *         {@link XMLReader#setProperty(String, Object)}.
	 */
	public Collection<RioSetting<?>> getCompulsoryXmlPropertySettings() {
		return Collections.<RioSetting<?>>emptyList();
	}

	/**
	 * Returns a collection of settings that will always be set as XML parser features using
	 * {@link XMLReader#setFeature(String, boolean)}.
	 * <p>
	 * Subclasses can override this to specify more supported settings.
	 *
	 * @return A collection of {@link RioSetting}s that indicate which boolean settings will always be setup using
	 *         {@link XMLReader#setFeature(String, boolean)}.
	 */
	public Collection<RioSetting<Boolean>> getCompulsoryXmlFeatureSettings() {
		Set<RioSetting<Boolean>> results = new HashSet<>();
		results.add(XMLParserSettings.SECURE_PROCESSING);
		results.add(XMLParserSettings.DISALLOW_DOCTYPE_DECL);
		results.add(XMLParserSettings.EXTERNAL_GENERAL_ENTITIES);
		results.add(XMLParserSettings.EXTERNAL_PARAMETER_ENTITIES);
		return results;
	}

	/**
	 * Returns a collection of settings that will be used, if set in {@link #getParserConfig()}, as XML parser
	 * properties using {@link XMLReader#setProperty(String, Object)}
	 * <p>
	 * Subclasses can override this to specify more supported settings.
	 *
	 * @return A collection of {@link RioSetting}s that indicate which properties can be setup using
	 *         {@link XMLReader#setProperty(String, Object)}.
	 */
	public Collection<RioSetting<?>> getOptionalXmlPropertySettings() {
		return Collections.<RioSetting<?>>emptyList();
	}

	/**
	 * Returns a collection of settings that will be used, if set in {@link #getParserConfig()}, as XML parser features
	 * using {@link XMLReader#setFeature(String, boolean)}.
	 * <p>
	 * Subclasses can override this to specify more supported settings.
	 *
	 * @return A collection of {@link RioSetting}s that indicate which boolean settings can be setup using
	 *         {@link XMLReader#setFeature(String, boolean)}.
	 */
	public Collection<RioSetting<Boolean>> getOptionalXmlFeatureSettings() {
		Set<RioSetting<Boolean>> results = new HashSet<>();
		results.add(XMLParserSettings.LOAD_EXTERNAL_DTD);
		return results;
	}

	@Override
	public Collection<RioSetting<?>> getSupportedSettings() {
		// Override to add SPARQL/XML specific supported settings
		Set<RioSetting<?>> results = new HashSet<>(super.getSupportedSettings());

		results.addAll(getCompulsoryXmlPropertySettings());
		results.addAll(getCompulsoryXmlFeatureSettings());
		results.addAll(getOptionalXmlPropertySettings());
		results.addAll(getOptionalXmlFeatureSettings());

		results.add(XMLParserSettings.CUSTOM_XML_READER);
		results.add(XMLParserSettings.FAIL_ON_SAX_NON_FATAL_ERRORS);

		return results;
	}

	@Override
	public void warning(SAXParseException exception) throws SAXException {
		reportWarning(exception.getMessage());
	}

	@Override
	public void error(SAXParseException exception) throws SAXException {
		try {
			if (getParserConfig().get(XMLParserSettings.FAIL_ON_SAX_NON_FATAL_ERRORS)) {
				if (getParseErrorListener() != null) {
					getParseErrorListener().error(exception.getMessage(),
							internalSAXParser.getLocator().getLineNumber(),
							internalSAXParser.getLocator().getColumnNumber());
				}

				if (!getParserConfig().isNonFatalError(XMLParserSettings.FAIL_ON_SAX_NON_FATAL_ERRORS)) {
					throw new QueryResultParseException(exception, internalSAXParser.getLocator().getLineNumber(),
							internalSAXParser.getLocator().getColumnNumber());
				}
			}
		} catch (QueryResultParseException e) {
			throw new SAXException(e);
		}
	}

	@Override
	public void fatalError(SAXParseException exception) throws SAXException {
		if (getParseErrorListener() != null) {
			getParseErrorListener().fatalError(exception.getMessage(), internalSAXParser.getLocator().getLineNumber(),
					internalSAXParser.getLocator().getColumnNumber());
		}

		throw new SAXParseException(exception.getMessage(), internalSAXParser.getLocator(),
				new QueryResultParseException(exception, internalSAXParser.getLocator().getLineNumber(),
						internalSAXParser.getLocator().getColumnNumber()));
	}

}
