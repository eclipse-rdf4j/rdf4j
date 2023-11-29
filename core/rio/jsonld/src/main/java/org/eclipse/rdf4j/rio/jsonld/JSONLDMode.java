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
package org.eclipse.rdf4j.rio.jsonld;

/**
 * Specifies constants to identify various modes that are relevant to JSONLD documents.
 *
 * @author Peter Ansell
 * @see <a href="http://json-ld.org/spec/latest/json-ld-api/#features">JSONLD Features</a>
 *
 * @since 4.3.0
 */
public enum JSONLDMode {

	EXPAND("Expansion", "https://www.w3.org/TR/json-ld11-api/#expansion"),

	COMPACT("Compaction", "https://www.w3.org/TR/json-ld11-api/#compaction"),

	FLATTEN("Flattening", "https://www.w3.org/TR/json-ld11-api/#flattening"),

	FRAME("Framing", "https://www.w3.org/TR/json-ld11-framing/"),

	;

	private final String label;

	private final String reference;

	JSONLDMode(String label, String reference) {
		this.label = label;
		this.reference = reference;
	}

	/**
	 * @return Returns the label.
	 */
	public String getLabel() {
		return label;
	}

	/**
	 * @return Returns the reference URL for the given mode.
	 */
	public String getReference() {
		return reference;
	}
}
