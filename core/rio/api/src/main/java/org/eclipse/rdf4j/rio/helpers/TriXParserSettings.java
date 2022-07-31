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

import org.eclipse.rdf4j.rio.RioSetting;

/**
 * ParserSettings for the TriX parser features.
 * <p>
 * Several of these settings can be overridden by means of a system property, but only if specified at JVM startup time.
 *
 * @author Peter Ansell
 */
public class TriXParserSettings {

	/**
	 * Boolean setting for parser to determine whether the TriX parser should treat missing datatypes as an error.
	 * <p>
	 * Defaults to true.
	 * <p>
	 * Can be overridden by setting system property {@code org.eclipse.rdf4j.rio.trix.fail_on_missing_datatype}.
	 */
	public static final RioSetting<Boolean> FAIL_ON_MISSING_DATATYPE = new BooleanRioSetting(
			"org.eclipse.rdf4j.rio.trix.fail_on_missing_datatype", "Fail on TriX missing datatype", Boolean.TRUE);

	/**
	 * Boolean setting for parser to determine whether the TriX parser should treat invalid statements as an error.
	 * <p>
	 * Defaults to true.
	 * <p>
	 * Can be overridden by setting system property {@code org.eclipse.rdf4j.rio.trix.fail_on_invalid_statement}.
	 */
	public static final RioSetting<Boolean> FAIL_ON_INVALID_STATEMENT = new BooleanRioSetting(
			"org.eclipse.rdf4j.rio.trix.fail_on_invalid_statement", "Fail on TriX invalid statement", Boolean.TRUE);

	/**
	 * Private constructor
	 */
	private TriXParserSettings() {
	}

}
