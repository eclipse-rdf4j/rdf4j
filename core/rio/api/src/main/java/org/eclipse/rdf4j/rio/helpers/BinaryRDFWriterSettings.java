/*******************************************************************************
 * Copyright (c) 2021 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/

package org.eclipse.rdf4j.rio.helpers;

import java.nio.charset.StandardCharsets;

import org.eclipse.rdf4j.rio.RioSetting;

/**
 * WriterSettings for the binary RDF writer.
 *
 * @author Frens Jan Rumph
 */
public class BinaryRDFWriterSettings {

	/**
	 * Setting for the binary RDF format to use.
	 * <p>
	 * Defaults to {@code 2}.
	 * <p>
	 * Can be overridden by setting system property {@code org.eclipse.rdf4j.rio.binary.format_version}
	 */
	public static final RioSetting<Long> VERSION = new LongRioSetting(
			"org.eclipse.rdf4j.rio.binary.format_version", "Binary RDF format", 2L);

	/**
	 * Setting for the number of statements to consider while analyzing duplicate RDF terms. Terms that occur twice or
	 * more within the buffer of statements are written out (starting from the second occurrence) as identifiers.
	 * <p>
	 * Defaults to {@code 8192}.
	 * <p>
	 * Can be overridden by setting system property {@code org.eclipse.rdf4j.rio.binary.buffer_size}
	 */
	public static final RioSetting<Long> BUFFER_SIZE = new LongRioSetting(
			"org.eclipse.rdf4j.rio.binary.buffer_size", "Buffer size", 8192L);

	/**
	 * Setting for the character set to use for encoding strings (only applicable to version 2 of the binary RDF
	 * format).
	 * <p>
	 * Defaults to {@code "UTF-8"}.
	 * <p>
	 * Can be overridden by setting system property {@code org.eclipse.rdf4j.rio.binary.charset}
	 */
	public static final RioSetting<String> CHARSET = new StringRioSetting(
			"org.eclipse.rdf4j.rio.binary.charset", "Charset", StandardCharsets.UTF_8.name());

	/**
	 * Setting for whether to recycle IDs while writing binary RDF files. (only applicable to version 2 of the binary
	 * RDF format).
	 * <p>
	 * If enabled (the default), once an RDF term is no longer referenced in the buffer of statements (see also
	 * {@link #BUFFER_SIZE}), the ID of that term can be reused and any in memory reference to that term is released. If
	 * disabled, once an RDF term is assigned an ID it is never released and an in-memory reference to that term is
	 * maintained in memory. Note that disabling this setting <i>may</i> decrease file size, but also <i>may</i> result
	 * in an {@link OutOfMemoryError} because heap memory used is for every term that is ever assigned an ID.
	 * </p>
	 * <p>
	 * Defaults to {@code true}.
	 * <p>
	 * Can be overridden by setting system property {@code org.eclipse.rdf4j.rio.binary.recycle_ids}
	 */
	public static final RioSetting<Boolean> RECYCLE_IDS = new BooleanRioSetting(
			"org.eclipse.rdf4j.rio.binary.recycle_ids", "Charset", true);

	/**
	 * Private constructor
	 */
	private BinaryRDFWriterSettings() {
	}

}
