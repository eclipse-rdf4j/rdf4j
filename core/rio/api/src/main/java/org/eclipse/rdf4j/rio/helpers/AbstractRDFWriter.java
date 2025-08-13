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
package org.eclipse.rdf4j.rio.helpers;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

import org.eclipse.rdf4j.common.io.Sink;
import org.eclipse.rdf4j.common.lang.FileFormat;
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
public abstract class AbstractRDFWriter implements RDFWriter, Sink {

	/**
	 * Mapping from namespace prefixes to namespace names.
	 */
	protected Map<String, String> namespaceTable = new LinkedHashMap<>();

	/**
	 * A collection of configuration options for this writer.
	 */
	private WriterConfig writerConfig = new WriterConfig();

	private boolean writingStarted;

	protected Consumer<Statement> statementConsumer;

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

	@Override
	public FileFormat getFileFormat() {
		return getRDFFormat();
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

		statementConsumer = this::consumeStatement;
		if (getWriterConfig().get(BasicWriterSettings.CONVERT_RDF_12_REIFICATION)) {
			// All writers can convert RDF 1.2 to reification on request
			statementConsumer = this::handleStatementConvertTripleTerms;
		} else if (!getRDFFormat().supportsTripleTerms()
				&& getWriterConfig().get(BasicWriterSettings.ENCODE_TRIPLE_TERMS)) {
			// By default, non-RDF-12 writers encode triple terms to special RDF IRIs
			// (all parsers, including RDF 1.2 will convert back the encoded IRIs)
			statementConsumer = this::handleStatementEncodeTripleTerms;
		}
	}

	@Override
	public void handleStatement(Statement st) throws RDFHandlerException {
		checkWritingStarted();
		statementConsumer.accept(st);
	}

	/**
	 * Consume a statement.
	 * <p>
	 * Extending classes must override this method instead of overriding {@link #handleStatement(Statement)} in order to
	 * benefit from automatic handling of RDF 1.2 conversion or encoding.
	 *
	 * @param st the statement to consume.
	 */
	protected void consumeStatement(Statement st) {
		// this method intended to be abstract, implemented as no-op to provide basic backward compatibility.
	}

	/**
	 * See if writing has started
	 *
	 * @return {@code true} if writing has started, {@code false} otherwise
	 */
	protected boolean isWritingStarted() {
		return writingStarted;
	}

	/**
	 * Verify that writing has started.
	 *
	 * @throws RDFHandlerException if writing has not yet started.
	 */
	protected void checkWritingStarted() {
		if (!writingStarted) {
			throw new RDFHandlerException("Document writing has not started yet");
		}
	}

	private void handleStatementConvertTripleTerms(Statement st) {
		Statements.convertRDF12ReificationToRDF11(st, this::consumeStatement);
	}

	private void handleStatementEncodeTripleTerms(Statement st) {
		Resource s = st.getSubject();
		Value o = st.getObject();
		if (s instanceof Triple || o instanceof Triple) {
			consumeStatement(new TripleTermEncodingStatement(st));
		} else {
			consumeStatement(st);
		}
	}
}
