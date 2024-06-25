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
 * WriterSettings for the N-Triples writer features.
 *
 * @author Peter Ansell
 *
 * @since 4.3.0
 */
public class NTriplesWriterSettings {

	/**
	 * Boolean setting for writer to determine if unicode escapes are used.
	 * <p>
	 * Defaults to false.
	 * <p>
	 * Can be overridden by setting system property {@code org.eclipse.rdf4j.rio.ntriples.escape_unicode}
	 */
	public static final BooleanRioSetting ESCAPE_UNICODE = new BooleanRioSetting(
			"org.eclipse.rdf4j.rio.ntriples.escape_unicode", "Escape Unicode characters", Boolean.FALSE);

	/**
	 * Private constructor
	 */
	private NTriplesWriterSettings() {
	}

	static {
		assert ESCAPE_UNICODE.equals(org.eclipse.rdf4j.rio.helpers.NTriplesWriterSettings.ESCAPE_UNICODE);
	}

}
