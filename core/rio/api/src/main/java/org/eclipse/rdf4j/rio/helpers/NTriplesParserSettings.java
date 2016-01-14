/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.rio.helpers;

import org.eclipse.rdf4j.rio.RioSetting;

/**
 * ParserSettings for the N-Triples parser features.
 * 
 * @author Peter Ansell
 * @since 2.7.0
 */
public class NTriplesParserSettings {

	/**
	 * Boolean setting for parser to determine whether syntactically invalid
	 * lines in N-Triples and N-Quads documents generate a parse error.
	 * <p>
	 * Defaults to true.
	 * 
	 * @since 2.7.0
	 */
	public static final RioSetting<Boolean> FAIL_ON_NTRIPLES_INVALID_LINES = new RioSettingImpl<Boolean>(
			"org.eclipse.rdf4j.rio.failonntriplesinvalidlines", "Fail on N-Triples invalid lines", Boolean.TRUE);

	/**
	 * Private constructor
	 */
	private NTriplesParserSettings() {
	}

}
