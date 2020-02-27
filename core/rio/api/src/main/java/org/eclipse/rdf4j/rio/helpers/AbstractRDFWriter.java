/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.rio.helpers;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.function.Consumer;

import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Triple;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.util.Statements;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.RioSetting;
import org.eclipse.rdf4j.rio.WriterConfig;

/**
 * Base class for {@link RDFWriter}s offering common functionality for RDF writers.
 * 
 * @author Peter Ansell
 */
public abstract class AbstractRDFWriter implements RDFWriter {

	/**
	 * Mapping from namespace prefixes to namespace names.
	 */
	protected Map<String, String> namespaceTable;

	/**
	 * A collection of configuration options for this writer.
	 */
	private WriterConfig writerConfig = new WriterConfig();

	protected boolean writingStarted;

	protected Consumer<Statement> statementConsumer;

	/**
	 * Default constructor.
	 */
	protected AbstractRDFWriter() {
	}

	@Override
	public void handleNamespace(String prefix, String uri) throws RDFHandlerException {
		namespaceTable.put(prefix, uri);
	}

	@Override
	public RDFWriter setWriterConfig(WriterConfig config) {
		this.writerConfig = config;
		return this;
	}

	@Override
	public WriterConfig getWriterConfig() {
		return this.writerConfig;
	}

	/*
	 * Default implementation. Implementing classes must override this to specify that they support given settings.
	 */
	@Override
	public Collection<RioSetting<?>> getSupportedSettings() {
		return Collections.emptyList();
	}

	@Override
	public <T> RDFWriter set(RioSetting<T> setting, T value) {
		getWriterConfig().set(setting, value);
		return this;
	}

	@Override
	public void startRDF() throws RDFHandlerException {
		if (writingStarted) {
			throw new RDFHandlerException("Document writing has already started");
		}

		writingStarted = true;

		statementConsumer = this::handleStatementImpl;
		if (getWriterConfig().get(BasicWriterSettings.CONVERT_RDF_STAR_TO_REIFICATION)) {
			// All writers can convert RDF* to reification on request
			statementConsumer = this::handleStatementConvertRDFStar;
		} else if (!getRDFFormat().supportsRDFStar() && getWriterConfig().get(BasicWriterSettings.ENCODE_RDF_STAR)) {
			// By default non-RDF* writers encode RDF* to special RDF IRIs
			// (all parsers, including RDF* will convert back the encoded IRIs)
			statementConsumer = this::handleStatementEncodeRDFStar;
		}
	}

	@Override
	public void handleStatement(Statement st) throws RDFHandlerException {
		checkWritingStarted();
		statementConsumer.accept(st);
	}

	/**
	 * Extending classes must implement this method instead of overriding {@link #handleStatement(Statement)} in order
	 * to benefit from automatic handling of RDF* conversion or encoding.
	 *
	 * @param st the statement to handle.
	 */
	protected abstract void handleStatementImpl(Statement st);

	protected void checkWritingStarted() {
		if (!writingStarted) {
			throw new RDFHandlerException("Document writing has not started yet");
		}
	}

	private void handleStatementConvertRDFStar(Statement st) {
		Statements.convertRDFStarToReification(st, this::handleStatementImpl);
	}

	private void handleStatementEncodeRDFStar(Statement st) {
		Resource s = st.getSubject();
		Value o = st.getObject();
		if (s instanceof Triple || o instanceof Triple) {
			handleStatementImpl(new RDFStarEncodingStatement(st));
		} else {
			handleStatementImpl(st);
		}
	}
}
