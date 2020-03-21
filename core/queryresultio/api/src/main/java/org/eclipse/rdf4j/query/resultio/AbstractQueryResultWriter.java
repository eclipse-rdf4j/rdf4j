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

import org.eclipse.rdf4j.rio.RioSetting;
import org.eclipse.rdf4j.rio.WriterConfig;
import org.eclipse.rdf4j.rio.helpers.BasicWriterSettings;

/**
 * Base class for {@link QueryResultWriter}s offering common functionality for query result writers.
 * 
 * @author Peter Ansell
 */
public abstract class AbstractQueryResultWriter implements QueryResultWriter {

	private WriterConfig writerConfig = new WriterConfig();
	private final OutputStream outputStream;

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

	protected boolean xsdStringToPlainLiteral() {
		return getWriterConfig().get(BasicWriterSettings.XSD_STRING_TO_PLAIN_LITERAL);
	}
}
