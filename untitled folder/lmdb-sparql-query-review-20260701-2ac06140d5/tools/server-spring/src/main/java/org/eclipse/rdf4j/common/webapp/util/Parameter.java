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
package org.eclipse.rdf4j.common.webapp.util;

/**
 * A parameter consisting of a key and a value, which are both strings.
 */
public class Parameter {

	private final String key;

	private final String value;

	public Parameter(String key, String value) {
		this.key = key;
		this.value = value;
	}

	public String getKey() {
		return key;
	}

	public String getValue() {
		return value;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Parameter) {
			Parameter other = (Parameter) obj;
			return key.equals(other.getKey()) && value.equals(other.getValue());
		}

		return false;
	}

	@Override
	public int hashCode() {
		return key.hashCode();
	}

	@Override
	public String toString() {
		if (value == null) {
			return key;
		} else {
			return key + "=" + value;
		}
	}
}
