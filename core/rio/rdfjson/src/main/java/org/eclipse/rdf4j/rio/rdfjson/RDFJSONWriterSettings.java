/*******************************************************************************
 * Copyright (c) 2022 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.rio.rdfjson;

import org.eclipse.rdf4j.rio.helpers.BooleanRioSetting;
import org.eclipse.rdf4j.rio.helpers.LongRioSetting;

/**
 * A selection of writer settings specific to RDF/JSON parsers.
 * <p>
 * Settings can be overridden by means of a system property, but only if specified at JVM startup time.
 *
 * @author Tomas Kovachev t.kovachev1996@gmail.com
 *
 * @since 4.3.0
 */
public class RDFJSONWriterSettings {

	/**
	 * Boolean setting for RDF/JSON writer to determine whether it should stream output which can result in duplicate
	 * object keys. By default, the writer sorts output with a bounded memory window and temporary disk runs to avoid
	 * duplicate keys.
	 * <p>
	 * Defaults to false.
	 * <p>
	 * Can be overridden by setting system property {@code org.eclipse.rdf4j.rio.rdfjson.allow_multiple_object_values}.
	 */
	public static final BooleanRioSetting ALLOW_MULTIPLE_OBJECT_VALUES = new BooleanRioSetting(
			"org.eclipse.rdf4j.rio.rdfjson.allow_multiple_object_values", "Allow multiple object values",
			Boolean.FALSE);

	/**
	 * Maximum estimated bytes of statements sorted in memory before spilling a run. A single RDF value may exceed this
	 * limit because it is already supplied in memory by the caller. Defaults to 8 MiB.
	 */
	public static final LongRioSetting MAX_IN_MEMORY_BYTES = new LongRioSetting(
			"org.eclipse.rdf4j.rio.rdfjson.max_in_memory_bytes", "RDF/JSON sorting window bytes",
			8L * 1024 * 1024);

	/**
	 * Maximum aggregate temporary bytes held by concurrent RDF/JSON writers. Exports that exceed this budget fail with
	 * an {@code RDFHandlerException}. Defaults to 1 GiB.
	 */
	public static final LongRioSetting MAX_SPILL_BYTES = new LongRioSetting(
			"org.eclipse.rdf4j.rio.rdfjson.max_spill_bytes", "RDF/JSON temporary disk bytes",
			1024L * 1024 * 1024);

	private RDFJSONWriterSettings() {
	}

	static {
		assert ALLOW_MULTIPLE_OBJECT_VALUES
				.equals(org.eclipse.rdf4j.rio.helpers.RDFJSONWriterSettings.ALLOW_MULTIPLE_OBJECT_VALUES);
	}

}
