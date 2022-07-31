/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sparqlbuilder.constraint;

public class In extends Function {
	private final Operand searchTerm;

	In(Operand searchTerm, Operand... expressions) {
		this(searchTerm, true, expressions);
	}

	In(Operand searchTerm, boolean in, Operand... expressions) {
		super(in ? SparqlFunction.IN : SparqlFunction.NOT_IN);
		this.searchTerm = searchTerm;
		addOperand(expressions);
	}

	@Override
	public String getQueryString() {
		StringBuilder inExpression = new StringBuilder();
		inExpression.append(searchTerm.getQueryString()).append(" ");
		inExpression.append(super.getQueryString());

		return inExpression.toString();
	}
}
