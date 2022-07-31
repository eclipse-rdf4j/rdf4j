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

/**
 * Enumeration for tracking versions of the RDFa specification to specify processing capabilities of RDFa modules.
 *
 * @author Peter Ansell
 */
public enum RDFaVersion {

	/**
	 * The initial RDFa 1.0 version (2008)
	 *
	 * @see <a href="http://www.w3.org/TR/2008/REC-rdfa-syntax-20081014/">RDFa in XHTML: Syntax and Processing</a>
	 */
	RDFA_1_0("RDFa 1.0", "http://www.w3.org/TR/2008/REC-rdfa-syntax-20081014/"),

	/**
	 * The modified RDFa 1.1 version (2012)
	 *
	 * @see <a href="http://www.w3.org/TR/2012/REC-rdfa-core-20120607/">RDFa Core 1.1</a>
	 */
	RDFA_1_1("RDFa 1.1", "http://www.w3.org/TR/2012/REC-rdfa-core-20120607/"),

	;

	private final String label;

	private final String reference;

	RDFaVersion(String nextLabel, String nextRef) {
		label = nextLabel;
		reference = nextRef;
	}

	/**
	 * @return Returns the reference URL for the given version.
	 */
	public String getReference() {
		return reference;
	}

	/**
	 * @return Returns the label.
	 */
	public String getLabel() {
		return label;
	}
}
