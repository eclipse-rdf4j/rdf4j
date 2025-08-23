/*******************************************************************************
 * Copyright (c) 2025 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.queryrender.sparql.ir;

/**
 * Textual IR node for a property path triple: subject, path expression, object. Values are kept as rendered strings to
 * allow alternation, sequences, and quantifiers.
 */
public class IrPathTriple extends IrNode {
	private final String subjectText;
	private final String pathText;
	private final String objectText;

	public IrPathTriple(String subjectText, String pathText, String objectText) {
		this.subjectText = subjectText;
		this.pathText = pathText;
		this.objectText = objectText;
	}

	public String getSubjectText() {
		return subjectText;
	}

	public String getPathText() {
		return pathText;
	}

	public String getObjectText() {
		return objectText;
	}
}
