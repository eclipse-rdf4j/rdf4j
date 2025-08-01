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
package org.eclipse.rdf4j.query.resultio;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.eclipse.rdf4j.common.io.Sink;
import org.eclipse.rdf4j.common.lang.FileFormat;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.TupleQueryResultHandlerException;
import org.eclipse.rdf4j.rio.RioSetting;
import org.eclipse.rdf4j.rio.WriterConfig;
import org.eclipse.rdf4j.rio.helpers.BasicWriterSettings;
import org.eclipse.rdf4j.rio.helpers.TripleTermUtil;

/**
 * Base class for {@link QueryResultWriter}s offering common functionality for query result writers.
 *
 * @author Peter Ansell
 */
public abstract class AbstractQueryResultWriter implements QueryResultWriter, Sink {

	private WriterConfig writerConfig = new WriterConfig();

	private boolean encodeTripleTerms;

	@Override
	public void setWriterConfig(WriterConfig config) {
		this.writerConfig = config;
	}

	@Override
	public WriterConfig getWriterConfig() {
		return this.writerConfig;
	}

	@Override
	public Collection<RioSetting<?>> getSupportedSettings() {
		return Arrays.asList(BasicWriterSettings.ENCODE_TRIPLE_TERMS, BasicWriterSettings.XSD_STRING_TO_PLAIN_LITERAL);
	}

	@Override
	public FileFormat getFileFormat() {
		return getQueryResultFormat();
	}

	@Override
	public void startQueryResult(List<String> bindingNames) throws TupleQueryResultHandlerException {
		// Formats without native RDF 1.2 support obey the ENCODE_TRIPLE_TERMS setting and may encode RDF 1.2 triples to
		// IRIs
		encodeTripleTerms = this instanceof TupleQueryResultWriter
				&& !((TupleQueryResultWriter) this).getTupleQueryResultFormat().supportsRDFStar()
				&& getWriterConfig().get(BasicWriterSettings.ENCODE_TRIPLE_TERMS);
	}

	@Override
	public void handleSolution(BindingSet bindingSet) throws TupleQueryResultHandlerException {
		if (encodeTripleTerms) {
			handleSolutionImpl(new ValueMappingBindingSet(bindingSet, TripleTermUtil::toRDFEncodedValue));
		} else {
			handleSolutionImpl(bindingSet);
		}
	}

	/**
	 * Extending classes must implement this method instead of overriding {@link #handleSolution(BindingSet)} in order
	 * to benefit from automatic handling of RDF 1.2 encoding.
	 *
	 * @param bindings the solution to handle
	 * @throws TupleQueryResultHandlerException
	 * @implNote this temporary implementation throws an {@link UnsupportedOperationException} and is only provided for
	 *           backward compatility.
	 * @since 3.2.0
	 */
	protected void handleSolutionImpl(BindingSet bindings) throws TupleQueryResultHandlerException {
		throw new UnsupportedOperationException();
	}

	protected boolean xsdStringToPlainLiteral() {
		return getWriterConfig().get(BasicWriterSettings.XSD_STRING_TO_PLAIN_LITERAL);
	}
}
