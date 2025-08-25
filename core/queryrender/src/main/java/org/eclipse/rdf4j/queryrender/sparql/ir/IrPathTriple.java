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
	private final String subject;
	private final String pathText;
	private final String object;

	public IrPathTriple(String subject, String pathText, String object) {
		this.subject = subject;
		this.pathText = pathText;
		this.object = object;
	}

	public String getSubject() {
		return subject;
	}

	public String getPathText() {
		return pathText;
	}

	public String getObject() {
		return object;
	}

	@Override
	public void print(IrPrinter p) {
		final String sTxt = p.applyOverridesToText(subject);
		final String oTxt = p.applyOverridesToText(object);
		p.line(sTxt + " " + pathText + " " + oTxt + " .");
	}
}
