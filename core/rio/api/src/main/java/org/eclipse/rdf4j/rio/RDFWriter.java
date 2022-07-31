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
package org.eclipse.rdf4j.rio;

import java.io.OutputStream;
import java.io.Writer;
import java.util.Collection;

/**
 * An interface for RDF document writers. To allow RDF document writers to be created through reflection, all
 * implementing classes should define at least two public constructors: one with an {@link OutputStream} argument and
 * one with an {@link Writer} argument.
 */
public interface RDFWriter extends RDFHandler {

	/**
	 * Gets the RDF format that this RDFWriter uses.
	 */
	RDFFormat getRDFFormat();

	/**
	 * Sets all supplied writer configuration options.
	 *
	 * @param config a writer configuration object.
	 * @return Either a copy of this writer, if it is immutable, or this object, to allow chaining of method calls.
	 */
	RDFWriter setWriterConfig(WriterConfig config);

	/**
	 * Retrieves the current writer configuration as a single object.
	 *
	 * @return a writer configuration object representing the current configuration of the writer.
	 */
	WriterConfig getWriterConfig();

	/**
	 * @return A collection of {@link RioSetting}s that are supported by this RDFWriter.
	 */
	Collection<RioSetting<?>> getSupportedSettings();

	/**
	 * Set a setting on the writer, and return this writer object to allow chaining.
	 *
	 * @param setting The setting to change.
	 * @param value   The value to change.
	 * @return Either a copy of this writer, if it is immutable, or this object, to allow chaining of method calls.
	 */
	<T> RDFWriter set(RioSetting<T> setting, T value);

}
