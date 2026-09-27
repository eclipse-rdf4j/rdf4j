/*******************************************************************************
 * Copyright (c) 2026 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.model.util;

public enum VersionLabel {
	RDF_1_2_FULL("1.2"),
	RDF_1_2_BASIC("1.2-basic"),
	RDF_1_1("1.1"),
	DEFAULT("1.2");

	private final String value;

	private VersionLabel(String value) {
		this.value = value;
	}

	public static VersionLabel fromLabel(String value) {
		for (VersionLabel v : values()) {
			if (v.value.equals(value)) {
				return v;
			}
		}
		throw new IllegalArgumentException("Unknown RDF version: " + value);
	}

	public String getValue() {
		return this.value;
	}

	public String toString() {
		return this.value;
	}
}
