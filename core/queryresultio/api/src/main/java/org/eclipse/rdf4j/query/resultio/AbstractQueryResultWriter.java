/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.resultio;

import java.io.OutputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.List;

import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.TupleQueryResultHandlerException;
import org.eclipse.rdf4j.rio.RioSetting;
import org.eclipse.rdf4j.rio.WriterConfig;
import org.eclipse.rdf4j.rio.helpers.BasicWriterSettings;
import org.eclipse.rdf4j.rio.helpers.RDFStarUtil;

/**
 * Base class for {@link QueryResultWriter}s offering common functionality for query result writers.
 * 
 * @author Peter Ansell
 */
public abstract class AbstractQueryResultWriter implements QueryResultWriter {

	private WriterConfig writerConfig = new WriterConfig();
	private final OutputStream outputStream;

	private boolean encodeRDFStar;

	/**
	 * Default constructor.
	 */
	protected AbstractQueryResultWriter() {
		this(null);
	}

	protected AbstractQueryResultWriter(OutputStream out) {
		this.outputStream = out;
	}

	public Optional<OutputStream> getOutputStream() {
		return Optional.ofNullable(outputStream);
	}

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
		return Collections.emptyList();
	}

	@Override
	public void startQueryResult(List<String> bindingNames) throws TupleQueryResultHandlerException {
		// Formats without native RDF* support obey the ENCODE_RDF_STAR setting and may encode RDF* triples to IRIs
		encodeRDFStar = this instanceof TupleQueryResultWriter
				&& !((TupleQueryResultWriter) this).getTupleQueryResultFormat().supportsRDFStar()
				&& getWriterConfig().get(BasicWriterSettings.ENCODE_RDF_STAR);
	}

	@Override
	public void handleSolution(BindingSet bindingSet) throws TupleQueryResultHandlerException {
		if (encodeRDFStar) {
			handleSolutionImpl(new ValueMappingBindingSet(bindingSet, RDFStarUtil::toRDFEncodedValue));
		} else {
			handleSolutionImpl(bindingSet);
		}
	}

	/**
	 * Extending classes must implement this method instead of overriding {@link #handleSolution(BindingSet)} in order
	 * to benefit from automatic handling of RDF* encoding.
	 *
	 * @param bindings the solution to handle
	 * @throws TupleQueryResultHandlerException
	 */
	protected abstract void handleSolutionImpl(BindingSet bindings) throws TupleQueryResultHandlerException;

	protected boolean xsdStringToPlainLiteral() {
		return getWriterConfig().get(BasicWriterSettings.XSD_STRING_TO_PLAIN_LITERAL);
	}
}
