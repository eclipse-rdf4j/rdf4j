/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.federated.endpoint;

import java.util.Arrays;
import java.util.List;

/**
 * Information about the type of an endpoint
 *
 * @author Andreas Schwarte
 *
 */
public enum EndpointType {
	NativeStore(Arrays.asList("NativeStore", "lsail/NativeStore")),
	SparqlEndpoint(Arrays.asList("SparqlEndpoint", "api/sparql")),
	RemoteRepository(List.of("RemoteRepository")),
	Other(List.of("Other"));

	private final List<String> formatNames;

	EndpointType(List<String> formatNames) {
		this.formatNames = formatNames;
	}

	/**
	 * Returns true if the endpoint type supports the given format (e.g. mime-type). Consider as an example the
	 * SparqlEndpoint which supports format "api/sparql".
	 *
	 * @param format
	 * @return true if the endpoint supports the given format
	 */
	public boolean supportsFormat(String format) {
		return formatNames.contains(format);
	}

	/**
	 * returns true if the given format is supported by some repository type.
	 *
	 * @param format
	 * @return wheter the given format is supported
	 */
	public static boolean isSupportedFormat(String format) {
		if (format == null) {
			return false;
		}
		for (EndpointType e : values()) {
			if (e.supportsFormat(format)) {
				return true;
			}
		}
		return false;
	}
}
