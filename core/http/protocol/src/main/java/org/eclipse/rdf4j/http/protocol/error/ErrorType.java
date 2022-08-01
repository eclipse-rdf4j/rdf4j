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
package org.eclipse.rdf4j.http.protocol.error;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Arjohn Kampman
 */
public class ErrorType {

	private static final Map<String, ErrorType> registry = new HashMap<>();

	public static final ErrorType MALFORMED_QUERY = register("MALFORMED QUERY");

	public static final ErrorType MALFORMED_DATA = register("MALFORMED DATA");

	public static final ErrorType UNSUPPORTED_QUERY_LANGUAGE = register("UNSUPPORTED QUERY LANGUAGE");

	public static final ErrorType UNSUPPORTED_FILE_FORMAT = register("UNSUPPORTED FILE FORMAT");

	public static final ErrorType REPOSITORY_EXISTS = register("REPOSITORY EXISTS");

	protected static ErrorType register(String label) {
		synchronized (registry) {
			ErrorType errorType = registry.get(label);

			if (errorType == null) {
				errorType = new ErrorType(label);
				registry.put(label, errorType);
			}

			return errorType;
		}
	}

	public static ErrorType forLabel(String label) {
		synchronized (registry) {
			return registry.get(label);
		}
	}

	/**
	 * The error type's label.
	 */
	private final String label;

	private ErrorType(String label) {
		this.label = label;
	}

	public String getLabel() {
		return label;
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof ErrorType) {
			return ((ErrorType) other).getLabel().equals(this.getLabel());
		}

		return false;
	}

	@Override
	public int hashCode() {
		return getLabel().hashCode();
	}

	@Override
	public String toString() {
		return label;
	}
}
