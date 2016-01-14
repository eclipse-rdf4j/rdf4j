/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.rio.jsonld;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.helpers.AbstractRDFParser;

import com.fasterxml.jackson.core.JsonParseException;
import com.github.jsonldjava.core.JsonLdError;
import com.github.jsonldjava.core.JsonLdOptions;
import com.github.jsonldjava.core.JsonLdProcessor;
import com.github.jsonldjava.utils.JsonUtils;

/**
 * An {@link RDFParser} that links to {@link JSONLDInternalTripleCallback}.
 * 
 * @author Peter Ansell
 */
public class JSONLDParser extends AbstractRDFParser implements RDFParser {

	/**
	 * Default constructor
	 */
	public JSONLDParser() {
		super();
	}

	/**
	 * Creates a Sesame JSONLD Parser using the given {@link ValueFactory} to
	 * create new {@link Value}s.
	 * 
	 * @param valueFactory
	 *        The ValueFactory to use
	 */
	public JSONLDParser(final ValueFactory valueFactory) {
		super(valueFactory);
	}

	@Override
	public RDFFormat getRDFFormat() {
		return RDFFormat.JSONLD;
	}

	@Override
	public void parse(final InputStream in, final String baseURI)
		throws IOException, RDFParseException, RDFHandlerException
	{
		final JSONLDInternalTripleCallback callback = new JSONLDInternalTripleCallback(getRDFHandler(),
				valueFactory, getParserConfig(), getParseErrorListener());

		final JsonLdOptions options = new JsonLdOptions(baseURI);
		options.useNamespaces = true;

		try {
			JsonLdProcessor.toRDF(JsonUtils.fromInputStream(in), callback, options);
		}
		catch (final JsonLdError e) {
			throw new RDFParseException("Could not parse JSONLD", e);
		}
		catch (final JsonParseException e) {
			throw new RDFParseException("Could not parse JSONLD", e);
		}
		catch (final RuntimeException e) {
			if (e.getCause() != null && e.getCause() instanceof RDFParseException) {
				throw (RDFParseException)e.getCause();
			}
			throw e;
		}
	}

	@Override
	public void parse(final Reader reader, final String baseURI)
		throws IOException, RDFParseException, RDFHandlerException
	{
		final JSONLDInternalTripleCallback callback = new JSONLDInternalTripleCallback(getRDFHandler(),
				valueFactory, getParserConfig(), getParseErrorListener());

		final JsonLdOptions options = new JsonLdOptions(baseURI);
		options.useNamespaces = true;

		try {
			JsonLdProcessor.toRDF(JsonUtils.fromReader(reader), callback, options);
		}
		catch (final JsonLdError e) {
			throw new RDFParseException("Could not parse JSONLD", e);
		}
		catch (final JsonParseException e) {
			throw new RDFParseException("Could not parse JSONLD", e);
		}
		catch (final RuntimeException e) {
			if (e.getCause() != null && e.getCause() instanceof RDFParseException) {
				throw (RDFParseException)e.getCause();
			}
			throw e;
		}
	}

}
