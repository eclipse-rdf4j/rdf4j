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

import org.eclipse.rdf4j.rio.RioSetting;

/**
 * A selection of writer settings specific to RDF/JSON parsers.
 * <p>
 * Settings can be overridden by means of a system property, but only if specified at JVM startup time.
 *
 * @author Tomas Kovachev t.kovachev1996@gmail.com
 */
public class RDFJSONWriterSettings {

	/**
	 * Boolean setting for RDF/JSON writer to determine whether it should stream output which can result in duplicate
	 * object keys. By default, the writer sorts output in-memory first to avoid duplicate keys.
	 * <p>
	 * Defaults to false.
	 * <p>
	 * Can be overridden by setting system property {@code org.eclipse.rdf4j.rio.rdfjson.allow_multiple_object_values}.
	 */
	public static final RioSetting<Boolean> ALLOW_MULTIPLE_OBJECT_VALUES = new BooleanRioSetting(
			"org.eclipse.rdf4j.rio.rdfjson.allow_multiple_object_values", "Allow multiple object values",
			Boolean.FALSE);

	private RDFJSONWriterSettings() {
	}

}
