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
package org.eclipse.rdf4j.rio.ntriples;

import org.eclipse.rdf4j.rio.RioSetting;
import org.eclipse.rdf4j.rio.helpers.BooleanRioSetting;

/**
 * ParserSettings for the N-Triples parser features.
 * <p>
 * Several of these settings can be overridden by means of a system property, but only if specified at JVM startup time.
 *
 * @author Peter Ansell
 *
 * @since 4.3.0
 */
public class NTriplesParserSettings {

	/**
	 * Boolean setting for parser to determine whether syntactically invalid lines in N-Triples and N-Quads documents
	 * generate a parse error.
	 * <p>
	 * Defaults to true.
	 * <p>
	 * Can be overridden by setting system property {@code org.eclipse.rdf4j.rio.ntriples.fail_on_invalid_lines}
	 */
	public static final RioSetting<Boolean> FAIL_ON_INVALID_LINES = new BooleanRioSetting(
			"org.eclipse.rdf4j.rio.ntriples.fail_on_invalid_lines", "Fail on N-Triples invalid lines", Boolean.TRUE);

	/**
	 * Private constructor
	 */
	private NTriplesParserSettings() {
	}

	static {
		assert FAIL_ON_INVALID_LINES.equals(org.eclipse.rdf4j.rio.helpers.NTriplesParserSettings.FAIL_ON_INVALID_LINES);
	}
}
