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
 * Generic textual line node when no more specific IR type is available.
 */
public class IrText extends IrNode {
	private final String text;

	public IrText(String text, boolean newScope) {
		super(newScope);
		this.text = text;
	}

	public String getText() {
		return text;
	}

	@Override
	public void print(IrPrinter p) {
		if (text == null) {
			return;
		}
		for (String ln : text.split("\\R", -1)) {
			p.line(ln);
		}
	}
}
