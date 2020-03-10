/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.rio.helpers;

import org.eclipse.rdf4j.rio.RioSetting;

/**
 * WriterSettings for the HDT writer features.
 * 
 * @author Bart
 */
public class HDTWriterSettings {

	/**
	 * Boolean setting for writer to specify the original file name in the metadata.
	 * <p>
	 * Defaults to empty string.
	 * <p>
	 * Can be overridden by setting system property {@code org.eclipse.rdf4j.rio.hdt.original_file}
	 */
	public static final RioSetting<String> ORIGINAL_FILE = new StringRioSetting(
			"org.eclipse.rdf4j.rio.hdt.original_file", "Escape Unicode characters", "");

	/**
	 * Private constructor
	 */
	private HDTWriterSettings() {
	}

}
