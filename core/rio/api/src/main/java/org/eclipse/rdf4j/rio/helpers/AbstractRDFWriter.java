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
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.TripleTerm;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.util.RDFVersionsConversionContext;
import org.eclipse.rdf4j.model.util.Statements;
import org.eclipse.rdf4j.model.util.VersionLabel;
import org.eclipse.rdf4j.model.vocabulary.RDF;
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
	/**
	 * True once at least one RDF 1.2-specific feature (triple term or directional literal) has been observed in the
	 * incoming stream.
	 */
	private boolean rdf12FeatureDetected;
	/**
	 * True once the version announcement has been written to the output stream.
	 */
	private boolean versionAnnouncementWritten;

	protected Consumer<Statement> statementConsumer;

	protected RDFVersionsConversionContext rdfVersionsConversionContext;

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
		} else if (getWriterConfig().get(BasicWriterSettings.RDF_OUTPUT_VERSION) == VersionLabel.RDF_1_2_BASIC) {
			// We need conversion context for assigning the same blank noodes for the same triple terms and not flooding
			// with PrpositionForm statements
			rdfVersionsConversionContext = new RDFVersionsConversionContext();
			statementConsumer = this::handleStatementsRDF12BasicConversion;
		} else if (getWriterConfig().get(BasicWriterSettings.RDF_OUTPUT_VERSION) == VersionLabel.RDF_1_1) {
			rdfVersionsConversionContext = new RDFVersionsConversionContext();
			statementConsumer = this::handleStatementsRDF11Conversion;
		} else if (!getRDFFormat().supportsTripleTerms()
				&& getWriterConfig().get(BasicWriterSettings.ENCODE_TRIPLE_TERMS)) {
			// By default, non-RDF-12 writers encode tripleTerm terms to special RDF IRIs
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

	/**
	 * Returns {@code true} if the given {@link Value} requires an RDF 1.2 version announcement (i.e. it is a triple
	 * term or a directional language-tagged literal).
	 *
	 * @param v the value to test – may be {@code null}, in which case {@code false} is returned.
	 */
	public static boolean isRdf12Feature(Value v) {
		if (v == null) {
			return false;
		}
		return v.isTripleTerm()
				|| (v.isLiteral() && RDF.DIRLANGSTRING.equals(((Literal) v).getDatatype()));
	}

	/**
	 * Records that an RDF 1.2-specific feature was observed in {@code values}. Has no effect once the first feature has
	 * already been noted.
	 *
	 * @param values zero or more {@link Value}s to inspect.
	 */
	protected void noteRdf12Feature(Value... values) {
		if (!rdf12FeatureDetected) {
			for (Value v : values) {
				if (isRdf12Feature(v)) {
					rdf12FeatureDetected = true;
					return;
				}
			}
		}
	}

	/**
	 * Returns {@code true} if at least one RDF 1.2-specific feature has been noted so far.
	 */
	protected boolean isRdf12FeatureDetected() {
		return rdf12FeatureDetected;
	}

	/**
	 * Returns {@code true} if the version announcement has already been written to the output stream.
	 */
	protected boolean isVersionAnnouncementWritten() {
		return versionAnnouncementWritten;
	}

	/**
	 * Override to return {@code true} for writers where an inline version announcement is mandatory when RDF
	 * 1.2-specific features are serialised (Turtle, TriG, N3, N-Triples, N-Quads).
	 * <p>
	 * Returns {@code false} by default (JSON-LD, RDF/JSON, NDJSON-LD writers keep it optional).
	 */
	protected boolean requiresVersionAnnouncement() {
		return false;
	}

	/**
	 * Called by {@link #ensureVersionAnnouncement()} immediately before {@link #writeVersionAnnouncement()}. Override
	 * to flush / close any open output structure that must not straddle the version directive (e.g. TurtleWriter closes
	 * an open predicate-object list).
	 * <p>
	 * Default implementation is a no-op.
	 */
	protected void prepareForVersionAnnouncement() throws RDFHandlerException {
		// no-op; subclasses may override
	}

	/**
	 * Writes the format-specific version directive to the output stream. Called at most once per document, by
	 * {@link #ensureVersionAnnouncement()}.
	 * <p>
	 * Default implementation is a no-op; concrete writers must override this.
	 */
	protected void writeVersionAnnouncement() throws RDFHandlerException {
		// no-op; subclasses override
	}

	/**
	 * Emits the version announcement if — and only if — all of the following hold:
	 * <ul>
	 * <li>at least one RDF 1.2 feature has been observed ({@link #noteRdf12Feature});</li>
	 * <li>the announcement has not been written yet;</li>
	 * <li>{@link #requiresVersionAnnouncement()} returns {@code true}.</li>
	 * </ul>
	 * Calls {@link #prepareForVersionAnnouncement()} before writing to let writers close any open output structure
	 * first.
	 */
	protected final void ensureVersionAnnouncement() throws RDFHandlerException {
		if (!rdf12FeatureDetected || versionAnnouncementWritten || !requiresVersionAnnouncement()
				|| !writerConfig.get(BasicWriterSettings.ANNOUNCE_RDF12_VERSION)) {
			return;
		}
		prepareForVersionAnnouncement();
		writeVersionAnnouncement();
		versionAnnouncementWritten = true;
	}

	private void handleStatementConvertTripleTerms(Statement st) {
		Statements.convertRDF12ToStandardReification(st, this::consumeStatement);
	}

	private void handleStatementEncodeTripleTerms(Statement st) {
		Resource s = st.getSubject();
		Value o = st.getObject();
		if (s instanceof TripleTerm || o instanceof TripleTerm) {
			consumeStatement(new TripleTermEncodingStatement(st));
		} else {
			consumeStatement(st);
		}
	}

	private void handleStatementsRDF12BasicConversion(Statement st) {
		Statements.convertRDFTo12Basic(st, this::consumeStatement, rdfVersionsConversionContext);
	}

	private void handleStatementsRDF11Conversion(Statement st) {
		Statements.convertRDFTo11(st, this::consumeStatement, rdfVersionsConversionContext);
	}

	public RDFVersionsConversionContext getRdfVersionsConversionContext() {
		return rdfVersionsConversionContext;
	}

	public void setRdfVersionsConversionContext(RDFVersionsConversionContext rdfVersionsConversionContext) {
		this.rdfVersionsConversionContext = rdfVersionsConversionContext;
	}
}
