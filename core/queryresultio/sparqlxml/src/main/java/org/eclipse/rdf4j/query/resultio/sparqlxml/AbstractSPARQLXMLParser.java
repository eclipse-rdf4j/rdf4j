/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.resultio.sparqlxml;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.eclipse.rdf4j.common.io.UncloseableInputStream;
import org.eclipse.rdf4j.common.xml.SimpleSAXParser;
import org.eclipse.rdf4j.common.xml.XMLReaderFactory;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.query.QueryResultHandlerException;
import org.eclipse.rdf4j.query.resultio.AbstractQueryResultParser;
import org.eclipse.rdf4j.query.resultio.QueryResultParseException;
import org.xml.sax.SAXException;

/**
 * Abstract base class for SPARQL Results XML Parsers.
 * 
 * @author Peter Ansell
 */
public abstract class AbstractSPARQLXMLParser extends AbstractQueryResultParser {

	/**
	 * 
	 */
	public AbstractSPARQLXMLParser() {
		super();
	}

	/**
	 * 
	 */
	public AbstractSPARQLXMLParser(ValueFactory valueFactory) {
		super(valueFactory);
	}

	@Override
	public void parseQueryResult(InputStream in)
		throws IOException, QueryResultParseException, QueryResultHandlerException
	{
		parseQueryResultInternal(in, true, true);
	}

	protected boolean parseQueryResultInternal(InputStream in, boolean attemptParseBoolean,
			boolean attemptParseTuple)
		throws IOException, QueryResultParseException, QueryResultHandlerException
	{
		if (!attemptParseBoolean && !attemptParseTuple) {
			throw new IllegalArgumentException(
					"Internal error: Did not specify whether to parse as either boolean and/or tuple");
		}

		BufferedInputStream buff = new BufferedInputStream(in);
		UncloseableInputStream uncloseable = new UncloseableInputStream(buff);

		SAXException caughtException = null;

		boolean result = false;

		try {
			if (attemptParseBoolean) {
				buff.mark(Integer.MAX_VALUE);
				try {
					SPARQLBooleanSAXParser valueParser = new SPARQLBooleanSAXParser();

					SimpleSAXParser booleanSAXParser = new SimpleSAXParser(XMLReaderFactory.createXMLReader());
					booleanSAXParser.setListener(valueParser);
					booleanSAXParser.parse(uncloseable);

					result = valueParser.getValue();

					try {
						if (this.handler != null) {
							this.handler.handleBoolean(result);
						}
					}
					catch (QueryResultHandlerException e) {
						if (e.getCause() != null && e.getCause() instanceof IOException) {
							throw (IOException)e.getCause();
						}
						else {
							throw new QueryResultParseException("Found an issue with the query result handler", e);
						}
					}
					// if there were no exceptions up to this point, return the
					// boolean
					// result;
					return result;
				}
				catch (SAXException e) {
					caughtException = e;
				}

				// Reset the buffered input stream and try again looking for tuple
				// results
				buff.reset();
			}

			if (attemptParseTuple) {
				try {
					SimpleSAXParser resultsSAXParser = new SimpleSAXParser(XMLReaderFactory.createXMLReader());
					resultsSAXParser.setPreserveWhitespace(true);

					resultsSAXParser.setListener(new SPARQLResultsSAXParser(this.valueFactory, this.handler));

					resultsSAXParser.parse(uncloseable);

					// we had success, so remove the exception that we were tracking
					// from
					// the boolean failure
					caughtException = null;
				}
				catch (SAXException e) {
					caughtException = e;
				}
			}

			if (caughtException != null) {
				Exception wrappedExc = caughtException.getException();

				if (wrappedExc == null) {
					throw new QueryResultParseException(caughtException);
				}
				else if (wrappedExc instanceof QueryResultParseException) {
					throw (QueryResultParseException)wrappedExc;
				}
				else if (wrappedExc instanceof QueryResultHandlerException) {
					throw (QueryResultHandlerException)wrappedExc;
				}
				else {
					throw new QueryResultParseException(wrappedExc);
				}
			}

		}
		finally {
			uncloseable.doClose();
		}
		
		return result;
	}

}