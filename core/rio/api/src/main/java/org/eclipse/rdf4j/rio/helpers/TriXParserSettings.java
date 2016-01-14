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
 * ParserSettings for the TriX parser features.
 * 
 * @author Peter Ansell
 * @since 2.7.0
 */
public class TriXParserSettings {

	/**
	 * Boolean setting for parser to determine whether missing datatypes in TriX
	 * are ignored.
	 * <p>
	 * Defaults to true.
	 * 
	 * @since 2.7.0
	 */
	public static final RioSetting<Boolean> FAIL_ON_TRIX_MISSING_DATATYPE = new RioSettingImpl<Boolean>(
			"org.eclipse.rdf4j.rio.ignoretrixmissingdatatype", "Ignore TriX missing datatype", Boolean.TRUE);

	/**
	 * Boolean setting for parser to determine whether invalid statements are
	 * ignored in TriX.
	 * <p>
	 * Defaults to true.
	 * 
	 * @since 2.7.0
	 */
	public static final RioSetting<Boolean> FAIL_ON_TRIX_INVALID_STATEMENT = new RioSettingImpl<Boolean>(
			"org.eclipse.rdf4j.rio.ignoretrixmissingdatatype", "Ignore TriX missing datatype", Boolean.TRUE);

	/**
	 * Private constructor
	 */
	private TriXParserSettings() {
	}

}
